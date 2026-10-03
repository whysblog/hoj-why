package top.hcode.hoj.service.oj;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.mapper.QuizAttemptMapper;
import top.hcode.hoj.pojo.entity.quiz.QuizAttempt;
import top.hcode.hoj.pojo.vo.QuizSubmitResultVO;
import top.hcode.hoj.shiro.AccountProfile;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QuizHistoryTest {
    QuizHistoryService service;
    QuizAttemptMapper mapper;
    Subject subject;
    @BeforeEach void setup() {
        service=new QuizHistoryService(); mapper=mock(QuizAttemptMapper.class);
        ReflectionTestUtils.setField(service,"mapper",mapper);
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
    @Test void failedInsertDoesNotReportSuccessfulAttempt() {
        assertThrows(StatusFailException.class,()->service.record("quiz",1L,"t",0,1,0,1,new Object()));
    }
}
