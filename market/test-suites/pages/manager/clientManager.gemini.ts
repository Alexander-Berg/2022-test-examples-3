import {makeShotCase, makeShotSuite, getTestingShop} from 'spec/utils';
import UserMenu from 'spec/pageObjects/UserMenu';
import {PLATFORM_TYPE} from '@yandex-market/b2b-core/shared/constants';
import App from './pageObjects/App';

const shop = getTestingShop('autotestmarket-6506.yandex.ru');

export default makeShotSuite({
    suiteName: 'Manager CRM link',
    feature: 'CRM',
    childSuites: [
        makeShotCase({
            suiteName: 'Manager page. White shop',
            id: 'marketmbi-2517',
            issue: 'MARKETPARTNER-10064',
            environment: 'testing',
            selector: App.crmCell,
            page: {
                route: 'market-partner:html:manager-partner-list:get',
                params: {managerId: 0, query: shop.campaignId, platformType: 'manager'},
            },
            user: shop.contacts.manager,
        }),
        makeShotCase({
            suiteName: 'Manager page. Subclient',
            id: 'marketmbi-2707',
            issue: 'MARKETPARTNER-10064',
            environment: 'testing',
            selector: App.crmCell,
            page: {
                route: 'market-partner:html:manager-partner-list:get',
                params: {managerId: 0, query: 1037133, platformType: 'manager'},
            },
            user: shop.contacts.manager,
        }),
        makeShotCase({
            suiteName: 'Manager. Multipassport',
            id: 'marketmbi-2518',
            issue: 'MARKETPARTNER-10064',
            environment: 'testing',
            selector: UserMenu.menu,
            page: {
                route: 'market-partner:html:shops-dashboard:get',
                params: {campaignId: shop.campaignId, platformType: PLATFORM_TYPE.SHOP, euid: 404744950},
            },
            user: shop.contacts.manager,
            capture(actions) {
                actions.click(UserMenu.trigger);
                actions.wait(1000);
                actions.waitForElementToShow(App.crm, 500);
            },
        }),
    ],
});
