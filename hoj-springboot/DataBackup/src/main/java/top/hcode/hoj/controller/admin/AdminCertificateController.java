package top.hcode.hoj.controller.admin;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @Author: Manus
 * @Date: 2026/01/29
 * @Description: 证书管理Controller
 */
@RestController
@RequestMapping("/api/admin/certificate")
@Slf4j(topic = "hoj")
public class AdminCertificateController {

    private static final String CSV_NAME_HEADER = "姓名";
    private static final String CSV_ID_CARD_HEADER = "身份证号";
    private static final String CSV_FILE_NAME_HEADER = "文件名";

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
    public CommonResult<String> batchUploadCertificate(@RequestParam("zipFile") MultipartFile zipFile,
                                                      @RequestParam("csvFile") MultipartFile csvFile) {
        if (zipFile == null || zipFile.isEmpty()) {
            return CommonResult.errorResponse("证书压缩包不能为空！");
        }
        if (csvFile == null || csvFile.isEmpty()) {
            return CommonResult.errorResponse("证书CSV不能为空！");
        }
        if (!isFileType(zipFile.getOriginalFilename(), ".zip")) {
            return CommonResult.errorResponse("证书压缩包必须是zip格式！");
        }
        if (!isFileType(csvFile.getOriginalFilename(), ".csv")) {
            return CommonResult.errorResponse("证书清单必须是csv格式！");
        }

        CsvParseResult csvParseResult = parseCertificateCsv(csvFile);
        if (StrUtil.isNotBlank(csvParseResult.getError())) {
            return CommonResult.errorResponse(csvParseResult.getError());
        }

        Set<String> expectedFileNames = new HashSet<>();
        for (CertificateCsvRow row : csvParseResult.getRows()) {
            expectedFileNames.add(row.getFileName());
        }

        Map<String, String> extractedFileMap = new HashMap<>();
        try {
            String unzipError = unzipCertificateFiles(zipFile, extractedFileMap, expectedFileNames);
            if (StrUtil.isNotBlank(unzipError)) {
                deleteExtractedFiles(extractedFileMap);
                return CommonResult.errorResponse(unzipError);
            }

            List<Certificate> certificates = new ArrayList<>();
            for (CertificateCsvRow row : csvParseResult.getRows()) {
                String filePath = extractedFileMap.get(row.getFileName());
                if (StrUtil.isBlank(filePath)) {
                    deleteExtractedFiles(extractedFileMap);
                    return CommonResult.errorResponse("压缩包中未找到CSV指定的证书文件：" + row.getFileName());
                }
                certificates.add(new Certificate()
                        .setName(row.getName())
                        .setIdCard(row.getIdCard())
                        .setCertificateName(buildCertificateName(row.getFileName()))
                        .setFilePath(filePath));
            }

            certificateManager.saveCertificates(certificates);
            return CommonResult.successResponse("批量上传成功，共上传" + certificates.size() + "个证书！");
        } catch (IOException e) {
            deleteExtractedFiles(extractedFileMap);
            log.error("证书批量上传异常-------------->{}", e.getMessage());
            return CommonResult.errorResponse("服务器异常：证书批量上传失败！");
        }
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

    private CsvParseResult parseCertificateCsv(MultipartFile csvFile) {
        List<CertificateCsvRow> rows = new ArrayList<>();
        Set<String> fileNames = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csvFile.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (StrUtil.isBlank(headerLine)) {
                return CsvParseResult.error("CSV不能为空！");
            }

            List<String> headers = parseCsvLine(removeUtf8Bom(headerLine));
            int nameIndex = headers.indexOf(CSV_NAME_HEADER);
            int idCardIndex = headers.indexOf(CSV_ID_CARD_HEADER);
            int fileNameIndex = headers.indexOf(CSV_FILE_NAME_HEADER);
            if (nameIndex < 0 || idCardIndex < 0 || fileNameIndex < 0) {
                return CsvParseResult.error("CSV第一行必须是标题行，且包含：姓名、身份证号、文件名");
            }

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (StrUtil.isBlank(line)) {
                    continue;
                }
                List<String> columns = parseCsvLine(line);
                String name = getCsvValue(columns, nameIndex);
                String idCard = getCsvValue(columns, idCardIndex);
                String fileName = normalizeCertificateFileName(getCsvValue(columns, fileNameIndex));
                if (StrUtil.hasBlank(name, idCard, fileName)) {
                    return CsvParseResult.error("CSV第" + lineNumber + "行姓名、身份证号、文件名不能为空！");
                }
                if (isUnsafeZipEntryName(fileName)) {
                    return CsvParseResult.error("CSV第" + lineNumber + "行文件名不能包含目录：" + fileName);
                }
                if (!fileNames.add(fileName)) {
                    return CsvParseResult.error("CSV文件名重复：" + fileName);
                }
                rows.add(new CertificateCsvRow(name, idCard, fileName));
            }
        } catch (IOException e) {
            log.error("证书CSV读取异常-------------->{}", e.getMessage());
            return CsvParseResult.error("服务器异常：CSV读取失败！");
        }

        if (rows.isEmpty()) {
            return CsvParseResult.error("CSV至少需要包含一条证书记录！");
        }
        return CsvParseResult.success(rows);
    }

    private List<String> parseCsvLine(String line) {
        List<String> columns = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char currentChar = line.charAt(i);
            if (currentChar == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (currentChar == ',' && !inQuotes) {
                columns.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(currentChar);
            }
        }
        columns.add(current.toString().trim());
        return columns;
    }

    private String getCsvValue(List<String> columns, int index) {
        return index < columns.size() ? columns.get(index) : "";
    }

    private String normalizeCertificateFileName(String fileName) {
        return StrUtil.isBlank(fileName) ? "" : removeUtf8Bom(fileName).trim();
    }

    private String removeUtf8Bom(String text) {
        return text != null && text.startsWith("\uFEFF") ? text.substring(1) : text;
    }

    private String unzipCertificateFiles(MultipartFile zipFile, Map<String, String> extractedFileMap, Set<String> expectedFileNames) throws IOException {
        String folderPath = Constants.File.CERTIFICATE_FOLDER.getPath();
        FileUtil.mkdir(folderPath);
        try (ZipInputStream zipInputStream = new ZipInputStream(zipFile.getInputStream(), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                String entryName = normalizeCertificateFileName(entry.getName());
                if (entry.isDirectory() || isUnsafeZipEntryName(entryName)) {
                    return "压缩包内不能包含子目录或非法路径：" + entryName;
                }
                if (!expectedFileNames.contains(entryName)) {
                    zipInputStream.closeEntry();
                    continue;
                }
                if (extractedFileMap.containsKey(entryName)) {
                    return "压缩包内文件名重复：" + entryName;
                }
                if (!entryName.contains(".")) {
                    return "压缩包内证书文件必须包含后缀名：" + entryName;
                }

                String suffix = entryName.substring(entryName.lastIndexOf("."));
                String fileName = IdUtil.simpleUUID() + suffix;
                String filePath = folderPath + File.separator + fileName;
                writeZipEntryToFile(zipInputStream, filePath);
                extractedFileMap.put(entryName, filePath);
                zipInputStream.closeEntry();
            }
        }
        if (extractedFileMap.isEmpty()) {
            return "压缩包内未找到CSV指定的证书文件！";
        }
        return null;
    }

    private void writeZipEntryToFile(ZipInputStream zipInputStream, String filePath) throws IOException {
        try (BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(filePath))) {
            byte[] buffer = new byte[1024 * 10];
            int len;
            while ((len = zipInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            outputStream.flush();
        }
    }

    private boolean isUnsafeZipEntryName(String fileName) {
        return StrUtil.isBlank(fileName)
                || fileName.contains("/")
                || fileName.contains("\\")
                || fileName.contains("..");
    }

    private boolean isFileType(String fileName, String suffix) {
        return StrUtil.isNotBlank(fileName) && fileName.toLowerCase().endsWith(suffix);
    }

    private String buildCertificateName(String fileName) {
        int suffixIndex = fileName.lastIndexOf(".");
        return suffixIndex > 0 ? fileName.substring(0, suffixIndex) : fileName;
    }

    private void deleteExtractedFiles(Map<String, String> extractedFileMap) {
        for (String filePath : extractedFileMap.values()) {
            deleteLocalFile(filePath);
        }
    }

    private void deleteLocalFile(String filePath) {
        if (StrUtil.isNotBlank(filePath) && FileUtil.exist(filePath)) {
            FileUtil.del(filePath);
        }
    }

    @Data
    @AllArgsConstructor
    private static class CertificateCsvRow {
        private String name;
        private String idCard;
        private String fileName;
    }

    @Data
    @AllArgsConstructor
    private static class CsvParseResult {
        private List<CertificateCsvRow> rows;
        private String error;

        private static CsvParseResult success(List<CertificateCsvRow> rows) {
            return new CsvParseResult(rows, null);
        }

        private static CsvParseResult error(String error) {
            return new CsvParseResult(null, error);
        }
    }
}
