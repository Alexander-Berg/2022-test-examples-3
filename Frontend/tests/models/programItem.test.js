const assert = require('assert');
const config = require('yandex-cfg');
const catchErrorAsync = require('catch-error-async');
const _ = require('lodash');

const { DbType } = require('db/constants');
const db = require('db');
const factory = require('tests/db/factory');
const cleanDb = require('tests/db/clean');
const ProgramItem = require('models/programItem');

const testDbType = DbType.internal;

describe('ProgramItem model', () => {
    beforeEach(cleanDb);

    describe('findOne', () => {
        it('should find a program item', async() => {
            const tags = await factory.tag.create([
                { id: 1, slug: 'javascript', name: 'javascript' },
                { id: 2, slug: 'php', name: 'php' },
            ]);
            const speakers = await factory.speaker.create([
                { id: 1, email: 'q1@ya.ru' },
                { id: 2, email: 'q2@ya.ru' },
            ]);

            const data = {
                id: 11,
                startDate: new Date('2018-02-16T04:00:00.000Z'),
                endDate: new Date('2018-02-16T11:30:00.000Z'),
                isTalk: true,
                eventId: {
                    id: 8,
                    slug: 'codefest',
                    title: 'codefest',
                },
                title: 'Эксперимент как инструмент для принятия решений',
                description: 'Виктор расскажет о подходе, который помогает определять.',
                sectionId: null,
                presentations: [],
                videos: [],
                createdAt: new Date(),
            };
            const programItem = await factory.programItem.create(data);

            await programItem.addTags(tags);
            await programItem.addSpeakers(speakers);

            const actual = await ProgramItem.findOne({ id: 11, scope: 'one', dbType: testDbType });
            const expectedTags = tags.map(t => _.pick(t.toJSON(), ['id', 'slug', 'name', 'createdAt']));
            const expectedSpeakers = speakers.map(s => _.pick(s.toJSON(), ['id', 'firstName', 'lastName']));
            const expected = { ...data, tags: expectedTags, speakers: expectedSpeakers, eventId: 8 };

            assert.deepEqual(actual.toJSON(), expected);
        });

        it('should throw if program item is not found', async() => {
            const error = await catchErrorAsync(
                ProgramItem.findOne.bind(ProgramItem), { id: 11, scope: 'one', dbType: testDbType },
            );

            assert.equal(error.message, 'Program item not found');
            assert.equal(error.statusCode, 404);
            assert.deepEqual(error.options, {
                internalCode: '404_ENF',
                id: 11,
                scope: 'one',
                dbType: testDbType,
            });
        });
    });

    describe('create', () => {
        it('should create a new program item', async() => {
            await factory.event.create({ id: 8, slug: 'codefest', title: 'codefest' });

            const data = {
                id: '5',
                title: 'Серверный рендер с React. Весело, задорно',
                startDate: new Date('2018-02-16T04:00:00.000Z'),
                endDate: new Date('2018-02-16T04:30:00.000Z'),
                isTalk: true,
                eventId: '8',
            };
            const programItem = new ProgramItem(data, { dbType: testDbType });
            const programItemId = await programItem.create({ dbType: testDbType });

            const actual = await ProgramItem.findOne({ id: programItemId, scope: 'one', dbType: testDbType });

            assert.equal(actual.id, data.id);
            assert.equal(actual.isTalk, data.isTalk);
            assert.equal(actual.title, data.title);
            assert.deepEqual(actual.startDate, data.startDate);
            assert.deepEqual(actual.endDate, data.endDate);
        });

        it('should save a new program item with thread and section', async() => {
            const programTypes = config.schema.sectionTypeEnum;

            await factory.event.create({ id: 8, slug: 'codefest', title: 'codefest' });
            await factory.section.create({
                id: 13,
                eventId: 8,
                title: 'Фронтэнд',
                type: programTypes.section,
            });
            await factory.section.create({
                id: 14,
                eventId: 8,
                title: 'Зал 1',
                type: programTypes.thread,
            });

            const data = {
                id: '5',
                title: 'Серверный рендер с React. Весело, задорно',
                startDate: new Date('2018-02-16T04:00:00.000Z'),
                endDate: new Date('2018-02-16T04:30:00.000Z'),
                isTalk: true,
                eventId: 8,
                sectionId: 13,
            };

            const programItem = new ProgramItem(data, { dbType: testDbType });
            const programItemId = await programItem.create({ dbType: testDbType });

            const actual = await ProgramItem.findOne({ id: programItemId, scope: 'one', dbType: testDbType });

            assert.equal(actual.id, data.id);
            assert.equal(actual.sectionId, data.sectionId);
        });

        it('should throw if related event not found', async() => {
            const data = {
                id: '5',
                title: 'Серверный рендер с React. Весело, задорно',
                startDate: new Date('2018-02-16T04:00:00.000Z'),
                endDate: new Date('2018-02-16T04:30:00.000Z'),
                eventId: '8',
            };

            const programItem = new ProgramItem(data, { dbType: testDbType });

            const error = await catchErrorAsync(
                programItem.create.bind(programItem, { dbType: testDbType }),
            );

            assert.equal(error.message, 'Related entity (from table event) not found');
            assert.equal(error.statusCode, 404);
            assert.deepEqual(error.options, {
                internalCode: '404_ENF',
                id: '8',
            });
        });

        it('should throw if related section not found', async() => {
            await factory.event.create({ id: 8, slug: 'codefest', title: 'codefest' });

            const data = {
                id: '5',
                title: 'Серверный рендер с React. Весело, задорно',
                startDate: new Date('2018-02-16T04:00:00.000Z'),
                endDate: new Date('2018-02-16T04:30:00.000Z'),
                eventId: '8',
                sectionId: '5',
            };

            const programItem = new ProgramItem(data, { dbType: testDbType });

            const error = await catchErrorAsync(
                programItem.create.bind(programItem, { dbType: testDbType }),
            );

            assert.equal(error.message, 'Related entity (from table section) not found');
            assert.equal(error.statusCode, 404);
            assert.deepEqual(error.options, {
                internalCode: '404_ENF',
                id: '5',
            });
        });

        it('should throw if related thread not found', async() => {
            await factory.event.create({ id: 8, slug: 'codefest', title: 'codefest' });

            const data = {
                id: '5',
                title: 'Серверный рендер с React. Весело, задорно',
                startDate: new Date('2018-02-16T04:00:00.000Z'),
                endDate: new Date('2018-02-16T04:30:00.000Z'),
                eventId: 8,
                sectionId: 5,
            };

            const programItem = new ProgramItem(data, { dbType: testDbType });

            const error = await catchErrorAsync(
                programItem.create.bind(programItem, { dbType: testDbType }),
            );

            assert.equal(error.message, 'Related entity (from table section) not found');
            assert.equal(error.statusCode, 404);
            assert.deepEqual(error.options, {
                internalCode: '404_ENF',
                id: '5',
            });
        });
    });

    describe('destroy', () => {
        it('should destroy a program item', async() => {
            await factory.programItem.create({
                id: '5',
                title: 'Серверный рендер с React. Весело, задорно',
                startDate: new Date('2018-02-16T04:00:00.000Z'),
                endDate: new Date('2018-02-16T04:30:00.000Z'),
                eventId: {
                    id: 8,
                    slug: 'codefest',
                    title: 'codefest',
                },
            }, { dbType: testDbType });

            const destroyed = await ProgramItem.destroy(5, { dbType: testDbType });
            const list = await db.programItem.findAll();

            assert.equal(destroyed, 1);
            assert.equal(list.length, 0);
        });

        it('should destroy a program item and relations with tags', async() => {
            const data = {
                id: '5',
                title: 'Серверный рендер с React. Весело, задорно',
                startDate: new Date('2018-02-16T04:00:00.000Z'),
                endDate: new Date('2018-02-16T04:30:00.000Z'),
                eventId: {
                    id: 8,
                    slug: 'codefest',
                    title: 'codefest',
                },
            };

            const programItem = await factory.programItem.create(data);
            const tags = await factory.tag.create([
                { id: 1, slug: 'javascript', name: 'javascript' },
                { id: 2, slug: 'php', name: 'php' },
                { id: 3, slug: 'dart', name: 'dart' },
            ]);

            await programItem.addTags(tags);

            const destroyed = await ProgramItem.destroy(5, { dbType: testDbType });

            /**
             * Удаление элемента программы не должно влиять на связанные теги,
             * но должны удалиться все связанные с тегом записи из programItemTags
             */
            const tagsAfterDestroyTag = await db.tag.findAll();
            const programItemTagsAfterDestroyTag = await db.programItemTags.findAll();

            assert.equal(destroyed, 1);
            assert.equal(tagsAfterDestroyTag.length, 3);
            assert.equal(programItemTagsAfterDestroyTag.length, 0);
        });

        it('should destroy a program item with presentations and videos', async() => {
            const data = {
                id: '5',
                title: 'Серверный рендер с React. Весело, задорно',
                startDate: new Date('2018-02-16T04:00:00.000Z'),
                endDate: new Date('2018-02-16T04:30:00.000Z'),
                eventId: {
                    id: 8,
                    slug: 'codefest',
                    title: 'codefest',
                },
                presentations: [{ downloadUrl: 'https://slides.yandex.ru/6ef6h' }],
                videos: [{ iframeUrl: 'https://www.youtube.com/embed/zB4I68XVPzQ' }],
            };

            await factory.programItem.create(data);

            const destroyed = await ProgramItem.destroy(5, { dbType: testDbType });
            const presentations = await db.presentation.findAll();
            const videos = await db.video.findAll();

            assert.equal(destroyed, 1);
            assert.equal(presentations.length, 0);
            assert.equal(videos.length, 0);
        });

        it('should throw if program item is not found', async() => {
            const error = await catchErrorAsync(
                ProgramItem.destroy.bind(ProgramItem), 11, { dbType: testDbType },
            );

            assert.equal(error.message, 'Program item not found');
            assert.equal(error.statusCode, 404);
            assert.deepEqual(error.options, {
                internalCode: '404_ENF',
                id: 11,
            });
        });
    });

    describe('patch', () => {
        it('should patch an existing program item', async() => {
            const data = {
                id: 11,
                startDate: new Date('2018-02-16T04:00:00.000Z'),
                endDate: new Date('2018-02-16T11:30:00.000Z'),
                eventId: { id: 8, slug: 'codefest', title: 'codefest' },
                title: 'Эксперимент как инструмент для принятия решений',
            };

            await factory.programItem.create(data);

            const programItem = new ProgramItem({ id: 11, title: 'кек' }, { dbType: testDbType });
            const programItemId = await programItem.patch({ dbType: testDbType });
            const actual = await db.programItem.findOne({ where: { id: programItemId }, dbType: testDbType });

            assert.equal(actual.id, data.id);
            assert.equal(actual.title, 'кек');
            assert.equal(actual.eventId, '8');
        });

        it('should update program item\'s presentations', async() => {
            const programItemData = { id: 13, title: 'old-title' };

            await factory.presentation.create({ programItemId: programItemData });

            const data = {
                id: 13,
                title: 'new-title',
                presentations: [{ downloadUrl: 'https://slides.yandex.ru/6ef6h' }],
            };

            const entity = new ProgramItem(data, { dbType: testDbType });
            const entityId = await entity.patch({ dbType: testDbType });
            const actual = await db.presentation.findAll({ order: [['id']], dbType: testDbType });

            assert.equal(actual.length, 1);
            assert.ok(_.every(actual, { programItemId: entityId }));
            assert.equal(actual[0].downloadUrl, 'https://slides.yandex.ru/6ef6h');
        });

        it('should not remove presentations if undefined is passed', async() => {
            await factory.presentation.create({ programItemId: { id: 13, title: 'old-title' } });

            const event = new ProgramItem({ id: 13 }, { dbType: testDbType });

            await event.patch({ dbType: testDbType });

            const actual = await db.presentation.count();

            assert.equal(actual, 1);
        });

        it('should remove presentations if empty array is passed', async() => {
            await factory.presentation.create({ programItemId: { id: 13, title: 'old-title' } });

            const event = new ProgramItem({ id: 13, presentations: [] }, { dbType: testDbType });

            await event.patch({ dbType: testDbType });

            const actual = await db.presentation.count();

            assert.equal(actual, 0);
        });

        it('should throw if related event not found', async() => {
            const data = {
                id: 11,
                startDate: new Date('2018-02-16T04:00:00.000Z'),
                endDate: new Date('2018-02-16T11:30:00.000Z'),
                eventId: { id: 8, slug: 'codefest', title: 'codefest' },
                title: 'Эксперимент как инструмент для принятия решений',
            };

            await factory.programItem.create(data);

            const programItem = new ProgramItem({ id: 11, eventId: 13 }, { dbType: testDbType });
            const error = await catchErrorAsync(
                programItem.patch.bind(programItem, { dbType: testDbType }),
            );

            assert.equal(error.message, 'Related entity (from table event) not found');
            assert.equal(error.statusCode, 404);
            assert.deepEqual(error.options, {
                internalCode: '404_ENF',
                id: '13',
            });
        });

        it('should throw on nonexistent program item', async() => {
            const programItem = new ProgramItem({ id: 13 }, { dbType: testDbType });
            const error = await catchErrorAsync(programItem.patch.bind(programItem, { dbType: testDbType }));

            assert.equal(error.message, 'Program item not found');
            assert.equal(error.statusCode, 404);
            assert.deepEqual(error.options, {
                internalCode: '404_ENF',
                id: 13,
            });
        });
    });

    describe('addTags', () => {
        it('should add program item tags', async() => {
            const data = {
                id: '5',
                title: 'Серверный рендер с React. Весело, задорно',
                startDate: new Date('2018-02-16T04:00:00.000Z'),
                endDate: new Date('2018-02-16T04:30:00.000Z'),
                eventId: {
                    id: 8,
                    slug: 'codefest',
                    title: 'codefest',
                },
            };

            await factory.programItem.create(data);
            await factory.tag.create([
                { id: 3, slug: 'javascript', name: 'Javascript' },
                { id: 4, slug: 'react', name: 'React' },
                { id: 5, slug: 'reconciliation', name: 'React reconciliation' },
                { id: 6, slug: 'cpp', name: 'C++' },
            ]);

            await ProgramItem.addTags(5, [3, 4, 5], { dbType: testDbType });
            const actual = await db.programItemTags.findAll({
                raw: true,
                order: [['tagId']],
            });

            assert.equal(actual.length, 3);
            assert.ok(_.every(actual, { programItemId: 5 }));
            assert.equal(actual[0].tagId, 3);
            assert.equal(actual[1].tagId, 4);
            assert.equal(actual[2].tagId, 5);
        });

        it('should throw on nonexistent program item', async() => {
            const error = await catchErrorAsync(
                ProgramItem.addTags.bind(ProgramItem), 5, [3, 4, 5], { dbType: testDbType },
            );

            assert.equal(error.message, 'Program item not found');
            assert.equal(error.statusCode, 404);
            assert.deepEqual(error.options, {
                internalCode: '404_ENF',
                id: 5,
            });
        });
    });

    describe('removeTags', () => {
        it('should remove program item tags', async() => {
            const data = {
                id: '5',
                title: 'Серверный рендер с React. Весело, задорно',
                startDate: new Date('2018-02-16T04:00:00.000Z'),
                endDate: new Date('2018-02-16T04:30:00.000Z'),
                eventId: {
                    id: 8,
                    slug: 'codefest',
                    title: 'codefest',
                },
            };

            const entity = await factory.programItem.create(data);
            const tags = await factory.tag.create([
                { id: 3, slug: 'javascript', name: 'Javascript' },
                { id: 4, slug: 'react', name: 'React' },
                { id: 5, slug: 'reconciliation', name: 'React reconciliation' },
            ]);

            await entity.addTags(tags);
            const added = await db.programItemTags.count();

            assert.equal(added, 3);

            await ProgramItem.removeTags(5, 3, { dbType: testDbType });

            const actual = await db.programItemTags.findAll({
                raw: true,
                order: [['tagId']],
            });

            assert.equal(actual.length, 2);
            assert.ok(_.every(actual, { programItemId: 5 }));
            assert.equal(actual[0].tagId, 4);
            assert.equal(actual[1].tagId, 5);
        });

        it('should throw on nonexistent program item', async() => {
            const error = await catchErrorAsync(
                ProgramItem.removeTags.bind(ProgramItem), 5, [3, 4, 5], { dbType: testDbType },
            );

            assert.equal(error.message, 'Program item not found');
            assert.equal(error.statusCode, 404);
            assert.deepEqual(error.options, {
                internalCode: '404_ENF',
                id: 5,
            });
        });
    });
});
