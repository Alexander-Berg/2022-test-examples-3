import {SECOND} from 'helpers/constants/dates';

import {Component} from 'components/Component';
import {TestUserInfo} from 'components/TestUserInfo';
import {TestNavigations} from 'components/TestNavigations';
import {TestSearchInformation} from 'components/TestSearchInformation';
import {TestNavigationSideSheet} from 'components/TestNavigationSideSheet/TestNavigationSideSheet';

export class TestHeader extends Component {
    readonly userInfo: TestUserInfo;
    readonly portalLogo: Component;
    readonly navigations: TestNavigations;
    readonly searchInformation: TestSearchInformation;
    readonly heartIcon: Component;
    readonly navigationSideSheet: TestNavigationSideSheet;

    constructor(browser: WebdriverIO.Browser, qa: QA = 'portalHeader') {
        super(browser, qa);

        this.portalLogo = new Component(browser, {
            parent: this.qa,
            current: 'portalLogo',
        });
        this.navigations = new TestNavigations(browser, {
            parent: this.qa,
            current: 'navigation',
        });
        this.searchInformation = new TestSearchInformation(browser);
        this.userInfo = new TestUserInfo(browser, {
            parent: this.qa,
            current: 'userInfo',
        });
        this.heartIcon = new Component(browser, 'heartIcon');
        this.navigationSideSheet = new TestNavigationSideSheet(browser, {
            parent: this.qa,
            current: 'navigationSideSheet',
        });
    }

    async openSearchForm(): Promise<void> {
        await this.searchInformation.click();

        await this.browser.pause(SECOND);
    }

    async clickTripsLink(): Promise<void> {
        if (!this.isTouch) {
            await this.userInfo.accountLink.click();

            return;
        }

        await this.navigationSideSheet.toggleButton.click();
        await this.navigationSideSheet.tripsLink.click();
    }
}
