package top.hcode.hoj.dao.problem;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import top.hcode.hoj.dao.problem.impl.ProblemEntityServiceImpl;
import top.hcode.hoj.pojo.entity.problem.*;
import top.hcode.hoj.utils.Constants;

import java.io.File;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PublicProblemCopyTest {
    private ProblemEntityServiceImpl service;
    private ProblemCaseEntityService cases;
    private CodeTemplateEntityService templates;
    private ProblemLanguageEntityService languages;
    private ProblemTagEntityService tags;
    private Problem source;
    private final Long sourcePid = 98765432101L;
    private final Long copyPid = 98765432102L;
    private String folder(Long pid) {
        return Constants.File.TESTCASE_BASE_FOLDER.getPath() + File.separator + "problem_" + pid;
    }

    @BeforeEach
    void setup() {
        service = spy(new ProblemEntityServiceImpl());
        cases = mock(ProblemCaseEntityService.class);
        templates = mock(CodeTemplateEntityService.class);
        languages = mock(ProblemLanguageEntityService.class);
        tags = mock(ProblemTagEntityService.class);
        ReflectionTestUtils.setField(service, "problemCaseEntityService", cases);
        ReflectionTestUtils.setField(service, "codeTemplateEntityService", templates);
        ReflectionTestUtils.setField(service, "problemLanguageEntityService", languages);
        ReflectionTestUtils.setField(service, "problemTagEntityService", tags);
        when(tags.list(any())).thenReturn(Collections.emptyList());
        when(languages.list(any())).thenReturn(Collections.emptyList());
        when(templates.list(any())).thenReturn(Collections.emptyList());
        when(cases.list(any())).thenReturn(new ArrayList<>(Collections.singletonList(
                new ProblemCase().setId(100L).setPid(sourcePid).setInput("2").setOutput("4").setScore(100).setGroupNum(1))));
        when(cases.saveBatch(anyCollection())).thenAnswer(call -> {
            Collection<ProblemCase> values = call.getArgument(0);
            for (ProblemCase value : values) {
                assertNull(value.getId());
                assertEquals(copyPid, value.getPid());
                value.setId(200L);
            }
            return true;
        });
        doAnswer(call -> {
            Problem problem = call.getArgument(0);
            assertNull(problem.getId());
            problem.setId(copyPid);
            return true;
        }).when(service).save(any(Problem.class));
        source = new Problem().setId(sourcePid).setProblemId("P1000").setTitle("Square")
                .setIsUploadCase(false).setJudgeMode("default").setJudgeCaseMode("default")
                .setAuthor("original").setIsGroup(false).setCaseVersion("old");
    }

    @AfterEach
    void cleanup() { FileUtil.del(folder(sourcePid)); FileUtil.del(folder(copyPid)); }

    @Test
    void handCasesGetIndependentIdsAndMetadata() {
        service.copyPublicProblemToGroup(source, 20L, "TEAMP1000", "teacher");
        assertEquals(sourcePid, source.getId());
        assertEquals("original", source.getAuthor());
        assertEquals("P1000", source.getProblemId());
        assertFalse(source.getIsGroup());
        assertEquals("4", FileUtil.readUtf8String(folder(copyPid) + "/1.out"));
        assertEquals(Long.valueOf(200), JSONUtil.parseObj(FileUtil.readUtf8String(folder(copyPid) + "/info"))
                .getJSONArray("testCases").getJSONObject(0).getLong("caseId"));
    }

    @Test
    void uploadedFilesAreCopiedAndSourceIsPreserved() {
        source.setIsUploadCase(true);
        FileUtil.writeUtf8String("2\n", folder(sourcePid) + "/a.in");
        FileUtil.writeUtf8String("4\n", folder(sourcePid) + "/a.out");
        FileUtil.writeUtf8String("original metadata", folder(sourcePid) + "/info");
        when(cases.list(any())).thenReturn(new ArrayList<>(Collections.singletonList(
                new ProblemCase().setId(100L).setPid(sourcePid).setInput("a.in").setOutput("a.out").setScore(100))));
        service.copyPublicProblemToGroup(source, 20L, "TEAMP1000", "teacher");
        assertEquals("4\n", FileUtil.readUtf8String(folder(copyPid) + "/a.out"));
        assertEquals("original metadata", FileUtil.readUtf8String(folder(sourcePid) + "/info"));
        assertEquals(Long.valueOf(200), JSONUtil.parseObj(FileUtil.readUtf8String(folder(copyPid) + "/info"))
                .getJSONArray("testCases").getJSONObject(0).getLong("caseId"));
    }

    @Test
    void templatesAndLanguagesAreInsertedWithNewIds() {
        when(languages.list(any())).thenReturn(new ArrayList<>(Collections.singletonList(
                new ProblemLanguage().setId(1L).setPid(sourcePid).setLid(10L))));
        when(templates.list(any())).thenReturn(new ArrayList<>(Collections.singletonList(
                new CodeTemplate().setId(2).setPid(sourcePid).setLid(10L).setCode("int main() {}"))));
        when(languages.saveBatch(anyCollection())).thenAnswer(call -> {
            ProblemLanguage value = ((Collection<ProblemLanguage>) call.getArgument(0)).iterator().next();
            assertNull(value.getId()); assertEquals(copyPid, value.getPid()); assertEquals(Long.valueOf(10), value.getLid());
            return true;
        });
        when(templates.saveBatch(anyCollection())).thenAnswer(call -> {
            CodeTemplate value = ((Collection<CodeTemplate>) call.getArgument(0)).iterator().next();
            assertNull(value.getId()); assertEquals(copyPid, value.getPid()); assertEquals("int main() {}", value.getCode());
            return true;
        });
        service.copyPublicProblemToGroup(source, 20L, "TEAMP1000", "teacher");
        verify(templates).saveBatch(anyCollection());
        verify(languages).saveBatch(anyCollection());
    }

    @Test
    void publicTagsAreMappedToTeamTags() {
        TagEntityService tagService = mock(TagEntityService.class);
        ReflectionTestUtils.setField(service, "tagEntityService", tagService);
        when(tags.list(any())).thenReturn(new ArrayList<>(Collections.singletonList(
                new ProblemTag().setId(1L).setPid(sourcePid).setTid(10L))));
        when(tagService.getById(10L)).thenReturn(new Tag().setId(10L).setName("DP").setColor("blue"));
        when(tagService.getOne(any(), eq(false))).thenReturn(new Tag().setId(30L).setGid(20L).setName("DP"));
        when(tags.saveBatch(anyCollection())).thenAnswer(call -> {
            ProblemTag relation = ((Collection<ProblemTag>) call.getArgument(0)).iterator().next();
            assertNull(relation.getId());
            assertEquals(copyPid, relation.getPid());
            assertEquals(Long.valueOf(30), relation.getTid());
            return true;
        });
        service.copyPublicProblemToGroup(source, 20L, "TEAMP1000", "teacher");
        verify(tags).saveBatch(anyCollection());
    }

    @Test
    void missingUploadDataFailsAndCleansTarget() {
        source.setIsUploadCase(true);
        assertThrows(IllegalStateException.class, () -> service.copyPublicProblemToGroup(source, 20L, "TEAMP1000", "teacher"));
        assertFalse(FileUtil.exist(folder(copyPid)));
    }
}
