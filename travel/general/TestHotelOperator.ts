import {Component} from 'components/Component';

export default class TestHotelOperator extends Component {
    icon: Component;
    name: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.icon = new Component(browser, {parent: this.qa, current: 'icon'});
        this.name = new Component(browser, {parent: this.qa, current: 'name'});
    }
}
