/* eslint-disable */
import test from 'ava';
import { createSandbox } from 'sinon';
import { createImageForSkill, createSkill, createUser, wipeDatabase } from '../../_helpers';
import { getCurrentETagVersion, getETag, getSkillModerationSettings } from '../../../../services/moderation/eTag';
import { DraftStatus } from '../../../../db/tables/draft';
import config from '../../../../services/config';

test.beforeEach(async t => {
    await wipeDatabase();
    await createUser();
});

test('etag is changed if status has changed', async t => {
    const skill = await createSkill();
    skill.draft.logo2 = await createImageForSkill(skill);
    const eTag = getETag(skill);
    skill.draft.status = DraftStatus.ReviewApproved;

    t.notDeepEqual(getETag(skill), eTag);
});

test("etag doesn't change if brandVerificationWebsite schema is changed", async t => {
    const skill = await createSkill({
        publishingSettings: {
            brandVerificationWebsite: 'http://dialogs.yandex.ru',
        },
    });
    skill.draft.logo2 = await createImageForSkill(skill);
    const httpETag = getETag(skill);

    await skill.draft.update({
        publishingSettings: {
            brandVerificationWebsite: 'https://dialogs.yandex.ru',
        },
    });
    await skill.draft.reload();

    t.deepEqual(getETag(skill), httpETag);
});

test('etag changes if webhook brandVerificationWebsite path is changed', async t => {
    const skill = await createSkill({
        publishingSettings: {
            brandVerificationWebsite: 'http://dialogs.yandex.ru',
            brandIsVerified: true,
        },
    });
    skill.draft.logo2 = await createImageForSkill(skill);
    const httpETag = getETag(skill);

    await skill.draft.update({
        publishingSettings: {
            brandVerificationWebsite: 'http://dialogs.yandex.ru/webhook',
        },
    });
    await skill.draft.reload();

    t.notDeepEqual(getETag(skill), httpETag);
});

test('etag changes if third level domain is changed', async t => {
    const skill = await createSkill({
        publishingSettings: {
            brandVerificationWebsite: 'http://dialogs.yandex.ru',
            brandIsVerified: true,
        },
    });
    skill.draft.logo2 = await createImageForSkill(skill);
    const httpETag = getETag(skill);

    await skill.draft.update({
        publishingSettings: {
            brandVerificationWebsite: 'http://dialogs2.yandex.ru',
        },
    });
    await skill.draft.reload();

    t.notDeepEqual(getETag(skill), httpETag);
});

test('etag changes if second level domain is changed', async t => {
    const skill = await createSkill({
        publishingSettings: {
            brandVerificationWebsite: 'http://yandex.ru',
            brandIsVerified: true,
        },
    });
    skill.draft.logo2 = await createImageForSkill(skill);
    const httpETag = getETag(skill);

    await skill.draft.update({
        publishingSettings: {
            brandVerificationWebsite: 'http://yandex2.ru',
        },
    });
    await skill.draft.reload();

    t.notDeepEqual(getETag(skill), httpETag);
});

test('eTag: default implementation', t => {
    t.is(getCurrentETagVersion(), 'v7');
});

test('eTag: change implementation', t => {
    const sinon = createSandbox();
    sinon.replace(config.app.moderation, 'ETagVersion', 'v3');

    t.is(getCurrentETagVersion(), 'v3');

    sinon.restore();
});

test('eTag: v3 contains activationPhrasesMeta', async t => {
    const sinon = createSandbox();
    sinon.replace(config.app.moderation, 'ETagVersion', 'v3');

    const skill = await createSkill();

    await skill.draft.update({
        activationPhrases: ['a', 'b', 'c'],
        activationPhrasesCommonness: ['green', 'red', 'red'],
        activationPhrasesForceApproval: [false, false, true],
    });

    t.deepEqual(getSkillModerationSettings(skill).activationPhrasesMeta, [
        { phrase: 'a', commonness: 'green', forceApprove: false },
        { phrase: 'b', commonness: 'red', forceApprove: false },
        { phrase: 'c', commonness: 'red', forceApprove: true },
    ]);

    sinon.restore();
});

test('eTag: brandVerificationWebsite empty if not approved', async t => {
    const skill = await createSkill();

    await skill.draft.update({
        publishingSettings: {
            brandVerificationWebsite: 'https://example.org',
            brandIsVerified: false,
        },
    });

    t.is(getSkillModerationSettings(skill).brandVerificationWebsite, '');
});

test('eTag: brandVerificationWebsite not empty if approved', async t => {
    const skill = await createSkill();

    await skill.draft.update({
        publishingSettings: {
            brandVerificationWebsite: 'https://example.org',
            brandIsVerified: true,
        },
    });

    t.is(getSkillModerationSettings(skill).brandVerificationWebsite, 'https://example.org');
});
