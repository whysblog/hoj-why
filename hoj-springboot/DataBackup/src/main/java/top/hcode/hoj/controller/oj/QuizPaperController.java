package top.hcode.hoj.controller.oj;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import top.hcode.hoj.annotation.AnonApi;
import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.common.result.CommonResult;
import top.hcode.hoj.pojo.dto.QuizPaperSubmitDTO;
import top.hcode.hoj.pojo.vo.QuizPaperDetailVO;
import top.hcode.hoj.pojo.vo.QuizPaperListVO;
import top.hcode.hoj.pojo.vo.QuizPaperSubmitResultVO;
import top.hcode.hoj.service.oj.QuizPaperService;

@RestController
@RequestMapping("/api/quiz/paper")
public class QuizPaperController {

    @Autowired
    private QuizPaperService quizPaperService;

    @GetMapping("/list")
    @AnonApi
    public CommonResult<Page<QuizPaperListVO>> list(@RequestParam(value = "limit", required = false) Integer limit,
                                                      @RequestParam(value = "currentPage", required = false) Integer currentPage,
                                                      @RequestParam(value = "keyword", required = false) String keyword,
                                                      @RequestParam(value = "langCategory", required = false) String langCategory) {
        return CommonResult.successResponse(quizPaperService.getPublicPage(limit, currentPage, keyword, langCategory));
    }

    @GetMapping("/{id}")
    @AnonApi
    public CommonResult<QuizPaperDetailVO> detail(@PathVariable Long id) {
        try {
            return CommonResult.successResponse(quizPaperService.getPublicDetail(id));
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        }
    }

    @PostMapping("/{id}/submit")
    @RequiresAuthentication
    public CommonResult<QuizPaperSubmitResultVO> submit(@PathVariable Long id,
                                                        @Validated @RequestBody QuizPaperSubmitDTO dto) {
        try {
            return CommonResult.successResponse(quizPaperService.submitPaper(id, dto));
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        }
    }
}
