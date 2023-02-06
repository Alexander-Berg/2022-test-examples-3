import Cookie from 'js-cookie';

import {TEST_TRAIN_DETAILS_COOKIE_NAME} from 'constants/testContext';

export function getMockImPath(): string | undefined {
    return Cookie.get(TEST_TRAIN_DETAILS_COOKIE_NAME);
}
