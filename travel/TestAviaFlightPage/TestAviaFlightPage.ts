import {flightPage} from 'suites/avia';

import TestAviaFlightPageBookBlock from 'helpers/project/avia/pages/TestAviaFlightPage/TestAviaFlightPageBookBlock';

import {Page} from 'components/Page';
import {Component} from 'components/Component';

export class TestAviaFlightPage extends Page {
    title: Component;
    bookBlock: TestAviaFlightPageBookBlock;
    constructor(browser: WebdriverIO.Browser, qa: QA = 'flightPage') {
        super(browser, qa);
        this.title = new Component(this.browser, {
            parent: this.qa,
            current: 'title',
        });
        this.bookBlock = new TestAviaFlightPageBookBlock(this.browser, {
            parent: this.qa,
            current: 'book',
        });
    }

    async goToFlightPage(flight: string, date: string): Promise<void> {
        await this.browser.url(flightPage.url(flight, date));
    }

    async waitForLoaded(): Promise<void> {
        await this.title.waitForVisible();
    }
}
