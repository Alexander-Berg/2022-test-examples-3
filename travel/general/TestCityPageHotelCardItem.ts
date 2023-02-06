import {Component} from 'components/Component';
import {Button} from 'components/Button';

export class TestCityPageHotelCardItem extends Component {
    buyButton: Button;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.buyButton = new Button(browser, {
            parent: this.qa,
            current: 'buyButton',
        });
    }
}
