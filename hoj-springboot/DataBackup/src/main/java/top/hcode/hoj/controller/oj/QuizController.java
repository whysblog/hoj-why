package top.hcode.hoj.controller.oj;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import top.hcode.hoj.annotation.AnonApi;
import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.common.result.CommonResult;
import top.hcode.hoj.pojo.dto.QuizSubmitDTO;
import top.hcode.hoj.pojo.vo.QuizQuestionInfoVO;
import top.hcode.hoj.pojo.vo.QuizQuestionListVO;
import top.hcode.hoj.pojo.vo.QuizSubmitResultVO;
import top.hcode.hoj.service.oj.QuizQuestionService;

@RestController
@RequestMapping("/api/quiz")
public class QuizController {

    @Autowired
    private QuizQuestionService quizQuestionService;

    @GetMapping("/list")
    @AnonApi
    public CommonResult<Page<QuizQuestionListVO>> list(@RequestParam(value = "limit", required = false) Integer limit,
                                                       @RequestParam(value = "currentPage", required = false) Integer currentPage,
                                                       @RequestParam(value = "keyword", required = false) String keyword,
                                                       @RequestParam(value = "difficulty", required = false) Integer difficulty,
                                                       @RequestParam(value = "langCategory", required = false) String langCategory) {
        return CommonResult.successResponse(quizQuestionService.getPublicPage(limit, currentPage, keyword, difficulty, langCategory));
    }

    @GetMapping("/{id}")
    @AnonApi
    public CommonResult<QuizQuestionInfoVO> info(@PathVariable("id") Long id) {
        try {
            return CommonResult.successResponse(quizQuestionService.getPublicInfo(id));
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        }
    }

    @PostMapping("/{id}/submit")
    @RequiresAuthentication
    public CommonResult<QuizSubmitResultVO> submit(@PathVariable("id") Long id,
                                                   @Validated @RequestBody QuizSubmitDTO dto) {
        try {
            return CommonResult.successResponse(quizQuestionService.submitAnswer(id, dto.getAnswer()));
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        }
    }
}
