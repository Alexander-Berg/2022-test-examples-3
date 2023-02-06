import TestMainOffer from 'helpers/project/hotels/pages/HotelPage/components/TestOffersInfo/components/TestMainOffers/components/TestMainOffer';

import {Component} from 'components/Component';
import {ComponentArray} from 'components/ComponentArray';

export default class TestMainOffers extends Component {
    offers: ComponentArray<TestMainOffer>;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.offers = new ComponentArray(
            browser,
            {parent: this.qa, current: 'mainOffer'},
            TestMainOffer,
        );
    }
}
