// @flow

import type {RouteParams as PageParams} from '@self/root/src/entities/route';
export type MockRoutes = string | { [string]: string | (?$ReadOnly<PageParams>) => string };
export default (moduleName: string = require.resolve('@self/root/src/utils/router')) =>
    (routes: MockRoutes = 'https://market.yandex.ru/') => {
        jest.doMock(moduleName, () => ({
            buildUrl: jest.fn((pageId: string, params?: $ReadOnly<PageParams>): string | null => {
                if (typeof routes === 'string') {
                    return routes;
                }

                // eslint-disable-next-line no-prototype-builtins
                if (routes.hasOwnProperty(pageId)) {
                    const builder = routes[pageId];

                    if (typeof builder === 'function') {
                        return builder(params);
                    }

                    return builder;
                }

                return null;
            }),
            // Хорошо бы расширить, когда потребуется
            parseUrl: jest.fn((url: string) => null),
        }));

        return () => jest.unmock(moduleName);
    };
