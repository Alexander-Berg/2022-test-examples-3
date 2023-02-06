import TestOrderHotelGuest from 'helpers/project/account/pages/OrderPage/components/TestOrderHotelsGuests/components/TestOrderHotelGuest';

import {Component} from 'components/Component';
import {ComponentArray} from 'components/ComponentArray';

export default class TestOrderHotelsGuests extends Component {
    title: Component;
    guests: ComponentArray<TestOrderHotelGuest>;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.title = new Component(this.browser, {
            parent: this.qa,
            current: 'title',
        });

        this.guests = new ComponentArray(
            this.browser,
            {parent: this.qa, current: 'guest'},
            TestOrderHotelGuest,
        );
    }
}
