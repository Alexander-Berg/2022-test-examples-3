package ru.yandex.market.vendors.analytics.platform.facade.external;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;
import ru.yandex.market.vendors.analytics.platform.yt.YtMetaTree;
import ru.yandex.market.vendors.analytics.platform.yt.YtMetaTreeConfig;

import static org.mockito.ArgumentMatchers.any;

public class ReferencePricesFacadeTest extends FunctionalTest {

    @Autowired
    private YtMetaTreeConfig ytMetaTreeConfig;

    @Autowired
    private YtMetaTree ytMetaTree;

    private ReferencePricesFacade referencePricesFacade;

    @BeforeEach
    void init() {
        referencePricesFacade = new ReferencePricesFacade(ytMetaTreeConfig, ytMetaTree);
    }

    @Test
    @DisplayName("Проверка, что метод getLastReferencePricesTime возвращает корректные данные при чтении нормальных данных из YT")
    public void getLastReferencePricesTime() {
        Mockito.when(ytMetaTree.getAttributeStringValue(any(), any())).thenReturn(Optional.of("20220704_1600"));

        LocalDateTime date = referencePricesFacade.getLastReferencePricesTime();

        Assertions.assertNotNull(date);
        Assertions.assertTrue(date.isEqual(LocalDateTime.of(2022, 7, 4, 16, 0)));
    }
}
