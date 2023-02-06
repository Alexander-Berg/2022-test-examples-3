/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import * as sinon from 'sinon';
import { Operation } from '../../../../../db';
import { requestReview, approveReview } from '../../../../../services/skill-lifecycle';
import * as skillLifecycle from '../../../../../services/skill-lifecycle';
import { createSkill, createUser, wipeDatabase } from '../../../_helpers';
import { getUserTicket, testUser } from '../../_helpers';
import { callApi, respondsWithError } from '../_helpers';
import * as nlu from '../../../../../services/nlu';
import { Voice } from '../../../../../fixtures/voices';
import * as tycoon from '../../../../../services/tycoon';
import { Channel, SkillAccess } from '../../../../../db/tables/settings';
import { UserInstance } from '../../../../../db/tables/user';
import * as validationRules from '../../../../../services/validationRules';
import FormValidationError from '../../../../../db/errors';
import * as starTrekService from '../../../../../services/startrek';
import * as samsaraService from '../../../../../services/samsara';
import * as skillValidation from '../../../../../services/skill-validation';
import * as endpointValidation from '../../../../../services/endpointUrlValidation';

function countOperations(itemId: string) {
    return Operation.count({ where: { itemId } });
}

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
    sinon.replace(skillValidation, 'isDraftAllowedForDeploy', sinon.fake.returns(true));
    sinon.replace(endpointValidation, 'validateEndpointUrl', sinon.fake.resolves(undefined));
    t.context.user = await createUser({ id: testUser.uid });
});

test.afterEach.always(async() => {
    sinon.restore();
});

test('Send draft to review', async t => {
    const skill = await createSkill({ userId: testUser.uid });

    const res = await callApi('post', `/skills/${skill.id}/candidate`, t.context);
    await skill.draft.reload();

    t.is(res.status, 201);
    t.deepEqual(skill.draft.status, 'reviewRequested');
});

test('Send smart home draft to review (skip webhook check)', async t => {
    sinon.stub(validationRules, 'webhookShouldReturnValidJson').value(
        // This should never be thrown cause of skipping webhook check
        sinon.fake.throws(new FormValidationError('uri', 'invalid')),
    );
    sinon.stub(starTrekService, 'createTicket').resolves({
        key: 'TEST-111',
        status: {
            key: 'open',
        },
    });
    sinon.stub(starTrekService, 'changeStatus').resolves({
        key: 'TEST-111',
        status: {
            key: 'moderation',
        },
    });
    sinon.stub(samsaraService, 'createTicket').resolves({
        rc: 'OK',
        description: 'TicketId',
        entityId: 103852057,
    });
    sinon.stub(samsaraService, 'linkToStartrek').resolves({
        rc: 'OK',
    });

    const skill = await createSkill({
        userId: testUser.uid,
        channel: Channel.SmartHome,
        backendSettings: {
            uri: 'https://invalid_url.com/',
        },
        publishingSettings: {
            description: 'test',
        },
    });

    const res = await callApi('post', `/skills/${skill.id}/candidate`, t.context);
    await skill.draft.reload();

    t.is(res.status, 201);
    t.deepEqual(skill.draft.status, 'reviewRequested');
});

test('Send custom skill draft to review (no skip webhook check)', async t => {
    sinon
        .stub(validationRules, 'webhookShouldReturnValidJson')
        .value(sinon.fake.throws(new FormValidationError('uri', 'invalid')));

    const skill = await createSkill({
        userId: testUser.uid,
        channel: Channel.AliceSkill,
        backendSettings: {
            uri: 'https://invalid_url.com/',
        },
    });

    const res = await callApi('post', `/skills/${skill.id}/candidate`, t.context);

    respondsWithError(
        {
            code: 400,
            message: 'Validation error',
            fields: {
                uri: 'invalid',
            },
        },
        res,
        t,
    );
});

test('Check not to skip review for public smart home skill', async t => {
    const skill = await createSkill({
        userId: testUser.uid,
        channel: Channel.SmartHome,
        hideInStore: false,
        publishingSettings: {
            description: 'test',
        },
    });
    sinon.stub(starTrekService, 'createTicket').resolves({
        key: 'TEST-111',
        status: {
            key: 'open',
        },
    });
    sinon.stub(starTrekService, 'changeStatus').resolves({
        key: 'TEST-111',
        status: {
            key: 'moderation',
        },
    });
    sinon.stub(samsaraService, 'createTicket').resolves({
        rc: 'OK',
        description: 'TicketId',
        entityId: 103852057,
    });
    sinon.stub(samsaraService, 'linkToStartrek').resolves({
        rc: 'OK',
    });

    const res = await callApi('post', `/skills/${skill.id}/candidate`, t.context);
    await skill.draft.reload();

    t.is(res.status, 201);
    t.deepEqual(skill.draft.status, 'reviewRequested');
});

test('Check to skip review for private smart home skill', async t => {
    const skill = await createSkill({
        userId: testUser.uid,
        channel: Channel.SmartHome,
        hideInStore: true,
    });

    const res = await callApi('post', `/skills/${skill.id}/candidate`, t.context);
    await skill.draft.reload();

    t.is(res.status, 201);
    t.deepEqual(skill.draft.status, 'reviewApproved');
});

test('Deploy smart home draft automatically', async t => {
    const skill = await createSkill({
        userId: testUser.uid,
        channel: Channel.SmartHome,
        onAir: false,
        publishingSettings: {
            description: 'test',
        },
        hideInStore: true,
    });

    await requestReview(skill, { user: t.context.user });
    await approveReview(skill, { user: t.context.user });

    const completeDeployStat = sinon.spy(skillLifecycle, 'completeDeploy');

    const res = await callApi('post', `/skills/${skill.id}/release`, t.context);

    t.is(res.status, 201);
    t.true(completeDeployStat.calledOnce);

    await skill.draft.reload();
    await skill.reload();

    t.true(skill.onAir);
    t.deepEqual(skill.draft.status, 'inDevelopment');
});

test("yaCloudGrant change doesn't require moderator review", async t => {
    const skill = await createSkill({ userId: testUser.uid });
    await skill.draft.update({
        status: 'reviewRequested',
    });
    // call approveReview to fill approvedETag field
    await approveReview(skill, { user: t.context.user });
    const operationCount = await countOperations(skill.id);
    await skill.draft.update({
        yaCloudGrant: true,
    });

    const res = await callApi('post', `/skills/${skill.id}/candidate`, t.context);
    await skill.draft.reload();

    t.is(res.status, 201);
    t.deepEqual(skill.draft.status, 'reviewApproved');
    t.deepEqual(operationCount, await countOperations(skill.id));
});

test('name changes require moderator review', async t => {
    const skill = await createSkill({ userId: testUser.uid });
    await skill.draft.update({
        status: 'reviewRequested',
    });
    // call approveReview to fill approvedETag field
    await approveReview(skill, { user: t.context.user });
    const operationCount = await countOperations(skill.id);
    await skill.draft.update({
        name: 'brand new name',
    });

    const res = await callApi('post', `/skills/${skill.id}/candidate`, t.context);
    await skill.draft.reload();

    t.is(res.status, 201);
    t.deepEqual(skill.draft.status, 'reviewRequested');
    t.deepEqual(operationCount + 1, await countOperations(skill.id));
});

test("private skill doesn't require review unless activationPhrase is changed", async t => {
    const sandbox = sinon.createSandbox();
    sinon.replace(nlu, 'inflect', sinon.fake.resolves([]));

    const skill = await createSkill({ userId: testUser.uid });
    await skill.draft.update({
        hideInStore: true,
        skillAccess: SkillAccess.Hidden,
        status: 'reviewRequested',
    });
    await approveReview(skill, { user: t.context.user });
    const operationCount1 = await countOperations(skill.id);

    await skill.draft.update({
        voice: Voice.Zahar,
    });

    const res1 = await callApi('post', `/skills/${skill.id}/candidate`, t.context);
    await skill.draft.reload();

    t.is(res1.status, 201);
    t.deepEqual(skill.draft.status, 'reviewApproved');
    t.deepEqual(operationCount1, await countOperations(skill.id));

    await skill.draft.update({
        activationPhrases: ['foo', 'bar', 'baz'],
    });
    await approveReview(skill, { user: t.context.user });
    const operationCount2 = await countOperations(skill.id);

    const res2 = await callApi('post', `/skills/${skill.id}/candidate`, t.context);
    await skill.draft.reload();

    t.is(res2.status, 201);
    t.deepEqual(skill.draft.status, 'reviewRequested');
    t.deepEqual(operationCount2 + 1, await countOperations(skill.id));

    sandbox.restore();
});

test("private skill doesn't require review if approved while being public", async t => {
    const skill = await createSkill({ userId: testUser.uid });
    await skill.draft.update({
        hideInStore: false,
        status: 'reviewRequested',
    });
    await approveReview(skill, { user: t.context.user });
    const operationCount = await countOperations(skill.id);
    await skill.draft.update({
        hideInStore: true,
        skillAccess: SkillAccess.Hidden,
    });

    const res = await callApi('post', `/skills/${skill.id}/candidate`, t.context);
    await skill.draft.reload();

    t.is(res.status, 201);
    t.deepEqual(skill.draft.status, 'reviewApproved');
    t.deepEqual(operationCount, await countOperations(skill.id));
});

test('public skill requires review if approved while being private', async t => {
    const skill = await createSkill({ userId: testUser.uid });
    await skill.draft.update({
        hideInStore: true,
        skillAccess: SkillAccess.Hidden,
        status: 'reviewRequested',
    });
    await approveReview(skill, { user: t.context.user });
    const operationCount = await countOperations(skill.id);
    await skill.draft.update({
        hideInStore: false,
        skillAccess: SkillAccess.Public,
    });

    const res = await callApi('post', `/skills/${skill.id}/candidate`, t.context);
    await skill.draft.reload();

    t.is(res.status, 201);
    t.deepEqual(skill.draft.status, 'reviewRequested');
    t.deepEqual(operationCount + 1, await countOperations(skill.id));
});

test('Tycoon checks organization from draft', async t => {
    const skill = await createSkill({ userId: testUser.uid, channel: Channel.OrganizationChat });
    skill.user = t.context.user;
    const checkOrganizationAccess = sinon.stub(tycoon, 'checkOrganizationAccess');
    checkOrganizationAccess.withArgs(sinon.match({ id: skill.user.id }), '1', Channel.OrganizationChat).resolves(false);
    checkOrganizationAccess.withArgs(sinon.match({ id: skill.user.id }), '2', Channel.OrganizationChat).resolves(true);
    await skill.update({
        publishingSettings: {
            organizationId: '1',
            organizationIsVerified: true,
        },
    });
    await skill.draft.update({
        publishingSettings: {
            organizationId: '2',
            organizationIsVerified: false,
        },
    });
    await skill.reload();

    const res = await callApi('post', `/skills/${skill.id}/candidate`, t.context);
    await skill.draft.reload();

    t.assert(checkOrganizationAccess.calledOnceWith(sinon.match({ id: skill.user.id }), '2', Channel.OrganizationChat));
    t.is(res.status, 201);
    t.deepEqual(skill.draft.status, 'reviewRequested');
});

test('Autochecks organization for Yandex user', async t => {
    const skill = await createSkill({ userId: testUser.uid, channel: Channel.OrganizationChat });

    await t.context.user.update({
        featureFlags: {
            allowConfigureYandexChat: true,
        },
    });
    await t.context.user.reload();
    skill.user = t.context.user;

    const checkOrganizationAccess = sinon.spy(tycoon, 'checkOrganizationAccess');
    await skill.update({
        publishingSettings: {
            organizationId: '1',
            organizationIsVerified: true,
        },
    });
    await skill.draft.update({
        publishingSettings: {
            organizationId: '2',
            organizationIsVerified: false,
        },
    });
    await skill.reload();

    const res = await callApi('post', `/skills/${skill.id}/candidate`, t.context);
    await skill.draft.reload();

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
    t.is(res.status, 201);
    t.deepEqual(skill.draft.status, 'reviewRequested');
});
