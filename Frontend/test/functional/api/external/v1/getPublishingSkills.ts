/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import * as sinon from 'sinon';
import { Channel } from '../../../../../db/tables/settings';
import { createSkill, createUser, wipeDatabase } from '../../../_helpers';
import { callApi, respondsWithResult } from './_helpers';
import { approveReview, requestReview, requestDeploy, completeDeploy } from '../../../../../services/skill-lifecycle';

const test = anyTest as TestInterface<{ clock: sinon.SinonFakeTimers }>;

test.beforeEach(async t => {
    t.context.clock = sinon.useFakeTimers(Date.now());

    await wipeDatabase();
});

test.afterEach(t => {
    t.context.clock.restore();
});

test.skip('returns only publishing and active skills', async t => {
    const user = await createUser();
    await createSkill();
    const skillOnAir = await createSkill({
        name: 'привет мир',
        activationPhrases: ['фраза раз', 'фраза два'],
    });
    const skillDeployRequested = await createSkill({
        name: 'привет мир два',
    });

    await skillOnAir.draft.update({
        activationPhrases: ['фраза раз два', 'фраза два два'],
    });
    await requestReview(skillOnAir, { user });
    await approveReview(skillOnAir, { user });
    await requestDeploy(skillOnAir, { user });
    await completeDeploy(skillOnAir);
    await skillOnAir.draft.update({
        activationPhrases: ['фраза раз три', 'фраза два три'],
    });

    t.context.clock.tick(1);

    await skillDeployRequested.draft.update({
        activationPhrases: ['фраза раз четыре', 'фраза два четыре'],
    });
    await requestReview(skillDeployRequested, { user });
    await approveReview(skillDeployRequested, { user });
    await requestDeploy(skillDeployRequested, { user });

    const res = await callApi('getPublishingSkills');

    respondsWithResult(
        [
            {
                id: skillOnAir.id,
                activationPhrases: ['фраза раз два', 'фраза два два'],
            },
            {
                id: skillDeployRequested.id,
                activationPhrases: ['фраза раз четыре', 'фраза два четыре'],
            },
        ],
        res,
        t,
    );
});

test.skip('ignores chats', async t => {
    const user = await createUser();
    const skill = await createSkill({
        channel: Channel.OrganizationChat,
    });

    await requestReview(skill, { user });
    await approveReview(skill, { user });
    await requestDeploy(skill, { user });

    const res = await callApi('getPublishingSkills');

    respondsWithResult([], res, t);
});
