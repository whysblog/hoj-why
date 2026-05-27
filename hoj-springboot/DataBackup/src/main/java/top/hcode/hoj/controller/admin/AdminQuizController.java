package top.hcode.hoj.controller.admin;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import top.hcode.hoj.common.result.CommonResult;
import top.hcode.hoj.pojo.entity.quiz.QuizQuestion;
import top.hcode.hoj.service.oj.QuizQuestionService;
import top.hcode.hoj.utils.QuizAnswerUtils;

@RestController
@RequestMapping("/api/admin/quiz")
public class AdminQuizController {

    @Autowired
    private QuizQuestionService quizQuestionService;

    @GetMapping("/list")
    @RequiresAuthentication
    @RequiresRoles(value = {"root", "admin", "problem_admin"}, logical = Logical.OR)
    public CommonResult<IPage<QuizQuestion>> list(@RequestParam(value = "limit", required = false) Integer limit,
                                                 @RequestParam(value = "currentPage", required = false) Integer currentPage,
                                                 @RequestParam(value = "keyword", required = false) String keyword,
                                                 @RequestParam(value = "status", required = false) Integer status,
                                                 @RequestParam(value = "langCategory", required = false) String langCategory) {
        int size = limit == null || limit <= 0 ? 20 : Math.min(limit, 100);
        int page = currentPage == null || currentPage <= 0 ? 1 : currentPage;
        QueryWrapper<QuizQuestion> qw = new QueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            qw.like("title", keyword);
        }
        if (status != null && (status == 0 || status == 1)) {
            qw.eq("status", status);
        }
        if (StrUtil.isNotBlank(langCategory)) {
            qw.eq("lang_category", langCategory.toLowerCase());
        }
        qw.orderByDesc("id");
        return CommonResult.successResponse(quizQuestionService.page(new Page<>(page, size), qw));
    }

    @GetMapping("/{id}")
    @RequiresAuthentication
    @RequiresRoles(value = {"root", "admin", "problem_admin"}, logical = Logical.OR)
    public CommonResult<QuizQuestion> detail(@PathVariable Long id) {
        QuizQuestion q = quizQuestionService.getById(id);
        if (q == null) {
            return CommonResult.errorResponse("题目不存在");
        }
        return CommonResult.successResponse(q);
    }

    @PostMapping("")
    @RequiresAuthentication
    @RequiresRoles(value = {"root", "admin", "problem_admin"}, logical = Logical.OR)
    public CommonResult<Long> create(@RequestBody QuizQuestion body) {
        String err = validate(body);
        if (err != null) {
            return CommonResult.errorResponse(err);
        }
        body.setId(null);
        if (body.getStatus() == null) {
            body.setStatus(1);
        }
        if (body.getDifficulty() == null) {
            body.setDifficulty(1);
        }
        if (body.getQuestionType() == null) {
            body.setQuestionType(0);
        }
        quizQuestionService.save(body);
        return CommonResult.successResponse(body.getId());
    }

    @PutMapping("/{id}")
    @RequiresAuthentication
    @RequiresRoles(value = {"root", "admin", "problem_admin"}, logical = Logical.OR)
    public CommonResult<Void> update(@PathVariable Long id, @RequestBody QuizQuestion body) {
        if (quizQuestionService.getById(id) == null) {
            return CommonResult.errorResponse("题目不存在");
        }
        String err = validate(body);
        if (err != null) {
            return CommonResult.errorResponse(err);
        }
        body.setId(id);
        quizQuestionService.updateById(body);
        return CommonResult.successResponse();
    }

    @DeleteMapping("/{id}")
    @RequiresAuthentication
    @RequiresRoles(value = {"root", "admin", "problem_admin"}, logical = Logical.OR)
    public CommonResult<Void> delete(@PathVariable Long id) {
        quizQuestionService.removeById(id);
        return CommonResult.successResponse();
    }

    private String validate(QuizQuestion q) {
        if (StrUtil.isBlank(q.getTitle())) {
            return "标题不能为空";
        }
        if (StrUtil.isBlank(q.getOptionA()) || StrUtil.isBlank(q.getOptionB())
                || StrUtil.isBlank(q.getOptionC()) || StrUtil.isBlank(q.getOptionD())) {
            return "四个选项不能为空";
        }
        if (StrUtil.isBlank(q.getAnswer())) {
            return "正确答案不能为空";
        }
        int qt = q.getQuestionType() == null ? 0 : q.getQuestionType();
        if (qt != 0 && qt != 1) {
            return "题型只能是 0（单选）或 1（多选）";
        }
        q.setQuestionType(qt);
        String norm = QuizAnswerUtils.normalize(q.getAnswer());
        if (StrUtil.isBlank(norm)) {
            return "正确答案须为 A-D 的组合";
        }
        if (qt == 0) {
            if (!QuizAnswerUtils.isValidSingle(norm)) {
                return "单选题答案必须是 A/B/C/D 之一";
            }
        } else {
            if (!QuizAnswerUtils.isValidMultiple(norm)) {
                return "多选题答案至少包含两个选项，如 AB、ACD";
            }
        }
        q.setAnswer(norm);
        if (q.getDifficulty() != null && (q.getDifficulty() < 0 || q.getDifficulty() > 2)) {
            return "难度取值 0~2";
        }
        if (q.getStatus() != null && q.getStatus() != 0 && q.getStatus() != 1) {
            return "状态取值 0 或 1";
        }
        if (StrUtil.isNotBlank(q.getLangCategory())
                && !"cpp".equalsIgnoreCase(q.getLangCategory())
                && !"python".equalsIgnoreCase(q.getLangCategory())) {
            return "分类仅支持 cpp 或 python";
        }
        if (StrUtil.isNotBlank(q.getLangCategory())) {
            q.setLangCategory(q.getLangCategory().toLowerCase());
        }
        return null;
    }
}
