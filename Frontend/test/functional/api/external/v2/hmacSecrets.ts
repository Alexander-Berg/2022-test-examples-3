/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import { createSkill, createUser, wipeDatabase } from '../../../_helpers';
import { getServiceTicket, testUser } from '../../_helpers';
import { SkillInstance } from '../../../../../db/tables/skill';
import { UserInstance } from '../../../../../db/tables/user';
import { callApi } from './_helpers';
import { PublishedHmacSecret } from '../../../../../db';

interface TestContext {
    serviceTicket: string;
    skill: SkillInstance;
    user: UserInstance;
}

interface SerializedSecret {
    value: string;
}

const test = anyTest as TestInterface<TestContext>;

test.before(async t => {
    const serviceTicket = await getServiceTicket();
    
    Object.assign(t.context, { serviceTicket });
});

test.beforeEach(async t => {
    await wipeDatabase();
    t.context.user = await createUser({ id: testUser.uid });
    t.context.skill = await createSkill({
        onAir: true,
        userId: t.context.user.id,
    });
});

test('get empty secret list', async (t) => {
    const response = await callApi('get', `/skills/${t.context.skill.id}/hmac-secrets`, {
        serviceTicket: t.context.serviceTicket,
    });
    t.is(response.status, 200);
    t.deepEqual(response.body.secrets, []);
});

test('get two secrets', async (t) => {
    await PublishedHmacSecret.create({
        skillId: t.context.skill.id,
        valueBase64: 'A',
    });
    await PublishedHmacSecret.create({
        skillId: t.context.skill.id,
        valueBase64: 'B',
    });
    const response = await callApi('get', `/skills/${t.context.skill.id}/hmac-secrets`, {
        serviceTicket: t.context.serviceTicket,
    });
    t.is(response.status, 200);
    t.is(response.body.secrets.length, 2);
    const values: string[] = response.body.secrets.map((s: SerializedSecret) => s.value);
    values.sort();
    t.deepEqual(values, ['A', 'B']);
});
