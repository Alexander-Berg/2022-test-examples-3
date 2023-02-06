/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import * as sinon from 'sinon';
import { createSkill, createUser, wipeDatabase } from '../../_helpers';
import { Channel } from '../../../../db/tables/settings';
import { Operation } from '../../../../db';
import * as stopSkills from '../../../../services/automaticSkillActions/stopSkills';
import { AutomaticSkillAction } from '../../../../services/automaticSkillActions/stopSkills';
import { OperationType } from '../../../../db/tables/operation';
import { UserInstance } from '../../../../db/tables/user';
import * as mail from '../../../../services/mail';
import config from '../../../../services/config';

const test = anyTest as TestInterface<{
    user: UserInstance;
    sendPingUnanswersAlert: sinon.SinonStub;
}>;

test.beforeEach(async t => {
    await wipeDatabase();
    t.context.user = await createUser();
    t.context.sendPingUnanswersAlert = sinon.stub(mail, 'sendPingUnanswersAlert');
});

test.afterEach.always(async t => {
    sinon.restore();
});

test('tryStopSkill stops active organization chat', async t => {
    const skill = await createSkill({
        channel: Channel.OrganizationChat,
        onAir: true,
    });
    await Operation.destroy({
        where: {
            itemId: skill.id,
        },
    });
    await stopSkills.tryStopSkill(skill);
    await skill.reload();
    t.false(skill.onAir);
    t.deepEqual(
        await Operation.count({
            where: {
                itemId: skill.id,
                type: OperationType.SkillStopped,
                comment: stopSkills.automaticSkillStopComment,
            },
        }),
        1,
    );
});

test('tryStopSkill resets draft.approvedETag', async t => {
    const skill = await createSkill({
        channel: Channel.OrganizationChat,
        onAir: true,
    });
    skill.draft.approvedETag = 'approvedETag';
    await stopSkills.tryStopSkill(skill);
    await skill.reload();
    t.is(skill.draft.approvedETag, null);
});

test('tryStopSkill resets draft.approvedPrivateETag', async t => {
    const skill = await createSkill({
        channel: Channel.OrganizationChat,
        onAir: true,
    });
    skill.draft.approvedPrivateETag = 'approvedPrivateETag';
    await stopSkills.tryStopSkill(skill);
    await skill.reload();
    t.is(skill.draft.approvedPrivateETag, null);
});

test('tryStopSkill sets skill.isRecommended to false', async t => {
    const skill = await createSkill({
        channel: Channel.AliceSkill,
        onAir: true,
    });
    skill.draft.approvedETag = 'approvedETag';
    await stopSkills.tryStopSkill(skill);
    await skill.reload();
    t.is(skill.isRecommended, false);
});

test('tryStopSkill stops active alice skill', async t => {
    const skill = await createSkill({
        channel: Channel.AliceSkill,
        onAir: true,
    });
    await Operation.destroy({
        where: {
            itemId: skill.id,
        },
    });
    await stopSkills.tryStopSkill(skill);
    await skill.reload();
    t.false(skill.onAir);
    t.deepEqual(
        await Operation.count({
            where: {
                itemId: skill.id,
                type: OperationType.SkillStopped,
                comment: stopSkills.automaticSkillStopComment,
            },
        }),
        1,
    );
});

test("tryStopSkill doesn't stop organizationChat with onAir=false", async t => {
    const skill = await createSkill({
        channel: Channel.OrganizationChat,
        onAir: false,
    });
    await Operation.destroy({
        where: {
            itemId: skill.id,
        },
    });
    await stopSkills.tryStopSkill(skill);
    await skill.reload();
    t.false(skill.onAir);
    t.deepEqual(
        await Operation.count({
            where: {
                itemId: skill.id,
                type: OperationType.SkillStopped,
            },
        }),
        0,
    );
});

test("tryStopSkill doesn't stop skill with onAir=false", async t => {
    const skill = await createSkill({
        channel: Channel.AliceSkill,
        onAir: false,
    });
    await Operation.destroy({
        where: {
            itemId: skill.id,
        },
    });
    await stopSkills.tryStopSkill(skill);
    await skill.reload();
    t.false(skill.onAir);
    t.deepEqual(
        await Operation.count({
            where: {
                itemId: skill.id,
                type: OperationType.SkillStopped,
            },
        }),
        0,
    );
});

test("tryStopSkill doesn't stop private skills", async t => {
    const skill = await createSkill({
        channel: Channel.AliceSkill,
        onAir: true,
        hideInStore: true,
    });
    await Operation.destroy({
        where: {
            itemId: skill.id,
        },
    });
    await stopSkills.tryStopSkill(skill);
    await skill.reload();
    t.true(skill.onAir);
    t.deepEqual(
        await Operation.count({
            where: {
                itemId: skill.id,
                type: OperationType.SkillStopped,
            },
        }),
        0,
    );
});

test("tryStopSkill doesn't stop internal skills", async t => {
    const skill = await createSkill({
        channel: Channel.AliceSkill,
        onAir: true,
        look: 'internal',
    });
    await Operation.destroy({
        where: {
            itemId: skill.id,
        },
    });
    await stopSkills.tryStopSkill(skill);
    await skill.reload();
    t.true(skill.onAir);
    t.deepEqual(
        await Operation.count({
            where: {
                itemId: skill.id,
                type: OperationType.SkillStopped,
            },
        }),
        0,
    );
});

test("processSingleFailingSkill doesn't fail on unknown skill", async t => {
    const result = await stopSkills.processSingleFailingSkill({
        skillId: '0b8a0495-978f-47fe-9360-5171487b8fbc',
        action: AutomaticSkillAction.Stop,
    });
    t.deepEqual(result, {
        skillId: '0b8a0495-978f-47fe-9360-5171487b8fbc',
        skillName: null,
        action: AutomaticSkillAction.Stop,
        applied: false,
    });
});

test('processSingleFailingSkill tries to stop known skill', async t => {
    const skill = await createSkill({ onAir: true });
    const tryStopSkill = sinon.stub(stopSkills.actionImplementations, stopSkills.AutomaticSkillAction.Stop).resolves({
        skillId: skill.id,
        skillName: skill.name,
        action: AutomaticSkillAction.Stop,
        applied: true,
    });
    const result = await stopSkills.processSingleFailingSkill({
        skillId: skill.id,
        action: AutomaticSkillAction.Stop,
    });
    t.deepEqual(result, {
        skillId: skill.id,
        skillName: skill.name,
        action: AutomaticSkillAction.Stop,
        applied: true,
    });
    t.true(tryStopSkill.calledOnce);
});

test('warnSkillDeveloper sets isRecommended to false', async t => {
    const skill = await createSkill({ onAir: true, isRecommended: true });
    await stopSkills.warnSkillDeveloper(skill);
    await skill.reload();
    t.false(skill.isRecommended);
});

test('warnSkillDeveloper sends email notification', async t => {
    const skill = await createSkill({ onAir: true, isRecommended: true });
    skill.user = t.context.user;
    await stopSkills.warnSkillDeveloper(skill);
    t.true(t.context.sendPingUnanswersAlert.calledOnceWith(t.context.user, skill.id, skill.name));
});

test("warnSkillDeveloper doesn't send second notification", async t => {
    const skill = await createSkill({ onAir: true, isRecommended: true });
    skill.user = t.context.user;
    await stopSkills.warnSkillDeveloper(skill);
    await stopSkills.warnSkillDeveloper(skill);
    t.true(t.context.sendPingUnanswersAlert.calledOnceWith(t.context.user, skill.id, skill.name));
});

test('warnSkillDeveloper sends second notification after cooldown period', async t => {
    const skill = await createSkill({ onAir: true, isRecommended: true });
    skill.user = t.context.user;
    sinon.stub(config.automaticSkillActions.emailAlerts, 'pingUnanswerAlertCooldownPeriod').value(0);
    await stopSkills.warnSkillDeveloper(skill);
    await stopSkills.warnSkillDeveloper(skill);
    t.is(t.context.sendPingUnanswersAlert.callCount, 2);
    for (const call of t.context.sendPingUnanswersAlert.getCalls()) {
        call.calledWith(t.context.user, skill.id, skill.name);
    }
});

test('restoreIsRecommended() restores flag', async t => {
    const skill = await createSkill({
        onAir: true,
        isRecommended: false,
    });
    await stopSkills.restoreIsRecommended(skill);
    await skill.reload();
    t.true(skill.isRecommended);
});
