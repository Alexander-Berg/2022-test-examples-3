import type { Request, Response } from 'express';
import { expressYandexCsp, SELF, INLINE, NONE } from '../src';

test('should send csp header', () => {
    const { headers } = applyMiddleware(expressYandexCsp({
        directives: {
            'default-src': [SELF],
            'script-src': [SELF, INLINE, 'somehost.com'],
            'style-src': [SELF, 'mystyles.net'],
            'img-src': ['data:', 'images.com'],
            'worker-src': [NONE],
            'block-all-mixed-content': true,
        },
    }));

    expect(headers['Content-Security-Policy']).toEqual(
        "default-src 'self'; script-src 'self' 'unsafe-inline' somehost.com; style-src 'self' mystyles.net; img-src data: images.com; worker-src 'none'; block-all-mixed-content;"
    );
});

test('should send empty csp header', () => {
    const { headers } = applyMiddleware(expressYandexCsp({}));

    expect(headers['Content-Security-Policy']).toEqual('');
});

test('should replace tld placeholder', () => {
    const { headers } = applyMiddleware(
        expressYandexCsp({
            directives: { 'script-src': ['service.yandex.%tld%'] },
        }),
        { hostname: 'yandex.ua' }
    );

    expect(headers['Content-Security-Policy']).toEqual('script-src service.yandex.ua;');
});

test('should replace non-ICANN tld placeholder', () => {
    const { headers } = applyMiddleware(
        expressYandexCsp({
            directives: { 'script-src': ['service.yandex.%tld%'] },
        }),
        { hostname: 'yandex.com.am' }
    );

    expect(headers['Content-Security-Policy']).toEqual('script-src service.yandex.com.am;');
});

test('should throw error if default report-uri enbaled and req.cookie is not defined', () => {
    expect(() => {
        applyMiddleware(
            expressYandexCsp({
                directives: {
                    'script-src': [SELF],
                },
                useDefaultReportUri: true,
                from: 'express-yandex-csp',
            }),
            {}
        );
    }).toThrowError();
});

test('should use default report uri', () => {
    const { headers } = applyMiddleware(
        expressYandexCsp(
            {
                directives: {
                    'script-src': [SELF],
                },
                useDefaultReportUri: true,
                from: 'ru.touch.my-project',
                project: 'my-project',
            }
        ),
        {
            cookies: {
                yandex_login: 'yauser',
                yandexuid: '1234567890',
            },
        },
    );

    expect(headers['Content-Security-Policy']).toEqual(
        "script-src 'self'; report-to default-group; report-uri https://csp.yandex.net/csp?yandex_login=yauser&yandexuid=1234567890&from=ru.touch.my-project&project=my-project;"
    );

    expect(headers['Report-To']).toEqual(
        '{"group":"default-group","endpoints":[{"url":"https://csp.yandex.net/csp?yandex_login=yauser&yandexuid=1234567890&from=ru.touch.my-project&project=my-project"}],"max_age":1800,"include_subdomains":true}'
    );
});

test('should not send report-to if disabled', () => {
    const { headers } = applyMiddleware(
        expressYandexCsp(
            {
                directives: {
                    'script-src': [SELF],
                },
                disableReportTo: true,
                useDefaultReportUri: true,
                from: 'ru.touch.my-project',
                project: 'my-project',
            }
        ),
        {
            cookies: {
                yandex_login: 'yauser',
                yandexuid: '1234567890',
            },
        },
    );

    expect(headers['Content-Security-Policy']).toEqual(
        "script-src 'self'; report-uri https://csp.yandex.net/csp?yandex_login=yauser&yandexuid=1234567890&from=ru.touch.my-project&project=my-project;"
    );

    expect(headers['Report-To']).toBeUndefined();
});

test('should encode symbols', () => {
    const { headers } = applyMiddleware(
        expressYandexCsp({
            directives: {
                'script-src': ['myhost.com'],
            },
            useDefaultReportUri: true,
            project: 'express-yandex-csp',
        }), {
            cookies: {
                yandex_login: '\u001f',
                yandexuid: 'Ā',
            },
        }
    );

    expect(headers['Content-Security-Policy']).toEqual(
        'script-src myhost.com; report-to default-group; report-uri https://csp.yandex.net/csp?yandex_login=%1F&yandexuid=%C4%80&project=express-yandex-csp;'
    );
});

function applyMiddleware(middleware: ReturnType<typeof expressYandexCsp>, req: Partial<Request> = {}) {
    const headers: Record<string, string> = {};
    const res = {
        set: (headerName: string, headerVal: string) => {
            headers[headerName] = headerVal;
        },
    };

    middleware(
        req as Request,
        res as Response,
        () => {},
    );

    return { headers };
}
