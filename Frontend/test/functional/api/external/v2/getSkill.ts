/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import * as sinon from 'sinon';
import * as promiseTimeout from 'promise-timeout';
import config from '../../../../../services/config';
import { Channel } from '../../../../../db/tables/settings';
import { getServiceTicket, getUserTicket, testUser } from '../../_helpers';
import {
    createImage,
    createImageForSkill,
    createOAuthApp,
    createSkill,
    createUser,
    wipeDatabase,
} from '../../../_helpers';
import { callApi, respondsWithError, respondsWithResult, respondsWithResultBulk } from './_helpers';
import { ImageType } from '../../../../../db/tables/image';
import { ImplicitSurface, Surface } from '../../../../../services/surface';
import * as skillInfo from '../../../../../services/skillInfo';
import * as unistat from '../../../../../services/unistat';
import * as apiPumpkin from '../../../../../services/api-pumpkin';
import * as skillRepository from '../../../../../db/repositories/skill';
import { testPumpkin } from '../../../../../fixtures/pumpkin';
import { mapSkillInfoV1ToV2 } from '../../../../../serializers/skills';
import {
    approveReview,
    completeDeploy,
    requestDeploy,
    requestReview,
} from '../../../../../services/skill-lifecycle';
import { CategoryType } from '../../../../../fixtures/categories';
import { UserReview } from '../../../../../db';
import { getPoolSet } from '../../../../../lib/pgPool';

const disabledCache = {
    get: () => null,
    set: () => null,
};

const test = anyTest as TestInterface<{ serviceTicket: string; userTicket: string }>;

test.before(async t => {
    // waiting for pg service startup
    await getPoolSet();
    const serviceTicket = await getServiceTicket();
    const userTicket = await getUserTicket(testUser.oauthToken);

    Object.assign(t.context, { serviceTicket, userTicket });
});

test.beforeEach(wipeDatabase);

test.afterEach.always(t => {
    sinon.restore();
});

// --- single request ---

test('returns skill status (without/with cache)', async t => {
    // - init db -

    await createUser();
    const skill = await createSkill({
        isTrustedSmartHomeSkill: true,
        publishingSettings: {
            secondaryTitle: 'secondary title',
            category: CategoryType.business_finance,
        },
    });
    const image = await createImage({
        type: ImageType.SkillSettings,
        skillId: skill.id,
        url: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
        origUrl: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
    });
    await skill.update({
        logoId: image.id,
        rating: [7, 2, 3, 5, 8],
        backendSettings: { uri: 'https://example.com/webhook' },
    });

    // - test without cache (obtained from db) -

    const incGetSkillCacheStat = sinon.spy(unistat, 'incGetSkillCacheStat');

    let res = await callApi('get', `/skills/${skill.id}`, {
        serviceTicket: t.context.serviceTicket,
    });

    t.true(incGetSkillCacheStat.calledOnceWith('miss'));
    respondsWithResult(
        {
            id: skill.id,
            channel: 'aliceSkill',
            userId: skill.userId,
            salt: skill.salt,
            name: 'skill 1',
            useZora: true,
            onAir: false,
            botGuid: null,
            isRecommended: true,
            isVip: false,
            logo: {
                avatarId: '5182/429f04ace2d88763a581',
            },
            look: 'external',
            monitoringType: 'nonmonitored',
            openInNewTab: true,
            surfaces: [Surface.Auto, Surface.Navigator, Surface.Station],
            storeUrl: 'https://localhost.msup.yandex.ru:8083/skills/',
            voice: 'good_oksana',
            useNLU: false,
            category: 'business_finance',
            categoryLabel: 'Бизнес и финансы',
            ratingHistogram: [7, 2, 3, 5, 8],
            averageRating: 3.2,
            userReview: null,
            accountLinking: null,
            backendUrl: 'https://example.com/webhook',
            functionId: null,
            trusted: true,
            secondaryTitle: 'secondary title',
            createdAt: skill.createdAt.getTime(),
            featureFlags: [],
            score: 1,
            accessToSkillTesting: {
                role: null,
                hasAccess: false,
            },
            useStateStorage: false,
            editorDescription: '',
            editorName: '',
            homepageBadgeTypes: [],
            tags: [],
        },
        res,
        t,
    );

    // - test with cache -

    incGetSkillCacheStat.resetHistory();

    res = await callApi('get', `/skills/${skill.id}`, { serviceTicket: t.context.serviceTicket });

    t.true(incGetSkillCacheStat.calledOnceWith('hit'));
    respondsWithResult(
        {
            id: skill.id,
            channel: 'aliceSkill',
            userId: skill.userId,
            salt: skill.salt,
            name: 'skill 1',
            useZora: true,
            onAir: false,
            botGuid: null,
            isRecommended: true,
            isVip: false,
            logo: {
                avatarId: '5182/429f04ace2d88763a581',
            },
            look: 'external',
            monitoringType: 'nonmonitored',
            openInNewTab: true,
            surfaces: [Surface.Auto, Surface.Navigator, Surface.Station],
            storeUrl: 'https://localhost.msup.yandex.ru:8083/skills/',
            voice: 'good_oksana',
            useNLU: false,
            category: 'business_finance',
            categoryLabel: 'Бизнес и финансы',
            ratingHistogram: [7, 2, 3, 5, 8],
            userReview: null,
            averageRating: 3.2,
            accountLinking: null,
            backendUrl: 'https://example.com/webhook',
            functionId: null,
            trusted: true,
            secondaryTitle: 'secondary title',
            createdAt: skill.createdAt.getTime(),
            featureFlags: [],
            score: 1,
            accessToSkillTesting: {
                role: null,
                hasAccess: false,
            },
            useStateStorage: false,
            editorDescription: '',
            editorName: '',
            homepageBadgeTypes: [],
            tags: [],
        },
        res,
        t,
    );
});

test('handles unknown skills', async t => {
    const res = await callApi('get', '/skills/00000000-0000-0000-0000-000000000000', {
        serviceTicket: t.context.serviceTicket,
    });

    respondsWithError(404, 'skill not found', res, t);
});

test('handles invalid skill id', async t => {
    const res = await callApi('get', '/skills/00000000-0000-0000-0000-000000000000xxx', {
        serviceTicket: t.context.serviceTicket,
    });

    respondsWithError(400, 'skillId must be a valid UUID', res, t);
});

test('ignores chats', async t => {
    const user = await createUser();
    const skill = await createSkill({
        channel: Channel.OrganizationChat,
    });

    await requestReview(skill, { user });
    await approveReview(skill, { user });
    await requestDeploy(skill, { user });
    await completeDeploy(skill);

    const res = await callApi('get', `/skills/${skill.id}`, {
        serviceTicket: t.context.serviceTicket,
    });

    respondsWithError(404, 'skill not found', res, t);
});

test('returns skills status (bulk mode with errors)', async t => {
    await createUser();
    const skill1 = await createSkill({
        backendSettings: {
            functionId: '123',
        },
    });

    const skill2 = await createSkill({
        backendSettings: {
            uri: 'http://example.com',
        },
    });
    const image1 = await createImage({
        type: ImageType.SkillSettings,
        skillId: skill1.id,
        url: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
        origUrl: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
    });
    const image2 = await createImage({
        type: ImageType.SkillSettings,
        skillId: skill1.id,
        url: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
        origUrl: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
    });
    await skill1.update({ logoId: image1.id, oauthAppId: (await createOAuthApp()).id });
    await skill2.update({ logoId: image2.id });

    const res = await callApi('post', '/skills/bulk/get', {
        serviceTicket: t.context.serviceTicket,
    }).send({
        skillIds: [
            skill1.id,
            '00000000-0000-0000-0000-000000000000xxx',
            '00000000-0000-0000-0000-000000000000',
            skill2.id,
        ],
    });

    respondsWithResultBulk(
        [
            {
                id: skill1.id,
                channel: 'aliceSkill',
                userId: skill1.userId,
                salt: skill1.salt,
                name: 'skill 3',
                useZora: true,
                onAir: false,
                botGuid: null,
                isRecommended: true,
                isVip: false,
                logo: {
                    avatarId: '5182/429f04ace2d88763a581',
                },
                look: 'external',
                monitoringType: 'nonmonitored',
                openInNewTab: true,
                surfaces: [Surface.Auto, Surface.Navigator, Surface.Station],
                storeUrl: 'https://localhost.msup.yandex.ru:8083/skills/',
                voice: 'good_oksana',
                useNLU: false,
                averageRating: 0,
                ratingHistogram: [0, 0, 0, 0, 0],
                userReview: null,
                accountLinking: { applicationName: 'social app name' },
                backendUrl: null,
                functionId: '123',
                trusted: false,
                secondaryTitle: '',
                createdAt: skill1.createdAt.getTime(),
                featureFlags: [],
                score: 1,
                accessToSkillTesting: {
                    role: null,
                    hasAccess: false,
                },
                useStateStorage: false,
                editorDescription: '',
                editorName: '',
                homepageBadgeTypes: [],
                tags: [],
            },
            {
                error: {
                    code: 400,
                    skillId: '00000000-0000-0000-0000-000000000000xxx',
                    message: 'skillId must be a valid UUID',
                },
            },
            {
                error: {
                    code: 404,
                    skillId: '00000000-0000-0000-0000-000000000000',
                    message: 'skill not found',
                },
            },
            {
                id: skill2.id,
                channel: 'aliceSkill',
                userId: skill2.userId,
                salt: skill2.salt,
                name: 'skill 4',
                useZora: true,
                onAir: false,
                botGuid: null,
                isRecommended: true,
                isVip: false,
                logo: {
                    avatarId: '5182/429f04ace2d88763a581',
                },
                look: 'external',
                monitoringType: 'nonmonitored',
                openInNewTab: true,
                surfaces: [Surface.Auto, Surface.Navigator, Surface.Station],
                storeUrl: 'https://localhost.msup.yandex.ru:8083/skills/',
                voice: 'good_oksana',
                useNLU: false,
                averageRating: 0,
                ratingHistogram: [0, 0, 0, 0, 0],
                userReview: null,
                accountLinking: null,
                backendUrl: 'http://example.com',
                functionId: null,
                trusted: false,
                secondaryTitle: '',
                createdAt: skill2.createdAt.getTime(),
                featureFlags: [],
                score: 1,
                accessToSkillTesting: {
                    role: null,
                    hasAccess: false,
                },
                useStateStorage: false,
                editorDescription: '',
                editorName: '',
                homepageBadgeTypes: [],
                tags: [],
            },
        ],
        res,
        t,
    );
});

test('returns "Too long skillIds list" (bulk mode)', async t => {
    sinon.stub(config.app.db, 'getSkillBulkMaxSkillsPerRequst').value(1);

    const res = await callApi('post', '/skills/bulk/get', {
        serviceTicket: t.context.serviceTicket,
    }).send({
        skillIds: [
            '00000000-0000-0000-0000-000000000000',
            '00000000-0000-0000-0000-000000000000',
            '00000000-0000-0000-0000-000000000000',
        ],
    });

    respondsWithError(403, 'Too long skillIds list', res, t);
});

test('handles invalid service ticket', async t => {
    const res = await callApi('get', '/skills/00000000-0000-0000-0000-000000000000', {
        serviceTicket: 'invalid_ticket',
    });

    respondsWithError(403, 'Forbidden (authentication error)', res, t);
});

test('handles expired service ticket', async t => {
    const res = await callApi('get', '/skills/00000000-0000-0000-0000-000000000000', {
        serviceTicket:
            '3:serv:CJoxEPTCzt8FIggI9Yp6EPWKeg:' +
            'MgVWLsYK1_NmpaSaMy4kdStabbZH_udIKG0sGzYwhGsg_' +
            'Go5FaeJGYsQygRDZCjyJGnHgfeqCIHbMzOUbvNT8aXeuKvCn1K' +
            'fAfhwJGZUx5ESgDgy5y9m4a99m00a5XK5WkKlIsETSj6qKBGMCIMxTVW3F048le87AEdmoCf3YA7',
    });

    respondsWithError(403, 'Forbidden (authentication error)', res, t);
});

test('handles not permitted service ticket', async t => {
    sinon.stub(config.tvmtool, 'permittedApps').value({});

    const res = await callApi('get', '/skills/00000000-0000-0000-0000-000000000000', {
        serviceTicket: t.context.serviceTicket,
    });

    respondsWithError(403, 'Forbidden (authentication error)', res, t);
});

test('handles request without service ticket', async t => {
    const res = await callApi('get', '/skills/00000000-0000-0000-0000-000000000000');

    respondsWithError(403, 'Forbidden (no credentials)', res, t);
});

test('test pumpkin invocation (end-to-end)', async t => {
    sinon.stub(apiPumpkin, 'getAPIPumpkin').value(async() => testPumpkin);

    const skillFromPumpkin = mapSkillInfoV1ToV2(
        testPumpkin['1eb04415-acb8-4649-9c3d-16f5c8748c0e'],
        {
            hasAccess: false,
            role: null,
        },
        null,
    );

    // --- test ---

    // disable cache
    sinon.stub(skillInfo, 'skillInfoCache').value(disabledCache);
    // emulate db error
    sinon.stub(skillRepository, 'getSkillWithLogoById').throws(new Error('fake db error'));
    // prepare spies
    const incGetSkillDbError = sinon.spy(unistat, 'incGetSkillDbError');
    const incGetSkillPumpkinStat = sinon.spy(unistat, 'incGetSkillPumpkinStat');

    // http call
    const res = await callApi('get', '/skills/1eb04415-acb8-4649-9c3d-16f5c8748c0e', {
        serviceTicket: t.context.serviceTicket,
    });

    // check response
    respondsWithResult(skillFromPumpkin, res, t);

    // check right workflow
    t.true(incGetSkillDbError.calledOnce);
    t.true(incGetSkillPumpkinStat.calledOnceWith('hit'));
});

test('test pumpkin error', async t => {
    // disable cache
    sinon.stub(skillInfo, 'skillInfoCache').value(disabledCache);
    // emulate db error
    sinon.stub(skillRepository, 'getSkillWithLogoById').throws(new Error('fake db error'));
    // prepare spies
    const incGetSkillDbError = sinon.spy(unistat, 'incGetSkillDbError');
    const incGetSkillPumpkinStat = sinon.spy(unistat, 'incGetSkillPumpkinStat');

    // http call
    const res = await callApi('get', '/skills/00000000-0000-0000-0000-000000000000', {
        serviceTicket: t.context.serviceTicket,
    });

    // check right workflow
    t.true(incGetSkillDbError.calledOnce);
    t.true(incGetSkillPumpkinStat.calledOnceWith('miss'));

    // check error
    respondsWithError(404, 'skill not found', res, t);
});

test('test promise error (db fails)', async t => {
    // disable cache
    sinon.stub(skillInfo, 'skillInfoCache').value(disabledCache);
    // prevent unhandled rejection
    sinon.stub(skillInfo, 'getSkillFromDB').resolves();
    // emulate db error
    sinon.stub(promiseTimeout, 'timeout').throws(new promiseTimeout.TimeoutError());
    // prepare spies
    const incGetSkillDbError = sinon.spy(unistat, 'incGetSkillDbError');
    const incGetSkillDbTimeout = sinon.spy(unistat, 'incGetSkillDbTimeout');

    // http call
    await callApi('get', '/skills/00000000-0000-0000-0000-000000000000', {
        serviceTicket: t.context.serviceTicket,
    });

    // check right workflow
    t.true(incGetSkillDbError.calledOnce);
    t.true(incGetSkillDbTimeout.calledOnce);

    // don't care about response
});

test('failed return smart home draft status for PA (no user ticket provided)', async t => {
    // - init db -

    await createUser();
    const skill = await createSkill({
        channel: Channel.SmartHome,
        isTrustedSmartHomeSkill: true,
        publishingSettings: {
            secondaryTitle: 'secondary title',
            category: CategoryType.smart_home,
        },
    });
    await skill.update({
        logoId: (await createImageForSkill(skill)).id,
        rating: [7, 2, 3, 5, 8],
        backendSettings: { uri: 'https://example.com/webhook' },
    });
    await skill.draft.update({
        logoId: (await createImageForSkill(skill, 'https://draftimage')).id,
        rating: [7, 2, 3, 5, 8],
        backendSettings: { uri: 'https://example.com/webhook' },
    });

    const res = await callApi('get', `/skills/${skill.id}-draft`, {
        serviceTicket: t.context.serviceTicket,
    }); // user ticket is absent

    respondsWithError(403, 'Forbidden (no credentials)', res, t);
});

test('failed return smart home draft status for PA (user not granted. userFeature canTestSmartHomeDrafts is absent)', async t => {
    // - init db -

    // create tester user
    await createUser({
        id: testUser.uid,
    });

    // create another user and his skill
    const user = await createUser();
    const skill = await createSkill({
        userId: user.id,
        channel: Channel.SmartHome,
        isTrustedSmartHomeSkill: true,
        publishingSettings: {
            secondaryTitle: 'secondary title',
            category: CategoryType.smart_home,
        },
    });
    await skill.update({
        logoId: (await createImageForSkill(skill)).id,
        rating: [7, 2, 3, 5, 8],
        backendSettings: { uri: 'https://example.com/webhook' },
    });
    await skill.draft.update({
        logoId: (await createImageForSkill(skill, 'https://draftimage')).id,
        rating: [7, 2, 3, 5, 8],
        backendSettings: { uri: 'https://example.com/webhook' },
    });

    const res = await callApi('get', `/skills/${skill.id}-draft`, {
        serviceTicket: t.context.serviceTicket,
        userTicket: t.context.userTicket,
    });

    respondsWithError(403, 'Forbidden (no credentials)', res, t);
});

test('returns smart home draft status for PA (all is fine)', async t => {
    // - init db -

    // create tester user
    await createUser({
        id: testUser.uid,
        featureFlags: {
            canTestSmartHomeDrafts: true,
        },
    });

    // create another user and his skill
    const user = await createUser();
    const skill = await createSkill({
        userId: user.id,
        channel: Channel.SmartHome,
        isTrustedSmartHomeSkill: true,
        publishingSettings: {
            secondaryTitle: 'secondary title',
            category: CategoryType.smart_home,
        },
    });
    await skill.update({
        logoId: (
            await createImageForSkill(
                skill,
                'https://draftimage/namespace/xxxx/yyyyyyyyyyyyy/11111',
            )
        ).id,
        rating: [7, 2, 3, 5, 8],
        backendSettings: { uri: 'https://example.com/webhook1' },
    });
    await skill.draft.update({
        logoId: (
            await createImageForSkill(
                skill,
                'https://draftimage/namespace/5182/429f04ace2d88763a581/11111',
            )
        ).id,
        rating: [7, 2, 3, 5, 8],
        backendSettings: { uri: 'https://example.com/webhook2' },
        oauthAppId: (await createOAuthApp()).id,
    });

    // - (cache is disabled for draft) -

    const incGetSkillCacheStat = sinon.spy(unistat, 'incGetSkillCacheStat');

    const res = await callApi('get', `/skills/${skill.id}-draft`, {
        serviceTicket: t.context.serviceTicket,
        userTicket: t.context.userTicket,
    });

    t.true(incGetSkillCacheStat.notCalled);
    respondsWithResult(
        {
            id: '',
            channel: 'smartHome',
            userId: skill.userId,
            salt: '',
            name: skill.draft.name,
            useZora: false,
            onAir: false,
            botGuid: null,
            isRecommended: false,
            isVip: false,
            logo: {
                avatarId: '5182/429f04ace2d88763a581',
            },
            look: '',
            monitoringType: '',
            openInNewTab: false,
            surfaces: [],
            storeUrl: '',
            voice: '',
            useNLU: false,
            category: 'smart_home',
            categoryLabel: 'Умный дом Яндекса',
            ratingHistogram: [7, 2, 3, 5, 8],
            averageRating: 3.2,
            userReview: null,
            accountLinking: { applicationName: 'social app name' },
            backendUrl: skill.draft.backendSettings.uri,
            functionId: null,
            trusted: true,
            secondaryTitle: 'secondary title',
            createdAt: skill.createdAt.getTime(),
            featureFlags: [],
            score: 0,
            accessToSkillTesting: {
                role: 'admin',
                hasAccess: true,
            },
            useStateStorage: false,
            editorDescription: '',
            editorName: '',
            homepageBadgeTypes: [],
            tags: [],
        },
        res,
        t,
    );
});

test('user can obtain own smart home drafts', async t => {
    const user = await createUser({
        id: testUser.uid,
    });

    const skill = await createSkill({
        userId: user.id,
        channel: Channel.SmartHome,
        isTrustedSmartHomeSkill: true,
        publishingSettings: {
            secondaryTitle: 'secondary title',
            category: CategoryType.smart_home,
        },
    });
    await skill.update({
        logoId: (
            await createImageForSkill(
                skill,
                'https://draftimage/namespace/xxxx/yyyyyyyyyyyyy/11111',
            )
        ).id,
        rating: [7, 2, 3, 5, 8],
        backendSettings: { uri: 'https://example.com/webhook1' },
    });
    await skill.draft.update({
        logoId: (
            await createImageForSkill(
                skill,
                'https://draftimage/namespace/5182/429f04ace2d88763a581/11111',
            )
        ).id,
        rating: [7, 2, 3, 5, 8],
        backendSettings: { uri: 'https://example.com/webhook2' },
        oauthAppId: (await createOAuthApp({ userId: testUser.uid })).id,
    });

    const res = await callApi('get', `/skills/${skill.id}-draft`, {
        serviceTicket: t.context.serviceTicket,
        userTicket: t.context.userTicket,
    });

    respondsWithResult(
        {
            id: '',
            salt: '',
            channel: 'smartHome',
            name: skill.draft.name,
            userId: skill.userId,
            useZora: false,
            onAir: false,
            botGuid: null,
            isRecommended: false,
            isVip: false,
            logo: {
                avatarId: '5182/429f04ace2d88763a581',
            },
            look: '',
            monitoringType: '',
            openInNewTab: false,
            surfaces: [],
            storeUrl: '',
            voice: '',
            useNLU: false,
            category: 'smart_home',
            categoryLabel: 'Умный дом Яндекса',
            ratingHistogram: [7, 2, 3, 5, 8],
            averageRating: 3.2,
            userReview: null,
            accountLinking: { applicationName: 'social app name' },
            backendUrl: skill.draft.backendSettings.uri,
            functionId: null,
            trusted: true,
            secondaryTitle: 'secondary title',
            createdAt: skill.createdAt.getTime(),
            featureFlags: [],
            score: 0,
            accessToSkillTesting: {
                role: 'owner',
                hasAccess: true,
            },
            useStateStorage: false,
            editorDescription: '',
            editorName: '',
            homepageBadgeTypes: [],
            tags: [],
        },
        res,
        t,
    );
});

test('admin with "canTestSmartHomeDrafts" flag can obtain smart home drafts', async t => {
    await createUser({
        id: testUser.uid,
        featureFlags: {
            canTestSmartHomeDrafts: true,
        },
    });

    const skillOwner = await createUser({
        id: testUser.uid + '1',
    });

    const skill = await createSkill({
        userId: skillOwner.id,
        channel: Channel.SmartHome,
        isTrustedSmartHomeSkill: true,
        publishingSettings: {
            secondaryTitle: 'secondary title',
            category: CategoryType.smart_home,
        },
    });
    await skill.update({
        logoId: (
            await createImageForSkill(
                skill,
                'https://draftimage/namespace/xxxx/yyyyyyyyyyyyy/11111',
            )
        ).id,
        rating: [7, 2, 3, 5, 8],
        backendSettings: { uri: 'https://example.com/webhook1' },
    });
    await skill.draft.update({
        logoId: (
            await createImageForSkill(
                skill,
                'https://draftimage/namespace/5182/429f04ace2d88763a581/11111',
            )
        ).id,
        rating: [7, 2, 3, 5, 8],
        backendSettings: { uri: 'https://example.com/webhook2' },
        oauthAppId: (await createOAuthApp({ userId: skillOwner.id })).id,
    });

    const res = await callApi('get', `/skills/${skill.id}-draft`, {
        serviceTicket: t.context.serviceTicket,
        userTicket: t.context.userTicket,
    });

    respondsWithResult(
        {
            id: '',
            channel: 'smartHome',
            salt: '',
            name: skill.draft.name,
            userId: skill.userId,
            useZora: false,
            onAir: false,
            botGuid: null,
            isRecommended: false,
            isVip: false,
            logo: {
                avatarId: '5182/429f04ace2d88763a581',
            },
            look: '',
            monitoringType: '',
            openInNewTab: false,
            surfaces: [],
            storeUrl: '',
            voice: '',
            useNLU: false,
            category: 'smart_home',
            categoryLabel: 'Умный дом Яндекса',
            averageRating: 3.2,
            userReview: null,
            ratingHistogram: [7, 2, 3, 5, 8],
            accountLinking: { applicationName: 'social app name' },
            backendUrl: skill.draft.backendSettings.uri,
            functionId: null,
            trusted: true,
            secondaryTitle: 'secondary title',
            createdAt: skill.createdAt.getTime(),
            featureFlags: [],
            score: 0,
            accessToSkillTesting: {
                role: 'admin',
                hasAccess: true,
            },
            useStateStorage: false,
            editorDescription: '',
            editorName: '',
            homepageBadgeTypes: [],
            tags: [],
        },
        res,
        t,
    );
});

test('getSkill works with unknown user id', async t => {
    const skillOwner = await createUser({
        id: testUser.uid + '1',
    });

    const skill = await createSkill({
        userId: skillOwner.id,
        channel: Channel.SmartHome,
        isTrustedSmartHomeSkill: true,
        onAir: true,
        publishingSettings: {
            secondaryTitle: 'secondary title',
            category: CategoryType.smart_home,
        },
    });
    const image = await createImage({
        type: ImageType.SkillSettings,
        skillId: skill.id,
        url: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
        origUrl: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
    });
    await skill.update({
        logoId: image.id,
        rating: [7, 2, 3, 5, 8],
        backendSettings: { uri: 'https://example.com/webhook' },
    });

    const res = await callApi('get', `/skills/${skill.id}`, {
        serviceTicket: t.context.serviceTicket,
        userTicket: t.context.userTicket,
    });

    respondsWithResult(
        {
            id: skill.id,
            channel: 'smartHome',
            userId: skill.userId,
            salt: skill.salt,
            name: 'skill 10',
            useZora: true,
            onAir: true,
            botGuid: null,
            isRecommended: true,
            isVip: false,
            logo: {
                avatarId: '5182/429f04ace2d88763a581',
            },
            look: 'external',
            monitoringType: 'nonmonitored',
            openInNewTab: true,
            surfaces: [ImplicitSurface.Mobile, Surface.Navigator, Surface.Station],
            storeUrl: 'https://localhost.msup.yandex.ru:8083/skills/',
            voice: 'good_oksana',
            useNLU: false,
            category: 'smart_home',
            categoryLabel: 'Умный дом Яндекса',
            averageRating: 3.2,
            ratingHistogram: [7, 2, 3, 5, 8],
            userReview: null,
            accountLinking: null,
            backendUrl: 'https://example.com/webhook',
            functionId: null,
            trusted: true,
            secondaryTitle: 'secondary title',
            createdAt: skill.createdAt.getTime(),
            featureFlags: [],
            score: 2,
            accessToSkillTesting: {
                role: null,
                hasAccess: false,
            },
            useStateStorage: false,
            editorDescription: '',
            editorName: '',
            homepageBadgeTypes: [],
            tags: [],
        },
        res,
        t,
    );
});

test('getSkill returns user review if it exists and user ticket is provided', async t => {
    const user = await createUser({
        id: testUser.uid,
    });
    const skill = await createSkill({
        isTrustedSmartHomeSkill: true,
        userId: user.id,
        onAir: true,
        publishingSettings: {
            secondaryTitle: 'secondary title',
            category: CategoryType.business_finance,
        },
    });
    const image = await createImage({
        type: ImageType.SkillSettings,
        skillId: skill.id,
        url: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
        origUrl: 'https://avatars.mdst.yandex.net/get-dialogs/5182/429f04ace2d88763a581/orig',
    });
    await skill.update({
        logoId: image.id,
        rating: [7, 2, 3, 5, 8],
        backendSettings: { uri: 'https://example.com/webhook' },
    });

    // - test without cache (obtained from db) -

    await UserReview.create({
        skillId: skill.id,
        userId: user.id,
        reviewText: 'классный скилл',
        quickAnswers: [],
        rating: 5,
    });

    const res = await callApi('get', `/skills/${skill.id}`, {
        serviceTicket: t.context.serviceTicket,
        userTicket: t.context.userTicket,
    });

    respondsWithResult(
        {
            id: skill.id,
            channel: 'aliceSkill',
            userId: skill.userId,
            salt: skill.salt,
            name: 'skill 11',
            useZora: true,
            onAir: true,
            botGuid: null,
            isRecommended: true,
            isVip: false,
            logo: {
                avatarId: '5182/429f04ace2d88763a581',
            },
            look: 'external',
            monitoringType: 'nonmonitored',
            openInNewTab: true,
            surfaces: [Surface.Auto, Surface.Navigator, Surface.Station],
            storeUrl: 'https://localhost.msup.yandex.ru:8083/skills/',
            voice: 'good_oksana',
            useNLU: false,
            category: 'business_finance',
            categoryLabel: 'Бизнес и финансы',
            ratingHistogram: [7, 2, 3, 5, 8],
            averageRating: 3.2,
            userReview: {
                rating: 5,
                reviewText: 'классный скилл',
                quickAnswers: [],
            },
            accountLinking: null,
            backendUrl: 'https://example.com/webhook',
            functionId: null,
            trusted: true,
            secondaryTitle: 'secondary title',
            createdAt: skill.createdAt.getTime(),
            featureFlags: [],
            score: 1,
            accessToSkillTesting: {
                role: 'owner',
                hasAccess: true,
            },
            useStateStorage: false,
            editorDescription: '',
            editorName: '',
            homepageBadgeTypes: [],
            tags: [],
        },
        res,
        t,
    );
});
