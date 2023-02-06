import type {BackendHandler} from 'isomorphic/testingMocks/backendHandlers';

import palette from './mocks/palette.json';

export const defaultBackendHandler: BackendHandler = (name, params, backendParams) => {
    // для запроса в бункер выкидываем ошибку, чтобы отдался кэш бункера
    if (name === 'bunker.cat' && params?.node === '/analyticsplatform/tanker') {
        return Promise.reject('bunker rejected to take cache');
    }
    // Палитра графиков в АП всегда одинаковая
    if (params?.logName === 'ColorClient.palette') {
        return Promise.resolve(palette);
    }

    const queryPage = params?.query?.page;

    if (
        name === 'cocon' &&
        (queryPage?.startsWith('market-analytics-platform:resolver') ||
            queryPage?.startsWith('market-analytics-platform:role'))
    ) {
        return {
            result: {
                pages: [{roles: {result: true}}],
            },
        };
    }

    if (name === 'analytics.changeWidget') {
        return params;
    }

    if (name === 'analytics.getDataExistIntervals') {
        return params?.widgetTypes?.map((widgetType: string) => ({
            widgetType,
            interval: {
                endDate: '2021-03-31',
                startDate: '2019-01-01',
            },
        }));
    }

    if (name === 'analytics.getModels') {
        return params?.modelIds?.map((id: number) => ({
            id,
            categoryId: id + 1000,
            brandId: id + 999,
            name: `dummy model ${id}`,
        }));
    }

    return undefined;
};
