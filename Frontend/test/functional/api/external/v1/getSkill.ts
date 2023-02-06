/* eslint-disable */
import test from 'ava';
import * as sinon from 'sinon';
import * as promiseTimeout from 'promise-timeout';
import { Channel, SkillAccess } from '../../../../../db/tables/settings';
import { createImage, createSkill, createUser, wipeDatabase } from '../../../_helpers';
import { callApi, respondsWithError, respondsWithResult } from './_helpers';
import { ImageType } from '../../../../../db/tables/image';
import { Surface } from '../../../../../services/surface';
import * as skillInfo from '../../../../../services/skillInfo';
import * as unistat from '../../../../../services/unistat';
import * as skillRepository from '../../../../../db/repositories/skill';
import { testPumpkin } from '../../../../../fixtures/pumpkin';
import * as apiPumpkin from '../../../../../services/api-pumpkin';
import {
    approveReview,
    requestReview,
    requestDeploy,
    completeDeploy,
} from '../../../../../services/skill-lifecycle';
import { CategoryType } from '../../../../../fixtures/categories';
import { getPoolSet } from '../../../../../lib/pgPool';

test.before(async() => {
    // Awaiting for pg service starting
    await getPoolSet();
});

test.beforeEach(wipeDatabase);

test('returns skill status (without/with cache)', async t => {
    // - init db -
    await createUser();
    const skill = await createSkill({
        publishingSettings: {
            category: CategoryType.business_finance,
        },
        backendSettings: {
            functionId: '123',
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
    });

    // - test without cache (obtained from db) -

    const incGetSkillCacheStat = sinon.spy(unistat, 'incGetSkillCacheStat');

    let res = await callApi('getSkill', [skill.id]);

    t.true(incGetSkillCacheStat.calledOnceWith('miss'));
    respondsWithResult(
        {
            id: skill.id,
            channel: 'aliceSkill',
            userId: skill.userId,
            salt: skill.salt,
            name: 'skill 1',
            useZora: true,
            exposeInternalFlags: false,
            backendSettings: {
                functionId: '123',
            },
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
            platforms: [Surface.Auto, Surface.Navigator, Surface.Station],
            surfaces: [Surface.Auto, Surface.Navigator, Surface.Station],
            storeUrl: 'https://localhost.msup.yandex.ru:8083/skills/',
            voice: 'good_oksana',
            useNLU: false,
            category: 'business_finance',
            categoryLabel: 'Бизнес и финансы',
            ratingHistogram: [7, 2, 3, 5, 8],
            averageRating: 3.2,
            accountLinking: null,
            trusted: false,
            secondaryTitle: '',
            createdAt: skill.createdAt.getTime(),
            featureFlags: [],
            score: 1,
            skillAccess: SkillAccess.Public,
            useStateStorage: false,
            editorDescription: '',
            editorName: '',
            homepageBadgeTypes: [],
            tags: [],
        },
        res,
        t,
    );

    t.truthy(res.body.result.id);
    t.truthy(res.body.result.salt);

    // - test with cache -

    incGetSkillCacheStat.resetHistory();

    res = await callApi('getSkill', [skill.id]);

    t.true(incGetSkillCacheStat.calledOnceWith('hit'));

    respondsWithResult(
        {
            id: skill.id,
            channel: 'aliceSkill',
            userId: skill.userId,
            salt: skill.salt,
            name: 'skill 1',
            useZora: true,
            exposeInternalFlags: false,
            backendSettings: {
                functionId: '123',
            },
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
            platforms: [Surface.Auto, Surface.Navigator, Surface.Station],
            surfaces: [Surface.Auto, Surface.Navigator, Surface.Station],
            storeUrl: 'https://localhost.msup.yandex.ru:8083/skills/',
            voice: 'good_oksana',
            useNLU: false,
            category: 'business_finance',
            categoryLabel: 'Бизнес и финансы',
            ratingHistogram: [7, 2, 3, 5, 8],
            averageRating: 3.2,
            accountLinking: null,
            trusted: false,
            secondaryTitle: '',
            createdAt: skill.createdAt.getTime(),
            featureFlags: [],
            score: 1,
            skillAccess: SkillAccess.Public,
            useStateStorage: false,
            editorDescription: '',
            editorName: '',
            homepageBadgeTypes: [],
            tags: [],
        },
        res,
        t,
    );

    t.truthy(res.body.result.id);
    t.truthy(res.body.result.salt);

    incGetSkillCacheStat.restore();
});

test('handles unknown skills', async t => {
    const res = await callApi('getSkill', ['70542675-b93a-4b9d-9cdd-5565c98d6cc7']);

    respondsWithError(
        {
            code: 1,
            message: 'Skill not found',
        },
        res,
        t,
    );
});

test('handles invalid skill id', async t => {
    const res = await callApi('getSkill', ['00000000-0000-0000-0000-000000000000xxx']);

    respondsWithError(
        {
            code: 5,
            message: 'Invalid skill ID',
        },
        res,
        t,
    );
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

    const res = await callApi('getSkill', [skill.id]);

    respondsWithError(
        {
            code: 1,
            message: 'Skill not found',
        },
        res,
        t,
    );
});

test('test pumpkin invocation (end-to-end)', async t => {
    sinon.stub(apiPumpkin, 'getAPIPumpkin').value(async() => testPumpkin);

    // --- test ---

    // disable cache
    sinon.stub(skillInfo, 'skillInfoCache').value({
        get: () => null,
        set: () => null,
    });
    // emulate db error
    sinon.stub(skillRepository, 'getSkillWithLogoById').throws(new Error('fake db error'));
    // prepare spies
    const incGetSkillDbError = sinon.spy(unistat, 'incGetSkillDbError');
    const incGetSkillPumpkinStat = sinon.spy(unistat, 'incGetSkillPumpkinStat');

    // http call
    const res = await callApi('getSkill', ['1eb04415-acb8-4649-9c3d-16f5c8748c0e']);

    // check response
    respondsWithResult(testPumpkin['1eb04415-acb8-4649-9c3d-16f5c8748c0e'], res, t);

    // check right workflow
    t.true(incGetSkillDbError.calledOnce);
    t.true(incGetSkillPumpkinStat.calledOnceWith('hit'));

    // finish
    incGetSkillDbError.restore();
    incGetSkillPumpkinStat.restore();
    sinon.restore();
});

test('test pumpkin error', async t => {
    // disable cache
    sinon.stub(skillInfo, 'skillInfoCache').value({
        get: () => null,
        set: () => null,
    });
    // emulate db error
    sinon.stub(skillRepository, 'getSkillWithLogoById').throws(new Error('fake db error'));
    // prepare spies
    const incGetSkillDbError = sinon.spy(unistat, 'incGetSkillDbError');
    const incGetSkillPumpkinStat = sinon.spy(unistat, 'incGetSkillPumpkinStat');

    // http call
    const res = await callApi('getSkill', ['00000000-0000-0000-0000-000000000000']);

    // check right workflow
    t.true(incGetSkillDbError.calledOnce);
    t.true(incGetSkillPumpkinStat.calledOnceWith('miss'));

    // check error
    respondsWithError(
        {
            code: 1,
            message: 'Skill not found',
        },
        res,
        t,
    );

    // finish
    incGetSkillDbError.restore();
    incGetSkillPumpkinStat.restore();
    sinon.restore();
});

test('test promise error (db fails)', async t => {
    // disable cache
    sinon.stub(skillInfo, 'skillInfoCache').value({
        get: () => null,
        set: () => null,
    });
    // emulate db error
    sinon.stub(promiseTimeout, 'timeout').throws(new promiseTimeout.TimeoutError());
    // prepare spies
    const incGetSkillDbError = sinon.spy(unistat, 'incGetSkillDbError');
    const incGetSkillDbTimeout = sinon.spy(unistat, 'incGetSkillDbTimeout');

    // http call
    await callApi('getSkill', ['00000000-0000-0000-0000-000000000000']);

    // check right workflow
    t.true(incGetSkillDbError.calledOnce);
    t.true(incGetSkillDbTimeout.calledOnce);

    // don't care about response

    // finish
    incGetSkillDbError.restore();
    incGetSkillDbTimeout.restore();
    sinon.restore();
});
