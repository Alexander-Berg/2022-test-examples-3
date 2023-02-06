import {Component} from 'components/Component';

export class TestHotelCardItem extends Component {
    hotelName: Component;
    nightsText: Component;
    budapeshtBanner: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.hotelName = new Component(browser, {
            parent: this.qa,
            current: 'hotelName',
        });

        this.nightsText = new Component(browser, {
            parent: this.qa,
            current: 'nightsCount',
        });

        this.budapeshtBanner = new Component(browser, {
            parent: this.qa,
            current: 'budapeshtBanner',
        });
    }
}
