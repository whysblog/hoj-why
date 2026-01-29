package top.hcode.hoj.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.hcode.hoj.pojo.entity.common.Certificate;

/**
 * @Author: Manus
 * @Date: 2026/01/29
 * @Description: 证书Mapper接口
 */
@Mapper
public interface CertificateMapper extends BaseMapper<Certificate> {
}
