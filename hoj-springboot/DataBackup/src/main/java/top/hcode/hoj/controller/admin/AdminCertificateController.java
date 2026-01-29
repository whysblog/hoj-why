package top.hcode.hoj.controller.admin;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import top.hcode.hoj.common.result.CommonResult;
import top.hcode.hoj.manager.oj.CertificateManager;
import top.hcode.hoj.pojo.entity.common.Certificate;
import top.hcode.hoj.utils.Constants;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @Author: Manus
 * @Date: 2026/01/29
 * @Description: 证书管理Controller
 */
@RestController
@RequestMapping("/api/admin/certificate")
@Slf4j(topic = "hoj")
public class AdminCertificateController {

    @Autowired
    private CertificateManager certificateManager;

    @GetMapping("/list")
    @RequiresAuthentication
    @RequiresRoles(value = {"root", "admin"}, logical = Logical.OR)
    public CommonResult<List<Certificate>> getCertificateList() {
        return CommonResult.successResponse(certificateManager.getAllCertificates());
    }

    @PostMapping("/upload")
    @RequiresAuthentication
    @RequiresRoles(value = {"root", "admin"}, logical = Logical.OR)
    public CommonResult<String> uploadCertificate(@RequestParam("file") MultipartFile file,
                                                 @RequestParam("name") String name,
                                                 @RequestParam("idCard") String idCard,
                                                 @RequestParam("certificateName") String certificateName) {
        
        if (file.isEmpty()) {
            return CommonResult.errorResponse("上传文件不能为空！");
        }

        // 获取文件后缀
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        
        // 生成文件名
        String fileName = IdUtil.simpleUUID() + suffix;
        String folderPath = Constants.File.CERTIFICATE_FOLDER.getPath();
        String filePath = folderPath + File.separator + fileName;

        // 确保目录存在
        FileUtil.mkdir(folderPath);

        try {
            file.transferTo(new File(filePath));
        } catch (IOException e) {
            log.error("证书文件上传异常-------------->{}", e.getMessage());
            return CommonResult.errorResponse("服务器异常：证书上传失败！");
        }

        // 保存到数据库
        Certificate certificate = new Certificate();
        certificate.setName(name)
                .setIdCard(idCard)
                .setCertificateName(certificateName)
                .setFilePath(filePath);
        
        certificateManager.saveCertificate(certificate);

        return CommonResult.successResponse("上传成功！");
    }

    @DeleteMapping("/delete")
    @RequiresAuthentication
    @RequiresRoles(value = {"root", "admin"}, logical = Logical.OR)
    public CommonResult<Void> deleteCertificate(@RequestParam("id") Long id) {
        certificateManager.deleteCertificate(id);
        return CommonResult.successResponse("删除成功！");
    }
}
