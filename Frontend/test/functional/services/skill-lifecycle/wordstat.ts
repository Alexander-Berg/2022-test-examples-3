/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import { createSandbox, fake } from 'sinon';
import { ActivationPhraseCommonness } from '../../../../db/tables/settings';
import config from '../../../../services/config';
import * as nlu from '../../../../services/nlu';
import * as cycle from '../../../../services/skill-lifecycle';
import * as wordstat from '../../../../services/wordstat';
import { callApi } from '../../api/dev-console/_helpers';
import { getUserTicket, testUser } from '../../api/_helpers';
import { createSkill, createUser, wipeDatabase } from '../../_helpers';
import { UserInstance } from '../../../../db/tables/user';

const test = anyTest as TestInterface<{
    userTicket: string;
    user: UserInstance;
}>;

test.before(async t => {
    const userTicket = await getUserTicket(testUser.oauthToken);

    Object.assign(t.context, { userTicket });
});

test.beforeEach(async t => {
    await wipeDatabase();
    t.context.user = await createUser({ id: testUser.uid });
});

test('activationPhrasesForceApproval: empty after skill created', async t => {
    const skill = await createSkill({ userId: testUser.uid });

    t.is(skill.draft.activationPhrasesForceApproval, null);
});

test('activationPhrasesForceApproval: defaults after draft patched', async t => {
    const skill = await createSkill({ userId: testUser.uid });

    await callApi('patch', `/skills/${skill.id}/draft`, t.context);
    await skill.reload();

    t.deepEqual(skill.draft.activationPhrasesForceApproval, []);
});

test('activationPhrasesForceApproval: not changed if activation phrase not changed', async t => {
    const skill = await createSkill({ userId: testUser.uid });
    await skill.draft.update({
        activationPhrases: ['a', 'b', 'c'],
        activationPhrasesForceApproval: [true, true, true],
    });

    await callApi('patch', `/skills/${skill.id}/draft`, t.context);
    await skill.reload();

    t.deepEqual(skill.draft.activationPhrasesForceApproval, [true, true, true]);
});

test('activationPhrasesForceApproval: changed if activation phrase changed', async t => {
    const sinon = createSandbox();
    sinon.replace(nlu, 'inflect', fake.resolves([]));

    const skill = await createSkill({ userId: testUser.uid });
    await skill.draft.update({
        activationPhrases: ['a', 'b', 'c'],
        activationPhrasesForceApproval: [true, true, true],
    });

    await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        activationPhrases: ['a', 'b1', 'c1'],
    });
    await skill.reload();

    t.deepEqual(skill.draft.activationPhrasesForceApproval, [true, false, false]);

    sinon.restore();
});

test('activationPhrasesForceApproval: copied to skill on deployCompleted', async t => {
    const skill = await createSkill({ userId: testUser.uid });
    await skill.draft.update({
        activationPhrasesForceApproval: [true, true, true],
    });

    await cycle.completeDeploy(skill);

    t.deepEqual(skill.activationPhrasesForceApproval, [true, true, true]);
});

test('activationPhrasesCommonness: empty after skill created', async t => {
    const skill = await createSkill({ userId: testUser.uid });

    t.is(skill.draft.activationPhrasesCommonness, null);
});

test('activationPhrasesCommonness: set on reviewRequested if enabled', async t => {
    const sinon = createSandbox();
    const getActivationPhrasesCommonness = fake.resolves(['green', 'green', 'green']);
    sinon.replace(wordstat, 'getActivationPhrasesCommonness', getActivationPhrasesCommonness);
    sinon.replace(config.app, 'checkActivationPhrasesCommonness', true);

    const skill = await createSkill({ userId: testUser.uid });
    await cycle.requestReview(skill, { user: t.context.user });

    t.true(getActivationPhrasesCommonness.calledOnce);
    t.deepEqual(skill.draft.activationPhrasesCommonness, ['green', 'green', 'green'] as ActivationPhraseCommonness[]);

    sinon.restore();
});

test('activationPhrasesCommonness: not set on reviewRequested if disabled', async t => {
    const sinon = createSandbox();
    const getActivationPhrasesCommonness = fake.resolves(['green', 'green', 'green']);
    sinon.replace(wordstat, 'getActivationPhrasesCommonness', getActivationPhrasesCommonness);
    sinon.replace(config.app, 'checkActivationPhrasesCommonness', false);

    const skill = await createSkill({ userId: testUser.uid });
    await cycle.requestReview(skill, { user: t.context.user });

    t.true(getActivationPhrasesCommonness.notCalled);
    t.deepEqual(skill.draft.activationPhrasesCommonness, null);

    sinon.restore();
});
