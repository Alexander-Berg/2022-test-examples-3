'use strict';

const method = require('./disk_mkdir.js');

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

test('отвечает 404 если сервиса нет (корп, например)', async () => {
    expect.assertions(1);
    delete core.config.services.disk;

    try {
        await method(core);
    } catch (err) {
        expect(err.code).toBe(404);
    }
});

test('отвечает 400, если не указан path', async () => {
    expect.assertions(2);

    try {
        await method(core);
    } catch (err) {
        expect(err.code).toBe(400);
        expect(err.message).toBe('path param is missing');
    }
});

describe('happy path', () => {
    beforeEach(() => {
        mockService.mockResolvedValueOnce();
    });

    it('передает в сервис path', async () => {
        core.params = { path: '/disk/foo' };

        await method(core);

        expect(mockService).toHaveBeenCalledWith('/json/mkdir', { path: '/disk/foo' });
    });

    it('отвечает статусом и путем', async () => {
        core.params = { path: '/disk/foo' };

        const res = await method(core);

        expect(res).toEqual({
            status: 'OK',
            path: '/disk/foo'
        });
    });
});

describe('диск отвечает ошибкой', () => {
    beforeEach(() => {
        core.params = { path: '/disk/foo' };
    });

    it('409: code 9 parent folder was not found', async () => {
        expect.assertions(2);
        mockService.mockRejectedValueOnce(new CUSTOM_ERROR({ code: 9, title: 'parent folder was not found' }));

        try {
            await method(core);
        } catch (err) {
            expect(err.code).toBe(404);
            expect(err.message).toBe('parent folder was not found');
        }
    });

    it('405: code 13 mkdir: resource already exists', async () => {
        expect.assertions(2);
        mockService.mockRejectedValueOnce(new CUSTOM_ERROR({ code: 9, title: 'mkdir: resource already exists' }));

        try {
            await method(core);
        } catch (err) {
            expect(err.code).toBe(404);
            expect(err.message).toBe('mkdir: resource already exists');
        }
    });

    it('404: code 48 Service not found', async () => {
        expect.assertions(2);
        mockService.mockRejectedValueOnce(new CUSTOM_ERROR({ code: 9, title: 'Service not found' }));

        try {
            await method(core);
        } catch (err) {
            expect(err.code).toBe(404);
            expect(err.message).toBe('Service not found');
        }
    });

    it('404: code 77 wrong path', async () => {
        expect.assertions(2);
        mockService.mockRejectedValueOnce(new CUSTOM_ERROR({ code: 77, title: 'wrong path' }));

        try {
            await method(core);
        } catch (err) {
            expect(err.code).toBe(404);
            expect(err.message).toBe('wrong path');
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
