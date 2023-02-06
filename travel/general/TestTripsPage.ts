import {SECOND} from 'helpers/constants/dates';

import TestAccountMenu from 'helpers/project/account/components/TestAccountMenu';
import {TestErrorModal} from 'helpers/project/common/components/TestErrorModal';
import TestSearchOrder from 'helpers/project/account/pages/TripsPage/components/TestSearchOrder';

import TestTripsPageContent from './components/TestTripsPageContent';
import TestLayoutDefault from 'components/TestLayoutDefault/TestLayoutDefault';

export default class TestTripsPage extends TestLayoutDefault {
    readonly accountMenu: TestAccountMenu;
    readonly loadingErrorModal: TestErrorModal;
    readonly content: TestTripsPageContent;
    readonly searchOrder: TestSearchOrder;

    constructor(browser: WebdriverIO.Browser) {
        super(browser, 'tripsPage');

        this.accountMenu = new TestAccountMenu(browser, {
            parent: this.qa,
            current: 'accountMenu',
        });

        this.loadingErrorModal = new TestErrorModal(browser, {
            parent: this.qa,
            current: 'loadingErrorModal',
        });

        this.content = new TestTripsPageContent(browser, {
            parent: this.qa,
            current: 'content',
        });

        this.searchOrder = new TestSearchOrder(browser);
    }

    waitUntilLoaded(): Promise<void> {
        return this.content.loader.waitUntilLoaded(5 * SECOND);
    }

    async isSupportPhoneVisible(): Promise<boolean> {
        if (this.isTouch) {
            return this.footer.isSupportPhoneVisible();
        }

        return this.accountMenu.supportPhone.isVisible();
    }
}
