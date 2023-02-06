import {mergeSuites, prepareSuite, makeSuite} from 'ginny';

// suites
import MultiAuthSingleSuite from '@self/platform/spec/hermione/test-suites/blocks/header2-nav/multiAuthSingle';
import MultiAuthManySuite from '@self/platform/spec/hermione/test-suites/blocks/header2-nav/multiAuthMany';
import AccountsListSuite from '@self/platform/spec/hermione/test-suites/blocks/AccountsList';
// page-objects
import Header2Nav from '@self/platform/spec/page-objects/header2-nav';
import AccountRow from '@self/platform/spec/page-objects/widgets/parts/AccountsList/AccountRow';

import {profiles} from '@self/platform/spec/hermione/configs/profiles';

export default makeSuite('Мультиавторизация.', {
    environment: 'kadavr',
    feature: 'Авторизация',
    params: {
        pageId: 'Страница, на которой проверяем',
    },
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    headerNav: () => this.createPageObject(Header2Nav),
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
                return this.browser.yaProfile('pan-topinambur', this.params.pageId);
            },
            afterEach() {
                return this.browser.yaLogout();
            },
        },
        prepareSuite(MultiAuthSingleSuite),
        prepareSuite(MultiAuthManySuite, {
            hooks: {
                async beforeEach() {
                    return this.browser.yaProfile('ugctest3', this.params.pageId);
                },
                async afterEach() {
                    return this.browser.yaLogout();
                },
            },
        }),
        prepareSuite(AccountsListSuite, {
            params: {
                defaultUid: profiles.ugctest3.uid,
                otherUid: profiles['pan-topinambur'].uid,
            },
            hooks: {
                async beforeEach() {
                    return this.browser.yaProfile('ugctest3', this.params.pageId);
                },
                async afterEach() {
                    return this.browser.yaLogout();
                },
            },
        })
    ),
});
