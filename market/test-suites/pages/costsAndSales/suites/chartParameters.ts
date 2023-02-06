import {Filters} from '~/pages/CostsAndSales/spec/e2e/pageObjects';
import {commonData} from '~/pages/CostsAndSales/spec/e2e/data';
import {Preloader} from 'spec/pageObjects/levitan';
import {Popup} from 'spec/pageObjects/components';

import {isProduction, getTestingShop, getProductionShop, getUser, makeKadavrCase, makeKadavrSuite} from 'spec/utils';
import {PLATFORM_TYPE, PRICE_CENTER_STATE_KEY} from '@yandex-market/b2b-core/shared/constants';
import {clickOnOption} from './utils';

const shop = isProduction
    ? getProductionShop('autotests-market-partner-web1.yandex.ru')
    : getTestingShop('autotests-market-partner-web-00.yandex.ru');
const user = getUser('autotest-check-report');

const commonProps = {
    id: 'marketmbi-1501',
    issue: 'MARKETPARTNER-7276',
};

export default makeKadavrSuite({
    suiteName: 'Indicators filter on chart (for chart)',
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
            suiteName: 'Update chart with all parameters',
            selector: Popup.root,
            capture(actions, find) {
                actions.click(Filters.parameterSelector);
                actions.waitForElementToShow(Popup.root);

                for (let i = 1; i < 10; i++) {
                    if (i !== 2) {
                        clickOnOption(i, actions, find);
                    }
                }

                actions.waitForElementToHide(Preloader.spinner);
            },
        }),
        makeKadavrCase({
            ...commonProps,
            suiteName: 'Update chart with delete parameters',
            selector: Popup.root,
            capture(actions, find) {
                actions.click(Filters.parameterSelector);
                actions.waitForElementToShow(Popup.root);

                for (let i = 1; i < 10; i++) {
                    if (i !== 2) {
                        clickOnOption(i, actions, find);
                    }
                }

                for (let i = 1; i < 3; i++) {
                    clickOnOption(i, actions, find);
                }

                actions.waitForElementToHide(Preloader.spinner);
            },
        }),
    ],
});
