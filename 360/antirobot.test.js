'use strict';

let core;
const service = require('./antirobot.js');

beforeEach(function() {
    core = {
        config: {
            services: {
                antirobot: 'http://antirobot'
            },
            USER_IP: '127.0.0.1'
        },
        got: jest.fn(),
        req: {
            tvm: {
                tickets: {
                    antirobot: {
                        ticket: '__x-ya-service-ticket-antirobot'
                    }
                }
            }
        }
    };
});

test('должен вызвать антиробот с ожидаемыми параметрами', async function() {
    await service(core, '/validate', { spravka: 'XXX' }, { headers: {} });
    expect(core.got).toHaveBeenCalledWith('http://antirobot/validate', {
        json: true,
        query: {
            https: 'on',
            ip: '127.0.0.1',
            spravka: 'XXX'
        },
        headers: {
            'x-ya-service-ticket': '__x-ya-service-ticket-antirobot'
        }
    });
});
