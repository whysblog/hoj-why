package top.hcode.hoj.controller.oj;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import top.hcode.hoj.annotation.AnonApi;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 阿里云短信回调接口
 */
@RestController
@RequestMapping("/api/sms")
@Slf4j(topic = "hoj")
public class SmsController {

    /**
     * 兼容阿里云回调校验与正式推送，统一返回成功格式。
     */
    @RequestMapping(value = "/callback", method = {RequestMethod.GET, RequestMethod.POST})
    @AnonApi
    public Map<String, Object> callback(HttpServletRequest request,
                                        @RequestBody(required = false) String body) {
        // 先记录回调请求，后续可按需解析签名、回执内容并落库。
        log.info("aliyun sms callback received, method={}, query={}, body={}",
                request.getMethod(), request.getQueryString(), body);

        Map<String, Object> result = new HashMap<>(2);
        result.put("code", 0);
        result.put("msg", "成功");
        return result;
    }
}
