/* eslint-disable global-require */

export const baseMockFunctionality = () => {
    const {
        CMS_SEARCH_LAYOUT,
    } = require('@self/root/src/spec/testament/search/catalogPage/layoutMock');

    jest.spyOn(require('@self/root/src/resolvers/search/layout/buildSearchLayout'), 'default')
        .mockImplementation(() => CMS_SEARCH_LAYOUT);

    /**
     * Router
     */
    const {mockRouterFabric} = require('@self/root/src/helpers/testament/mock');
    mockRouterFabric('router')({
        'external:yandex-passport': () => '//pass-test.yandex.ru',
        'market:index': '//market.yandex.ru',
    });
};
