package ru.yandex.market.mbo.cms.tms.executors.signal.executor;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.cms.core.models.CmsPageSort;
import ru.yandex.market.mbo.cms.core.models.CmsPagesFilter;
import ru.yandex.market.mbo.cms.core.models.Page;
import ru.yandex.market.mbo.cms.core.service.CmsService;
import ru.yandex.market.mbo.cms.signal.model.Signal;
import ru.yandex.market.mbo.cms.signal.model.SignalFilter;
import ru.yandex.market.mbo.cms.signal.model.SignalSet;
import ru.yandex.market.mbo.cms.signal.model.SignalType;
import ru.yandex.market.mbo.cms.signal.service.SignalStorageService;

public class CmsPageSignalGenerateExecutorTest {

    private static final String PAGE_ID = "pageId";
    private static final int ID1 = 1;
    private static final int ID2 = 2;
    private SignalSet signalSet;

    @Test
    public void testDoRealJob() throws Exception {
        SignalStorageService signalStorageService = signalStorageServiceMock();
        CmsService cmsService = cmsServiceMock();

        CmsPageSignalGenerateExecutor cmsPageSignalGenerateExecutor =
            new CmsPageSignalGenerateExecutor(signalStorageService, cmsService);
        cmsPageSignalGenerateExecutor.addPlugin(plugin());

        cmsPageSignalGenerateExecutor.doRealJob(null);

        Assert.assertEquals(2, signalSet.getSignals().size());
        Assert.assertEquals(String.valueOf(ID1), signalSet.getSignals().get(0).getData().get(PAGE_ID));
        Assert.assertEquals(String.valueOf(ID2), signalSet.getSignals().get(1).getData().get(PAGE_ID));
    }

    CmsPageSignalGenerateExecutorPlugin plugin() {
        return new CmsPageSignalGenerateExecutorPlugin() {
            @Override
            public void init() {

            }

            @Nonnull
            @Override
            public List<Signal> checkPage(Page page) {
                return Collections.singletonList(
                    new Signal(
                        new Date(),
                        SignalType.PAGE_DELETED,
                        Collections.singletonMap(PAGE_ID, String.valueOf(page.getId()))
                    )
                );
            }

            @Override
            public void finish() {

            }
        };
    }

    SignalStorageService signalStorageServiceMock() {
        return new SignalStorageService() {
            @Override
            public void add(SignalSet signals) {
                signalSet = signals;
            }

            @Override
            public void find(SignalFilter filter, Consumer<Signal> consumer) {

            }

            @Override
            public SignalSet find(SignalFilter filter) {
                return null;
            }

            @Override
            public Integer getCount(SignalFilter filter) {
                throw new UnsupportedOperationException("");
            }
        };
    }

    CmsService cmsServiceMock() {
        Page page1 = new Page();
        Page page2 = new Page();
        page1.setId(ID1);
        page2.setId(ID2);
        Map<Long, Page> pages = new HashMap<>();
        pages.put(page1.getId(), page1);
        pages.put(page2.getId(), page2);
        CmsService cmsService = Mockito.mock(CmsService.class);
        Mockito
            .doAnswer(
                invocation -> {
                    Consumer<Page> consumer = (Consumer<Page>) invocation.getArguments()[2];
                    consumer.accept(page1);
                    consumer.accept(page2);
                    return null;
                }
            )
            .when(cmsService)
            .foreachPages(
                Mockito.any(CmsPagesFilter.class),
                Mockito.any(CmsPageSort.class),
                Mockito.any(Consumer.class)
            );
        Mockito
            .doAnswer(
                invocation -> {
                    Long pageId = (Long) invocation.getArguments()[0];
                    return pages.get(pageId);
                }
            )
            .when(cmsService)
            .findPromoPagesById(Mockito.anyLong(), Mockito.anyLong());
        return cmsService;
    }
}
