package top.hcode.hoj.controller.oj;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import top.hcode.hoj.common.result.CommonResult;
import top.hcode.hoj.manager.oj.CertificateManager;
import top.hcode.hoj.pojo.entity.common.Certificate;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @Author: Manus
 * @Date: 2026/01/29
 * @Description: 证书查询Controller
 */
@RestController
@RequestMapping("/api/certificate")
public class CertificateController {

    @Autowired
    private CertificateManager certificateManager;

    @GetMapping("/query")
    public CommonResult<List<Certificate>> queryCertificates(@RequestParam("name") String name,
                                                            @RequestParam("idCard") String idCard) {
        return CommonResult.successResponse(certificateManager.getCertificates(name, idCard));
    }

    @GetMapping("/download")
    public void downloadCertificate(@RequestParam("id") Long id, HttpServletResponse response) {
        certificateManager.downloadCertificate(id, response);
    }
}
