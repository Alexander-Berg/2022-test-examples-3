import { IImagesStaticProps, Images } from './images';
import { RequestAnimationFrameMock } from '../../test/requestAnimationFrameMock';
import { ISPASContext } from '../../config/types';

const rafMock = new RequestAnimationFrameMock();
rafMock.useFakeRaf();
rafMock.triggerAllFrames();

//@ts-ignore
global.fetch = jest.fn(() =>
    Promise.resolve({
        json: () => Promise.resolve(),
    }),
);

function makeSPASCtx(ctx?: Partial<ISPASContext>): ISPASContext {
    return {
        url: 'https://yandex.ru/images/touch/?text=котики+картинки',
        ...ctx,
    };
}

function makeImages(staticProps: Partial<IImagesStaticProps> = {}) {
    return new Images({ staticProps: { platform: 'touch-phone', additionalQueryParams: [], additionalHeaders: [], flags: {}, ...staticProps } });
}

describe('Приложение картинок', () => {
    beforeEach(() => {
        // @ts-ignore
        global.fetch.mockClear();
    });

    describe('bootstrap', () => {
        test('Работает как предзагрузка', async() => {
            const images = makeImages();
            await images.bootstrap(makeSPASCtx({ isActive: false }));
            expect(global.fetch).toBeCalledTimes(1);
            expect(global.fetch).toBeCalledWith(
                'https://yandex.ru/images/touch/?spas=1&request=%7B%22blocks%22%3A%5B%7B%22block%22%3A%22b-page%3Aajax%22%2C%22version%22%3A%222%22%7D%5D%7D&bootstrap=1&prefetch=1',
                { headers: {} },
            );
        });

        test('Работает с походом за данными', async() => {
            const images = makeImages();
            // @ts-ignore
            global.fetch.mockReturnValueOnce(Promise.resolve({ json: () => Promise.resolve('from-bootstrap') }));
            const result = await images.bootstrap(makeSPASCtx({ isActive: true }));

            expect(global.fetch).toBeCalledTimes(2);
            expect(global.fetch).nthCalledWith(
                1,
                'https://yandex.ru/images/touch/?spas=1&request=%7B%22blocks%22%3A%5B%7B%22block%22%3A%22b-page%3Aajax%22%2C%22version%22%3A%222%22%7D%5D%7D&bootstrap=1&prefetch=1',
                { headers: {} },
            );
            expect(global.fetch).nthCalledWith(
                2,
                'https://yandex.ru/images/touch/?text=%D0%BA%D0%BE%D1%82%D0%B8%D0%BA%D0%B8+%D0%BA%D0%B0%D1%80%D1%82%D0%B8%D0%BD%D0%BA%D0%B8&spas=1&request=%7B%22blocks%22%3A%5B%7B%22block%22%3A%22b-page%3Aajax%22%2C%22version%22%3A%222%22%7D%5D%7D',
                { headers: {} },
            );

            expect(result).toBe('from-bootstrap');
        });

        test('Добрасывает query-параметры из свойств в запрос за bootstrap', async() => {
            const images = makeImages({ additionalQueryParams: [['test', '1'], ['foo', 'bar'], ['exp_flags', 'flag=1'], ['exp_flags', 'flag2=2']] });
            await images.bootstrap(makeSPASCtx());
            expect(global.fetch).toBeCalledTimes(1);
            expect(global.fetch).toBeCalledWith(
                'https://yandex.ru/images/touch/?test=1&foo=bar&exp_flags=flag%3D1&exp_flags=flag2%3D2&spas=1&request=%7B%22blocks%22%3A%5B%7B%22block%22%3A%22b-page%3Aajax%22%2C%22version%22%3A%222%22%7D%5D%7D&bootstrap=1&prefetch=1',
                { headers: {} },
            );
        });

        test('Добрасывает query-параметры из свойств в параллельный запрос за данными', async() => {
            const images = makeImages({ additionalQueryParams: [['test', '1'], ['foo', 'bar'], ['exp_flags', 'flag=1'], ['exp_flags', 'flag2=2']] });
            await images.bootstrap(makeSPASCtx({
                isActive: true,
            }));

            expect(global.fetch).toBeCalledTimes(2);
            expect(global.fetch).nthCalledWith(
                1,
                'https://yandex.ru/images/touch/?test=1&foo=bar&exp_flags=flag%3D1&exp_flags=flag2%3D2&spas=1&request=%7B%22blocks%22%3A%5B%7B%22block%22%3A%22b-page%3Aajax%22%2C%22version%22%3A%222%22%7D%5D%7D&bootstrap=1&prefetch=1',
                { headers: {} },
            );
            expect(global.fetch).nthCalledWith(
                2,
                'https://yandex.ru/images/touch/?text=%D0%BA%D0%BE%D1%82%D0%B8%D0%BA%D0%B8+%D0%BA%D0%B0%D1%80%D1%82%D0%B8%D0%BD%D0%BA%D0%B8&test=1&foo=bar&exp_flags=flag%3D1&exp_flags=flag2%3D2&spas=1&request=%7B%22blocks%22%3A%5B%7B%22block%22%3A%22b-page%3Aajax%22%2C%22version%22%3A%222%22%7D%5D%7D',
                { headers: {} },
            );
        });

        test('Добрасывает заголовки из свойств в запрос за bootstrap', async() => {
            const images = makeImages({ additionalHeaders: [['some-header', '1'], ['foo', 'bar']] });
            await images.bootstrap(makeSPASCtx());
            expect(global.fetch).toBeCalledTimes(1);
            expect(global.fetch).toBeCalledWith(
                'https://yandex.ru/images/touch/?spas=1&request=%7B%22blocks%22%3A%5B%7B%22block%22%3A%22b-page%3Aajax%22%2C%22version%22%3A%222%22%7D%5D%7D&bootstrap=1&prefetch=1',
                { headers: { 'some-header': '1', foo: 'bar' } },
            );
        });

        test('Добрасывает заголовки из свойств в параллельный запрос за данными', async() => {
            const images = makeImages({ additionalHeaders: [['some-header', '1'], ['foo', 'bar']] });
            await images.bootstrap(makeSPASCtx({
                isActive: true,
            }));

            expect(global.fetch).toBeCalledTimes(2);
            expect(global.fetch).nthCalledWith(
                1,
                'https://yandex.ru/images/touch/?spas=1&request=%7B%22blocks%22%3A%5B%7B%22block%22%3A%22b-page%3Aajax%22%2C%22version%22%3A%222%22%7D%5D%7D&bootstrap=1&prefetch=1',
                { headers: { 'some-header': '1', foo: 'bar' } },
            );
            expect(global.fetch).nthCalledWith(
                2,
                'https://yandex.ru/images/touch/?text=%D0%BA%D0%BE%D1%82%D0%B8%D0%BA%D0%B8+%D0%BA%D0%B0%D1%80%D1%82%D0%B8%D0%BD%D0%BA%D0%B8&spas=1&request=%7B%22blocks%22%3A%5B%7B%22block%22%3A%22b-page%3Aajax%22%2C%22version%22%3A%222%22%7D%5D%7D',
                { headers: { 'some-header': '1', foo: 'bar' } },
            );
        });
    });

    describe('mount', () => {
        beforeEach(() => {
            // @ts-ignore
            global.fetch.mockReturnValue(
                Promise.resolve(
                    {
                        json: () => ({
                            metadata: {},
                            blocks: [{ html: '' }],
                            assets: { assets: [] },
                            meta: [],
                        }),
                    },
                ),
            );
            // @ts-ignore
            window.BEM = {
                DOM: {
                    _initAdvanced() {},
                },
                blocks: {
                    'i-global': {
                        setParams() {},
                    },
                },
                afterCurrentEvent(cb: () => void) { cb() },
            };
            // @ts-ignore
            window.$ = () => {};
        });

        afterEach(done => {
            // Подчищаем в отдельной задаче, чтобы уже зарегистрированные таймеры имели правильное окружение.
            setTimeout(() => {
                // @ts-ignore
                delete window.BEM;
                // @ts-ignore
                delete window.$;
                done();
            }, 0);
        });

        test('Не работает без bootstrap', async() => {
            const images = makeImages();

            expect(images.mount(makeSPASCtx())).rejects.toBe(undefined);
        });

        test('Добрасывает query-параметры из свойств в запрос за данными при предзагрузке', async() => {
            const images = makeImages({ additionalQueryParams: [['test', '1'], ['foo', 'bar'], ['exp_flags', 'flag=1'], ['exp_flags', 'flag2=2']] });

            await images.bootstrap(makeSPASCtx({
                isActive: false,
            }));

            await images.mount(makeSPASCtx());
            expect(global.fetch).nthCalledWith(
                2,
                'https://yandex.ru/images/touch/?text=%D0%BA%D0%BE%D1%82%D0%B8%D0%BA%D0%B8+%D0%BA%D0%B0%D1%80%D1%82%D0%B8%D0%BD%D0%BA%D0%B8&test=1&foo=bar&exp_flags=flag%3D1&exp_flags=flag2%3D2&spas=1&request=%7B%22blocks%22%3A%5B%7B%22block%22%3A%22b-page%3Aajax%22%2C%22version%22%3A%222%22%7D%5D%7D',
                { headers: {} },
            );
        });

        test('Добрасывает заголовки из свойств в запрос за данными при предзагрузке', async() => {
            const images = makeImages({ additionalHeaders: [['some-header', '1'], ['foo', 'bar']] });

            await images.bootstrap(makeSPASCtx({
                isActive: false,
            }));

            await images.mount(makeSPASCtx());
            expect(global.fetch).nthCalledWith(
                2,
                'https://yandex.ru/images/touch/?text=%D0%BA%D0%BE%D1%82%D0%B8%D0%BA%D0%B8+%D0%BA%D0%B0%D1%80%D1%82%D0%B8%D0%BD%D0%BA%D0%B8&spas=1&request=%7B%22blocks%22%3A%5B%7B%22block%22%3A%22b-page%3Aajax%22%2C%22version%22%3A%222%22%7D%5D%7D',
                { headers: { 'some-header': '1', foo: 'bar' } },
            );
        });
    });
});
