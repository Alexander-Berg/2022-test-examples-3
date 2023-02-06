/* eslint-disable */
import test from 'ava';
import { wipeDatabase, createUser, createSkill } from '../../_helpers';
import { isSkillBelongsToUser } from '../../../../db/entities/users';

test.beforeEach(wipeDatabase);

test('isSkillBelongsToUser: returns true', async t => {
    const user = await createUser();
    const skill = await createSkill({ userId: user.id });

    t.true(await isSkillBelongsToUser({ userId: user.id, skillId: skill.id }));
});

test('isSkillBelongsToUser: returns false', async t => {
    const user = await createUser();

    await createUser({ id: '1234' });
    const skill = await createSkill({ userId: '1234' });

    t.false(await isSkillBelongsToUser({ userId: user.id, skillId: skill.id }));
});
