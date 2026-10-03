package top.hcode.hoj.controller.oj;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.common.result.CommonResult;
import top.hcode.hoj.pojo.entity.quiz.QuizAttempt;
import top.hcode.hoj.service.oj.QuizHistoryService;

@RestController
@RequiresAuthentication
@RequestMapping("/api/quiz/history")
public class QuizHistoryController {
    @Autowired private QuizHistoryService historyService;
    @GetMapping("")
    public CommonResult<Page<QuizAttempt>> history(@RequestParam(required = false) Integer currentPage,
            @RequestParam(required = false) Integer limit, @RequestParam(required = false) String kind,
            @RequestParam(required = false) Long resourceId) {
        try { return CommonResult.successResponse(historyService.history(currentPage, limit, kind, resourceId)); }
        catch (StatusFailException e) { return CommonResult.errorResponse(e.getMessage()); }
    }
    @GetMapping("/{id}")
    public CommonResult<JSONObject> detail(@PathVariable Long id) {
        try { return CommonResult.successResponse(historyService.detail(id)); }
        catch (StatusFailException e) { return CommonResult.errorResponse(e.getMessage()); }
    }
}
