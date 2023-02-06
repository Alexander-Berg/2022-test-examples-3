import {Component} from 'components/Component';

export class TestTrainsTariffClassFilterItem extends Component {
    text: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.text = new Component(browser, {
            parent: this.qa,
            current: 'text',
        });
    }
}
