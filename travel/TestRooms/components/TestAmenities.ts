import {Component} from 'components/Component';
import {Button} from 'components/Button';

export default class TestAmenities extends Component {
    moreButton: Button;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.moreButton = new Button(browser, {
            parent: this.qa,
            current: 'moreButton',
        });
    }
}
