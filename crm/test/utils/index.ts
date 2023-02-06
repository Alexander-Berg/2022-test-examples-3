import supertest, { Test, SuperTest } from 'supertest';
import { Request } from 'express';
import { v2 } from '@yandex-int/secret-key';
import { createApp } from 'createApp';
import { Config } from 'services/Config';

export const config = Config.getInstance();

jest.mock('@yandex-int/express-bunker', () => {
    return {
        __esModule: true,
        default: () => (req, res, next) => {
            req.bunker = { title: 'title_1', child: { title: 'title_2' } };
            next();
        },
    };
});

jest.mock('@yandex-int/express-blackbox', () => {
    const createBlackboxData = (status: string) => {
        return {
            error: 'OK',
            uid: 'uid',
            rawUid: {
                value: 'uid',
                lite: false,
                hosted: false,
            },
            havePassword: true,
            haveHint: false,
            karma: 0,
            displayName: 'login',
            avatar: {
                default: '0/0-0',
                empty: true,
            },
            status,
            age: 52785,
            ttl: '5',
            userTicket: 'user_ticket',
            raw: {
                age: 52785,
                expires_in: 7723215,
                ttl: '5',
                error: 'OK',
                status: {
                    value: status,
                    id: 0,
                },
                uid: {
                    value: '1120000000041619',
                    lite: false,
                    hosted: false,
                },
                login: 'login',
                have_password: true,
                have_hint: false,
                karma: {
                    value: 0,
                },
                karma_status: {
                    value: 0,
                },
                regname: 'login',
                display_name: {
                    name: 'login',
                    avatar: {
                        default: '0/0-0',
                        empty: true,
                    },
                },
                attributes: {},
                auth: {
                    password_verification_age: 4237308,
                    have_password: true,
                    secure: true,
                    partner_pdd_token: false,
                },
                connection_id: 'connection_id',
                user_ticket: 'user_ticket',
            },
            login: 'login',
        };
    };

    const getBlackboxDataByRequest = (req: Request) => {
        if (req.cookies.Session_id === 'auth') {
            return createBlackboxData('VALID');
        }

        if (req.cookies.Session_id === 'need_reset') {
            return createBlackboxData('NEED_RESET');
        }

        return {
            status: 'INVALID_PARAMS',
            error: 'BlackBox error: Missing userip argument',
        };
    };

    return {
        __esModule: true,
        default: () => (req: Request, res, next) => {
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            (req as any).blackbox = getBlackboxDataByRequest(req);

            next();
        },
    };
});

jest.mock('services/IndexHTMLProvider/IndexHTMLProvider');
jest.mock('services/Config/Config');

const app = createApp();

export const AUTH_COOKIE = 'Session_id=auth;yandexuid=yandexuid';
export const NEED_RESET_COOKIE = 'Session_id=need_reset;yandexuid=yandexuid';

export const csrfToken = v2({ uid: 'uid', salt: config.appKey, yandexuid: 'yandexuid' });

export const requestAuthWithoutCsrf = supertest.agent(app).set('Cookie', AUTH_COOKIE) as unknown as SuperTest<Test>;

export const requestAuth = supertest.agent(app).set('Cookie', AUTH_COOKIE).set('x-csrf-token', csrfToken) as unknown as SuperTest<Test>;
export const requestNeedReset = supertest.agent(app).set('Cookie', NEED_RESET_COOKIE).set('x-csrf-token', csrfToken) as unknown as SuperTest<Test>;

export const requestAnonymous = supertest(app);

export * from './defaultBunkerData';
