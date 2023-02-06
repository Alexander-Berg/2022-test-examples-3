require('co-mocha');

const { expect } = require('chai');
const nock = require('nock');
const moment = require('moment');
const _ = require('lodash');
const sinon = require('sinon');
const mockery = require('mockery');
let log = require('logger');

const dbHelper = require('tests/helpers/clear');
const catchError = require('tests/helpers/catchError').generator;
const nockMdsServices = require('tests/helpers/mdsServices');
const nockAvatars = nockMdsServices.avatars;
const nockMds = nockMdsServices.mds;
const mockCache = require('tests/helpers/cache');
const mockMailer = require('tests/helpers/mailer');
const nockTvm = require('tests/helpers/nockTvm');

const { Certificate, Trial } = require('db/postgres');
const { achievements, geoadv } = require('yandex-config');

const certificateFactory = require('tests/factory/certificatesFactory');
const trialsFactory = require('tests/factory/trialsFactory');
const trialToQuestionsFactory = require('tests/factory/trialToQuestionsFactory');
const trialTemplateAllowedFailsFactory = require('tests/factory/trialTemplateAllowedFailsFactory');
const trialTemplateToSectionsFactory = require('tests/factory/trialTemplateToSectionsFactory');
const usersFactory = require('tests/factory/usersFactory');
const proctoringResponsesFactory = require('tests/factory/proctoringResponsesFactory');

let CertificateModel = require('models/certificate');
const AttemptModel = require('models/attempt');
const MdsModel = require('models/mds');

describe('Certificate model', () => {
    beforeEach(dbHelper.clear);

    describe('`find`', () => {
        it('should find certificate by id and lastname', function *() {
            const dueDate = new Date(2027, 1, 2, 3, 4, 5);
            const finished = new Date(2016, 1, 2, 3, 4, 5);
            const service = { id: 3, code: 'direct', title: 'Yandex.Direct' };
            const type = { id: 4, code: 'cert' };
            const trialTemplate = { id: 5, previewImagePath: '2345/345667' };
            const trial = { id: 6 };
            const cert = {
                id: 2,
                firstname: 'Ivan',
                lastname: 'Ivanov',
                dueDate,
                active: 1,
                confirmedDate: finished,
                imagePath: '255/38472434872_13',
                pdfPath: '255/38472434872_13.pdf'
            };

            yield certificateFactory.createWithRelations(
                cert,
                { trial, trialTemplate, service, type }
            );

            const certData = yield CertificateModel.find(2, 'Ivanov');
            const actual = certData.toJSON();

            const actualTrial = actual.trial;
            const actualTrialTemplate = actualTrial.trialTemplate;

            expect(actual.id).to.equal(2);
            expect(actual.dueDate).to.deep.equal(dueDate);
            expect(actual.firstname).to.equal('Ivan');
            expect(actual.lastname).to.equal('Ivanov');
            expect(actual.active).to.equal(1);
            expect(actual.confirmedDate).to.deep.equal(finished);
            expect(actual.imagePath).to.equal(certData.imagePath);
            expect(actual.pdfPath).to.equal(certData.pdfPath);
            expect(actual.type).to.deep.equal({ code: 'cert' });
            expect(actualTrial.trialTemplateId).to.equal(5);
            expect(actualTrialTemplate.service).to.deep.equal(service);
            expect(actualTrialTemplate.id).to.equal(5);
            expect(actualTrialTemplate.previewImagePath).to.equal('2345/345667');
        });

        it('should find certificate when lastnames in db and in query are different', function *() {
            const type = { code: 'cert' };
            const trial = { nullified: 0 };
            const cert = {
                id: 7,
                lastname: 'ZhigaloV ',
                active: 1
            };

            yield certificateFactory.createWithRelations(cert, { type, trial });

            const certData = yield CertificateModel.find(7, '  zhigalov  ');
            const actual = certData.toJSON();

            expect(actual.id).to.equal(7);
            expect(actual.lastname).to.equal('ZhigaloV ');
        });

        it('should throw 403 when cert was nullified', function *() {
            const cert = { id: 13, lastname: 'Pupkin', active: 0 };
            const type = { id: 4, code: 'cert' };

            yield certificateFactory.createWithRelations(cert, { trial: { nullified: 0 }, type });

            const error = yield catchError(CertificateModel.find.bind(CertificateModel, 13, 'Pupkin'));

            expect(error.statusCode).to.equal(403);
            expect(error.options).to.deep.equal({ internalCode: '403_CWN' });
            expect(error.message).to.equal('Certificate was nullified');
        });

        it('should throw 404 when certificate not exists', function *() {
            const error = yield catchError(CertificateModel.find.bind(CertificateModel, 2, 'Ivanov'));

            expect(error.statusCode).to.equal(404);
            expect(error.options).to.deep.equal({ internalCode: '404_CNF' });
            expect(error.message).to.equal('Certificate not found');
        });

        it('should throw 404 when user find not `cert`', function *() {
            const trial = { id: 2, passed: 1, expired: 1 };
            const trialTemplate = { id: 3 };
            const type = { id: 23, code: 'test' };
            const certData = { id: 3, lastname: 'Pupkin' };

            yield certificateFactory.createWithRelations(certData, { trial, trialTemplate, type });

            const error = yield catchError(CertificateModel.find.bind(CertificateModel, 3, 'Pupkin'));

            expect(error.statusCode).to.equal(404);
            expect(error.options).to.deep.equal({ internalCode: '404_CNF' });
            expect(error.message).to.equal('Certificate not found');
        });

        it('should throw 404 when lastname not equal', function *() {
            const certData = { id: 2, lastname: 'Petrov', active: 1 };

            yield certificateFactory.createWithRelations(certData);

            const error = yield catchError(CertificateModel.find.bind(CertificateModel, 2, 'Ivanov'));

            expect(error.statusCode).to.equal(404);
            expect(error.options).to.deep.equal({ internalCode: '404_CNF' });
            expect(error.message).to.equal('Certificate not found');
        });

        it('should throw 404 when cert belongs to nullified trial', function *() {
            const nullifiedTrial = { id: 2, passed: 1, expired: 1, nullified: 1 };
            const cert = { id: 5, lastname: 'Pupkin' };
            const type = { id: 4, code: 'cert' };

            yield certificateFactory.createWithRelations(cert, { trial: nullifiedTrial, type });

            const error = yield catchError(CertificateModel.find.bind(CertificateModel, 5, 'Pupkin'));

            expect(error.statusCode).to.equal(404);
            expect(error.options).to.deep.equal({ internalCode: '404_CNF' });
            expect(error.message).to.equal('Certificate not found');
        });
    });

    describe('`pickCertificateData`', () => {
        before(nockAvatars.success);
        after(nock.cleanAll);

        it('should correct pick data from certificate model', function *() {
            const trial = { id: 2 };
            const trialTemplate = { id: 3, previewImagePath: '345/35672', isProctoring: false };
            const finished = new Date(2016, 6, 11);
            const dueDate = new Date(2017, 6, 11);
            const cert = {
                id: 25,
                dueDate,
                firstname: 'Vasya',
                lastname: 'Pupkin',
                active: 1,
                confirmedDate: finished,
                imagePath: '255/38472434872_13'
            };
            const type = { id: 3, code: 'cert' };
            const service = { id: 4, code: 'direct', title: 'Yandex.Direct' };

            yield certificateFactory.createWithRelations(
                cert,
                { trialTemplate, trial, type, service }
            );

            const certData = yield CertificateModel.find(25, 'Pupkin');
            const actual = yield CertificateModel.pickCertificateData(certData);

            const expectedData = {
                certId: 25,
                confirmedDate: finished,
                firstname: 'Vasya',
                lastname: 'Pupkin',
                dueDate,
                active: 1,
                imagePath: MdsModel.getAvatarsPath(cert.imagePath),
                service,
                certType: 'cert',
                previewImagePath: MdsModel.getAvatarsPath('345/35672')
            };

            expect(actual).to.deep.equal(expectedData);
        });

        it('should draw and put certificate when `imagePath` is null', function *() {
            const dueDate = new Date(2027, 1, 2, 3, 4, 5);
            const finished = new Date(2016, 1, 2, 3, 4, 5);
            const cert = {
                id: 2,
                lastname: 'Ivanov',
                dueDate,
                active: 1,
                confirmedDate: finished,
                imagePath: null
            };
            const type = { id: 3, code: 'cert' };

            yield certificateFactory.createWithRelations(cert, { trialTemplate: { isProctoring: false }, type });

            const certBefore = yield Certificate.findOne({
                attributes: ['imagePath', 'typeId'],
                where: { id: 2 }
            });

            expect(certBefore.imagePath).to.be.null;

            const certData = yield CertificateModel.find(2, 'Ivanov');
            const actual = yield CertificateModel.pickCertificateData(certData);

            const certAfter = yield Certificate.findOne({
                attributes: ['imagePath', 'typeId'],
                where: { id: 2 }
            });

            const imagePath = '603/1468925144742_555555';

            expect(certAfter.imagePath).to.equal(imagePath);
            expect(actual.imagePath).to.equal(MdsModel.getAvatarsPath(imagePath));
            expect(actual.certType).to.equal('cert');
        });
    });

    describe('`tryCreate`', () => {
        before(nockAvatars.success);
        after(nock.cleanAll);

        it('should not create new certificate when attempt not passed', function *() {
            yield trialsFactory.createWithRelations({ id: 2, passed: 0 });

            const attempt = yield AttemptModel.findById(2);

            yield CertificateModel.tryCreate(attempt, {});

            const actual = yield Certificate.count({ where: { trialId: 2 } });

            expect(actual).to.equal(0);
        });

        it('should create new certificate when attempt passed and test without proctoring', function *() {
            const trialTemplate = { isProctoring: false };
            const trial = { id: 2, passed: 1, finished: new Date() };

            yield trialsFactory.createWithRelations(trial, { trialTemplate });

            const attempt = yield AttemptModel.findById(2);

            yield CertificateModel.tryCreate(attempt, { isProctoring: false });

            const actual = yield Certificate.count({ where: { trialId: 2 } });

            expect(actual).to.equal(1);
        });

        describe('`tryCreate` with proctoring', () => {
            it('should create new certificate when attempt passed and proctoring is ok', function *() {
                const trialTemplate = { isProctoring: true };
                const trial = { id: 2, passed: 1, finished: new Date() };

                yield trialsFactory.createWithRelations(trial, { trialTemplate });

                const attempt = yield AttemptModel.findById(2);

                yield CertificateModel.tryCreate(attempt, { isProctoring: true, isProctoringCorrect: true });

                const actual = yield Certificate.count({ where: { trialId: 2 } });

                expect(actual).to.equal(1);
            });

            it('should not create certificate when attempt passed and proctoring is not correct', function *() {
                const trialTemplate = { isProctoring: true };
                const trial = { id: 2, passed: 1, finished: new Date() };

                yield trialsFactory.createWithRelations(trial, { trialTemplate });

                const attempt = yield AttemptModel.findById(2);

                yield CertificateModel.tryCreate(attempt, { isProctoring: true, isProctoringCorrect: false });

                const actual = yield Certificate.count({ where: { trialId: 2 } });

                expect(actual).to.equal(0);
            });

            describe('`create`', () => {
                before(nockAvatars.success);
                after(nock.cleanAll);

                it('should create new certificate metrika', function *() {
                    const finished = new Date(2016, 6, 11);
                    const trial = { id: 2, finished, passed: 1 };
                    const trialTemplate = { id: 3, validityPeriod: '1y', isProctoring: false };
                    const user = {
                        id: 14,
                        firstname: 'Petr',
                        lastname: 'Ivanov'
                    };
                    const type = { id: 13, code: 'cert' };
                    const service = { code: 'metrika' };

                    yield trialsFactory.createWithRelations(
                        trial,
                        { trialTemplate, user, type, service }
                    );
                    const attempt = yield AttemptModel.findById(2);

                    yield CertificateModel.create(attempt);

                    const certificate = yield Certificate.findOne({
                        where: { trialId: 2 },
                        attributes: [
                            'trialId',
                            'typeId',
                            'confirmedDate',
                            'dueDate',
                            'active',
                            'confirmed',
                            'firstname',
                            'lastname',
                            'imagePath'
                        ]
                    });

                    const actual = certificate.toJSON();

                    expect(actual.trialId).to.equal(2);
                    expect(actual.typeId).to.equal(13);
                    expect(actual.firstname).to.equal('Petr');
                    expect(actual.lastname).to.equal('Ivanov');
                    expect(actual.confirmedDate).to.deep.equal(finished);
                    expect(actual.dueDate).to.deep.equal(new Date(finished.setYear(2017)));
                    expect(actual.active).to.equal(1);
                    expect(actual.confirmed).to.equal(1);
                    expect(actual.imagePath).to.equal('603/1468925144742_555555');
                });

                it('should create new certificate market', function *() {
                    const finished = new Date(2018, 6, 11);
                    const trial = { id: 2, finished, passed: 1 };
                    const trialTemplate = { id: 3, validityPeriod: '1y', isProctoring: false };
                    const user = {
                        id: 14,
                        firstname: 'Petr',
                        lastname: 'Ivanov'
                    };
                    const type = { id: 13, code: 'cert' };
                    const service = { code: 'market' };

                    yield trialsFactory.createWithRelations(
                        trial,
                        { trialTemplate, user, type, service }
                    );
                    const attempt = yield AttemptModel.findById(2);

                    yield CertificateModel.create(attempt);

                    const certificate = yield Certificate.findOne({
                        where: { trialId: 2 },
                        attributes: [
                            'trialId',
                            'typeId',
                            'confirmedDate',
                            'dueDate',
                            'active',
                            'confirmed',
                            'firstname',
                            'lastname',
                            'imagePath'
                        ]
                    });

                    const actual = certificate.toJSON();

                    expect(actual.trialId).to.equal(2);
                    expect(actual.typeId).to.equal(13);
                    expect(actual.firstname).to.equal('Petr');
                    expect(actual.lastname).to.equal('Ivanov');
                    expect(actual.confirmedDate).to.deep.equal(finished);
                    expect(actual.dueDate).to.deep.equal(new Date(finished.setYear(2019)));
                    expect(actual.active).to.equal(1);
                    expect(actual.confirmed).to.equal(1);
                    expect(actual.imagePath).to.equal('603/1468925144742_555555');
                });

                it('should create new certificate direct base', function *() {
                    const finished = new Date(2016, 6, 11);
                    const trial = { id: 2, finished, passed: 1 };
                    const trialTemplate = { id: 3, validityPeriod: '1y', isProctoring: false };
                    const user = {
                        id: 14,
                        firstname: 'Petr',
                        lastname: 'Ivanov'
                    };
                    const type = { id: 13, code: 'cert' };
                    const service = { code: 'direct_base' };

                    yield trialsFactory.createWithRelations(
                        trial,
                        { trialTemplate, user, type, service }
                    );
                    const attempt = yield AttemptModel.findById(2);

                    yield CertificateModel.create(attempt);

                    const certificate = yield Certificate.findOne({
                        where: { trialId: 2 },
                        attributes: [
                            'trialId',
                            'typeId',
                            'confirmedDate',
                            'dueDate',
                            'active',
                            'confirmed',
                            'firstname',
                            'lastname',
                            'imagePath'
                        ]
                    });

                    const actual = certificate.toJSON();

                    expect(actual.trialId).to.equal(2);
                    expect(actual.typeId).to.equal(13);
                    expect(actual.firstname).to.equal('Petr');
                    expect(actual.lastname).to.equal('Ivanov');
                    expect(actual.confirmedDate).to.deep.equal(finished);
                    expect(actual.dueDate).to.deep.equal(new Date(finished.setYear(2017)));
                    expect(actual.active).to.equal(1);
                    expect(actual.confirmed).to.equal(1);
                    expect(actual.imagePath).to.equal('603/1468925144742_555555');
                });

                it('should create new certificate direct pro', function *() {
                    const finished = new Date(2016, 6, 11);
                    const trial = { id: 2, finished, passed: 1 };
                    const trialTemplate = { id: 3, validityPeriod: '1y', isProctoring: false };
                    const user = {
                        id: 14,
                        firstname: 'Petr',
                        lastname: 'Ivanov'
                    };
                    const type = { id: 13, code: 'cert' };
                    const service = { code: 'direct_pro' };

                    yield trialsFactory.createWithRelations(
                        trial,
                        { trialTemplate, user, type, service }
                    );
                    const attempt = yield AttemptModel.findById(2);

                    yield CertificateModel.create(attempt);

                    const certificate = yield Certificate.findOne({
                        where: { trialId: 2 },
                        attributes: [
                            'trialId',
                            'typeId',
                            'confirmedDate',
                            'dueDate',
                            'active',
                            'confirmed',
                            'firstname',
                            'lastname',
                            'imagePath'
                        ]
                    });

                    const actual = certificate.toJSON();

                    expect(actual.trialId).to.equal(2);
                    expect(actual.typeId).to.equal(13);
                    expect(actual.firstname).to.equal('Petr');
                    expect(actual.lastname).to.equal('Ivanov');
                    expect(actual.confirmedDate).to.deep.equal(finished);
                    expect(actual.dueDate).to.deep.equal(new Date(finished.setYear(2017)));
                    expect(actual.active).to.equal(1);
                    expect(actual.confirmed).to.equal(1);
                    expect(actual.imagePath).to.equal('603/1468925144742_555555');
                });
            });
        });

        it('should create new certificate', function *() {
            const finished = new Date(2016, 6, 11);
            const trial = { id: 2, finished, passed: 1 };
            const trialTemplate = { id: 3, validityPeriod: '1y', isProctoring: false };
            const user = {
                id: 14,
                firstname: 'Petr',
                lastname: 'Ivanov'
            };
            const type = { id: 13, code: 'cert' };
            const service = { code: 'direct' };

            yield trialsFactory.createWithRelations(
                trial,
                { trialTemplate, user, type, service }
            );
            const attempt = yield AttemptModel.findById(2);

            yield CertificateModel.create(attempt);

            const certificate = yield Certificate.findOne({
                where: { trialId: 2 },
                attributes: [
                    'trialId',
                    'typeId',
                    'confirmedDate',
                    'dueDate',
                    'active',
                    'confirmed',
                    'firstname',
                    'lastname',
                    'imagePath'
                ]
            });

            const actual = certificate.toJSON();

            expect(actual.trialId).to.equal(2);
            expect(actual.typeId).to.equal(13);
            expect(actual.firstname).to.equal('Petr');
            expect(actual.lastname).to.equal('Ivanov');
            expect(actual.confirmedDate).to.deep.equal(finished);
            expect(actual.dueDate).to.deep.equal(new Date(finished.setYear(2017)));
            expect(actual.active).to.equal(1);
            expect(actual.confirmed).to.equal(1);
            expect(actual.imagePath).to.equal('603/1468925144742_555555');
        });

        it('should create certificate, upload it and fill `imagePath`', function *() {
            const finished = new Date(2016, 6, 11);
            const trial = { id: 2, finished, passed: 1 };
            const trialTemplate = { id: 3, validityPeriod: '1y', isProctoring: false };
            const type = { id: 1, code: 'cert' };
            const service = { code: 'direct' };

            yield trialsFactory.createWithRelations(
                trial,
                { trialTemplate, type, service }
            );
            const attempt = yield AttemptModel.findById(2);

            yield CertificateModel.create(attempt);

            const cert = yield Certificate.findOne({
                where: { trialId: 2 },
                attributes: ['imagePath']
            });

            expect(cert.imagePath).to.equal('603/1468925144742_555555');
        });

        it('should save image path to db when it is achievement', function *() {
            const finished = new Date(2016, 6, 11);
            const trial = { id: 2, finished, passed: 1 };
            const trialTemplate = { id: 3, validityPeriod: '1y', isProctoring: false };
            const type = { id: 1, code: 'achievement' };
            const service = { code: 'shim' };

            yield trialsFactory.createWithRelations(
                trial,
                { trialTemplate, type, service }
            );
            const attempt = yield AttemptModel.findById(2);

            yield CertificateModel.create(attempt);

            const cert = yield Certificate.findOne({
                where: { trialId: 2 },
                attributes: ['imagePath']
            });

            expect(cert.imagePath).to.equal(achievements.shim);
        });

        describe('with proctoring', () => {
            it('should create certificate with `dueDate` from proctoring response', function *() {
                const verdictTime = new Date(2016, 6, 14);

                yield trialsFactory.createWithRelations(
                    { id: 2, finished: new Date(2016, 6, 11), passed: 1 },
                    {
                        trialTemplate: { id: 3, validityPeriod: '1y', isProctoring: true },
                        user: { id: 14 },
                        type: { id: 1 }
                    }
                );
                yield proctoringResponsesFactory.create({
                    trialId: 2,
                    verdict: 'correct',
                    isLast: true,
                    time: verdictTime
                });

                const attempt = yield AttemptModel.findById(2);

                yield CertificateModel.create(attempt);

                const actual = yield Certificate.findOne({
                    where: { trialId: 2 },
                    attributes: [
                        'trialId',
                        'typeId',
                        'confirmedDate',
                        'dueDate',
                        'active',
                        'confirmed'
                    ],
                    raw: true
                });
                const expectedDueDate = new Date(new Date(verdictTime).setYear(2017));
                const expected = {
                    trialId: 2,
                    typeId: 1,
                    confirmedDate: verdictTime,
                    dueDate: expectedDueDate,
                    active: 1,
                    confirmed: 1
                };

                expect(actual).to.deep.equal(expected);
            });
        });

    });

    describe('`findAll`', () => {
        before(nockAvatars.success);
        after(nock.cleanAll);

        const user = {
            id: 12,
            uid: 1234567890,
            firstname: 'Petya',
            lastname: 'Petrov'
        };
        const authType = { id: 2, code: 'web' };
        const trialTemplate = { id: 3, previewImagePath: 'path/to/preview/image', slug: 'direct' };
        const finished = new Date(2016, 6, 11);
        const service = {
            id: 12,
            code: 'direct',
            title: 'Yandex.Direct'
        };
        const firstCertificateData = {
            certId: 13,
            certType: 'cert',
            dueDate: new Date(2017, 6, 11),
            firstname: 'Vasya',
            lastname: 'Pupkin',
            active: 1,
            confirmedDate: finished,
            imagePath: MdsModel.getAvatarsPath('255/38472434872_13'),
            service,
            previewImagePath: MdsModel.getAvatarsPath('path/to/preview/image'),
            exam: { id: 3, slug: 'direct' }
        };
        const secondCertificateData = {
            certId: 14,
            certType: 'test',
            dueDate: new Date(2019, 5, 12),
            firstname: 'Vasya',
            lastname: 'Pupkin',
            active: 1,
            confirmedDate: new Date(2018, 5, 12),
            imagePath: MdsModel.getAvatarsPath('255/38472434872_14'),
            service,
            previewImagePath: MdsModel.getAvatarsPath('path/to/preview/image'),
            exam: { id: 3, slug: 'direct' }
        };
        const type = { id: 1, code: 'cert' };

        beforeEach(function *() {
            const trial = {
                id: 2,
                finished,
                passed: 1,
                nullified: 0
            };
            const certificate = {
                id: 13,
                dueDate: new Date(2017, 6, 11),
                firstname: 'Vasya',
                lastname: 'Pupkin',
                active: 1,
                confirmedDate: finished,
                imagePath: '255/38472434872_13'
            };

            yield certificateFactory.createWithRelations(
                certificate,
                { trial, user, trialTemplate, service, type, authType }
            );
        });

        it('should return correct `firstname` and `lastname` fields when user exists in db', function *() {
            const otherUser = {
                id: 15,
                uid: 9876543210,
                firstname: 'Misha',
                lastname: 'Laptev'
            };
            const trial = {
                id: 6,
                finished: new Date(2013, 6, 11),
                passed: 0,
                nullified: 0
            };

            yield trialsFactory.createWithRelations(
                trial,
                { trialTemplate, service, user: otherUser, authType }
            );

            const actual = yield CertificateModel.findAll(otherUser.uid);

            expect(actual.firstname).to.equal('Misha');
            expect(actual.lastname).to.equal('Laptev');
            expect(actual.certificates.length).to.equal(0);
        });

        it('should find one certificate for one exam when one passed trial', function *() {
            const actual = yield CertificateModel.findAll(user.uid);

            const certificates = [firstCertificateData];

            expect(actual.firstname).to.equal('Petya');
            expect(actual.lastname).to.equal('Petrov');
            expect(actual.certificates.length).to.equal(1);
            expect(actual.certificates).to.deep.equal(certificates);
        });

        it('should not return certificate for nullified trial', function *() {
            const otherUser = {
                id: 23,
                uid: 98765432101,
                firstname: 'Misha',
                lastname: 'Laptev'
            };
            const failedTrial = {
                id: 2,
                finished: new Date(2015, 3, 3),
                passed: 1,
                nullified: 1
            };
            const failedCertificate = {
                id: 15,
                dueDate: new Date(2016, 1, 1),
                firstname: 'Vasya',
                lastname: 'Pupkin',
                active: 1,
                confirmedDate: new Date(2015, 3, 3),
                imagePath: '255/38472434872_15'
            };

            yield certificateFactory.createWithRelations(
                failedCertificate,
                { trial: failedTrial, user: otherUser, trialTemplate, service, authType }
            );

            const actual = yield CertificateModel.findAll(otherUser.uid);

            expect(actual.firstname).to.equal('Misha');
            expect(actual.lastname).to.equal('Laptev');
            expect(actual.certificates).to.deep.equal([]);
        });

        it('should not return certificate when cert is not active', function *() {
            const otherUser = { id: 23, uid: 98765432101 };
            const cert = { id: 15, active: 0 };

            yield certificateFactory.createWithRelations(cert, { trial: { nullified: 0 }, user: otherUser, authType });

            const actual = yield CertificateModel.findAll(otherUser.uid);

            expect(actual.certificates).to.deep.equal([]);
        });

        it('should find all certificates for one exam', function *() {
            const otherTrial = {
                id: 4,
                finished: new Date(2018, 5, 12),
                passed: 1,
                nullified: 0
            };
            const certificate = {
                id: 14,
                dueDate: new Date(2019, 5, 12),
                firstname: 'Vasya',
                lastname: 'Pupkin',
                active: 1,
                confirmedDate: new Date(2018, 5, 12),
                imagePath: '255/38472434872_14'
            };
            const failedTrial = {
                id: 12,
                finished: new Date(2020, 5, 12),
                passed: 0,
                nullified: 0
            };

            yield trialsFactory.createWithRelations(
                failedTrial,
                { trialTemplate, service, user, authType }
            );

            yield certificateFactory.createWithRelations(
                certificate,
                { trial: otherTrial, user, trialTemplate, service, authType, type }
            );

            const actual = yield CertificateModel.findAll(user.uid);

            const secondCertData = _.cloneDeep(secondCertificateData);

            secondCertData.certType = 'cert';

            const certificates = [
                secondCertData,
                firstCertificateData
            ];

            expect(actual.firstname).to.equal('Petya');
            expect(actual.lastname).to.equal('Petrov');
            expect(actual.certificates.length).to.equal(2);
            expect(actual.certificates).to.deep.equal(certificates);
        });

        it('should find all certificates and achievements for all exams', function *() {
            const otherTrialTemplate = { id: 9, slug: 'metrika' };
            const otherType = { id: 2, code: 'test' };
            const otherService = {
                id: 15,
                code: 'metrika',
                title: 'Yandex.Metrika'
            };
            const otherTrial = {
                id: 8,
                finished: new Date(2018, 5, 12),
                passed: 1,
                nullified: 0
            };
            const otherCertificate = {
                id: 14,
                dueDate: new Date(2019, 5, 12),
                firstname: 'Vasya',
                lastname: 'Pupkin',
                active: 1,
                confirmedDate: new Date(2018, 5, 12),
                imagePath: '255/38472434872_14'
            };

            yield certificateFactory.createWithRelations(
                otherCertificate,
                {
                    trial: otherTrial,
                    trialTemplate: otherTrialTemplate,
                    service: otherService,
                    user,
                    type: otherType,
                    authType
                }
            );

            const otherCertificateData = _.cloneDeep(secondCertificateData);

            otherCertificateData.service = otherService;
            otherCertificateData.previewImagePath = '';
            otherCertificateData.exam = { id: 9, slug: 'metrika' };

            const actual = yield CertificateModel.findAll(user.uid);
            const certificates = [
                otherCertificateData,
                firstCertificateData
            ];

            expect(actual.firstname).to.equal('Petya');
            expect(actual.lastname).to.equal('Petrov');
            expect(actual.certificates.length).to.equal(2);
            expect(actual.certificates).to.deep.equal(certificates);
        });

        it('should return 2 certificates when user passed new trial in `periodBeforeCertificateReset`', function *() {
            const now = moment(Date.now()).toDate();
            const dueDate = moment(now).add(1, 'month').toDate();
            const otherFinished = moment(now).subtract(5, 'month').toDate();
            const otherTrialTemplate = {
                id: 567,
                periodBeforeCertificateReset: '1M',
                delays: '2M, 2M, 2M',
                timeLimit: 90000,
                validityPeriod: '6M'
            };
            const otherUser = { id: 27, uid: 123628123746 };
            const firstTrial = {
                id: 6,
                passed: 1,
                started: moment(otherFinished).subtract(1, 'hour').toDate(),
                finished: otherFinished,
                expired: 1,
                nullified: 0
            };
            const secondTrial = {
                id: 7,
                started: moment(now).subtract(1, 'hour').toDate(),
                nullified: 0
            };
            const firstCert = {
                id: 17,
                dueDate,
                confirmedDate: otherFinished,
                active: 1
            };

            yield certificateFactory.createWithRelations(
                firstCert,
                { trial: firstTrial, trialTemplate: otherTrialTemplate, user: otherUser, service, authType }
            );

            yield trialsFactory.createWithRelations(
                secondTrial,
                { trialTemplate: otherTrialTemplate, user: otherUser, authType }
            );

            const section = { id: 3 };
            const question = { id: 4 };

            yield trialToQuestionsFactory.createWithRelations(
                { seq: 2, answered: 1, correct: 1 },
                { trial: secondTrial, question, section, trialTemplate: otherTrialTemplate }
            );

            yield trialTemplateToSectionsFactory.createWithRelations(
                { quantity: 1 },
                { trialTemplate: otherTrialTemplate, section }
            );
            yield trialTemplateAllowedFailsFactory.createWithRelations(
                { allowedFails: 0 },
                { trialTemplate: otherTrialTemplate, section }
            );

            const trialItem = yield Trial.findById(7);
            const attempt = new AttemptModel(trialItem);

            yield attempt.finish();

            const actual = yield CertificateModel.findAll(otherUser.uid);

            expect(actual.certificates.length).to.equal(2);
            expect(actual.certificates[0].active).to.equal(1);
            expect(actual.certificates[1].active).to.equal(1);
        });

        it('should return 1 active certificate when user failed new trial', function *() {
            const now = moment(Date.now()).toDate();
            const dueDate = moment(now).add(1, 'month').toDate();
            const otherFinished = moment(now).subtract(5, 'month').toDate();
            const otherTrialTemplate = {
                id: 567,
                periodBeforeCertificateReset: '1M',
                delays: '2M, 2M, 2M',
                timeLimit: 90000,
                validityPeriod: '6M'
            };
            const otherUser = { id: 45, uid: 123628123746 };
            const firstTrial = {
                id: 6,
                passed: 1,
                started: moment(otherFinished).subtract(1, 'hour').toDate(),
                finished: otherFinished,
                expired: 1,
                nullified: 0
            };
            const secondTrial = {
                id: 7,
                started: moment(now).subtract(1, 'hour').toDate(),
                nullified: 0
            };
            const firstCert = {
                id: 17,
                dueDate,
                confirmedDate: otherFinished,
                active: 1
            };

            yield certificateFactory.createWithRelations(
                firstCert,
                { trial: firstTrial, trialTemplate: otherTrialTemplate, user: otherUser, service, authType }
            );

            yield trialsFactory.createWithRelations(
                secondTrial,
                { trialTemplate: otherTrialTemplate, user: otherUser, authType }
            );

            const section = { id: 3 };
            const question = { id: 4 };

            yield trialToQuestionsFactory.createWithRelations(
                { seq: 2, answered: 1, correct: 0 },
                { trial: secondTrial, question, section, trialTemplate: otherTrialTemplate }
            );

            yield trialTemplateToSectionsFactory.createWithRelations(
                { quantity: 1 },
                { trialTemplate: otherTrialTemplate, section }
            );
            yield trialTemplateAllowedFailsFactory.createWithRelations(
                { allowedFails: 0 },
                { trialTemplate: otherTrialTemplate, section }
            );

            const trialItem = yield Trial.findById(7);
            const attempt = new AttemptModel(trialItem);

            yield attempt.finish();

            const actual = yield CertificateModel.findAll(otherUser.uid);

            expect(actual.certificates.length).to.equal(1);
            expect(actual.certificates[0].active).to.equal(1);
        });

        it('should draw and put certificate when `imagePath` is null', function *() {
            const otherTrial = {
                id: 4,
                finished: new Date(2018, 5, 12),
                passed: 1,
                nullified: 0,
                isProctoring: false
            };
            const certificate = {
                id: 14,
                dueDate: new Date(2019, 5, 12),
                firstname: 'Vasya',
                lastname: 'Pupkin',
                active: 1,
                confirmedDate: new Date(2018, 5, 12),
                imagePath: null
            };

            yield certificateFactory.createWithRelations(
                certificate,
                { trial: otherTrial, user, trialTemplate, service, authType, type }
            );

            const certBefore = yield Certificate.findOne({
                attributes: ['imagePath', 'typeId'],
                where: { id: certificate.id }
            });

            expect(certBefore.imagePath).to.be.null;
            expect(certBefore.typeId).to.equal(1);

            const actual = yield CertificateModel.findAll(user.uid);

            expect(actual.certificates.length).to.equal(2);

            const certAfter = yield Certificate.findOne({
                attributes: ['imagePath', 'typeId'],
                where: { id: certificate.id }
            });

            expect(certAfter.imagePath).to.equal('603/1468925144742_555555');
            expect(certAfter.typeId).to.equal(1);
        });

        it('should not return certificates when user does not exist in db', function *() {
            const error = yield catchError(CertificateModel.findAll.bind(CertificateModel, 1029384756));

            expect(error.statusCode).to.equal(404);
            expect(error.options).to.deep.equal({ internalCode: '404_UNF' });
            expect(error.message).to.equal('User not found');
        });

        it('should throw 404 when auth type is not `web`', function *() {
            yield usersFactory.createWithRelations({ id: 76, uid: 243545345 }, { authType: { code: 'telegram' } });

            const error = yield catchError(CertificateModel.findAll.bind(CertificateModel, 243545345));

            expect(error.statusCode).to.equal(404);
            expect(error.options).to.deep.equal({ internalCode: '404_UNF' });
            expect(error.message).to.equal('User not found');
        });
    });

    describe('`getMyCertificates`', () => {
        before(nockAvatars.success);
        after(nock.cleanAll);

        const getAttemptInfo = AttemptModel.getInfo.bind(AttemptModel);
        const user = { id: 23, uid: 1234567890 };
        const authType = { id: 2, code: 'web' };
        const trialTemplate = {
            id: 3,
            delays: '1M, 1M, 1M',
            timeLimit: 90000,
            slug: 'direct',
            periodBeforeCertificateReset: '1M',
            previewImagePath: 'path/to/preview/image',
            isProctoring: false
        };
        const type = { id: 1, code: 'cert' };
        const finished = new Date(2015, 5, 11);
        const service = {
            id: 12,
            code: 'direct',
            title: 'Yandex.Direct'
        };
        const trial = {
            id: 2,
            started: moment(finished).subtract(1, 'hour').toDate(),
            finished,
            passed: 1,
            nullified: 0,
            expired: 1
        };
        const certificate = {
            id: 13,
            dueDate: new Date(2016, 1, 1),
            firstname: 'Vasya',
            lastname: 'Pupkin',
            active: 1,
            confirmedDate: finished,
            imagePath: '255/38472434872_13'
        };
        const firstCertificateData = {
            certId: 13,
            certType: 'cert',
            dueDate: new Date(2016, 1, 1),
            firstname: 'Vasya',
            lastname: 'Pupkin',
            active: 1,
            confirmedDate: finished,
            imagePath: MdsModel.getAvatarsPath('255/38472434872_13'),
            previewImagePath: MdsModel.getAvatarsPath('path/to/preview/image'),
            service,
            exam: {
                slug: 'direct',
                id: 3
            },
            check: {
                state: 'enabled',
                hasValidCert: false
            }
        };

        beforeEach(function *() {
            yield certificateFactory.createWithRelations(
                certificate,
                { trial, user, trialTemplate, service, type, authType }
            );
        });

        it('should return [] when user has not certificates', function *() {
            const actual = yield CertificateModel.getMyCertificates(10293847561029, 'vasya', getAttemptInfo);

            expect(actual).to.deep.equal([]);
        });

        it('should get one certificate for one exam when one passed trial', function *() {
            const actual = yield CertificateModel.getMyCertificates(1234567890, 'vasya', getAttemptInfo);

            expect(actual.length).to.equal(1);
            expect(actual[0]).to.deep.equal(firstCertificateData);
        });

        it('should return [] when user`s auth type is not `web`', function *() {
            const otherUser = { id: 7583, uid: 2324254 };
            const otherAuthType = { id: 23, code: 'telegram' };
            const otherTrial = {
                id: 54,
                started: moment(finished).subtract(1, 'hour').toDate(),
                finished,
                passed: 1,
                nullified: 0,
                expired: 1
            };

            yield certificateFactory.createWithRelations(
                {
                    id: 78,
                    dueDate: new Date(2016, 1, 1),
                    firstname: 'Vasya',
                    lastname: 'Pupkin',
                    active: 1,
                    confirmedDate: finished,
                    imagePath: '255/38472434872_13'
                },
                { trial: otherTrial, user: otherUser, authType: otherAuthType, trialTemplate, service, type }
            );

            const actual = yield CertificateModel.getMyCertificates(2324254, 'vasya', getAttemptInfo);

            expect(actual).to.deep.equal([]);
        });

        it('should return [] when cert is not active', function *() {
            const otherUser = { id: 73659, uid: 98765432101 };
            const cert = { id: 78, active: 0 };

            yield certificateFactory.createWithRelations(cert, { trial: { nullified: 0 }, user: otherUser, authType });

            const actual = yield CertificateModel.getMyCertificates(otherUser.uid, 'vasya', getAttemptInfo);

            expect(actual).to.deep.equal([]);
        });

        it('should get all certificates for one exam', function *() {
            const now = moment(Date.now()).startOf('minute').toDate();
            const dueDate = moment(now).add(2, 'month').toDate();
            const availabilityDate = moment(dueDate)
                .subtract(1, 'month')
                .startOf('day')
                .toDate();

            const otherTrial = {
                id: 3,
                started: new Date(2016, 5, 4),
                finished: new Date(2016, 5, 5),
                passed: 1,
                nullified: 0,
                expired: 1
            };
            const otherCertificate = {
                id: 14,
                dueDate,
                firstname: 'Vasya',
                lastname: 'Pupkin',
                active: 1,
                confirmedDate: new Date(2016, 5, 5),
                imagePath: '255/38472434872_14'
            };
            const failedTrial = {
                id: 93,
                started: new Date(2016, 7, 4),
                finished: new Date(2016, 7, 5),
                passed: 1,
                nullified: 1,
                expired: 1
            };

            yield certificateFactory.createWithRelations(
                { id: 23 },
                { trial: failedTrial, user, trialTemplate, service, authType }
            );

            yield certificateFactory.createWithRelations(
                otherCertificate,
                { trial: otherTrial, user, trialTemplate, service, type, authType }
            );

            const actual = yield CertificateModel.getMyCertificates(user.uid, 'vasya', getAttemptInfo);

            const secondCertificateData = {
                certId: 14,
                certType: 'cert',
                dueDate,
                firstname: 'Vasya',
                lastname: 'Pupkin',
                active: 1,
                confirmedDate: new Date(2016, 5, 5),
                imagePath: MdsModel.getAvatarsPath('255/38472434872_14'),
                service,
                exam: {
                    slug: 'direct',
                    id: 3
                },
                check: {
                    state: 'disabled',
                    availabilityDate,
                    hasValidCert: true
                },
                previewImagePath: MdsModel.getAvatarsPath('path/to/preview/image')
            };

            const firstCertData = _.cloneDeep(firstCertificateData);

            firstCertData.check = {
                state: 'disabled',
                availabilityDate,
                hasValidCert: true
            };

            expect(actual.length).to.equal(2);
            expect(actual[0]).to.deep.equal(secondCertificateData);
            expect(actual[1]).to.deep.equal(firstCertData);
        });

        it('should get all certificates and achievements for all exams', function *() {
            const now = moment(Date.now()).startOf('minute').toDate();
            const dueDate = moment(now).add(3, 'month').toDate();
            const availabilityDate = moment(dueDate)
                .subtract(2, 'month')
                .startOf('day')
                .toDate();

            const otherService = {
                id: 15,
                code: 'metrika',
                title: 'Yandex.Metrika'
            };
            const otherType = {
                id: 2,
                code: 'test'
            };
            const otherTrialTemplate = {
                id: 4,
                delays: '1M, 1M, 1M',
                timeLimit: 90000,
                slug: 'metrika',
                periodBeforeCertificateReset: '2M'
            };
            const otherTrial = {
                id: 8,
                started: new Date(2016, 5, 11),
                finished: new Date(2016, 5, 12),
                passed: 1,
                expired: 1,
                nullified: 0
            };
            const otherCertificate = {
                id: 14,
                dueDate,
                firstname: 'Vasya',
                lastname: 'Pupkin',
                active: 1,
                confirmedDate: new Date(2016, 5, 12),
                imagePath: '255/38472434872_14'
            };

            yield certificateFactory.createWithRelations(
                otherCertificate,
                {
                    trial: otherTrial,
                    trialTemplate: otherTrialTemplate,
                    service: otherService,
                    user,
                    type: otherType,
                    authType
                }
            );

            const secondCertificateData = {
                certId: 14,
                certType: 'test',
                dueDate,
                firstname: 'Vasya',
                lastname: 'Pupkin',
                active: 1,
                confirmedDate: new Date(2016, 5, 12),
                imagePath: MdsModel.getAvatarsPath('255/38472434872_14'),
                service: otherService,
                exam: {
                    slug: 'metrika',
                    id: 4
                },
                check: {
                    state: 'disabled',
                    availabilityDate,
                    hasValidCert: true
                },
                previewImagePath: ''
            };

            const actual = yield CertificateModel.getMyCertificates(1234567890, 'vasya', getAttemptInfo);

            expect(actual.length).to.equal(2);
            expect(actual[0]).to.deep.equal(secondCertificateData);
            expect(actual[1]).to.deep.equal(firstCertificateData);
        });

        it('should return 2 certificates when user passed new trial in `periodBeforeCertificateReset`', function *() {
            const now = moment(Date.now()).toDate();
            const dueDate = moment(now).add(1, 'month').toDate();
            const otnerFinished = moment(now).subtract(5, 'month').toDate();
            const otherTrialTemplate = {
                id: 567,
                periodBeforeCertificateReset: '1M',
                delays: '2M, 2M, 2M',
                timeLimit: 90000,
                validityPeriod: '6M'
            };
            const otherUser = { id: 456, uid: 123628123746 };
            const firstTrial = {
                id: 6,
                passed: 1,
                started: moment(otnerFinished).subtract(1, 'hour').toDate(),
                finished: otnerFinished,
                expired: 1,
                nullified: 0
            };
            const secondTrial = {
                id: 7,
                started: moment(now).subtract(1, 'hour').toDate(),
                nullified: 0
            };
            const firstCert = {
                id: 17,
                dueDate,
                confirmedDate: otnerFinished,
                active: 1
            };

            yield certificateFactory.createWithRelations(
                firstCert,
                { trial: firstTrial, trialTemplate: otherTrialTemplate, user: otherUser, service, authType }
            );

            yield trialsFactory.createWithRelations(
                secondTrial,
                { trialTemplate: otherTrialTemplate, user: otherUser, authType }
            );

            const section = { id: 3 };
            const question = { id: 4 };

            yield trialToQuestionsFactory.createWithRelations(
                { seq: 2, answered: 1, correct: 1 },
                { trial: secondTrial, question, section, trialTemplate: otherTrialTemplate }
            );

            yield trialTemplateToSectionsFactory.createWithRelations(
                { quantity: 1 },
                { trialTemplate: otherTrialTemplate, section }
            );
            yield trialTemplateAllowedFailsFactory.createWithRelations(
                { allowedFails: 0 },
                { trialTemplate: otherTrialTemplate, section }
            );

            const trialItem = yield Trial.findById(7);
            const attempt = new AttemptModel(trialItem);

            yield attempt.finish();

            const actual = yield CertificateModel.getMyCertificates(otherUser.uid, 'vasya', getAttemptInfo);

            const availabilityDate = moment(now)
                .add(6, 'month')
                .subtract(1, 'month')
                .startOf('day')
                .toDate();

            expect(actual.length).to.equal(2);
            expect(actual[0].check.availabilityDate).to.deep.equal(availabilityDate);
            expect(actual[1].check.availabilityDate).to.deep.equal(availabilityDate);
            expect(actual[0].check.state).to.deep.equal('disabled');
            expect(actual[1].check.state).to.deep.equal('disabled');
            expect(actual[0].active).to.equal(1);
            expect(actual[1].active).to.equal(1);
        });

        it('should return 1 cert and correct `availabilityDate` when user failed new trial', function *() {
            const now = moment(Date.now()).toDate();
            const dueDate = moment(now).add(1, 'month').toDate();
            const otherFinished = moment(now).subtract(5, 'month').toDate();
            const otherTrialTemplate = {
                id: 567,
                periodBeforeCertificateReset: '1M',
                delays: '2M, 2M, 2M',
                timeLimit: 90000,
                validityPeriod: '6M'
            };
            const otherUser = { id: 456, uid: 123628123746 };
            const firstTrial = {
                id: 6,
                passed: 1,
                started: moment(otherFinished).subtract(1, 'hour').toDate(),
                finished: otherFinished,
                expired: 1,
                nullified: 0
            };
            const secondTrial = {
                id: 7,
                started: moment(now).subtract(1, 'hour').toDate(),
                nullified: 0
            };
            const firstCert = {
                id: 17,
                dueDate,
                confirmedDate: otherFinished,
                active: 1
            };

            yield certificateFactory.createWithRelations(
                firstCert,
                { trial: firstTrial, trialTemplate: otherTrialTemplate, user: otherUser, service, authType }
            );

            yield trialsFactory.createWithRelations(
                secondTrial,
                { trialTemplate: otherTrialTemplate, user: otherUser, authType }
            );

            const section = { id: 3 };
            const question = { id: 4 };

            yield trialToQuestionsFactory.createWithRelations(
                { seq: 2, answered: 1, correct: 0 },
                { trial: secondTrial, question, section, trialTemplate: otherTrialTemplate }
            );

            yield trialTemplateToSectionsFactory.createWithRelations(
                { quantity: 1 },
                { trialTemplate: otherTrialTemplate, section }
            );
            yield trialTemplateAllowedFailsFactory.createWithRelations(
                { allowedFails: 0 },
                { trialTemplate: otherTrialTemplate, section }
            );

            const trialItem = yield Trial.findById(7);
            const attempt = new AttemptModel(trialItem);

            yield attempt.finish();

            const actual = yield CertificateModel.getMyCertificates(otherUser.uid, 'vasya', getAttemptInfo);

            const availabilityDate = moment(now)
                .add(2, 'month')
                .startOf('day')
                .toDate();

            expect(actual.length).to.equal(1);
            expect(actual[0].check.availabilityDate).to.deep.equal(availabilityDate);
            expect(actual[0].check.state).to.equal('disabled');
            expect(actual[0].active).to.equal(1);
        });

        it('should draw and put certificate when `imagePath` is null', function *() {
            const otherTrial = {
                id: 4,
                started: new Date(2018, 5, 11),
                finished: new Date(2018, 5, 12),
                passed: 1,
                nullified: 0
            };
            const otherCertificate = {
                id: 14,
                dueDate: new Date(2019, 5, 12),
                firstname: 'Vasya',
                lastname: 'Pupkin',
                active: 1,
                confirmedDate: new Date(2018, 5, 12),
                imagePath: null
            };

            yield certificateFactory.createWithRelations(
                otherCertificate,
                { trial: otherTrial, user, trialTemplate, service, authType, type }
            );

            const certBefore = yield Certificate.findOne({
                attributes: ['imagePath', 'typeId'],
                where: { id: otherCertificate.id }
            });

            expect(certBefore.imagePath).to.be.null;
            expect(certBefore.typeId).to.equal(1);

            const actual = yield CertificateModel.getMyCertificates(user.uid, 'vasya', getAttemptInfo);

            expect(actual.length).to.equal(2);

            const certAfter = yield Certificate.findOne({
                attributes: ['imagePath', 'typeId'],
                where: { id: otherCertificate.id }
            });

            expect(certAfter.imagePath).to.equal('603/1468925144742_555555');
            expect(certAfter.typeId).to.equal(1);
        });
    });

    describe('`getPdf`', () => {
        before(nockMds.pdf);
        after(nock.cleanAll);

        const user = { uid: 1234567890 };
        const authType = { code: 'web' };
        const trialTemplate = {
            id: 3,
            delays: '1M, 1M, 1M',
            timeLimit: 90000,
            slug: 'direct',
            periodBeforeCertificateReset: '1M',
            previewImagePath: 'path/to/preview/image',
            isProctoring: false
        };
        const type = { id: 1, code: 'cert' };
        const finished = new Date(2015, 5, 11);
        const service = {
            id: 12,
            code: 'direct',
            title: 'Yandex.Direct'
        };
        const trial = {
            id: 2,
            started: moment(finished).subtract(1, 'hour').toDate(),
            finished,
            passed: 1,
            nullified: 0,
            expired: 1
        };
        const certificate = {
            id: 13,
            dueDate: new Date(2016, 1, 1),
            firstname: 'Vasya',
            lastname: 'Pupkin',
            active: 1,
            confirmedDate: finished,
            imagePath: '255/38472434872_13',
            pdfPath: null
        };

        beforeEach(function *() {
            yield certificateFactory.createWithRelations(
                certificate,
                { trial, user, trialTemplate, service, type, authType }
            );
        });

        it('should return PDF file from MDS if `pdfPath` exists', function *() {
            const certData = yield Certificate.findOne({
                where: { id: 13 }
            });

            certData.pdfPath = '255/38472434872_13.pdf';
            yield certData.save();

            const actual = yield CertificateModel.getPdf('13', 1234567890);

            expect(actual).to.not.be.empty;
        });

        it('should return generated PDF file if `pdfPath` is empty', function *() {
            const actual = yield CertificateModel.getPdf(13, '1234567890');

            expect(actual).to.not.be.empty;
            expect(actual).to.be.an.instanceof(Buffer);
        });

        it('should throw 403 when cert was nullified', function *() {
            const cert = { id: 8465, active: 0 };
            const otherType = { code: 'cert' };
            const otherUser = { id: 7456, uid: 236467833 };

            yield certificateFactory.createWithRelations(
                cert,
                { trial: { nullified: 0 }, type: otherType, user: otherUser, authType }
            );

            const error = yield catchError(CertificateModel.getPdf.bind(CertificateModel, 8465, otherUser.uid));

            expect(error.statusCode).to.equal(403);
            expect(error.options).to.deep.equal({ internalCode: '403_CWN' });
            expect(error.message).to.equal('Certificate was nullified');
        });

        it('should throw 404 when `uid` is wrong', function *() {
            const error = yield catchError(CertificateModel.getPdf.bind(CertificateModel, 13, 1234567890123456));

            expect(error.statusCode).to.equal(404);
            expect(error.options).to.deep.equal({ internalCode: '404_CNF' });
            expect(error.message).to.equal('Certificate not found');
        });

        it('should throw 404 when `certId` is wrong', function *() {
            const error = yield catchError(CertificateModel.getPdf.bind(CertificateModel, 131313, 1234567890));

            expect(error.statusCode).to.equal(404);
            expect(error.options).to.deep.equal({ internalCode: '404_CNF' });
            expect(error.message).to.equal('Certificate not found');
        });
    });

    describe('`findCertsByUids`', () => {
        beforeEach(dbHelper.clear);

        const now = new Date();
        const firstUser = { id: 1, uid: 123 };
        const secondUser = { id: 2, uid: 345 };
        const authType = { code: 'web' };
        const trialTemplate = { id: 3, slug: 'direct' };
        const service = { id: 12, code: 'direct', title: 'Yandex.Direct' };
        const type = { code: 'cert' };
        const firstTrial = { nullified: 0 };
        const secondTrial = { nullified: 0 };
        const firstCertificate = {
            id: 13,
            dueDate: moment(now).subtract(1, 'y'),
            firstname: 'Vasya',
            lastname: 'Pupkin',
            active: 1,
            confirmedDate: moment(now).subtract(2, 'y'),
            imagePath: '255/38472434872_13'
        };
        const secondCertificate = {
            id: 14,
            dueDate: moment(now).add(1, 'y'),
            firstname: 'Vasilisa',
            lastname: 'Pupkina',
            active: 1,
            confirmedDate: moment(now).subtract(1, 'd'),
            imagePath: '255/38472434872_14'
        };

        const expectedCertData = {
            certId: 14,
            certType: 'cert',
            dueDate: moment(now).add(1, 'y').toDate(),
            firstname: 'Vasilisa',
            lastname: 'Pupkina',
            active: 1,
            confirmedDate: moment(now).subtract(1, 'd').toDate(),
            imagePath: MdsModel.getAvatarsPath('255/38472434872_14'),
            previewImagePath: '',
            service,
            exam: { id: 3, slug: 'direct' }
        };

        it('should return empty array for user without certificates', function *() {
            const actual = yield CertificateModel.findCertsByUids([123]);

            expect(actual).to.deep.equal({ 123: [] });
        });

        it('should return only actual certs', function *() {
            yield certificateFactory.createWithRelations(
                firstCertificate,
                { trial: firstTrial, user: firstUser, trialTemplate, service, type, authType }
            );
            yield certificateFactory.createWithRelations(
                secondCertificate,
                { trial: secondTrial, user: firstUser, trialTemplate, service, type, authType }
            );

            const actual = yield CertificateModel.findCertsByUids([123]);

            expect(actual).to.deep.equal({
                123: [expectedCertData]
            });
        });

        it('should group certs by user', function *() {
            const otherCertificate = {
                id: 15,
                dueDate: moment(now).add(2, 'y'),
                firstname: 'Vasilisa',
                lastname: 'Pupkina',
                active: 1,
                confirmedDate: moment(now).subtract(2, 'd'),
                imagePath: '255/38472434872_15'
            };
            const otherExpectedCertData = {
                certId: 15,
                certType: 'cert',
                dueDate: moment(now).add(2, 'y').toDate(),
                firstname: 'Vasilisa',
                lastname: 'Pupkina',
                active: 1,
                confirmedDate: moment(now).subtract(2, 'd').toDate(),
                imagePath: MdsModel.getAvatarsPath('255/38472434872_15'),
                previewImagePath: '',
                service,
                exam: { id: 3, slug: 'direct' }
            };

            yield certificateFactory.createWithRelations(
                secondCertificate,
                { trial: firstTrial, user: firstUser, trialTemplate, service, type, authType }
            );
            yield certificateFactory.createWithRelations(
                otherCertificate,
                { trial: secondTrial, user: secondUser, trialTemplate, service, type, authType }
            );

            const actual = yield CertificateModel.findCertsByUids([123, 345]);

            expect(actual).to.deep.equal({
                123: [expectedCertData],
                345: [otherExpectedCertData]
            });
        });
    });

    describe('`nullify`', () => {
        it('should nullify several certificate by id', function *() {
            const now = Date.now();

            yield [123, 456, 789].map(id => certificateFactory.createWithRelations(
                { id, active: 1, deactivateReason: null, deactivateDate: null },
                {}
            ));

            yield CertificateModel.nullify([123, 789], 'rules');

            const actual = yield Certificate.findAll({
                attributes: ['active', 'deactivateReason', 'deactivateDate'],
                order: [['id']],
                raw: true
            });

            expect(actual.length).to.equal(3);

            expect(actual[0].active).to.equal(0);
            expect(actual[0].deactivateReason).to.equal('rules');
            expect(actual[0].deactivateDate).to.be.at.least(now);

            expect(actual[1].active).to.equal(1);
            expect(actual[1].deactivateReason).to.be.null;
            expect(actual[1].deactivateDate).to.be.null;

            expect(actual[2].active).to.equal(0);
            expect(actual[2].deactivateReason).to.equal('rules');
            expect(actual[2].deactivateDate).to.be.at.least(now);
        });

        it('should work correctly when there is no certificate with a specified id', function *() {
            const certificate = { id: 123, active: 1, deactivateReason: null, deactivateDate: null };

            yield certificateFactory.createWithRelations(certificate);

            yield CertificateModel.nullify([456], 'rules');

            const actual = yield Certificate.findAll({
                attributes: ['id', 'active', 'deactivateReason', 'deactivateDate'],
                raw: true
            });

            expect(actual).to.deep.equal([certificate]);
        });
    });

    describe('`getUsersForNullifyCerts`', () => {
        it('should get correct data by cert ids', function *() {
            const firstUser = { id: 1, uid: 111 };
            const direct = { id: 13, language: 0 };
            const directService = { id: 1, code: 'direct' };
            const metrika = { id: 14, language: 1 };
            const metrikaService = { id: 2, code: 'metrika_en' };

            yield certificateFactory.createWithRelations({ id: 34 }, {
                user: firstUser,
                trialTemplate: direct,
                service: directService
            });
            yield certificateFactory.createWithRelations({ id: 12 }, {
                user: firstUser,
                trialTemplate: metrika,
                service: metrikaService
            });
            yield certificateFactory.createWithRelations({ id: 56 }, {
                user: { id: 2, uid: 222 },
                trialTemplate: direct,
                service: directService
            });
            yield certificateFactory.createWithRelations({ id: 78 }, {
                user: { id: 3, uid: 333 },
                trialTemplate: metrika,
                service: metrikaService
            });

            const actual = yield CertificateModel.getUsersForNullifyCerts([12, 34, 56]);

            const expected = [
                {
                    uid: 111,
                    services: [
                        { code: 'metrika_en', language: 'en' },
                        { code: 'direct', language: 'ru' }
                    ]
                },
                {
                    uid: 222,
                    services: [{ code: 'direct', language: 'ru' }]
                }
            ];

            expect(actual).to.deep.equal(expected);
        });

        it('should return [] when there are no certificates with the specified id', function *() {
            yield trialsFactory.createWithRelations(
                { id: 13, passed: 0 },
                { user: { id: 3, uid: 123 }, trialTemplate: { id: 12 }, service: { code: 'market' } }
            );

            const actual = yield CertificateModel.getUsersForNullifyCerts([12, 34]);

            expect(actual).to.deep.equal([]);
        });
    });

    describe('`getCertsByGlobalUser`', () => {
        const now = new Date();

        it('should return only not nullified certificates', function *() {
            const dueDate = moment(now).add(1, 'year').toDate();
            const user = { id: 3, uid: 123, login: 'midyac' };
            const globalUser = { id: 10 };

            yield certificateFactory.createWithRelations(
                { id: 13, active: 1, dueDate },
                {
                    trial: { id: 1 },
                    user,
                    trialTemplate: { id: 4, language: 0 },
                    service: { id: 2, code: 'summer' },
                    globalUser
                }
            );
            yield certificateFactory.createWithRelations(
                { id: 14, active: 0, dueDate },
                {
                    trial: { id: 2 },
                    user,
                    trialTemplate: { id: 5, language: 1 },
                    service: { id: 3, code: 'winter' },
                    globalUser
                }
            );

            const actual = yield CertificateModel.getCertsByGlobalUser(10, [4, 5]);

            expect(actual).to.deep.equal({
                certsIds: [13],
                usersData: [
                    {
                        uid: 123,
                        services: [{ code: 'summer', language: 'ru' }]
                    }
                ]
            });
        });

        it('should return only currently active certificates', function *() {
            const user = { id: 3, uid: 123, login: 'midyac' };
            const globalUser = { id: 10 };

            yield certificateFactory.createWithRelations(
                { id: 13, active: 1, dueDate: moment(now).add(1, 'year').toDate() },
                {
                    trial: { id: 1 },
                    user,
                    trialTemplate: { id: 4, language: 0 },
                    service: { id: 2, code: 'summer' },
                    globalUser
                }
            );
            yield certificateFactory.createWithRelations(
                { id: 14, active: 1, dueDate: moment(now).subtract(1, 'year').toDate() },
                {
                    trial: { id: 2 },
                    user,
                    trialTemplate: { id: 5, language: 1 },
                    service: { id: 3, code: 'winter' },
                    globalUser
                }
            );

            const actual = yield CertificateModel.getCertsByGlobalUser(10, [4, 5]);

            expect(actual).to.deep.equal({
                certsIds: [13],
                usersData: [
                    {
                        uid: 123,
                        services: [{ code: 'summer', language: 'ru' }]
                    }
                ]
            });
        });

        it('should filter users by globalId', function *() {
            const firstUser = { id: 3, uid: 123, login: 'midyac' };
            const secondUser = { id: 7, uid: 456, login: 'dotokoto' };
            const trialTemplate = { id: 4, language: 0 };

            const dueDate = moment(now).add(1, 'year').toDate();

            yield certificateFactory.createWithRelations(
                { id: 13, active: 1, dueDate },
                {
                    trial: { id: 1 },
                    user: firstUser,
                    trialTemplate,
                    service: { id: 2, code: 'summer' },
                    globalUser: { id: 10 }
                }
            );
            yield certificateFactory.createWithRelations(
                { id: 14, active: 1, dueDate },
                {
                    trial: { id: 2 },
                    user: secondUser,
                    trialTemplate,
                    service: { id: 3, code: 'winter' },
                    globalUser: { id: 20 }
                }
            );

            const actual = yield CertificateModel.getCertsByGlobalUser(10, [4, 5]);

            expect(actual).to.deep.equal({
                certsIds: [13],
                usersData: [
                    {
                        uid: 123,
                        services: [{ code: 'summer', language: 'ru' }]
                    }
                ]
            });
        });

        it('should filter users by trialTemplateId', function *() {
            const user = { id: 3, uid: 123, login: 'midyac' };
            const dueDate = moment(now).add(1, 'year').toDate();
            const globalUser = { id: 10 };

            yield certificateFactory.createWithRelations(
                { id: 13, active: 1, dueDate },
                {
                    trial: { id: 1 },
                    user,
                    trialTemplate: { id: 4, language: 0 },
                    service: { id: 2, code: 'summer' },
                    globalUser
                }
            );
            yield certificateFactory.createWithRelations(
                { id: 14, active: 1, dueDate },
                {
                    trial: { id: 2 },
                    user,
                    trialTemplate: { id: 5, language: 1 },
                    service: { id: 3, code: 'winter' },
                    globalUser
                }
            );

            const actual = yield CertificateModel.getCertsByGlobalUser(10, [5]);

            expect(actual).to.deep.equal({
                certsIds: [14],
                usersData: [
                    {
                        uid: 123,
                        services: [{ code: 'winter', language: 'en' }]
                    }
                ]
            });
        });

        it('should correct transform certificates data', function *() {
            const firstUser = { id: 3, uid: 123, login: 'midyac' };
            const secondUser = { id: 7, uid: 456, login: 'dotokoto' };
            const globalUser = { id: 10 };

            yield certificateFactory.createWithRelations(
                { id: 13, active: 1, dueDate: moment(now).add(1, 'year').toDate() },
                {
                    trial: { id: 1 },
                    user: firstUser,
                    trialTemplate: { id: 4, language: 0 },
                    service: { id: 2, code: 'summer' },
                    globalUser
                }
            );
            yield certificateFactory.createWithRelations(
                { id: 14, active: 1, dueDate: moment(now).add(1, 'month').toDate() },
                {
                    trial: { id: 2 },
                    user: firstUser,
                    trialTemplate: { id: 5, language: 1 },
                    service: { id: 3, code: 'winter' },
                    globalUser
                }
            );
            yield certificateFactory.createWithRelations(
                { id: 15, active: 1, dueDate: moment(now).add(1, 'week').toDate() },
                {
                    trial: { id: 3 },
                    user: secondUser,
                    trialTemplate: { id: 6, language: 0 },
                    service: { id: 4, code: 'spring' },
                    globalUser
                }
            );

            const actual = yield CertificateModel.getCertsByGlobalUser(10, [4, 5, 6]);

            expect(actual).to.deep.equal({
                certsIds: [13, 14, 15],
                usersData: [
                    {
                        uid: 123,
                        services: [
                            { code: 'summer', language: 'ru' },
                            { code: 'winter', language: 'en' }
                        ]
                    },
                    {
                        uid: 456,
                        services: [
                            { code: 'spring', language: 'ru' }
                        ]
                    }
                ]
            });
        });
    });

    describe('`getNullifiedCertificates`', () => {
        const trialTemplate = { id: 1, slug: 'rain' };
        const service = { id: 4, code: 'summer' };
        const certType = { id: 1, code: 'cert' };

        it('should return only not active certificates', function *() {
            const user = { id: 1, uid: 123 };
            const authType = { id: 2, code: 'web' };
            const dueDate = moment().add(1, 'year').toDate();
            const confirmedDate = new Date(2, 2, 2);

            yield certificateFactory.createWithRelations(
                { id: 3, active: 0, dueDate, confirmedDate },
                {
                    trial: { id: 3, nullified: 0 },
                    user,
                    trialTemplate,
                    service,
                    authType,
                    type: certType
                }
            );
            yield certificateFactory.createWithRelations(
                { id: 4, active: 1, dueDate, confirmedDate: new Date(1, 1, 1) },
                {
                    trial: { id: 4, nullified: 0 },
                    user,
                    trialTemplate,
                    service,
                    authType,
                    type: certType
                }
            );

            const actual = yield CertificateModel.getNullifiedCertificates(123);

            expect(actual).to.deep.equal([
                {
                    id: 3,
                    confirmedDate,
                    dueDate,
                    exam: { id: 1, slug: 'rain' },
                    service: 'summer'
                }
            ]);
        });

        it('should return certificates with `dueDate` after now', function *() {
            const user = { id: 1, uid: 123 };
            const authType = { id: 2, code: 'web' };
            const dueDate = moment().add(1, 'year').toDate();
            const confirmedDate = new Date(2, 2, 2);

            yield certificateFactory.createWithRelations(
                { id: 3, active: 0, dueDate, confirmedDate },
                {
                    trial: { id: 3, nullified: 0 },
                    user,
                    trialTemplate,
                    service,
                    authType,
                    type: certType
                }
            );
            yield certificateFactory.createWithRelations(
                {
                    id: 4,
                    active: 0,
                    dueDate: moment().subtract(1, 'year').toDate(),
                    confirmedDate: new Date(1, 1, 1)
                },
                {
                    trial: { id: 4, nullified: 0 },
                    user,
                    trialTemplate,
                    service,
                    authType,
                    type: certType
                }
            );

            const actual = yield CertificateModel.getNullifiedCertificates(123);

            expect(actual).to.deep.equal([
                {
                    id: 3,
                    confirmedDate,
                    dueDate,
                    exam: { id: 1, slug: 'rain' },
                    service: 'summer'
                }
            ]);
        });

        it('should return certificates with type `cert`', function *() {
            const user = { id: 1, uid: 123 };
            const authType = { id: 2, code: 'web' };
            const dueDate = moment().add(1, 'year').toDate();
            const confirmedDate = new Date(2, 2, 2);

            yield certificateFactory.createWithRelations(
                { id: 3, active: 0, dueDate, confirmedDate },
                {
                    trial: { id: 3, nullified: 0 },
                    user,
                    trialTemplate,
                    service,
                    authType,
                    type: certType
                }
            );
            yield certificateFactory.createWithRelations(
                { id: 4, active: 0, dueDate, confirmedDate: new Date(1, 1, 1) },
                {
                    trial: { id: 4, nullified: 0 },
                    user,
                    trialTemplate,
                    service,
                    authType,
                    type: { id: 2, code: 'achievement' }
                }
            );

            const actual = yield CertificateModel.getNullifiedCertificates(123);

            expect(actual).to.deep.equal([
                {
                    id: 3,
                    confirmedDate,
                    dueDate,
                    exam: { id: 1, slug: 'rain' },
                    service: 'summer'
                }
            ]);
        });

        it('should return certificates only for not nullified trials', function *() {
            const user = { id: 1, uid: 123 };
            const authType = { id: 2, code: 'web' };
            const dueDate = moment().add(1, 'year').toDate();
            const confirmedDate = new Date(2, 2, 2);

            yield certificateFactory.createWithRelations(
                { id: 3, active: 0, dueDate, confirmedDate },
                {
                    trial: { id: 3, nullified: 0 },
                    user,
                    trialTemplate,
                    service,
                    authType,
                    type: certType
                }
            );
            yield certificateFactory.createWithRelations(
                { id: 4, active: 0, dueDate, confirmedDate: new Date(1, 1, 1) },
                {
                    trial: { id: 4, nullified: 1 },
                    user,
                    trialTemplate,
                    service,
                    authType,
                    type: certType
                }
            );

            const actual = yield CertificateModel.getNullifiedCertificates(123);

            expect(actual).to.deep.equal([
                {
                    id: 3,
                    confirmedDate,
                    dueDate,
                    exam: { id: 1, slug: 'rain' },
                    service: 'summer'
                }
            ]);
        });

        it('should return certificates for correct user uid', function *() {
            const firstUser = { id: 1, uid: 123 };
            const secondUser = { id: 2, uid: 456 };
            const authType = { id: 2, code: 'web' };
            const dueDate = moment().add(1, 'year').toDate();
            const confirmedDate = new Date(2, 2, 2);

            yield certificateFactory.createWithRelations(
                { id: 3, active: 0, dueDate, confirmedDate },
                {
                    trial: { id: 3, nullified: 0 },
                    user: firstUser,
                    trialTemplate,
                    service,
                    authType,
                    type: certType
                }
            );
            yield certificateFactory.createWithRelations(
                { id: 4, active: 0, dueDate, confirmedDate: new Date(1, 1, 1) },
                {
                    trial: { id: 4, nullified: 0 },
                    user: secondUser,
                    trialTemplate,
                    service,
                    authType,
                    type: certType
                }
            );

            const actual = yield CertificateModel.getNullifiedCertificates(123);

            expect(actual).to.deep.equal([
                {
                    id: 3,
                    confirmedDate,
                    dueDate,
                    exam: { id: 1, slug: 'rain' },
                    service: 'summer'
                }
            ]);
        });

        it('should return certificates only for web users', function *() {
            const webUser = { id: 1, uid: 123 };
            const telegramUser = { id: 2, uid: 123 };
            const webAuthType = { id: 1, code: 'web' };
            const telegramAuthType = { id: 2, code: 'telegram' };
            const dueDate = moment().add(1, 'year').toDate();
            const confirmedDate = new Date(2, 2, 2);

            yield certificateFactory.createWithRelations(
                { id: 3, active: 0, dueDate, confirmedDate },
                {
                    trial: { id: 3, nullified: 0 },
                    user: webUser,
                    trialTemplate,
                    service,
                    authType: webAuthType,
                    type: certType
                }
            );
            yield certificateFactory.createWithRelations(
                { id: 4, active: 0, dueDate, confirmedDate: new Date(1, 1, 1) },
                {
                    trial: { id: 4, nullified: 0 },
                    user: telegramUser,
                    trialTemplate,
                    service,
                    authType: telegramAuthType,
                    type: certType
                }
            );

            const actual = yield CertificateModel.getNullifiedCertificates(123);

            expect(actual).to.deep.equal([
                {
                    id: 3,
                    confirmedDate,
                    dueDate,
                    exam: { id: 1, slug: 'rain' },
                    service: 'summer'
                }
            ]);
        });

        it('should return `[]` when there are no suitable certificates', function *() {
            const actual = yield CertificateModel.getNullifiedCertificates(123);

            expect(actual).to.deep.equal([]);
        });
    });

    describe('`getCertificatesInfo`', () => {
        it('should filter certificates by id', function *() {
            const firstUser = { id: 11, uid: 11111, login: 'first' };
            const firstTrialTemplate = { id: 1, slug: 'direct' };

            yield certificateFactory.createWithRelations({
                id: 1,
                active: 1,
                confirmedDate: new Date(1, 1, 1),
                dueDate: new Date(2, 2, 2)
            }, {
                trial: { id: 123, nullified: 0 },
                trialTemplate: firstTrialTemplate,
                user: firstUser
            });

            yield certificateFactory.createWithRelations({
                id: 2,
                active: 0,
                confirmedDate: new Date(3, 3, 3),
                dueDate: new Date(4, 4, 4)
            }, {
                trial: { id: 456, nullified: 1 },
                trialTemplate: { id: 2, slug: 'cpm' },
                user: { id: 22, uid: 22222, login: 'second' }
            });

            yield certificateFactory.createWithRelations({
                id: 3,
                active: 0,
                confirmedDate: new Date(5, 5, 5),
                dueDate: new Date(6, 6, 6)
            }, {
                trial: { id: 789, nullified: 0 },
                trialTemplate: firstTrialTemplate,
                user: firstUser
            });

            const actual = yield CertificateModel.getCertificatesInfo([1, 2]);

            expect(actual).to.deep.equal([
                {
                    id: 1,
                    isActive: true,
                    confirmedDate: new Date(1, 1, 1),
                    dueDate: new Date(2, 2, 2),
                    trialId: 123,
                    isTrialNullified: false,
                    login: 'first',
                    examSlug: 'direct'
                },
                {
                    id: 2,
                    isActive: false,
                    confirmedDate: new Date(3, 3, 3),
                    dueDate: new Date(4, 4, 4),
                    trialId: 456,
                    isTrialNullified: true,
                    login: 'second',
                    examSlug: 'cpm'
                }
            ]);
        });

        it('should return empty array if certificates do not exist', function *() {
            const actual = yield CertificateModel.getCertificatesInfo([1, 2]);

            expect(actual).to.deep.equal([]);
        });
    });

    describe('`getGeoadvCertificates`', () => {
        it('should filter by exam slug', function *() {
            yield certificateFactory.createWithRelations(
                { id: 333, active: 1, isSentToGeoadv: false },
                {
                    trialTemplate: { slug: 'msp' },
                    user: { id: 1, uid: 123456 },
                    trial: { id: 123, nullified: 0 }
                }
            );
            yield certificateFactory.createWithRelations(
                { id: 222, active: 1, isSentToGeoadv: false },
                {
                    trialTemplate: { slug: 'msp' },
                    user: { id: 2, uid: 756789 },
                    trial: { id: 456, nullified: 0 }
                }
            );
            yield certificateFactory.createWithRelations(
                { id: 444, active: 1, isSentToGeoadv: false },
                {
                    trialTemplate: { slug: 'direct' },
                    user: { id: 3, uid: 700 },
                    trial: { id: 789, nullified: 0 }
                }
            );
            yield certificateFactory.createWithRelations(
                { id: 555, active: 1, isSentToGeoadv: false },
                {
                    trialTemplate: { slug: 'market' },
                    user: { id: 4, uid: 10101010 },
                    trial: { id: 1011, nullified: 0 }
                }
            );

            const actual = yield CertificateModel.getGeoadvCertificates();

            actual.sort((cert1, cert2) => cert1.certId - cert2.certId);

            expect(actual).to.deep.equal([
                {
                    certId: 222,
                    uid: 756789,
                    examSlug: 'msp'
                },
                {
                    certId: 333,
                    uid: 123456,
                    examSlug: 'msp'
                },
                {
                    certId: 444,
                    uid: 700,
                    examSlug: 'direct'
                }
            ]);
        });

        it('should return only active certificates', function *() {
            yield certificateFactory.createWithRelations(
                { id: 333, active: 1, isSentToGeoadv: false },
                {
                    trialTemplate: { slug: 'msp' },
                    user: { id: 1, uid: 123456 },
                    trial: { id: 123, nullified: 0 }
                }
            );
            yield certificateFactory.createWithRelations(
                { id: 444, active: 0, isSentToGeoadv: false },
                {
                    trialTemplate: { slug: 'msp' },
                    user: { id: 2, uid: 756789 },
                    trial: { id: 456, nullified: 0 }
                }
            );

            const actual = yield CertificateModel.getGeoadvCertificates();

            expect(actual).to.deep.equal([
                {
                    certId: 333,
                    uid: 123456,
                    examSlug: 'msp'
                }
            ]);
        });

        it('should return only unsent certificates', function *() {
            yield certificateFactory.createWithRelations(
                { id: 333, active: 1, isSentToGeoadv: false },
                {
                    trialTemplate: { slug: 'msp' },
                    user: { id: 1, uid: 123456 },
                    trial: { id: 123, nullified: 0 }
                }
            );
            yield certificateFactory.createWithRelations(
                { id: 444, active: 1, isSentToGeoadv: true },
                {
                    trialTemplate: { slug: 'msp' },
                    user: { id: 2, uid: 756789 },
                    trial: { id: 456, nullified: 0 }
                }
            );

            const actual = yield CertificateModel.getGeoadvCertificates();

            expect(actual).to.deep.equal([
                {
                    certId: 333,
                    uid: 123456,
                    examSlug: 'msp'
                }
            ]);
        });

        it('should return certificates only for not nullified trials', function *() {
            yield certificateFactory.createWithRelations(
                { id: 333, active: 1, isSentToGeoadv: false },
                {
                    trialTemplate: { slug: 'msp' },
                    user: { id: 1, uid: 123456 },
                    trial: { id: 123, nullified: 0 }
                }
            );
            yield certificateFactory.createWithRelations(
                { id: 444, active: 1, isSentToGeoadv: false },
                {
                    trialTemplate: { slug: 'msp' },
                    user: { id: 2, uid: 756789 },
                    trial: { id: 456, nullified: 1 }
                }
            );

            const actual = yield CertificateModel.getGeoadvCertificates();

            expect(actual).to.deep.equal([
                {
                    certId: 333,
                    uid: 123456,
                    examSlug: 'msp'
                }
            ]);
        });

        it('should return `[]` when suitable certificates are not exist', function *() {
            yield certificateFactory.createWithRelations(
                { id: 333, active: 1, isSentToGeoadv: true },
                {
                    trialTemplate: { slug: 'msp' },
                    user: { id: 1, uid: 123456 },
                    trial: { id: 123, nullified: 0 }
                }
            );

            const actual = yield CertificateModel.getGeoadvCertificates();

            expect(actual).to.deep.equal([]);
        });
    });

    describe('sendCertificatesToGeoadv', () => {
        const ticket = 'someTicket';

        before(() => {
            mockMailer();
            mockCache(ticket);

            CertificateModel = require('models/certificate');
            log = require('logger');
        });

        after(() => {
            mockery.disable();
            mockery.deregisterAll();
        });

        beforeEach(() => {
            sinon.spy(log, 'error');
        });

        afterEach(() => {
            log.error.restore();
            nock.cleanAll();
        });

        it('should do nothing when there are no certificates', function *() {
            const tvmNock = nockTvm.getTicket({ 'geoadv-testing': { ticket } });
            const geoadvNock = nock(geoadv.host)
                .put(geoadv.path)
                .reply(200);

            yield CertificateModel.sendCertificatesToGeoadv([]);

            expect(log.error.called).to.be.false;
            expect(tvmNock.isDone()).to.be.false;
            expect(geoadvNock.isDone()).to.be.false;
        });

        it('should request to geoadv several times', function *() {
            yield certificateFactory.createWithRelations({ id: 222, isSentToGeoadv: false }, {});
            yield certificateFactory.createWithRelations({ id: 333, isSentToGeoadv: false }, {});
            const firstCertData = {
                certId: 222,
                uid: 756789,
                examSlug: 'msp'
            };
            const secondCertData = {
                certId: 333,
                uid: 123456,
                examSlug: 'market'
            };
            const tvmNock = nockTvm.getTicket({ 'geoadv-testing': { ticket } }, 2);
            const geoadvNockFirst = nock(geoadv.host)
                .put(geoadv.path, firstCertData)
                .reply(200);
            const geoadvNockSecond = nock(geoadv.host)
                .put(geoadv.path, secondCertData)
                .reply(200);

            yield CertificateModel.sendCertificatesToGeoadv([
                firstCertData,
                secondCertData
            ]);

            expect(log.error.called).to.be.false;
            expect(geoadvNockFirst.isDone()).to.be.true;
            expect(geoadvNockSecond.isDone()).to.be.true;
            expect(tvmNock.isDone()).to.be.true;

            const actualDbData = yield Certificate.findAll({ attributes: ['isSentToGeoadv'], raw: true });

            expect(actualDbData).to.deep.equal([{ isSentToGeoadv: true }, { isSentToGeoadv: true }]);
        });

        it('should log error when request was failed', function *() {
            yield certificateFactory.createWithRelations({ id: 222, isSentToGeoadv: false }, {});
            yield certificateFactory.createWithRelations({ id: 333, isSentToGeoadv: false }, {});
            const firstCertData = {
                certId: 222,
                uid: 756789,
                examSlug: 'msp'
            };
            const secondCertData = {
                certId: 333,
                uid: 123456,
                examSlug: 'msp'
            };
            const tvmNock = nockTvm.getTicket({ 'geoadv-testing': { ticket } }, 2);
            const geoadvNockFirst = nock(geoadv.host)
                .put(geoadv.path, firstCertData)
                .reply(500);
            const geoadvNockSecond = nock(geoadv.host)
                .put(geoadv.path, secondCertData)
                .reply(200);

            yield CertificateModel.sendCertificatesToGeoadv([
                firstCertData,
                secondCertData
            ]);

            expect(log.error.calledOnce).to.be.true;
            expect(geoadvNockFirst.isDone()).to.be.true;
            expect(geoadvNockSecond.isDone()).to.be.true;
            expect(tvmNock.isDone()).to.be.true;

            const actualDbData = yield Certificate.findAll({
                attributes: ['id', 'isSentToGeoadv'],
                raw: true,
                order: [['id']]
            });

            expect(actualDbData).to.deep.equal([
                { id: 222, isSentToGeoadv: false },
                { id: 333, isSentToGeoadv: true }
            ]);
        });
    });
});
