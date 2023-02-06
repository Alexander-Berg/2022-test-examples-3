/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import * as sinon from 'sinon';
import { getUserTicket, testUser } from '../../_helpers';
import { createSkill, createUser, wipeDatabase } from '../../../_helpers';
import { callApi } from '../_helpers';
import { DraftIntent } from '../../../../../db';
import * as granet from '../../../../../services/granet';

const test = anyTest as TestInterface<{ userTicket: string }>;

test.before(async t => {
    const userTicket = await getUserTicket(testUser.oauthToken);

    Object.assign(t.context, { userTicket });
});

test.beforeEach(async() => {
    await wipeDatabase();
    sinon.stub(granet, 'compileGrammars').value(async() => {
        return {
            grammar_base64: 'base64',
            true_positives: [],
            true_negatives: [],
            false_positives: [],
            false_negatives: [],
        };
    });
});

test.afterEach(() => {
    sinon.restore();
});

test('Edit intent', async t => {
    const data = {
        formName: 'paskills.formName',
        sourceText: 'source',
        humanReadableName: 'имя',
        positiveTests: ['one', 'two'],
        negativeTests: ['three', 'four', 'five'],
    };
    const user = await createUser({ id: testUser.uid });
    const skill = await createSkill({ userId: user.id });
    const intent = await DraftIntent.create({
        skillId: skill.id,
        ...data,
    });
    const res = await callApi(
        'patch',
        `/skills/${skill.id}/intents/${intent.id}/draft`,
        t.context,
    ).send({
        formName: 'paskills.formName.2',
    });
    t.is(res.status, 200);

    t.deepEqual(res.body.result, {
        intent: {
            id: intent.id,
            formName: 'paskills.formName.2',
            isActivation: false,
            humanReadableName: 'имя',
            sourceText: 'source',
            positiveTests: 'one\ntwo',
            negativeTests: 'three\nfour\nfive',
            status: 'OK',
        },
        validationError: null,
    });

    await intent.reload();
    t.is(intent.skillId, skill.id);
    t.is(intent.formName, 'paskills.formName.2');
    t.is(intent.sourceText, data.sourceText);
    t.deepEqual(intent.positiveTests, ['one', 'two']);
    t.deepEqual(intent.negativeTests, ['three', 'four', 'five']);
});

test('Empty negative tests string is treated as empty array', async t => {
    const data = {
        formName: 'paskills.formName',
        sourceText: 'source',
        humanReadableName: 'имя',
        positiveTests: ['one', 'two'],
        negativeTests: ['three', 'four', 'five'],
    };
    const user = await createUser({ id: testUser.uid });
    const skill = await createSkill({ userId: user.id });
    const intent = await DraftIntent.create({
        skillId: skill.id,
        ...data,
    });
    const res = await callApi(
        'patch',
        `/skills/${skill.id}/intents/${intent.id}/draft`,
        t.context,
    ).send({
        formName: 'paskills.formName',
        humanReadableName: 'имя',
        negativeTests: '\n\n',
        positiveTests: 'one\ntwo',
        sourceText: 'source\n',
    });
    t.is(res.status, 200);

    t.deepEqual(res.body.result, {
        intent: {
            id: intent.id,
            formName: 'paskills.formName',
            humanReadableName: 'имя',
            sourceText: 'source\n',
            positiveTests: 'one\ntwo',
            negativeTests: '',
            status: 'OK',
            isActivation: false,
        },
        validationError: null,
    });

    await intent.reload();
    t.is(intent.skillId, skill.id);
    t.is(intent.formName, 'paskills.formName');
    t.is(intent.sourceText, data.sourceText + '\n');
    t.deepEqual(intent.positiveTests, ['one', 'two']);
    t.deepEqual(intent.negativeTests, []);
});

test("Cannot set formName equal to existing intent's", async t => {
    const user = await createUser({ id: testUser.uid });
    const skill = await createSkill({ userId: user.id });
    const intent1 = await DraftIntent.create({
        skillId: skill.id,
        formName: 'paskills.formName',
        sourceText: 'source',
        positiveTests: ['one', 'two'],
        negativeTests: ['three', 'four', 'five'],
    });
    await DraftIntent.create({
        skillId: skill.id,
        formName: 'paskills.formName.2',
        sourceText: 'source',
        positiveTests: ['one', 'two'],
        negativeTests: ['three', 'four', 'five'],
    });
    const res = await callApi(
        'patch',
        `/skills/${skill.id}/intents/${intent1.id}/draft`,
        t.context,
    ).send({
        formName: 'paskills.formName.2',
    });
    t.is(res.status, 400);

    await intent1.reload();
    t.is(intent1.skillId, skill.id);
    t.is(intent1.formName, 'paskills.formName');
    t.is(intent1.sourceText, 'source');
    t.deepEqual(intent1.positiveTests, ['one', 'two']);
    t.deepEqual(intent1.negativeTests, ['three', 'four', 'five']);
});
