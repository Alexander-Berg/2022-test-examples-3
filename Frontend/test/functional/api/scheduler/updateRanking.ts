/* eslint-disable */
import anyTest, { TestInterface } from 'ava';
import * as sinon from 'sinon';
import { sequelize, Skill } from '../../../../db';
import config from '../../../../services/config';
import { bulkCreateSkill, createSkill, createUser, wipeDatabase } from '../../_helpers';
import { callApi } from './_helpers';

const test = anyTest as TestInterface<{ clock: sinon.SinonFakeTimers }>;

test.beforeEach(async t => {
    await wipeDatabase();
});

test('update on empty database', async t => {
    const res = await callApi('skills/ranking/update', { ranks: [] });
    t.deepEqual(res.body, {});
    t.truthy(res.status === 200);
});

test('skill id', async t => {
    const ranks = [{ skillId: '3078694a-3256-4a3f-841f-6e96d260f9ed', rank: 100 }];
    const res = await callApi('skills/ranking/update', { ranks });
    t.truthy(res.status === 200);
});

test('update set catalogRank field', async t => {
    await createUser();
    const skill = await createSkill();
    const ranks = [{ skillId: skill.id, rank: 999 }];
    const res = await callApi('skills/ranking/update', { ranks });
    t.truthy(res.status === 200);
    await skill.reload();
    t.truthy(skill.catalogRank === 999);
});

test("invalid UUID in skillId does't affect other skills", async t => {
    await createUser();
    const skill = await createSkill();
    const ranks = [{ skillId: skill.id, rank: 100 }, { skillId: 'notauuid', rank: 200 }];
    const res = await callApi('skills/ranking/update', { ranks });
    t.truthy(res.status === 200);
    await skill.reload();
    t.truthy(skill.catalogRank === 100);
});

test.skip('update 10k skills', async t => {
    await createUser();
    const skills = await bulkCreateSkill(10000);
    // 150 мс достаточно для простых запросов, во всех сложных должен использоваться config.app.db.longQueryTimeout
    (sequelize.options.retry as any).timeout = 150;
    const ranks = skills.map((skill, index) => {
        return { skillId: skill.id, rank: index };
    });
    const res = await callApi('skills/ranking/update', { ranks });
    t.truthy(res.status === 200);
    (sequelize.options.retry as any).timeout = 5000;
    const updatedSkills = await Skill.findAll();
    for (const skill of updatedSkills) {
        t.truthy(skill.catalogRank !== null);
        t.truthy(Number.isInteger(skill.catalogRank!));
    }
});

test('timeoutError causes http code 500', async t => {
    const entities = require('../../../../db/entities');
    const stub = sinon.stub(entities, 'updateSkillsTableColumn').throws(new sequelize.TimeoutError('Query timed out'));
    const res = await callApi('skills/ranking/update', { ranks: [] });
    t.truthy(res.status === 500);
    stub.restore();
});

test('small longQueryTimeout causes http code 500', async t => {
    await createUser();
    const skills = await bulkCreateSkill(100);
    config.app.db.longQueryTimeout = 1;
    (sequelize.options.retry as any).timeout = 50000;
    const ranks = skills.map((skill, index) => {
        return { skillId: skill.id, rank: index };
    });
    const res = await callApi('skills/ranking/update', { ranks });
    t.truthy(res.status === 500);
});

test.skip('transaction is passed to each query', async t => {
    await createUser();
    const skill = await createSkill();
    const ranks = [{ skillId: skill.id, rank: 1 }];
    const db = require('../../../db');
    const stub = sinon.stub(db.sequelize, 'query').resolves();

    const res = await callApi('skills/ranking/update', { ranks });

    t.truthy(res.status === 200);
    t.truthy(stub.callCount > 0);
    stub.getCalls().map((call: any) => {
        t.truthy(call.args[1].transaction !== undefined);
    });

    stub.restore();
});
