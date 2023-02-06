import {getTestingShop, getUser, makeShotCase, makeShotSuite} from 'spec/utils';
import UserMenu from 'spec/pageObjects/UserMenu';
import {PLATFORM_TYPE} from '@yandex-market/b2b-core/shared/constants';

const shop = getTestingShop('auction-autotest.yandex.ru');
const euid = shop.contacts.owner.uid;
const user = getUser('autotestreader');

const suites = [
    {
        name: 'Manager page',
        id: 'marketmbi-2509',
        page: {
            route: 'market-partner:html:manager-partner-list:get',
            params: {
                platformType: 'manager',
            },
        },
    },
    {
        name: 'Order page',
        id: 'marketmbi-2510',
        page: {
            route: 'market-partner:html:shops-dashboard:get',
            params: {campaignId: shop.campaignId, platformType: PLATFORM_TYPE.SHOP, euid},
        },
    },
];

export default makeShotSuite({
    suiteName: 'Link to CRM from the user menu',
    feature: 'CRM',
    childSuites: suites.map(suite =>
        makeShotCase({
            suiteName: suite.name,
            // @ts-expect-error(TS2322) найдено в рамках MARKETPARTNER-16237
            page: suite.page,
            user,
            environment: 'testing',
            id: suite.id,
            issue: 'MARKETPARTNER-10050',
            selector: UserMenu.menu,
            capture(actions, find) {
                // @ts-expect-error(TS2339) найдено в рамках MARKETPARTNER-16237
                this.userMenuTrigger = find(UserMenu.trigger);
                // @ts-expect-error(TS2554) найдено в рамках MARKETPARTNER-16237
                actions.click(this.userMenuTrigger);
                actions.wait(300);
            },
        }),
    ),
});
