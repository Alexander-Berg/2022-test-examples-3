import {Component} from 'components/Component';

export default class TestAviaTariffsOnOrderPage extends Component {
    readonly price: Component;
    readonly carryOnInfo: Component;
    readonly baggageInfo: Component;
    readonly refundInfo: Component;
    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);
        this.price = new Component(browser, {
            parent: this.qa,
            current: 'price',
        });

        this.carryOnInfo = new Component(browser, {
            parent: this.qa,
            current: 'carryOnIcon',
        });

        this.baggageInfo = new Component(browser, {
            parent: this.qa,
            current: 'baggageIcon',
        });

        this.refundInfo = new Component(browser, {
            parent: this.qa,
            current: 'refundInfo',
        });
    }

    async isTariffInfoVisible(): Promise<boolean> {
        return (
            await Promise.all([
                this.price.isVisible(),
                this.carryOnInfo.isVisible(),
                this.baggageInfo.isVisible(),
                this.refundInfo.isVisible(),
            ])
        ).every(Boolean);
    }
}
