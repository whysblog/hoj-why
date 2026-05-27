package top.hcode.hoj.service.oj;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.pojo.dto.QuizPaperItemDTO;
import top.hcode.hoj.pojo.dto.QuizPaperSubmitDTO;
import top.hcode.hoj.pojo.entity.quiz.QuizPaper;
import top.hcode.hoj.pojo.vo.QuizPaperDetailVO;
import top.hcode.hoj.pojo.vo.QuizPaperItemVO;
import top.hcode.hoj.pojo.vo.QuizPaperListVO;
import top.hcode.hoj.pojo.vo.QuizPaperSubmitResultVO;

import java.util.List;

public interface QuizPaperService extends IService<QuizPaper> {

    Page<QuizPaperListVO> getPublicPage(Integer limit, Integer currentPage, String keyword, String langCategory);

    QuizPaperDetailVO getPublicDetail(Long paperId) throws StatusFailException;

    QuizPaperSubmitResultVO submitPaper(Long paperId, QuizPaperSubmitDTO dto) throws StatusFailException;

    void replacePaperItems(Long paperId, List<Long> questionIds);

    void replacePaperMixedItems(Long paperId, List<QuizPaperItemDTO> items);

    List<Long> listQuestionIdsByPaperId(Long paperId);

    List<QuizPaperItemVO> listPaperItemsByPaperId(Long paperId);
}
