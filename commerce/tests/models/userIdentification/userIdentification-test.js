require('co-mocha');

const fs = require('fs');
const { expect } = require('chai');
const mockery = require('mockery');
const sinon = require('sinon');

const dbHelper = require('tests/helpers/clear');
const mockS3 = require('tests/helpers/s3');

const usersFactory = require('tests/factory/usersFactory');
const trialTemplatesFactory = require('tests/factory/trialTemplatesFactory');
const userIdentificationsFactory = require('tests/factory/userIdentificationsFactory');
const catchError = require('tests/helpers/catchError').generator;

let UserIdentificationModel = require('models/userIdentification');
const { UserIdentification } = require('db/postgres');

describe('UserIdentification model', () => {
    beforeEach(dbHelper.clear);

    describe('`create`', () => {
        const userId = 1;
        const trialTemplateId = 4;
        const S3Response = true;
        const now = 123;
        const photo = fs.readFileSync('tests/models/userIdentification/mock-photo').toString();

        before(() => {
            sinon.stub(Date, 'now').returns(now);
            mockS3(S3Response);

            UserIdentificationModel = require('models/userIdentification');
        });

        beforeEach(function *() {
            yield usersFactory.createWithRelations({ id: userId });
            yield trialTemplatesFactory.createWithRelations({ id: trialTemplateId });
        });

        after(() => {
            Date.now.restore();
            mockery.disable();
            mockery.deregisterAll();
        });

        it('should save new user identification entity', function *() {
            yield UserIdentificationModel.create(userId, trialTemplateId, photo, photo);

            const actual = yield UserIdentification.findAll({
                where: { userId, trialTemplateId },
                attributes: ['userId', 'trialTemplateId', 'document', 'face'],
                raw: true
            });

            const expected = {
                userId,
                trialTemplateId,
                document: `documents/${userId}_${now}.jpg`,
                face: `faces/${userId}_${now}.jpg`
            };

            expect(actual).to.deep.equal([expected]);
        });

        it('should return 400 when photo is not image in base64', function *() {
            const error = yield catchError(UserIdentificationModel.create.bind(
                null, userId, trialTemplateId, photo, 'not_an_image'));

            expect(error.status).to.equal(400);
            expect(error.message).to.equal('Photo is incorrect');
            expect(error.options).to.deep.equal({ internalCode: '400_PII' });
        });
    });

    describe('`getLastFace`', () => {
        const user = { id: 1 };
        const trialTemplate = { id: 2 };
        const face = 'old-face';
        const newFace = 'new-face';

        it('should get last face', function *() {
            yield userIdentificationsFactory.createWithRelations({
                userId: user.id,
                trialTemplateId: trialTemplate.id,
                face
            }, { user, trialTemplate });

            yield userIdentificationsFactory.createWithRelations({
                userId: user.id,
                trialTemplateId: trialTemplate.id,
                face: newFace
            }, { user, trialTemplate });

            const actual = yield UserIdentificationModel.getLastFace(user.id);

            expect(actual).to.be.equal(newFace);
        });

        it('should get face for specified user', function *() {
            const otherUser = { id: 42 };

            yield userIdentificationsFactory.createWithRelations({
                userId: otherUser.id,
                trialTemplateId: trialTemplate.id,
                face: newFace
            }, { user: otherUser, trialTemplate });

            yield userIdentificationsFactory.createWithRelations({
                userId: user.id,
                trialTemplateId: trialTemplate.id,
                face
            }, { user, trialTemplate });

            const actual = yield UserIdentificationModel.getLastFace(user.id);

            expect(actual).to.be.equal(face);
        });

        it('should throw 400 if there is no identification', function *() {
            const error = yield catchError(
                UserIdentificationModel.getLastFace.bind(null, user.id)
            );

            expect(error.status).to.equal(400);
            expect(error.message).to.equal('Face not found');
            expect(error.options).to.deep.equal({ internalCode: '400_FNF' });
        });
    });
});
