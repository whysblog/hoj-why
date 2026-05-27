package top.hcode.hoj.service.oj.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.mapper.QuizQuestionMapper;
import top.hcode.hoj.pojo.entity.quiz.QuizQuestion;
import top.hcode.hoj.pojo.vo.QuizOptionVO;
import top.hcode.hoj.pojo.vo.QuizQuestionInfoVO;
import top.hcode.hoj.pojo.vo.QuizQuestionListVO;
import top.hcode.hoj.pojo.vo.QuizSubmitResultVO;
import top.hcode.hoj.service.oj.QuizQuestionService;
import top.hcode.hoj.utils.QuizAnswerUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuizQuestionServiceImpl extends ServiceImpl<QuizQuestionMapper, QuizQuestion> implements QuizQuestionService {

    @Override
    public Page<QuizQuestionListVO> getPublicPage(Integer limit, Integer currentPage, String keyword, Integer difficulty, String langCategory) {
        int size = limit == null || limit <= 0 ? 20 : Math.min(limit, 100);
        int page = currentPage == null || currentPage <= 0 ? 1 : currentPage;
        QueryWrapper<QuizQuestion> qw = new QueryWrapper<>();
        qw.eq("status", 1);
        if (StrUtil.isNotBlank(keyword)) {
            qw.and(w -> w.like("title", keyword).or().like("description", keyword));
        }
        if (difficulty != null && difficulty >= 0 && difficulty <= 2) {
            qw.eq("difficulty", difficulty);
        }
        if (StrUtil.isNotBlank(langCategory)) {
            qw.eq("lang_category", langCategory.toLowerCase());
        }
        qw.orderByDesc("id");
        IPage<QuizQuestion> entityPage = page(new Page<>(page, size), qw);
        Page<QuizQuestionListVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream().map(this::toListVO).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public QuizQuestionInfoVO getPublicInfo(Long id) throws StatusFailException {
        QuizQuestion q = getOne(new QueryWrapper<QuizQuestion>().eq("id", id).eq("status", 1));
        if (q == null) {
            throw new StatusFailException("题目不存在或未公开");
        }
        return buildPublicInfo(q);
    }

    @Override
    public QuizQuestionInfoVO buildPublicInfo(QuizQuestion q) {
        QuizQuestionInfoVO vo = new QuizQuestionInfoVO();
        vo.setId(q.getId());
        vo.setTitle(q.getTitle());
        vo.setDescription(q.getDescription());
        vo.setDifficulty(q.getDifficulty());
        vo.setAuthor(q.getAuthor());
        vo.setQuestionType(q.getQuestionType() == null ? 0 : q.getQuestionType());
        vo.setOptions(Arrays.asList(
                new QuizOptionVO("A", q.getOptionA()),
                new QuizOptionVO("B", q.getOptionB()),
                new QuizOptionVO("C", q.getOptionC()),
                new QuizOptionVO("D", q.getOptionD())
        ));
        return vo;
    }

    @Override
    public QuizSubmitResultVO submitAnswer(Long id, String userAnswer) throws StatusFailException {
        if (StrUtil.isBlank(userAnswer)) {
            throw new StatusFailException("请选择答案");
        }
        String normalizedUser = QuizAnswerUtils.normalize(userAnswer);
        if (StrUtil.isBlank(normalizedUser)) {
            throw new StatusFailException("答案格式错误，请仅选择 A-D");
        }
        QuizQuestion q = getOne(new QueryWrapper<QuizQuestion>().eq("id", id).eq("status", 1));
        if (q == null) {
            throw new StatusFailException("题目不存在或未公开");
        }
        int qType = q.getQuestionType() == null ? 0 : q.getQuestionType();
        String correctRaw = q.getAnswer() == null ? "" : q.getAnswer().trim();
        String normalizedCorrect = QuizAnswerUtils.normalize(correctRaw);
        if (qType == 0) {
            if (!QuizAnswerUtils.isValidSingle(normalizedUser)) {
                throw new StatusFailException("本题为单选题，请只选择一个选项");
            }
            if (!QuizAnswerUtils.isValidSingle(normalizedCorrect)) {
                throw new StatusFailException("题目数据异常：单选题答案无效");
            }
        } else {
            if (!QuizAnswerUtils.isValidMultiple(normalizedUser)) {
                throw new StatusFailException("本题为多选题，请至少选择两个选项");
            }
            if (!QuizAnswerUtils.isValidMultiple(normalizedCorrect)) {
                throw new StatusFailException("题目数据异常：多选题答案无效");
            }
        }
        boolean ok = normalizedUser.equals(normalizedCorrect);
        QuizSubmitResultVO vo = new QuizSubmitResultVO();
        vo.setCorrect(ok);
        vo.setCorrectAnswer(normalizedCorrect);
        vo.setExplanation(q.getExplanation());
        vo.setMessage(ok ? "回答正确" : "回答错误");
        return vo;
    }

    private QuizQuestionListVO toListVO(QuizQuestion q) {
        QuizQuestionListVO vo = new QuizQuestionListVO();
        vo.setId(q.getId());
        vo.setTitle(q.getTitle());
        vo.setDifficulty(q.getDifficulty());
        vo.setAuthor(q.getAuthor());
        vo.setQuestionType(q.getQuestionType() == null ? 0 : q.getQuestionType());
        return vo;
    }
}
