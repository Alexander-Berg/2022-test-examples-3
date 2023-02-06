import {Component} from 'helpers/project/common/components';

export class TestHotelOfferLabels extends Component {
    mealInfo: Component;
    cancellationInfo: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.mealInfo = new Component(browser, {
            parent: this.qa,
            current: 'mealInfo',
        });

        this.cancellationInfo = new Component(browser, {
            parent: this.qa,
            current: 'cancellationInfo',
        });
    }
}
