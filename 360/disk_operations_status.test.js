'use strict';

const method = require('./disk_operations_status');

let core;

beforeEach(() => {
    core = {
        params: {},
        config: {
            services: {
                disk: {}
            }
        },
        request: jest.fn()
    };
});

test('отвечает 404 если сервиса нет (корп, например)', async () => {
    expect.assertions(2);
    delete core.config.services.disk;

    try {
        await method(core);
    } catch (err) {
        expect(err.code).toBe(404);
        expect(err.message).toBe('disk service not available');
    }
});

test('отвечает 400, если не указан oids', async () => {
    expect.assertions(2);

    try {
        await method(core);
    } catch (err) {
        expect(err.code).toBe(400);
        expect(err.message).toBe('oids param is missing');
    }
});

test('отвечает 400, если oids не массив', async () => {
    expect.assertions(2);
    core.params = { oids: 'deadbeef' };

    try {
        await method(core);
    } catch (err) {
        expect(err.code).toBe(400);
        expect(err.message).toBe('oids param must be array');
    }
});

describe('если указан oids', () => {
    beforeEach(() => {
        core.params = { oids: [ 'dead', 'beef' ] };
    });

    it('дергает модель', async () => {
        core.request.mockResolvedValueOnce();

        await method(core);

        expect(core.request).toHaveBeenCalled();
    });

    it('модель валится', async () => {
        expect.assertions(2);
        core.request.mockRejectedValueOnce({ message: 'boo' });

        try {
            await method(core);
        } catch (err) {
            expect(err.code).toBe(500);
            expect(err.message).toBe('boo');
        }
    });
});
