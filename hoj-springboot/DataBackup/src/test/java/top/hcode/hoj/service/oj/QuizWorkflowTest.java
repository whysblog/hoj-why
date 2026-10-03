package top.hcode.hoj.service.oj;

import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.dao.judge.JudgeEntityService;
import top.hcode.hoj.mapper.*;
import top.hcode.hoj.pojo.dto.*;
import top.hcode.hoj.pojo.entity.judge.Judge;
import top.hcode.hoj.pojo.entity.problem.Problem;
import top.hcode.hoj.pojo.entity.quiz.*;
import top.hcode.hoj.pojo.vo.*;
import top.hcode.hoj.service.oj.impl.*;
import top.hcode.hoj.shiro.AccountProfile;
import top.hcode.hoj.utils.QuizAnswerUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QuizWorkflowTest {
    QuizPaperServiceImpl papers;
    QuizQuestionServiceImpl questions;
    QuizPaperItemMapper items;
    ProblemMapper problems;
    JudgeEntityService judges;
    QuizHistoryService history;
    QuizQuestion question;
    QuizPaper paper;
    QuizPaperSubmitDTO request;

    @BeforeEach void setup() {
        questions = spy(new QuizQuestionServiceImpl());
        papers = spy(new QuizPaperServiceImpl());
        items = mock(QuizPaperItemMapper.class);
        problems = mock(ProblemMapper.class);
        judges = mock(JudgeEntityService.class);
        history = mock(QuizHistoryService.class);
        ReflectionTestUtils.setField(questions,"historyService",history);
        ReflectionTestUtils.setField(papers,"historyService",history);
        ReflectionTestUtils.setField(papers,"quizQuestionService",questions);
        ReflectionTestUtils.setField(papers,"quizPaperItemMapper",items);
        ReflectionTestUtils.setField(papers,"problemMapper",problems);
        ReflectionTestUtils.setField(papers,"judgeEntityService",judges);
        AccountProfile user = new AccountProfile(); user.setUid("alice");
        Subject subject = mock(Subject.class); when(subject.getPrincipal()).thenReturn(user); ThreadContext.bind(subject);
        question = new QuizQuestion().setId(10L).setTitle("Question").setStatus(1).setQuestionType(0)
            .setOptionA("one").setOptionB("two").setOptionC("three").setOptionD("four").setAnswer("A").setExplanation("why");
        doReturn(question).when(questions).getOne(any());
        doReturn(question).when(questions).getById(10L);
        paper = new QuizPaper().setId(20L).setTitle("Paper").setStatus(1);
        doReturn(paper).when(papers).getOne(any()); doReturn(paper).when(papers).getById(20L);
        request = new QuizPaperSubmitDTO(); request.setAnswers(new HashMap<>());
    }
    @AfterEach void cleanup() { ThreadContext.remove(); }

    QuizPaperItem item(String type, long id, int score) {
        return new QuizPaperItem().setPaperId(20L).setQuestionId(id).setItemType(type).setScore(score);
    }
    void paperItems(QuizPaperItem... rows) { when(items.selectList(any())).thenReturn(Arrays.asList(rows)); }

    @Test void normalizationRejectsGarbageAndSupportsOrder() {
        assertEquals("ABC",QuizAnswerUtils.normalize("c, b，a"));
        assertEquals("",QuizAnswerUtils.normalize("Agarbage"));
        assertEquals("",QuizAnswerUtils.normalize("A<script>"));
        assertEquals("",QuizAnswerUtils.normalize("E"));
        assertFalse(QuizAnswerUtils.isValidMultiple("AA"));
    }
    @Test void publicInfoHasNoAnswerAndSingleSubmissionHasSnapshot() throws Exception {
        QuizQuestionInfoVO info = questions.getPublicInfo(10L);
        assertFalse(cn.hutool.json.JSONUtil.toJsonStr(info).contains("correctAnswer"));
        QuizSubmitResultVO result = questions.submitAnswer(10L,"a");
        assertTrue(result.getCorrect()); assertEquals(1,result.getScore());
        assertEquals("why", result.getExplanation());
        assertEquals("why", cn.hutool.json.JSONUtil.parseObj(cn.hutool.json.JSONUtil.toJsonStr(result)).getStr("explanation"));
        assertEquals("one",result.getQuestion().getOptions().get(0).getText());
        verify(history).record(eq("quiz"),eq(10L),eq("Question"),eq(1),eq(1),eq(1),eq(1),same(result));
    }
    @Test void multiPartialIsWrongAndGarbageFailsBeforeRecording() throws Exception {
        question.setQuestionType(1).setAnswer("BA");
        assertFalse(questions.submitAnswer(10L,"A").getCorrect());
        assertTrue(questions.submitAnswer(10L,"B,A").getCorrect());
        assertThrows(StatusFailException.class,()->questions.submitAnswer(10L,"Agarbage"));
        verify(history,times(2)).record(anyString(),anyLong(),anyString(),anyInt(),anyInt(),anyInt(),anyInt(),any());
    }
    @Test void hiddenOrMissingQuestionIsRejected() {
        doReturn(null).when(questions).getOne(any());
        assertThrows(StatusFailException.class,()->questions.getPublicInfo(10L));
        assertThrows(StatusFailException.class,()->questions.submitAnswer(10L,"A"));
        verifyNoInteractions(history);
    }
    @Test void configuredObjectiveScoreAndUnansweredAreRespected() throws Exception {
        paperItems(item("quiz",10,35)); request.getAnswers().put("10","A");
        QuizPaperSubmitResultVO result = papers.submitPaper(20L,request);
        assertEquals(35,result.getScore()); assertEquals(35,result.getMaxScore()); assertEquals(1,result.getCorrectCount());
        request.getAnswers().clear(); result=papers.submitPaper(20L,request);
        assertEquals(0,result.getScore()); assertEquals(1,result.getUnansweredCount());
        assertEquals("one",result.getItemResults().get(0).getQuestion().getOptions().get(0).getText());
    }
    @Test void malformedPaperAnswerDoesNotCreateAttempt() {
        paperItems(item("quiz",10,35)); request.getAnswers().put("10","Agarbage");
        assertThrows(StatusFailException.class,()->papers.submitPaper(20L,request)); verifyNoInteractions(history);
    }
    @Test void paperExplanationSurvivesBothResultFormatsAndHistorySerialization() throws Exception {
        question.setExplanation("**解析**：选择 A。\n第二行");
        paperItems(item("quiz",10,35));
        for (String answer : Arrays.asList("A", "B", "")) {
            request.getAnswers().put("10", answer);
            QuizPaperSubmitResultVO result = papers.submitPaper(20L, request);
            assertEquals(question.getExplanation(), result.getItemResults().get(0).getExplanation());
            assertEquals(question.getExplanation(), result.getQuestionResults().get(0).getExplanation());
            cn.hutool.json.JSONObject snapshot = cn.hutool.json.JSONUtil.parseObj(cn.hutool.json.JSONUtil.toJsonStr(result));
            assertEquals(question.getExplanation(), snapshot.getJSONArray("itemResults").getJSONObject(0).getStr("explanation"));
            assertEquals(question.getExplanation(), snapshot.getJSONArray("questionResults").getJSONObject(0).getStr("explanation"));
        }
    }
    @Test void paperMultiPartialIsWrongRatherThanUnanswered() throws Exception {
        question.setQuestionType(1).setAnswer("AB"); paperItems(item("quiz",10,25)); request.getAnswers().put("10","A");
        QuizPaperSubmitResultVO result=papers.submitPaper(20L,request);
        assertEquals(1,result.getWrongCount()); assertEquals(0,result.getUnansweredCount()); assertEquals(0,result.getScore());
    }
    Judge programmingSubmission(int status,int rawScore) {
        paperItems(item("problem",30,40));
        when(problems.selectById(30L)).thenReturn(new Problem().setId(30L).setAuth(1).setIsGroup(false).setTitle("Code").setType(1).setIoScore(200));
        QuizPaperSubmitDTO.ProblemSnapshotDTO snap=new QuizPaperSubmitDTO.ProblemSnapshotDTO();
        snap.setSubmitId(50L); snap.setScore(999); snap.setStatus(0);
        request.setProblemSnapshots(Collections.singletonMap("30",snap));
        Judge judge=new Judge().setSubmitId(50L).setUid("alice").setPid(30L).setCid(0L).setStatus(status).setScore(rawScore).setLanguage("C++");
        when(judges.getById(50L)).thenReturn(judge); return judge;
    }
    @Test void programmingUsesServerRecordAndScalesOiScore() throws Exception {
        programmingSubmission(1,100);
        QuizPaperSubmitResultVO result=papers.submitPaper(20L,request);
        assertEquals(20,result.getScore()); assertEquals(40,result.getMaxScore()); assertEquals(50L,result.getItemResults().get(0).getSubmitId());
        programmingSubmission(0,0); assertEquals(40,papers.submitPaper(20L,request).getScore());
    }
    @Test void programmingScoreIsBounded() throws Exception {
        programmingSubmission(1,999); assertEquals(40,papers.submitPaper(20L,request).getScore());
        programmingSubmission(1,-100); assertEquals(0,papers.submitPaper(20L,request).getScore());
    }
    @Test void forgedOwnerContestAndPendingSubmissionsFail() {
        Judge judge=programmingSubmission(0,200).setUid("bob");
        assertThrows(StatusFailException.class,()->papers.submitPaper(20L,request));
        judge.setUid("alice").setCid(2L); assertThrows(StatusFailException.class,()->papers.submitPaper(20L,request));
        judge.setCid(0L).setStatus(5); assertThrows(StatusFailException.class,()->papers.submitPaper(20L,request));
        verifyNoInteractions(history);
    }
    @Test void invalidItemsAreValidatedBeforeDeletingExistingOrder() {
        QuizPaperItemDTO row=new QuizPaperItemDTO();row.setItemType("quiz");row.setQuestionId(10L);row.setScore(25);
        assertThrows(StatusFailException.class,()->papers.replacePaperMixedItems(20L,Arrays.asList(row,row)));
        row.setScore(-1); assertThrows(StatusFailException.class,()->papers.replacePaperMixedItems(20L,Collections.singletonList(row)));
        verify(items,never()).delete(any()); verify(items,never()).insert(any());
    }
    @Test void emptyPublicPaperCannotSaveButEmptyDraftCan() throws Exception {
        assertThrows(StatusFailException.class,()->papers.replacePaperMixedItems(20L,Collections.emptyList()));
        paper.setStatus(0); papers.replacePaperMixedItems(20L,Collections.emptyList()); verify(items).delete(any());
    }

    @Test void completeSaveValidatesBeforeWritingPaperMetadata() {
        QuizPaperSaveDTO dto=new QuizPaperSaveDTO();dto.setPaper(paper);dto.setItems(Collections.emptyList());
        assertThrows(StatusFailException.class,()->papers.savePaperWithItems(dto));
        verify(papers,never()).saveOrUpdate(any(QuizPaper.class));verify(items,never()).delete(any());
    }
    @Test void completeSaveKeepsOrderAndZeroPointItems() throws Exception {
        QuizPaperItemDTO first=new QuizPaperItemDTO();first.setQuestionId(10L);first.setItemType("quiz");first.setScore(0);
        QuizPaperSaveDTO dto=new QuizPaperSaveDTO();dto.setPaper(paper);dto.setItems(Collections.singletonList(first));
        doReturn(true).when(papers).saveOrUpdate(any(QuizPaper.class));when(items.insert(any())).thenReturn(1);
        assertEquals(20L,papers.savePaperWithItems(dto));
        org.mockito.ArgumentCaptor<QuizPaperItem> capture=org.mockito.ArgumentCaptor.forClass(QuizPaperItem.class);
        verify(items).insert(capture.capture());assertEquals(0,capture.getValue().getScore());assertEquals(0,capture.getValue().getSortOrder());
    }
    @Test void hiddenQuestionCannotBePublishedAndMissingItemCannotDisappear() {
        question.setStatus(0);QuizPaperItemDTO row=new QuizPaperItemDTO();row.setQuestionId(10L);row.setItemType("quiz");row.setScore(25);
        assertThrows(StatusFailException.class,()->papers.replacePaperMixedItems(20L,Collections.singletonList(row)));
        verify(items,never()).delete(any());doReturn(null).when(questions).getOne(any());paperItems(item("quiz",10,25));
        assertThrows(StatusFailException.class,()->papers.getPublicDetail(20L));
    }
    @Test void submittingStatusCannotBeRecordedAsFinalGrade() {
        programmingSubmission(9,200);assertThrows(StatusFailException.class,()->papers.submitPaper(20L,request));verifyNoInteractions(history);
    }
}
