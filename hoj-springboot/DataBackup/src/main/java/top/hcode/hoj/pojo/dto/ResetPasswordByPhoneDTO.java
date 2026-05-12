package top.hcode.hoj.pojo.dto;

import lombok.Data;

/**
 * 通过手机号+短信验证码重置密码
 */
@Data
public class ResetPasswordByPhoneDTO {

    private String phone;

    private String password;

    private String smsCode;
}

