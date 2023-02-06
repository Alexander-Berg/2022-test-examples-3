import {Component} from 'components/Component';

export default class TestAdvantage extends Component {
    icon: Component;
    title: Component;
    description: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.icon = new Component(this.browser, {
            parent: this.qa,
            current: 'icon',
        });

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
