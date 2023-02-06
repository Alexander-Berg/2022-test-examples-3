/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import * as sinon from 'sinon';
import { createSkill, createUser, wipeDatabase } from '../../../_helpers';
import { getUserTicket, testUser } from '../../_helpers';
import { Channel, SkillAccess } from '../../../../../db/tables/settings';
import * as tycoon from '../../../../../services/tycoon';
import { callApi } from '../_helpers';
import { UserInstance } from '../../../../../db/tables/user';
import * as nlu from '../../../../../services/nlu';

interface TestContext {
    userTicket: string;
    user: UserInstance;
}

const test = anyTest as TestInterface<TestContext>;

test.before(async t => {
    const userTicket = await getUserTicket(testUser.oauthToken);

    t.context.userTicket = userTicket;
});

test.beforeEach(async t => {
    await wipeDatabase();
    t.context.user = await createUser({ id: testUser.uid });
});

test.afterEach.always(async t => {
    sinon.restore();
});

test('patchDraft: validates organization from request', async t => {
    const skill = await createSkill({ userId: testUser.uid, channel: Channel.OrganizationChat });
    skill.user = t.context.user;
    const checkOrganizationAccess = sinon.stub(tycoon, 'checkOrganizationAccess');
    checkOrganizationAccess.withArgs(sinon.match({ id: skill.user.id }), '1', Channel.OrganizationChat).resolves(false);
    checkOrganizationAccess.withArgs(sinon.match({ id: skill.user.id }), '2', Channel.OrganizationChat).resolves(true);
    await skill.draft.update({
        publishingSettings: {
            organizationId: '1',
            organizationIsVerified: false,
        },
    });
    await skill.reload();

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        publishingSettings: {
            organizationId: '2',
        },
    });

    t.is(res.status, 200);
    await skill.reload();
    t.assert(checkOrganizationAccess.calledOnceWith(sinon.match({ id: skill.user.id }), '2', Channel.OrganizationChat));
});

test('patchDraft: autovalidates organization from request for Yandex user', async t => {
    const skill = await createSkill({ userId: testUser.uid, channel: Channel.OrganizationChat });

    await t.context.user.update({
        featureFlags: {
            allowConfigureYandexChat: true,
        },
    });
    await t.context.user.reload();
    skill.user = t.context.user;

    const checkOrganizationAccess = sinon.spy(tycoon, 'checkOrganizationAccess');
    await skill.draft.update({
        publishingSettings: {
            organizationId: '1',
            organizationIsVerified: false,
        },
    });
    await skill.reload();

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        publishingSettings: {
            organizationId: '2',
        },
    });

    t.is(res.status, 200);
    await skill.reload();
    t.assert(
        checkOrganizationAccess.calledOnceWith(
            sinon.match({
                id: skill.user.id,
                featureFlags: sinon.match({
                    allowConfigureYandexChat: true,
                }),
            }),
            '2',
            Channel.OrganizationChat,
        ),
    );
    t.assert(checkOrganizationAccess.returned(Promise.resolve(true)));
});

const soundSet = {
    sounds: [
        {
            originalName: 'F4.mp3',
            originalPath:
                'dialogs-upload/sounds/orig/47d930d7-93b9-4a85-8ff5-1db83998e3f8/1a428912-c996-4519-a359-01956e8e3f1b',
            id: '1a428912-c996-4519-a359-01956e8e3f1b',
        },
        {
            originalName: 'G4.mp3',
            originalPath:
                'dialogs-upload/sounds/orig/47d930d7-93b9-4a85-8ff5-1db83998e3f8/f0b69318-a7c7-477b-ad2a-b59ad4f1bb89',
            id: 'f0b69318-a7c7-477b-ad2a-b59ad4f1bb89',
        },
        {
            originalName: 'D4.mp3',
            originalPath:
                'dialogs-upload/sounds/orig/47d930d7-93b9-4a85-8ff5-1db83998e3f8/530bde04-5165-4807-a584-a4034cab1d58',
            id: '530bde04-5165-4807-a584-a4034cab1d58',
        },
        {
            originalName: 'E4.mp3',
            originalPath:
                'dialogs-upload/sounds/orig/47d930d7-93b9-4a85-8ff5-1db83998e3f8/66dca389-8bc9-4774-843a-0283880d0ce5',
            id: '66dca389-8bc9-4774-843a-0283880d0ce5',
        },
        {
            originalName: 'F4.mp3',
            originalPath:
                'dialogs-upload/sounds/orig/47d930d7-93b9-4a85-8ff5-1db83998e3f8/9e08665b-935b-439b-8226-e35c56599ab1',
            id: '9e08665b-935b-439b-8226-e35c56599ab1',
        },
        {
            originalName: 'G4.mp3',
            originalPath:
                'dialogs-upload/sounds/orig/47d930d7-93b9-4a85-8ff5-1db83998e3f8/24204cc6-c585-4024-83ac-31d6fc0b4669',
            id: '24204cc6-c585-4024-83ac-31d6fc0b4669',
        },
    ],
    settings: { noOverlaySamples: true, repeatSoundInside: true, stopOnCeil: false },
};

test('patchDraft: automatically publish valid private thereminvox skill', async t => {
    await t.context.user.update({
        featureFlags: {
            allowCreateThereminvoxSkills: true,
        },
    });
    await t.context.user.reload();

    sinon.replace(nlu, 'inflect', sinon.fake.resolves(['test']));

    const skill = await createSkill({ userId: testUser.uid, channel: Channel.Thereminvox });
    skill.user = t.context.user;

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        name: 'Новый навык Синтезатор',
        hideInStore: true,
        backendSettings: {
            soundSet,
        },
    });

    await skill.reload();

    t.is(res.status, 200);
    t.true(skill.onAir);
});

test('patchDraft: automatically publish invalid private thereminvox skill', async t => {
    await t.context.user.update({
        featureFlags: {
            allowCreateThereminvoxSkills: true,
        },
    });
    await t.context.user.reload();

    sinon.replace(nlu, 'inflect', sinon.fake.resolves(['test']));

    const skill = await createSkill({ userId: testUser.uid, channel: Channel.Thereminvox });
    skill.user = t.context.user;

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        name: 'Новый навык Синтезатор',
        hideInStore: true,
        backendSettings: {},
    });

    await skill.reload();

    t.is(res.status, 200);
    t.true(skill.onAir);
});

// FIXME delete when tehreminvox skills went public

test('patchDraft: ignore hideInStore = false and set it to true', async t => {
    await t.context.user.update({
        featureFlags: {
            allowCreateThereminvoxSkills: true,
        },
    });
    await t.context.user.reload();

    sinon.replace(nlu, 'inflect', sinon.fake.resolves(['test']));

    const skill = await createSkill({ userId: testUser.uid, channel: Channel.Thereminvox });
    skill.user = t.context.user;

    await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        name: 'Новый навык Синтезатор',
        hideInStore: false,
        backendSettings: {
            soundSet,
        },
    });

    await skill.reload();
    await skill.draft.reload();

    t.true(skill.draft.hideInStore);
    t.true(skill.hideInStore);

    t.is(skill.draft.skillAccess, SkillAccess.Private);
    t.is(skill.skillAccess, SkillAccess.Private);

    t.true(skill.onAir);
});

test('patchDraft: thereminvox ignore skillAccess != private and set it to private', async t => {
    await t.context.user.update({
        featureFlags: {
            allowCreateThereminvoxSkills: true,
        },
    });
    await t.context.user.reload();

    sinon.replace(nlu, 'inflect', sinon.fake.resolves(['test']));

    const skill = await createSkill({ userId: testUser.uid, channel: Channel.Thereminvox });
    skill.user = t.context.user;

    await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        name: 'Новый навык Синтезатор',
        skillAccess: SkillAccess.Public,
        backendSettings: {
            soundSet,
        },
    });

    await skill.reload();
    await skill.draft.reload();

    t.true(skill.draft.hideInStore);
    t.true(skill.hideInStore);

    t.is(skill.draft.skillAccess, SkillAccess.Private);
    t.is(skill.skillAccess, SkillAccess.Private);

    t.true(skill.onAir);
});

// FIXME unskip when thereminvox skills went public

test.skip('patchDraft: not automatically publish public thereminvox skill', async t => {
    await t.context.user.update({
        featureFlags: {
            allowCreateThereminvoxSkills: true,
        },
    });
    await t.context.user.reload();

    sinon.replace(nlu, 'inflect', sinon.fake.resolves(['test']));

    const skill = await createSkill({ userId: testUser.uid, channel: Channel.Thereminvox });
    skill.user = t.context.user;

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        name: 'Новый навык Синтезатор',
        hideInStore: false,
        backendSettings: {
            soundSet,
        },
    });

    await skill.reload();

    t.is(res.status, 200);
    t.false(skill.onAir);
});
