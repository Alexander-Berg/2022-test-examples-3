'use strict';

const service = require('./xiva.js');

let core;

beforeEach(() => {
    core = {
        auth: {
            get: () => ({ uid: '12' })
        },
        config: {
            services: {
                xiva: 'http://xiva'
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

describe('общие параметры запроса ->', () => {
    it('должен добавить uid', () => {
        service(core, '/folders', {}, {});
        expect(core.got.mock.calls[0][1].query).toEqual({ uid: '12' });
    });

    it('не должен перезаписать uid', () => {
        core.auth.get = () => ({ uid: '12', suid: '34' });
        service(core, '/folders', { uid: '42' }, {});
        expect(core.got.mock.calls[0][1].query).toEqual({ uid: '42' });
    });

    it('apns_queue_repeat возвращает json', () => {
        service(core, '/v2/apns_queue_repeat', {}, {});
        expect(core.got.mock.calls[0][1].json).toEqual(true);
    });
});
