/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import * as sinon from 'sinon';
import * as unistat from '../../../../services/unistat';
import * as recommendations from '../../../../services/automaticSkillActions/recommendations';
import { createSkill, createUser, wipeDatabase } from '../../_helpers';

interface TestContext {
    incCounter: sinon.SinonStub;
}

const test = anyTest as TestInterface<TestContext>;

test.before(t => {
    t.context.incCounter = sinon.stub(unistat, 'incCounter');
});

test.beforeEach(async t => {
    await wipeDatabase();
    await createUser();
    t.context.incCounter.resetHistory();
});

test.after(t => {
    t.context.incCounter.restore();
});

test('setAutomaticIsRecommended() sets flag to true from null', async t => {
    const skill = await createSkill({ onAir: true });
    t.is(skill.automaticIsRecommended, null);
    await recommendations.setAutomaticIsRecommended(skill.id, true);
    await skill.reload();
    t.true(skill.automaticIsRecommended);
    t.is(t.context.incCounter.callCount, 2);
    t.true(t.context.incCounter.firstCall.calledWith('automatic_skill_isrecommended_true_summ'));
    t.true(
        t.context.incCounter.secondCall.calledWith('automatic_skill_isrecommended_changed_summ', 1),
    );
});

test('setAutomaticIsRecommended() sets flag to false from null', async t => {
    const skill = await createSkill({ onAir: true });
    t.is(skill.automaticIsRecommended, null);
    await recommendations.setAutomaticIsRecommended(skill.id, false);
    await skill.reload();
    t.false(skill.automaticIsRecommended);
    t.is(t.context.incCounter.callCount, 2);
    t.true(t.context.incCounter.firstCall.calledWith('automatic_skill_isrecommended_false_summ'));
    t.true(
        t.context.incCounter.secondCall.calledWith('automatic_skill_isrecommended_changed_summ', 1),
    );
});

test("setAutomaticIsRecommended() doesn't rewrite flag with same value (true)", async t => {
    const skill = await createSkill({ onAir: true, automaticIsRecommended: true });
    t.true(skill.automaticIsRecommended);
    await recommendations.setAutomaticIsRecommended(skill.id, true);
    await skill.reload();
    t.true(skill.automaticIsRecommended);
    t.is(t.context.incCounter.callCount, 2);
    t.true(t.context.incCounter.firstCall.calledWith('automatic_skill_isrecommended_true_summ'));
    t.true(
        t.context.incCounter.secondCall.calledWith('automatic_skill_isrecommended_changed_summ', 0),
    );
});

test("setAutomaticIsRecommended() doesn't rewrite flag with same value (false)", async t => {
    const skill = await createSkill({ onAir: true, automaticIsRecommended: false });
    t.false(skill.automaticIsRecommended);
    await recommendations.setAutomaticIsRecommended(skill.id, false);
    await skill.reload();
    t.false(skill.automaticIsRecommended);
    t.is(t.context.incCounter.callCount, 2);
    t.true(t.context.incCounter.firstCall.calledWith('automatic_skill_isrecommended_false_summ'));
    t.true(
        t.context.incCounter.secondCall.calledWith('automatic_skill_isrecommended_changed_summ', 0),
    );
});

test('setAutomaticIsRecommended() rewrites flag from true to false', async t => {
    const skill = await createSkill({ onAir: true, automaticIsRecommended: false });
    t.false(skill.automaticIsRecommended);
    await recommendations.setAutomaticIsRecommended(skill.id, true);
    await skill.reload();
    t.true(skill.automaticIsRecommended);
    t.is(t.context.incCounter.callCount, 2);
    t.true(t.context.incCounter.firstCall.calledWith('automatic_skill_isrecommended_true_summ'));
    t.true(
        t.context.incCounter.secondCall.calledWith('automatic_skill_isrecommended_changed_summ', 1),
    );
});

test('setAutomaticIsRecommended() rewrites flag from false to true', async t => {
    const skill = await createSkill({ onAir: true, automaticIsRecommended: true });
    t.true(skill.automaticIsRecommended);
    await recommendations.setAutomaticIsRecommended(skill.id, false);
    await skill.reload();
    t.false(skill.automaticIsRecommended);
    t.is(t.context.incCounter.callCount, 2);
    t.true(t.context.incCounter.firstCall.calledWith('automatic_skill_isrecommended_false_summ'));
    t.true(
        t.context.incCounter.secondCall.calledWith('automatic_skill_isrecommended_changed_summ', 1),
    );
});
