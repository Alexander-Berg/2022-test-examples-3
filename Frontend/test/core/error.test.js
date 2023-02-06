describe('errorReporter', () => {
    let utils;
    let errorReporter;

    beforeEach(() => jest.resetModules());

    describe('testing', () => {
        beforeEach(() => {
            utils = { reportError: jest.fn() };

            jest.mock('../../configs/current/config', () => () => ({
                env: 'development',
            }));

            errorReporter = require('../../core/error').errorReporter;
        });

        it('должен вернуть вернуть пустую строчку во внешней сети', () => {
            expect(errorReporter(new Error('This is oshibka'), utils, {
                reqdata: { is_yandex_net: 0 },
                env: { platform: 'touch-phone' },
            })).toEqual('');
        });

        it('должен вернуть вернуть непустую строчку во внутренней сети', () => {
            expect(errorReporter(new Error('This is oshibka'), utils, {
                reqdata: { is_yandex_net: 1 },
                env: { platform: 'touch-phone' },
            })).toContain('template-error');
        });

        it('должен позвать логирование ошибок во внешней сети', () => {
            errorReporter(new Error('This is oshibka'), utils, {
                reqdata: { is_yandex_net: 0 },
                env: { platform: 'touch-phone' },
            });

            expect(utils.reportError).toBeCalledTimes(1);
        });

        it('должен позвать логирование ошибок во внутренней сети', () => {
            errorReporter(new Error('This is oshibka'), utils, {
                reqdata: { is_yandex_net: 1 },
                env: { platform: 'touch-phone' },
            });

            expect(utils.reportError).toBeCalledTimes(1);
        });
    });

    describe('production', () => {
        beforeEach(() => {
            utils = { reportError: jest.fn() };

            jest.mock('../../configs/current/config', () => () => ({
                env: 'production',
            }));

            errorReporter = require('../../core/error').errorReporter;
        });

        it('должно выбрасываться исключение в production окружении', () => {
            const fn = () => errorReporter(new Error('This is oshibka'), utils, {
                reqdata: { is_yandex_net: 0 },
                env: { platform: 'touch-phone' },
            });

            expect(fn, 'This is oshibka').toThrow();
        });
    });
});

const { rendererErrorReporter } = require('../../core/error');

describe('rendererErrorReporter', () => {
    let utils;

    beforeEach(() => {
        utils = { reportError: jest.fn() };
    });

    it('должен вернуть вернуть пустую строчку во внешней сети', () => {
        expect(rendererErrorReporter(new Error('This is oshibka'), utils, {
            reqdata: { is_yandex_net: 0 },
            env: { platform: 'touch-phone' },
        })).toEqual('');
    });

    it('должен вернуть вернуть непустую строчку во внутренней сети', () => {
        expect(rendererErrorReporter(new Error('This is oshibka'), utils, {
            reqdata: { is_yandex_net: 1 },
            env: { platform: 'touch-phone' },
        })).toContain('template-error');
    });

    it('должен позвать логирование ошибок во внешней сети', () => {
        rendererErrorReporter(new Error('This is oshibka'), utils, {
            reqdata: { is_yandex_net: 0 },
            env: { platform: 'touch-phone' },
        });

        expect(utils.reportError).toBeCalledTimes(1);
    });

    it('должен позвать логирование ошибок во внутренней сети', () => {
        rendererErrorReporter(new Error('This is oshibka'), utils, {
            reqdata: { is_yandex_net: 1 },
            env: { platform: 'touch-phone' },
        });

        expect(utils.reportError).toBeCalledTimes(1);
    });
});
