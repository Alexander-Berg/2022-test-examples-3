const assert = require('assert');
const _ = require('lodash');
const catchErrorAsync = require('catch-error-async');

const { DbType } = require('db/constants');
const db = require('db');
const factory = require('tests/db/factory');
const cleanDb = require('tests/db/clean');
const Tag = require('models/tag');

const testDbType = DbType.internal;

describe('Tag model', () => {
    beforeEach(cleanDb);

    describe('findOne', () => {
        it('should find a tag', async() => {
            const eventsData = [
                { slug: 'codefest', title: 'codefest' },
                { slug: 'highload', title: 'highload' },
            ];

            const events = await factory.event.create(eventsData);
            const tag = await factory.tag.create({ id: 11, slug: 'php', name: 'php' });

            await tag.addEvents(events);

            const actual = await Tag.findOne({ id: 11, dbType: testDbType });

            assert.equal(actual.slug, 'php');
            assert.equal(actual.events.length, 2);
        });

        it('should find a tag by slug', async() => {
            await factory.tag.create({ id: 11, slug: 'php', name: 'php' });

            const actual = await Tag.findOne({ slug: 'php', dbType: testDbType });

            assert.equal(actual.slug, 'php');
            assert.equal(actual.id, 11);
        });

        it('should order events in a tag', async() => {
            const eventsData = [
                { slug: 'codefest', title: 'codefest' },
                { slug: 'highload', title: 'highload' },
                { slug: 'holyjs', title: 'holyjs' },
            ];

            const events = await factory.event.create(eventsData);
            const tag = await factory.tag.create({
                id: 11,
                slug: 'javascript',
                name: 'javascript',
            });

            await tag.addEvents(events);

            const actual = await Tag.findOne({ id: 11, dbType: testDbType });

            assert.equal(actual.slug, 'javascript');
            assert.equal(actual.events.length, 3);
            assert.equal(actual.events[0].slug, 'codefest');
            assert.equal(actual.events[1].slug, 'highload');
            assert.equal(actual.events[2].slug, 'holyjs');
        });

        it('should throw if tag is not found', async() => {
            const error = await catchErrorAsync(
                Tag.findOne.bind(Tag), { id: 11, scope: 'one', dbType: testDbType },
            );

            assert.equal(error.message, 'Tag not found');
            assert.equal(error.statusCode, 404);
            assert.deepEqual(error.options, {
                internalCode: '404_ENF',
                id: 11,
                scope: 'one',
                dbType: testDbType,
            });
        });
    });

    describe('count', () => {
        it('should count all tags', async() => {
            const tags = _.range(1, 26)
                .map(num => ({ id: num, slug: `#${num}` }));

            await factory.tag.create(tags);

            const actual = await Tag.count({ scope: 'page', dbType: testDbType });

            assert.equal(actual, 25);
        });
    });

    describe('findPage', () => {
        it('should find tags with limit and offset', async() => {
            const tags = _.range(1, 26)
                .map(num => ({ id: num, slug: `#${num}` }));

            await factory.tag.create(tags);

            const actual = await Tag.findPage(
                { scope: 'page', pageSize: 3, pageNumber: 3, dbType: testDbType });

            assert.equal(actual.rows.length, 3);
            assert.equal(actual.rows[0].slug, '#19');
            assert.equal(actual.rows[1].slug, '#18');
            assert.equal(actual.rows[2].slug, '#17');
            assert.equal(actual.meta.totalSize, 25);
            assert.equal(actual.meta.pageNumber, 3);
            assert.equal(actual.meta.pageSize, 3);
        });

        it('should find tags filtered by condition', async() => {
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
            const tags = _.range(1, 26)
                .map(num => ({ id: num, slug: `#${num}` }));

            await factory.tag.create(tags);

            const actual = await Tag.findPage({
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
        it('should create a new tag', async() => {
            const tag = new Tag({
                slug: 'php',
                name: 'php',
            }, { dbType: testDbType });

            const tagId = await tag.create({ dbType: testDbType });

            const actual = await db.tag.findAll();

            assert.ok(/\d/.test(tagId));
            assert.equal(actual.length, 1);
            assert.equal(actual[0].slug, 'php');
        });

        it('should throw if a slug already exists', async() => {
            await factory.tag.create({ id: 12, slug: 'php' });

            const tag = new Tag({
                slug: 'php',
                name: 'php',
            }, { dbType: testDbType });

            const error = await catchErrorAsync(
                tag.create.bind(tag, { dbType: testDbType }),
            );

            assert.equal(error.message, 'Tag already exists');
            assert.equal(error.statusCode, 409);
            assert.deepEqual(error.options, {
                internalCode: '409_EAE',
                slug: 'php',
            });
        });

        it('should throw if category is not existed', async() => {
            const tag = new Tag({
                slug: 'php',
                name: 'php',
                categoryId: 12,
            }, { dbType: testDbType });

            const error = await catchErrorAsync(
                tag.create.bind(tag, { dbType: testDbType }),
            );

            assert.strictEqual(error.message, 'Related entity (from table tagCategory) not found');
            assert.strictEqual(error.statusCode, 404);
            assert.deepStrictEqual(error.options, {
                internalCode: '404_ENF',
                id: 12,
            });
        });
    });

    describe('patch', () => {
        it('should patch an existing tag', async() => {
            await factory.tag.create({
                id: 13,
                slug: 'old-tag',
                name: 'old tag',
            });

            const data = {
                id: 13,
                name: 'new tag',
            };

            const tag = new Tag(data, { dbType: testDbType });
            const tagId = await tag.patch({ yandexuid: '1', dbType: testDbType });
            const actual = await db.tag.findAll();

            assert.ok(/\d/.test(tagId));
            assert.equal(actual.length, 1);
            assert.equal(actual[0].name, 'new tag');
        });

        it('should not patch other tags', async() => {
            await factory.tag.create([{
                id: 13,
                slug: 'old-tag',
                name: 'old tag',
            }, {
                id: 15,
                slug: 'one-more-event',
                name: 'old more tag',
            }]);

            const data = {
                id: 13,
                name: 'new tag',
            };

            const tag = new Tag(data, { dbType: testDbType });
            const tagId = await tag.patch({ yandexuid: '1', dbType: testDbType });
            const actual = await db.tag.findAll({ order: [['id']] });

            assert.ok(/\d/.test(tagId));
            assert.equal(actual.length, 2);
            assert.equal(actual[0].name, 'new tag');
            assert.equal(actual[1].name, 'old more tag');
        });

        it('should throw on nonexistent tag', async() => {
            const tag = new Tag({ id: 13 }, { dbType: testDbType });
            const error = await catchErrorAsync(
                tag.patch.bind(tag, { dbType: testDbType }),
            );

            assert.equal(error.message, 'Tag not found');
            assert.equal(error.statusCode, 404);
            assert.deepEqual(error.options, {
                internalCode: '404_ENF',
                id: 13,
            });
        });
    });

    describe('destroy', () => {
        it('should destroy a tag', async() => {
            await factory.tag.create({ id: 11, slug: 'php', name: 'php' });

            const destroyed = await Tag.destroy(11, { dbType: testDbType });
            const tags = await Tag.findPage(
                { scope: 'page', pageSize: 10, pageNumber: 1, dbType: testDbType });

            assert.equal(destroyed, 1);
            assert.equal(tags.meta.totalSize, 0);
        });

        it('should destroy a tag and relations with events', async() => {
            const eventsData = [
                { slug: 'codefest', title: 'codefest' },
                { slug: 'highload', title: 'highload' },
                { slug: 'holyjs', title: 'holyjs' },
            ];

            const events = await factory.event.create(eventsData);
            const tag = await factory.tag.create({
                id: 11,
                slug: 'javascript',
                name: 'javascript',
            });

            await tag.addEvents(events);

            /**
             * После добавления к тегу событий проверяем, чтобы в таблице-связке
             * появились соответствующие записи
             */
            // eslint-disable-next-line camelcase
            const eventTags = await db.eventTags.findAll({ where: { tagId: 11 } });

            assert.equal(eventTags.length, 3);

            const destroyed = await Tag.destroy(11, { dbType: testDbType });
            const tags = await db.tag.findAll();

            /**
             * Удаление тега не должно влиять на связанные события,
             * но удалились все связанные с тегом записи из eventTags
             */
            const eventsAfterDestroyTag = await db.event.findAll();
            const eventTagsAfterDestroyTag = await db.eventTags.findAll();

            assert.equal(destroyed, 1);
            assert.equal(tags.length, 0);
            assert.equal(eventsAfterDestroyTag.length, 3);
            assert.equal(eventTagsAfterDestroyTag.length, 0);
        });

        it('should throw if tag is not found', async() => {
            const error = await catchErrorAsync(
                Tag.destroy.bind(Tag), 11, { dbType: testDbType },
            );

            assert.equal(error.message, 'Tag not found');
            assert.equal(error.statusCode, 404);
            assert.deepEqual(error.options, {
                internalCode: '404_ENF',
                id: 11,
            });
        });
    });

    describe('suggest', () => {
        beforeEach(async() => {
            await factory.tag.create([
                { name: 'котлин', slug: 'kotlin' },
                { name: 'эндпоинт', slug: 'endpoint' },
                { name: 'фронтэнд', slug: 'frontend' },
            ]);
        });

        it('should return tags contains substring and order by position', async() => {
            const actual = await Tag.suggest('энд', 10, testDbType);

            assert.equal(actual.length, 2);
            assert.equal(actual[0].name, 'эндпоинт');
            assert.equal(actual[1].name, 'фронтэнд');
        });

        it('should return tags contains any word and order by position', async() => {
            const actual = await Tag.suggest('  пои   фронт   ', 10, testDbType);

            assert.equal(actual.length, 2);
            assert.equal(actual[0].name, 'фронтэнд');
            assert.equal(actual[1].name, 'эндпоинт');
        });

        it('should limit tags', async() => {
            const actual = await Tag.suggest('о', 2, testDbType);

            assert.equal(actual.length, 2);
        });

        it('should return all tags when limit is not passed', async() => {
            const actual = await Tag.suggest('о', null, testDbType);

            assert.equal(actual.length, 3);
        });
    });
});
