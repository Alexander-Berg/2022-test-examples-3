import {Component} from 'components/Component';
import {ComponentArray} from 'components/ComponentArray';

import {TestHotelCardItem} from './TestHotelCardItem';
import {TestHotelsSearchStatusProvider} from './TestHotelsSearchStatusProvider';

export class TestHotelsSearchResults extends Component {
    status: TestHotelsSearchStatusProvider;
    prevButton: Component;
    nextButton: Component;
    hotelCards: ComponentArray<TestHotelCardItem>;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.status = new TestHotelsSearchStatusProvider(browser, qa);
        this.prevButton = new Component(browser, {
            parent: this.qa,
            current: 'prevButton',
        });
        this.nextButton = new Component(browser, {
            parent: this.qa,
            current: 'nextButton',
        });

        this.hotelCards = new ComponentArray(
            browser,
            {parent: this.qa, current: 'hotelCard'},
            TestHotelCardItem,
        );
    }
}
