package top.hcode.hoj.service.oj;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.mapper.QuizAttemptMapper;
import top.hcode.hoj.pojo.entity.quiz.QuizAttempt;
import top.hcode.hoj.shiro.AccountProfile;
import java.util.Date;

@Service
public class QuizHistoryService {
    @Autowired
    private QuizAttemptMapper mapper;

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
        return JSONUtil.parseObj(row.getResultJson()).set("attemptId", row.getId()).set("kind", row.getKind())
                .set("resourceId", row.getResourceId()).set("submittedAt", row.getGmtCreate());
    }
}
