package top.hcode.hoj.controller.admin;

import top.hcode.hoj.pojo.dto.QuizPaperSaveDTO;
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
import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.pojo.dto.QuizPaperItemDTO;
import top.hcode.hoj.pojo.dto.QuizPaperItemsDTO;
import top.hcode.hoj.pojo.entity.quiz.QuizPaper;
import top.hcode.hoj.pojo.vo.QuizPaperAdminDetailVO;
import top.hcode.hoj.service.oj.QuizPaperService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/quiz/paper")
public class AdminQuizPaperController {

    @Autowired
    private QuizPaperService quizPaperService;

    @GetMapping("/list")
    @RequiresAuthentication
    @RequiresRoles(value = {"root", "admin", "problem_admin"}, logical = Logical.OR)
    public CommonResult<IPage<QuizPaper>> list(@RequestParam(value = "limit", required = false) Integer limit,
                                               @RequestParam(value = "currentPage", required = false) Integer currentPage,
                                               @RequestParam(value = "keyword", required = false) String keyword,
                                               @RequestParam(value = "status", required = false) Integer status,
                                               @RequestParam(value = "langCategory", required = false) String langCategory) {
        int size = limit == null || limit <= 0 ? 20 : Math.min(limit, 100);
        int page = currentPage == null || currentPage <= 0 ? 1 : currentPage;
        QueryWrapper<QuizPaper> qw = new QueryWrapper<>();
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
        vo.setItems(quizPaperService.listPaperItemsByPaperId(id));
        return CommonResult.successResponse(vo);
    }

    @PostMapping("")
    @RequiresAuthentication
    @RequiresRoles(value = {"root", "admin", "problem_admin"}, logical = Logical.OR)
    public CommonResult<Long> create(@RequestBody QuizPaper body) {
        if (body == null) return CommonResult.errorResponse("请提供套卷信息");
        body.setId(null);
        if (body.getStatus() == null) body.setStatus(0);
        QuizPaperSaveDTO dto = new QuizPaperSaveDTO();
        dto.setPaper(body); dto.setItems(java.util.Collections.emptyList());
        return save(dto);
    }

    @PutMapping("/{id}")
    @RequiresAuthentication
    @RequiresRoles(value = {"root", "admin", "problem_admin"}, logical = Logical.OR)
    public CommonResult<Void> update(@PathVariable Long id, @RequestBody QuizPaper body) {
        if (body == null) return CommonResult.errorResponse("请提供套卷信息");
        QuizPaper existing = quizPaperService.getById(id);
        if (existing == null) return CommonResult.errorResponse("套卷不存在");
        body.setId(id);
        if (body.getStatus() == null) body.setStatus(existing.getStatus());
        QuizPaperSaveDTO dto = new QuizPaperSaveDTO();
        dto.setPaper(body);
        dto.setItems(quizPaperService.listPaperItemsByPaperId(id).stream().map(item -> {
            QuizPaperItemDTO row = new QuizPaperItemDTO();
            row.setItemType(item.getItemType()); row.setQuestionId(item.getQuestionId()); row.setScore(item.getScore());
            return row;
        }).collect(java.util.stream.Collectors.toList()));
        try { quizPaperService.savePaperWithItems(dto); return CommonResult.successResponse(); }
        catch (StatusFailException e) { return CommonResult.errorResponse(e.getMessage()); }
    }

    @PutMapping("/{id}/items")
    @RequiresAuthentication
    @RequiresRoles(value = {"root", "admin", "problem_admin"}, logical = Logical.OR)
    public CommonResult<Void> saveItems(@PathVariable Long id, @RequestBody QuizPaperItemsDTO dto) {
        try {
            if (dto == null) return CommonResult.errorResponse("请提供题目列表");
            if (dto.getItems() != null) quizPaperService.replacePaperMixedItems(id, dto.getItems());
            else quizPaperService.replacePaperItems(id, dto.getQuestionIds());
            return CommonResult.successResponse();
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        }
    }

    @PostMapping("/save")
    @RequiresAuthentication
    @RequiresRoles(value = {"root", "admin", "problem_admin"}, logical = Logical.OR)
    public CommonResult<Long> save(@RequestBody QuizPaperSaveDTO dto) {
        try { return CommonResult.successResponse(quizPaperService.savePaperWithItems(dto)); }
        catch (StatusFailException e) { return CommonResult.errorResponse(e.getMessage()); }
    }

    @DeleteMapping("/{id}")
    @RequiresAuthentication
    @RequiresRoles(value = {"root", "admin", "problem_admin"}, logical = Logical.OR)
    public CommonResult<Void> delete(@PathVariable Long id) {
        if (!quizPaperService.removeById(id)) return CommonResult.errorResponse("套卷不存在或删除失败");
        return CommonResult.successResponse();
    }

}
