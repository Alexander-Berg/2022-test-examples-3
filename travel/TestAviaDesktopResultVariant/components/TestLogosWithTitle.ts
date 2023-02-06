import {Component} from 'components/Component';

export class TestLogosWithTitle extends Component {
    readonly logo: Component;
    readonly count: Component;
    readonly title: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.logo = new Component(browser, {parent: this.qa, current: 'icon'});
        this.count = new Component(browser, {
            parent: this.qa,
            current: 'count',
        });
        this.title = new Component(browser, {
            parent: this.qa,
            current: 'title',
        });
    }
}
