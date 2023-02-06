import {TestCityPageHotelCardItem} from 'helpers/project/hotels/pages/HotelsCityPage/TestCityPageHotelCardItem';

import {Component} from 'components/Component';
import {ComponentArray} from 'components/ComponentArray';

export class TestHotelsCityPageSearchResults extends Component {
    hotelCards: ComponentArray<TestCityPageHotelCardItem>;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.hotelCards = new ComponentArray(
            browser,
            {parent: this.qa, current: 'hotelCard'},
            TestCityPageHotelCardItem,
        );
    }
}
