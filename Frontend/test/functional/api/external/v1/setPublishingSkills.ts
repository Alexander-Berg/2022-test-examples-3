/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import { omit } from 'lodash';
import * as sinon from 'sinon';
import { Channel } from '../../../../../db/tables/settings';
import { createSkill, createUser, wipeDatabase } from '../../../_helpers';
import { callApi, respondsWithError, respondsWithResult } from './_helpers';
import { approveReview, requestReview, requestDeploy, completeDeploy } from '../../../../../services/skill-lifecycle';

const test = anyTest as TestInterface<{ clock: sinon.SinonFakeTimers }>;

test.beforeEach(async t => {
    t.context.clock = sinon.useFakeTimers(Date.now());

    await wipeDatabase();
});

test.afterEach(t => {
    t.context.clock.restore();
});

test.skip('actually publish skills', async t => {
    const user = await createUser();
    const skill = await createSkill();

    await requestReview(skill, { user });
    await approveReview(skill, { user });
    await requestDeploy(skill, { user });

    const pubRes = await callApi('setPublishedSkills', [[skill.id]]);

    respondsWithResult('ok', pubRes, t);

    const getRes = await callApi('getSkill', [skill.id]);

    respondsWithResult(
        {
            id: skill.id,
            name: 'skill 1',
            useZora: true,
            exposeInternalFlags: false,
            salt: skill.salt,
            backendSettings: {},
            onAir: true,
        },
        getRes,
        t,
    );

    t.truthy(getRes.body.result.id);
    t.truthy(getRes.body.result.salt);
});

test.skip('publish only skills that awaits publishing', async t => {
    const user = await createUser();
    const skillOnAir = await createSkill();
    const skillDeployRequested = await createSkill();

    await requestReview(skillOnAir, { user });
    await approveReview(skillOnAir, { user });
    await requestDeploy(skillOnAir, { user });
    await completeDeploy(skillOnAir);

    await requestReview(skillDeployRequested, { user });
    await approveReview(skillDeployRequested, { user });
    await requestDeploy(skillDeployRequested, { user });

    const res = await callApi('setPublishedSkills', [[skillOnAir.id, skillDeployRequested.id]]);

    respondsWithResult('ok', res, t);

    await skillOnAir.reload();
    await skillDeployRequested.reload();

    const skillOnAirOperations = (await skillOnAir.getOperations())
        .map(x => x.get({ plain: true }))
        .map(x => omit(x, 'id', 'createdAt', 'updatedAt'));

    const skillDeployRequestedOperations = (await skillDeployRequested.getOperations())
        .map(x => x.get({ plain: true }))
        .map(x => omit(x, 'id', 'createdAt', 'updatedAt'));

    t.deepEqual(skillOnAirOperations, [
        {
            type: 'skillCreated',
            comment: '',
            itemId: skillOnAir.id,
        },
        {
            type: 'reviewRequested',
            comment: '',
            itemId: skillOnAir.id,
        },
        {
            type: 'reviewApproved',
            comment: '',
            itemId: skillOnAir.id,
        },
        {
            type: 'deployRequested',
            comment: '',
            itemId: skillOnAir.id,
        },
        {
            type: 'deployCompleted',
            comment: '',
            itemId: skillOnAir.id,
        },
    ]);

    t.deepEqual(skillDeployRequestedOperations, [
        {
            type: 'skillCreated',
            comment: '',
            itemId: skillDeployRequested.id,
        },
        {
            type: 'reviewRequested',
            comment: '',
            itemId: skillDeployRequested.id,
        },
        {
            type: 'reviewApproved',
            comment: '',
            itemId: skillDeployRequested.id,
        },
        {
            type: 'deployRequested',
            comment: '',
            itemId: skillDeployRequested.id,
        },
        {
            type: 'deployCompleted',
            comment: '',
            itemId: skillDeployRequested.id,
        },
    ]);
});

test.skip('with empty db shourd return []', async t => {
    const res = await callApi('getPublishingSkills');

    respondsWithResult([], res, t);
});

test.skip('with publishing skill should return it', async t => {
    const user = await createUser();
    await createSkill();
    const skill = await createSkill();

    await requestReview(skill, { user });
    await approveReview(skill, { user });
    await requestDeploy(skill, { user });

    const res = await callApi('getPublishingSkills');

    respondsWithResult(
        [
            {
                id: skill.id,
                activationPhrases: [],
            },
        ],
        res,
        t,
    );
});

test.skip('with published skill should return 403', async t => {
    const user = await createUser();
    await createSkill();
    const skill = await createSkill();

    await requestReview(skill, { user });
    await approveReview(skill, { user });
    await requestDeploy(skill, { user });

    await callApi('getPublishingSkills');

    await completeDeploy(skill);

    const res = await callApi('getPublishingSkills');

    respondsWithError(
        {
            code: 3,
            message: 'Not Modified',
        },
        res,
        t,
    );
});

test.skip('with draft in review should return 403', async t => {
    const user = await createUser();
    await createSkill();
    const skill = await createSkill();

    await requestReview(skill, { user });
    await approveReview(skill, { user });
    await requestDeploy(skill, { user });

    await callApi('getPublishingSkills');

    await completeDeploy(skill);
    await requestReview(skill, { user });

    const res = await callApi('getPublishingSkills');

    respondsWithError(
        {
            code: 3,
            message: 'Not Modified',
        },
        res,
        t,
    );
});

test.skip('with publishing draft should return it', async t => {
    const user = await createUser();
    await createSkill();
    const skill = await createSkill();

    await requestReview(skill, { user });
    await approveReview(skill, { user });
    await requestDeploy(skill, { user });
    await completeDeploy(skill);
    t.context.clock.tick(1);
    await requestDeploy(skill, { user });

    const res = await callApi('getPublishingSkills');

    respondsWithResult(
        [
            {
                id: skill.id,
                activationPhrases: [],
            },
        ],
        res,
        t,
    );
});

test.skip('ignores chats', async t => {
    const user = await createUser();
    const skill = await createSkill({ channel: Channel.OrganizationChat });

    await requestReview(skill, { user });
    await approveReview(skill, { user });
    await requestDeploy(skill, { user });

    await callApi('setPublishedSkills', [[skill.id]]);

    await skill.reload();

    t.is(skill.onAir, false);
});
