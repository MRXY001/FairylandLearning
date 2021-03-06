package com.iwxyi.fairyland.server.Services;

import java.util.Date;
import java.util.List;

import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.iwxyi.fairyland.server.Config.ConstantKey;
import com.iwxyi.fairyland.server.Config.ConstantValue;
import com.iwxyi.fairyland.server.Config.ErrorCode;
import com.iwxyi.fairyland.server.Exception.FormatedException;
import com.iwxyi.fairyland.server.Models.PhoneValidation;
import com.iwxyi.fairyland.server.Repositories.PhoneRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PhoneService {
    @Autowired
    PhoneRepository phoneRepository;

    /**
     * 发送验证码
     * 
     * @param number 手机号（不包含区号吧）
     * @param cpuId  调用起的唯一设备码（不一定有）
     */
    public void sendCaptcha(String number, String ip, String cpuId, String message) {
        // 判断一分钟内是否发送过了，避免轰炸（不管有没有成功验证，都限制只发一次）
        PhoneValidation history = phoneRepository.findFirstByNumberOrderByCreateTimeDesc(number);
        if (history != null && history.getCreateTime().getTime() + 60 * 1000 > (new Date()).getTime()) {
            // 一分钟内有发送过得验证码
            if (!history.isVerified()) {
                // 可能是客户端的时间没弄好？
                throw new FormatedException("请等待验证码接收", ErrorCode.Wait);
            } else {
                // 想不到用户刚发送验证码并确认后居然又要改什么隐秘的东西了？
                throw new FormatedException("短信发送过于频繁，请稍后再试", ErrorCode.FrequencyNumber);
            }
        }

        // 如果一小时内同一个设备连续发送了5次，就不发送了
        Date early1h = new Date((new Date()).getTime() - 12 * ConstantValue.PHONE_VALIDATION_VALID);
        List<PhoneValidation> historysIn1h = phoneRepository.findByNumberAndCreateTimeGreaterThan(number, early1h);
        if (historysIn1h.size() > 5) {
            throw new FormatedException("短信发送过于频繁，请稍后再试", ErrorCode.FrequencyNumber);
        }

        // number不同，但是ip或cpuId相同（难道是用我的程序来轰炸别人？）
        if (ip != null) {
            List<PhoneValidation> historysOfIpIn1h = phoneRepository.findByIpAndVerifiedAndCreateTimeGreaterThan(ip,
                    false, early1h);
            if (historysOfIpIn1h.size() > 10) { // 同一局域网内是同一个IP，这个要适当放宽
                throw new FormatedException("短信发送过于频繁，请稍后再试", ErrorCode.FrequencyIp);
            }
        }
        if (cpuId != null) {
            List<PhoneValidation> historysOfIpIn1h = phoneRepository.findByIpAndVerifiedAndCreateTimeGreaterThan(ip,
                    false, early1h);
            if (historysOfIpIn1h.size() > 5) { // 同一局域网内是同一个IP，这个要适当放宽
                throw new FormatedException("短信发送过于频繁，请稍后再试", ErrorCode.FrequencyDevice);
            }
        }

        // 如果是短期内发送过而未验证的，则重新发送该验证码
        String captcha;
        if (history != null && !history.isVerified()
                && history.getCreateTime().getTime() + ConstantValue.PHONE_VALIDATION_VALID > (new Date()).getTime()) {
            // 有有效期内未验证的验证码，使用上次发送的验证码
            // 这样可能会有多条重复记录，但应该不影响吧
            captcha = history.getCaptcha();
        } else { // 没有可用的
            // 使用心得随机数验证码
            int num = (int) (Math.random() * 9000 + 1000);
            captcha = String.valueOf(num);
        }

        // 创建验证码
        Date time = new Date();
        PhoneValidation validation = new PhoneValidation(number, captcha, time, message);
        if (ip != null) {
            validation.setIp(ip); // 记录ID
        }
        if (cpuId != null) {
            validation.setCpuId(cpuId);
        }
        phoneRepository.save(validation);

        // 调用API发送验证码
        sendCaptcha(number, captcha);
    }

    /**
     * 验证手机验证码
     * 
     * @param number  手机号
     * @param captcha 验证码
     * @return 是否成功
     */
    public void validateCaptcha(String number, String captcha) {
        PhoneValidation validation = phoneRepository.findFirstByNumberOrderByCreateTimeDesc(number);
        if (validation == null) { // 未检测到这个号码
            throw new FormatedException("未发送" + number + "的验证码", ErrorCode.Invalid);
        }
        if (validation.isVerified() && validation.getCreateTime().getTime()
                + ConstantValue.PHONE_VALIDATION_VALID < (new Date()).getTime()) {
            throw new FormatedException("请发送手机验证码", ErrorCode.Invalid);
        }
        if (validation.isVerified()) { // 已经使用过的验证码
            throw new FormatedException("请重新发送验证码", ErrorCode.Retry);
        }
        if (validation.getFailCount() > 3) { // 多次验证失败
            throw new FormatedException("验证次数过多，请稍后重试", ErrorCode.Bomb);
        }
        if (validation.getCreateTime().getTime() + ConstantValue.PHONE_VALIDATION_VALID < (new Date()).getTime()) {
            throw new FormatedException("验证码已失效，请重新发送", ErrorCode.Overdue);
        }

        // 进行验证，判断是否正确
        if (validation.getCaptcha().equals(captcha)) {
            // 验证成功
            validation.setVerified(); // 设置为已经使用
            phoneRepository.save(validation);
            return;
        }
        // 验证失败，失败次数+1
        validation.setFailCount(validation.getFailCount() + 1);
        phoneRepository.save(validation);
        throw new FormatedException("请输入正确的验证码", ErrorCode.Incorrect);
    }

    /**
     * 接入手机号发送API
     */
    private void sendCaptcha(String number, String code) {
        if (ConstantKey.ALIYUN_ACCESS_KEY_ID.isEmpty()) {
            System.out.println("短信验证码 ALIYUN_ACCESS_KEY_ID 为空，假装发送了：" + code);
            return;
        }

        DefaultProfile profile = DefaultProfile.getProfile("cn-hangzhou", ConstantKey.ALIYUN_ACCESS_KEY_ID,
                ConstantKey.ALIYUN_ACCESS_KEY_SECRET);
        IAcsClient client = new DefaultAcsClient(profile);

        CommonRequest request = new CommonRequest();
        request.setSysMethod(MethodType.POST);
        request.setSysDomain("dysmsapi.aliyuncs.com");
        request.setSysVersion("2017-05-25");
        request.setSysAction("SendSms");
        request.putQueryParameter("RegionId", "cn-hangzhou");
        request.putQueryParameter("PhoneNumbers", number);
        request.putQueryParameter("SignName", "写作天下");
        request.putQueryParameter("TemplateCode", "SMS_202817872");
        request.putQueryParameter("TemplateParam", "{\"code\":" + code + "}");
        try {
            CommonResponse response = client.getCommonResponse(request);
            System.out.println(response.getData());
        } catch (ServerException e) {
            e.printStackTrace();
        } catch (ClientException e) {
            e.printStackTrace();
        }
    }
}
