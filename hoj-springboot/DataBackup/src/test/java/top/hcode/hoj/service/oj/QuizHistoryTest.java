package top.hcode.hoj.service.oj;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.mapper.QuizAttemptMapper;
import top.hcode.hoj.mapper.QuizQuestionMapper;
import top.hcode.hoj.pojo.entity.quiz.QuizAttempt;
import top.hcode.hoj.pojo.entity.quiz.QuizQuestion;
import top.hcode.hoj.pojo.vo.QuizSubmitResultVO;
import top.hcode.hoj.shiro.AccountProfile;
import java.util.Date;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QuizHistoryTest {
    QuizHistoryService service;
    QuizAttemptMapper mapper;
    QuizQuestionMapper questionMapper;
    Subject subject;
    @BeforeEach void setup() {
        service=new QuizHistoryService(); mapper=mock(QuizAttemptMapper.class);
        ReflectionTestUtils.setField(service,"mapper",mapper);
        questionMapper = mock(QuizQuestionMapper.class);
        ReflectionTestUtils.setField(service,"questionMapper",questionMapper);
        subject=mock(Subject.class); AccountProfile profile=new AccountProfile(); profile.setUid("alice");
        when(subject.getPrincipal()).thenReturn(profile); ThreadContext.bind(subject);
    }
    @AfterEach void cleanup() { ThreadContext.remove(); }
    @Test void snapshotIsPersistedWithCurrentOwnerAndImmutableAnswers() throws Exception {
        when(mapper.insert(any())).thenAnswer(call->{QuizAttempt row=call.getArgument(0);row.setId(99L);return 1;});
        QuizSubmitResultVO result=new QuizSubmitResultVO();result.setUserAnswer("A");result.setCorrectAnswer("B");result.setExplanation("original explanation");
        assertEquals(99L,service.record("quiz",1L,"Title",0,1,0,1,result));
        result.setCorrectAnswer("C");result.setExplanation("edited explanation");
        ArgumentCaptor<QuizAttempt> capture=ArgumentCaptor.forClass(QuizAttempt.class);verify(mapper).insert(capture.capture());
        QuizAttempt stored=capture.getValue();assertEquals("alice",stored.getUid());
        assertTrue(stored.getResultJson().contains("original explanation"));assertFalse(stored.getResultJson().contains("edited explanation"));
        assertTrue(stored.getResultJson().contains("\"correctAnswer\":\"B\""));
    }
    @Test void listSelectsSummariesForCurrentOwnerAndCapsPageSize() throws Exception {
        assertEquals(100,service.history(-2,10000,"paper",2L).getSize());
        ArgumentCaptor<QueryWrapper> capture=ArgumentCaptor.forClass(QueryWrapper.class);verify(mapper).selectPage(any(),capture.capture());
        QueryWrapper query=capture.getValue();assertTrue(query.getSqlSegment().contains("uid"));assertTrue(query.getParamNameValuePairs().containsValue("alice"));
        assertFalse(query.getSqlSelect().contains("result_json"));assertFalse(query.getSqlSelect().contains("uid"));
    }
    @Test void detailQueryIsRestrictedToCurrentOwner() {
        assertThrows(StatusFailException.class,()->service.detail(99L));
        ArgumentCaptor<QueryWrapper> capture=ArgumentCaptor.forClass(QueryWrapper.class);verify(mapper).selectOne(capture.capture());
        assertTrue(capture.getValue().getSqlSegment().contains("uid"));assertTrue(capture.getValue().getParamNameValuePairs().containsValue("alice"));
        verifyNoInteractions(questionMapper);
    }
    @Test void storedSnapshotReviewAddsResourceAndAttemptMetadata() throws Exception {
        QuizAttempt row=new QuizAttempt();row.setId(99L);row.setKind("quiz");row.setResourceId(3L);row.setGmtCreate(new Date());row.setResultJson("{\"correctAnswer\":\"B\",\"explanation\":\"original\"}");
        when(mapper.selectOne(any())).thenReturn(row);
        cn.hutool.json.JSONObject review=service.detail(99L);assertEquals("B",review.getStr("correctAnswer"));assertEquals(99L,review.getLong("attemptId"));assertEquals(3L,review.getLong("resourceId"));
    }
    @Test void anonymousUserCannotReadOrSaveAttempts() {
        when(subject.getPrincipal()).thenReturn(null);
        assertThrows(StatusFailException.class,()->service.history(1,20,null,null));assertThrows(StatusFailException.class,()->service.detail(1L));
        assertThrows(StatusFailException.class,()->service.record("quiz",1L,"t",0,1,0,1,new Object()));verifyNoInteractions(mapper);
    }
    @Test void singleReviewUsesLatestExplanationWithoutChangingAnswersOrStoredSnapshot() throws Exception {
        QuizAttempt row=new QuizAttempt();row.setId(99L);row.setKind("quiz");row.setResourceId(3L);
        row.setResultJson("{\"correctAnswer\":\"B\",\"score\":0,\"explanation\":\"original\"}");
        when(mapper.selectOne(any())).thenReturn(row);
        when(questionMapper.selectList(any())).thenReturn(Collections.singletonList(new QuizQuestion().setId(3L).setExplanation("补录解析")));
        cn.hutool.json.JSONObject review=service.detail(99L);
        assertEquals("补录解析",review.getStr("explanation"));assertEquals("B",review.getStr("correctAnswer"));assertEquals(0,review.getInt("score"));
        assertTrue(row.getResultJson().contains("original"));
        ArgumentCaptor<QueryWrapper> query=ArgumentCaptor.forClass(QueryWrapper.class);verify(questionMapper).selectList(query.capture());
        assertEquals("id,explanation",query.getValue().getSqlSelect());
        verify(mapper,never()).updateById(any());
    }
    @Test void paperReviewRefreshesBothFormatsSkipsProgrammingAndKeepsDeletedQuestionSnapshot() throws Exception {
        QuizAttempt row=new QuizAttempt();row.setId(99L);row.setKind("paper");row.setResourceId(20L);
        row.setResultJson("{\"itemResults\":[{\"itemType\":\"quiz\",\"questionId\":3,\"explanation\":\"old\"},{\"itemType\":\"quiz\",\"questionId\":4,\"explanation\":\"deleted\"},{\"itemType\":\"problem\",\"questionId\":5,\"explanation\":\"code\"}],\"questionResults\":[{\"questionId\":3,\"explanation\":\"old\"}]}");
        when(mapper.selectOne(any())).thenReturn(row);
        when(questionMapper.selectList(any())).thenReturn(Arrays.asList(new QuizQuestion().setId(3L).setExplanation("补录")));
        cn.hutool.json.JSONObject review=service.detail(99L);
        assertEquals("补录",review.getJSONArray("itemResults").getJSONObject(0).getStr("explanation"));
        assertEquals("补录",review.getJSONArray("questionResults").getJSONObject(0).getStr("explanation"));
        assertEquals("deleted",review.getJSONArray("itemResults").getJSONObject(1).getStr("explanation"));
        assertEquals("code",review.getJSONArray("itemResults").getJSONObject(2).getStr("explanation"));
        verify(questionMapper,times(1)).selectList(any());
        when(questionMapper.selectList(any())).thenReturn(Collections.singletonList(new QuizQuestion().setId(3L).setExplanation(null)));
        assertEquals("",service.detail(99L).getJSONArray("itemResults").getJSONObject(0).getStr("explanation"));
    }
    @Test void failedInsertDoesNotReportSuccessfulAttempt() {
        assertThrows(StatusFailException.class,()->service.record("quiz",1L,"t",0,1,0,1,new Object()));
    }
}
