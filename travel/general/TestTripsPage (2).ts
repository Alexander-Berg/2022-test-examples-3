import {SECOND} from 'helpers/constants/dates';

import TestAccountMenu from 'helpers/project/account/components/TestAccountMenu';
import {TestErrorModal} from 'helpers/project/common/components/TestErrorModal';
import TestSearchOrder from 'helpers/project/account/pages/TripsPage/components/TestSearchOrder';
import TestNoAuthTripsPage from 'helpers/project/account/pages/TripsPage/components/TestNoAuthTripsPage';

import {Component} from 'components/Component';
import {TestFooter} from 'components/TestFooter';
import TestTripsPageContent from './components/TestTripsPageContent';
import {Loader} from 'components/Loader';

export default class TestTripsPage extends Component {
    readonly accountMenu: TestAccountMenu;
    readonly loadingErrorModal: TestErrorModal;
    readonly footer: TestFooter;
    readonly content: TestTripsPageContent;
    readonly loader: Loader;
    readonly searchOrder: TestSearchOrder;
    readonly noAuthTripsPage: TestNoAuthTripsPage;

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

        this.loader = new Loader(browser, {parent: this.qa, current: 'loader'});

        this.footer = new TestFooter(browser);

        this.searchOrder = new TestSearchOrder(browser);

        this.noAuthTripsPage = new TestNoAuthTripsPage(browser, {
            parent: this.qa,
            current: 'noAuthTripsPage',
        });
    }

    waitUntilLoaded(): Promise<void> {
        return this.loader.waitUntilLoaded(5 * SECOND);
    }

    async isSupportPhoneVisible(): Promise<boolean> {
        if (this.isTouch) {
            return this.footer.isSupportPhoneVisible();
        }

        return this.accountMenu.supportPhone.isVisible();
    }
}
