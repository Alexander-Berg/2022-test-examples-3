import * as Loadable from 'react-loadable';

export const YandexMarketCheck = Loadable({
    loader: () => import(
        /* webpackChunkName: "experiment-turbo-app-tested-by-market" */
        './YandexMarketCheck')
        .then(module => {
            return module.YandexMarketCheck;
        }),
    loading: () => null,
    modules: ['./YandexMarketCheck'],
    webpack: () => [require.resolveWeak('./YandexMarketCheck') as number],
});
