// @flow

import {invariant} from '@yandex-market/invariant';

/**
 * Урл в окружении Тестамента выглядит не как урл
 * Единственное оружие -- унифицировать слой доступа
 */

export const splitTestingUrlByPath = (testingUrl: string) =>
    testingUrl.split(/_(.*)/);

export const parseTestingUrl = (testingUrl: string) => {
    invariant(typeof testingUrl === 'string' && testingUrl !== '', 'testingUrl must be string');
    const [pageId, serializedParams] = splitTestingUrlByPath(testingUrl);

    return {
        pageId,
        // $FlowFixMe String#replaceAll
        searchParams: JSON.parse(serializedParams.replaceAll('\'', '"')),
    };
};
