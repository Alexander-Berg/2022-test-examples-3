/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import { match, stub, SinonStub } from 'sinon';
import { createSkill, createUser, wipeDatabase } from '../../_helpers';
import { getUserTicket, testUser } from '../_helpers';
import { callApi, respondsWithError, respondsWithExistingModelContains } from './_helpers';
import * as nlu from '../../../../services/nlu';
import { requestReview, requestDeploy, completeDeploy, stop, markDeleted } from '../../../../services/skill-lifecycle';
import { Channel, SkillAccess } from '../../../../db/tables/settings';

const test = anyTest as TestInterface<{ userTicket: string; inflectStub: SinonStub }>;

test.before(async t => {
    const userTicket = await getUserTicket(testUser.oauthToken);
    const inflectStub = stub(nlu, 'inflect');

    inflectStub.withArgs(match.string, ['FOO']).resolves(['foo']);
    inflectStub.withArgs(match.string, ['FoO']).resolves(['foo']);
    inflectStub.withArgs(match.string, ['часы']).resolves(['часов', 'часы', 'часам', 'часами', 'часах']);
    inflectStub.withArgs(match.string, ['часов']).resolves(['часов', 'часы', 'часам', 'часами', 'часах']);
    inflectStub.returnsArg(1);

    Object.assign(t.context, { userTicket, inflectStub });
});

test.after(async t => {
    t.context.inflectStub.restore();
});

test.beforeEach(async() => {
    await wipeDatabase();
});

test('name uniqueness: consider published skill', async t => {
    await createUser({ id: testUser.uid });

    const origSkill = await createSkill({
        userId: testUser.uid,
        name: 'foo',
    });
    await completeDeploy(origSkill);

    const skill = await createSkill({ userId: testUser.uid });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        name: 'foo',
    });

    respondsWithError(
        {
            code: 400,
            message: 'Validation error',
            fields: {
                name: 'Это название уже зарегистрировано',
            },
        },
        res,
        t,
    );
});

test('name uniqueness: consider deploying draft', async t => {
    const user = await createUser({ id: testUser.uid });

    const origSkill = await createSkill({
        userId: testUser.uid,
        name: 'foo',
    });
    await requestDeploy(origSkill, { user });

    const skill = await createSkill({ userId: testUser.uid });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        name: 'foo',
    });

    respondsWithError(
        {
            code: 400,
            message: 'Validation error',
            fields: {
                name: 'Это название уже зарегистрировано',
            },
        },
        res,
        t,
    );
});

test('name uniqueness: ignore deleted skill', async t => {
    await createUser({ id: testUser.uid });

    const origSkill = await createSkill({
        userId: testUser.uid,
        name: 'foo',
    });
    await completeDeploy(origSkill);
    await markDeleted(origSkill);

    const skill = await createSkill({ userId: testUser.uid });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        name: 'foo',
    });

    respondsWithExistingModelContains(
        {
            name: 'foo',
        },
        res,
        t,
    );
});

test('name uniqueness: ignore stopped skill', async t => {
    const user = await createUser({ id: testUser.uid });

    const origSkill = await createSkill({
        userId: testUser.uid,
        name: 'foo',
    });
    await completeDeploy(origSkill);
    await stop(origSkill, {
        user,
        comment: '',
        notifyFeedbackPlatform: false,
    });

    const skill = await createSkill({ userId: testUser.uid });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        name: 'foo',
    });

    respondsWithExistingModelContains(
        {
            name: 'foo',
        },
        res,
        t,
    );
});

test('name uniqueness: ignore draft other than deploying', async t => {
    const user = await createUser({ id: testUser.uid });

    const origSkill = await createSkill({
        userId: testUser.uid,
        name: 'foo',
    });
    await requestReview(origSkill, { user });

    const skill = await createSkill({ userId: testUser.uid });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        name: 'foo',
    });

    respondsWithExistingModelContains(
        {
            name: 'foo',
        },
        res,
        t,
    );
});

test('name uniqueness: check is case-sensitive', async t => {
    await createUser({ id: testUser.uid });

    const origSkill = await createSkill({
        userId: testUser.uid,
        name: 'foo',
    });
    await completeDeploy(origSkill);

    const skill = await createSkill({ userId: testUser.uid });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        name: 'FOO',
    });

    respondsWithError(
        {
            code: 400,
            message: 'Validation error',
            fields: {
                name: 'Это название уже зарегистрировано',
            },
        },
        res,
        t,
    );
});

test('activation phrases uniqueness: consider published skill', async t => {
    await createUser({ id: testUser.uid });

    const origSkill = await createSkill({
        userId: testUser.uid,
        activationPhrases: ['foo'],
        inflectedActivationPhrases: ['foo'], // Валидация активационного проводится по полю inflectedActivationPhrases
    });
    await completeDeploy(origSkill);

    const skill = await createSkill({ userId: testUser.uid });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        activationPhrases: ['foo'],
    });

    respondsWithError(
        {
            code: 400,
            message: 'Validation error',
            fields: {
                activationPhrases: ['Это активационное имя уже зарегистрировано'],
            },
        },
        res,
        t,
    );
});

test('activation phrases uniqueness: consider deploying draft', async t => {
    const user = await createUser({ id: testUser.uid });

    const origSkill = await createSkill({
        userId: testUser.uid,
        activationPhrases: ['foo'],
        inflectedActivationPhrases: ['foo'],
    });
    await requestDeploy(origSkill, { user });

    const skill = await createSkill({ userId: testUser.uid });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        activationPhrases: ['foo'],
    });

    respondsWithError(
        {
            code: 400,
            message: 'Validation error',
            fields: {
                activationPhrases: ['Это активационное имя уже зарегистрировано'],
            },
        },
        res,
        t,
    );
});

test('activation phrases uniqueness: ignore deleted skill', async t => {
    await createUser({ id: testUser.uid });

    const origSkill = await createSkill({
        userId: testUser.uid,
        activationPhrases: ['foo'],
        inflectedActivationPhrases: ['foo'],
    });
    await completeDeploy(origSkill);
    await markDeleted(origSkill);

    const skill = await createSkill({ userId: testUser.uid });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        activationPhrases: ['foo'],
    });

    respondsWithExistingModelContains(
        {
            activationPhrases: ['foo'],
        },
        res,
        t,
    );
});

test('activation phrases uniqueness: ignore stopped skill', async t => {
    const user = await createUser({ id: testUser.uid });

    const origSkill = await createSkill({
        userId: testUser.uid,
        activationPhrases: ['foo'],
        inflectedActivationPhrases: ['foo'],
    });
    await completeDeploy(origSkill);
    await stop(origSkill, {
        user,
        comment: '',
        notifyFeedbackPlatform: false,
    });

    const skill = await createSkill({ userId: testUser.uid });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        activationPhrases: ['foo'],
    });

    respondsWithExistingModelContains(
        {
            activationPhrases: ['foo'],
        },
        res,
        t,
    );
});

test('activation phrases uniqueness: ignore draft other than deploying', async t => {
    const user = await createUser({ id: testUser.uid });

    const origSkill = await createSkill({
        userId: testUser.uid,
        activationPhrases: ['foo'],
        inflectedActivationPhrases: ['foo'],
    });
    await requestReview(origSkill, { user });

    const skill = await createSkill({ userId: testUser.uid });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        activationPhrases: ['foo'],
    });

    respondsWithExistingModelContains(
        {
            activationPhrases: ['foo'],
        },
        res,
        t,
    );
});

test('activation phrases uniqueness: check is case-insensitive 1', async t => {
    await createUser({ id: testUser.uid });

    const origSkill = await createSkill({
        userId: testUser.uid,
        activationPhrases: ['foo'],
        inflectedActivationPhrases: ['foo'],
    });
    await completeDeploy(origSkill);

    const skill = await createSkill({ userId: testUser.uid });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        activationPhrases: ['FOO'],
    });

    respondsWithError(
        {
            code: 400,
            message: 'Validation error',
            fields: {
                activationPhrases: ['Это активационное имя уже зарегистрировано'],
            },
        },
        res,
        t,
    );
});

test('activation phrases uniqueness: check is case-insensitive 2', async t => {
    await createUser({ id: testUser.uid });

    const origSkill = await createSkill({
        userId: testUser.uid,
        activationPhrases: ['FOO'],
        inflectedActivationPhrases: ['foo'],
    });
    await completeDeploy(origSkill);

    const skill = await createSkill({ userId: testUser.uid });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        activationPhrases: ['foo'],
    });

    respondsWithError(
        {
            code: 400,
            message: 'Validation error',
            fields: {
                activationPhrases: ['Это активационное имя уже зарегистрировано'],
            },
        },
        res,
        t,
    );
});

test('activation phrases uniqueness: check is case-insensitive 3', async t => {
    await createUser({ id: testUser.uid });

    const origSkill = await createSkill({
        userId: testUser.uid,
        activationPhrases: ['fOo'],
        inflectedActivationPhrases: ['foo'],
    });
    await completeDeploy(origSkill);

    const skill = await createSkill({ userId: testUser.uid });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        activationPhrases: ['FoO'],
    });

    respondsWithError(
        {
            code: 400,
            message: 'Validation error',
            fields: {
                activationPhrases: ['Это активационное имя уже зарегистрировано'],
            },
        },
        res,
        t,
    );
});

test('activation phrase uniqueness: consider inflections', async t => {
    await createUser({ id: testUser.uid });

    const origSkill = await createSkill({
        userId: testUser.uid,
        activationPhrases: ['часы'],
        inflectedActivationPhrases: ['часов', 'часы', 'часам', 'часами', 'часах'],
    });
    await completeDeploy(origSkill);

    const skill = await createSkill({ userId: testUser.uid });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        activationPhrases: ['часов'],
    });

    respondsWithError(
        {
            code: 400,
            message: 'Validation error',
            fields: {
                activationPhrases: ['Это активационное имя уже зарегистрировано'],
            },
        },
        res,
        t,
    );
});

test('patchDraft: do not clash activation names public/private', async t => {
    await createUser({ id: testUser.uid });

    const origSkill = await createSkill({
        userId: testUser.uid,
        activationPhrases: ['часы'],
        inflectedActivationPhrases: ['часов', 'часы', 'часам', 'часами', 'часах'],
        skillAccess: SkillAccess.Private,
    });
    await completeDeploy(origSkill);

    const skill = await createSkill({ userId: testUser.uid });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        activationPhrases: ['часов'],
        skillAccess: SkillAccess.Public,
    });

    t.true(res.ok);
});

test('patchDraft: do not clash activation names hidden/private', async t => {
    await createUser({ id: testUser.uid });

    const origSkill = await createSkill({
        userId: testUser.uid,
        activationPhrases: ['часы'],
        inflectedActivationPhrases: ['часов', 'часы', 'часам', 'часами', 'часах'],
        skillAccess: SkillAccess.Private,
    });
    await completeDeploy(origSkill);

    const skill = await createSkill({ userId: testUser.uid, skillAccess: SkillAccess.Hidden });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        activationPhrases: ['часов'],
        skillAccess: SkillAccess.Hidden,
    });

    t.true(res.ok);
});

test("patchDraft: do not clash activation names public/other's private", async t => {
    await createUser({ id: '0001' });

    const origSkill = await createSkill({
        userId: '0001',
        activationPhrases: ['часы'],
        inflectedActivationPhrases: ['часов', 'часы', 'часам', 'часами', 'часах'],
        skillAccess: SkillAccess.Private,
    });
    await completeDeploy(origSkill);

    await createUser({ id: testUser.uid });
    const skill = await createSkill({ userId: testUser.uid });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        activationPhrases: ['часов'],
        skillAccess: SkillAccess.Public,
    });

    t.true(res.ok);
});

test("patchDraft: do not clash activation names hidden/other's private", async t => {
    await createUser({ id: '0001' });

    const origSkill = await createSkill({
        userId: '0001',
        activationPhrases: ['часы'],
        inflectedActivationPhrases: ['часов', 'часы', 'часам', 'часами', 'часах'],
        skillAccess: SkillAccess.Private,
    });
    await completeDeploy(origSkill);

    await createUser({ id: testUser.uid });
    const skill = await createSkill({ userId: testUser.uid, skillAccess: SkillAccess.Hidden });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        activationPhrases: ['часов'],
        skillAccess: SkillAccess.Hidden,
    });

    t.true(res.ok);
});

test("patchDraft: do not clash activation names private/other's private", async t => {
    await createUser({ id: '0001' });

    const origSkill = await createSkill({
        userId: '0001',
        activationPhrases: ['часы'],
        inflectedActivationPhrases: ['часов', 'часы', 'часам', 'часами', 'часах'],
        skillAccess: SkillAccess.Private,
    });
    await completeDeploy(origSkill);

    await createUser({ id: testUser.uid });
    const skill = await createSkill({ userId: testUser.uid, skillAccess: SkillAccess.Private });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        activationPhrases: ['часов'],
        skillAccess: SkillAccess.Private,
    });

    t.true(res.ok);
});

test("patchDraft: do not clash activation names private/other's hidden", async t => {
    await createUser({ id: '0001' });

    const origSkill = await createSkill({
        userId: '0001',
        activationPhrases: ['часы'],
        inflectedActivationPhrases: ['часов', 'часы', 'часам', 'часами', 'часах'],
        skillAccess: SkillAccess.Hidden,
    });
    await completeDeploy(origSkill);

    await createUser({ id: testUser.uid });
    const skill = await createSkill({ userId: testUser.uid, skillAccess: SkillAccess.Private });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        activationPhrases: ['часов'],
        skillAccess: SkillAccess.Private,
    });

    t.true(res.ok);
});

test('patchDraft: clash activation names private/private', async t => {
    await createUser({ id: testUser.uid });

    const origSkill = await createSkill({
        userId: testUser.uid,
        activationPhrases: ['часы'],
        inflectedActivationPhrases: ['часов', 'часы', 'часам', 'часами', 'часах'],
        skillAccess: SkillAccess.Private,
    });
    await completeDeploy(origSkill);

    const skill = await createSkill({ userId: testUser.uid, skillAccess: SkillAccess.Private });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        activationPhrases: ['часов'],
        skillAccess: SkillAccess.Private,
    });

    respondsWithError(
        {
            code: 400,
            message: 'Validation error',
            fields: {
                activationPhrases: ['Это активационное имя уже зарегистрировано'],
            },
        },
        res,
        t,
    );
});

test('patchDraft: not clash activation names private/public', async t => {
    await createUser({ id: testUser.uid });

    const origSkill = await createSkill({
        userId: testUser.uid,
        activationPhrases: ['часы'],
        inflectedActivationPhrases: ['часов', 'часы', 'часам', 'часами', 'часах'],
        skillAccess: SkillAccess.Public,
    });
    await completeDeploy(origSkill);

    const skill = await createSkill({ userId: testUser.uid, skillAccess: SkillAccess.Private });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        activationPhrases: ['часов'],
        skillAccess: SkillAccess.Private,
    });

    t.true(res.ok);
});

test('patchDraft: not clash activation names private/hidden', async t => {
    await createUser({ id: testUser.uid });

    const origSkill = await createSkill({
        userId: testUser.uid,
        activationPhrases: ['часы'],
        inflectedActivationPhrases: ['часов', 'часы', 'часам', 'часами', 'часах'],
        skillAccess: SkillAccess.Hidden,
    });
    await completeDeploy(origSkill);

    const skill = await createSkill({ userId: testUser.uid, skillAccess: SkillAccess.Private });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        activationPhrases: ['часов'],
        skillAccess: SkillAccess.Private,
    });

    t.true(res.ok);
});

test("patchDraft: not clash activation names private/other's public", async t => {
    await createUser({ id: '0001' });

    const origSkill = await createSkill({
        userId: '0001',
        activationPhrases: ['часы'],
        inflectedActivationPhrases: ['часов', 'часы', 'часам', 'часами', 'часах'],
        skillAccess: SkillAccess.Public,
    });
    await completeDeploy(origSkill);

    await createUser({ id: testUser.uid });
    const skill = await createSkill({ userId: testUser.uid, skillAccess: SkillAccess.Private });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        activationPhrases: ['часов'],
        skillAccess: SkillAccess.Private,
    });

    t.true(res.ok);
});

test("patchDraft: not clash activation names private/other's hidden", async t => {
    await createUser({ id: '0001' });

    const origSkill = await createSkill({
        userId: '0001',
        activationPhrases: ['часы'],
        inflectedActivationPhrases: ['часов', 'часы', 'часам', 'часами', 'часах'],
        skillAccess: SkillAccess.Hidden,
    });
    await completeDeploy(origSkill);

    await createUser({ id: testUser.uid });
    const skill = await createSkill({ userId: testUser.uid, skillAccess: SkillAccess.Private });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        activationPhrases: ['часов'],
        skillAccess: SkillAccess.Private,
    });

    t.true(res.ok);
});

test('jivosite id uniqueness: consider published skill', async t => {
    await createUser({ id: testUser.uid });

    const origSkill = await createSkill({
        userId: testUser.uid,
        backendSettings: {
            jivositeId: 'foo',
        },
    });
    await completeDeploy(origSkill);

    const skill = await createSkill({ userId: testUser.uid });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        backendSettings: {
            jivositeId: 'foo',
        },
    });

    respondsWithError(
        {
            code: 400,
            message: 'Validation error',
            fields: {
                jivositeId: 'Чат с таким ID уже зарегистрирован',
            },
        },
        res,
        t,
    );
});

test('jivosite id uniqueness: consider deploying draft', async t => {
    const user = await createUser({ id: testUser.uid });

    const origSkill = await createSkill({
        userId: testUser.uid,
        backendSettings: {
            jivositeId: 'foo',
        },
    });
    await requestDeploy(origSkill, { user });

    const skill = await createSkill({ userId: testUser.uid });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        backendSettings: {
            jivositeId: 'foo',
        },
    });

    respondsWithError(
        {
            code: 400,
            message: 'Validation error',
            fields: {
                jivositeId: 'Чат с таким ID уже зарегистрирован',
            },
        },
        res,
        t,
    );
});

test('jivosite id uniqueness: ignore deleted skill', async t => {
    await createUser({ id: testUser.uid });

    const origSkill = await createSkill({
        userId: testUser.uid,
        backendSettings: {
            jivositeId: 'foo',
        },
    });
    await completeDeploy(origSkill);
    await markDeleted(origSkill);

    const skill = await createSkill({ userId: testUser.uid });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        backendSettings: {
            jivositeId: 'foo',
        },
    });

    respondsWithExistingModelContains(
        {
            backendSettings: {
                jivositeId: 'foo',
                backendType: 'webhook',
            },
        },
        res,
        t,
    );
});

test('jivosite id uniqueness: ignore stopped skill', async t => {
    const user = await createUser({ id: testUser.uid });

    const origSkill = await createSkill({
        userId: testUser.uid,
        backendSettings: {
            jivositeId: 'foo',
        },
    });
    await completeDeploy(origSkill);
    await stop(origSkill, {
        user,
        comment: '',
        notifyFeedbackPlatform: false,
    });

    const skill = await createSkill({ userId: testUser.uid });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        backendSettings: {
            jivositeId: 'foo',
        },
    });

    respondsWithExistingModelContains(
        {
            backendSettings: {
                backendType: 'webhook',
                jivositeId: 'foo',
            },
        },
        res,
        t,
    );
});

test('jivosite id uniqueness: ignore draft other than deploying', async t => {
    const user = await createUser({ id: testUser.uid });

    const origSkill = await createSkill({
        userId: testUser.uid,
        backendSettings: {
            jivositeId: 'foo',
        },
    });
    await requestReview(origSkill, { user });

    const skill = await createSkill({ userId: testUser.uid });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        backendSettings: {
            jivositeId: 'foo',
        },
    });

    respondsWithExistingModelContains(
        {
            backendSettings: {
                jivositeId: 'foo',
                backendType: 'webhook',
            },
        },
        res,
        t,
    );
});

test('jivosite id uniqueness: check is case-sensitive', async t => {
    await createUser({ id: testUser.uid });

    const origSkill = await createSkill({
        userId: testUser.uid,
        backendSettings: {
            jivositeId: 'foo',
        },
    });
    await completeDeploy(origSkill);

    const skill = await createSkill({ userId: testUser.uid });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, t.context).send({
        backendSettings: {
            jivositeId: 'FOO',
        },
    });

    respondsWithError(
        {
            code: 400,
            message: 'Validation error',
            fields: {
                jivositeId: 'Чат с таким ID уже зарегистрирован',
            },
        },
        res,
        t,
    );
});

test('thereminvox name uniqueness: should throw on existing user private skill', async t => {
    await createUser({
        id: testUser.uid,
        featureFlags: {
            allowCreateThereminvoxSkills: true,
        },
    });

    const commonSettings = {
        userId: testUser.uid,
        channel: Channel.Thereminvox,
        hideInStore: true,
        skillAccess: SkillAccess.Private,
    };

    const origSkill = await createSkill({ ...commonSettings, name: 'test' });

    await completeDeploy(origSkill);

    const skill = await createSkill(commonSettings);

    const res = await callApi('patch', `/skills/${skill.id}/draft`, { userTicket: t.context.userTicket }).send({
        name: 'test',
        hideInStore: true,
    });

    respondsWithError(
        {
            code: 400,
            message: 'Validation error',
            fields: {
                name: 'У вас уже есть навык с таким названием',
            },
        },
        res,
        t,
    );
});

test('thereminvox name uniqueness: should not throw on existing user smart home skill', async t => {
    await createUser({
        id: testUser.uid,
        featureFlags: {
            allowCreateThereminvoxSkills: true,
        },
    });

    const commonSettings = {
        userId: testUser.uid,
        channel: Channel.Thereminvox,
        hideInStore: true,
        skillAccess: SkillAccess.Private,
    };

    const origSkill = await createSkill({
        ...commonSettings,
        channel: Channel.SmartHome,
        name: 'test',
    });

    await completeDeploy(origSkill);

    const skill = await createSkill(commonSettings);

    const res = await callApi('patch', `/skills/${skill.id}/draft`, { userTicket: t.context.userTicket }).send({
        name: 'test',
        hideInStore: true,
    });

    t.true(res.ok);
});

test('thereminvox name uniqueness: should not throw on existing user chat', async t => {
    await createUser({
        id: testUser.uid,
        featureFlags: {
            allowCreateThereminvoxSkills: true,
        },
    });

    const commonSettings = {
        userId: testUser.uid,
        channel: Channel.Thereminvox,
        hideInStore: true,
        skillAccess: SkillAccess.Private,
    };

    const origSkill = await createSkill({
        ...commonSettings,
        channel: Channel.OrganizationChat,
        name: 'test',
    });

    await completeDeploy(origSkill);

    const skill = await createSkill(commonSettings);

    const res = await callApi('patch', `/skills/${skill.id}/draft`, { userTicket: t.context.userTicket }).send({
        name: 'test',
        hideInStore: true,
    });

    t.true(res.ok);
});

test('thereminvox name uniqueness: should throw on existing user public skill', async t => {
    await createUser({
        id: testUser.uid,
        featureFlags: {
            allowCreateThereminvoxSkills: true,
        },
    });

    const commonSettings = {
        userId: testUser.uid,
        channel: Channel.Thereminvox,
        hideInStore: true,
        skillAccess: SkillAccess.Private,
    };

    const origSkill = await createSkill({
        ...commonSettings,
        name: 'test',
        hideInStore: false,
        skillAccess: SkillAccess.Public,
    });

    await completeDeploy(origSkill);

    const skill = await createSkill(commonSettings);

    const res = await callApi('patch', `/skills/${skill.id}/draft`, { userTicket: t.context.userTicket }).send({
        name: 'test',
        hideInStore: true,
    });

    respondsWithError(
        {
            code: 400,
            message: 'Validation error',
            fields: {
                name: 'У вас уже есть навык с таким названием',
            },
        },
        res,
        t,
    );
});

test('thereminvox name uniqueness: should not throw on someone elses skill', async t => {
    await createUser({
        id: testUser.uid,
        featureFlags: {
            allowCreateThereminvoxSkills: true,
        },
    });

    await createUser({
        id: '1234',
        featureFlags: {
            allowCreateThereminvoxSkills: true,
        },
    });

    const origSkill = await createSkill({
        channel: Channel.Thereminvox,
        name: 'test',
        userId: '1234',
    });

    await completeDeploy(origSkill);

    const skill = await createSkill({ channel: Channel.Thereminvox, hideInStore: true, userId: testUser.uid });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, { userTicket: t.context.userTicket }).send({
        name: 'test',
        hideInStore: true,
    });

    t.true(res.ok);
});

// Fixme unskip when tehreminvox skills went public
test.skip('thereminvox name uniqueness: should not throw on someone elses private skill', async t => {
    await createUser({
        id: testUser.uid,
        featureFlags: {
            allowCreateThereminvoxSkills: true,
        },
    });

    await createUser({
        id: '1234',
        featureFlags: {
            allowCreateThereminvoxSkills: true,
        },
    });

    const origSkill = await createSkill({
        channel: Channel.Thereminvox,
        name: 'test',
        userId: '1234',
        hideInStore: true,
    });

    await completeDeploy(origSkill);

    const skill = await createSkill({ channel: Channel.Thereminvox, userId: testUser.uid });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, { userTicket: t.context.userTicket }).send({
        name: 'test',
    });

    t.true(res.ok);
});

test.skip('thereminvox name uniqueness: should throw on someone elses public skill', async t => {
    await createUser({
        id: testUser.uid,
        featureFlags: {
            allowCreateThereminvoxSkills: true,
        },
    });

    await createUser({
        id: '1234',
        featureFlags: {
            allowCreateThereminvoxSkills: true,
        },
    });

    const origSkill = await createSkill({
        channel: Channel.Thereminvox,
        name: 'test',
        userId: '1234',
        hideInStore: false,
    });

    await completeDeploy(origSkill);

    const skill = await createSkill({ channel: Channel.Thereminvox, userId: testUser.uid, hideInStore: false });

    const res = await callApi('patch', `/skills/${skill.id}/draft`, { userTicket: t.context.userTicket }).send({
        name: 'test',
    });

    respondsWithError(
        {
            code: 400,
            message: 'Validation error',
            fields: {
                name: 'Навык с таким названием уже зарегистрирован',
            },
        },
        res,
        t,
    );
});
