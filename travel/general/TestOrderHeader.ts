import {Component} from 'components/Component';

export class TestOrderHeader extends Component {
    successBadge: Component;
    numberBlock: Component;
    title: Component;
    status: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA = 'orderHeader') {
        super(browser, qa);

        this.successBadge = new Component(browser, {
            parent: this.qa,
            current: 'successBadge',
        });

        this.numberBlock = new Component(browser, {
            parent: this.qa,
            current: 'numberBlock',
        });

        this.title = new Component(browser, {
            parent: this.qa,
            current: 'title',
        });

        this.status = new Component(browser, {
            parent: this.qa,
            current: 'status',
        });
    }
}
