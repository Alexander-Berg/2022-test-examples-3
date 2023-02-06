/* eslint-disable */
import test from 'ava';
import * as sinon from 'sinon';
import { sequelize, Skill, Sequelize } from '../../../db';
import configureMigrationUtils from '../../../db/migrationUtils';
import { createSkill, createUser, wipeDatabase } from '../_helpers';

const tableName = Skill.getTableName() as string;

const queryInterface = sequelize.getQueryInterface();
const migrationUtils = configureMigrationUtils(queryInterface, Sequelize);
const { batchFillDefaultValue, updateBatch } = migrationUtils;

test.beforeEach(async t => {
    await wipeDatabase();
});

/**
 * Для удобства разработки тесты завязаны на skills.catalogRank как на nullable поле.
 * Тестируемый модуль поддерживает произвольную таблицу и колонку в ней.
 */

test('batchFillDefaultValue() on empty database does not update anything', async t => {
    const mockQuery = sinon.stub(migrationUtils, 'updateBatch');

    await batchFillDefaultValue(tableName, 'catalogRank', 1, 1000, 10);
    t.truthy(mockQuery.callCount === 0);

    mockQuery.restore();
});

test('batchFillDefaultValue() updates only null values', async t => {
    await createUser();
    const skill1 = await createSkill({ catalogRank: 10 });
    const skill2 = await createSkill({ catalogRank: null });

    await batchFillDefaultValue(tableName, 'catalogRank', 1, 1000, 10);
    await skill1.reload();
    await skill2.reload();

    t.truthy(skill1.catalogRank === 10);
    t.truthy(skill2.catalogRank === 1); // значение изменилось на defaultValue
});

test('batchFillDefaultValue() updates deleted skills', async t => {
    await createUser();
    const skill = await createSkill();
    skill.catalogRank = null;
    skill.deletedAt = new Date();
    await skill.save();

    await batchFillDefaultValue(tableName, 'catalogRank', 1, 1000, 10);

    await skill.reload();
    t.truthy(skill.catalogRank === 1);
});

test('update multiple skills', async t => {
    await createUser();
    await createSkill({ catalogRank: null });
    await createSkill({ catalogRank: null });

    await batchFillDefaultValue(tableName, 'catalogRank', 1, 1000, 10);

    const nullCount = await Skill.count({
        where: {
            catalogRank: null,
        } as any,
    });

    t.truthy(nullCount === 0);
});

test('updateBatch() updates exactly batchSize records at a time', async t => {
    await createUser();
    const skill1 = await createSkill({ catalogRank: null });
    const skill2 = await createSkill({ catalogRank: null });
    t.truthy(1 === 1);

    await updateBatch(Skill.getTableName() as string, 'catalogRank', 1, 1, true);
    await skill1.reload();
    await skill2.reload();

    t.truthy(
        (skill1.catalogRank === null && skill2.catalogRank === 1) ||
            (skill1.catalogRank === 1 && skill2.catalogRank === null),
    );
});
