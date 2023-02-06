/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import * as sinon from 'sinon';
import { createSkill, createUser, wipeDatabase } from '../../_helpers';
import { Channel } from '../../../../db/tables/settings';
import { SkillInstance, SkillAttributes } from '../../../../db/tables/skill';
import { completeDeploy } from '../../../../services/skill-lifecycle';
import * as SkillEntities from '../../../../entities/skill';

interface TestContext {
    stubs: sinon.SinonStub[];
    skill: SkillInstance;
}

const test = anyTest as TestInterface<TestContext>;

test.beforeEach(async t => {
    await wipeDatabase();
    await createUser();
    t.context.skill = await createSkill();
});

test.afterEach.always(async t => {
    sinon.restore();
});

test('score defaults to 1 for skills', async t => {
    const skill = t.context.skill;
    skill.channel = Channel.AliceSkill;
    t.true(skill.score === null);
    const deployScore = SkillEntities.getSkillScore(skill);
    t.deepEqual(deployScore, 1);
});

test('score defaults to 0 for organization chats', async t => {
    const skill = t.context.skill;
    skill.channel = Channel.OrganizationChat;
    t.true(skill.score === null);
    const deployScore = SkillEntities.getSkillScore(skill);
    t.deepEqual(deployScore, 0);
});

test('min score for skills is 1', async t => {
    const skill = t.context.skill;
    skill.channel = Channel.AliceSkill;
    skill.score = 0.5;
    const deployScore = SkillEntities.getSkillScore(skill);
    t.deepEqual(deployScore, 1);
});

test('min score for organization chats is 0', async t => {
    const skill = t.context.skill;
    skill.channel = Channel.OrganizationChat;

    skill.score = -0.5;
    const deployScore = SkillEntities.getSkillScore(skill);
    t.deepEqual(deployScore, 0);
});

test('skill is updated with deployScore on completeDeploy()', async t => {
    const skill = t.context.skill;
    const getScore = sinon.stub(SkillEntities, 'getSkillScore').returns(1000);
    const updateSkill = sinon.stub(skill, 'update').resolves();

    await completeDeploy(skill);

    t.true(getScore.calledOnce);
    t.true(updateSkill.calledOnce);
    t.deepEqual((updateSkill.firstCall.args[0] as SkillAttributes).score, 1000);
});
