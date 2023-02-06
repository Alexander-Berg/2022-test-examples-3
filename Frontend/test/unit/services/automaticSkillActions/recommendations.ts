/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import * as sinon from 'sinon';
import config from '../../../../services/config';
import * as skillRepository from '../../../../db/repositories/skill';
import * as skillRecommendationActions from '../../../../services/automaticSkillActions/recommendations';

interface TestContext {
    setAutomaticIsRecommended: sinon.SinonStub;
    dryRun: sinon.SinonStub;
}

const test = anyTest as TestInterface<TestContext>;

test.beforeEach(async t => {
    t.context.setAutomaticIsRecommended = sinon.stub(skillRepository, 'setAutomaticIsRecommended');
    t.context.dryRun = sinon.stub(config.automaticSkillActions.isRecommended, 'dryRun');
});

test.afterEach.always(async t => {
    sinon.restore();
});

// test.serial нужны для того, чтобы избежать гонки при создании стабов

test.serial("doesn't call skillRepository if dry run is true", async t => {
    t.context.dryRun.value(true);
    await skillRecommendationActions.setAutomaticIsRecommended('4d9aad50-187d-48a4-aff7-f06b869d7254', true);
    t.is(t.context.setAutomaticIsRecommended.callCount, 0);
});

test.serial('calls skillRepository if dry run is false', async t => {
    t.context.dryRun.value(false);
    const skillId = '4d9aad50-187d-48a4-aff7-f06b869d7254';
    await skillRecommendationActions.setAutomaticIsRecommended(skillId, true);
    t.true(t.context.setAutomaticIsRecommended.calledOnceWithExactly(skillId, true));
});
