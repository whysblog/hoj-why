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
import top.hcode.hoj.pojo.dto.QuizPaperItemsDTO;
import top.hcode.hoj.pojo.entity.quiz.QuizPaper;
import top.hcode.hoj.pojo.entity.quiz.QuizQuestion;
import top.hcode.hoj.pojo.vo.QuizPaperAdminDetailVO;
import top.hcode.hoj.service.oj.QuizPaperService;
import top.hcode.hoj.service.oj.QuizQuestionService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/quiz/paper")
public class AdminQuizPaperController {

    @Autowired
    private QuizPaperService quizPaperService;

    @Autowired
    private QuizQuestionService quizQuestionService;

    @GetMapping("/list")
    @RequiresAuthentication
    @RequiresRoles(value = {"root", "admin", "problem_admin"}, logical = Logical.OR)
    public CommonResult<IPage<QuizPaper>> list(@RequestParam(value = "limit", required = false) Integer limit,
                                               @RequestParam(value = "currentPage", required = false) Integer currentPage,
                                               @RequestParam(value = "keyword", required = false) String keyword,
                                               @RequestParam(value = "status", required = false) Integer status) {
        int size = limit == null || limit <= 0 ? 20 : Math.min(limit, 100);
        int page = currentPage == null || currentPage <= 0 ? 1 : currentPage;
        QueryWrapper<QuizPaper> qw = new QueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            qw.like("title", keyword);
        }
        if (status != null && (status == 0 || status == 1)) {
            qw.eq("status", status);
        }
        qw.orderByDesc("id");
        return CommonResult.successResponse(quizPaperService.page(new Page<>(page, size), qw));
    }

    @GetMapping("/{id}")
    @RequiresAuthentication
    @RequiresRoles(value = {"root", "admin", "problem_admin"}, logical = Logical.OR)
    public CommonResult<QuizPaperAdminDetailVO> detail(@PathVariable Long id) {
        QuizPaper paper = quizPaperService.getById(id);
        if (paper == null) {
            return CommonResult.errorResponse("套卷不存在");
        }
        QuizPaperAdminDetailVO vo = new QuizPaperAdminDetailVO();
        vo.setPaper(paper);
        vo.setQuestionIds(quizPaperService.listQuestionIdsByPaperId(id));
        return CommonResult.successResponse(vo);
    }

    @PostMapping("")
    @RequiresAuthentication
    @RequiresRoles(value = {"root", "admin", "problem_admin"}, logical = Logical.OR)
    public CommonResult<Long> create(@RequestBody QuizPaper body) {
        String err = validatePaper(body);
        if (err != null) {
            return CommonResult.errorResponse(err);
        }
        body.setId(null);
        if (body.getStatus() == null) {
            body.setStatus(1);
        }
        quizPaperService.save(body);
        return CommonResult.successResponse(body.getId());
    }

    @PutMapping("/{id}")
    @RequiresAuthentication
    @RequiresRoles(value = {"root", "admin", "problem_admin"}, logical = Logical.OR)
    public CommonResult<Void> update(@PathVariable Long id, @RequestBody QuizPaper body) {
        if (quizPaperService.getById(id) == null) {
            return CommonResult.errorResponse("套卷不存在");
        }
        String err = validatePaper(body);
        if (err != null) {
            return CommonResult.errorResponse(err);
        }
        body.setId(id);
        quizPaperService.updateById(body);
        return CommonResult.successResponse();
    }

    @PutMapping("/{id}/items")
    @RequiresAuthentication
    @RequiresRoles(value = {"root", "admin", "problem_admin"}, logical = Logical.OR)
    public CommonResult<Void> saveItems(@PathVariable Long id, @RequestBody QuizPaperItemsDTO dto) {
        if (quizPaperService.getById(id) == null) {
            return CommonResult.errorResponse("套卷不存在");
        }
        List<Long> qids = dto == null ? null : dto.getQuestionIds();
        if (qids != null) {
            for (Long qid : qids) {
                if (qid == null) {
                    continue;
                }
                QuizQuestion q = quizQuestionService.getById(qid);
                if (q == null) {
                    return CommonResult.errorResponse("题目不存在: " + qid);
                }
            }
        }
        quizPaperService.replacePaperItems(id, qids);
        return CommonResult.successResponse();
    }

    @DeleteMapping("/{id}")
    @RequiresAuthentication
    @RequiresRoles(value = {"root", "admin", "problem_admin"}, logical = Logical.OR)
    public CommonResult<Void> delete(@PathVariable Long id) {
        quizPaperService.removeById(id);
        return CommonResult.successResponse();
    }

    private String validatePaper(QuizPaper p) {
        if (StrUtil.isBlank(p.getTitle())) {
            return "套卷标题不能为空";
        }
        if (p.getStatus() != null && p.getStatus() != 0 && p.getStatus() != 1) {
            return "状态取值 0 或 1";
        }
        return null;
    }
}
