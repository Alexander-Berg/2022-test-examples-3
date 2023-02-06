require('co-mocha');

const _ = require('lodash');
const { expect } = require('chai');
const fs = require('fs');
const mockery = require('mockery');
const nock = require('nock');
const request = require('co-supertest').agent(require('api').callback());
const sinon = require('sinon');

const { ProctoringVideos, Trial } = require('db/postgres');

let ProctorEdu = require('models/proctoring/proctorEdu');
let S3 = require('web/s3');

const dbHelper = require('tests/helpers/clear');
const { removeDir } = require('tests/helpers/directory');
const mockS3 = require('tests/helpers/s3');
const mockMailer = require('tests/helpers/mailer');
const nockProctorEdu = require('tests/helpers/proctorEdu');
const mockUuid = require('tests/helpers/uuid');

const trialsFactory = require('tests/factory/trialsFactory');

let log = require('logger');

describe('S3 controller', () => {
    beforeEach(dbHelper.clear);

    describe('`uploadFilesAsync`', () => {
        const s3Success = { status: 'OK' };
        const videosResponse = [
            {
                attach: [
                    {
                        id: 'video-id',
                        filename: 'webcam.webm',
                        mimetype: 'video/webm',
                        createdAt: '2020-05-13T09:30:46.565Z' // 1589362246565
                    }
                ]
            }
        ];
        const trialId = 123;

        beforeEach(function *() {
            mockMailer();

            log = require('logger');
            sinon.spy(log, 'error');

            yield trialsFactory.createWithRelations(
                {
                    id: trialId,
                    openId: 'open-id',
                    filesStatus: 'initial',
                    expired: 1,
                    pdf: null
                },
                { trialTemplate: { isProctoring: true } }
            );

            fs.mkdirSync('models/proctoring/output');
        });

        afterEach(() => {
            log.error.restore();
            nock.cleanAll();
            mockery.disable();
            mockery.deregisterAll();

            removeDir('models/proctoring/input');
            removeDir('models/proctoring/output');
        });

        function stubConcatenation() {
            sinon.stub(ProctorEdu, 'concatVideos', groups => {
                return groups.reduce((videosByName, group) => {
                    videosByName[group.name] = {
                        path: `models/proctoring/output/${group.name}.webm`,
                        duration: 60000
                    };

                    fs.writeFileSync(`models/proctoring/output/${group.name}.webm`, 'video');

                    return videosByName;
                }, {});
            });

            sinon.stub(ProctorEdu, '_downloadVideoThroughFfmpeg', (video, videoId) => {
                return Promise.resolve({
                    videoPath: `models/proctoring/input/${videoId}.webm`,
                    duration: 60000
                });
            });
        }

        const attributes = ['trialId', 'name', 'startTime', 'duration'];
        const trialOptions = { where: { id: trialId }, attributes: ['filesStatus', 'pdf'], raw: true };

        it('should successfully upload videos and pdf to S3', function *() {
            mockS3(s3Success);
            mockUuid();

            S3 = require('web/s3');
            ProctorEdu = require('models/proctoring/proctorEdu');
            stubConcatenation();

            nockProctorEdu.protocol({
                openId: 'open-id',
                response: { complete: true, pdf: 'some-pdf', status: 'stopped' }
            });
            nockProctorEdu.events({ openId: 'open-id', response: videosResponse });
            nockProctorEdu.file({ id: 'some-pdf', response: 'pdf' });
            nockProctorEdu.file({ id: 'video-id', response: 'video' });

            yield S3.uploadFilesAsync();

            const videos = yield ProctoringVideos.findAll({ attributes, raw: true });
            const trial = yield Trial.findOne(trialOptions);
            const outputFiles = fs.readdirSync('models/proctoring/output');

            expect(videos).to.deep.equal([
                {
                    trialId: 123,
                    name: '123_some-random-uuid.webm',
                    startTime: 1589362186565, // = 1589362246565 - 60000
                    duration: 60000
                }
            ]);
            expect(trial).to.deep.equal({ filesStatus: 'saved', pdf: 'some-pdf.pdf' });
            expect(log.error.notCalled).to.be.true;
            expect(outputFiles).to.deep.equal([]);

            ProctorEdu.concatVideos.restore();
            ProctorEdu._downloadVideoThroughFfmpeg.restore();
        });

        it('should set `files_status` = saved when there is no video', function *() {
            mockS3(s3Success);

            S3 = require('web/s3');

            nockProctorEdu.protocol({
                openId: 'open-id',
                response: { complete: true, pdf: 'some-pdf', status: 'stopped' }
            });
            nockProctorEdu.events({ openId: 'open-id', code: 404 });
            nockProctorEdu.file({ id: 'some-pdf', response: 'pdf' });

            yield S3.uploadFilesAsync();

            const videos = yield ProctoringVideos.findAll();
            const trial = yield Trial.findOne(trialOptions);

            expect(trial).to.deep.equal({ filesStatus: 'saved', pdf: 'some-pdf.pdf' });
            expect(videos).to.deep.equal([]);
            expect(log.error.calledOnce).to.be.true;
        });

        it('should skip trial when proctoring does not send session data', function *() {
            S3 = require('web/s3');

            nockProctorEdu.protocol({ openId: 'open-id', code: 500 });

            yield S3.uploadFilesAsync();

            const videos = yield ProctoringVideos.findAll();
            const trial = yield Trial.findOne(trialOptions);

            expect(videos).to.deep.equal([]);
            expect(trial).to.deep.equal({ filesStatus: 'initial', pdf: null });
            expect(log.error.calledOnce).to.be.true;
        });

        it('should skip trial when proctoring has not processed it yet', function *() {
            S3 = require('web/s3');

            nockProctorEdu.protocol({ openId: 'open-id', response: { complete: false } });

            yield S3.uploadFilesAsync();

            const videos = yield ProctoringVideos.findAll();
            const trial = yield Trial.findOne(trialOptions);

            expect(videos).to.deep.equal([]);
            expect(trial).to.deep.equal({ filesStatus: 'initial', pdf: null });
            expect(log.error.notCalled).to.be.true;
        });

        // EXPERTDEV-896: [API] Починить загрузку файлов в s3 по крону
        it('should set `filesStatus` = `error` when trial is complete but not stopped', function *() {
            S3 = require('web/s3');

            nockProctorEdu.protocol({
                openId: 'open-id',
                response: { complete: true, status: 'skipped' }
            });

            yield S3.uploadFilesAsync();

            const videos = yield ProctoringVideos.findAll();
            const trial = yield Trial.findOne(trialOptions);

            expect(videos).to.deep.equal([]);
            expect(trial).to.deep.equal({ filesStatus: 'error', pdf: null });
            expect(log.error.notCalled).to.be.true;
        });

        it('should set `filesStatus` = `error` when trial status is `created`, complete is false', function *() {
            S3 = require('web/s3');

            nockProctorEdu.protocol({
                openId: 'open-id',
                response: { complete: false, status: 'created' }
            });

            yield S3.uploadFilesAsync();

            const videos = yield ProctoringVideos.findAll();
            const trial = yield Trial.findOne(trialOptions);

            expect(videos).to.deep.equal([]);
            expect(trial).to.deep.equal({ filesStatus: 'error', pdf: null });
            expect(log.error.calledOnce).to.be.true;
        });

        it('should set `filesStatus` = `error` when S3 is out of service', function *() {
            mockS3({}, 'fail');
            mockUuid();

            S3 = require('web/s3');
            ProctorEdu = require('models/proctoring/proctorEdu');
            stubConcatenation();

            nockProctorEdu.protocol({
                openId: 'open-id',
                response: { complete: true, pdf: 'some-pdf', status: 'stopped' }
            });
            nockProctorEdu.events({ openId: 'open-id', response: videosResponse });
            nockProctorEdu.file({ id: 'some-pdf', response: 'pdf' });
            nockProctorEdu.file({ id: 'video-id', response: 'video' });

            yield S3.uploadFilesAsync();

            const videos = yield ProctoringVideos.findAll();
            const trial = yield Trial.findOne(trialOptions);
            const outputFiles = fs.readdirSync('models/proctoring/output');

            expect(videos).to.deep.equal([]);
            expect(trial).to.deep.equal({ filesStatus: 'error', pdf: null });
            expect(outputFiles).to.deep.equal([]);
            expect(log.error.calledTwice).to.be.true;
            expect(log.error.args).to.deep.equal([
                [
                    'Failed put object to S3',
                    {
                        error: 'Some error'
                    }
                ],
                [
                    'Upload data was failed',
                    {
                        error: {
                            expose: true,
                            options: {
                                error: 'Some error',
                                bucket: 'expert',
                                key: 'testing/videos/123_some-random-uuid.webm',
                                internalCode: '424_DNL'
                            },
                            statusCode: 424,
                            status: 424
                        }
                    }
                ]
            ]);

            ProctorEdu.concatVideos.restore();
            ProctorEdu._downloadVideoThroughFfmpeg.restore();
        });

        it('should return 424 when ProctorEdu pdf not loaded', function *() {
            S3 = require('web/s3');

            nockProctorEdu.protocol({
                openId: 'open-id',
                response: { complete: true, pdf: 'some-pdf', status: 'stopped' }
            });
            nockProctorEdu.events({ openId: 'open-id', code: 404 });
            nockProctorEdu.file({ id: 'some-pdf', code: 500 });

            yield S3.uploadFilesAsync();

            const videos = yield ProctoringVideos.findAll();
            const trial = yield Trial.findOne(trialOptions);

            expect(videos).to.deep.equal([]);
            expect(trial).to.deep.equal({ filesStatus: 'error', pdf: null });

            const errorFields = [
                'message',
                'host',
                'hostname',
                'method',
                'path',
                'statusCode',
                'statusMessage'
            ];

            expect(log.error.calledThrice).to.be.true;

            log.error.args[0][1].err = _.pick(log.error.args[0][1].err, errorFields);
            expect(log.error.args[0]).to.deep.equal([
                'Request to ProctorEdu was failed',
                {
                    err: {
                        message: 'Response code 404 (Not Found)',
                        host: 'yandex-dev.proctoring.online:443',
                        hostname: 'yandex-dev.proctoring.online',
                        method: 'GET',
                        path: '/api/rest/v3/event/open-id',
                        statusCode: 404,
                        statusMessage: 'Not Found'
                    }
                }
            ]);

            log.error.args[1][1].err = _.pick(log.error.args[1][1].err, errorFields);
            expect(log.error.args[1]).to.deep.equal([
                'Request to ProctorEdu was failed',
                {
                    err: {
                        message: 'Response code 500 (Internal Server Error)',
                        host: 'yandex-dev.proctoring.online:443',
                        hostname: 'yandex-dev.proctoring.online',
                        method: 'GET',
                        path: '/api/rest/v3/storage/some-pdf',
                        statusCode: 500,
                        statusMessage: 'Internal Server Error'
                    }
                }
            ]);

            expect(log.error.args[2][0]).to.equal('Upload data was failed');
            expect(log.error.args[2][1].error.statusCode).to.equal(424);
            expect(log.error.args[2][1].error.options).to.deep.equal({
                openId: 'open-id',
                sessionPdf: 'some-pdf',
                internalCode: '424_PNL'
            });
        });

        it('should process all selected trials from db', function *() {
            mockS3(s3Success);
            mockUuid();

            yield trialsFactory.createWithRelations(
                {
                    id: 1,
                    openId: 'open-id-with-error-pdf',
                    filesStatus: 'initial',
                    expired: 1,
                    pdf: null
                },
                { trialTemplate: { isProctoring: true } }
            );

            S3 = require('web/s3');
            ProctorEdu = require('models/proctoring/proctorEdu');
            stubConcatenation();

            nockProctorEdu.protocol({
                openId: 'open-id',
                response: { complete: true, pdf: 'some-pdf', status: 'stopped' }
            });
            nockProctorEdu.protocol({
                openId: 'open-id-with-error-pdf',
                response: { complete: true, pdf: 'error-pdf', status: 'stopped' }
            });
            nockProctorEdu.events({ openId: 'open-id', response: videosResponse });
            nockProctorEdu.events({ openId: 'open-id-with-error-pdf', response: videosResponse });
            nockProctorEdu.file({ id: 'some-pdf', response: 'pdf' });
            nockProctorEdu.file({ id: 'error-pdf', code: 500 });
            nockProctorEdu.file({ id: 'video-id', response: 'video' });

            yield S3.uploadFilesAsync();

            const trials = yield Trial.findAll({
                attributes: ['id', 'filesStatus', 'pdf'],
                order: [['id']],
                raw: true
            });

            expect(trials).to.deep.equal([
                { id: 1, filesStatus: 'error', pdf: null },
                { id: 123, filesStatus: 'saved', pdf: 'some-pdf.pdf' }
            ]);

            ProctorEdu.concatVideos.restore();
            ProctorEdu._downloadVideoThroughFfmpeg.restore();
        });
    });

    describe('`uploadFiles`', () => {
        it('should response on request with 204', function *() {
            yield request
                .get('/v1/s3/uploadFiles')
                .expect(204);
        });
    });
});
