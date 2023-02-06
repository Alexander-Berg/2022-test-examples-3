/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import * as sinon from 'sinon';
import { createUser, createSkill, wipeDatabase } from '../../_helpers';
import * as lifecycle from '../../../../services/skill-lifecycle';
import { callApi } from '../../api/dev-console/_helpers';
import { getUserTicket, testUser } from '../../api/_helpers';

interface TestInterfaceParams {
    userTicket: string;
}

const test = anyTest as TestInterface<TestInterfaceParams>;

test.beforeEach(async t => {
    t.context.userTicket = await getUserTicket(testUser.oauthToken);
    await wipeDatabase();
    sinon.restore();
});

test('rejectDeploy: reject to reviewApproved status when failed publication', async t => {
    sinon.replace(lifecycle, 'requestDeploy', sinon.fake.rejects(new Error()));

    const user = await createUser({ id: testUser.uid });
    const skill = await createSkill({ userId: user.id });
    await lifecycle.approveReview(skill, { user });

    const res = await callApi('post', `/skills/${skill.id}/release`, t.context);
    await skill.draft.reload();

    t.false(res.ok);
    t.is(skill.draft.status, 'reviewApproved');
});

test('rejectDeploy: reject to inDevelopment status', async t => {
    const user = await createUser({ id: testUser.uid });
    const skill = await createSkill({ userId: user.id });

    await lifecycle.approveReview(skill, { user });

    await skill.reload();
    await skill.draft.reload();

    t.is(skill.draft.status, 'reviewApproved');

    await lifecycle.rejectDeploy(skill, 'test');

    await skill.reload();
    await skill.draft.reload();

    t.is(skill.draft.status, 'inDevelopment');
});
