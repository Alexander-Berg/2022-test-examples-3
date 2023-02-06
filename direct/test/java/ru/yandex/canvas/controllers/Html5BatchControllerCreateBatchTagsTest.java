package ru.yandex.canvas.controllers;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ru.yandex.canvas.config.CanvasTest;
import ru.yandex.canvas.model.html5.Source;
import ru.yandex.canvas.repository.html5.SourcesRepository;
import ru.yandex.canvas.service.SessionParams;
import ru.yandex.canvas.service.html5.Html5BatchesService;
import ru.yandex.canvas.steps.SourceSteps;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@CanvasTest
@RunWith(Parameterized.class)
public class Html5BatchControllerCreateBatchTagsTest {
    private static final String OK_CONTENT_TEMPLATE =
            "{\"name\": \"%s\", \"sources\" : [{\"id\": \"%s\"}, {\"id\": \"%s\"}]}";
    private static final String URI = "/html5/batch";

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private MockMvc mvc;

    @SpyBean
    private Html5BatchesService batchesService;

    @Autowired
    private SessionParams sessionParams;

    @Autowired
    private SourcesRepository sourcesRepository;

    private Long clientId;

    @Parameterized.Parameter
    public String description;

    @Parameterized.Parameter(1)
    public Function<Long, Source> sourceFunction;

    @Parameterized.Parameter(2)
    public SessionParams.SessionTag sessionTag;

    @Parameterized.Parameter(3)
    public SessionParams.Html5Tag expectedHtml5tag;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(
                new Object[]{"Вызов без тегов", source(SourceSteps::defaultActiveSource), null,
                        SessionParams.Html5Tag.HTML5_CPM_BANNER},
                new Object[]{"Тег CPM", source(SourceSteps::defaultActiveSource), SessionParams.SessionTag.CPM_BANNER,
                        SessionParams.Html5Tag.HTML5_CPM_BANNER},
                new Object[]{"Тег BASE", source(SourceSteps::defaultActiveSource), SessionParams.SessionTag.CPM_BANNER,
                        SessionParams.Html5Tag.HTML5_CPM_BANNER},
                new Object[]{"Тег CPM_YNDX_FRONTPAGE", source(SourceSteps::defaultActiveCpmYndxFrontpageSource),
                        SessionParams.SessionTag.CPM_YNDX_FRONTPAGE, SessionParams.Html5Tag.HTML5_CPM_YNDX_FRONTPAGE},
                new Object[]{"Тег CPM_GEOPRODUCT", source(SourceSteps::defaultActiveCpmGeoproductSource),
                        SessionParams.SessionTag.CPM_GEOPRODUCT, SessionParams.Html5Tag.HTML5_CPM_GEOPRODUCT},
                new Object[]{"Тег MOBILE_CONTENT", source(SourceSteps::defaultActiveSource),
                        SessionParams.SessionTag.CPM_BANNER, SessionParams.Html5Tag.HTML5_CPM_BANNER}
        );
    }

    private static Function<Long, Source> source(Function<Long, Source> function) {
        return function;
    }

    @Before
    public void before() {
        clientId = RandomUtils.nextLong(0, Integer.MAX_VALUE);

        when(sessionParams.getSessionType()).thenReturn(sessionTag);
        when(sessionParams.sessionIs(sessionTag)).thenReturn(true);
        when(sessionParams.getHtml5SessionTag()).thenCallRealMethod();
        when(sessionParams.getClientId()).thenReturn(clientId);
    }

    @After
    public void after() {
        Mockito.reset(sessionParams);
    }

    @Test
    public void checkTagsProductTypeTest() throws Exception {
        Source source1 = sourceFunction.apply(clientId);
        Source source2 = sourceFunction.apply(clientId);
        sourcesRepository.insertSource(source1);
        sourcesRepository.insertSource(source2);

        String batchName = RandomStringUtils.randomAlphanumeric(32);
        String expected = String.format(OK_CONTENT_TEMPLATE, batchName, source1.getId(), source2.getId());

        MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>() {{
            add("client_id", clientId.toString());
            add("user_id", "456");
        }};

        mvc.perform(post(URI)
                .params(params)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(expected)
        ).andExpect(status().is(201));

        verify(batchesService).createBatchFromSources(eq(clientId), eq(batchName), anyList(), eq(expectedHtml5tag),
                isNull());
    }
}
