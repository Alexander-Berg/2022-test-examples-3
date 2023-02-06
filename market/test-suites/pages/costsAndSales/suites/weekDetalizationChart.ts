import {Filters, Chart} from '~/pages/CostsAndSales/spec/e2e/pageObjects';
import {weekData} from '~/pages/CostsAndSales/spec/e2e/data';
import {Select} from 'spec/pageObjects/b2b';
import {Popup} from 'spec/pageObjects/components';
import {Preloader} from 'spec/pageObjects/levitan';

import {getTestingShop, getUser, makeKadavrCase, makeKadavrSuite} from 'spec/utils';
import {PLATFORM_TYPE, PRICE_CENTER_STATE_KEY} from '@yandex-market/b2b-core/shared/constants';

const shop = getTestingShop('autotests-market-partner-web-00.yandex.ru');
const user = getUser('autotest-check-report');

export default makeKadavrSuite({
    suiteName: 'Detailization by week chart filter',
    feature: 'Статистика по кликам',
    state: {
        [PRICE_CENTER_STATE_KEY]: weekData,
    },
    page: {
        route: 'market-partner:html:costs-and-sales:get',
        params: {
            campaignId: shop.campaignId,
            platformType: PLATFORM_TYPE.SHOP,
            fromDate: '2020-02-11',
            toDate: '2020-03-10',
        },
    },
    user,
    before(actions) {
        actions.setWindowSize(1500, 1000);
    },
    childSuites: [
        makeKadavrCase({
            id: 'marketmbi-4890',
            issue: 'MARKETPARTNER-7276',
            suiteName: 'Week detalization chart',
            selector: Chart.root,
            capture(actions, find) {
                actions.waitForElementToHide(Preloader.spinner, 10000);
                actions.click(Filters.group);
                actions.waitForElementToShow(Popup.root);

                const weekOption = find(Select.popupItem(2));
                actions.click(weekOption);
                actions.click(Chart.root);
                actions.waitForElementToHide(Preloader.spinner);
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
