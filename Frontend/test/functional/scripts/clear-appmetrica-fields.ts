/* eslint-disable */
import test from 'ava';
import { clearAppMetricaFields } from '../../../scripts/clear-appmetrica-fields';
import { createSkill, createUser, wipeDatabase } from '../_helpers';

test.beforeEach(wipeDatabase);

test('Should clear appmetrica api key for skill from user without feature flag', async t => {
    await createUser({});
    const skill = await createSkill({
        appMetricaApiKey: 'key',
    });

    t.is(skill.appMetricaApiKey, 'key');
    t.is(skill.draft.appMetricaApiKey, 'key');

    await clearAppMetricaFields();
    await skill.reload();

    t.is(skill.appMetricaApiKey, null);
    t.is(skill.draft.appMetricaApiKey, null);
});

test('Should not clear appmetrica api key for skill from user with feature flag', async t => {
    await createUser({
        featureFlags: {
            allowUseAppMetrica: true,
        },
    });
    const skill = await createSkill({
        appMetricaApiKey: 'key',
    });

    t.is(skill.appMetricaApiKey, 'key');
    t.is(skill.draft.appMetricaApiKey, 'key');

    await clearAppMetricaFields();
    await skill.reload();

    t.is(skill.appMetricaApiKey, 'key');
    t.is(skill.draft.appMetricaApiKey, 'key');
});

test('mixed case', async t => {
    const user1 = await createUser({
        featureFlags: {
            allowUseAppMetrica: true,
        },
    });

    const user2 = await createUser({
        id: '0002',
        featureFlags: {},
    });

    const skill1 = await createSkill({
        appMetricaApiKey: 'key',
        userId: user1.id,
    });

    const skill2 = await createSkill({
        appMetricaApiKey: 'key',
        userId: user2.id,
    });

    t.is(skill1.appMetricaApiKey, 'key');
    t.is(skill1.draft.appMetricaApiKey, 'key');
    t.is(skill2.appMetricaApiKey, 'key');
    t.is(skill2.draft.appMetricaApiKey, 'key');

    await clearAppMetricaFields();
    await skill1.reload();
    await skill2.reload();

    t.is(skill1.appMetricaApiKey, 'key');
    t.is(skill1.draft.appMetricaApiKey, 'key');
    t.is(skill2.appMetricaApiKey, null);
    t.is(skill2.draft.appMetricaApiKey, null);
});
