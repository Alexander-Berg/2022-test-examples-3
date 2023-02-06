import {Component} from 'components/Component';

export class TestTransferSeparator extends Component {
    description: Component;
    duration: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.description = new Component(browser, {
            parent: this.qa,
            current: 'description',
        });

        this.duration = new Component(browser, {
            parent: this.qa,
            current: 'duration',
        });
    }
}
