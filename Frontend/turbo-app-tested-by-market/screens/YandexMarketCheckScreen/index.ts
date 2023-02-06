import * as Loadable from 'react-loadable';

export const YandexMarketCheckScreen = Loadable({
    loader: () => import(
        /* webpackChunkName: "screens-yandex-market-check" */
        './YandexMarketCheckScreen')
        .then(module => {
            return module.YandexMarketCheckScreen;
        }),
    loading: () => null,
    modules: ['./YandexMarketCheckScreen'],
    webpack: () => [require.resolveWeak('./YandexMarketCheckScreen')],
});
