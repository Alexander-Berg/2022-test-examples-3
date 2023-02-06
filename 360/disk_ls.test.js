'use strict';

const method = require('./disk_ls.js');
const _omit = require('lodash/omit');

const { CUSTOM_ERROR, HTTP_ERROR } = require('@yandex-int/duffman').errors;
const httpError = (statusCode) => new HTTP_ERROR({ statusCode });

let core;
let mockService;

beforeEach(() => {
    mockService = jest.fn();
    core = {
        params: {},
        config: {
            services: {
                disk: {}
            }
        },
        service: () => mockService
    };
});

describe('happy path', () => {
    it('по-умлолчанию метод /json/list', async () => {
        mockService.mockResolvedValueOnce();

        await method(core);

        expect(mockService.mock.calls[0][0]).toBe('/json/list');
    });

    it('по-умлолчанию вызывает диск с path=/disk', async () => {
        mockService.mockResolvedValueOnce();

        await method(core);

        expect(mockService.mock.calls[0][1]).toMatchObject({ path: '/disk' });
    });

    it('можно передать path', async () => {
        mockService.mockResolvedValueOnce();
        core.params = { path: '/disk/Загрузки' };

        await method(core);

        expect(mockService.mock.calls[0][1]).toMatchObject({ path: '/disk/Загрузки' });
    });

    it('можно передать dirlist для вывода только папок', async () => {
        mockService.mockResolvedValueOnce();
        core.params = { dirlist: 1 };

        await method(core);

        expect(mockService.mock.calls[0][0]).toBe('/json/dir_list');
    });

    describe('amount', () => {
        it('по-умлолчанию 40', async () => {
            mockService.mockResolvedValueOnce();

            await method(core);

            expect(mockService.mock.calls[0][1]).toMatchObject({ amount: 40 });
        });

        it('можно передать в параметрах', async () => {
            core.params = { amount: 42 };
            mockService.mockResolvedValueOnce();

            await method(core);

            expect(mockService.mock.calls[0][1]).toMatchObject({ amount: 42 });
        });

        it('для dirlist нет', async () => {
            mockService.mockResolvedValueOnce();
            core.params = { dirlist: 1 };

            await method(core);

            expect(mockService.mock.calls[0][1]).not.toHaveProperty('amount');
        });
    });

    describe('offset', () => {
        it('по-умлолчанию 0', async () => {
            mockService.mockResolvedValueOnce();

            await method(core);

            expect(mockService.mock.calls[0][1]).toMatchObject({ offset: 0 });
        });

        it('можно передать в параметрах', async () => {
            core.params = { offset: 42 };
            mockService.mockResolvedValueOnce();

            await method(core);

            expect(mockService.mock.calls[0][1]).toMatchObject({ offset: 42 });
        });

        it('для dirlist нет', async () => {
            mockService.mockResolvedValueOnce();
            core.params = { dirlist: 1 };

            await method(core);

            expect(mockService.mock.calls[0][1]).not.toHaveProperty('offset');
        });
    });
});

describe('диск отвечает ошибкой', () => {
    it('404: code 6', async () => {
        expect.assertions(2);
        mockService.mockRejectedValueOnce(new CUSTOM_ERROR({ code: 6, title: 'foo' }));

        try {
            await method(core);
        } catch (err) {
            expect(err.code).toBe(404);
            expect(err.message).toBe('foo');
        }
    });

    it('404: code 77', async () => {
        expect.assertions(2);
        mockService.mockRejectedValueOnce(new CUSTOM_ERROR({ code: 77, title: 'bar' }));

        try {
            await method(core);
        } catch (err) {
            expect(err.code).toBe(404);
            expect(err.message).toBe('bar');
        }
    });

    it('404', async () => {
        expect.assertions(1);
        mockService.mockRejectedValueOnce(httpError(404));

        try {
            await method(core);
        } catch (err) {
            expect(err.code).toBe(404);
        }
    });
});

test('отвечает 404 если сервиса нет (корп, например)', () => {
    core.config.services = _omit(core.config.services, 'disk');

    return method(core).then(
        () => Promise.reject('MUST REJECT'),
        (err) => {
            expect(err.code).toBe(404);
        }
    );
});
