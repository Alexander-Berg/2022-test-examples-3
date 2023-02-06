/* eslint-disable quotes */
import { IBlackboxStatus } from '@yandex-int/frontend-apphost-context';

import { billingRoute } from '../../../routes';
import { Reply } from '../../../lib/renderer';
import { accessMiddleware } from '../access';
import { StationApphostContext } from '../../context';

interface ComputeMockContext {
    isStation: boolean;
    isTV?: boolean;
    isYaBro: boolean;
    isInternalNetwork: boolean;
    isBlackboxValid?: boolean;
    hasDisableAuthCheckFlag?: boolean;
    isBillingRoute?: boolean;
}

const computeMockContext = (
    {
        isStation,
        isTV = false,
        isYaBro,
        isInternalNetwork,
        isBlackboxValid = true,
        hasDisableAuthCheckFlag = false,
        isBillingRoute = false,
    }: ComputeMockContext) => ({
    isStation() {
        return isStation;
    },
    isTV() {
        return isTV;
    },
    isYandexBrowser() {
        return isYaBro;
    },
    isYandexNet() {
        return isInternalNetwork;
    },
    setResponseHeader(_header: string, _values: string | string[]) {},
    get blackbox() {
        return {
            status: {
                id: isBlackboxValid ? IBlackboxStatus.OK : IBlackboxStatus.NOAUTH,
            },
        };
    },
    get request() {
        return {
            params: hasDisableAuthCheckFlag
                ? { disableAuthCheck: true }
                : {}
            ,
        };
    },
    get match() {
        return isBillingRoute ? { name: billingRoute.name } : undefined;
    },
});

// @ts-ignore
// eslint-disable-next-line max-len
const checkAccessTestRunner = (ctx: ReturnType<typeof computeMockContext>) => () => accessMiddleware()(ctx as StationApphostContext, new Reply());

describe('accessMiddleware', () => {
    describe('не должна закрывать доступ', () => {
        const OLD_ENV = process.env;

        it(`если тестируется с помощью гермионы`, () => {
            process.env = { ...OLD_ENV };
            process.env.HERMIONE_TESTS_RUN = '1';

            expect(
                checkAccessTestRunner(
                    computeMockContext({
                        isStation: false,
                        isYaBro: false,
                        isInternalNetwork: false,
                        isBlackboxValid: false,
                    }),
                ),
            ).not.toThrowError();

            process.env = { ...OLD_ENV };
            process.env.DISABLE_AUTH_CHECK = '1';

            expect(
                checkAccessTestRunner(
                    computeMockContext({
                        isStation: false,
                        isYaBro: false,
                        isInternalNetwork: false,
                        isBlackboxValid: false,
                    }),
                ),
            ).not.toThrowError();

            process.env = { ...OLD_ENV };
        });

        it(`если произошел вход со "Станции"`, () => {
            expect(
                checkAccessTestRunner(
                    computeMockContext({ isStation: true, isYaBro: false, isInternalNetwork: false }),
                ),
            ).not.toThrowError();
        });

        it(`если произошел вход с ТВ`, () => {
            expect(
                checkAccessTestRunner(
                    computeMockContext({ isStation: false, isTV: true, isYaBro: false, isInternalNetwork: false }),
                ),
            ).not.toThrowError();
        });

        it(`если пользователь зашел из внутренней сети через "Яндекс Браузер"`, () => {
            expect(
                checkAccessTestRunner(computeMockContext({ isStation: false, isYaBro: true, isInternalNetwork: true })),
            ).not.toThrowError();
        });

        it(`если пользователь зашел из внутренней через Станцию`, () => {
            expect(
                checkAccessTestRunner(computeMockContext({ isStation: true, isYaBro: false, isInternalNetwork: true })),
            ).not.toThrowError();
        });

        it('если куки пользователя невалидна, но есть флаг disableAuthCheck', () => {
            expect(
                checkAccessTestRunner(
                    computeMockContext({
                        isStation: true,
                        isYaBro: false,
                        isInternalNetwork: false,
                        isBlackboxValid: false,
                        hasDisableAuthCheckFlag: true,
                    }),
                ),
            ).not.toThrowError();
        });

        it('если пользователь зашел на лендинг биллинга в ПП', () => {
            expect(
                checkAccessTestRunner(
                    computeMockContext({
                        isStation: false,
                        isYaBro: false,
                        isInternalNetwork: false,
                        isBlackboxValid: false,
                        hasDisableAuthCheckFlag: false,
                        isBillingRoute: true,
                    }),
                ),
            ).not.toThrowError();
        });
    });

    describe('должна вызывать ошибку', () => {
        it('если пользователь зашел через интернет не со Станции и не с "Яндекс Браузера"', () => {
            expect(
                checkAccessTestRunner(
                    computeMockContext({ isStation: false, isYaBro: false, isInternalNetwork: false }),
                ),
            ).toThrowErrorMatchingInlineSnapshot(`"Доступ к странице запрещён"`);
        });

        it('если пользователь зашел из внутренней сети, но не через "Яндекс Браузер"', () => {
            expect(
                checkAccessTestRunner(
                    computeMockContext({ isStation: false, isYaBro: false, isInternalNetwork: true }),
                ),
            ).toThrowErrorMatchingInlineSnapshot(`"Доступ к странице запрещён"`);
        });

        it('если пользователь зашел через Яндекс Браузер, но не из внутренней сети', () => {
            expect(
                checkAccessTestRunner(
                    computeMockContext({ isStation: false, isYaBro: true, isInternalNetwork: false }),
                ),
            ).toThrowErrorMatchingInlineSnapshot(`"Доступ к странице запрещён"`);
        });

        it('если куки пользователя невалидна и нет флага disableAuthCheck', () => {
            expect(
                checkAccessTestRunner(
                    computeMockContext({
                        isStation: true,
                        isYaBro: true,
                        isInternalNetwork: true,
                        isBlackboxValid: false,
                    }),
                ),
            ).toThrowErrorMatchingInlineSnapshot(`"Cookie авторизации отсутствует или требует обновления"`);
        });
    });
});
