const assert = require('assert');
const _ = require('lodash');
const catchErrorAsync = require('catch-error-async');

const { DbType } = require('db/constants');
const factory = require('tests/db/factory');
const cleanDb = require('tests/db/clean');
const Group = require('models/group');
const databases = require('db/databases');
const db = require('db');

const { Op } = db.sequelize;
const testDbType = DbType.internal;

describe('Group model', () => {
    beforeEach(cleanDb);

    describe('findOne', () => {
        it('should find a group', async() => {
            await factory.group.create({ id: 11, slug: 'php', name: 'php' });

            const actual = await Group.findOne({ id: 11, dbType: testDbType });

            assert.equal(actual.slug, 'php');
        });

        it('should find a group by slug', async() => {
            await factory.group.create({ id: 11, slug: 'php', name: 'php' });

            const actual = await Group.findOne({ slug: 'php', dbType: testDbType });

            assert.equal(actual.slug, 'php');
            assert.equal(actual.id, 11);
        });

        it('should throw if group is not found', async() => {
            const error = await catchErrorAsync(
                Group.findOne.bind(Group), { id: 12, scope: 'one', dbType: testDbType },
            );

            assert.equal(error.message, 'Group not found');
            assert.equal(error.statusCode, 404);
            assert.deepEqual(error.options, {
                internalCode: '404_ENF',
                id: 12,
                scope: 'one',
                dbType: testDbType,
            });
        });
    });

    describe('count', () => {
        it('should count all groups', async() => {
            const groups = _.range(1, 26)
                .map(num => ({ id: num, slug: `#${num}` }));

            await factory.group.create(groups);

            const actual = await Group.count({ scope: 'page', dbType: testDbType });

            assert.equal(actual, 25);
        });
    });

    describe('findPage', () => {
        it('should find groups with limit and offset', async() => {
            const groups = _.range(1, 26)
                .map(num => ({ id: num, slug: `#${num}` }));

            await factory.group.create(groups);

            const actual = await Group.findPage(
                { scope: 'page', pageSize: 3, pageNumber: 3, dbType: testDbType });

            assert.equal(actual.rows.length, 3);
            assert.equal(actual.rows[0].slug, '#19');
            assert.equal(actual.rows[1].slug, '#18');
            assert.equal(actual.rows[2].slug, '#17');
            assert.equal(actual.meta.totalSize, 25);
            assert.equal(actual.meta.pageNumber, 3);
            assert.equal(actual.meta.pageSize, 3);
        });

        it('should find groups filtered by condition', async() => {
            const filterParams = {
                and: [
                    {
                        type: 'string',
                        name: 'slug',
                        value: '3',
                        compare: 'cont',
                    },
                ],
            };
            const groups = _.range(1, 26)
                .map(num => ({ id: num, slug: `#${num}` }));

            await factory.group.create(groups);

            const actual = await Group.findPage({
                scope: 'page', pageSize: 10, pageNumber: 1, filterParams, dbType: testDbType,
            });

            assert.equal(actual.rows.length, 3);
            assert.equal(actual.rows[0].slug, '#23');
            assert.equal(actual.rows[1].slug, '#13');
            assert.equal(actual.rows[2].slug, '#3');
            assert.equal(actual.meta.totalSize, 3);
            assert.equal(actual.meta.pageNumber, 1);
            assert.equal(actual.meta.pageSize, 10);
        });
    });

    describe('create', () => {
        it('should create a new group', async() => {
            const group = new Group({
                slug: 'php',
                name: 'php',
            }, { dbType: testDbType });

            const groupId = await group.create({ dbType: testDbType });

            const actual = await databases[testDbType].groups.findAll();

            assert.ok(/\d/.test(groupId));
            assert.equal(actual.length, 1);
            assert.equal(actual[0].slug, 'php');
        });

        it('should throw if a slug already exists', async() => {
            await factory.group.create({ id: 12, slug: 'php' });

            const group = new Group({
                slug: 'php',
                name: 'php',
            }, { dbType: testDbType });

            const error = await catchErrorAsync(
                group.create.bind(group, { dbType: testDbType }),
            );

            assert.equal(error.message, 'Group already exists');
            assert.equal(error.statusCode, 409);
            assert.deepEqual(error.options, {
                internalCode: '409_EAE',
                slug: 'php',
            });
        });
    });

    describe('patch', () => {
        it('should patch an existing group', async() => {
            await factory.group.create({
                id: 13,
                slug: 'old-group',
                name: 'old group',
            });

            const data = {
                id: 13,
                name: 'new group',
            };

            const group = new Group(data, { dbType: testDbType });
            const groupId = await group.patch({ yandexuid: '1', dbType: testDbType });
            const actual = await databases[testDbType].groups.findAll({ dbType: testDbType });

            assert.ok(/\d/.test(groupId));
            assert.equal(actual.length, 1);
            assert.equal(actual[0].name, 'new group');
        });

        it('should not patch other groups', async() => {
            await factory.group.create([{
                id: 13,
                slug: 'old-group',
                name: 'old group',
            }, {
                id: 15,
                slug: 'one-more-event',
                name: 'old more group',
            }]);

            const data = {
                id: 13,
                name: 'new group',
            };

            const group = new Group(data, { dbType: testDbType });
            const groupId = await group.patch({ yandexuid: '1', dbType: testDbType });
            const actual = await databases[testDbType].groups.findAll({ order: [['id']] });

            assert.ok(/\d/.test(groupId));
            assert.equal(actual.length, 2);
            assert.equal(actual[0].name, 'new group');
            assert.equal(actual[1].name, 'old more group');
        });

        it('should throw on nonexistent group', async() => {
            const group = new Group({ id: 13 }, { dbType: testDbType });
            const error = await catchErrorAsync(
                group.patch.bind(group, { dbType: testDbType }),
            );

            assert.equal(error.message, 'Group not found');
            assert.equal(error.statusCode, 404);
            assert.deepEqual(error.options, {
                internalCode: '404_ENF',
                id: 13,
            });
        });
    });

    describe('destroy', () => {
        it('should destroy a group', async() => {
            await factory.group.create({ id: 11, slug: 'php', name: 'php' });

            const destroyed = await Group.destroy(11, { dbType: testDbType });
            const groups = await Group.findPage(
                { scope: 'page', pageSize: 10, pageNumber: 1, dbType: testDbType });

            assert.equal(destroyed, 1);
            assert.equal(groups.meta.totalSize, 0);
        });

        it('should throw if group have relations with events', async() => {
            const eventsData = [
                { id: 1, slug: 'codefest', title: 'codefest' },
                { id: 2, slug: 'highload', title: 'highload' },
                { id: 3, slug: 'holyjs', title: 'holyjs' },
            ];

            const events = await factory.event.create(eventsData);
            const group = await factory.group.create({
                id: 11,
                slug: 'javascript',
                name: 'javascript',
            });

            await group.addEvents(events);

            /**
             * После добавления к группе событий проверяем, чтобы в таблице-связке
             * появились соответствующие записи
             */
            const eventsGroup = await databases[testDbType].eventsGroup.findAll({ where: { groupId: 11 } });

            assert.equal(eventsGroup.length, 3);

            const error = await catchErrorAsync(Group.destroy.bind(Group), 11, { dbType: testDbType });

            assert.equal(error.message, 'Need remove group from events');
            assert.equal(error.statusCode, 400);
            assert.deepEqual(error.options, {
                internalCode: '400_DGE',
                eventIds: [1, 2, 3],
            });
        });

        it('should throw if exist group idm role', async() => {
            await factory.userRole.create({ login: 'yoda', role: 'groupManager', eventGroupId: { id: 11 } });

            const error = await catchErrorAsync(Group.destroy.bind(Group), 11, { dbType: testDbType });

            assert.equal(error.message, 'Need remove idm role before remove group. Role count: 1');
            assert.equal(error.statusCode, 400);
            assert.deepEqual(error.options, {
                internalCode: '400_DGE',
            });
        });

        it('should throw if group is not found', async() => {
            const error = await catchErrorAsync(
                Group.destroy.bind(Group), 11, { dbType: testDbType },
            );

            assert.equal(error.message, 'Group not found');
            assert.equal(error.statusCode, 404);
            assert.deepEqual(error.options, {
                internalCode: '404_ENF',
                id: 11,
            });
        });
    });

    describe('suggest', () => {
        beforeEach(async() => {
            await factory.group.create([
                { id: 1, name: 'эндпоинт', slug: 'endpoint' },
                { id: 2, name: 'котлин', slug: 'kotlin' },
                { id: 3, name: 'фронтэнд', slug: 'frontend' },
            ]);
        });

        it('should return groups contains substring and order by position', async() => {
            const actual = await Group.suggest('энд', 10, { dbType: testDbType });

            assert.equal(actual.length, 2);
            assert.equal(actual[0].name, 'эндпоинт');
            assert.equal(actual[1].name, 'фронтэнд');
        });

        it('should return groups contains any word and order by position', async() => {
            const actual = await Group.suggest('  пои   фронт   ', 10, { dbType: testDbType });

            assert.equal(actual.length, 2);
            assert.equal(actual[0].name, 'фронтэнд');
            assert.equal(actual[1].name, 'эндпоинт');
        });

        it('should limit groups', async() => {
            const actual = await Group.suggest('о', 2, { dbType: testDbType });

            assert.equal(actual.length, 2);
        });

        it('should return all groups when limit is not passed', async() => {
            const actual = await Group.suggest('о', null, { dbType: testDbType });

            assert.equal(actual.length, 3);
        });

        it('should return by permission', async() => {
            const actual = await Group.suggest('о', null, {
                dbType: testDbType,
                permission: { id: { [Op.or]: [1, 2] } },
            });

            assert.equal(actual.length, 2);
        });
    });
});
