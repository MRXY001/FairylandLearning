package com.iwxyi.fairyland.server.Models;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class ClientStartup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long startupId;
    private String cpuId; // 每个设备的标识符，未读取到则没有
    private Long userId; // 用户ID，未登录则没有
    private String app; // 程序名字
    private String platform; // 平台(全小写)：windows/mac/linux/android/ios
    private String period; // 测试阶段：dev/pre-alpha/alpha/beta/stable
    private String channel; // 发布渠道：允许为空
    private Integer version; // 1010
    private Date startupTime; // 应用启动时间
    private Date closeTime; // 应用关闭时间
    private Integer wordCount; // 这一段期间的字数统计

    /**
     * 可能会有多次打开，而只上传一次
     * 这是会有m个 startup-close 数组对，以及单个 startup
     * 最终添加 m + 1 条记录
     */
    public ClientStartup(String cpuId, Long userId, String app, String platform, String period, String channel,
            int version, Date startupTime, Date closeTime, int wordCount) {
        this.cpuId = cpuId;
        this.userId = userId;
        this.app = app;
        this.platform = platform;
        this.period = period;
        this.channel = channel;
        this.version = version;
        this.startupTime = startupTime;
        this.closeTime = closeTime;
        this.wordCount = wordCount;
    }
}
