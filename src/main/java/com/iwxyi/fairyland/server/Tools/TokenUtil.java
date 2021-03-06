package com.iwxyi.fairyland.server.Tools;

import java.util.Date;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.iwxyi.fairyland.server.Config.ConstantKey;
import com.iwxyi.fairyland.server.Config.ErrorCode;
import com.iwxyi.fairyland.server.Exception.FormatedException;
import com.iwxyi.fairyland.server.Models.User;

public class TokenUtil {

    public static String createTokenByUser(User user) {
        return JWT.create().withAudience(user.getUserId() + "")// 将 user id 保存到 token 里面
                .withExpiresAt(new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000))// 定义token的有效期
                .sign(Algorithm.HMAC256(ConstantKey.USER_JWT_KEY));// 加密秘钥，也可以使用用户保持在数据库中的密码字符串
    }

    public static boolean verifyToken(String token) {
        JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(ConstantKey.USER_JWT_KEY)).build();
        try {
            jwtVerifier.verify(token);
        } catch (JWTVerificationException e) {
            throw new FormatedException("无效 token，请重新登录", ErrorCode.Login);
        }
        return true;
    }

    public static Long getUserIdByToken(String token) {
        try {
            return Long.parseLong(JWT.decode(token).getAudience().get(0));
        } catch (JWTDecodeException j) {
            throw new FormatedException("无效 token，请重新登录", ErrorCode.Login);
        }
    }

}
