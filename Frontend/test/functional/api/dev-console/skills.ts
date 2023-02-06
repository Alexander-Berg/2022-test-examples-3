/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import { createSandbox, fake } from 'sinon';
import { v4 as uuid } from 'uuid';
import { OAuthApp } from '../../../../db';
import { defaultVoice, Voice } from '../../../../fixtures/voices';
import { approveReview, requestDeploy } from '../../../../services/skill-lifecycle';
import * as socialService from '../../../../services/social';
import * as webmaster from '../../../../services/webmaster';
import { createSkill, createUser, wipeDatabase } from '../../_helpers';
import { getUserTicket, testUser } from '../_helpers';
import {
    callApi,
    respondsWithCreatedModelContains,
    respondsWithError,
    respondsWithExistingModel,
    respondsWithExistingModelContains,
} from './_helpers';

const test = anyTest as TestInterface<{ userTicket: string }>;

test.before(async t => {
    const userTicket = await getUserTicket(testUser.oauthToken);

    Object.assign(t.context, { userTicket });
});

test.beforeEach(async() => {
    await wipeDatabase();
});

test('Get empty skills', async t => {
    await createUser({ id: testUser.uid });
    const res = await callApi('get', '/skills', t.context);

    respondsWithExistingModel([], res, t);
});

test("Ignore other's skills", async t => {
    await createUser({ id: testUser.uid });
    await createUser({ id: '0002', name: 'user2' });
    await createSkill({ userId: '0002' });

    const res = await callApi('get', '/skills', t.context);

    respondsWithExistingModel([], res, t);
});

test('Get own skills', async t => {
    await createUser({ id: testUser.uid });
    await createSkill({ userId: testUser.uid });

    const res = await callApi('get', '/skills', t.context);

    respondsWithExistingModelContains({}, res, t);
});

test('Create skill', async t => {
    await createUser({ id: testUser.uid });

    const res = await callApi('post', '/skills', t.context).send({
        name: 'skill',
        channel: 'aliceSkill',
    });

    respondsWithCreatedModelContains({}, res, t);
});

test('Get own skill', async t => {
    await createUser({ id: testUser.uid });
    const skill = await createSkill({ userId: testUser.uid });

    const res = await callApi('get', `/skills/${skill.id}`, t.context);

    respondsWithExistingModelContains(
        {
            id: skill.id,
        },
        res,
        t,
    );
});

test('Get unknown skill', async t => {
    await createUser({ id: testUser.uid });

    const res = await callApi('get', '/skills/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', t.context);

    respondsWithError(
        {
            code: 404,
            message: 'Resource not found',
        },
        res,
        t,
    );
});

test("Get other's skill", async t => {
    await createUser({ id: testUser.uid });
    await createUser({ id: '0002', name: 'other' });
    const skill = await createSkill({ userId: '0002' });

    const res = await callApi('get', `/skills/${skill.id}`, t.context);

    respondsWithError(
        {
            code: 403,
            message: 'Access violation',
        },
        res,
        t,
    );
});

test('Send draft to deploy', async t => {
    const user = await createUser({ id: testUser.uid });
    const skill = await createSkill({ userId: testUser.uid });

    await approveReview(skill, { user });

    const res = await callApi('post', `/skills/${skill.id}/release`, t.context);

    t.is(res.body.result.skill.draft.status, 'deployRequested');
    respondsWithCreatedModelContains(
        {
            published: false,
        },
        res,
        t,
    );
});

test('create skill voice should be default', async t => {
    await createUser({ id: testUser.uid });

    const {
        body: { result: skill },
    } = await callApi('post', '/skills', t.context).send({
        name: 'skill',
        channel: 'aliceSkill',
    });

    const res = await callApi('get', `/skills/${skill.id}`, t.context);

    t.is(res.body.result.draft.voice, defaultVoice);
    t.is(res.body.result.voice, defaultVoice);
});

test('update draft set voice non default', async t => {
    await createUser({ id: testUser.uid });

    const {
        body: { result: skill },
    } = await callApi('post', '/skills', t.context).send({
        name: 'skill',
        channel: 'aliceSkill',
    });

    const voice = Voice.Ermil;

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        voice,
        publishingSettings: {},
    });

    t.is(res.body.result.voice, voice);
});

test('clear jivositeId when yandex provider is selected', async t => {
    await createUser({ id: testUser.uid });

    const {
        body: { result: skill },
    } = await callApi('post', '/skills', t.context).send({
        name: 'skill',
        channel: 'organizationChat',
    });

    let res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        backendSettings: {
            provider: 'jivosite',
            jivositeId: '123',
        },
    });

    t.is(res.body.result.backendSettings.jivositeId, '123');

    res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        backendSettings: {
            provider: 'yandex',
        },
    });

    t.is(res.body.result.backendSettings.jivositeId, '');
});

test('prevent draft update if not in development', async t => {
    const user = await createUser({ id: testUser.uid });
    const skill = await createSkill({ userId: testUser.uid });

    await requestDeploy(skill, { user });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        name: 'new name',
    });

    respondsWithError(
        {
            code: 403,
            message: 'Operation not allowed',
        },
        res,
        t,
    );
});

test('prevent publishing without moderation', async t => {
    await createUser({ id: testUser.uid });
    const skill = await createSkill({ userId: testUser.uid });

    const res = await callApi('post', `/skills/${skill.id}/release`, t.context);

    respondsWithError(
        {
            code: 403,
            message: 'Operation not allowed',
        },
        res,
        t,
    );
});

test('brand verified if website changed', async t => {
    const sinon = createSandbox();
    const verifyUrl = fake.returns(true);
    sinon.replace(webmaster, 'verifyUrl', verifyUrl);

    await createUser({ id: testUser.uid });
    const skill = await createSkill({ userId: testUser.uid });

    await skill.draft.update({
        publishingSettings: {
            brandVerificationWebsite: 'https://example.org',
        },
    });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        publishingSettings: {
            brandVerificationWebsite: 'https://example.org/1',
        },
    });

    t.true(verifyUrl.calledOnce);
    t.true(res.body.result.publishingSettings.brandIsVerified);

    sinon.restore();
});

test('brand not verified if already verified and website not changed', async t => {
    const sinon = createSandbox();
    const verifyUrl = fake.returns(false);
    sinon.replace(webmaster, 'verifyUrl', verifyUrl);

    await createUser({ id: testUser.uid });
    const skill = await createSkill({ userId: testUser.uid });

    await skill.draft.update({
        publishingSettings: {
            brandVerificationWebsite: 'https://example.org',
            brandIsVerified: true,
        },
    });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        publishingSettings: {
            brandVerificationWebsite: 'https://example.org',
        },
    });

    t.true(verifyUrl.notCalled);
    t.true(res.body.result.publishingSettings.brandIsVerified);

    sinon.restore();
});

test('brand verified if not yet verified and website not changed', async t => {
    const sinon = createSandbox();
    const verifyUrl = fake.returns(true);
    sinon.replace(webmaster, 'verifyUrl', verifyUrl);

    await createUser({ id: testUser.uid });
    const skill = await createSkill({ userId: testUser.uid });

    await skill.draft.update({
        publishingSettings: {
            brandVerificationWebsite: 'https://example.org',
            brandIsVerified: false,
        },
    });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        publishingSettings: {
            brandVerificationWebsite: 'https://example.org',
        },
    });

    t.true(verifyUrl.calledOnce);
    t.true(res.body.result.publishingSettings.brandIsVerified);

    sinon.restore();
});

test('brand should be auto verified for Yandex user', async t => {
    const sinon = createSandbox();
    const verifyUrl = sinon.spy(webmaster, 'verifyUrl');

    await createUser({
        id: testUser.uid,
        featureFlags: {
            allowConfigureYandexChat: true,
        },
    });
    const skill = await createSkill({ userId: testUser.uid });

    await skill.draft.update({
        publishingSettings: {
            brandVerificationWebsite: 'https://example.org',
            brandIsVerified: false,
        },
    });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        publishingSettings: {
            brandVerificationWebsite: 'https://example.org',
        },
    });

    t.true(verifyUrl.calledOnce);
    t.assert(verifyUrl.returned(Promise.resolve(true)));
    t.true(res.body.result.publishingSettings.brandIsVerified);

    sinon.restore();
});

test('saving draft with account linking by another user is forbidden', async t => {
    await createUser({ id: testUser.uid });

    const skill = await createSkill({
        userId: testUser.uid,
        name: 'foo',
    });

    const anotherUser = await createUser({ id: uuid() });

    const oauthApp = await OAuthApp.create({
        name: 'name',
        userId: anotherUser.id,
        socialAppName: 'socialAppName',
    });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        name: 'bar',
        oauthAppId: oauthApp.id,
    });

    respondsWithError(
        {
            code: 403,
            message: 'Operation not allowed',
        },
        res,
        t,
    );
});

test('saving draft with account linking by owner is permitted', async t => {
    await createUser({ id: testUser.uid });
    const sinon = createSandbox();
    sinon.replace(socialService, 'changeStationApp', fake.returns(Promise.resolve({})));

    const skill = await createSkill({
        userId: testUser.uid,
        name: 'foo',
    });

    const oauthApp = await OAuthApp.create({
        name: 'name',
        userId: testUser.uid,
        socialAppName: 'socialAppName',
    });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        name: 'bar',
        oauthAppId: oauthApp.id,
    });

    respondsWithExistingModelContains(
        {
            oauthAppId: oauthApp.id,
        },
        res,
        t,
    );

    sinon.restore();
});

test('saving draft with account linking by admin permitted', async t => {
    const user = await createUser({ id: uuid() });

    const sinon = createSandbox();
    sinon.replace(socialService, 'changeStationApp', fake.returns(Promise.resolve({})));

    const oauthApp = await OAuthApp.create({
        name: 'name',
        userId: user.id,
        socialAppName: 'socialAppName',
    });

    const skill = await createSkill({
        userId: user.id,
        name: 'foo',
        oauthAppId: oauthApp.id,
    });

    t.is(skill.oauthAppId, oauthApp.id);

    await createUser({
        id: testUser.uid,
        roles: ['admin'],
    });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        name: 'bar',
    });

    respondsWithExistingModelContains(
        {
            name: 'bar',
            oauthAppId: oauthApp.id,
        },
        res,
        t,
    );

    sinon.restore();
});
