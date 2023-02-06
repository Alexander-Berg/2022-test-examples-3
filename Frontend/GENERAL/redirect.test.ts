import { createApphostContextStub } from '../helpers/create-apphost-context-stub';

import { generateRedirectUrl } from './redirect';

describe('middlewares/redirect: generateRedirectUrl', () => {
    it('Должен корректно обрабатываться относительный путь без query-параметров', () => {
        expect(generateRedirectUrl(createApphostContextStub({
            request: {
                headers: {},
                path: '/quasar/iot',
                uri: '/quasar/iot',
            },
        }), { path: '/promo' })).toEqual('/quasar/promo');
    });

    it('Должен корректно обрабатываться относительный путь с query-параметрами', () => {
        expect(generateRedirectUrl(createApphostContextStub({
            request: {
                headers: {},
                path: '/quasar/iot',
                uri: '/quasar/iot?renderer_export=binary&no_bolver=1&json=request&some-test-flag=1',
            },
        }), { path: '/promo' })).toEqual('/quasar/promo?json=request&some-test-flag=1');
    });

    it('Должен корректно обрабатываться абсолютный путь', () => {
        expect(generateRedirectUrl(createApphostContextStub({}), { url: 'https://example.com/example' }))
            .toEqual('https://example.com/example');
    });
});
