import TestAviaBadges from 'helpers/project/avia/pages/SearchResultsPage/components/TestAviaResultVariant/components/TestAviaMobileResultVariant/components/TestAviaBadges';
import TestAviaTariffSelectorPopup from 'helpers/project/avia/pages/SearchResultsPage/components/TestAviaResultVariant/components/TestAviaDesktopResultVariant/components/TestAviaTariffSelectorPopup';

import TestFlights from './components/TestFlights';
import {Component} from 'components/Component';
import {ComponentArray} from 'components/ComponentArray';
import {TestPrice} from 'components/TestPrice';
import {Button} from 'components/Button';

export default class TestAviaMobileResultVariant extends Component {
    orderLink: Component;
    flights: ComponentArray<TestFlights>;
    price: TestPrice;
    buyButton: Button;
    badges: TestAviaBadges;
    partnerInfo: Component;
    logo: Component;
    title: Component;
    baggageInfo: Component;
    textBuyButton: Component;

    readonly tariffSelectorPopup: ComponentArray<TestAviaTariffSelectorPopup>;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.flights = new ComponentArray(
            browser,
            {parent: this.qa, current: 'flights'},
            TestFlights,
        );

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
            current: 'partner',
        });

        this.orderLink = new Component(browser, {
            parent: this.qa,
            current: 'link',
        });

        this.badges = new TestAviaBadges(browser, {
            parent: this.qa,
            current: 'badges',
        });

        this.partnerInfo = new Component(browser, {
            parent: this.qa,
            current: 'partnerInfo',
        });

        this.logo = new Component(browser, {parent: this.qa, current: 'logo'});

        this.title = new Component(browser, {
            parent: this.qa,
            current: 'title',
        });

        this.baggageInfo = new Component(browser, {
            parent: this.qa,
            current: 'baggageInfo',
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
    }
}
