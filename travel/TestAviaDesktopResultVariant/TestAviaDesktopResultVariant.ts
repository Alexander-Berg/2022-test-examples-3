import {TestPrice} from 'helpers/project/common/components/TestPrice';
import TestAviaBadges from 'helpers/project/avia/pages/SearchResultsPage/components/TestAviaResultVariant/components/TestAviaMobileResultVariant/components/TestAviaBadges';
import {Button, ComponentArray} from 'helpers/project/common/components';
import TestAviaTariffSelectorPopup from 'helpers/project/avia/pages/SearchResultsPage/components/TestAviaResultVariant/components/TestAviaDesktopResultVariant/components/TestAviaTariffSelectorPopup';

import TestDesktopFlights from './components/TestDesktopFlights';
import {TestLogosWithTitle} from './components/TestLogosWithTitle';
import {Component} from 'components/Component';

export default class TestAviaDesktopResultVariant extends Component {
    readonly airline: TestLogosWithTitle;
    readonly forwardFlights: TestDesktopFlights;
    readonly backwardFlights: TestDesktopFlights;
    readonly orderLink: Component;
    readonly carryOnIcon: Component;
    readonly baggageIcon: Component;
    readonly price: TestPrice;
    readonly buyButton: Button;
    readonly tariffSelectorPopup: ComponentArray<TestAviaTariffSelectorPopup>;
    readonly textBuyButton: Component;
    readonly badges: TestAviaBadges;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.airline = new TestLogosWithTitle(browser, {
            parent: this.qa,
            current: 'airline',
        });

        this.forwardFlights = new TestDesktopFlights(browser, {
            parent: this.qa,
            current: 'forwardFlights',
        });
        this.backwardFlights = new TestDesktopFlights(browser, {
            parent: this.qa,
            current: 'backwardFlights',
        });

        this.orderLink = new Component(browser, {
            parent: this.qa,
            current: 'link',
        });

        this.price = new TestPrice(browser, {
            parent: this.qa,
            current: 'price',
        });

        this.buyButton = new Button(browser, {
            parent: this.qa,
            current: 'buyButton',
        });

        this.textBuyButton = new Component(browser, {
            parent: this.qa,
            current: 'text',
        });

        this.badges = new TestAviaBadges(browser, {
            parent: this.qa,
            current: 'badges',
        });

        this.tariffSelectorPopup =
            new ComponentArray<TestAviaTariffSelectorPopup>(
                browser,
                {
                    parent: this.qa,
                    current: 'tariffSelectorPopup',
                },
                TestAviaTariffSelectorPopup,
            );

        this.carryOnIcon = new Component(browser, {
            parent: this.qa,
            current: 'carryOnIcon',
        });

        this.baggageIcon = new Component(browser, {
            parent: this.qa,
            current: 'baggageIcon',
        });
    }

    async isBaggageInfoVisible(): Promise<boolean> {
        return (
            await Promise.all([
                this.carryOnIcon.isVisible(),
                this.baggageIcon.isVisible(),
            ])
        ).every(Boolean);
    }
}
