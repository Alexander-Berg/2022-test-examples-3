package ru.yandex.market.crm.triggers.services.bpm.delegates;

import java.util.Collections;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.crm.core.domain.Color;
import ru.yandex.market.crm.core.domain.trigger.ProductItem;
import ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames;
import ru.yandex.market.crm.core.services.report.ReportService;
import ru.yandex.market.crm.core.services.trigger.MessageTypes;
import ru.yandex.market.crm.domain.report.Offer;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.ModelInfo;
import ru.yandex.market.crm.json.serialization.JsonSerializer;
import ru.yandex.market.crm.triggers.services.bpm.variables.ModelOffersInfo;
import ru.yandex.market.crm.triggers.services.bpm.variables.ProductItemChange;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PdoProductItemChangeListenerTest {

    @Mock
    private ReportService reportService;

    @Mock
    private JsonSerializer jsonSerializer;

    private ProductItemChangeListener itemChangeListener;

    @Before
    public void setup() {
        itemChangeListener = new PdoProductItemChangeListener(reportService, jsonSerializer);
    }

    @Test
    public void testOffersInfoIsSet() throws Exception {
        DelegateExecutionContext context = mockContext(54L, ProductItem.model(null, "112233"));

        ModelInfo modelInfo = new ModelInfo("112233");
        modelInfo.setPrice("1 024");
        when(reportService.getProductInfo(Mockito.any(), Mockito.same(Color.GREEN), Mockito.eq(54L)))
                .thenReturn(modelInfo);

        Offer offer = new Offer();
        offer.setId("offerId_1");
        offer.setPromo(new Offer.Promo()
                .setKey("promo_key_1")
                .setType("gift-with-purchase")
                .setDescription("Купи любой смартфон или планшет и получи дополнительный год гарантии."));
        offer.setPrice(new Offer.Price().setDiscountPercent("12"));

        when(reportService.getOffersWithPromoOrDiscount(Collections.singleton("112233"), Color.GREEN, 54L))
                .thenReturn(Collections.singletonList(offer));

        itemChangeListener.doExecute(context);

        ArgumentCaptor<ModelOffersInfo> captor = ArgumentCaptor.forClass(ModelOffersInfo.class);
        verify(jsonSerializer).writeObjectAsString(captor.capture());

        ModelOffersInfo offersInfo = captor.getValue();
        assertEquals(54, offersInfo.getRegion());
        assertEquals("112233", offersInfo.getModelInfo().getId());
        assertEquals("1 024", offersInfo.getModelInfo().getPrice());
        assertEquals(ImmutableSet.of("gift-with-purchase"), offersInfo.getPromoTypes());
        assertEquals(12, offersInfo.getMaxDiscount());
    }


    private DelegateExecutionContext mockContext(Long region, ProductItem productItem) {
        DelegateExecutionContext ctx = mock(DelegateExecutionContext.class);
        when(ctx.getProcessVariable(ProcessVariablesNames.Event.PRODUCT_ITEM_CHANGE))
                .thenReturn(new ProductItemChange(MessageTypes.WISHLIST_ITEM_ADDED, Color.GREEN, productItem, region,
                        false));
        return ctx;
    }
}
