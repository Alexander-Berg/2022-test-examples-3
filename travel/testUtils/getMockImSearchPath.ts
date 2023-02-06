import Cookie from 'js-cookie';

import {TEST_TRAIN_SEARCH_COOKIE_NAME} from 'constants/testContext';

export function getMockImSearchPath(): string | undefined {
    return Cookie.get(TEST_TRAIN_SEARCH_COOKIE_NAME);
}
