import {every} from 'p-iteration';

import {Component} from 'components/Component';
import {TestNavigation} from 'components/TestNavigation';

export type TTestNavigationLinkType = 'avia' | 'trains' | 'hotels' | 'bus';

export class TestNavigations extends Component {
    readonly avia: TestNavigation;
    readonly trains: TestNavigation;
    readonly hotels: TestNavigation;
    readonly bus: TestNavigation;

    constructor(browser: WebdriverIO.Browser, qa: QA = 'navigation') {
        super(browser, qa);

        this.avia = new TestNavigation(browser, {
            parent: this.qa,
            current: 'avia',
        });
        this.trains = new TestNavigation(browser, {
            parent: this.qa,
            current: 'trains',
        });
        this.hotels = new TestNavigation(browser, {
            parent: this.qa,
            current: 'hotels',
        });
        this.bus = new TestNavigation(browser, {
            parent: this.qa,
            current: 'buses',
        });
    }

    async areDisplayed(order: TTestNavigationLinkType[]): Promise<boolean> {
        return every(order, name => this[name].isDisplayed());
    }
}
