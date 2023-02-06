import { createApphostContextStub } from './create-apphost-context-stub';
import { isRouteForAuthorizedUsers } from './is-route-for-authorized-users';

const stub = createApphostContextStub({
    request: {
        path: 'quasar',
    },
});

describe('helpers/is-route-for-authorized-users', () => {
    it('Корректно определяет открытые урлы', () => {
        expect(isRouteForAuthorizedUsers('quasar/promo', stub)).toEqual(false);
        expect(isRouteForAuthorizedUsers('quasar/login', stub)).toEqual(false);
        expect(isRouteForAuthorizedUsers('quasar/account', stub)).toEqual(false);
        expect(isRouteForAuthorizedUsers('quasar/push', stub)).toEqual(false);
    });

    it('Корректно определяет закрытые урлы', () => {
        expect(isRouteForAuthorizedUsers('quasar/iot/add', stub)).toEqual(true);
        expect(isRouteForAuthorizedUsers('quasar/iot/speaker/123', stub)).toEqual(true);
    });
});
