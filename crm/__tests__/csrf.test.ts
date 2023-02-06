import { createGrpc } from '@crm/apphost-test';
import { NAppHostHttp } from '@crm/protos';
import { v2 } from '@yandex-int/secret-key';
import cookie from 'cookie';
import { Config } from 'services/Config';
import { createApp } from '../createApp';
import { CSRF_COOKIE_KEY, CSRF_HEADER_KEY } from '../handlers/csrf';

const EMethod = NAppHostHttp.THttpRequest.EMethod;

jest.useFakeTimers().setSystemTime(new Date('2020-01-01'));

const createContext = (data: {
    cookie?: string;
    uid?: string;
    csrfToken?: string;
    method?: number;
}): object => {
    const { cookie, uid, csrfToken, method = EMethod.Post } = data;

    const Headers = [];
    if (cookie) {
        Headers.push({
            Name: 'Cookie',
            Value: cookie,
        });
    }

    if (csrfToken) {
        Headers.push({
            Name: CSRF_HEADER_KEY,
            Value: csrfToken,
        });
    }

    const results: object[] = [
        {
            type: 'proto_http_request',
            binary: {
                Method: method,
                Headers,
            },
            __content_type: 'json',
        },
    ];
    if (uid != null) {
        results.push({
            type: 'blackbox_user',
            binary: {
                Uid: uid,
            },
            __content_type: 'json',
        });
    }

    return [
        {
            name: 'REQUEST',
            results,
        },
    ];
};

describe('/csrf', () => {
    const grpc = createGrpc(createApp);

    it('returns new token', async () => {
        const response = await grpc('/csrf', {
            context: createContext({
                cookie: 'yandexuid=123',
                uid: '321',
            }),
        });

        const setCookieHeader = response.answers[0].Headers.find(
            (header: NAppHostHttp.THeader) =>
                String(header.Name) === 'Set-Cookie',
        );
        const cookies = cookie.parse(setCookieHeader.Value);

        expect(cookies[CSRF_COOKIE_KEY]).toBe(
            v2({
                uid: '321',
                yandexuid: '123',
                salt: Config.defaultSalt,
            }),
        );
    });

    it('returns new token if no blackbox uid', async () => {
        const response = await grpc('/csrf', {
            context: createContext({
                cookie: 'yandexuid=123',
            }),
        });

        const setCookieHeader = response.answers[0].Headers.find(
            (header: NAppHostHttp.THeader) =>
                String(header.Name) === 'Set-Cookie',
        );
        const cookies = cookie.parse(setCookieHeader.Value);

        expect(cookies[CSRF_COOKIE_KEY]).toBe(
            v2({
                yandexuid: '123',
                salt: Config.defaultSalt,
            }),
        );
    });

    it('returns csrf-valid flag', async () => {
        const response = await grpc('/csrf', {
            context: createContext({
                cookie: 'yandexuid=123',
                csrfToken: v2({
                    yandexuid: '123',
                    salt: Config.defaultSalt,
                }),
            }),
        });

        expect(response.meta).toContain('csrf-valid');
    });

    it("returns 'Invalid CSRF token' when no yandexuid cookie", async () => {
        const response = await grpc('/csrf', {
            context: createContext({
                uid: '321',
            }),
        });

        const content = Buffer.from(
            response.answers[0].Content,
            'base64',
        ).toString('ascii');

        expect(/Invalid CSRF token/.test(content)).toBe(true);
    });

    it("returns 'Invalid CSRF token' when it is fake", async () => {
        const response = await grpc('/csrf', {
            context: createContext({
                cookie: 'yandexuid=123',
                uid: '321',
                csrfToken: v2({
                    yandexuid: 123,
                    uid: '321',
                    salt: 'fake',
                }),
            }),
        });

        const content = Buffer.from(
            response.answers[0].Content,
            'base64',
        ).toString('ascii');

        expect(/Invalid CSRF token/.test(content)).toBe(true);
    });

    it("returns 'Invalid CSRF token' when it is expired", async () => {
        const oldToken = v2({
            yandexuid: 123,
            uid: '321',
            salt: Config.defaultSalt,
            timestamp: Date.now(),
        });
        jest.advanceTimersByTime(1000 * 60 * 60 * 25);
        const response = await grpc('/csrf', {
            context: createContext({
                cookie: 'yandexuid=123',
                uid: '321',
                csrfToken: oldToken,
            }),
        });

        const content = Buffer.from(
            response.answers[0].Content,
            'base64',
        ).toString('ascii');

        expect(/Invalid CSRF token/.test(content)).toBe(true);
    });

    it('returns 403 status code when token is invalid', async () => {
        const response = await grpc('/csrf', {
            context: createContext({
                cookie: 'yandexuid=123',
                uid: '321',
                csrfToken: v2({
                    yandexuid: 1,
                    uid: '1',
                    salt: 'fake',
                }),
            }),
        });

        const statusCode = response.answers[0].StatusCode;

        expect(statusCode).toBe(403);
    });

    it('returns undefined status code when token is valid', async () => {
        const response = await grpc('/csrf', {
            context: createContext({
                cookie: 'yandexuid=123',
                uid: '321',
                csrfToken: v2({
                    yandexuid: 123,
                    uid: '321',
                    salt: Config.defaultSalt,
                }),
            }),
        });

        const statusCode = response.answers[0].StatusCode;

        expect(statusCode).toBeUndefined();
    });

    it("doesn't validate GET method request", async () => {
        const response = await grpc('/csrf', {
            context: createContext({
                method: EMethod.Get,
                cookie: '',
                uid: '321',
                csrfToken: v2({
                    yandexuid: 123,
                    uid: '456',
                    salt: 'fake',
                }),
            }),
        });

        expect(response.answers[0].Content).toBeFalsy();
    });

    it("doesn't validate HEAD method request", async () => {
        const response = await grpc('/csrf', {
            context: createContext({
                method: EMethod.Head,
                cookie: '',
                uid: '321',
                csrfToken: v2({
                    yandexuid: 123,
                    uid: '456',
                    salt: 'fake',
                }),
            }),
        });

        expect(response.answers[0].Content).toBeFalsy();
    });

    it("doesn't validate OPTIONS method request", async () => {
        const response = await grpc('/csrf', {
            context: createContext({
                method: EMethod.Options,
                cookie: '',
                uid: '321',
                csrfToken: v2({
                    yandexuid: 123,
                    uid: '456',
                    salt: 'fake',
                }),
            }),
        });

        expect(response.answers[0].Content).toBeFalsy();
    });
});
