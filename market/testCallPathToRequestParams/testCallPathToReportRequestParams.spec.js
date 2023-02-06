import {createFakeContext} from '@self/root/src/spec/utils/fakeContext';
import * as userHelpers from '@self/root/src/utils/unsafe/user';
import * as commonReportFunctions from '@self/root/src/resources/report/params';

import {REARR_FACTORS as REARR_FACTORS_QUERY} from '@self/root/src/constants/queryParams';

import handlerOfDJUniversalProducts from '../../resolveDJUniversalProducts/v1';
import djUniversalProductsParams from './djUniversalProductsParams';

import handlerOfDJUniversalLinks from '../../resolveDJUniversalLinks/v1';
import djUniversalLinksParams from './djUniversalLinksParams';

jest.mock('@yandex-market/mandrel/handler', () => ({
    createHandler: jest.fn().mockImplementation(fn => fn),
}));

jest.mock('@yandex-market/mandrel/resolver', () => ({
    createResolver: jest.fn().mockImplementation(fn => fn),
    createSyncResolver: jest.fn().mockImplementation(fn => fn),
    withNormalization: jest.fn().mockImplementation(fn => fn),
}));

jest.mock('@self/root/src/utils/resource', () => ({
    /**
     * schema.prepare вызов для проверки подготовки параметров
     */
    method: jest.fn(schema => (ctx, params) => schema.prepare(params)),
    getBackendClass: () => class {},
}));

jest.mock('@self/root/src/resolvers/report/paramsStrategy', () => ({
    strategies: [{
        name: 'usePerks',
        defaultValue: '',
        paramName: 'perks',
        getValue() {
            return Promise.resolve('perkawdawd_value');
        },
    }],
}));

jest.mock('@self/root/src/utils/error', () => ({
    // eslint-disable-next-line no-console
    stubWithError: () => error => console.log('Error (in custom mock stab) :\n', error),
}));

/**
 * Проверяет полный путь прокидывания.
 *
 * Задача этого теста - подготовить моки функций вызовов внутри релозверов,
 * чтобы можно было честно просмотреть прокидывание параметров (что их никто не перетирает по пути)
 * до итоговой передачи в формирование запроса
 */
describe('Прокидывает корректно данные до формирования парметров запроса', () => {
    process.env.PLATFORM = 'api';
    let getRequestWithHeaders;
    let ctx;

    const userSettings = {
        currency: 'rur',
        family: 'familyawd',
        marketYs: 'marketYsawd',
    };
    const rearrFactorsValue = 'awdawdRearr';

    beforeEach(() => {
        ctx = createFakeContext({
            user: {isAuth: true},
            params: {
                /**
                 * Да, приложеньки реары пробрасывают через query,
                 * поэтому такой обходной путь так же важно тестом покрыть
                 */
                [REARR_FACTORS_QUERY]: rearrFactorsValue,
            },
            requestGarbage: {
                abt: {expFlags: {}},
            },
        });

        jest
            .spyOn(userHelpers, 'getCurrentUserSettings')
            .mockImplementation(() => ({
                getSetting: key => userSettings[key],
            }));

        getRequestWithHeaders = jest.spyOn(commonReportFunctions, 'getRequestWithHeaders');
    });

    it('handlerOfDJUniversalProducts прокидывает в getRequestWithHeaders правильные параметры', async () => {
        await handlerOfDJUniversalProducts(ctx, djUniversalProductsParams.paramsForHandler);

        expect(getRequestWithHeaders).toHaveBeenCalledWith({
            'rearr-factors': rearrFactorsValue,
            family: userSettings.family,
            wprid: userSettings.marketYs,
            currency: userSettings.currency,
            ...djUniversalProductsParams.expectParamsForPrepareRequest,
        });
    });

    it('handlerOfDJUniversalLinks прокидывает в getRequestWithHeaders правильные параметры', async () => {
        await handlerOfDJUniversalLinks(ctx, djUniversalLinksParams.paramsForHandler);

        expect(getRequestWithHeaders).toHaveBeenCalledWith({
            'rearr-factors': rearrFactorsValue,
            family: userSettings.family,
            wprid: userSettings.marketYs,
            currency: userSettings.currency,
            ...djUniversalLinksParams.expectParamsForPrepareRequest,
        });
    });
});
