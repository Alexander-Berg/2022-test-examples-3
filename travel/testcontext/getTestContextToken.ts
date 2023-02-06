import Cookie from 'js-cookie';

import {BUSES_TEST_CONTEXT_TOKEN_COOKIE_NAME} from 'constants/testContext';

import appData from 'utilities/appData/appData';
import {getQueryByBrowserHistory} from 'utilities/getQueryByBrowserHistory/getQueryByBrowserHistory';

export function getTestContextTokens(): string | undefined {
    if (appData.isProductionEnv) {
        return undefined;
    }

    const {[BUSES_TEST_CONTEXT_TOKEN_COOKIE_NAME]: busesTestContextToken} =
        getQueryByBrowserHistory();

    if (busesTestContextToken) {
        return Array.isArray(busesTestContextToken)
            ? busesTestContextToken[0]
            : busesTestContextToken;
    }

    return Cookie.get(BUSES_TEST_CONTEXT_TOKEN_COOKIE_NAME);
}
