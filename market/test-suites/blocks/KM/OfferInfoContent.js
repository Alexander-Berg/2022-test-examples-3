import OfferInfo from '@self/platform/widgets/content/OfferDetailsPopup/__pageObject';
import SnippetList from '@self/platform/widgets/content/productOffers/Results/components/ResultsPaged/__pageObject';

export default {
    suiteName: 'KMPopupContent',
    selector: OfferInfo.content,
    // Элемент, найденный через find почему-то кешируется и протухает, поэтому если вынести этот before в общий перед
    // каждым кейсом - то второй кейс всегда будет падать с StaleElementReference
    before(actions, find) {
        actions
            .click(find(SnippetList.root))
            .waitForElementToShow(OfferInfo.card, 5000)
            .wait(250); // И чуть-чуть ждём, пока закончится анимация.
    },
    capture() {},
};
