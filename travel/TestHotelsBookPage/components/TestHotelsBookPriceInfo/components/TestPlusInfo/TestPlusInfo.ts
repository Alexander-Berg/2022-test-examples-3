import {Component} from 'components/Component';
import {Button} from 'components/Button';

export default class TestPlusInfo extends Component {
    readonly label: Component;
    readonly triggerDetailsButton: Button;
    readonly discountPrice: Component;
    readonly plusPoints: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.label = new Component(browser, {
            parent: this.qa,
            current: 'label',
        });
        this.triggerDetailsButton = new Button(browser, {
            parent: this.qa,
            current: 'triggerDetailsButton',
        });
        this.discountPrice = new Component(browser, {
            parent: this.qa,
            current: 'discountPrice',
        });
        this.plusPoints = new Component(browser, {
            parent: this.qa,
            current: 'plusPoints',
        });
    }
}
