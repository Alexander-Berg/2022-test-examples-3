/* eslint-disable */
import test from 'ava';
import * as sinon from 'sinon';
import { callApi } from './_helpers';
import { createImageForSkill, createOAuthApp, createSkill, createUser, wipeDatabase } from '../../_helpers';
import * as socialService from '../../../../services/social';

test.beforeEach(async t => {
    await wipeDatabase();
});

test('account linking returns 404 if skill not found', async t => {
    const skillId = 'b8d31875-8d3c-4300-a6f2-0483ce6f6712';
    const response = await callApi('get', `/dialogs/${skillId}/account_linking`);
    t.is(response.status, 404);
});

test('account linking returns 409 if no OAuth app is assigned to skill', async t => {
    await createUser();
    const skill = await createSkill({
        onAir: true,
    });
    const logo = await createImageForSkill(skill);
    skill.logoId = logo.id;
    await skill.save();
    const response = await callApi('get', `/dialogs/${skill.id}/account_linking`);
    t.is(response.status, 409);
});

test('account linking returns valid response if OAuth app is assigned to skill', async t => {
    sinon.stub(socialService, 'getStationApp').value(async() => {
        return {
            applications: {
                'social app name': {
                    application_name: 'social app name',
                    authorization_url: 'https://example.com/auth',
                    client_id: '1',
                    masked_client_secret: '*****',
                    client_secret: '1112',
                    refresh_token_url: 'https://example.com/token',
                    scope: '1',
                    token_url: 'https://example.com/token',
                    yandex_client_id: 'yandex client id',
                },
            },
            status: 'ok',
        };
    });
    await createUser();
    const skill = await createSkill({
        onAir: true,
        name: 'skill name',
        slug: 'skill-slug',
        publishingSettings: {
            developerName: 'skill developer',
            description: 'skill description',
            smartHome: {
                deepLinks: {
                    ios: {
                        url: 'https://ios_deep_link',
                        fallbackUrl: 'https://ios_deep_link',
                    },
                    android: {
                        url: 'https://android_deep_link',
                    },
                },
            },
        },
    });
    const oauthApp = await createOAuthApp();
    const logo = await createImageForSkill(skill);
    skill.logoId = logo.id;
    skill.oauthAppId = oauthApp.id;
    await skill.save();
    const response = await callApi('get', `/dialogs/${skill.id}/account_linking`);
    t.is(response.status, 200);
    t.deepEqual(response.body, {
        result: {
            id: skill.id,
            slug: 'skill-slug',
            name: 'skill name',
            description: 'skill description',
            developerName: 'skill developer',
            logo: logo.url,
            trusted: false,
            secondaryTitle: '',
            deepLinks: {
                ios: {
                    url: 'https://ios_deep_link',
                    fallbackUrl: 'https://ios_deep_link',
                },
                android: {
                    url: 'https://android_deep_link',
                },
            },
            accountLinking: {
                applicationName: 'social app name',
                clientId: 'yandex client id',
            },
        },
    });
    sinon.restore();
});

test('account linking returns valid response if OAuth app is assigned to skill (partial deeplinks)', async t => {
    sinon.stub(socialService, 'getStationApp').value(async() => {
        return {
            applications: {
                'social app name': {
                    application_name: 'social app name',
                    authorization_url: 'https://example.com/auth',
                    client_id: '1',
                    masked_client_secret: '*****',
                    client_secret: '1112',
                    refresh_token_url: 'https://example.com/token',
                    scope: '1',
                    token_url: 'https://example.com/token',
                    yandex_client_id: 'yandex client id',
                },
            },
            status: 'ok',
        };
    });
    await createUser();
    const skill = await createSkill({
        onAir: true,
        name: 'skill name',
        slug: 'skill-slug',
        isTrustedSmartHomeSkill: true,
        publishingSettings: {
            developerName: 'skill developer',
            description: 'skill description',
            secondaryTitle: 'secondary title',
            smartHome: {
                deepLinks: {
                    android: {
                        url: 'https://android_deep_link',
                    },
                },
            },
        },
    });
    const oauthApp = await createOAuthApp();
    const logo = await createImageForSkill(skill);
    skill.logoId = logo.id;
    skill.oauthAppId = oauthApp.id;
    await skill.save();
    const response = await callApi('get', `/dialogs/${skill.id}/account_linking`);
    t.is(response.status, 200);
    t.deepEqual(response.body, {
        result: {
            id: skill.id,
            slug: 'skill-slug',
            name: 'skill name',
            description: 'skill description',
            developerName: 'skill developer',
            logo: logo.url,
            secondaryTitle: 'secondary title',
            trusted: true,
            deepLinks: {
                ios: {
                    url: null,
                    fallbackUrl: null,
                },
                android: {
                    url: 'https://android_deep_link',
                },
            },
            accountLinking: {
                applicationName: 'social app name',
                clientId: 'yandex client id',
            },
        },
    });
    sinon.restore();
});
