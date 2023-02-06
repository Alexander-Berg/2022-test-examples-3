/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import { callApi, respondsWithError, createStoreSkillWithLogo } from './_helpers';
import { wipeDatabase, createUser } from '../../_helpers';
import { getUserTicket, testUser } from '../_helpers';

const test = anyTest as TestInterface<{ userTicket: string }>;

test.before(async t => {
    const userTicket = await getUserTicket(testUser.oauthToken);

    Object.assign(t.context, { userTicket });
});

test.beforeEach(async() => {
    await wipeDatabase();
});

const route = '/personal/favourites';

test('favouriteSkills: should mark skill as favourite', async t => {
    await createUser({ id: testUser.uid });
    const skill = await createStoreSkillWithLogo({ publishingSettings: {}, backendSettings: {} });

    await callApi('post', route, { userTicket: t.context.userTicket }).send({ skillId: skill.id });

    const res = await callApi('get', route, { userTicket: t.context.userTicket });

    t.deepEqual(res.body.result.map(({ id }: any) => id), [skill.id]);
});

test('favouriteSkills: should mark several skills as favourite', async t => {
    await createUser({ id: testUser.uid });

    const skill1 = await createStoreSkillWithLogo();
    const skill2 = await createStoreSkillWithLogo();
    const skill3 = await createStoreSkillWithLogo();

    await callApi('post', route, { userTicket: t.context.userTicket }).send({ skillId: skill1.id });
    await callApi('post', route, { userTicket: t.context.userTicket }).send({ skillId: skill2.id });
    await callApi('post', route, { userTicket: t.context.userTicket }).send({ skillId: skill3.id });

    const res = await callApi('get', route, { userTicket: t.context.userTicket });

    t.deepEqual(res.body.result.map(({ id }: any) => id), [skill3.id, skill2.id, skill1.id]);
});

test('favouriteSkills: should remove favourite skill', async t => {
    await createUser({ id: testUser.uid });

    const skill = await createStoreSkillWithLogo();

    await callApi('post', route, { userTicket: t.context.userTicket }).send({ skillId: skill.id });

    let res = await callApi('get', route, { userTicket: t.context.userTicket });

    t.deepEqual(res.body.result.map(({ id }: any) => id), [skill.id]);

    await callApi('delete', route + '/' + skill.id, { userTicket: t.context.userTicket });

    res = await callApi('get', route, { userTicket: t.context.userTicket });

    t.is(res.body.result.length, 0);
});

test('favouriteSkills: should not mark same skill as favourite', async t => {
    await createUser({ id: testUser.uid });

    const skill = await createStoreSkillWithLogo();

    await callApi('post', route, { userTicket: t.context.userTicket }).send({ skillId: skill.id });

    const res = await callApi('post', route, { userTicket: t.context.userTicket }).send({ skillId: skill.id });

    respondsWithError(409, 'Skill already marked as favourite', res, t);
});

test('favouriteSkills: should send 400 when skillId is not specified', async t => {
    const res = await callApi('post', route, { userTicket: t.context.userTicket });

    respondsWithError(400, 'Missing skill id', res, t);
});

test('favouriteSkills: should send 400 when skillId is not valid', async t => {
    const res = await callApi('post', route, { userTicket: t.context.userTicket }).send({ skillId: '1234' });

    respondsWithError(400, 'Invalid skill id', res, t);
});
