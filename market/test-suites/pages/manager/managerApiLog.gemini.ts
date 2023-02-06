import {Preloader} from 'spec/pageObjects/levitan';
import {BaseFilters} from '~/pages/ManagerApiLog/spec/e2e/pageObjects';
import {apiLog} from '~/pages/ManagerApiLog/spec/e2e/data/apiLog';
import {apiResources} from '~/pages/ManagerApiLog/spec/e2e/data/apiResources';

import {makeKadavrSuite, makeKadavrCase, getUser} from 'spec/utils';
import {MBI_PARTNER_STATE_KEY} from '@yandex-market/b2b-core/shared/constants';
import App from './pageObjects/App';

const testManager = getUser('autotestmanager');

// @ts-expect-error(TS7006) найдено в рамках MARKETPARTNER-16237
const getFirstPageElements = apiLogResponse => {
    const [log, users, pager] = apiLogResponse.result;
    const firstPageElements = log.slice(0, pager.pageSize);

    return {
        result: [firstPageElements, users, pager],
    };
};

export default makeKadavrSuite({
    suiteName: 'Manager API Log',
    feature: 'Manager API Log',
    state: {
        [MBI_PARTNER_STATE_KEY]: {
            apiLog: getFirstPageElements(apiLog),
            apiResources,
        },
    },
    user: testManager,
    page: {
        route: 'market-partner:html:manager-api-log:get',
        params: {
            platformType: 'manager',
        },
    },
    before(actions) {
        actions.setWindowSize(1500, 1000);
        actions.wait(5000);
    },
    childSuites: [
        makeKadavrCase({
            id: 'marketmbi-3428',
            suiteName: 'Page display',
            issue: 'MARKETPARTNER-12272',
            selector: App.root,
            capture(actions, find) {
                const submitFiltersButton = find(BaseFilters.submitFilters);
                actions.click(submitFiltersButton);
                actions.waitForElementToHide(Preloader.spinner);
            },
        }),
    ],
});
