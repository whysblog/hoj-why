package top.hcode.hoj.manager.oj;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.hcode.hoj.common.result.ResultStatus;
import top.hcode.hoj.pojo.entity.common.Certificate;
import top.hcode.hoj.service.oj.CertificateService;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: Manus
 * @Date: 2026/01/29
 * @Description: 证书业务逻辑层
 */
@Component
@Slf4j(topic = "hoj")
public class CertificateManager {

    @Autowired
    private CertificateService certificateService;

    public List<Certificate> getCertificates(String name, String idCard) {
        QueryWrapper<Certificate> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", name).eq("id_card", idCard);
        return certificateService.list(queryWrapper);
    }

    public void saveCertificate(Certificate certificate) {
        certificateService.save(certificate);
    }

    public void deleteCertificate(Long id) {
        certificateService.removeById(id);
    }
    
    public List<Certificate> getAllCertificates() {
        return certificateService.list();
    }

    public void downloadCertificate(Long id, HttpServletResponse response) {
        Certificate certificate = certificateService.getById(id);
        if (certificate == null) {
            return;
        }
        String filePath = certificate.getFilePath();
        if (!FileUtil.exist(filePath)) {
            return;
        }
        
        File file = new File(filePath);
        String fileName = certificate.getCertificateName() + filePath.substring(filePath.lastIndexOf("."));
        
        try (InputStream is = new BufferedInputStream(new FileInputStream(file));
             OutputStream os = new BufferedOutputStream(response.getOutputStream())) {
            
            response.setContentType("application/x-download");
            response.setHeader("Content-disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8"));
            
            byte[] buffer = new byte[1024 * 10];
            int len;
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            os.flush();
        } catch (IOException e) {
            log.error("下载证书文件异常------------>", e);
        }
    }
}
