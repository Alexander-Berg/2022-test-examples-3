import {Component} from 'helpers/project/common/components';
import {TestPrice} from 'helpers/project/common/components/TestPrice';

import TestHotelPromoCodes from './TestHotelPromoCodes';

export default class TestHotelFullPrice extends Component {
    price: TestPrice;
    arrowIcon: Component;
    promoCodes: TestHotelPromoCodes;

    constructor(browser: WebdriverIO.Browser, qa = 'hotelFullPrice') {
        super(browser, qa);

        this.price = new TestPrice(browser, {
            path: [this.qa],
            current: 'price',
        });

        this.arrowIcon = new Component(browser, {
            path: [this.qa],
            current: 'arrow',
        });

        this.promoCodes = new TestHotelPromoCodes(browser);
    }
}
