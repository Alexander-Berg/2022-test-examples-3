import { IRequest } from '@yandex-turbo/core/types/fetcher-params';
import fetcher from '../fetcher';

describe('applications/lpc/fetcher', () => {
    const req = { language: 'en', headers: {} } as IRequest;

    it('should return host from query', () => {
        expect(fetcher({
            ...req,
            query: {
                text: ['lpc/foobar'],
                host: ['http://foo.bar'],
            },
        })).toEqual({
            uri: 'http://foo.bar/api/turbo-json?text=lpc%2Ffoobar&lang=en',
            headers: {},
        });
    });

    it('should return production host for LPC', () => {
        expect(fetcher({
            ...req,
            query: {
                text: ['lpc-turbo/foobar'],
            },
        })).toEqual({
            uri: 'https://lp-constructor.yandex-team.ru/api/turbo-json?text=lpc-turbo%2Ffoobar&lang=en',
            headers: {},
        });
    });

    it('should return production host for UC', () => {
        expect(fetcher({
            ...req,
            query: {
                text: ['lpc/foobar'],
            },
        })).toEqual({
            uri: 'https://ad-constructor.yandex.ru/api/turbo-json?text=lpc%2Ffoobar&lang=en',
            headers: {},
        });
    });

    describe('beautiful turbo url', () => {
        it('should prefer turbo_key over text query', () => {
            expect(fetcher({
                ...req,
                turboKey: 'lpc/baz',
                query: {
                    text: ['lpc/foobar'],
                    host: ['http://foo.bar'],
                },
            })).toEqual({
                uri: 'http://foo.bar/api/turbo-json?text=lpc%2Fbaz&lang=en',
                headers: {},
            });
        });

        it('should return production host for LPC', () => {
            expect(fetcher({
                ...req,
                turboKey: 'lpc-turbo/foobar',
            })).toEqual({
                uri: 'https://lp-constructor.yandex-team.ru/api/turbo-json?text=lpc-turbo%2Ffoobar&lang=en',
                headers: {},
            });
        });

        it('should return production host for UC', () => {
            expect(fetcher({
                ...req,
                turboKey: 'lpc/foobar',
            })).toEqual({
                uri: 'https://ad-constructor.yandex.ru/api/turbo-json?text=lpc%2Ffoobar&lang=en',
                headers: {},
            });
        });
    });

    it('should return header "x-user-data" from headers', () => {
        expect(fetcher({
            ...req,
            headers: {
                'x-user-data': '{"foo": "bar"}',
            },
            query: {
                text: ['lpc-turbo/foobar'],
            },
        })).toEqual({
            uri: 'https://lp-constructor.yandex-team.ru/api/turbo-json?text=lpc-turbo%2Ffoobar&lang=en',
            headers: {
                'X-User-Data': '{"foo": "bar"}',
            },
        });
    });

    it('for UC should return url as "x-original-turbo-router-url" if header "x-original-turbo-router-url" NOT sent', () => {
        expect(fetcher({
            ...req,
            url: 'https://yandex.ru/turbo?text=lpc/jdkfbhjkdfhjkehrkejhwrww',
            query: {
                text: ['lpc/foobar'],
            },
        }).headers).toEqual({
            'x-original-turbo-router-url': 'https://yandex.ru/turbo?text=lpc/jdkfbhjkdfhjkehrkejhwrww',
        });
    });

    it('for LPC should return url as "x-original-turbo-router-url" if header "x-original-turbo-router-url" NOT sent', () => {
        expect(fetcher({
            ...req,
            url: 'https://yandex.ru/turbo?textlpc-turbo/jdkfbhjkdfhjkehrkejhwrww',
            query: {
                text: ['lpc-turbo/foobar'],
            },
        }).headers).toEqual({
            'x-original-turbo-router-url': 'https://yandex.ru/turbo?textlpc-turbo/jdkfbhjkdfhjkehrkejhwrww',
        });
    });

    it('for UC should return header "x-original-turbo-router-url" if header "x-original-turbo-router-url" sent', () => {
        expect(fetcher({
            ...req,
            headers: { 'x-original-turbo-router-url': 'my-site.turbo.site' },
            url: 'https://yandex.ru/turbo?text=lpc/jdkfbhjkdfhjkehrkejhwrww',
            query: {
                text: ['lpc/foobar'],
            },
        }).headers).toEqual({ 'x-original-turbo-router-url': 'my-site.turbo.site' });
    });

    it('for LPC should return header "x-original-turbo-router-url" if header "x-original-turbo-router-url" sent', () => {
        expect(fetcher({
            ...req,
            headers: { 'x-original-turbo-router-url': 'https://yandex.ru/promo/example/' },
            url: 'https://yandex.ru/turbo?textlpc-turbo/jdkfbhjkdfhjkehrkejhwrww',
            query: {
                text: ['lpc/foobar'],
            },
        }).headers).toEqual({ 'x-original-turbo-router-url': 'https://yandex.ru/promo/example/' });
    });
});
