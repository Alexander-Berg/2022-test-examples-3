import { deepFreeze } from '../utils/tests';
import { getServiceTicket, Service } from './tvm';

describe('Lib. tvm. getServiceTicket', () => {
    const services: Array<[Service, string]> = [
        [Service.Blackbox, 'blackbox-service-ticket'],
        [Service.Geocoder, 'geocoder-service-ticket'],
        [Service.PersAddress, 'pers-address-service-ticket'],
    ];

    const emptyPartialReq = deepFreeze({
        tvm: {
            [Service.TapBackend]: {
                tickets: {},
            },
        },
    });
    const filledPartialReq = deepFreeze({
        tvm: {
            [Service.TapBackend]: {
                tickets: {
                    [Service.Blackbox]: {
                        tvm_id: 1,
                        ticket: 'blackbox-service-ticket',
                    },
                    [Service.Geocoder]: {
                        tvm_id: 2,
                        ticket: 'geocoder-service-ticket',
                    },
                    [Service.PersAddress]: {
                        tvm_id: 3,
                        ticket: 'pers-address-service-ticket',
                    },
                },
            },
        },
    });

    test.each(services)('должен корректно вернуть сервис тикет для сервиса %s', (service, ticket) => {
        expect(getServiceTicket(filledPartialReq, service)).toBe(ticket);
    });

    test.each(services)('должен пробрасывать ошибку, если сервис тикет не определен для сервиса %s', service => {
        expect(() => getServiceTicket(emptyPartialReq, service)).toThrowError(`TVM ticket for ${service} is missing`);
    });
});
