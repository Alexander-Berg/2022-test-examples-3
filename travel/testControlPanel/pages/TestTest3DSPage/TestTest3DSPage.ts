import TestTest3DSDemoPage from 'helpers/project/testControlPanel/pages/TestTest3DSPage/pages/TestTest3DSDemoPage';
import TestTest3DSFramePage from 'helpers/project/testControlPanel/pages/TestTest3DSPage/pages/TestTest3DSFramePage';
import TestTest3DSExternalDemoPage from 'helpers/project/testControlPanel/pages/TestTest3DSPage/pages/TestTest3DSExternalDemoPage';

import {Component} from 'components/Component';

export default class TestTest3DSPage extends Component {
    test3DSDemoPage: TestTest3DSDemoPage;
    test3DSFramePage: TestTest3DSFramePage;
    test3DSExternalDemoPage: TestTest3DSExternalDemoPage;

    constructor(browser: WebdriverIO.Browser) {
        super(browser, 'test3DSPage');

        this.test3DSDemoPage = new TestTest3DSDemoPage(browser);
        this.test3DSFramePage = new TestTest3DSFramePage(browser);
        this.test3DSExternalDemoPage = new TestTest3DSExternalDemoPage(browser);
    }
}
