import {Component} from 'components/Component';
import {TestPrice} from 'components/TestPrice';

export class TestTrainsSearchHeader extends Component {
    private readonly price: TestPrice;

    constructor(browser: WebdriverIO.Browser, qa: QA = 'trainsSearchHeader') {
        super(browser, qa);

        this.price = new TestPrice(browser, {
            parent: this.qa,
            current: 'price',
        });
    }

    async getPriceFrom(): Promise<number> {
        return this.price.getValue();
    }
}
