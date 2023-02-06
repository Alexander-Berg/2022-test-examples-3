import {Component} from 'components/Component';

export default class TestFieldLabel extends Component {
    label: Component;
    value: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.label = new Component(browser, {
            parent: this.qa,
            current: 'label',
        });
        this.value = new Component(browser, {
            parent: this.qa,
            current: 'value',
        });
    }
}
