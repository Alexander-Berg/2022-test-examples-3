import TestAccountApp from 'helpers/project/account/TestAccountApp';
import Account from 'helpers/project/common/passport/Account';
import {TestAviaApp} from 'helpers/project/avia/app/TestAviaApp';
import {TestHotelsBookApp} from 'helpers/project/hotels/app/TestHotelsBookApp/TestHotelsBookApp';
import {TestPrintForm} from 'helpers/project/common/TestPrintForm/TestPrintForm';
import getRelativePathName from 'helpers/utilities/getRelativePathName';
import {TestHotelsApp} from 'helpers/project/hotels/app/TestHotelsApp';
import {TestBusesApp} from 'helpers/project/buses/app/TestBusesApp';
import {TestTrainsApp} from 'helpers/project/trains/app/TestTrainsApp';

import {Component} from 'components/Component';

export default class TestApp extends Component {
    readonly accountApp: TestAccountApp;
    readonly aviaApp: TestAviaApp;
    readonly hotelsBookApp: TestHotelsBookApp;
    readonly printForm: TestPrintForm;
    readonly hotelsApp: TestHotelsApp;
    readonly trainsApp: TestTrainsApp;
    readonly busesApp: TestBusesApp;

    constructor(browser: WebdriverIO.Browser) {
        super(browser);

        this.accountApp = new TestAccountApp(browser);
        this.aviaApp = new TestAviaApp(browser);

        this.hotelsBookApp = new TestHotelsBookApp(browser);
        this.hotelsApp = new TestHotelsApp(browser);
        this.trainsApp = new TestTrainsApp(browser);
        this.busesApp = new TestBusesApp(browser);

        this.printForm = new TestPrintForm(browser);
    }

    async loginRandomAccount(): Promise<{login: string; password: string}> {
        const account = new Account();
        const {
            account: {login, password},
        } = await account.getOrCreate();

        await this.browser.login(login, password);

        return {
            login,
            password,
        };
    }

    async getPagePathname(): Promise<string> {
        return getRelativePathName(await this.browser.getUrl());
    }

    async goAviaIndexPage(): Promise<void> {
        await this.aviaApp.indexPage.goIndexPage();
    }
}
