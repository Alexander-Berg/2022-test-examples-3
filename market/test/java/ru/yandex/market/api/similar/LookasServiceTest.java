package ru.yandex.market.api.similar;

import com.google.common.collect.Maps;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import ru.yandex.market.api.domain.ModelOrOfferV2;
import ru.yandex.market.api.domain.OfferId;
import ru.yandex.market.api.domain.v2.ModelV2;
import ru.yandex.market.api.domain.v2.OfferV2;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.internal.common.GenericParams;
import ru.yandex.market.api.internal.computervision.ComputerVisionClient;
import ru.yandex.market.api.internal.computervision.ComputerVisionResult;
import ru.yandex.market.api.internal.computervision.EntityId;
import ru.yandex.market.api.model.ModelService;
import ru.yandex.market.api.offer.OfferService;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;
import ru.yandex.market.api.util.concurrent.Futures;
import ru.yandex.market.api.util.concurrent.Pipelines;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.market.api.computervision.ComputerVisionTestHelper.model;
import static ru.yandex.market.api.computervision.ComputerVisionTestHelper.offer;

/**
 * @author dimkarp93
 */
@WithMocks
public class LookasServiceTest extends UnitTestBase {
    @InjectMocks
    LookasServiceImpl similarService;

    @Mock
    ComputerVisionClient computerVisionClient;

    @Mock
    OfferService offerService;
    @Mock
    ModelService modelService;

    @Test
    public void allElements() {
        String url = "http://supermodel.com";

        ComputerVisionResult result = new ComputerVisionResult(true, "3",
            Arrays.asList(offer("1"), model(2), model(3), offer("4")));

        when(computerVisionClient.looksas(url)).thenReturn(Pipelines.startWithValue(result));
        when(modelService.<ModelV2>getModels(any(), any(), any())).thenReturn(
            Pipelines.startWithValue(makeModels(2, 3))
        );
        when(offerService.getOffersV2(any(), any(), any())).thenReturn(
            Pipelines.startWithValue(
                Maps.uniqueIndex(makeOffers("1", "4"), offer -> offer.getId().getWareMd5())
            )
        );

        InternalLookasResult<ModelOrOfferV2> isr =
            Futures.waitAndGet(
                similarService.lookas(url,
                    LookasService.Entity.ALL,
                    Collections.emptyList(),
                    GenericParams.DEFAULT)
            );

        assertEquals("3", isr.getCbirId());

        List<? extends ModelOrOfferV2> entities = isr.getElements();

        assertEquals(4, entities.size());
        assertModel(2, entities.get(0));
        assertModel(3, entities.get(1));
        assertOffer("1", entities.get(2));
        assertOffer("4", entities.get(3));

    }

    @Test
    public void notFoundTest() {
        String url = "http://supermodel.com";

        ComputerVisionResult result = new ComputerVisionResult(true, "3", null);

        when(computerVisionClient.looksas(url)).thenReturn(Pipelines.startWithValue(result));

        InternalLookasResult<ModelOrOfferV2> isr = Futures.waitAndGet(
            similarService.lookas(url,
                LookasService.Entity.ALL,
                Collections.emptyList(),
                GenericParams.DEFAULT)
        );

        List<ModelOrOfferV2> elements = isr.getElements();

        assertNotNull(elements);
        assertTrue(elements.isEmpty());
    }

    @Test
    public void notFoundModelsTest() {
        String url = "http://supermodel.com";

        ComputerVisionResult result = new ComputerVisionResult(
            true,
            "3",
            Arrays.asList(new EntityId(-1, "abs"))
        );

        when(computerVisionClient.looksas(url)).thenReturn(Pipelines.startWithValue(result));

        InternalLookasResult<ModelOrOfferV2> isr = Futures.waitAndGet(
            similarService.lookas(url,
                LookasService.Entity.MODELS,
                Collections.emptyList(),
                GenericParams.DEFAULT)
        );

        List<ModelOrOfferV2> elements = isr.getElements();

        assertNotNull(elements);
        assertTrue(elements.isEmpty());
    }

    @Test
    public void onlyModelsSimilar() {
        String url = "http://supermodel.com";

        ComputerVisionResult result = new ComputerVisionResult(true, "1",
            Arrays.asList(offer("1"), model(2), model(3), offer("4")));

        when(computerVisionClient.looksas(url)).thenReturn(Pipelines.startWithValue(result));
        when(modelService.<ModelV2>getModels(any(), any(), any())).thenReturn(
            Pipelines.startWithValue(makeModels(2, 3))
        );


        InternalLookasResult<ModelV2> isr =
            Futures.waitAndGet(
                similarService.lookas(url,
                    LookasService.Entity.MODELS,
                    Collections.emptyList(),
                    GenericParams.DEFAULT)
            );

        assertEquals("1", isr.getCbirId());

        List<ModelV2> entities = isr.getElements();

        assertEquals(2, entities.size());
        assertModel(2, entities.get(0));
        assertModel(3, entities.get(1));
    }

    @Test
    public void onlyOffersSimilar() {
        String url = "http://supermodel.com";

        ComputerVisionResult result = new ComputerVisionResult(true, "2",
            Arrays.asList(offer("1"), model(2), model(3), offer("4")));

        when(computerVisionClient.looksas(url)).thenReturn(Pipelines.startWithValue(result));
        when(offerService.getOffersV2(any(), any(), any())).thenReturn(
            Pipelines.startWithValue(
                Maps.uniqueIndex(makeOffers("1", "4"), offer -> offer.getId().getWareMd5())
            )
        );

        InternalLookasResult<OfferV2> isr =
            Futures.waitAndGet(
                similarService.lookas(url,
                    LookasService.Entity.OFFERS,
                    Collections.emptyList(),
                    GenericParams.DEFAULT)
            );

        assertEquals("2", isr.getCbirId());

        List<OfferV2> entities = isr.getElements();

        assertEquals(2, entities.size());
        assertOffer("1", entities.get(0));
        assertOffer("4", entities.get(1));
    }

    @Test
    public void processComputerVisionError() {
        String url = "http://supermodel.com";

        ComputerVisionResult result = new ComputerVisionResult(false, "3", null);

        when(computerVisionClient.looksas(url)).thenReturn(Pipelines.startWithValue(result));

        exception.expect(ExternalComputerVisionErrorException.class);

        Futures.waitAndGet(
            similarService.lookas(url,
                LookasService.Entity.ALL,
                Collections.emptyList(),
                GenericParams.DEFAULT)
        );
    }

    private void assertModel(long modelId, ModelOrOfferV2 modelOrOffer) {
        assertTrue(modelOrOffer instanceof ModelV2);
        ModelV2 model = (ModelV2) modelOrOffer;
        assertEquals(modelId, model.getId());
    }

    private void assertOffer(String wareMd5, ModelOrOfferV2 modelOrOffer) {
        assertTrue(modelOrOffer instanceof OfferV2);
        OfferV2 offer = (OfferV2) modelOrOffer;
        assertEquals(wareMd5, offer.getId().getWareMd5());
    }

    private ModelV2 makeModel(long modelId) {
        ModelV2 model = new ModelV2();
        model.setId(modelId);
        return model;
    }

    private List<ModelV2> makeModels(long... modelIds) {
        return Arrays.stream(modelIds).mapToObj(this::makeModel).collect(Collectors.toList());
    }

    private OfferV2 makeOffer(String wareMd5) {
        OfferV2 offer = new OfferV2();
        offer.setId(new OfferId(wareMd5, null));
        return offer;
    }

    private List<OfferV2> makeOffers(String... waresMd5) {
        return Arrays.stream(waresMd5).map(this::makeOffer).collect(Collectors.toList());
    }

}
