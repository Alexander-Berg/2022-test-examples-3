const assert = require('assert');
const _ = require('lodash');
const config = require('yandex-cfg');
const catchErrorAsync = require('catch-error-async');

const { DbType } = require('db/constants');
const db = require('db');
const factory = require('tests/db/factory');
const cleanDb = require('tests/db/clean');
const Section = require('models/section');

const testDbType = DbType.internal;
const types = config.schema.sectionTypeEnum;

describe('Section model', () => {
    beforeEach(cleanDb);

    describe('findOne', () => {
        it('should find a section', async() => {
            const data = {
                id: 5,
                createdAt: new Date(),
                title: 'Фронтэнд',
                order: 1,
                isPublished: true,
                slug: 'front',
                eventId: {
                    id: 8,
                    slug: 'codefest',
                    title: 'codefest',
                },
            };

            await factory.section.create(data);

            const actual = await Section.findOne({ id: 5, dbType: testDbType });

            assert.deepEqual(actual.toJSON(), { ...data, eventId: 8 });
        });

        it('should throw if section is not found', async() => {
            const error = await catchErrorAsync(
                Section.findOne.bind(Section),
                { id: 11, dbType: testDbType },
            );

            assert.equal(error.message, 'Section not found');
            assert.equal(error.statusCode, 404);
            assert.deepEqual(error.options, {
                internalCode: '404_ENF',
                id: 11,
                dbType: testDbType,
            });
        });
    });

    describe('create', () => {
        it('should save a new section', async() => {
            await factory.event.create({ id: 8, slug: 'codefest', title: 'codefest' });

            const data = {
                id: 5,
                title: 'Фронтэнд',
                order: 1,
                isPublished: true,
                slug: 'front',
                eventId: 8,
                createdAt: new Date(),
                type: types.section,
            };
            const section = new Section(data, { authorLogin: 'art00', dbType: testDbType });
            const sectionId = await section.create({ dbType: testDbType });

            const actual = await db.section.findOne({ where: { id: sectionId } });

            assert.deepEqual(actual.toJSON(), data);
        });

        it('should create history record when a section was created', async() => {
            await factory.event.create({ id: 8, slug: 'codefest', title: 'codefest' });

            const data = {
                id: 5,
                title: 'Фронтэнд',
                order: 1,
                isPublished: true,
                slug: 'front',
                eventId: 8,
                createdAt: new Date(),
            };
            const section = new Section(data, { authorLogin: 'art00', dbType: testDbType });

            await section.create({ dbType: testDbType });

            const [record] = await db.history.findAll();
            const actual = _.pick(record, ['entityId', 'authorLogin', 'operation', 'entityType']);
            const expected = {
                entityId: 8,
                authorLogin: 'art00',
                operation: 'update',
                entityType: 'event',
            };

            assert.deepEqual(actual, expected);
        });

        it('should throw if related event not found', async() => {
            const section = new Section({
                title: 'Фронтэнд',
                eventId: '8',
            }, { dbType: testDbType });

            const error = await catchErrorAsync(
                section.create.bind(section, { dbType: testDbType }),
            );

            assert.equal(error.message, 'Event not found');
            assert.equal(error.statusCode, 404);
            assert.deepEqual(error.options, {
                internalCode: '404_ENF',
                id: '8',
            });
        });

        it('should throw if section with this slug already has in event', async() => {
            const data = {
                id: 5,
                createdAt: new Date(),
                title: 'Фронтэнд',
                order: 1,
                isPublished: true,
                slug: 'front',
                eventId: {
                    id: 8,
                    slug: 'codefest',
                    title: 'codefest',
                },
            };

            await factory.section.create(data);

            const newSection = new Section({
                title: 'Фронтэнд',
                slug: 'front',
                eventId: 8,
            }, { authorLogin: 'art00', dbType: testDbType });

            const error = await catchErrorAsync(
                newSection.create.bind(newSection, { dbType: testDbType }),
            );

            assert.equal(error.message, 'Section with this slug already has in event');
            assert.equal(error.statusCode, 404);
            assert.deepEqual(error.options, {
                internalCode: '404_SSH',
                slug: 'front',
                eventId: '8',
            });
        });
    });

    describe('patch', () => {
        it('should patch an existing section', async() => {
            const data = {
                id: '5',
                title: 'Фронтэнд',
                order: 1,
                eventId: {
                    id: 8,
                    slug: 'codefest',
                    title: 'codefest',
                },
            };

            await factory.section.create(data);

            const section = new Section({ id: 5, order: 2 }, { authorLogin: 'art00', dbType: testDbType });
            const sectionId = await section.patch({ dbType: testDbType });
            const actual = await db.section.findOne({ where: { id: sectionId } });

            assert.equal(actual.id, data.id);
            assert.equal(actual.title, data.title);
            assert.equal(actual.order, 2);
        });

        it('should create history record when a section was created', async() => {
            const data = {
                id: '5',
                title: 'Фронтэнд',
                order: 1,
                eventId: {
                    id: 8,
                    slug: 'codefest',
                    title: 'codefest',
                },
            };

            await factory.section.create(data);

            const section = new Section({ id: 5, order: 2 }, { authorLogin: 'art00', dbType: testDbType });

            await section.patch({ dbType: testDbType });

            const [record] = await db.history.findAll();
            const actual = _.pick(record, ['entityId', 'authorLogin', 'operation', 'entityType']);
            const expected = {
                entityId: 8,
                authorLogin: 'art00',
                operation: 'update',
                entityType: 'event',
            };

            assert.deepEqual(actual, expected);
        });

        it('should throw if related event not found', async() => {
            await factory.section.create({
                id: '5',
                title: 'Фронтэнд',
                order: 1,
                eventId: {
                    id: 8,
                    slug: 'codefest',
                    title: 'codefest',
                },
            });

            const section = new Section({ id: '5', eventId: '13' }, { dbType: testDbType });
            const error = await catchErrorAsync(
                section.patch.bind(section, { dbType: testDbType }),
            );

            assert.equal(error.message, 'Event not found');
            assert.equal(error.statusCode, 404);
            assert.deepEqual(error.options, {
                internalCode: '404_ENF',
                id: '13',
            });
        });

        it('should throw on nonexistent section', async() => {
            const section = new Section({ id: 13 }, { dbType: testDbType });
            const error = await catchErrorAsync(section.patch.bind(section, { dbType: testDbType }));

            assert.equal(error.message, 'Section not found');
            assert.equal(error.statusCode, 404);
            assert.deepEqual(error.options, {
                internalCode: '404_ENF',
                id: 13,
            });
        });

        it('should throw if section with this slug already has in event', async() => {
            await factory.section.create([
                {
                    id: 5,
                    title: 'Фронтэнд',
                    slug: 'front',
                    eventId: {
                        id: 8,
                        slug: 'codefest',
                        title: 'codefest',
                    },
                },
                {
                    id: 6,
                    title: 'Фронтэнд2',
                    slug: 'front2',
                    eventId: 8,
                },
            ]);

            const newSection = new Section({ id: 6, slug: 'front' }, { authorLogin: 'art00', dbType: testDbType });
            const error = await catchErrorAsync(
                newSection.patch.bind(newSection, { dbType: testDbType }),
            );

            assert.equal(error.message, 'Section with this slug already has in event');
            assert.equal(error.statusCode, 404);
            assert.deepEqual(error.options, {
                internalCode: '404_SSH',
                slug: 'front',
                eventId: '8',
            });
        });
    });

    describe('destroy', () => {
        it('should destroy a section', async() => {
            const data = {
                id: '5',
                title: 'Фронтэнд',
                eventId: { id: 8, slug: 'codefest', title: 'codefest' },
            };

            await factory.section.create(data);

            const destroyed = await Section.destroy(5, { authorLogin: 'art00', dbType: testDbType });
            const list = await db.section.findAll();

            assert.equal(destroyed, 1);
            assert.equal(list.length, 0);
        });

        it('should create history record when a section was destroyed', async() => {
            const data = {
                id: '5',
                title: 'Фронтэнд',
                eventId: { id: 8, slug: 'codefest', title: 'codefest' },
            };

            await factory.section.create(data);
            await Section.destroy(5, { authorLogin: 'art00', dbType: testDbType });

            const [record] = await db.history.findAll();
            const actual = _.pick(record, ['entityId', 'authorLogin', 'operation', 'entityType']);
            const expected = {
                entityId: 8,
                authorLogin: 'art00',
                operation: 'update',
                entityType: 'event',
            };

            assert.deepEqual(actual, expected);
        });

        it('should throw if section is not found', async() => {
            const error = await catchErrorAsync(
                Section.destroy.bind(Section), 11, { dbType: testDbType },
            );

            assert.equal(error.message, 'Section not found');
            assert.equal(error.statusCode, 404);
            assert.deepEqual(error.options, {
                internalCode: '404_ENF',
                id: 11,
            });
        });

        it('should set item sectionId to null when destroy a section', async() => {
            const data = {
                id: 5,
                title: 'Фронтэнд',
                eventId: { id: 8, slug: 'codefest', title: 'codefest' },
            };

            await factory.section.create(data);
            await factory.programItem.create({
                startDate: new Date('2018-02-16T04:00:00.000Z'),
                endDate: new Date('2018-02-16T11:30:00.000Z'),
                eventId: 8,
                title: 'Эксперимент как инструмент для принятия решений',
                sectionId: 5,
            });

            const programItemListBeforeDestroy = await db.programItem.findAll();

            assert.equal(programItemListBeforeDestroy.length, 1);
            assert.equal(programItemListBeforeDestroy[0].sectionId, 5);

            const destroyed = await Section.destroy(5, { authorLogin: 'art00', dbType: testDbType });
            const programItemList = await db.programItem.findAll();

            assert.equal(destroyed, 1);
            assert.equal(programItemList.length, 1);
            assert.equal(programItemList[0].sectionId, null);
        });
    });
});
