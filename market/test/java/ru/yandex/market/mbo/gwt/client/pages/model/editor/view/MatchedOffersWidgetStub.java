package ru.yandex.market.mbo.gwt.client.pages.model.editor.view;

import ru.yandex.market.mbo.gwt.client.pages.model.editor.addon.matched_offers.MatchedOffersWidget;
import ru.yandex.market.mbo.gwt.models.gurulight.OfferData;

import java.util.List;

/**
 * @author yuramalinov
 * @created 06.12.18
 */
public class MatchedOffersWidgetStub extends EditorWidgetStub implements MatchedOffersWidget {
    @Override
    public void setOffers(List<OfferData> offers) {
    }

    @Override
    public void setLoaderVisible(boolean visible) {
    }

    @Override
    public void setWarningVisible(boolean visible) {
    }

    @Override
    public void setLimitCount(int count) {
    }

}
