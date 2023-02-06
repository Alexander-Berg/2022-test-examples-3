import {getUser, getTestingShop, makeShotCase, makeShotSuite} from 'spec/utils';
import AppBar from 'spec/pageObjects/AppBar';

import AdminMenu from 'spec/pageObjects/AdminMenu';
import UserMenu from 'spec/pageObjects/UserMenu';

const manager = getUser('autotestmanager');
const shop = getTestingShop('autotests-market-partner-web-00.yandex.ru');
const euid = shop.contacts.owner.uid;
const managerId = manager.uid;

export default makeShotSuite({
    before(actions) {
        actions.setWindowSize(1400, 1000);
    },
    feature: 'Менеджерский раздел',
    suiteName: 'ManagerMenu',
    childSuites: [
        makeShotCase({
            id: 'marketmbi-3149',
            issue: 'MARKETPARTNER-11591',
            suiteName: 'ManagerMenuElement',
            environment: 'testing',
            user: manager,
            page: {
                route: 'market-partner:html:manager-partner-list:get',
                params: {managerId, platformType: 'manager'},
            },
            selector: UserMenu.menu,
            capture(actions, find) {
                const menuTrigger = find(AdminMenu.menuTrigger);
                actions.click(menuTrigger);
            },
        }),

        makeShotCase({
            id: 'marketmbi-3074',
            issue: 'MARKETPARTNER-11591',
            suiteName: 'ManagerHeaderNoEuid',
            environment: 'testing',
            user: manager,
            page: {
                route: 'market-partner:html:manager-partner-list:get',
                params: {managerId, platformType: 'manager'},
            },
            selector: AppBar.root,
        }),

        makeShotCase({
            id: 'marketmbi-3079',
            issue: 'MARKETPARTNER-11591',
            suiteName: 'ManagerHeaderEuid',
            environment: 'testing',
            user: manager,
            page: {
                route: 'market-partner:html:manager-partner-list:get',
                params: {
                    managerId,
                    euid,
                    platformType: 'manager',
                },
            },
            selector: AppBar.root,
        }),
    ],
});
