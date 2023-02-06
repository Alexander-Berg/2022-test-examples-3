import TestOrder from 'helpers/project/account/pages/TripPage/components/TestOrder';
import TestOrderMainInfo from 'helpers/project/account/pages/TripPage/components/TestOrderMainInfo';
import TestDescriptionAndActions from 'helpers/project/account/pages/TripPage/components/TestBusOrder/components/TestDescriptionAndActions';

export default class TestBusOrder extends TestOrder {
    readonly orderMainInfo: TestOrderMainInfo;
    readonly descriptionAndActions: TestDescriptionAndActions;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.orderMainInfo = new TestOrderMainInfo(this.browser, {
            parent: this.qa,
            current: 'orderMainInfo',
        });
        this.descriptionAndActions = new TestDescriptionAndActions(
            this.browser,
            {parent: this.qa, current: 'descriptionAndActions'},
        );
    }
}
