import {mergeSuites, prepareSuite, makeSuite} from 'ginny';

// suites
import SideMenuMultiAuthSingleSuite from '@self/platform/spec/hermione/test-suites/blocks/SideMenu/multiAuthSingle';
// page-objects
import SideMenu from '@self/platform/spec/page-objects/widgets/core/SideMenuRedesign/SideMenu';
import Header from '@self/platform/spec/page-objects/widgets/core/Header';
import AccountsList from '@self/platform/spec/page-objects/widgets/parts/AccountsList/AccountsList';
import AccountRow from '@self/platform/spec/page-objects/widgets/parts/AccountsList/AccountRow';

export default makeSuite('Мультиавторизация.', {
    environment: 'kadavr',
    feature: 'Боковое меню',
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    sideMenu: () => this.createPageObject(SideMenu),
                    header: () => this.createPageObject(Header),
                    accountsList: () => this.createPageObject(AccountsList),
                    otherAccountRow: () => this.createPageObject(AccountRow, {
                        parent: this.accountsList,
                        root: `${AccountRow.root}:nth-child(1)`,
                    }),
                    // В тесте после второго залогина активным становится второй профиль
                    defaultAccountRow: () => this.createPageObject(AccountRow, {
                        parent: this.accountsList,
                        root: `${AccountRow.root}:nth-child(2)`,
                    }),
                });
                return this.browser.yaProfile('dzot61');
            },
            afterEach() {
                return this.browser.yaLogout();
            },
        },
        prepareSuite(SideMenuMultiAuthSingleSuite)
    ),
});
