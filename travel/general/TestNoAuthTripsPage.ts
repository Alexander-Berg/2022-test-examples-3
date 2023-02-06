import TestOrderSearchForm from 'helpers/project/account/pages/OrdersSearch/components/TestOrdersSearchForm/TestOrderSearchForm';

import {Component} from 'components/Component';

export default class TestNoAuthTripsPage extends Component {
    readonly supportPhone: Component;
    readonly searchFormTitle: Component;
    readonly orderSearchForm: TestOrderSearchForm;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.searchFormTitle = new Component(this.browser, {
            parent: this.qa,
            current: 'searchFormTitle',
        });

        this.supportPhone = new Component(this.browser, {
            parent: this.qa,
            current: 'supportPhone',
        });

        this.orderSearchForm = new TestOrderSearchForm(browser, {
            parent: this.qa,
            current: 'orderSearchForm',
        });
    }
}
