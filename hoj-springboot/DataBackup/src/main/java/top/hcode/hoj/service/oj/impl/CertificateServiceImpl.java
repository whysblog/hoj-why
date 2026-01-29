package top.hcode.hoj.service.oj.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import top.hcode.hoj.mapper.CertificateMapper;
import top.hcode.hoj.pojo.entity.common.Certificate;
import top.hcode.hoj.service.oj.CertificateService;

/**
 * @Author: Manus
 * @Date: 2026/01/29
 * @Description: 证书Service实现类
 */
@Service
public class CertificateServiceImpl extends ServiceImpl<CertificateMapper, Certificate> implements CertificateService {
}
