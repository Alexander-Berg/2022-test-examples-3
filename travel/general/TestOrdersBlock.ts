import TestOrder from 'helpers/project/account/pages/TripPage/components/TestOrder';

import {Component, IConstructable} from 'components/Component';
import {ComponentArray} from 'components/ComponentArray';

export default class TestOrdersBlock<T extends TestOrder> extends Component {
    title: Component;
    orders: ComponentArray<T>;

    constructor(
        browser: WebdriverIO.Browser,
        qa: QA,
        orderComponent: IConstructable<T>,
    ) {
        super(browser, qa);

        this.title = new Component(this.browser, {
            parent: this.qa,
            current: 'title',
        });
        this.orders = new ComponentArray(
            this.browser,
            {
                parent: this.qa,
                current: 'order',
            },
            orderComponent,
        );
    }
}
