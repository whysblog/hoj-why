package top.hcode.hoj.service.oj.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.mapper.ProblemMapper;
import top.hcode.hoj.mapper.QuizPaperItemMapper;
import top.hcode.hoj.mapper.QuizPaperMapper;
import top.hcode.hoj.pojo.dto.QuizPaperItemDTO;
import top.hcode.hoj.pojo.dto.QuizPaperSubmitDTO;
import top.hcode.hoj.pojo.entity.problem.Problem;
import top.hcode.hoj.pojo.entity.quiz.QuizPaper;
import top.hcode.hoj.pojo.entity.quiz.QuizPaperItem;
import top.hcode.hoj.pojo.entity.quiz.QuizQuestion;
import top.hcode.hoj.pojo.vo.QuizPaperDetailVO;
import top.hcode.hoj.pojo.vo.QuizPaperItemVO;
import top.hcode.hoj.pojo.vo.QuizPaperListVO;
import top.hcode.hoj.pojo.vo.QuizPaperQuestionResultVO;
import top.hcode.hoj.pojo.vo.QuizPaperSubmitResultVO;
import top.hcode.hoj.pojo.vo.QuizQuestionInfoVO;
import top.hcode.hoj.service.oj.QuizPaperService;
import top.hcode.hoj.service.oj.QuizQuestionService;
import top.hcode.hoj.utils.QuizAnswerUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class QuizPaperServiceImpl extends ServiceImpl<QuizPaperMapper, QuizPaper> implements QuizPaperService {

    @Autowired
    private QuizPaperItemMapper quizPaperItemMapper;

    @Autowired
    private QuizQuestionService quizQuestionService;

    @Autowired
    private ProblemMapper problemMapper;

    @Override
    public Page<QuizPaperListVO> getPublicPage(Integer limit, Integer currentPage, String keyword) {
        int size = limit == null || limit <= 0 ? 20 : Math.min(limit, 100);
        int page = currentPage == null || currentPage <= 0 ? 1 : currentPage;
        QueryWrapper<QuizPaper> qw = new QueryWrapper<>();
        qw.eq("status", 1);
        if (StrUtil.isNotBlank(keyword)) {
            qw.and(w -> w.like("title", keyword).or().like("description", keyword));
        }
        qw.orderByDesc("id");
        IPage<QuizPaper> entityPage = page(new Page<>(page, size), qw);
        Page<QuizPaperListVO> voPage = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        voPage.setRecords(entityPage.getRecords().stream().map(this::toListVO).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public QuizPaperDetailVO getPublicDetail(Long paperId) throws StatusFailException {
        QuizPaper paper = getOne(new QueryWrapper<QuizPaper>().eq("id", paperId).eq("status", 1));
        if (paper == null) {
            throw new StatusFailException("套卷不存在或未公开");
        }
        List<QuizPaperItem> items = quizPaperItemMapper.selectList(
                new QueryWrapper<QuizPaperItem>().eq("paper_id", paperId).orderByAsc("sort_order"));
        List<QuizQuestionInfoVO> questions = new ArrayList<>();
        List<QuizPaperItemVO> paperItems = new ArrayList<>();
        int seq = 0;
        for (QuizPaperItem it : items) {
            seq++;
            QuizPaperItemVO itemVO = buildItemVO(it, seq, true);
            if ("quiz".equals(itemVO.getItemType()) && itemVO.getQuizQuestion() != null) {
                questions.add(itemVO.getQuizQuestion());
            }
            paperItems.add(itemVO);
        }
        QuizPaperDetailVO vo = new QuizPaperDetailVO();
        vo.setId(paper.getId());
        vo.setTitle(paper.getTitle());
        vo.setDescription(paper.getDescription());
        vo.setAuthor(paper.getAuthor());
        vo.setQuestions(questions);
        vo.setItems(paperItems);
        return vo;
    }

    @Override
    public QuizPaperSubmitResultVO submitPaper(Long paperId, QuizPaperSubmitDTO dto) throws StatusFailException {
        if (dto == null || dto.getAnswers() == null) {
            throw new StatusFailException("请提交答案");
        }
        Map<String, String> answers = dto.getAnswers();
        QuizPaper paper = getOne(new QueryWrapper<QuizPaper>().eq("id", paperId).eq("status", 1));
        if (paper == null) {
            throw new StatusFailException("套卷不存在或未公开");
        }
        List<QuizPaperItem> items = quizPaperItemMapper.selectList(
                new QueryWrapper<QuizPaperItem>().eq("paper_id", paperId).orderByAsc("sort_order"));
        if (items.isEmpty()) {
            throw new StatusFailException("该套卷暂无题目");
        }
        int total = 0;
        int correct = 0;
        int wrong = 0;
        int unanswered = 0;
        List<QuizPaperQuestionResultVO> questionResults = new ArrayList<>();
        int seq = 0;
        for (QuizPaperItem it : items) {
            if ("problem".equals(normalizeItemType(it.getItemType()))) {
                continue;
            }
            seq++;
            total++;
            Long qid = it.getQuestionId();
            QuizQuestion q = quizQuestionService.getOne(
                    new QueryWrapper<QuizQuestion>().eq("id", qid).eq("status", 1));
            if (q == null) {
                throw new StatusFailException("题目不存在或未公开");
            }
            QuizPaperQuestionResultVO row = new QuizPaperQuestionResultVO();
            row.setNo(seq);
            row.setQuestionId(qid);
            row.setTitle(q.getTitle());
            int qType = q.getQuestionType() == null ? 0 : q.getQuestionType();
            row.setQuestionType(qType);
            String nc = QuizAnswerUtils.normalize(q.getAnswer());
            row.setCorrectAnswer(StrUtil.isBlank(nc) ? "" : nc);

            String raw = answers.get(String.valueOf(qid));
            if (raw == null) {
                raw = answers.get(Long.toString(qid));
            }
            if (StrUtil.isBlank(raw)) {
                row.setOutcome("UNANSWERED");
                row.setUserAnswer("");
                unanswered++;
                questionResults.add(row);
                continue;
            }
            String nu = QuizAnswerUtils.normalize(raw);
            if (StrUtil.isBlank(nu)) {
                row.setOutcome("UNANSWERED");
                row.setUserAnswer(raw.trim());
                unanswered++;
                questionResults.add(row);
                continue;
            }
            if (qType == 0) {
                if (!QuizAnswerUtils.isValidSingle(nu)) {
                    row.setOutcome("WRONG");
                    row.setUserAnswer(nu);
                    wrong++;
                    questionResults.add(row);
                    continue;
                }
            } else if (!QuizAnswerUtils.isValidMultiple(nu)) {
                row.setOutcome("WRONG");
                row.setUserAnswer(nu);
                wrong++;
                questionResults.add(row);
                continue;
            }
            if (nu.equals(nc)) {
                row.setOutcome("CORRECT");
                row.setUserAnswer(nu);
                correct++;
            } else {
                row.setOutcome("WRONG");
                row.setUserAnswer(nu);
                wrong++;
            }
            questionResults.add(row);
        }
        QuizPaperSubmitResultVO vo = new QuizPaperSubmitResultVO();
        vo.setPaperId(paper.getId());
        vo.setPaperTitle(paper.getTitle());
        vo.setTotalQuestions(total);
        vo.setCorrectCount(correct);
        vo.setWrongCount(wrong);
        vo.setUnansweredCount(unanswered);
        vo.setQuestionResults(questionResults);
        vo.setMessage(String.format("答对 %d / %d 题（错误 %d，未作答 %d）", correct, total, wrong, unanswered));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replacePaperItems(Long paperId, List<Long> questionIds) {
        quizPaperItemMapper.delete(new QueryWrapper<QuizPaperItem>().eq("paper_id", paperId));
        if (questionIds == null || questionIds.isEmpty()) {
            return;
        }
        int order = 0;
        for (Long qid : questionIds) {
            if (qid == null) {
                continue;
            }
            QuizPaperItem row = new QuizPaperItem();
            row.setPaperId(paperId);
            row.setQuestionId(qid);
            row.setItemType("quiz");
            row.setSortOrder(order++);
            quizPaperItemMapper.insert(row);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replacePaperMixedItems(Long paperId, List<QuizPaperItemDTO> items) {
        quizPaperItemMapper.delete(new QueryWrapper<QuizPaperItem>().eq("paper_id", paperId));
        if (items == null || items.isEmpty()) {
            return;
        }
        int order = 0;
        for (QuizPaperItemDTO item : items) {
            if (item == null || item.getQuestionId() == null) {
                continue;
            }
            QuizPaperItem row = new QuizPaperItem();
            row.setPaperId(paperId);
            row.setQuestionId(item.getQuestionId());
            row.setItemType(normalizeItemType(item.getItemType()));
            row.setSortOrder(order++);
            quizPaperItemMapper.insert(row);
        }
    }

    @Override
    public List<Long> listQuestionIdsByPaperId(Long paperId) {
        return quizPaperItemMapper.selectList(
                        new QueryWrapper<QuizPaperItem>().eq("paper_id", paperId).orderByAsc("sort_order"))
                .stream()
                .filter(item -> "quiz".equals(normalizeItemType(item.getItemType())))
                .map(QuizPaperItem::getQuestionId)
                .collect(Collectors.toList());
    }

    @Override
    public List<QuizPaperItemVO> listPaperItemsByPaperId(Long paperId) {
        List<QuizPaperItem> items = quizPaperItemMapper.selectList(
                new QueryWrapper<QuizPaperItem>().eq("paper_id", paperId).orderByAsc("sort_order"));
        List<QuizPaperItemVO> rows = new ArrayList<>();
        int seq = 0;
        for (QuizPaperItem item : items) {
            rows.add(buildItemVO(item, ++seq, false));
        }
        return rows;
    }

    private QuizPaperItemVO buildItemVO(QuizPaperItem item, Integer no, boolean publicOnly) {
        String itemType = normalizeItemType(item.getItemType());
        QuizPaperItemVO vo = new QuizPaperItemVO();
        vo.setNo(no);
        vo.setItemType(itemType);
        vo.setQuestionId(item.getQuestionId());
        if ("problem".equals(itemType)) {
            Problem p = problemMapper.selectById(item.getQuestionId());
            if (p != null && (!publicOnly || (p.getAuth() != null && p.getAuth() == 1))) {
                vo.setProblemId(p.getProblemId());
                vo.setTitle(p.getTitle());
            }
        } else {
            QueryWrapper<QuizQuestion> qw = new QueryWrapper<QuizQuestion>().eq("id", item.getQuestionId());
            if (publicOnly) {
                qw.eq("status", 1);
            }
            QuizQuestion q = quizQuestionService.getOne(qw);
            if (q != null) {
                vo.setTitle(q.getTitle());
                vo.setQuestionType(q.getQuestionType());
                if (publicOnly) {
                    vo.setQuizQuestion(quizQuestionService.buildPublicInfo(q));
                }
            }
        }
        return vo;
    }

    private String normalizeItemType(String itemType) {
        return "problem".equalsIgnoreCase(itemType) ? "problem" : "quiz";
    }

    private QuizPaperListVO toListVO(QuizPaper p) {
        QuizPaperListVO vo = new QuizPaperListVO();
        vo.setId(p.getId());
        vo.setTitle(p.getTitle());
        vo.setAuthor(p.getAuthor());
        return vo;
    }
}
