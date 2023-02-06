package ru.yandex.direct.core.entity.banner.type.href;

import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects;
import ru.yandex.direct.core.entity.uac.service.trackingurl.TrackingUrlParseService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.validation.result.Defect;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static ru.yandex.direct.core.entity.banner.type.href.BannerWithHrefConstraints.validTrackingSystem;

@CoreTest
@RunWith(Parameterized.class)
public class BannerWithHrefConstraintsTrackingSystemTest {

    @Autowired
    private TrackingUrlParseService trackingUrlParseService;

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public String href;

    @Parameterized.Parameter(2)
    public Defect<String> expectedDefect;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {"url не относится к трекинговой системе", "https://play.google.com/store/apps/details?id=test", BannerDefects.trackingSystemDomainNotSupported()},
                {"url не относится к трекинговой системе", "https://apps.apple.com/ru/id123", BannerDefects.trackingSystemDomainNotSupported()},
                {"url от adjust трекинговой системы", "https://app.adjust.com/test", null},
                {"url от appsflyer трекинговой системы", "https://app.appsflyer.com/ru", null},
                {"url от mytracker трекинговой системы", "https://trk.mail.ru/c/1", null}
        });
    }

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
    }

    @Test
    public void testParametrized() {
        assertThat(validTrackingSystem(trackingUrlParseService).apply(href), is(expectedDefect));
    }
}
