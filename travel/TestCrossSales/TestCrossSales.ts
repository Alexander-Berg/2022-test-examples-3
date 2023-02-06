import {TestHappyPageHotelsCrossSaleMap} from './components/TestHappyPageHotelsCrossSaleMap';
import {Component} from 'components/Component';

export default class TestCrossSales extends Component {
    readonly hotelsCrossSaleMap: TestHappyPageHotelsCrossSaleMap;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.hotelsCrossSaleMap = new TestHappyPageHotelsCrossSaleMap(browser, {
            parent: this.qa,
            current: 'hotelsCrossSaleMap',
        });
    }
}
