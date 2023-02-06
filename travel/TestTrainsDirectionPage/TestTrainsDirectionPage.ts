import {TestTrainsBaseSearchPage} from 'helpers/project/trains/pages/TestTrainsBaseSearchPage';

import {Component} from 'components/Component';

export class TestTrainsDirectionPage extends TestTrainsBaseSearchPage {
    title: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA = 'directionPage') {
        super(browser, qa);

        this.title = new Component(browser, {
            parent: this.qa,
            current: 'header-title',
        });
    }
}
