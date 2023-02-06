package ru.yandex.direct.grid.processing.service.banner;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.type.href.BannersUrlHelper;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.grid.core.entity.banner.repository.GridFindAndReplaceBannerRepository;
import ru.yandex.direct.grid.core.entity.banner.service.GridFindAndReplaceBannerHrefService;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.exception.GridValidationException;
import ru.yandex.direct.grid.processing.service.validation.GridValidationResultConversionService;
import ru.yandex.direct.grid.processing.service.validation.GridValidationService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FindAndReplaceBannerHrefDomainServiceGetDomainsTest {
    private static final String ROOT_NAME = "adIds";
    private static final ClientId CLIENT_ID = ClientId.fromLong(1);
    private static final List<Long> BANNER_IDS = Arrays.asList(1L, 2L);
    private final Set<String> domains = new HashSet<>();

    @InjectMocks
    private GridValidationResultConversionService validationService;
    private FindAndReplaceBannerHrefDomainService service;

    @Autowired
    private BannersUrlHelper bannersUrlHelper;


    @Before
    public void before() {

        MockitoAnnotations.initMocks(this);
        GridFindAndReplaceBannerRepository repository = mock(GridFindAndReplaceBannerRepository.class);
        when(repository.getBannerAndSitelinksDomains(anyInt(), any(ClientId.class), any(Collection.class)))
                .thenReturn(domains);

        GridFindAndReplaceBannerHrefService gridService =
                new GridFindAndReplaceBannerHrefService(repository, null, null, mock(ShardHelper.class));

        GridValidationService gridValidationService = new GridValidationService(validationService);
        service = new FindAndReplaceBannerHrefDomainService(null, gridService, gridValidationService, null,
                bannersUrlHelper);

    }

    @Test(expected = GridValidationException.class)
    public void nullBannersTest() {
        service.getDomains(null, CLIENT_ID, ROOT_NAME);
    }

    @Test
    public void emptyResultTest() {
        domains.clear();
        Set<String> resultDomains = service.getDomains(BANNER_IDS, CLIENT_ID, ROOT_NAME);
        assertTrue("Result should be empty", resultDomains.isEmpty());
    }

    // возможно, такой ситуации когда href баннера не задан, а сайтлинк задан - не может быть
    @Test
    public void existOnlySitelinksDomainsTest() {
        Set<String> expectedDomains =
                new HashSet<>(Collections.singletonList("sitelink.domain.ru"));

        domains.clear();
        domains.add("sitelink.domain.ru");

        Set<String> resultDomains = service.getDomains(BANNER_IDS, CLIENT_ID, ROOT_NAME);
        assertEquals("Sets of domains should be equals", expectedDomains, resultDomains);
    }

    @Test
    public void manyDomainsTest() {
        Set<String> expectedDomains =
                new HashSet<>(Arrays.asList("yandex2.ru", "yandex.ru", "m.ya.ru", "test2.ru", "test.ru", "ya.ru"));

        domains.clear();
        domains.addAll(Arrays.asList("m.ya.ru", "test.ru", "yandex.ru", "ya.ru", "test2.ru", "yandex2.ru"));

        Set<String> resultDomains = service.getDomains(BANNER_IDS, CLIENT_ID, ROOT_NAME);
        assertEquals("Sets of domains should be equals", expectedDomains, resultDomains);
    }

}
