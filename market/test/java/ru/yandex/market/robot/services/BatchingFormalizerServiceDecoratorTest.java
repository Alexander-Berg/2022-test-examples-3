package ru.yandex.market.robot.services;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.http.ServiceException;
import ru.yandex.market.ir.http.Formalizer;
import ru.yandex.market.ir.http.FormalizerService;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

/**
 * @author inenakhov
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class BatchingFormalizerServiceDecoratorTest {
    private int categoryId = 100;
    private int vendorId = 100;
    private int shopId = 1;

    @Mock
    private FormalizerService idealFormalizerMock;

    @Mock
    private FormalizerService slowFormalizerMock;

    @Mock
    private FormalizerService completlyBrokenFormalizerMock;

    @Before
    public void setup() {
        when(idealFormalizerMock.formalize(any())).thenAnswer(i -> formalize(i.getArgument(0)));

        when(slowFormalizerMock.formalize(any())).thenAnswer(i -> {
            Formalizer.FormalizerRequest request = i.getArgument(0);

            if (request.getOfferCount() > 20) {
                throw new ServiceException("timeout");
            }

            return formalize(request);
        });

        when(completlyBrokenFormalizerMock.formalize(any())).thenAnswer(i -> {
            throw new ServiceException("timeout");
        });
    }

    @Test
    public void testFormalizeIdealFormalizer() throws Exception {
        BatchingFormalizerServiceDecorator batchingFormalizerServiceDecorator =
            new BatchingFormalizerServiceDecorator();
        batchingFormalizerServiceDecorator.setFormalizerService(idealFormalizerMock);
        int offersCount = 100;

        Formalizer.FormalizerRequest.Builder builder = Formalizer.FormalizerRequest.newBuilder();
        builder.addAllOffer(generateOffers(offersCount));

        Formalizer.FormalizerResponse response = batchingFormalizerServiceDecorator.formalize(builder.build());
        assertEquals(response.getOfferCount(), offersCount);
        verify(idealFormalizerMock, times(1)).formalize(any());
    }

    @Test
    public void testFormalizeBrokenFormalizer() throws Exception {
        BatchingFormalizerServiceDecorator batchingFormalizerServiceDecorator =
            new BatchingFormalizerServiceDecorator();
        batchingFormalizerServiceDecorator.setFormalizerService(completlyBrokenFormalizerMock);
        int offersCount = 170;

        Formalizer.FormalizerRequest.Builder builder = Formalizer.FormalizerRequest.newBuilder();
        builder.addAllOffer(generateOffers(offersCount));

        try {
            batchingFormalizerServiceDecorator.formalize(builder.build());
            assertFalse(true);
        } catch (ServiceException se) {
            assertTrue(true);
            verify(completlyBrokenFormalizerMock, times(4)).formalize(any());
        }
    }

    @Test
    public void testFormalizeSlowFormalizer() throws Exception {
        BatchingFormalizerServiceDecorator batchingFormalizerServiceDecorator =
            new BatchingFormalizerServiceDecorator();
        batchingFormalizerServiceDecorator.setFormalizerService(slowFormalizerMock);
        int offersCount = 170;

        Formalizer.FormalizerRequest.Builder builder = Formalizer.FormalizerRequest.newBuilder();
        builder.addAllOffer(generateOffers(offersCount));

        Formalizer.FormalizerResponse response = batchingFormalizerServiceDecorator.formalize(builder.build());

        assertEquals(response.getOfferCount(), offersCount);
        verify(slowFormalizerMock, times(21)).formalize(any());
    }

    private List<Formalizer.Offer> generateOffers(int count) {
        ArrayList<Formalizer.Offer> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Formalizer.Offer.Builder builder = Formalizer.Offer.newBuilder();
            builder.setCategoryId(categoryId);
            builder.setTitle("title");
            builder.setDescription("description");
            builder.setShopId(shopId);
            result.add(builder.build());
        }

        return result;
    }

    @NotNull
    private Formalizer.FormalizerResponse formalize(Formalizer.FormalizerRequest request) {
        Formalizer.FormalizerResponse.Builder responseBuilder = Formalizer.FormalizerResponse.newBuilder();
        request.getOfferList().forEach(offer -> {
            Formalizer.FormalizedOffer.Builder offerBuilder = Formalizer.FormalizedOffer.newBuilder();
            offerBuilder.setCategoryId(categoryId);
            offerBuilder.setVendorId(vendorId);
            responseBuilder.addOffer(offerBuilder.build());
        });

        return responseBuilder.build();
    }
}