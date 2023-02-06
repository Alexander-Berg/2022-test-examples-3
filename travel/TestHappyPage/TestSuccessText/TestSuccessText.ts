import {Component} from 'components/Component';

export default class TestSuccessText extends Component {
    title: Component;
    description: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.title = new Component(this.browser, {
            parent: this.qa,
            current: 'title',
        });
        this.description = new Component(this.browser, {
            parent: this.qa,
            current: 'description',
        });
    }
}
