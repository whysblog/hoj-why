package top.hcode.hoj.service.oj.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.dao.judge.JudgeEntityService;
import top.hcode.hoj.mapper.ProblemMapper;
import top.hcode.hoj.mapper.QuizPaperItemMapper;
import top.hcode.hoj.mapper.QuizPaperMapper;
import top.hcode.hoj.pojo.entity.judge.Judge;
import top.hcode.hoj.shiro.AccountProfile;
import top.hcode.hoj.pojo.dto.QuizPaperItemDTO;
import top.hcode.hoj.pojo.dto.QuizPaperSubmitDTO;
import top.hcode.hoj.pojo.entity.problem.Problem;
import top.hcode.hoj.pojo.entity.quiz.QuizPaper;
import top.hcode.hoj.pojo.entity.quiz.QuizPaperItem;
import top.hcode.hoj.pojo.entity.quiz.QuizQuestion;
import top.hcode.hoj.pojo.vo.QuizPaperDetailVO;
import top.hcode.hoj.pojo.vo.QuizPaperItemVO;
import top.hcode.hoj.pojo.vo.QuizPaperListVO;
import top.hcode.hoj.pojo.vo.QuizPaperItemResultVO;
import top.hcode.hoj.pojo.vo.QuizPaperQuestionResultVO;
import top.hcode.hoj.pojo.vo.QuizPaperSubmitResultVO;
import top.hcode.hoj.pojo.vo.QuizQuestionInfoVO;
import top.hcode.hoj.service.oj.QuizPaperService;
import top.hcode.hoj.service.oj.QuizQuestionService;
import top.hcode.hoj.utils.Constants;
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

    @Autowired
    private JudgeEntityService judgeEntityService;

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
        AccountProfile user = (AccountProfile) SecurityUtils.getSubject().getPrincipal();
        String uid = user != null ? user.getUid() : null;

        int quizTotal = 0;
        int correct = 0;
        int wrong = 0;
        int unanswered = 0;
        int problemCount = 0;
        int problemScored = 0;
        int problemTotalScore = 0;
        int problemMaxScore = 0;

        List<QuizPaperItemResultVO> itemResults = new ArrayList<>();
        List<QuizPaperQuestionResultVO> questionResults = new ArrayList<>();
        int seq = 0;
        for (QuizPaperItem it : items) {
            seq++;
            if ("problem".equals(normalizeItemType(it.getItemType()))) {
                itemResults.add(buildProblemItemResult(seq, it, uid));
                QuizPaperItemResultVO pr = itemResults.get(itemResults.size() - 1);
                problemCount++;
                if (pr.getScore() != null) {
                    problemScored += pr.getScore();
                }
                if (pr.getMaxScore() != null) {
                    problemMaxScore += pr.getMaxScore();
                }
                continue;
            }

            quizTotal++;
            Long qid = it.getQuestionId();
            QuizQuestion q = quizQuestionService.getOne(
                    new QueryWrapper<QuizQuestion>().eq("id", qid).eq("status", 1));
            if (q == null) {
                throw new StatusFailException("题目不存在或未公开");
            }

            QuizPaperItemResultVO row = new QuizPaperItemResultVO();
            row.setNo(seq);
            row.setItemType("quiz");
            row.setQuestionId(qid);
            row.setTitle(q.getTitle());
            int qType = q.getQuestionType() == null ? 0 : q.getQuestionType();
            row.setQuestionType(qType);
            row.setExplanation(q.getExplanation());
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
                itemResults.add(row);
                questionResults.add(toLegacyQuestionRow(row));
                continue;
            }
            String nu = QuizAnswerUtils.normalize(raw);
            if (StrUtil.isBlank(nu)) {
                row.setOutcome("UNANSWERED");
                row.setUserAnswer(raw.trim());
                unanswered++;
                itemResults.add(row);
                questionResults.add(toLegacyQuestionRow(row));
                continue;
            }
            if (qType == 0) {
                if (!QuizAnswerUtils.isValidSingle(nu)) {
                    row.setOutcome("WRONG");
                    row.setUserAnswer(nu);
                    wrong++;
                    itemResults.add(row);
                    questionResults.add(toLegacyQuestionRow(row));
                    continue;
                }
            } else if (!QuizAnswerUtils.isValidMultiple(nu)) {
                row.setOutcome("WRONG");
                row.setUserAnswer(nu);
                wrong++;
                itemResults.add(row);
                questionResults.add(toLegacyQuestionRow(row));
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
            itemResults.add(row);
            questionResults.add(toLegacyQuestionRow(row));
        }

        QuizPaperSubmitResultVO vo = new QuizPaperSubmitResultVO();
        vo.setPaperId(paper.getId());
        vo.setPaperTitle(paper.getTitle());
        vo.setTotalQuestions(quizTotal);
        vo.setCorrectCount(correct);
        vo.setWrongCount(wrong);
        vo.setUnansweredCount(unanswered);
        vo.setItemResults(itemResults);
        vo.setQuestionResults(questionResults);
        StringBuilder msg = new StringBuilder();
        msg.append(String.format("客观题：答对 %d / %d（错误 %d，未作答 %d）", correct, quizTotal, wrong, unanswered));
        if (problemCount > 0) {
            msg.append(String.format("；编程题 %d 道，得分合计 %d / %d", problemCount, problemScored, problemMaxScore));
        }
        vo.setMessage(msg.toString());
        return vo;
    }

    private QuizPaperQuestionResultVO toLegacyQuestionRow(QuizPaperItemResultVO row) {
        QuizPaperQuestionResultVO legacy = new QuizPaperQuestionResultVO();
        legacy.setNo(row.getNo());
        legacy.setQuestionId(row.getQuestionId());
        legacy.setTitle(row.getTitle());
        legacy.setQuestionType(row.getQuestionType());
        legacy.setOutcome(row.getOutcome());
        legacy.setUserAnswer(row.getUserAnswer());
        legacy.setCorrectAnswer(row.getCorrectAnswer());
        return legacy;
    }

    private QuizPaperItemResultVO buildProblemItemResult(int no, QuizPaperItem it, String uid) {
        QuizPaperItemResultVO row = new QuizPaperItemResultVO();
        row.setNo(no);
        row.setItemType("problem");
        row.setPid(it.getQuestionId());
        Problem p = problemMapper.selectById(it.getQuestionId());
        if (p == null) {
            row.setTitle("编程题");
            row.setJudgeStatus(Constants.Judge.STATUS_NOT_SUBMITTED.getStatus());
            row.setJudgeStatusName(Constants.Judge.STATUS_NOT_SUBMITTED.getName());
            row.setScore(0);
            row.setMaxScore(100);
            return row;
        }
        row.setTitle(p.getTitle());
        row.setProblemId(p.getProblemId());
        int maxScore = p.getIoScore() != null && p.getIoScore() > 0 ? p.getIoScore() : 100;
        row.setMaxScore(maxScore);

        if (StrUtil.isBlank(uid)) {
            row.setJudgeStatus(Constants.Judge.STATUS_NOT_SUBMITTED.getStatus());
            row.setJudgeStatusName(Constants.Judge.STATUS_NOT_SUBMITTED.getName());
            row.setScore(0);
            return row;
        }

        QueryWrapper<Judge> qw = new QueryWrapper<>();
        qw.eq("pid", p.getId()).eq("uid", uid).eq("cid", 0).isNull("gid")
                .notIn("status",
                        Constants.Judge.STATUS_PENDING.getStatus(),
                        Constants.Judge.STATUS_COMPILING.getStatus(),
                        Constants.Judge.STATUS_JUDGING.getStatus(),
                        Constants.Judge.STATUS_SUBMITTING.getStatus())
                .orderByDesc("submit_time");
        List<Judge> judges = judgeEntityService.list(qw);
        if (judges == null || judges.isEmpty()) {
            row.setJudgeStatus(Constants.Judge.STATUS_NOT_SUBMITTED.getStatus());
            row.setJudgeStatusName(Constants.Judge.STATUS_NOT_SUBMITTED.getName());
            row.setScore(0);
            return row;
        }

        Judge latest = judges.get(0);
        boolean isAcm = p.getType() != null && p.getType().equals(Constants.ProblemType.ACM.getType());
        int latestStatus = latest.getStatus() == null ? Constants.Judge.STATUS_NOT_SUBMITTED.getStatus() : latest.getStatus();
        int latestScore;
        if (isAcm) {
            latestScore = latestStatus == Constants.Judge.STATUS_ACCEPTED.getStatus() ? maxScore : 0;
        } else {
            latestScore = latest.getScore() != null ? latest.getScore() : 0;
            if (latestStatus == Constants.Judge.STATUS_ACCEPTED.getStatus() && latestScore < maxScore) {
                latestScore = maxScore;
            }
        }
        row.setJudgeStatus(latestStatus);
        row.setJudgeStatusName(resolveJudgeStatusName(latestStatus));
        row.setLanguage(latest.getLanguage());
        row.setScore(latestScore);
        return row;
    }

    private String resolveJudgeStatusName(int status) {
        for (Constants.Judge j : Constants.Judge.values()) {
            if (j.getStatus().equals(status)) {
                return j.getName();
            }
        }
        return "Unknown";
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
