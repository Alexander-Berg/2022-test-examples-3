package ru.yandex.market.vendors.analytics.platform.facade.external;

import java.time.LocalDate;
import java.util.Optional;

import com.google.common.cache.LoadingCache;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import ru.yandex.market.vendors.analytics.core.dao.brand.BrandDao;
import ru.yandex.market.vendors.analytics.core.dao.category.CategoryDao;
import ru.yandex.market.vendors.analytics.core.dao.clickhouse.external.UnfulfilledDemandDao;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;
import ru.yandex.market.vendors.analytics.platform.controller.external.unfulfilled_demand.response.UnfulfilledDemandDetailsResponse;
import ru.yandex.market.vendors.analytics.platform.yt.YtMetaTree;
import ru.yandex.market.vendors.analytics.platform.yt.YtMetaTreeConfig;

import static org.mockito.ArgumentMatchers.any;

public class UnfulfilledDemandFacadeTest extends FunctionalTest {
    private static final String YT_KEY_ATTRIBUTE_NAME = "key";

    @Autowired
    private UnfulfilledDemandDao unfulfilledDemandDao;

    @Autowired
    private CategoryDao categoryDao;

    @Autowired
    private BrandDao brandDao;

    @Autowired
    private YtMetaTreeConfig ytMetaTreeConfig;

    @Autowired
    private YtMetaTree ytMetaTree;

    private UnfulfilledDemandFacade unfulfilledDemandFacade;

    @BeforeEach
    void init() {
        unfulfilledDemandFacade = new UnfulfilledDemandFacade(
                unfulfilledDemandDao, categoryDao, brandDao, ytMetaTreeConfig, ytMetaTree);
    }

    @Test
    @DisplayName("Проверка, что метод getDetails возвращает корректные данные при чтении нормальных данных из YT")
    public void getDetails() {
        Mockito.when(ytMetaTree.getAttributeStringValue(any(), any())).thenReturn(Optional.of("2021-08-17"));

        UnfulfilledDemandDetailsResponse details = unfulfilledDemandFacade.getDetails();

        Assertions.assertNotNull(details);
        Assertions.assertTrue(details.getActualDate().isEqual(LocalDate.of(2021, 8, 17)));
    }

    @Test
    @DisplayName("Проверка, что метод getDetails возвращает пустые данные при отсутствии данных в YT")
    void getDetailsEmpty() {
        Mockito.when(ytMetaTree.getAttributeStringValue(any(), any())).thenReturn(Optional.empty());

        UnfulfilledDemandDetailsResponse details = unfulfilledDemandFacade.getDetails();

        Assertions.assertNotNull(details);
        Assertions.assertNull(details.getActualDate());
    }

    @Test
    @DisplayName("Проверка, что в случае ошибки обращения к YT метод getDetails вернет закешированные ранее данные")
    void ytTableIsNotReachable() {
        // При втором должно быть исключение
        Mockito.when(ytMetaTree.getAttributeStringValue(any(), any()))
                .thenReturn(Optional.of("2021-08-17"))
                .thenThrow(RuntimeException.class);

        UnfulfilledDemandDetailsResponse details = unfulfilledDemandFacade.getDetails();

        Assertions.assertNotNull(details);
        Assertions.assertTrue(details.getActualDate().isEqual(LocalDate.of(2021, 8, 17)));

        // Обновить кэш, чтобы был вызов yt клиента
        //noinspection unchecked
        var cache = (LoadingCache<String, Optional<LocalDate>>)ReflectionTestUtils.getField(
                unfulfilledDemandFacade, "cache");

        Assertions.assertNotNull(cache);
        cache.refresh(YT_KEY_ATTRIBUTE_NAME);

        details = unfulfilledDemandFacade.getDetails();

        Mockito.verify(ytMetaTree, Mockito.times(2)).getAttributeStringValue(any(), any());

        // При втором вызове должно быть возвращено значение из кэша
        Assertions.assertNotNull(details);
        Assertions.assertTrue(details.getActualDate().isEqual(LocalDate.of(2021, 8, 17)));
    }
}
