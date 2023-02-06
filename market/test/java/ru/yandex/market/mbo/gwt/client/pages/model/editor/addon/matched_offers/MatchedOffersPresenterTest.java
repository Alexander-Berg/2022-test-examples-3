package ru.yandex.market.mbo.gwt.client.pages.model.editor.addon.matched_offers;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import ru.yandex.market.mbo.common.processing.ProcessingResult;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.EditorEventBus;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.MessagesEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.rpc.Rpc;
import ru.yandex.market.mbo.gwt.client.utils.messages.MessageType;
import ru.yandex.market.mbo.gwt.models.gurulight.OfferData;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author yuramalinov
 * @created 06.12.18
 */
@SuppressWarnings({"unchecked", "checkstyle:magicnumber"})
public class MatchedOffersPresenterTest {

    private MatchedOffersPresenter.View view;
    private MatchedOffersPresenter presenter;
    private EditorEventBus eventBus;
    private Rpc rpc;

    @Before
    public void setUp() {
        view = Mockito.mock(MatchedOffersPresenter.View.class);
        rpc = Mockito.mock(Rpc.class);
        eventBus = Mockito.mock(EditorEventBus.class);
        presenter = new MatchedOffersPresenter(view, eventBus, rpc);
    }

    @Test
    public void testLoadModelData() {
        Mockito.doAnswer(call -> {
            ((Consumer<List<OfferData>>) call.getArgument(2))
                .accept(Collections.singletonList(new OfferData()));
            return null;
        }).when(rpc).getModelMatchedOffers(Mockito.anyLong(), Mockito.any(), Mockito.any(), Mockito.any());

        presenter.loadOffers(1, false);

        ArgumentCaptor<List<OfferData>> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(view).setOffers(captor.capture());
        Assertions.assertThat(captor.getValue()).hasSize(1);
        Mockito.verify(view).setWarningVisible(Mockito.eq(false));

        verifyVisible();
    }

    @Test
    public void testLoadModificationData() {
        Mockito.doAnswer(call -> {
            ((Consumer<List<OfferData>>) call.getArgument(2))
                .accept(Collections.singletonList(new OfferData()));
            return null;
        }).when(rpc).getModificationMatchedOffers(Mockito.anyLong(), Mockito.any(), Mockito.any(), Mockito.any());

        presenter.loadOffers(1, true);

        ArgumentCaptor<List<OfferData>> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(view).setOffers(captor.capture());
        Assertions.assertThat(captor.getValue()).hasSize(1);
        Mockito.verify(view).setWarningVisible(Mockito.eq(false));

        verifyVisible();
    }

    @Test
    public void testLoadALotOfData() {
        Mockito.doAnswer(call -> {
            ((Consumer<List<OfferData>>) call.getArgument(2))
                .accept(IntStream.range(0, MatchedOffersPresenter.MAX_OFFERS + 2)
                    .mapToObj(i -> new OfferData())
                    .collect(Collectors.toList()));
            return null;
        }).when(rpc).getModelMatchedOffers(Mockito.anyLong(), Mockito.any(), Mockito.any(), Mockito.any());

        presenter.loadOffers(1, false);

        ArgumentCaptor<List<OfferData>> captor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(view).setOffers(captor.capture());
        Assertions.assertThat(captor.getValue()).hasSize(MatchedOffersPresenter.MAX_OFFERS);
        Mockito.verify(view).setWarningVisible(Mockito.eq(true));
        Mockito.verify(view).setLimitCount(Mockito.eq(MatchedOffersPresenter.MAX_OFFERS));

        verifyVisible();
    }

    @Test
    public void testError() {
        Mockito.doAnswer(call -> {
            ((Consumer<Throwable>) call.getArgument(3))
                .accept(new RuntimeException("Something bad happened"));
            return null;
        }).when(rpc).getModelMatchedOffers(Mockito.anyLong(), Mockito.any(), Mockito.any(), Mockito.any());

        presenter.loadOffers(1, false);

        Mockito.verify(view, Mockito.never()).setOffers(Mockito.anyList());
        Mockito.verify(view, Mockito.never()).setWarningVisible(Mockito.anyBoolean());
        Mockito.verify(view, Mockito.never()).setLimitCount(Mockito.anyInt());

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        Mockito.verify(eventBus).fireEventSync(captor.capture());

        Assertions.assertThat(captor.getValue()).isInstanceOf(MessagesEvent.class);
        MessagesEvent event = (MessagesEvent) captor.getValue();
        Assertions.assertThat(event.getType()).isEqualTo(MessageType.ERROR);
        Assertions.assertThat(event.getResults())
            .extracting(ProcessingResult::getText)
            .allSatisfy(error ->
                Assertions.assertThat(error).contains("bad happened"));

        verifyVisible();
    }

    private void verifyVisible() {
        ArgumentCaptor<Boolean> visible = ArgumentCaptor.forClass(Boolean.class);
        Mockito.verify(view, Mockito.times(2)).setLoaderVisible(visible.capture());
        Assertions.assertThat(visible.getAllValues()).containsExactly(true, false);
    }
}
