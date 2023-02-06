import TestTitleAndDates from 'helpers/project/account/pages/TripPage/components/TestTitleAndDates';
import TestOrder from 'helpers/project/account/pages/TripPage/components/TestOrder';
import TestDescriptionAndActions from 'helpers/project/account/pages/TripPage/components/TestTrainOrder/components/TestDescriptionAndActions';

import {Component} from 'components/Component';

export default class TestTrainOrder extends TestOrder {
    titleAndDates: TestTitleAndDates;
    descriptionAndActions: TestDescriptionAndActions;
    cancelCaption: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.titleAndDates = new TestTitleAndDates(this.browser, {
            parent: this.qa,
            current: 'titleAndDates',
        });
        this.descriptionAndActions = new TestDescriptionAndActions(
            this.browser,
            {
                parent: this.qa,
                current: 'descriptionAndActions',
            },
        );
        this.cancelCaption = new Component(this.browser, {
            parent: this.qa,
            current: 'cancelCaption',
        });
    }
}
