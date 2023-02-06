import {Component} from 'components/Component';

export default class TestAviaTariffSelector extends Component {
    readonly carryOnIcon: Component;
    readonly baggageIcon: Component;
    readonly refundInfo: Component;
    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.carryOnIcon = new Component(browser, {
            parent: this.qa,
            current: 'carryOnIcon',
        });

        this.baggageIcon = new Component(browser, {
            parent: this.qa,
            current: 'baggageIcon',
        });

        this.refundInfo = new Component(browser, {
            parent: this.qa,
            current: 'refundInfo',
        });
    }

    async isBaggageInfoVisible(): Promise<boolean> {
        if (this.isDesktop) {
            return (
                await Promise.all([
                    this.carryOnIcon.isVisible(),
                    this.baggageIcon.isVisible(),
                    this.refundInfo.isVisible(),
                ])
            ).every(Boolean);
        }

        return (
            await Promise.all([
                this.carryOnIcon.isVisible(),
                this.baggageIcon.isVisible(),
            ])
        ).every(Boolean);
    }
}
