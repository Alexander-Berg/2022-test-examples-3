/* eslint-disable */
import anyTest, {TestInterface} from 'ava';
import {OneTimeShare, SkillUserShare} from '../../../../db';
import {createSkill, createUser, wipeDatabase} from '../../_helpers';
import {getUserTicket, testUser} from '../_helpers';
import {callApi, respondsWithCreatedModel, respondsWithExistingModel} from './_helpers';
import {generatePublicShareKey} from '../../../../utils/shares';

const test = anyTest as TestInterface<{ userTicket: string }>;

test.before(async t => {
    const userTicket = await getUserTicket(testUser.oauthToken);

    Object.assign(t.context, {userTicket});
});

test.beforeEach(async () => {
    await wipeDatabase();
});

test('Create skill public share', async t => {
    await createUser({id: testUser.uid});
    const skill = await createSkill({userId: testUser.uid});

    t.is(skill.publicShareKey, null);

    const res = await callApi('post', `/skills/${skill.id}/shares/public`, t.context);

    await skill.reload();

    t.is(typeof skill.publicShareKey, 'string');
    respondsWithCreatedModel(skill.publicShareKey, res, t);
});

test('Update skill public share', async t => {
    await createUser({id: testUser.uid});
    const initialPublicShareKey = generatePublicShareKey();
    const skill = await createSkill({userId: testUser.uid, publicShareKey: initialPublicShareKey});

    t.is(skill.publicShareKey, initialPublicShareKey);

    const res = await callApi('post', `/skills/${skill.id}/shares/public`, t.context);

    await skill.reload();

    t.is(typeof skill.publicShareKey, 'string');
    t.not(skill.publicShareKey, initialPublicShareKey);
    respondsWithCreatedModel(skill.publicShareKey, res, t);
});

test('Create skill onetime share', async t => {
    await createUser({id: testUser.uid});
    const skill = await createSkill({userId: testUser.uid});

    const res = await callApi('post', `/skills/${skill.id}/shares`, t.context);

    const oneTimeShares = await OneTimeShare.findAll({
        where: {
            skillId: skill.id,
        },
    });

    t.is(oneTimeShares.length, 1);
    respondsWithCreatedModel(oneTimeShares[0].id, res, t);
});

test('Get skill user shares', async t => {
    await createUser({id: testUser.uid});
    await createUser({id: '0001', name: 'user1'});
    await createUser({id: '0002', name: 'user2'});
    const skill = await createSkill({userId: testUser.uid});

    await SkillUserShare.bulkCreate([
        {skill_id: skill.id, user_id: '0001'},
        {skill_id: skill.id, user_id: '0002'},
    ]);

    const res = await callApi('get', `/skills/${skill.id}/user-shares`, t.context);

    respondsWithExistingModel(
        [
            {uid: '0001', login: 'user1'},
            {uid: '0002', login: 'user2'},
        ],
        res,
        t,
    );
});

test('Delete skill user share', async t => {
    await createUser({id: testUser.uid});
    await createUser({id: '0001', name: 'user1'});
    const skill = await createSkill({userId: testUser.uid});

    await SkillUserShare.bulkCreate([{skill_id: skill.id, user_id: '0001'}]);

    const res = await callApi('delete', `/skills/${skill.id}/user-shares/0001`, t.context);
    const userShares = await SkillUserShare.findAll({
        where: {
            skill_id: skill.id,
            user_id: '0001',
        },
    });

    t.is(userShares.length, 0);
    respondsWithExistingModel('ok', res, t);
});
