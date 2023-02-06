import {Component} from 'components/Component';
import {TestModal} from 'components/TestModal';

export default class TestMapModal extends TestModal {
    title: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.title = new Component(this.browser, {
            parent: this.qa,
            current: 'title',
        });
    }
}
