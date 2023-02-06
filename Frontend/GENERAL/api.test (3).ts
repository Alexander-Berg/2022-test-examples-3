import { setNondecreasingContentHeight } from '.';

jest.mock('../rum', () => ({ logError: jest.fn(), logWarning: jest.fn() }));

describe('JS API', () => {
    describe('YandexCheckoutApi', () => {
        const storedYandexCheckoutInternalApi = window.yandexCheckoutInternalApi;

        beforeEach(() => {
            window.yandexCheckoutInternalApi = {
                ...window.yandexCheckoutInternalApi,
                checkout: {
                    ...window.yandexCheckoutInternalApi?.checkout,
                    setContentHeight: jest.fn().mockResolvedValue(undefined),
                }
            };
        });

        afterEach(() => {
            window.yandexCheckoutInternalApi = storedYandexCheckoutInternalApi;
        });

        it('должен запустить ресайз контейнера до определенной высоты', async() => {
            await setNondecreasingContentHeight(100);

            expect(window.yandexCheckoutInternalApi.checkout.setContentHeight).toBeCalledTimes(1);
            expect(window.yandexCheckoutInternalApi.checkout.setContentHeight).toHaveBeenNthCalledWith(1, 100);
        });

        it('должен зачеинить вызовы js-api', done => {
            window.yandexCheckoutInternalApi.checkout.setContentHeight = jest.fn().mockImplementation(nthCall => {
                return new Promise(resolve => setTimeout(resolve))
                    .then(() => {
                        // Проверяем, что на время завершения промиса не было лишних вызовов
                        expect(window.yandexCheckoutInternalApi.checkout.setContentHeight)
                            .toBeCalledTimes(nthCall);
                        expect(window.yandexCheckoutInternalApi.checkout.setContentHeight)
                            .toHaveBeenNthCalledWith(nthCall, nthCall);
                    });
            });
            setNondecreasingContentHeight(1);
            setNondecreasingContentHeight(2);
            setNondecreasingContentHeight(3).then(done);
        });
    });
});
