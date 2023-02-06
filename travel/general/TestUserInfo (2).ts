import {ElementClass} from 'helpers/project/common/ElementClass/ElementClass';

import {Component} from 'components/Component';

export class TestUserInfo extends Component {
    readonly accountLink: Component;
    readonly login: Component;
    readonly plus: Component;
    readonly favoriteLink: Component;

    constructor(browser: WebdriverIO.Browser, qa: QA) {
        super(browser, qa);

        this.accountLink = new Component(browser, {
            parent: this.qa,
            current: 'accountLink',
        });
        this.plus = new Component(browser, {
            parent: this.qa,
            current: 'plus',
        });
        this.favoriteLink = new Component(browser, {
            parent: this.qa,
            current: 'favoriteLink',
        });
        this.login = new Component(browser, {
            parent: this.qa,
            current: 'login',
        });
    }

    get user(): Promise<ElementClass> {
        return this.getElement(`${this.prepareQaSelector(this.qa)} .user2`);
    }
}
