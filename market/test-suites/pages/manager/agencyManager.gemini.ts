import {makeShotCase, makeShotSuite, getTestingShop} from 'spec/utils';
import UserMenu from 'spec/pageObjects/UserMenu';
import {PLATFORM_TYPE} from '@yandex-market/b2b-core/shared/constants';
import App from './pageObjects/App';

const shopAgency = getTestingShop('autotestmarket-6506.yandex.ru');

export default makeShotSuite({
    suiteName: 'Agency CRM link',
    feature: 'CRM',
    childSuites: [
        makeShotCase({
            suiteName: 'Agency manager page.',
            id: 'marketmbi-2519',
            issue: 'MARKETPARTNER-10064',
            environment: 'testing',
            selector: App.crmCell,
            user: shopAgency.contacts.agencyManager,
            page: {
                route: 'market-partner:html:manager-partner-list:get',
                params: {
                    id: shopAgency.campaignId,
                    platformType: 'manager',
                },
            },
        }),
        makeShotCase({
            suiteName: 'Agency manager. Multipassport',
            id: 'marketmbi-2520',
            issue: 'MARKETPARTNER-10064',
            environment: 'testing',
            selector: UserMenu.menu,
            page: {
                route: 'market-partner:html:shops-dashboard:get',
                params: {campaignId: shopAgency.campaignId, platformType: PLATFORM_TYPE.SHOP, euid: 404744950},
            },
            user: shopAgency.contacts.agencyManager,
            capture(actions) {
                actions.click(App.avatar);
                /**
                 * Меню появляется с анимацией, длящейся .3s,
                 * если делать скриншот сразу, как только стала видна ссылка на CRM,
                 * то получаем плавающий тест
                 */
                actions.wait(1000);
                actions.waitForElementToShow(App.crm, 500);
            },
        }),
    ],
});
