/* eslint-disable */
import test from 'ava';
import { getIntentStatus, intentHasSettings, IntentStatus } from '../../../../services/granet/intentStatus';
import { createIntent, wipeDatabase, createSkill, createUser } from '../../_helpers';

test.beforeEach(wipeDatabase);

test('intentHasSettings: should check if one of settings exists', async t => {
    await createUser();
    const skill = await createSkill();

    const intent = await createIntent(
        {
            skillId: skill.id,
            formName: 'hello',
        },
        { isDraft: true },
    );

    t.true(intentHasSettings(intent));
});

test('intentHasSettings: should check if one of settings exists and return false if not', async t => {
    await createUser();
    const skill = await createSkill();

    const intent = await createIntent(
        {
            skillId: skill.id,
        },
        { isDraft: true },
    );

    t.false(intentHasSettings(intent));
});

test('getIntentStatus: should return new status for just created intent', async t => {
    await createUser();
    const skill = await createSkill();

    const intent = await createIntent(
        {
            skillId: skill.id,
        },
        { isDraft: true },
    );

    t.is(getIntentStatus(intent), IntentStatus.NEW);
});

test('getIntentStatus: should not return new status when field is initially set', async t => {
    await createUser();
    const skill = await createSkill();

    const intent = await createIntent(
        {
            skillId: skill.id,
            formName: 'hello',
        },
        { isDraft: true },
    );

    t.not(getIntentStatus(intent), IntentStatus.NEW);
});

test('getIntentStatus: should return invalid status for intent without base64', async t => {
    await createUser();
    const skill = await createSkill();

    const intent = await createIntent(
        {
            skillId: skill.id,
            formName: 'hello',
        },
        { isDraft: true },
    );

    t.is(getIntentStatus(intent), IntentStatus.INVALID_GRAMMAR);
});

test('getIntentStatus: should return valid status for valid intent', async t => {
    await createUser();
    const skill = await createSkill();

    const intent = await createIntent(
        {
            skillId: skill.id,
            formName: 'hello',
            humanReadableName: 'hello',
            sourceText: 'root: hello',
            base64: 'base64',
        },
        { isDraft: true },
    );

    t.is(getIntentStatus(intent), IntentStatus.OK);
});
