import TestBusesSearchSegment from 'helpers/project/buses/components/TestBusesSearchSegments/components/TestBusesSearchSegment/TestBusesSearchSegment';

import {Component} from 'components/Component';
import {ComponentArray} from 'components/ComponentArray';

export default class TestBusesSearchSegments extends Component {
    items: ComponentArray<TestBusesSearchSegment>;
    skeletons: ComponentArray<Component>;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.items = new ComponentArray(
            browser,
            {
                parent: this.qa,
                current: 'item',
            },
            TestBusesSearchSegment,
        );

        this.skeletons = new ComponentArray(
            browser,
            {
                parent: this.qa,
                current: 'skeleton',
            },
            Component,
        );
    }

    async isItemsDisplayedCorrectly(): Promise<boolean> {
        return this.items.every(item => item.isDisplayedCorrectly());
    }
}
