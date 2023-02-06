import {SECOND} from 'helpers/constants/dates';

import {Component} from 'components/Component';
import {TestHotelsCrossSaleMapCard} from 'components/TestHotelsCrossSaleMapCard';

export class TestSearchHotelsCrossSaleMap extends Component {
    readonly mapCard: TestHotelsCrossSaleMapCard;
    readonly skeleton: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.mapCard = new TestHotelsCrossSaleMapCard(this.browser, {
            parent: this.qa,
            current: 'card',
        });
        this.skeleton = new Component(this.browser, {
            parent: this.qa,
            current: 'skeleton',
        });
    }

    async scrollIntoView(): Promise<void> {
        await this.skeleton.scrollIntoView();
    }

    async waitForLoading(): Promise<void> {
        await this.mapCard.waitForVisible(10 * SECOND);
    }
}
