package top.hcode.hoj.service.oj;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.pojo.entity.quiz.QuizQuestion;
import top.hcode.hoj.pojo.vo.QuizQuestionInfoVO;
import top.hcode.hoj.pojo.vo.QuizQuestionListVO;
import top.hcode.hoj.pojo.vo.QuizSubmitResultVO;

public interface QuizQuestionService extends IService<QuizQuestion> {

    Page<QuizQuestionListVO> getPublicPage(Integer limit, Integer currentPage, String keyword, Integer difficulty, String langCategory);

    QuizQuestionInfoVO getPublicInfo(Long id) throws StatusFailException;

    /**
     * 将已加载的题目实体转为前台展示 VO（不含正确答案以外的敏感字段，答案永不返回）。
     */
    QuizQuestionInfoVO buildPublicInfo(QuizQuestion q);

    QuizSubmitResultVO submitAnswer(Long id, String userAnswer) throws StatusFailException;
}
