package ru.yandex.market.crm.triggers.services.bpm.messages;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames;
import ru.yandex.market.crm.core.services.phone.PhoneService;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;
import ru.yandex.market.crm.triggers.services.bpm.UidBpmMessage;
import ru.yandex.market.crm.triggers.services.order.BusinessOrderInfoProvider;
import ru.yandex.market.crm.triggers.services.order.OrderInfoProvider;
import ru.yandex.market.sdk.userinfo.service.NoSideEffectUserService;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author phiyaomsk
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {
        BusinessOrderEventBpmMessageFactoryTestBase.TestConfiguration.class,
        BusinessOrderEventBpmMessageFactory.class
})
public abstract class BusinessOrderEventBpmMessageFactoryTestBase {

    protected static final String CLIENT_NAME = "Full Name";

    @Configuration
    @ImportResource("classpath:/WEB-INF/checkouter-client.xml")
    static class TestConfiguration {
        @Bean
        public NoSideEffectUserService noSideEffectUserService() {
            return Mockito.mock(NoSideEffectUserService.class);
        }

        @Bean
        public BusinessOrderInfoProvider orderInfoProvider() {
            return Mockito.mock(BusinessOrderInfoProvider.class);
        }

        @Bean
        public PhoneService phoneService() {
            return Mockito.mock(PhoneService.class);
        }
    }

    @Inject
    protected BusinessOrderInfoProvider businessOrderInfoProvider;

    @Before
    public void setUp() {
        Mockito.when(businessOrderInfoProvider.getFullNameForOrder(Mockito.any(), Mockito.any()))
                .thenReturn(CLIENT_NAME);
    }

    @Inject
    protected BusinessOrderEventBpmMessageFactory factory;

    @Inject
    @Named("checkouterAnnotationObjectMapper")
    private ObjectMapper objectMapper;

    @Test
    public void shouldNotCreateBpmMessageIfOrderNotExists() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        event.setOrderAfter(null);

        List<UidBpmMessage> messages = factory.from(event);

        assertTrue(messages.isEmpty());
    }

    @Test
    public void shouldNotCreateBpmMessageIfBuyerNotExists() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        event.getOrderAfter().setBuyer(null);

        List<UidBpmMessage> messages = factory.from(event);

        assertTrue(messages.isEmpty());
    }

    @Test
    public void shouldCreateBpmMessageWithEmailUidIfMuidExists() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        event.getOrderAfter().getBuyer().setMuid(123L);

        List<UidBpmMessage> messages = factory.from(event);

        assertThat(messages.size(), equalTo(1));
        assertThat(messages.get(0), instanceOf(UidBpmMessage.class));

        UidBpmMessage message = messages.get(0);

        assertThat(message.getUid().getType(), is(UidType.EMAIL));
        assertThat(message.getUid().getValue(), equalTo(String.valueOf(event.getOrderAfter().getBuyer().getEmail())));
    }

    @Test
    public void shouldNotCreateBpmMessageIfBuyerHasNoPhone() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        event.getOrderAfter().getBuyer().setNormalizedPhone(null);

        List<UidBpmMessage> messages = factory.from(event);

        assertTrue(messages.isEmpty());
    }


    @Test
    public void shouldNotCreateBpmMessageIfBuyerHasNoEmail() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        event.getOrderAfter().getBuyer().setEmail(null);

        List<UidBpmMessage> messages = factory.from(event);

        assertTrue(messages.isEmpty());
    }

    @Test
    public void shouldNotCreateBpmMessageIfBuyerTypeIsPerson() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        event.getOrderAfter().getBuyer().setBusinessBalanceId(null);

        List<UidBpmMessage> messages = factory.from(event);

        assertTrue(messages.isEmpty());
    }

    @Test
    public void shouldCreateBpmMessageWithExperimentsIfExist() {
        OrderHistoryEvent event = loadOrderEvent(testedJson());
        event.getOrderAfter().setProperty(OrderPropertyType.EXPERIMENTS, "exp1=1;exp2=b");

        List<UidBpmMessage> messages = factory.from(event);

        assertThat(messages.size(), greaterThanOrEqualTo(1));
        assertThat(messages.get(0), instanceOf(UidBpmMessage.class));

        UidBpmMessage message = messages.get(0);

        assertThat(message.getVariables(), instanceOf(Map.class));
        assertThat(message.getVariables().get(ProcessVariablesNames.EXPERIMENTS), instanceOf(Map.class));

        Map<String, String> experiments
                = (Map<String, String>) message.getVariables().get(ProcessVariablesNames.EXPERIMENTS);

        assertThat(experiments, equalTo(Map.of("exp1", "1", "exp2", "b")));
    }

    OrderHistoryEvent loadOrderEvent(Resource resource) {
        try {
            byte[] message = Files.readAllBytes(resource.getFile().toPath());
            return objectMapper.readValue(message, OrderHistoryEvent.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract Resource testedJson();
}
