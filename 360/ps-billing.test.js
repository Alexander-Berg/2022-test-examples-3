const _set = require('lodash/set');
const psBillingService = require('./ps-billing');

describe('service:ps-billing', () => {
    let gotFn;
    let coreMock;

    beforeEach(() => {
        gotFn = jest.fn().mockImplementation(() => Promise.resolve());
        coreMock = {
            got: gotFn,
            auth: {
                get: () => ({
                    uid: '12345',
                }),
            }
        };

        _set(coreMock, 'ip', '127.0.0.1');
        _set(coreMock, 'config.services.ps-billing', 'https://ps-billing/v1/');
        _set(coreMock, 'tvmTickets.ps-billing', 'ps-billing-tvm-ticket');

        _set(coreMock, 'tvmTickets.userTicket', 'some-user-ticket');

        _set(coreMock, 'req.ycrid', 'tuning-123');
    });

    describe('#', () => {
        test('should form and send request to ps-billing', () => {
            const params = {
                a: 1,
                b: 2,
                c: 3,
            };
            const options = {
                optionA: 1,
                optionB: 2,
                optionC: 3,
                headers: {
                    abc: 123,
                },
            };

            psBillingService(coreMock, 'test_method', params, options);

            expect(gotFn).toHaveBeenCalledWith(
                `${coreMock.config.services['ps-billing']}test_method`,
                {
                    ...options,
                    headers: {
                        ...options.headers,
                        'x-uid': '12345',
                        'x-user-ip': coreMock.ip,
                        'Yandex-Cloud-Request-ID': 'tuning-123',
                        'X-Ya-Service-Ticket':
                            coreMock.tvmTickets['ps-billing'],
                        'X-Ya-User-Ticket': coreMock.tvmTickets.userTicket,
                    },
                    query: params,
                    json: true,
                }
            );
        });

        test('should return response of ps-billing', async() => {
            const expected = Symbol();

            gotFn.mockImplementation(() => Promise.resolve(expected));

            const actual = await psBillingService(coreMock, 'test_method', {}, {});

            expect(actual).toBe(expected);
        });
    });
});
