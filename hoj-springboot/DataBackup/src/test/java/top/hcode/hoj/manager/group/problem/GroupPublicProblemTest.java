package top.hcode.hoj.manager.group.problem;

import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import top.hcode.hoj.common.exception.*;
import top.hcode.hoj.dao.group.GroupEntityService;
import top.hcode.hoj.dao.problem.ProblemEntityService;
import top.hcode.hoj.pojo.dto.AddGroupProblemFromPublicDTO;
import top.hcode.hoj.pojo.entity.group.Group;
import top.hcode.hoj.pojo.entity.problem.Problem;
import top.hcode.hoj.shiro.AccountProfile;
import top.hcode.hoj.validator.GroupValidator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GroupPublicProblemTest {
    private GroupProblemManager manager;
    private ProblemEntityService problems;
    private GroupEntityService groups;
    private GroupValidator validator;
    private AddGroupProblemFromPublicDTO request;
    private Problem source;

    @BeforeEach
    void setup() {
        manager = new GroupProblemManager();
        problems = mock(ProblemEntityService.class);
        groups = mock(GroupEntityService.class);
        validator = mock(GroupValidator.class);
        ReflectionTestUtils.setField(manager, "problemEntityService", problems);
        ReflectionTestUtils.setField(manager, "groupEntityService", groups);
        ReflectionTestUtils.setField(manager, "groupValidator", validator);
        Subject subject = mock(Subject.class);
        AccountProfile profile = new AccountProfile();
        profile.setUid("user");
        profile.setUsername("teacher");
        when(subject.getPrincipal()).thenReturn(profile);
        ThreadContext.bind(subject);
        request = new AddGroupProblemFromPublicDTO();
        request.setPid(10L);
        request.setGid(20L);
        when(groups.getById(20L)).thenReturn(new Group().setStatus(0).setShortName("TEAM"));
        when(validator.isGroupAdmin("user", 20L)).thenReturn(true);
        source = new Problem().setId(10L).setProblemId("P1000").setAuth(1).setIsGroup(false).setIsRemote(false);
        when(problems.getById(10L)).thenReturn(source);
    }

    @AfterEach
    void cleanup() { ThreadContext.remove(); }

    @Test
    void teamAdminCanCopyPublicProblem() throws Exception {
        manager.addProblemFromPublic(request);
        verify(problems).copyPublicProblemToGroup(source, 20L, "TEAMP1000", "teacher");
        assertEquals(Long.valueOf(10), source.getId());
        assertEquals("P1000", source.getProblemId());
    }

    @Test
    void memberCannotCopy() {
        when(validator.isGroupAdmin("user", 20L)).thenReturn(false);
        assertThrows(StatusForbiddenException.class, () -> manager.addProblemFromPublic(request));
        verify(problems, never()).getById(any());
    }

    @Test
    void missingOrBannedGroupCannotCopy() {
        when(groups.getById(20L)).thenReturn(null);
        assertThrows(StatusNotFoundException.class, () -> manager.addProblemFromPublic(request));
        when(groups.getById(20L)).thenReturn(new Group().setStatus(1));
        assertThrows(StatusNotFoundException.class, () -> manager.addProblemFromPublic(request));
    }

    @Test
    void privateAndOtherTeamProblemsCannotBeCopied() {
        source.setAuth(2);
        assertThrows(StatusForbiddenException.class, () -> manager.addProblemFromPublic(request));
        source.setAuth(1).setIsGroup(true).setGid(30L);
        assertThrows(StatusForbiddenException.class, () -> manager.addProblemFromPublic(request));
    }

    @Test
    void missingRemoteAndDuplicateProblemsAreRejected() {
        when(problems.getById(10L)).thenReturn(null);
        assertThrows(StatusNotFoundException.class, () -> manager.addProblemFromPublic(request));
        when(problems.getById(10L)).thenReturn(source.setIsRemote(true));
        assertThrows(StatusFailException.class, () -> manager.addProblemFromPublic(request));
        source.setIsRemote(false);
        when(problems.count(any())).thenReturn(1);
        assertThrows(StatusFailException.class, () -> manager.addProblemFromPublic(request));
        verify(problems, never()).copyPublicProblemToGroup(any(), anyLong(), anyString(), anyString());
    }
}
