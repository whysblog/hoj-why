package top.hcode.hoj.service.oj.impl;

import top.hcode.hoj.pojo.dto.QuizPaperSaveDTO;
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
import top.hcode.hoj.mapper.ProblemMapper;
import top.hcode.hoj.dao.judge.JudgeEntityService;
import top.hcode.hoj.pojo.entity.judge.Judge;
import top.hcode.hoj.service.oj.QuizHistoryService;
import top.hcode.hoj.mapper.QuizPaperItemMapper;
import top.hcode.hoj.mapper.QuizPaperMapper;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class QuizPaperServiceImpl extends ServiceImpl<QuizPaperMapper, QuizPaper> implements QuizPaperService {

    @Autowired private JudgeEntityService judgeEntityService;
    @Autowired private QuizHistoryService historyService;

    @Autowired
    private QuizPaperItemMapper quizPaperItemMapper;

    @Autowired
    private QuizQuestionService quizQuestionService;

    @Autowired
    private ProblemMapper problemMapper;

    @Override
    public Page<QuizPaperListVO> getPublicPage(Integer limit, Integer currentPage, String keyword, String langCategory) {
        int size = limit == null || limit <= 0 ? 20 : Math.min(limit, 100);
        int page = currentPage == null || currentPage <= 0 ? 1 : currentPage;
        QueryWrapper<QuizPaper> qw = new QueryWrapper<>();
        qw.eq("status", 1);
        if (StrUtil.isNotBlank(keyword)) {
            qw.and(w -> w.like("title", keyword).or().like("description", keyword));
        }
        if (StrUtil.isNotBlank(langCategory)) {
            qw.eq("lang_category", langCategory.toLowerCase());
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
            if (it.getScore() != null && (it.getScore() < 0 || it.getScore() > 1000)) throw new StatusFailException("套卷题目分值配置无效");
            QuizPaperItemVO itemVO = buildItemVO(it, seq, true);
            if (itemVO.getTitle() == null || ("quiz".equals(itemVO.getItemType()) && itemVO.getQuizQuestion() == null)) {
                throw new StatusFailException("套卷包含已删除或未公开的题目，请联系管理员");
            }
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
        Map<String, QuizPaperSubmitDTO.ProblemSnapshotDTO> problemSnapshots =
                dto.getProblemSnapshots() == null ? Collections.emptyMap() : dto.getProblemSnapshots();
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
        int problemMaxScore = 0;

        List<QuizPaperItemResultVO> itemResults = new ArrayList<>();
        List<QuizPaperQuestionResultVO> questionResults = new ArrayList<>();
        int seq = 0;
        for (QuizPaperItem it : items) {
            seq++;
            if (it.getScore() != null && (it.getScore() < 0 || it.getScore() > 1000)) throw new StatusFailException("套卷题目分值配置无效");
            if ("problem".equals(normalizeItemType(it.getItemType()))) {
                itemResults.add(buildProblemItemResult(seq, it, problemSnapshots, uid));
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
            if (qType != 0 && qType != 1) throw new StatusFailException("题目类型配置无效");
            row.setQuestionType(qType);
            row.setExplanation(q.getExplanation());
            row.setQuestion(quizQuestionService.buildPublicInfo(q));
            row.setMaxScore(it.getScore() == null ? 100 : it.getScore());
            row.setScore(0);
            String nc = QuizAnswerUtils.normalize(q.getAnswer());
            if ((qType == 0 && !QuizAnswerUtils.isValidSingle(nc))
                    || (qType == 1 && !QuizAnswerUtils.isValidMultiple(nc))) {
                throw new StatusFailException("题目答案配置无效：" + q.getTitle());
            }
            row.setCorrectAnswer(nc);

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
                throw new StatusFailException("答案格式错误：" + q.getTitle());
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
                row.setScore(row.getMaxScore());
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
        vo.setScore(itemResults.stream().mapToInt(row -> row.getScore() == null ? 0 : row.getScore()).sum());
        vo.setMaxScore(itemResults.stream().mapToInt(row -> row.getMaxScore() == null ? 0 : row.getMaxScore()).sum());
        vo.setMessage(msg.toString());
        vo.setAttemptId(historyService.record("paper", paperId, paper.getTitle(), vo.getScore(), vo.getMaxScore(),
                correct, quizTotal, vo));
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
        legacy.setExplanation(row.getExplanation());
        return legacy;
    }

    private QuizPaperItemResultVO buildProblemItemResult(int no, QuizPaperItem it,
            Map<String, QuizPaperSubmitDTO.ProblemSnapshotDTO> snapshots, String uid) throws StatusFailException {
        Problem p = problemMapper.selectById(it.getQuestionId());
        if (p == null || !Integer.valueOf(1).equals(p.getAuth()) || Boolean.TRUE.equals(p.getIsGroup()) || p.getGid() != null) {
            throw new StatusFailException("编程题不存在或未公开");
        }
        QuizPaperItemResultVO row = new QuizPaperItemResultVO();
        row.setNo(no); row.setItemType("problem"); row.setPid(p.getId());
        row.setTitle(p.getTitle()); row.setProblemId(p.getProblemId());
        int max = it.getScore() == null ? 100 : it.getScore();
        row.setMaxScore(max); row.setScore(0);
        row.setJudgeStatus(Constants.Judge.STATUS_NOT_SUBMITTED.getStatus());
        row.setJudgeStatusName(Constants.Judge.STATUS_NOT_SUBMITTED.getName());
        QuizPaperSubmitDTO.ProblemSnapshotDTO snapshot = snapshots.get(String.valueOf(it.getQuestionId()));
        if (snapshot == null || snapshot.getSubmitId() == null) return row;
        Judge submission = judgeEntityService.getById(snapshot.getSubmitId());
        if (submission == null || uid == null || !uid.equals(submission.getUid())
                || !it.getQuestionId().equals(submission.getPid())
                || (submission.getCid() != null && submission.getCid() != 0)
                || submission.getGid() != null) {
            throw new StatusFailException("编程题提交记录无效或不属于当前用户");
        }
        int status = submission.getStatus() == null ? 5 : submission.getStatus();
        if (status == 5 || status == 6 || status == 7 || status == 9) throw new StatusFailException("编程题仍在评测中，请等待评测完成后交卷");
        int score = 0;
        if (status == Constants.Judge.STATUS_ACCEPTED.getStatus()) score = max;
        else if (Integer.valueOf(1).equals(p.getType())) {
            int rawMax = p.getIoScore() == null || p.getIoScore() <= 0 ? 100 : p.getIoScore();
            score = (int) Math.round(Math.max(0, Math.min(rawMax, submission.getScore() == null ? 0 : submission.getScore()))
                    * (double) max / rawMax);
        }
        row.setSubmitId(submission.getSubmitId()); row.setJudgeStatus(status);
        row.setJudgeStatusName(resolveJudgeStatusName(status)); row.setLanguage(submission.getLanguage()); row.setScore(score);
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
    public Long savePaperWithItems(QuizPaperSaveDTO dto) throws StatusFailException {
        if (dto == null || dto.getPaper() == null || dto.getItems() == null) throw new StatusFailException("请提交完整的套卷信息与题目列表");
        QuizPaper paper = dto.getPaper();
        if (StrUtil.isBlank(paper.getTitle()) || paper.getTitle().trim().length() > 255) throw new StatusFailException("标题不能为空且不得超过255字");
        if (paper.getStatus() == null) paper.setStatus(0);
        if (paper.getStatus() != 0 && paper.getStatus() != 1) throw new StatusFailException("状态无效");
        if (StrUtil.isNotBlank(paper.getLangCategory()) && !"cpp".equals(paper.getLangCategory()) && !"python".equals(paper.getLangCategory())) throw new StatusFailException("分类仅支持cpp或python");
        if (paper.getAuthor() != null && paper.getAuthor().length() > 255) throw new StatusFailException("作者长度超限");
        validateItems(dto.getItems(), paper.getStatus() == 1);
        paper.setTitle(paper.getTitle().trim()); paper.setGmtCreate(null); paper.setGmtModified(null);
        if (paper.getId() != null && getById(paper.getId()) == null) throw new StatusFailException("套卷不存在");
        if (!saveOrUpdate(paper)) throw new StatusFailException("套卷保存失败");
        replacePaperMixedItems(paper.getId(), dto.getItems());
        return paper.getId();
    }

    private void validateItems(List<QuizPaperItemDTO> items, boolean published) throws StatusFailException {
        if (items == null) throw new StatusFailException("题目列表不能为空");
        if (items.size() > 200) throw new StatusFailException("一份套卷最多200道题");
        if (published && items.isEmpty()) throw new StatusFailException("公开套卷至少需要一道题");
        java.util.Set<String> keys = new java.util.HashSet<>();
        for (QuizPaperItemDTO item : items) {
            if (item == null || item.getQuestionId() == null || item.getQuestionId() <= 0) throw new StatusFailException("题目ID无效");
            String type = item.getItemType() == null ? "quiz" : item.getItemType();
            if (!"quiz".equals(type) && !"problem".equals(type)) throw new StatusFailException("题目类型无效");
            if (!keys.add(type + ":" + item.getQuestionId())) throw new StatusFailException("套卷不能重复添加同一道题");
            if (item.getScore() != null && (item.getScore() < 0 || item.getScore() > 1000)) throw new StatusFailException("分值范围为0~1000");
            if ("quiz".equals(type)) {
                QuizQuestion q = quizQuestionService.getById(item.getQuestionId());
                if (q == null || (published && !Integer.valueOf(1).equals(q.getStatus()))) throw new StatusFailException("客观题不存在或未公开：" + item.getQuestionId());
                if (q.getQuestionType() != null && q.getQuestionType() != 0 && q.getQuestionType() != 1) throw new StatusFailException("题目类型配置无效");
                String answer = QuizAnswerUtils.normalize(q.getAnswer());
                if (published && ((Integer.valueOf(1).equals(q.getQuestionType()) && !QuizAnswerUtils.isValidMultiple(answer))
                        || (!Integer.valueOf(1).equals(q.getQuestionType()) && !QuizAnswerUtils.isValidSingle(answer)))) throw new StatusFailException("题目答案配置无效");
            } else {
                Problem p = problemMapper.selectById(item.getQuestionId());
                if (p == null || !Integer.valueOf(1).equals(p.getAuth()) || Boolean.TRUE.equals(p.getIsGroup()) || p.getGid() != null) throw new StatusFailException("编程题必须来自公开题库");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replacePaperItems(Long paperId, List<Long> questionIds) throws StatusFailException {
        if (questionIds == null) throw new StatusFailException("请提供题目列表");
        List<QuizPaperItemDTO> items = new ArrayList<>();
        for (Long id : questionIds) {
            QuizPaperItemDTO item = new QuizPaperItemDTO();
            item.setItemType("quiz"); item.setQuestionId(id); item.setScore(100); items.add(item);
        }
        replacePaperMixedItems(paperId, items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replacePaperMixedItems(Long paperId, List<QuizPaperItemDTO> items) throws StatusFailException {
        QuizPaper paper = getById(paperId);
        if (paper == null) throw new StatusFailException("套卷不存在");
        validateItems(items, Integer.valueOf(1).equals(paper.getStatus()));
        quizPaperItemMapper.delete(new QueryWrapper<QuizPaperItem>().eq("paper_id", paperId));
        int order = 0;
        for (QuizPaperItemDTO item : items) {
            QuizPaperItem row = new QuizPaperItem();
            row.setPaperId(paperId); row.setQuestionId(item.getQuestionId());
            row.setItemType(normalizeItemType(item.getItemType())); row.setSortOrder(order++);
            row.setScore(item.getScore() == null ? 100 : item.getScore());
            if (quizPaperItemMapper.insert(row) != 1) throw new StatusFailException("保存题目列表失败");
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
        vo.setScore(item.getScore() == null ? 100 : item.getScore());
        if ("problem".equals(itemType)) {
            Problem p = problemMapper.selectById(item.getQuestionId());
            if (p != null && (!publicOnly || (Integer.valueOf(1).equals(p.getAuth()) && !Boolean.TRUE.equals(p.getIsGroup()) && p.getGid() == null))) {
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
        vo.setLangCategory(p.getLangCategory());
        return vo;
    }
}
