import {Component} from 'components/Component';
import {Button} from 'components/Button';

export class TestFooter extends Component {
    readonly supportPhone: Component;
    readonly supportPhoneButton: Button;

    constructor(browser: WebdriverIO.Browser) {
        super(browser, 'portalFooter');

        this.supportPhone = new Component(browser, {
            parent: this.qa,
            current: 'supportPhone',
        });
        this.supportPhoneButton = new Button(browser, {
            parent: this.qa,
            current: 'supportPhoneButton',
        });
    }

    async isSupportPhoneVisible(): Promise<boolean> {
        return (
            (await this.supportPhone.isVisible()) &&
            (await this.supportPhoneButton.isVisible())
        );
    }
}
