import {Filters, App, Chart} from '~/pages/CostsAndSales/spec/e2e/pageObjects';
import {commonData} from '~/pages/CostsAndSales/spec/e2e/data';
import {Preloader} from 'spec/pageObjects/levitan';
import {Popup} from 'spec/pageObjects/b2b';

import {isProduction, getTestingShop, getProductionShop, getUser, makeKadavrCase, makeKadavrSuite} from 'spec/utils';
import {PLATFORM_TYPE, PRICE_CENTER_STATE_KEY} from '@yandex-market/b2b-core/shared/constants';
import {clickOnOption} from './utils';

const shop = isProduction
    ? getProductionShop('autotests-market-partner-web1.yandex.ru')
    : getTestingShop('autotests-market-partner-web-00.yandex.ru');
const user = getUser('autotest-check-report');

const commonProps = {
    id: 'marketmbi-1463',
    issue: 'MARKETPARTNER-7276',
};

export default makeKadavrSuite({
    suiteName: 'Parameters filter values',
    feature: 'Статистика по кликам',
    state: {
        [PRICE_CENTER_STATE_KEY]: commonData,
    },
    page: {
        route: 'market-partner:html:costs-and-sales:get',
        params: {
            campaignId: shop.campaignId,
            platformType: PLATFORM_TYPE.SHOP,
            fromDate: '2019-12-15',
            toDate: '2020-01-12',
        },
    },
    user,
    before(actions) {
        actions.setWindowSize(1500, 1000);
    },
    childSuites: [
        makeKadavrCase({
            ...commonProps,
            suiteName: 'Default value: clicks',
            selector: Popup.root,
            capture(actions) {
                actions.click(Filters.parameterSelector);
                actions.waitForElementToShow(Popup.root);
            },
        }),
        makeKadavrCase({
            ...commonProps,
            suiteName: 'Offers with all parameters',
            selector: Chart.root,
            capture(actions, find) {
                actions.waitForElementToHide(Preloader.spinner, 10000);
                actions.click(Filters.parameterSelector);
                actions.waitForElementToShow(Popup.root);

                for (let i = 2; i < 9; i++) {
                    clickOnOption(i, actions, find);
                }

                for (let i = 1; i < 6; i++) {
                    actions.click(find(App.checkboxRowByIndex(i)));
                    actions.waitForElementToHide(Preloader.spinner, 5000);
                }
                actions.waitForElementToHide(Preloader.spinner, 10000);
            },
        }),
    ],
});
