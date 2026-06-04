package top.hcode.hoj.controller.admin;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
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
import java.util.ArrayList;
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
        if (StrUtil.hasBlank(name, idCard, certificateName)) {
            return CommonResult.errorResponse("姓名、身份证号和证书名称不能为空！");
        }

        String filePath = saveCertificateFile(file);
        if (filePath == null) {
            return CommonResult.errorResponse("服务器异常：证书上传失败！");
        }

        Certificate certificate = new Certificate();
        certificate.setName(name)
                .setIdCard(idCard)
                .setCertificateName(certificateName)
                .setFilePath(filePath);

        certificateManager.saveCertificate(certificate);

        return CommonResult.successResponse("上传成功！");
    }

    @PostMapping("/batch-upload")
    @RequiresAuthentication
    @RequiresRoles(value = {"root", "admin"}, logical = Logical.OR)
    public CommonResult<String> batchUploadCertificate(@RequestParam("files") MultipartFile[] files,
                                                      @RequestParam("name") String name,
                                                      @RequestParam("idCard") String idCard,
                                                      @RequestParam(value = "certificateName", required = false) String certificateName) {
        if (files == null || files.length == 0) {
            return CommonResult.errorResponse("上传文件不能为空！");
        }
        if (StrUtil.hasBlank(name, idCard)) {
            return CommonResult.errorResponse("姓名和身份证号不能为空！");
        }

        List<Certificate> certificates = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String filePath = saveCertificateFile(file);
            if (filePath == null) {
                return CommonResult.errorResponse("服务器异常：证书上传失败！");
            }
            String currentCertificateName = buildBatchCertificateName(certificateName, file.getOriginalFilename());
            certificates.add(new Certificate()
                    .setName(name)
                    .setIdCard(idCard)
                    .setCertificateName(currentCertificateName)
                    .setFilePath(filePath));
        }

        if (certificates.isEmpty()) {
            return CommonResult.errorResponse("上传文件不能为空！");
        }
        certificateManager.saveCertificates(certificates);
        return CommonResult.successResponse("批量上传成功，共上传" + certificates.size() + "个证书！");
    }

    @PostMapping("/update")
    @RequiresAuthentication
    @RequiresRoles(value = {"root", "admin"}, logical = Logical.OR)
    public CommonResult<String> updateCertificate(@RequestParam("id") Long id,
                                                 @RequestParam("name") String name,
                                                 @RequestParam("idCard") String idCard,
                                                 @RequestParam("certificateName") String certificateName,
                                                 @RequestParam(value = "file", required = false) MultipartFile file) {
        if (id == null) {
            return CommonResult.errorResponse("证书ID不能为空！");
        }
        if (StrUtil.hasBlank(name, idCard, certificateName)) {
            return CommonResult.errorResponse("姓名、身份证号和证书名称不能为空！");
        }

        Certificate certificate = certificateManager.getCertificateById(id);
        if (certificate == null) {
            return CommonResult.errorResponse("证书不存在！");
        }

        certificate.setName(name)
                .setIdCard(idCard)
                .setCertificateName(certificateName);

        if (file != null && !file.isEmpty()) {
            String oldFilePath = certificate.getFilePath();
            String newFilePath = saveCertificateFile(file);
            if (newFilePath == null) {
                return CommonResult.errorResponse("服务器异常：证书文件更新失败！");
            }
            certificate.setFilePath(newFilePath);
            deleteLocalFile(oldFilePath);
        }

        certificateManager.updateCertificate(certificate);
        return CommonResult.successResponse("修改成功！");
    }

    @DeleteMapping("/delete")
    @RequiresAuthentication
    @RequiresRoles(value = {"root", "admin"}, logical = Logical.OR)
    public CommonResult<Void> deleteCertificate(@RequestParam("id") Long id) {
        certificateManager.deleteCertificate(id);
        return CommonResult.successResponse("删除成功！");
    }

    private String saveCertificateFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (StrUtil.isBlank(originalFilename) || !originalFilename.contains(".")) {
            return null;
        }
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = IdUtil.simpleUUID() + suffix;
        String folderPath = Constants.File.CERTIFICATE_FOLDER.getPath();
        String filePath = folderPath + File.separator + fileName;
        FileUtil.mkdir(folderPath);

        try {
            file.transferTo(new File(filePath));
            return filePath;
        } catch (IOException e) {
            log.error("证书文件上传异常-------------->{}", e.getMessage());
            return null;
        }
    }

    private String buildBatchCertificateName(String certificateName, String originalFilename) {
        if (StrUtil.isNotBlank(certificateName)) {
            return certificateName;
        }
        if (StrUtil.isBlank(originalFilename)) {
            return "证书";
        }
        int suffixIndex = originalFilename.lastIndexOf(".");
        return suffixIndex > 0 ? originalFilename.substring(0, suffixIndex) : originalFilename;
    }

    private void deleteLocalFile(String filePath) {
        if (StrUtil.isNotBlank(filePath) && FileUtil.exist(filePath)) {
            FileUtil.del(filePath);
        }
    }
}
