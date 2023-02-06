import {Component} from 'components/Component';
import {TestHotelsBookSearchParams} from '../../../TestHotelsBookPage/components/TestHotelsBookSearchParams/TestHotelsBookSearchParams';

export class TestHotelsOrderInfo extends Component {
    hotelName: Component;

    searchParams: TestHotelsBookSearchParams;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.hotelName = new Component(browser, {
            parent: this.qa,
            current: 'hotelName',
        });

        this.searchParams = new TestHotelsBookSearchParams(browser, this.qa);
    }
}
