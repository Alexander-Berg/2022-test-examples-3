import TestOrderMainInfo from 'helpers/project/account/pages/TripPage/components/TestOrderMainInfo';
import TestOrder from 'helpers/project/account/pages/TripPage/components/TestOrder';
import TestDescriptionAndActions from 'helpers/project/account/pages/TripPage/components/TestTrainOrder/components/TestDescriptionAndActions';

export default class TestTrainOrder extends TestOrder {
    orderMainInfo: TestOrderMainInfo;
    descriptionAndActions: TestDescriptionAndActions;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.orderMainInfo = new TestOrderMainInfo(this.browser, {
            parent: this.qa,
            current: 'orderMainInfo',
        });
        this.descriptionAndActions = new TestDescriptionAndActions(
            this.browser,
            {
                parent: this.qa,
                current: 'descriptionAndActions',
            },
        );
    }
}
