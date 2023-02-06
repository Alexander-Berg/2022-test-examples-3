'use strict';

const service = require('./corp-ava.js');

let core;

beforeEach(() => {
    core = {
        config: {
            services: {
                'corp-ava': 'http://corp-ava'
            }
        },
        got: jest.fn().mockResolvedValue({}),
        req: {},
        auth: {
            get: () => ({ uid: 'TEST_UID' })
        }
    };
});

describe('обработка ошибок ->', () => {
    it('реджектится с ошибкой', async () => {
        expect.assertions(1);
        core.got.mockRejectedValue({ error: 'SOME ERROR' });

        try {
            await service(core, '/some_method');
        } catch (err) {
            expect(err).toEqual({ error: 'SOME ERROR' });
        }
    });
});

describe('параметры запроса ->', () => {
    it('добавляет options, params', () => {
        const params = { foo: 'bar' };
        service(core, '/method', params, { option: 'value' });
        expect(core.got.mock.calls[0][1]).toEqual({
            query: { foo: 'bar' },
            option: 'value'
        });
    });
});
