/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import * as sinon from 'sinon';
import * as yt from '../../../../services/yt';
import { SkillInstance } from '../../../../db/tables/skill';
import { UserInstance } from '../../../../db/tables/user';
import { createSkill, createUser, wipeDatabase } from '../../_helpers';
import { callApi } from './_helpers';
import { requestReview } from '../../../../services/skill-lifecycle';

const test = anyTest as TestInterface<{
    skill: SkillInstance;
    user: UserInstance;
    stub: sinon.SinonStub;
}>;

test.beforeEach(async t => {
    await wipeDatabase();

    t.context.user = await createUser();
    t.context.skill = await createSkill();
    t.context.stub = sinon.stub(yt, 'writeYtTableWithTTL');
});

test.afterEach(async t => {
    t.context.stub.restore();
});

test('do not upload skill twice', async t => {
    await requestReview(t.context.skill, { user: t.context.user });
    await callApi('/skills/review-requests/upload-to-yt');
    await callApi('/skills/review-requests/upload-to-yt');

    t.true(t.context.stub.calledOnce);
});

test('upload skill after new review request', async t => {
    await requestReview(t.context.skill, { user: t.context.user });
    await callApi('/skills/review-requests/upload-to-yt');
    await requestReview(t.context.skill, { user: t.context.user });
    await callApi('/skills/review-requests/upload-to-yt');

    t.true(t.context.stub.calledTwice);
});
