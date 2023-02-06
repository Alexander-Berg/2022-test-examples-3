/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import { getUserTicket, testUser } from '../../_helpers';
import { createSkill, createUser, wipeDatabase } from '../../../_helpers';
import { callApi } from '../_helpers';
import { DraftIntent } from '../../../../../db';

const test = anyTest as TestInterface<{ userTicket: string }>;

test.before(async t => {
    const userTicket = await getUserTicket(testUser.oauthToken);

    Object.assign(t.context, { userTicket });
});

test.beforeEach(async() => {
    await wipeDatabase();
});

test('Create empty intent', async t => {
    const user = await createUser({ id: testUser.uid });
    const skill = await createSkill({ userId: user.id });
    const res = await callApi('post', `/skills/${skill.id}/intents/drafts`, t.context).send({});
    t.is(res.status, 201);
    const intentId = res.body.result.id;
    const intent = (await DraftIntent.findByPk(intentId))!;
    t.is(intent.skillId, skill.id);
});

test('Create intent with content', async t => {
    const data = {
        formName: 'paskills.formName',
        humanReadableName: 'имя',
        sourceText: 'source',
        positiveTests: 'one\ntwo',
        negativeTests: 'three\nfour\nfive',
    };
    const user = await createUser({ id: testUser.uid });
    const skill = await createSkill({ userId: user.id });
    const res = await callApi('post', `/skills/${skill.id}/intents/drafts`, t.context).send(data);
    t.is(res.status, 201);
    const intentId = res.body.result.id;
    const intent = (await DraftIntent.findByPk(intentId))!;
    t.is(intent.skillId, skill.id);
    t.is(intent.formName, data.formName);
    t.is(intent.sourceText, data.sourceText + '\n');
    t.is(intent.humanReadableName, data.humanReadableName);
    t.deepEqual(intent.positiveTests, ['one', 'two']);
    t.deepEqual(intent.negativeTests, ['three', 'four', 'five']);
});

test('Create intent without tests', async t => {
    const data = {
        formName: 'paskills.formName',
        humanReadableName: 'имя',
        sourceText: 'source',
        positiveTests: '',
        negativeTests: '',
    };
    const user = await createUser({ id: testUser.uid });
    const skill = await createSkill({ userId: user.id });
    const res = await callApi('post', `/skills/${skill.id}/intents/drafts`, t.context).send(data);
    t.is(res.status, 201);
    const intentId = res.body.result.id;
    const intent = (await DraftIntent.findByPk(intentId))!;
    t.is(intent.skillId, skill.id);
    t.is(intent.formName, data.formName);
    t.is(intent.sourceText, data.sourceText + '\n');
    t.is(intent.humanReadableName, data.humanReadableName);
    t.deepEqual(intent.positiveTests, []);
    t.deepEqual(intent.negativeTests, []);
});

test('Cannot create two intents with same form_name', async t => {
    const data = {
        formName: 'paskills.formName',
    };
    const user = await createUser({ id: testUser.uid });
    const skill = await createSkill({ userId: user.id });
    // create first intent
    await DraftIntent.create({
        skillId: skill.id,
        formName: data.formName,
    });
    const res = await callApi('post', `/skills/${skill.id}/intents/drafts`, t.context).send(data);
    t.is(res.status, 400);
});
