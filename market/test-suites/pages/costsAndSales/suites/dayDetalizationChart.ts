import {Chart} from '~/pages/CostsAndSales/spec/e2e/pageObjects';
import {oneDateData} from '~/pages/CostsAndSales/spec/e2e/data';
import {Preloader} from 'spec/pageObjects/levitan';

import {getTestingShop, getUser, makeKadavrCase, makeKadavrSuite} from 'spec/utils';
import {PLATFORM_TYPE, PRICE_CENTER_STATE_KEY} from '@yandex-market/b2b-core/shared/constants';

const shop = getTestingShop('autotests-market-partner-web-00.yandex.ru');
const user = getUser('autotest-check-report');

export default makeKadavrSuite({
    suiteName: 'Detailization by days chart filter. One day',
    feature: 'Статистика по кликам',
    state: {
        [PRICE_CENTER_STATE_KEY]: oneDateData,
    },
    page: {
        route: 'market-partner:html:costs-and-sales:get',
        params: {
            campaignId: shop.campaignId,
            platformType: PLATFORM_TYPE.SHOP,
            fromDate: '2020-02-13',
            toDate: '2020-02-13',
        },
    },
    user,
    before(actions) {
        actions.setWindowSize(1500, 1000);
    },
    childSuites: [
        makeKadavrCase({
            id: 'marketmbi-4889',
            issue: 'MARKETPARTNER-7276',
            suiteName: 'One day chart',
            selector: Chart.root,
            capture(actions) {
                actions.waitForElementToHide(Preloader.spinner, 10000);
                // ждем, пока дорисуется график
                actions.wait(5000);
                // doubleClick() не срабатывает, поэтому такая комбинация,
                // чтобы отобразился тултип на графике
                actions.click(Chart.root);
                actions.click(Chart.root);
            },
        }),
    ],
});
