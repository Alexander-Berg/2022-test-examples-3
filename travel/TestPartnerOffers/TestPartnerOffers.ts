import TestPartnerOffer from 'helpers/project/hotels/pages/HotelPage/components/TestOffersInfo/components/TestPartnerOffers/components/TestPartnerOffer/TestPartnerOffer';

import {Component} from 'components/Component';
import {ComponentArray} from 'components/ComponentArray';

export default class TestPartnerOffers extends Component {
    title: Component;
    hotelName: Component;
    offers: ComponentArray<TestPartnerOffer>;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.title = new Component(browser, {
            parent: this.qa,
            current: 'title',
        });
        this.hotelName = new Component(browser, {
            parent: this.qa,
            current: 'hotelName',
        });

        this.offers = new ComponentArray(
            browser,
            {parent: this.qa, current: 'offer'},
            TestPartnerOffer,
        );
    }
}
