const { expect } = require('chai');

const DraftModel = require('models/draft');
const { Draft } = require('db/postgres');

const dbHelper = require('tests/helpers/clear');
const trialTemplatesFactory = require('tests/factory/trialTemplatesFactory');
const draftsFactory = require('tests/factory/draftsFactory');
const adminsFactory = require('tests/factory/adminsFactory');

const draftJSON = require('tests/models/data/json/correctDraft.json');

describe('`Draft model`', () => {
    beforeEach(dbHelper.clear);

    describe('`create`', () => {
        it('should create new record with default `status = saved`', function *() {
            yield trialTemplatesFactory.createWithRelations({ id: 2, slug: 'year' });
            yield adminsFactory.create({ id: 7, login: 'rinka' });

            const draftData = {
                exam: draftJSON,
                adminId: 7,
                trialTemplateId: 2
            };
            const expectedDraftData = {
                exam: draftJSON,
                adminId: 7,
                trialTemplateId: 2,
                status: 'saved'
            };

            const created = yield DraftModel.create(draftData);

            expect(created.status).to.equal('saved');

            const actual = yield Draft.findAll({
                attributes: ['adminId', 'trialTemplateId', 'exam', 'status'],
                raw: true
            });

            expect(actual).to.deep.equal([expectedDraftData]);
        });

        it('should create new record with required `status`', function *() {
            yield trialTemplatesFactory.createWithRelations({ id: 2, slug: 'year' });
            yield adminsFactory.create({ id: 7, login: 'rinka' });

            const draftData = {
                exam: draftJSON,
                adminId: 7,
                trialTemplateId: 2,
                status: 'ignored'
            };

            yield DraftModel.create(draftData);

            const actual = yield Draft.findAll({
                attributes: ['adminId', 'trialTemplateId', 'exam', 'status'],
                raw: true
            });

            expect(actual).to.deep.equal([draftData]);
        });
    });

    describe('`find`', () => {
        it('should find last draft', function *() {
            const trialTemplate = { id: 2 };
            const firstJSON = {
                questions: [{ id: 3, text: 'first text' }],
                answers: [{ id: 5, questionId: { id: 3, text: 'first text' } }]
            };
            const secondJSON = {
                questions: [{ id: 4, text: 'second text' }],
                answers: [{ id: 0, questionId: { id: 4, text: 'second text' } }]
            };

            yield draftsFactory.createWithRelations(
                { exam: firstJSON, saveDate: new Date(2018, 5, 10), status: 'saved' },
                { trialTemplate }
            );

            yield draftsFactory.createWithRelations(
                { exam: secondJSON, saveDate: new Date(2018, 10, 10), status: 'published' },
                { trialTemplate }
            );

            const actual = yield DraftModel.find(2);

            expect(actual).to.deep.equal({
                exam: secondJSON,
                status: 'published'
            });
        });

        it('should find draft for current exam', function *() {
            const firstJSON = {
                questions: [{ id: 3, text: 'first text' }],
                answers: [{ id: 5, questionId: { id: 3, text: 'first text' } }]
            };
            const secondJSON = {
                questions: [{ id: 4, text: 'second text' }],
                answers: [{ id: 0, questionId: { id: 4, text: 'second text' } }]
            };
            const now = new Date();

            yield draftsFactory.createWithRelations(
                { exam: firstJSON, saveDate: now, status: 'saved' },
                { trialTemplate: { id: 2 } }
            );

            yield draftsFactory.createWithRelations(
                { exam: secondJSON, saveDate: now, status: 'saved' },
                { trialTemplate: { id: 3 } }
            );

            const actual = yield DraftModel.find(3);

            expect(actual).to.deep.equal({
                exam: secondJSON,
                status: 'saved'
            });
        });

        it('should return null when draft is absent', function *() {
            const actual = yield DraftModel.find(3);

            expect(actual).to.be.null;
        });
    });

    describe('`updateStatus`', () => {
        it('should update status by id', function *() {
            yield draftsFactory.createWithRelations(
                { id: 1, status: 'saved', exam: {} },
                { trialTemplate: { id: 1 }, admin: { id: 1 } }
            );
            yield draftsFactory.createWithRelations(
                { id: 2, status: 'saved', exam: {} },
                { trialTemplate: { id: 1 }, admin: { id: 1 } }
            );

            yield DraftModel.updateStatus(1, 'published');

            const actual = yield Draft.findAll({
                attributes: ['status', 'id'],
                order: [['id']],
                raw: true
            });

            expect(actual).to.deep.equal([
                { id: 1, status: 'published' },
                { id: 2, status: 'saved' }
            ]);
        });
    });
});
