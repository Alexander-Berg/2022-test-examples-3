package ru.yandex.market.mbi.util.url_capacity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class UrlCapacityLimitingAspectFunctionalTest extends UrlCapacityLimitingFunctionalTest {

    @Autowired
    public UrlCapacityLimiter urlCapacityLimiter;

    @Autowired
    public EnvUrlCapacityLimitFlags urlCapacityLimitFlags;

    @BeforeEach
    public void init() {
        urlCapacityLimitFlags.reset();
    }

    @Test
    @DisplayName("Сервантлет с методом process() должен подхватиться аспектом")
    public void testBeanWithProcessMethod() {
        setFlagEnabled(true);

        FunctionalTestHelper.get(baseUrl + "/searchShops");

        verify(urlCapacityLimiter, times(1)).tryProcessOneMoreRequest(eq("searchShops"));
        verify(urlCapacityLimiter, times(1)).requestProcessed(eq("searchShops"));
    }

    @Test
    @DisplayName("CRUD-Сервантлет должен подхватиться аспектом на каждом из основных методов")
    public void testCrudServantlet() {
        setFlagEnabled(true);

        //Берем ManageShopPlacementServantlet в качестве примера
        FunctionalTestHelper.get(baseUrl + "/manageShopPlacement?a=c");
        FunctionalTestHelper.get(baseUrl + "/manageShopPlacement?a=r");
        FunctionalTestHelper.get(baseUrl + "/manageShopPlacement?a=u");
        FunctionalTestHelper.get(baseUrl + "/manageShopPlacement?a=d");

        verify(urlCapacityLimiter, times(1)).tryProcessOneMoreRequest(eq("manageShopPlacementRequest"));
        verify(urlCapacityLimiter, times(1)).requestProcessed(eq("manageShopPlacementRequest"));
        verify(urlCapacityLimiter, times(1)).tryProcessOneMoreRequest(eq("manageShopPlacementCreate"));
        verify(urlCapacityLimiter, times(1)).requestProcessed(eq("manageShopPlacementCreate"));
        verify(urlCapacityLimiter, times(1)).tryProcessOneMoreRequest(eq("manageShopPlacementUpdate"));
        verify(urlCapacityLimiter, times(1)).requestProcessed(eq("manageShopPlacementUpdate"));
        verify(urlCapacityLimiter, times(1)).tryProcessOneMoreRequest(eq("manageShopPlacementDelete"));
        verify(urlCapacityLimiter, times(1)).requestProcessed(eq("manageShopPlacementDelete"));
    }

    @Test
    @DisplayName("Сервантлет с методом processWithParams() должен быть подцеплен аспектом")
    public void testServantletWithParams() {
        setFlagEnabled(true);

        //Берем GetCampaignsBriefInfo в качестве примера
        FunctionalTestHelper.get(baseUrl + "/getCampaignsBriefInfo?");

        verify(urlCapacityLimiter, times(1))
                .tryProcessOneMoreRequest(eq("getCampaignsBriefInfo"));
        verify(urlCapacityLimiter, times(1)).requestProcessed(eq("getCampaignsBriefInfo"));
    }

    @Test
    @DisplayName("Сервантлет с методом processWithClient() должен быть подцеплен аспектом")
    public void testServantletWithClient() {
        setFlagEnabled(true);

        //Берем GetClientIds в качестве примера
        FunctionalTestHelper.get(baseUrl + "/getClientIds");

        verify(urlCapacityLimiter, times(1))
                .tryProcessOneMoreRequest("getClientIds");
        verify(urlCapacityLimiter, times(1)).requestProcessed(eq("getClientIds"));
    }

}
