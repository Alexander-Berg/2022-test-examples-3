/* eslint-disable */
import * as nock from 'nock';
import * as sinon from 'sinon';
import anyTest, { TestInterface, ExecutionContext } from 'ava';
import config from '../../../../services/config';
import * as tvm from '../../../../services/tvm';
import { getUserTicket, testUser } from '../../api/_helpers';
import { wipeDatabase, createUser, createOAuthApp } from '../../_helpers';
import { callApi, respondsWithError } from '../../api/dev-console/_helpers';

const test = anyTest as TestInterface<{ userTicket: string }>;

const scope = nock(config.social.url);

test.before(async t => {
    t.context.userTicket = await getUserTicket(testUser.oauthToken);
});

test.beforeEach(async() => {
    nock.cleanAll();
    sinon.restore();
    await wipeDatabase();
    await createUser({ id: testUser.uid });
});

test('Should respond error on creation correctly', async t => {
    const { res } = await callCreateOauthApp(t, {
        responseBody: {
            status: 'error',
            request_id: '454624-c9d07d2a1d9b4d3b8035d317bd1441bb-1580112303',
            errors: ['authorization_url.invalid', 'token_url.empty', 'client_id.long'],
        },
        responseCode: 200,
    });

    respondsWithError(
        {
            code: 400,
            message: 'Bad request',
            fields: {
                authorizationUrl: 'Некорректное значение',
                tokenUrl: 'Обязательное поле',
                clientId: 'Введено слишком длинное значение',
            },
        },
        res,
        t,
    );
});

test('Should respond error on creation correctly 2', async t => {
    const { res } = await callCreateOauthApp(t, {
        responseBody: {
            status: 'error',
            request_id: '454624-c9d07d2a1d9b4d3b8035d317bd1441bb-1580112303',
            errors: ['internal_error'],
        },
        responseCode: 200,
    });

    respondsWithError(
        {
            code: 500,
            message: 'Unknown social error',
        },
        res,
        t,
    );
});

test('Should respond error on update correctly', async t => {
    const { res } = await callUpdateOauthApp(t, {
        responseBody: {
            status: 'error',
            request_id: '454624-c9d07d2a1d9b4d3b8035d317bd1441bb-1580112303',
            errors: ['authorization_url.invalid', 'token_url.empty', 'client_id.long'],
        },
        responseCode: 200,
    });

    respondsWithError(
        {
            code: 400,
            message: 'Bad request',
            fields: {
                authorizationUrl: 'Некорректное значение',
                tokenUrl: 'Обязательное поле',
                clientId: 'Введено слишком длинное значение',
            },
        },
        res,
        t,
    );
});

test('Should respond error on update correctly 2', async t => {
    const { res } = await callUpdateOauthApp(t, {
        responseBody: {
            status: 'error',
            request_id: '454624-c9d07d2a1d9b4d3b8035d317bd1441bb-1580112303',
            errors: ['internal_error'],
        },
        responseCode: 200,
    });

    respondsWithError(
        {
            code: 500,
            message: 'Unknown social error',
        },
        res,
        t,
    );
});

const callCreateOauthApp = async(
    t: ExecutionContext<{ userTicket: string }>,
    params: { responseBody?: any; responseCode: number },
) => {
    sinon.replace(tvm, 'getServiceTickets', sinon.fake.resolves({ social: { ticket: '' } }));

    scope.post(/\/create_station_application*/).reply(params.responseCode, params.responseBody);

    return {
        res: await callApi('post', '/oauth/apps', { userTicket: t.context.userTicket }).send({
            name: 'test',
            clientId: 'test',
            clientSecret: 'test',
            authorizationUrl: 'https://example.com',
            tokenUrl: 'https://example.com',
            refreshTokenUrl: 'https://example.com',
            scope: 'test',
            yandexClientId: 'test',
            userId: testUser.uid,
        }),
    };
};

const callUpdateOauthApp = async(
    t: ExecutionContext<{ userTicket: string }>,
    params: { responseBody?: any; responseCode: number },
) => {
    sinon.replace(tvm, 'getServiceTickets', sinon.fake.resolves({ social: { ticket: '' } }));
    const app = await createOAuthApp({ userId: testUser.uid });

    scope.post(/\/change_station_application*/).reply(params.responseCode, params.responseBody);

    return {
        res: await callApi('post', '/oauth/apps', { userTicket: t.context.userTicket }).send({
            id: app.id,
            name: 'test',
            clientId: 'test',
            clientSecret: 'test',
            authorizationUrl: 'https://example.com',
            tokenUrl: 'https://example.com',
            refreshTokenUrl: 'https://example.com',
            scope: 'test',
            yandexClientId: 'test',
            userId: testUser.uid,
        }),
    };
};
