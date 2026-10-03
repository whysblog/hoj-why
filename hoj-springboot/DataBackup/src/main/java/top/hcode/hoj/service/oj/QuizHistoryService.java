package top.hcode.hoj.service.oj;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.mapper.QuizAttemptMapper;
import top.hcode.hoj.mapper.QuizQuestionMapper;
import top.hcode.hoj.pojo.entity.quiz.QuizAttempt;
import top.hcode.hoj.pojo.entity.quiz.QuizQuestion;
import top.hcode.hoj.shiro.AccountProfile;
import java.util.Date;
import java.util.*;

@Service
public class QuizHistoryService {
    @Autowired
    private QuizAttemptMapper mapper;
    @Autowired
    private QuizQuestionMapper questionMapper;

    private String uid() throws StatusFailException {
        AccountProfile user = (AccountProfile) SecurityUtils.getSubject().getPrincipal();
        if (user == null) throw new StatusFailException("请先登录");
        return user.getUid();
    }

    public Long record(String kind, Long resourceId, String title, int score, int maxScore,
                       int correct, int count, Object result) throws StatusFailException {
        QuizAttempt row = new QuizAttempt();
        row.setUid(uid()); row.setKind(kind); row.setResourceId(resourceId); row.setTitle(title);
        row.setScore(score); row.setMaxScore(maxScore); row.setCorrectCount(correct); row.setQuestionCount(count);
        row.setResultJson(JSONUtil.toJsonStr(result)); row.setGmtCreate(new Date());
        if (mapper.insert(row) != 1) throw new StatusFailException("作答记录保存失败，请重试");
        return row.getId();
    }

    public Page<QuizAttempt> history(Integer currentPage, Integer limit, String kind, Long resourceId) throws StatusFailException {
        QueryWrapper<QuizAttempt> query = new QueryWrapper<QuizAttempt>().eq("uid", uid());
        if (kind != null && !kind.isEmpty()) query.eq("kind", kind);
        if (resourceId != null) query.eq("resource_id", resourceId);
        query.select("id", "kind", "resource_id", "title", "score", "max_score", "correct_count", "question_count", "gmt_create")
                .orderByDesc("id");
        Page<QuizAttempt> page = new Page<>(currentPage == null ? 1 : Math.max(1, currentPage),
                limit == null ? 20 : Math.min(100, Math.max(1, limit)));
        mapper.selectPage(page, query);
        return page;
    }

    public JSONObject detail(Long id) throws StatusFailException {
        QuizAttempt row = mapper.selectOne(new QueryWrapper<QuizAttempt>().eq("id", id).eq("uid", uid()));
        if (row == null) throw new StatusFailException("作答记录不存在或无权查看");
        JSONObject result = JSONUtil.parseObj(row.getResultJson());
        // Only refresh explanations after ownership has been verified; answers and grades stay frozen.
        Map<Long, List<JSONObject>> targets = new HashMap<>();
        if ("quiz".equals(row.getKind())) {
            addExplanationTarget(targets, row.getResourceId(), result);
        } else if ("paper".equals(row.getKind())) {
            collectExplanationTargets(targets, result.getJSONArray("itemResults"));
            collectExplanationTargets(targets, result.getJSONArray("questionResults"));
        }
        if (!targets.isEmpty()) {
            List<QuizQuestion> questions = questionMapper.selectList(new QueryWrapper<QuizQuestion>()
                    .select("id", "explanation").in("id", targets.keySet()));
            for (QuizQuestion question : questions) {
                List<JSONObject> rows = targets.get(question.getId());
                if (rows != null) for (JSONObject target : rows) {
                    target.set("explanation", question.getExplanation() == null ? "" : question.getExplanation());
                }
            }
        }
        // Deleted questions retain their saved explanation. Never rewrite the stored attempt.
        return result.set("attemptId", row.getId()).set("kind", row.getKind())
                .set("resourceId", row.getResourceId()).set("submittedAt", row.getGmtCreate());
    }

    private void collectExplanationTargets(Map<Long, List<JSONObject>> targets, JSONArray rows) {
        if (rows == null) return;
        for (int i = 0; i < rows.size(); i++) {
            JSONObject item = rows.getJSONObject(i);
            if (item != null && !"problem".equals(item.getStr("itemType"))) {
                addExplanationTarget(targets, item.getLong("questionId"), item);
            }
        }
    }

    private void addExplanationTarget(Map<Long, List<JSONObject>> targets, Long id, JSONObject target) {
        if (id != null) targets.computeIfAbsent(id, key -> new ArrayList<>()).add(target);
    }
}
