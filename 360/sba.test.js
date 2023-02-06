'use strict';

const service = require('./sba.js');

let core;

beforeEach(() => {
    core = {
        config: {
            services: {
                sba: 'http://sba'
            }
        },
        got: jest.fn().mockResolvedValue({})
    };
});

describe('обработка ошибок ->', () => {
    it('реджектится с ошибкой', async () => {
        expect.assertions(1);
        core.got.mockRejectedValue({ error: 'SOME ERROR' });

        try {
            await service(core, '/some_method', {}, {});
        } catch (err) {
            expect(err).toEqual({ error: 'SOME ERROR' });
        }
    });
});

describe('параметры вызова got ->', () => {
    it('должен передать правильные аргументы в got', () => {
        service(core, '/method', { param: 'value' }, { option: 'value' });

        expect(core.got.mock.calls[0][1]).toEqual({
            query: { param: 'value' },
            json: true,
            option: 'value'
        });
    });
});
