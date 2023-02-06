/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import * as sinon from 'sinon';
import { getUserTicket, testUser } from '../../api/_helpers';
import * as oauthEntities from '../../../../db/entities/oauthApps';
import * as dbEntities from '../../../../db/entities';
import { User, OAuthApp } from '../../../../db';
import * as socialService from '../../../../services/social';
import { callApi, respondsWithExistingModel } from './_helpers';

const test = anyTest as TestInterface<{ userTicket: string }>;

test.before(async t => {
    const userTicket = await getUserTicket(testUser.oauthToken);

    Object.assign(t.context, { userTicket });
});

/**
 * Этот тест не зависит от базы.
 * Может выполняться параллельно.
 * Но в unit его неправильно класть, потому что это функциональный тест.
 * Нужно для таких тестов завести в будущем отдельное место.
 * И все тесты из functional переписать на независимые от базы копии.
 */

test('get oauth app by id', async t => {
    sinon.stub(dbEntities, 'findUserWithId').value(async() =>
        User.build({
            id: testUser.uid,
        }),
    );
    sinon.stub(oauthEntities, 'userHasPermissionForApp').value(async() => true);
    sinon.stub(oauthEntities, 'findUserOauthApp').value(async() =>
        OAuthApp.build({
            id: '867f879b-ab2f-45b2-9a10-9ef9f89aad4c',
            userId: testUser.uid,
            name: 'link1',
            socialAppName: 'f88a6db5aeff4aa09e812344644da197',
            deletedAt: null,
        }),
    );
    sinon.stub(socialService, 'getStationApp').value(async() => {
        return {
            applications: {
                f88a6db5aeff4aa09e812344644da197: {
                    application_name: 'f88a6db5aeff4aa09e812344644da197',
                    authorization_url: 'https://yandex.ru/auth',
                    client_id: '1',
                    masked_client_secret: '*****',
                    client_secret: '1112',
                    refresh_token_url: 'https://yandex.ru/token',
                    scope: '1',
                    token_url: 'https://yandex.ru/token',
                    yandex_client_id: 'fake_client_id_1',
                },
            },
            status: 'ok',
        };
    });

    const res = await callApi('get', '/oauth/apps/867f879b-ab2f-45b2-9a10-9ef9f89aad4c', {
        userTicket: t.context.userTicket,
    });
    respondsWithExistingModel(
        {
            id: '867f879b-ab2f-45b2-9a10-9ef9f89aad4c',
            name: 'link1',
            clientId: '1',
            authorizationUrl: 'https://yandex.ru/auth',
            tokenUrl: 'https://yandex.ru/token',
            refreshTokenUrl: 'https://yandex.ru/token',
            scope: '1',
            yandexClientId: 'fake_client_id_1',
        },
        res,
        t,
    );

    sinon.restore();
});
