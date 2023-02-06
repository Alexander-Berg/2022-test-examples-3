package ru.yandex.market.checkout.checkouter.order.getOrder;

import com.google.common.collect.Sets;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GetOrdersByColorSetTest extends AbstractWebTestBase {

    @Autowired
    private TestSerializationService testSerializationService;

    @BeforeAll
    public void setUp() {
        super.setUpBase();
        orderCreateHelper.createOrder(defaultBlueOrderParameters());

        orderCreateHelper.createOrder(WhiteParametersProvider.simpleWhiteParameters());
    }

    @AfterEach
    @Override
    public void tearDownBase() {
    }

    @AfterAll
    public void tearDownAll() {
        super.tearDownBase();
    }

    @Test
    public void shouldCountByMultipleColor() throws Exception {
        mockMvc.perform(get("/orders/count")
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                .param(CheckouterClientParams.RGB, Color.BLUE.name())
        ).andExpect(jsonPath("$.value").value(1));
    }

    @Test
    public void shouldCountByMultipleColorWithClient() throws Exception {
        OrderSearchRequest orderSearchRequest = new OrderSearchRequest();
        orderSearchRequest.setRgbs(new Color[]{Color.BLUE});

        Assertions.assertEquals(1, client.getOrdersCount(orderSearchRequest, ClientRole.SYSTEM, 0L));
    }

    @Test
    public void shouldSelectByMultipleColor() throws Exception {
        OrderSearchRequest request = new OrderSearchRequest();
        request.rgbs = Sets.newHashSet(Color.BLUE);

        mockMvc.perform(post("/get-orders")
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                .content(testSerializationService.serializeCheckouterObject(request))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(
                        jsonPath("$.orders").value(hasSize(1))
                )
                .andExpect(
                        jsonPath("$.orders[*].rgb")
                                .value(CoreMatchers.hasItems(Color.BLUE.name()))
                );
    }
}
