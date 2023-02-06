import {Button} from './Button';
import {Component} from './Component';
import {TestModal} from './TestModal';

export class TestErrorModal extends TestModal {
    readonly text: Component;
    readonly secondaryActionButton: Button;
    readonly primaryActionButton: Button;
    readonly content: Component;
    readonly title: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA = 'errorModal') {
        super(browser, qa);

        this.title = new Component(browser, {
            parent: this.qa,
            current: 'title',
        });
        this.text = new Component(browser, {
            parent: this.qa,
            current: 'text',
        });
        this.primaryActionButton = new Button(browser, {
            parent: this.qa,
            current: 'primaryAction',
        });
        this.secondaryActionButton = new Button(browser, {
            parent: this.qa,
            current: 'secondaryAction',
        });

        this.content = new Component(browser, {
            parent: this.qa,
            current: 'content',
        });
    }
}
