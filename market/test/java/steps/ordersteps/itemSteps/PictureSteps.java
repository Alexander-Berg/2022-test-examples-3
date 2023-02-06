package steps.orderSteps.itemSteps;

import java.util.ArrayList;
import java.util.Collection;

import ru.yandex.market.common.report.model.OfferPicture;

class PictureSteps {
    private static final String URL = "//cs-ellpic01gt.yandex.ru/market_9TdaAraUke26IAansmxUZQ_";
    private static final Integer WIDTH = 1;
    private static final Integer HEIGHT = 2;
    private static final Integer CONTAINER_WIDTH = 3;
    private static final Integer CONTAINER_HEIGHT = 4;

    private PictureSteps() {
    }

    static Collection<OfferPicture> getPictures() {
        ArrayList<OfferPicture> offerPicturesList = new ArrayList<>();
        OfferPicture offerPicture = new OfferPicture();

        offerPicture.setContainerHeight(CONTAINER_HEIGHT);
        offerPicture.setWidth(WIDTH);
        offerPicture.setHeight(HEIGHT);
        offerPicture.setContainerWidth(CONTAINER_WIDTH);
        offerPicture.setUrl(URL);

        offerPicturesList.add(offerPicture);
        return offerPicturesList;
    }

    static String getUrl() {
        return URL;
    }
}
