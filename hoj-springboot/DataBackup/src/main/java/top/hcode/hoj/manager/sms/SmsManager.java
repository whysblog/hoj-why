package top.hcode.hoj.manager.sms;

import cn.hutool.core.util.StrUtil;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsRequest;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * @Author: Himit_ZH
 * @Date: 2026/4/15
 * @Description: 阿里云短信服务管理
 */
@Component
@Slf4j(topic = "hoj")
public class SmsManager {

    @Value("${hoj.sms.aliyun.access-key-id:}")
    private String accessKeyId;

    @Value("${hoj.sms.aliyun.access-key-secret:}")
    private String accessKeySecret;

    @Value("${hoj.sms.aliyun.endpoint:dysmsapi.aliyuncs.com}")
    private String endpoint;

    @Value("${hoj.sms.aliyun.sign-name:}")
    private String signName;

    @Value("${hoj.sms.aliyun.reset-template-code:}")
    private String resetTemplateCode;

    public boolean isOk() {
        return StrUtil.isNotBlank(accessKeyId)
                && StrUtil.isNotBlank(accessKeySecret)
                && StrUtil.isNotBlank(signName)
                && StrUtil.isNotBlank(resetTemplateCode);
    }

    @Async
    public void sendResetPassword(String username, String code, String phone) {
        if (!isOk()) {
            return;
        }
        DefaultProfile profile = DefaultProfile.getProfile("cn-hangzhou", accessKeyId, accessKeySecret);
        IAcsClient client = new DefaultAcsClient(profile);
        SendSmsRequest request = new SendSmsRequest();
        request.setSysEndpoint(endpoint);
        request.setPhoneNumbers(phone);
        request.setSignName(signName);
        request.setTemplateCode(resetTemplateCode);
        // 当前模板仅使用 code 变量：${code}
        request.setTemplateParam("{\"code\":\"" + code + "\"}");
        try {
            SendSmsResponse response = client.getAcsResponse(request);
            if (!"OK".equals(response.getCode())) {
                log.error("发送重置密码短信失败，phone={}, code={}, message={}", phone, response.getCode(), response.getMessage());
            }
        } catch (ClientException e) {
            log.error("发送重置密码短信异常，phone={}, err={}", phone, e.getMessage());
        }
    }
}
