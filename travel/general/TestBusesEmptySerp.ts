import {Component} from 'components/Component';

export default class TestBusesEmptySerp extends Component {
    title: Component;
    text: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA = 'emptySerp') {
        super(browser, qa);

        this.title = new Component(browser, {
            parent: this.qa,
            current: 'title',
        });

        this.text = new Component(browser, {
            parent: this.qa,
            current: 'text',
        });
    }
}
