import {Component} from 'components/Component';
import {Loader} from 'components/Loader';
import {Button} from 'components/Button';
import {TestPrice} from 'components/TestPrice';

export default class TestOrderOrchActionModal extends Component {
    loader: Loader;
    price: TestPrice;
    submitButton: Button;
    cancelButton: Button;

    constructor(browser: WebdriverIO.Browser) {
        super(browser, 'orderTrainsOrchActionModal');

        this.loader = new Loader(browser, {
            parent: this.qa,
            current: 'loader',
        });
        this.price = new TestPrice(browser, {
            parent: this.qa,
            current: 'price',
        });
        this.submitButton = new Button(browser, {
            parent: this.qa,
            current: 'submitButton',
        });
        this.cancelButton = new Button(browser, {
            parent: this.qa,
            current: 'cancelButton',
        });
    }
}
