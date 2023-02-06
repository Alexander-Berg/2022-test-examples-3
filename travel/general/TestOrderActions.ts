import {Component} from 'components/Component';
import {Button} from 'components/Button';

export class TestOrderActions extends Component {
    printButton: Button;
    downloadButton: Button;
    detailsLink: Component;
    cancelButton: Button;

    constructor(browser: WebdriverIO.Browser, qa: QA = 'orderActions') {
        super(browser, qa);

        this.printButton = new Button(browser, {
            parent: this.qa,
            current: 'printButton',
        });
        this.downloadButton = new Button(browser, {
            parent: this.qa,
            current: 'downloadButton',
        });
        this.detailsLink = new Component(browser, {
            parent: this.qa,
            current: 'detailsLink',
        });
        this.cancelButton = new Button(browser, {
            parent: this.qa,
            current: 'cancelButton',
        });
    }
}
