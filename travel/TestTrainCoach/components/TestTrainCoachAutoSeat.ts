import {Button} from 'components/Button';
import {Component} from 'components/Component';

export default class TestTrainCoachAutoSeat extends Component {
    label: Component;
    button: Button;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.label = new Component(browser, {
            parent: this.qa,
            current: 'autoSeatLabel',
        });
        this.button = new Button(browser, {
            parent: this.qa,
            current: 'autoSeatButton',
        });
    }
}
