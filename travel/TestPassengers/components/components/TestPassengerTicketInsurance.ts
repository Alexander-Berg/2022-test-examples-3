import {Component} from 'components/Component';
import {TestLink} from 'components/TestLink';
import {TestPrice} from 'components/TestPrice';
import {Button} from 'components/Button';

export default class TestPassengerTicketInsurance extends Component {
    price: TestPrice;
    detailsLink: TestLink;
    questionButton: Button;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.price = new TestPrice(browser, {
            parent: this.qa,
            current: 'price',
        });
        this.detailsLink = new TestLink(browser, {
            parent: this.qa,
            current: 'detailsLink',
        });
        this.questionButton = new Button(browser, {
            parent: this.qa,
            current: 'questionButton',
        });
    }
}
