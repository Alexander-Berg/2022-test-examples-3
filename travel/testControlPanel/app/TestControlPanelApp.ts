import TestTest3DSPage from 'helpers/project/testControlPanel/pages/TestTest3DSPage/TestTest3DSPage';
import TestTest3DSExternalDemoPage from 'helpers/project/testControlPanel/pages/TestTest3DSPage/pages/TestTest3DSExternalDemoPage';

export default class TestControlPanelApp {
    private browser: WebdriverIO.Browser;
    private test3DSPage: TestTest3DSPage;

    constructor(browser: WebdriverIO.Browser) {
        this.browser = browser;

        this.test3DSPage = new TestTest3DSPage(browser);
    }

    async goToTest3DSPage(): Promise<TestTest3DSPage> {
        await this.browser.url('/test/3ds/');

        return this.test3DSPage;
    }

    async goToTest3DSExternalDemoPage(): Promise<TestTest3DSExternalDemoPage> {
        await this.browser.url('/test/3ds/external');

        return this.test3DSPage.test3DSExternalDemoPage;
    }
}
