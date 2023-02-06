import {Filters} from '~/pages/CostsAndSales/spec/e2e/pageObjects';

import {getTestingShop, getUser, makeShotCase, makeShotSuite} from 'spec/utils';
import {PLATFORM_TYPE} from '@yandex-market/b2b-core/shared/constants';

const shop = getTestingShop('autotests-market-partner-web-00.yandex.ru');
const user = getUser('autotest-check-report');

export default makeShotSuite({
    suiteName: 'Detailization chart filter. Default',
    feature: 'Статистика по кликам',
    before(actions) {
        actions.setWindowSize(1500, 1000);
    },
    childSuites: [
        makeShotCase({
            id: 'marketmbi-1500',
            issue: 'MARKETPARTNER-7276',
            suiteName: 'Default filter value - by days',
            environment: 'testing',
            page: {
                route: 'market-partner:html:costs-and-sales:get',
                params: {
                    campaignId: shop.campaignId,
                    platformType: PLATFORM_TYPE.SHOP,
                },
            },
            user,
            selector: Filters.group,
        }),
    ],
});
