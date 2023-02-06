'use strict';

jest.unmock('@yandex-int/duffman');

const { CUSTOM_ERROR, EXTERNAL_ERROR } = require('@yandex-int/duffman').errors;
const doFiltersEdit = require('./_do-filters-edit.js');

let core;
const furitaService = jest.fn();

let params;
const baseParams = {
    attachment: '',
    field3: 'Hello',
    field2: '3',
    letter: 'nospam',
    stop: '',
    logic: '0',
    field1: 'subject',
    clicker: 'move',
    name: 'Моё правило',
    connection_id: 'LIZA-97576914-1526479220312',
    order: '1'
};

let isUserRecentlyEnteredPasswordModel;
let settingsModel;

beforeEach(() => {
    isUserRecentlyEnteredPasswordModel = jest.fn();
    settingsModel = jest.fn();

    core = {
        service: () => furitaService,
        request: jest.fn((name, ...args) => {
            if (name === 'is-user-recently-entered-password') {
                return isUserRecentlyEnteredPasswordModel(...args);
            } else if (name === 'settings') {
                return settingsModel(...args);
            }

            throw new Error(`Model ${name} is not mocked`);
        }),

        config: {
            IS_CORP: false,
            yandexDomain: 'yandex.ru',
            locale: 'ru'
        }
    };

    settingsModel.mockResolvedValueOnce({ default_email: '__DEFAULT_EMAIL_FROM__' });
});

describe('c проверкой давности ввода пароля', () => {
    describe('заглушка для старой схемы', () => {
        beforeEach(() => {
            params = {
                ...baseParams,
                autoanswer: 'I am away',
                password: 'password'
            };
        });

        it('должен удалить password из общих params', async () => {
            expect(params).toHaveProperty('password');

            await doFiltersEdit(params, core).catch(() => {});

            expect(params).not.toHaveProperty('password');
        });

        it('должен кинуть ошибку password_check', async () => {
            expect.assertions(2);

            try {
                await doFiltersEdit(params, core);
            } catch (err) {
                expect(err).toBeInstanceOf(CUSTOM_ERROR);
                expect(err.error).toBe('password_check');
            }
        });
    });

    describe('непустые значения', () => {
        it('должен кинуть ошибку bad_request', async () => {
            params = {
                ...baseParams,
                autoanswer: ''
            };
            expect.assertions(2);

            try {
                await doFiltersEdit(params, core);
            } catch (err) {
                expect(err).toBeInstanceOf(CUSTOM_ERROR);
                expect(err.error).toBe('bad_request');
            }
        });
    });

    describe('новая схема, запрос модели is-user-recently-entered-password', () => {
        beforeEach(() => {
            params = {
                ...baseParams,
                autoanswer: 'I am away'
            };
        });

        it('должен запросить время ввода пароля у модели', async () => {
            expect(isUserRecentlyEnteredPasswordModel).not.toBeCalled();

            isUserRecentlyEnteredPasswordModel.mockResolvedValueOnce({ check: true });

            await doFiltersEdit(params, core);

            expect(isUserRecentlyEnteredPasswordModel).toHaveBeenCalledTimes(1);
        });

        it(
            'должен кинуть ошибку user_password_was_entered_long_ago, если пароль вводился давно',
            async () => {
                expect.assertions(2);

                isUserRecentlyEnteredPasswordModel.mockResolvedValueOnce({ check: false });

                try {
                    await doFiltersEdit(params, core);
                } catch (err) {
                    expect(err).toBeInstanceOf(CUSTOM_ERROR);
                    expect(err.error).toBe('user_password_was_entered_long_ago');
                }
            }
        );

        it(
            'должен кинуть ошибку user_password_was_entered_long_ago, если blackbox зафейлился',
            async () => {
                expect.assertions(2);

                isUserRecentlyEnteredPasswordModel.mockRejectedValueOnce(new Error('Some Blackbox Error'));

                try {
                    await doFiltersEdit(params, core);
                } catch (err) {
                    expect(err).toBeInstanceOf(EXTERNAL_ERROR);
                    expect(err.error).toBe('user_password_was_entered_long_ago');
                }
            }
        );

        it(
            'должен запросить furita/api/edit.json, если пароль вводили недавно',
            async () => {
                isUserRecentlyEnteredPasswordModel.mockResolvedValueOnce({ check: true });

                await doFiltersEdit(params, core);

                expect(furitaService).toHaveBeenCalledTimes(1);
                expect(furitaService).toHaveBeenCalledWith(
                    '/api/edit.json',
                    {
                        attachment: '',
                        auth_domain: 'yandex.ru',
                        autoanswer: 'I am away',
                        clicker: 'move',
                        confirm_domain: 'mail.yandex.ru',
                        connection_id: 'LIZA-97576914-1526479220312',
                        field1: 'subject',
                        field2: '3',
                        field3: 'Hello',
                        from: '__DEFAULT_EMAIL_FROM__',
                        lang: 'ru',
                        letter: 'nospam',
                        logic: '0',
                        name: 'Моё правило',
                        order: '1',
                        stop: ''
                    });
            }
        );

        it(
            'должен вернуть результат, если пароль вводили недавно (меньше 30 минут назад)',
            async () => {
                isUserRecentlyEnteredPasswordModel.mockResolvedValueOnce({ check: true });

                furitaService.mockResolvedValueOnce('__homyachok__');

                const res = await doFiltersEdit(params, core);
                expect(res).toBe('__homyachok__');

                expect(isUserRecentlyEnteredPasswordModel).toHaveBeenCalledTimes(1);
            }
        );

        it(
            'должен вернуть ошибку фуриты, если пароль ввели недавно, но фейл фуриты',
            async () => {
                expect.assertions(2);

                isUserRecentlyEnteredPasswordModel.mockResolvedValueOnce({ check: true });
                furitaService.mockRejectedValueOnce(new Error('Some Furita Error'));

                try {
                    await doFiltersEdit(params, core);
                } catch (err) {
                    expect(err).toBeInstanceOf(EXTERNAL_ERROR);
                    expect(err.error).toBe('furita_error');
                }
            }
        );
    });
});

describe('без проверки пароля', () => {
    beforeEach(() => {
        params = { ...baseParams };
        isUserRecentlyEnteredPasswordModel.mockRejectedValueOnce(new Error('Some Blackbox Error'));
    });

    it('запрос furita', async () => {
        furitaService.mockResolvedValueOnce('__FURITA_SERVICE_RESPONSE__');

        await doFiltersEdit(params, core);

        expect(furitaService).toHaveBeenCalledTimes(1);
        expect(furitaService).toHaveBeenCalledWith(
            '/api/edit.json',
            {
                attachment: '',
                clicker: 'move',
                connection_id: 'LIZA-97576914-1526479220312',
                field1: 'subject',
                field2: '3',
                field3: 'Hello',
                letter: 'nospam',
                logic: '0',
                name: 'Моё правило',
                order: '1',
                stop: '',
                noconfirm: '1'
            });
    });

    it(
        'должен вернуть выдачу фильтра если запрос furita успешен',
        async () => {
            furitaService.mockResolvedValueOnce('__homyachok__');

            const res = await doFiltersEdit(params, core);
            expect(res).toBe('__homyachok__');

            expect(furitaService).toHaveBeenCalledTimes(1);
            expect(furitaService).toHaveBeenCalledWith(
                '/api/edit.json',
                {
                    attachment: '',
                    clicker: 'move',
                    connection_id: 'LIZA-97576914-1526479220312',
                    field1: 'subject',
                    field2: '3',
                    field3: 'Hello',
                    letter: 'nospam',
                    logic: '0',
                    name: 'Моё правило',
                    order: '1',
                    stop: '',
                    noconfirm: '1'
                });
        }
    );

    it(
        'должен вернуть furita_error если запрос furita неуспешен',
        async () => {
            expect.assertions(2);

            furitaService.mockRejectedValueOnce(new Error('Some Furita Error'));

            try {
                await doFiltersEdit(params, core);
            } catch (err) {
                expect(err).toBeInstanceOf(EXTERNAL_ERROR);
                expect(err.error).toBe('furita_error');
            }
        }
    );
});

[
    [ 'пересылку', 'forward_address' ],
    [ 'уведомления', 'notify_address' ]
].forEach(([ name, field ]) => {
    test(`должен запретить ${name} для корпов`, async () => {
        expect.assertions(2);

        core.config.IS_CORP = true;
        params = {
            ...baseParams,
            [field]: '__EMAIL__'
        };

        try {
            await doFiltersEdit(params, core);
        } catch (err) {
            expect(err).toBeInstanceOf(CUSTOM_ERROR);
            expect(err.error).toBe('notifications_and_forwarding_are_disabled');
        }
    });
});
