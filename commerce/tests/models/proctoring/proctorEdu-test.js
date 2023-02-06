require('co-mocha');

const _ = require('lodash');
const { expect } = require('chai');
const fs = require('fs');
const nock = require('nock');
const sinon = require('sinon');
const mockery = require('mockery');

let ProctorEdu = require('models/proctoring/proctorEdu');
let log = require('logger');

const catchError = require('tests/helpers/catchError').generator;
const { removeDir } = require('tests/helpers/directory');
const mockMailer = require('tests/helpers/mailer');
const nockProctorEdu = require('tests/helpers/proctorEdu');

describe('ProctorEdu Model', () => {
    const inputFilesPath = 'models/proctoring/input';
    const outputFilesPath = 'models/proctoring/output';

    describe('`getSessionData`', () => {
        before(() => {
            mockMailer();
            ProctorEdu = require('models/proctoring/proctorEdu');
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
            nock.cleanAll();
            log.error.restore();
        });

        it('should success get session data', function *() {
            const expected = {
                openId: 'correct-open-id',
                duration: 3,
                evaluation: 43
            };

            nockProctorEdu.protocol({ openId: 'correct-open-id', response: expected });

            const actual = yield ProctorEdu.getSessionData('correct-open-id');

            expect(actual).to.deep.equal(expected);
            expect(log.error.notCalled).to.be.true;
        });

        it('should retry when statusCode is 5** or 499', function *() {
            const expected = { evaluation: 43 };

            nockProctorEdu.retry({ openId: 'retry-500-open-id', code: 500, response: expected });

            const actual = yield ProctorEdu.getSessionData('retry-500-open-id');

            expect(actual).to.deep.equal(expected);
            expect(log.error.notCalled).to.be.true;
        });

        it('should not retry when statusCode is not 5** or 499', function *() {
            nockProctorEdu.retry({ openId: 'retry-400-open-id', code: 400, response: { evaluation: 43 } });

            const actual = yield ProctorEdu.getSessionData('retry-400-open-id');

            expect(actual).to.be.undefined;
            expect(log.error.calledOnce).to.be.true;
        });

        it('should return `undefined` when request to ProctorEdu was failed', function *() {
            nockProctorEdu.protocol({ openId: 'correct-open-id', code: 500, response: { evaluation: 43 } });

            const actual = yield ProctorEdu.getSessionData('correct-open-id');

            expect(actual).to.be.undefined;
            expect(log.error.calledOnce).to.be.true;
        });

        it('should return `undefined` when response is empty', function *() {
            nockProctorEdu.protocol({ openId: 'invalid-open-id', response: {} });

            const actual = yield ProctorEdu.getSessionData('invalid-open-id');

            expect(actual).to.be.undefined;
            expect(log.error.notCalled).to.be.true;
        });

        it('should return `undefined` when data parsing was failed', function *() {
            nockProctorEdu.protocol({ openId: 'failed-open-id', response: null });

            const actual = yield ProctorEdu.getSessionData('failed-open-id');

            expect(actual).to.be.undefined;
            expect(log.error.calledOnce).to.be.true;
        });
    });

    describe('`getVideosByOpenId`', () => {
        before(() => {
            mockMailer();
            ProctorEdu = require('models/proctoring/proctorEdu');
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
            nock.cleanAll();
            log.error.restore();
        });

        it('should successfully get videos', function *() {
            const response = [
                {
                    attach: [
                        {
                            id: 'video1',
                            filename: 'webcam.webm',
                            mimetype: 'video/webm',
                            createdAt: '2020-05-13T09:30:46.565Z'
                        },
                        {
                            id: 'video2',
                            filename: 'screen.webm',
                            mimetype: 'video/webm',
                            createdAt: '2020-05-13T09:30:46.528Z'
                        }
                    ]
                },
                {
                    attach: []
                },
                {
                    attach: [
                        {
                            id: 'video3',
                            filename: 'webcam.webm',
                            mimetype: 'video/webm',
                            createdAt: '2020-05-13T09:31:46.405Z'
                        },
                        {
                            id: 'video4',
                            filename: 'screen.webm',
                            mimetype: 'video/webm',
                            createdAt: '2020-05-13T09:31:46.400Z'
                        }
                    ]
                }
            ];

            nockProctorEdu.events({ openId: 'open-id', response });

            const actual = yield ProctorEdu.getVideosByOpenId('open-id');

            const expected = [
                {
                    id: 'video1',
                    eventSaveTime: 1589362246565,
                    source: 'webcam'
                },
                {
                    id: 'video2',
                    eventSaveTime: 1589362246528,
                    source: 'screen'
                },
                {
                    id: 'video3',
                    eventSaveTime: 1589362306405,
                    source: 'webcam'
                },
                {
                    id: 'video4',
                    eventSaveTime: 1589362306400,
                    source: 'screen'
                }
            ];

            expect(actual).to.deep.equal(expected);
            expect(log.error.notCalled).to.be.true;
        });

        it('should get only "video" mimetype', function *() {
            const response = [
                {
                    attach: [
                        {
                            id: 'data1',
                            filename: 'webcam.jpg',
                            mimetype: 'image/jpeg',
                            createdAt: '2020-05-13T09:31:46.153Z'
                        },
                        {
                            id: 'data2',
                            filename: 'webcam.webm',
                            mimetype: 'video/webm',
                            createdAt: '2020-05-13T09:30:46.565Z' // 1589362246565
                        },
                        {
                            id: 'data3',
                            filename: 'screen.jpg',
                            mimetype: 'image/jpeg',
                            createdAt: '2020-05-13T09:31:46.277Z'
                        },
                        {
                            id: 'data4',
                            filename: 'screen.webm',
                            mimetype: 'video/webm',
                            createdAt: '2020-05-13T09:30:46.528Z' // 1589362246528
                        },
                        {
                            id: 'data5',
                            filename: 'webcam.webm',
                            mimetype: 'video/x-matroska',
                            createdAt: '2020-05-13T09:36:46.528Z' // 1589362606528
                        }
                    ]
                }
            ];

            nockProctorEdu.events({ openId: 'open-id', response });

            const actual = yield ProctorEdu.getVideosByOpenId('open-id');

            const expected = [
                {
                    id: 'data2',
                    eventSaveTime: 1589362246565,
                    source: 'webcam'
                },
                {
                    id: 'data4',
                    eventSaveTime: 1589362246528,
                    source: 'screen'
                },
                {
                    id: 'data5',
                    eventSaveTime: 1589362606528,
                    source: 'webcam'
                }
            ];

            expect(actual).to.deep.equal(expected);
            expect(log.error.notCalled).to.be.true;
        });

        it('should retry when statusCode is 5**', function *() {
            const response = [
                {
                    attach: [
                        {
                            id: 'video',
                            filename: 'webcam.webm',
                            mimetype: 'video/webm',
                            createdAt: '2020-05-13T09:30:46.565Z' // 1589362246565
                        }
                    ]
                }
            ];

            nockProctorEdu.retryEvents({ openId: 'retry-500-open-id', response, code: 500 });

            const actual = yield ProctorEdu.getVideosByOpenId('retry-500-open-id');

            const expected = [
                {
                    id: 'video',
                    eventSaveTime: 1589362246565,
                    source: 'webcam'
                }
            ];

            expect(actual).to.deep.equal(expected);
            expect(log.error.notCalled).to.be.true;
        });

        it('should not retry when statusCode is not 5**', function *() {
            const response = [
                {
                    attach: [
                        {
                            id: 'video',
                            filename: 'webcam.webm',
                            mimetype: 'video/webm',
                            createdAt: '2020-05-13T09:30:46.565Z'
                        }
                    ]
                }
            ];

            nockProctorEdu.retryEvents({ openId: 'retry-400-open-id', response, code: 400 });

            const actual = yield ProctorEdu.getVideosByOpenId('retry-400-open-id');

            expect(actual).to.be.undefined;
            expect(log.error.calledTwice).to.be.true;
        });

        it('should return `undefined` when request to ProctorEdu was failed', function *() {
            nockProctorEdu.events({ openId: 'open-id', code: 500 });

            const actual = yield ProctorEdu.getVideosByOpenId('open-id');

            expect(actual).to.be.undefined;
            expect(log.error.calledTwice).to.be.true;
        });

        it('should return `[]` when response is empty', function *() {
            nockProctorEdu.events({ openId: 'empty-open-id', response: [] });

            const actual = yield ProctorEdu.getVideosByOpenId('empty-open-id');

            expect(actual).to.deep.equal([]);
            expect(log.error.notCalled).to.be.true;
        });

        it('should return `undefined` when data parsing was failed', function *() {
            nockProctorEdu.events({ openId: 'open-id', response: 'not-a-json' });

            const actual = yield ProctorEdu.getVideosByOpenId('open-id');

            expect(actual).to.be.undefined;
            expect(log.error.calledOnce).to.be.true;
        });

        it('should return `[]` when videos are absent', function *() {
            nockProctorEdu.events({ openId: 'open-id', code: 404 });

            const actual = yield ProctorEdu.getVideosByOpenId('open-id');

            expect(actual).to.deep.equal([]);
            expect(log.error.calledOnce).to.be.true;
        });
    });

    describe('`getFile`', () => {
        before(() => {
            mockMailer();
            ProctorEdu = require('models/proctoring/proctorEdu');
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
            nock.cleanAll();
            log.error.restore();
        });

        it('should successfully get pdf', function *() {
            const response = Buffer.from('pdf');

            nockProctorEdu.file({ id: 'some-pdf', response });

            const actual = yield ProctorEdu.getFile('some-pdf', 'pdf');

            expect(actual).to.deep.equal(response);
            expect(log.error.notCalled).to.be.true;
        });

        it('should successfully get video', function *() {
            const response = Buffer.from('video');

            nockProctorEdu.file({ id: 'some-video', response });

            const actual = yield ProctorEdu.getFile('some-video', 'video');

            expect(actual).to.deep.equal(response);
            expect(log.error.notCalled).to.be.true;
        });

        it('should retry when statusCode is 5**', function *() {
            const response = Buffer.from('pdf');

            nockProctorEdu.retryFile({ id: 'retry-500-pdf', code: 500, response });

            const actual = yield ProctorEdu.getFile('retry-500-pdf', 'pdf');

            expect(actual).to.deep.equal(response);
            expect(log.error.notCalled).to.be.true;
        });

        it('should not retry when statusCode is not 5**', function *() {
            const response = Buffer.from('pdf');

            nockProctorEdu.retryFile({ id: 'retry-400-pdf', code: 400, response });

            const actual = yield ProctorEdu.getFile('retry-400-pdf', 'pdf');

            expect(actual).to.be.undefined;
            expect(log.error.calledOnce).to.be.true;
        });

        it('should return `undefined` when request to ProctorEdu was failed', function *() {
            nockProctorEdu.file({ id: 'failed-file', code: 500 });

            const actual = yield ProctorEdu.getFile('failed-file', 'pdf');

            expect(actual).to.be.undefined;
            expect(log.error.calledOnce).to.be.true;
        });

        it('should return `null` when file does not exist', function *() {
            nockProctorEdu.file({ id: 'not-found-file', code: 404 });

            const actual = yield ProctorEdu.getFile('not-found-file', 'pdf');

            expect(actual).to.be.null;
            expect(log.error.calledOnce).to.be.true;
        });
    });

    describe('`getVideosMarkup`', () => {
        it('should correct process several videos and metrics', () => {
            const sessionData = {
                startedAt: '2018-01-01T00:00:00.000Z', // 1514764800000
                timesheet: {
                    xaxis: [1, 3, 9, 15],
                    yaxis: [
                        { b1: 0, b2: 0, s2: 0, c1: 0, c2: 80, c3: 0, c4: 0, m1: 2, m2: 20, k1: 0, n1: 0 },
                        { b1: 0, b2: 0, s2: 56, c1: 0, c2: 20, c3: 0, c4: 0, m1: 0, m2: 5, k1: 0, n1: 0 },
                        { b1: 0, b2: 0, s2: 0, c1: 21, c2: 0, c3: 0, c4: 0, m1: 0, m2: 0, k1: 0, n1: 100 },
                        { b1: 0, b2: 0, s2: 0, c1: 0, c2: 0, c3: 49, c4: 0, m1: 2, m2: 8, k1: 0, n1: 0 }
                    ]
                }
            };
            const videos = [
                {
                    name: '1.webm',
                    startTime: 1514764830000, // '2018-01-01T00:00:30.000Z'
                    duration: 240000 // 4 min
                },
                {
                    name: '2.webm',
                    startTime: 1514765100000, // '2018-01-01T00:05:00.000Z'
                    duration: 120000 // 2 min
                },
                {
                    name: '3.webm',
                    startTime: 1514765330000, // '2018-01-01T00:08:50.000Z'
                    duration: 480000 // 8 min
                }
            ];

            const expected = [
                {
                    url: 'https://yastatic.net/s3/expert/testing/videos/1.webm',
                    intervals: [
                        { start: 0, end: 30 },
                        { start: 30, end: 150 },
                        { start: 145, end: 240 }
                    ]
                },
                {
                    url: 'https://yastatic.net/s3/expert/testing/videos/2.webm',
                    intervals: [
                        { start: 0, end: 120 }
                    ]
                },
                {
                    url: 'https://yastatic.net/s3/expert/testing/videos/3.webm',
                    intervals: [
                        { start: 10, end: 370 }
                    ]
                }
            ];

            const actual = ProctorEdu.getVideosMarkup(sessionData, videos, false);

            expect(actual).to.deep.equal(expected);
        });

        it('should return `[]` when no acceptable intervals', () => {
            const sessionData = {
                startedAt: '2018-01-01T00:00:00.000Z', // 1514764800000
                timesheet: {
                    xaxis: [1],
                    yaxis: [
                        { b1: 0, b2: 0, s2: 0, c1: 0, c2: 80, c3: 0, c4: 0, m1: 2, m2: 20, k1: 0, n1: 0 }
                    ]
                }
            };
            const videos = [
                {
                    name: '1.webm',
                    startTime: 1514764800000,
                    duration: 3000 // 3 s
                },
                {
                    name: '2.webm',
                    startTime: 1514764804000,
                    duration: 10000 // 10 s
                },
                {
                    name: '3.webm',
                    startTime: 1514764815000,
                    duration: 9876 // 9 s 876 ms
                }
            ];

            const actual = ProctorEdu.getVideosMarkup(sessionData, videos, false);

            expect(actual).to.deep.equal([]);
        });

        it('should return `[]` when all metrics are acceptable', () => {
            const sessionData = {
                startedAt: '2018-01-01T00:00:00.000Z', // 1514764800000
                timesheet: {
                    xaxis: [2, 6, 18],
                    yaxis: [
                        { b1: 0, b2: 0, s2: 0, c1: 0, c2: 2, c3: 0, c4: 0, m1: 2, m2: 20, k1: 0, n1: 0 },
                        { b1: 0, b2: 4, s2: 0, c1: 0, c2: 0, c3: 0, c4: 5, m1: 2, m2: 0, k1: 8, n1: 0 },
                        { b1: 0, b2: 0, s2: 10, c1: 0, c2: 0, c3: 0, c4: 0, m1: 2, m2: 20, k1: 0, n1: 0 }
                    ]
                }
            };
            const videos = [
                {
                    name: '1.webm',
                    startTime: 1514764800000, // '2018-01-01T00:00:00.000Z'
                    duration: 60000 // 1 min
                },
                {
                    name: '2.webm',
                    startTime: 1514764920000, // '2018-01-01T00:02:00.000Z'
                    duration: 120000 // 2 min
                },
                {
                    name: '3.webm',
                    startTime: 1514765100000, // '2018-01-01T00:05:00.000Z'
                    duration: 900000 // 15 min
                }
            ];

            const actual = ProctorEdu.getVideosMarkup(sessionData, videos, false);

            expect(actual).to.deep.equal([]);
        });

        it('should return several intervals for one video when several metrics belongs to it', () => {
            const sessionData = {
                startedAt: '2018-01-01T00:00:00.000Z', // 1514764800000
                timesheet: {
                    xaxis: [1, 2, 5],
                    yaxis: [
                        { b1: 0, b2: 0, s2: 0, c1: 8, c2: 2, c3: 0, c4: 0, m1: 2, m2: 20, k1: 0, n1: 0 },
                        { b1: 0, b2: 4, s2: 0, c1: 0, c2: 0, c3: 53, c4: 5, m1: 2, m2: 0, k1: 8, n1: 0 }, // <-
                        { b1: 0, b2: 4, s2: 44, c1: 0, c2: 0, c3: 0, c4: 5, m1: 2, m2: 0, k1: 8, n1: 0 } // <-
                    ]
                }
            };
            const videos = [
                {
                    name: '1.webm',
                    startTime: 1514764830000, // '2018-01-01T00:00:30.000Z'
                    duration: 600000 // 10 min
                }
            ];

            const actual = ProctorEdu.getVideosMarkup(sessionData, videos, false);

            const expected = [
                {
                    url: 'https://yastatic.net/s3/expert/testing/videos/1.webm',
                    intervals: [
                        { start: 30, end: 90 },
                        { start: 88, end: 270 }
                    ]
                }
            ];

            expect(actual).to.deep.equal(expected);
        });

        it('should return `[]` when sessionData is undefined', () => {
            const actual = ProctorEdu.getVideosMarkup(undefined, [], false);

            expect(actual).to.deep.equal([]);
        });

        it('should return all intervals when session data for trial with revision', () => {
            const sessionData = {
                startedAt: '2018-01-01T00:00:00.000Z', // 1514764800000
                timesheet: {
                    xaxis: [1, 2, 5],
                    yaxis: [
                        { b1: 0, b2: 0, s2: 0, c1: 0, c2: 0, c3: 0, c4: 0, m1: 0, m2: 0, k1: 0, n1: 0 },
                        { b1: 0, b2: 4, s2: 0, c1: 0, c2: 0, c3: 53, c4: 5, m1: 2, m2: 0, k1: 8, n1: 0 },
                        { b1: 0, b2: 4, s2: 0, c1: 0, c2: 0, c3: 0, c4: 5, m1: 2, m2: 0, k1: 8, n1: 0 }
                    ]
                }
            };
            const videos = [
                {
                    name: '1.webm',
                    startTime: 1514764830000, // '2018-01-01T00:00:30.000Z'
                    duration: 600000 // 10 min
                }
            ];

            const actual = ProctorEdu.getVideosMarkup(sessionData, videos, true);

            const expected = [
                {
                    url: 'https://yastatic.net/s3/expert/testing/videos/1.webm',
                    intervals: [
                        { start: 0, end: 30 },
                        { start: 30, end: 90 },
                        { start: 88, end: 270 }
                    ]
                }
            ];

            expect(actual).to.deep.equal(expected);
        });
    });

    describe('`isMetricsHigh`', () => {
        it('should return `true` when some of technical metrics are high', () => {
            const sessionData = {
                averages: { b2: 91, s2: 90, s1: 98, c1: 0, c2: 20, c3: 0, c4: 43, c5: 98, m1: 0, m2: 6, n1: 0 }
            };

            const actual = ProctorEdu.isMetricsHigh(sessionData);

            expect(actual).to.be.true;
        });

        it('should return `false` when all technical metrics are low', () => {
            const sessionData = {
                averages: { b2: 87, s2: 56, s1: 34, c1: 0, c2: 100, c3: 0, c4: 43, c5: 0, m1: 98, m2: 100, n1: 0 }
            };

            const actual = ProctorEdu.isMetricsHigh(sessionData);

            expect(actual).to.be.false;
        });
    });

    describe('`getUserData`', () => {
        before(() => {
            mockMailer();
            ProctorEdu = require('models/proctoring/proctorEdu');
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
            nock.cleanAll();
            log.error.restore();
        });

        it('should successfully get user data', function *() {
            const response = {
                similar: [
                    { user: 'expert', distance: 0.07100095407149228 },
                    { user: 'example', distance: 0.09195195926739229 }
                ]
            };

            nockProctorEdu.user({ login: 'pro-user', response });

            const actual = yield ProctorEdu.getUserData('pro-user');

            expect(actual).to.deep.equal(response);
            expect(log.error.notCalled).to.be.true;
        });

        it('should retry when statusCode is 5**', function *() {
            const response = {
                similar: [
                    { user: 'expert', distance: 0.07100095407149228 },
                    { user: 'example', distance: 0.09195195926739229 }
                ]
            };

            nockProctorEdu.retryUser({ login: 'retry-500-user', code: 500, response });

            const actual = yield ProctorEdu.getUserData('retry-500-user');

            expect(actual).to.deep.equal(response);
            expect(log.error.notCalled).to.be.true;
        });

        it('should not retry when statusCode is not 5**', function *() {
            nockProctorEdu.retryUser({ login: 'retry-400-user', code: 400 });

            const actual = yield ProctorEdu.getUserData('retry-400-user');

            expect(actual).to.be.undefined;
            expect(log.error.calledTwice).to.be.true;
        });

        it('should return `undefined` when request to ProctorEdu was failed', function *() {
            nockProctorEdu.user({ login: 'failed-user', code: 500 });

            const actual = yield ProctorEdu.getUserData('failed-user');

            expect(actual).to.be.undefined;
            expect(log.error.calledTwice).to.be.true;
        });

        it('should return `undefined` when parsing body was failed', function *() {
            nockProctorEdu.user({ login: 'incorrect-res-user', code: 200, response: 'data' });

            const actual = yield ProctorEdu.getUserData('incorrect-res-user');

            expect(actual).to.be.undefined;
            expect(log.error.calledOnce).to.be.true;
        });
    });

    describe('`concatVideos`', () => {
        function generateInputFiles(videoPathsConfig, fileNames, groupName) {
            const videoPaths = [];

            for (const fileName of fileNames) {
                const file = fs.readFileSync(`tests/models/data/webm/${fileName}`);
                const filePath = `${inputFilesPath}/${groupName}-${fileName}`;

                fs.writeFileSync(filePath, file);

                videoPaths.push(filePath);
            }

            const config = fs.readFileSync(`tests/models/data/webm/${videoPathsConfig}`);

            fs.writeFileSync(`${inputFilesPath}/${videoPathsConfig}`, config);

            return videoPaths;
        }

        before(() => {
            mockMailer();
            ProctorEdu = require('models/proctoring/proctorEdu');
        });

        after(() => {
            mockery.disable();
            mockery.deregisterAll();
        });

        beforeEach(() => {
            fs.mkdirSync(inputFilesPath);
        });

        afterEach(() => {
            removeDir(inputFilesPath);
            removeDir(outputFilesPath);
        });

        it('should concat vp8 video groups', function *() {
            const groupOneFilePaths = generateInputFiles(
                'group1.txt',
                ['first.webm', 'second.webm', 'third.webm'],
                'group-one'
            );
            const groupTwoFilePaths = generateInputFiles(
                'group2.txt',
                ['third.webm', 'first.webm'],
                'group-two'
            );
            const groupWithOneVideo = generateInputFiles(
                'group3.txt',
                ['second.webm'],
                'group-three'
            );
            const groupWithVideoOneSec = generateInputFiles(
                'group4.txt',
                ['first.webm', 'second.webm', 'third.webm', 'zero_time_one.webm'],
                'group-four'
            );

            const groups = [
                {
                    name: 'group-one',
                    videoPathsConfig: 'models/proctoring/input/group1.txt',
                    videoPaths: groupOneFilePaths
                },
                {
                    name: 'group-two',
                    videoPathsConfig: 'models/proctoring/input/group2.txt',
                    videoPaths: groupTwoFilePaths
                },
                {
                    name: 'group-three',
                    videoPathsConfig: 'models/proctoring/input/group3.txt',
                    videoPaths: groupWithOneVideo
                },
                {
                    name: 'group-four',
                    videoPathsConfig: 'models/proctoring/input/group4.txt',
                    videoPaths: groupWithVideoOneSec
                }
            ];

            const actual = yield ProctorEdu.concatVideos(groups, 123);

            expect(actual).to.deep.equal({
                'group-one': {
                    path: 'models/proctoring/output/group-one.webm',
                    duration: 179817
                },
                'group-two': {
                    path: 'models/proctoring/output/group-two.webm',
                    duration: 119878
                },
                'group-three': {
                    path: 'models/proctoring/output/group-three.webm',
                    duration: 59939
                },
                'group-four': {
                    path: 'models/proctoring/output/group-four.webm',
                    duration: 179818
                }
            });

            const inputFiles = fs.readdirSync(inputFilesPath);

            expect(inputFiles).to.deep.equal([]);
        });

        it('should concat h264 video group', function *() {
            const groupOneFilePaths = generateInputFiles(
                'h264.txt',
                ['h264-after-convert.webm', 'h264-after-convert-2.webm'],
                'group-h264'
            );

            const groups = [
                {
                    name: 'group-h264',
                    videoPathsConfig: 'models/proctoring/input/h264.txt',
                    videoPaths: groupOneFilePaths
                }
            ];

            const actual = yield ProctorEdu.concatVideos(groups, 123);

            expect(actual).to.deep.equal({
                'group-h264': {
                    path: 'models/proctoring/output/group-h264.webm',
                    duration: 117846
                }
            });

            const inputFiles = fs.readdirSync(inputFilesPath);

            expect(inputFiles).to.deep.equal([]);
        });

        it('should remove saved files when concat was failed', function *() {
            const failedGroupVideoPaths = generateInputFiles(
                'failed.txt',
                ['example.txt', 'first.webm'],
                'failed-group'
            );
            const successGroupVideoPaths = generateInputFiles(
                'success.txt',
                ['first.webm', 'second.webm'],
                'success-group'
            );

            const groups = [
                {
                    name: 'success-group',
                    videoPathsConfig: 'models/proctoring/input/success.txt',
                    videoPaths: successGroupVideoPaths
                },
                {
                    name: 'failed-group',
                    videoPathsConfig: 'models/proctoring/input/failed.txt',
                    videoPaths: failedGroupVideoPaths
                }
            ];

            const error = yield catchError(ProctorEdu.concatVideos.bind(ProctorEdu, groups, 12345));

            expect(error.statusCode).to.equal(500);
            expect(error.message).to.equal('Concatenate videos was failed');
            expect(error.options.internalCode).to.equal('500_CVF');
            expect(error.options.trialId).to.equal(12345);

            const inputFiles = fs.readdirSync(inputFilesPath);
            const outputFiles = fs.readdirSync(outputFilesPath);

            expect(inputFiles).to.deep.equal([]);
            expect(outputFiles).to.deep.equal([]);
        });
    });

    describe('`splitVideosIntoGroups`', () => {
        it('should correct split videos into groups', () => {
            // event = 1 webcam + 1 screen
            const tsWebcam1 = 1589362246565;
            const tsScreen1 = 1589362246528;

            const tsWebcam4 = tsWebcam1 + 240327;
            const tsScreen4 = tsScreen1 + 242924;

            const tsWebcam6 = tsWebcam1 + 604723;
            const tsScreen6 = tsScreen1 + 604922;

            const tsWebcam7 = tsWebcam1 + 1073594;
            const tsScreen7 = tsScreen1 + 1073687;

            const videos = [
                // 1 event
                {
                    videoId: '1_w',
                    startTime: tsWebcam1,
                    duration: 59800,
                    videoPath: '1_w_p',
                    source: 'webcam'
                },
                {
                    videoId: '1_s',
                    startTime: tsScreen1,
                    duration: 59868,
                    videoPath: '1_s_p',
                    source: 'screen'
                },
                // 2 event, after 1
                {
                    videoId: '2_w',
                    startTime: tsWebcam1 + 59840,
                    duration: 59934,
                    videoPath: '2_w_p',
                    source: 'webcam'
                },
                {
                    videoId: '2_s',
                    startTime: tsScreen1 + 59872,
                    duration: 59675,
                    videoPath: '2_s_p',
                    source: 'screen'
                },
                // 3 event, after 2
                {
                    videoId: '3_w',
                    startTime: tsWebcam1 + 119983,
                    duration: 60000,
                    videoPath: '3_w_p',
                    source: 'webcam'
                },
                {
                    videoId: '3_s',
                    startTime: tsScreen1 + 120187,
                    duration: 60000,
                    videoPath: '3_s_p',
                    source: 'screen'
                },
                // 4 event
                {
                    videoId: '4_w',
                    startTime: tsWebcam4,
                    duration: 59435,
                    videoPath: '4_w_p',
                    source: 'webcam'
                },
                {
                    videoId: '4_s',
                    startTime: tsScreen4,
                    duration: 59800,
                    videoPath: '4_s_p',
                    source: 'screen'
                },
                // 5 event, after 4 without screen
                {
                    videoId: '5_w',
                    startTime: tsWebcam4 + 59588,
                    duration: 59800,
                    videoPath: '5_w_p',
                    source: 'webcam'
                },
                // 6 event, separately
                {
                    videoId: '6_w',
                    startTime: tsWebcam6,
                    duration: 60000,
                    videoPath: '6_w_p',
                    source: 'webcam'
                },
                {
                    videoId: '6_s',
                    startTime: tsScreen6,
                    duration: 60000,
                    videoPath: '6_s_p',
                    source: 'screen'
                },
                // 7 event, penultimate
                {
                    videoId: '7_w',
                    startTime: tsWebcam7,
                    duration: 59900,
                    videoPath: '7_w_p',
                    source: 'webcam'
                },
                {
                    videoId: '7_s',
                    startTime: tsScreen7,
                    duration: 59800,
                    videoPath: '7_s_p',
                    source: 'screen'
                },
                // 8 event, after 7
                {
                    videoId: '8_w',
                    startTime: tsWebcam7 + 59957,
                    duration: 59800,
                    videoPath: '8_w_p',
                    source: 'webcam'
                },
                {
                    videoId: '8_s',
                    startTime: tsScreen7 + 59981,
                    duration: 59800,
                    videoPath: '8_s_p',
                    source: 'screen'
                }
            ];

            const trialId = 12345;
            const actual = ProctorEdu.splitVideosIntoGroups(videos, trialId);
            const actualWithoutNames = actual.map(group => _.omit(group, 'name'));
            const actualNames = actual.map(group => group.name);
            const isStartWithTrialId = actualNames.every(name => name.startsWith(`${trialId}_`));

            expect(isStartWithTrialId).to.be.true;
            expect(actualWithoutNames).to.deep.equal([
                {
                    videoIds: [
                        '1_w',
                        '2_w',
                        '3_w'
                    ],
                    videoPaths: [
                        '1_w_p',
                        '2_w_p',
                        '3_w_p'
                    ],
                    startTime: 1589362246565,
                    source: 'webcam'
                },
                {
                    videoIds: [
                        '4_w',
                        '5_w'
                    ],
                    videoPaths: [
                        '4_w_p',
                        '5_w_p'
                    ],
                    startTime: 1589362486892,
                    source: 'webcam'
                },
                {
                    videoIds: [
                        '6_w'
                    ],
                    videoPaths: [
                        '6_w_p'
                    ],
                    startTime: 1589362851288,
                    source: 'webcam'
                },
                {
                    videoIds: [
                        '7_w',
                        '8_w'
                    ],
                    videoPaths: [
                        '7_w_p',
                        '8_w_p'
                    ],
                    startTime: 1589363320159,
                    source: 'webcam'
                },
                {
                    videoIds: [
                        '1_s',
                        '2_s',
                        '3_s'
                    ],
                    videoPaths: [
                        '1_s_p',
                        '2_s_p',
                        '3_s_p'
                    ],
                    startTime: 1589362246528,
                    source: 'screen'
                },
                {
                    videoIds: [
                        '4_s'
                    ],
                    videoPaths: [
                        '4_s_p'
                    ],
                    startTime: 1589362489452,
                    source: 'screen'
                },
                {
                    videoIds: [
                        '6_s'
                    ],
                    videoPaths: [
                        '6_s_p'
                    ],
                    startTime: 1589362851450,
                    source: 'screen'
                },
                {
                    videoIds: [
                        '7_s',
                        '8_s'
                    ],
                    videoPaths: [
                        '7_s_p',
                        '8_s_p'
                    ],
                    startTime: 1589363320215,
                    source: 'screen'
                }
            ]);
        });

        it('should return `[]` when array of videos is empty', () => {
            const actual = ProctorEdu.splitVideosIntoGroups([]);

            expect(actual).to.deep.equal([]);
        });
    });

    describe('`downloadVideos`', () => {
        before(() => {
            mockMailer();
            ProctorEdu = require('models/proctoring/proctorEdu');
        });

        after(() => {
            mockery.disable();
            mockery.deregisterAll();
        });

        afterEach(() => {
            nock.cleanAll();

            removeDir(inputFilesPath);
        });

        // test ~10 sec on mac because converting is a long operation
        it('should download videos', function *() {
            const firstVideo = fs.readFileSync('tests/models/data/webm/first.webm');
            const secondVideo = fs.readFileSync('tests/models/data/webm/group-one.webm');

            nockProctorEdu.file({ id: 'video1', code: 200, response: firstVideo });
            nockProctorEdu.file({ id: 'video2', code: 200, response: secondVideo });

            const videosData = [
                {
                    id: 'video1',
                    eventSaveTime: 60050,
                    source: 'webcam'
                },
                {
                    id: 'video2',
                    eventSaveTime: 180150,
                    source: 'screen'
                }
            ];

            const actual = yield ProctorEdu.downloadVideos(videosData, 'open-id');

            expect(actual).to.deep.equal([
                {
                    videoId: 'video1',
                    startTime: 110,
                    source: 'webcam',
                    videoPath: `${inputFilesPath}/video1.webm`,
                    duration: 59940
                },
                {
                    videoId: 'video2',
                    startTime: 330,
                    source: 'screen',
                    videoPath: `${inputFilesPath}/video2.webm`,
                    duration: 179820
                }
            ]);

            const inputDirFiles = fs.readdirSync(inputFilesPath).sort();

            expect(inputDirFiles).to.deep.equal([
                'video1.webm',
                'video2.webm'
            ]);
        });

        it('should download videos. Duration first video is 0 second.', function *() {
            const firstVideo = fs.readFileSync('tests/models/data/webm/zero_time_one.webm');
            const secondVideo = fs.readFileSync('tests/models/data/webm/group-one.webm');

            nockProctorEdu.file({ id: 'video1', code: 200, response: firstVideo });
            nockProctorEdu.file({ id: 'video2', code: 200, response: secondVideo });

            const videosData = [
                {
                    id: 'video1',
                    eventSaveTime: 0,
                    source: 'webcam'
                },
                {
                    id: 'video2',
                    eventSaveTime: 180150,
                    source: 'screen'
                }
            ];

            const actual = yield ProctorEdu.downloadVideos(videosData, 'open-id');

            expect(actual).to.deep.equal([
                {
                    videoId: 'video1',
                    startTime: 0,
                    source: 'webcam',
                    videoPath: `${inputFilesPath}/video1.webm`,
                    duration: 0
                },
                {
                    videoId: 'video2',
                    startTime: 330,
                    source: 'screen',
                    videoPath: `${inputFilesPath}/video2.webm`,
                    duration: 179820
                }
            ]);

            const inputDirFiles = fs.readdirSync(inputFilesPath).sort();

            expect(inputDirFiles).to.deep.equal([
                'video1.webm',
                'video2.webm'
            ]);
        });

        it('should download videos. Durations videos are 0 second.', function *() {
            const firstVideo = fs.readFileSync('tests/models/data/webm/zero_time_one.webm');
            const secondVideo = fs.readFileSync('tests/models/data/webm/zero_time_two.webm');

            nockProctorEdu.file({ id: 'video1', code: 200, response: firstVideo });
            nockProctorEdu.file({ id: 'video2', code: 200, response: secondVideo });

            const videosData = [
                {
                    id: 'video1',
                    eventSaveTime: 0,
                    source: 'webcam'
                },
                {
                    id: 'video2',
                    eventSaveTime: 0,
                    source: 'screen'
                }
            ];

            const actual = yield ProctorEdu.downloadVideos(videosData, 'open-id');

            expect(actual).to.deep.equal([
                {
                    videoId: 'video1',
                    startTime: 0,
                    source: 'webcam',
                    videoPath: `${inputFilesPath}/video1.webm`,
                    duration: 0
                },
                {
                    videoId: 'video2',
                    startTime: 0,
                    source: 'screen',
                    videoPath: `${inputFilesPath}/video2.webm`,
                    duration: 0
                }
            ]);

            const inputDirFiles = fs.readdirSync(inputFilesPath).sort();

            expect(inputDirFiles).to.deep.equal([
                'video1.webm',
                'video2.webm'
            ]);
        });

        it('should return `[]` when array with video ids is empty', function *() {
            const actual = yield ProctorEdu.downloadVideos([], 'open-id');
            const inputDirFiles = fs.readdirSync(inputFilesPath);

            expect(actual).to.deep.equal([]);
            expect(inputDirFiles).to.deep.equal([]);
        });

        it('should throw error when video not loaded', function *() {
            nockProctorEdu.file({ id: 'video', code: 500 });

            const videosData = [
                {
                    id: 'video',
                    eventSaveTime: 123,
                    source: 'webcam'
                }
            ];

            const error = yield catchError(ProctorEdu.downloadVideos.bind(ProctorEdu, videosData, 'open-id'));

            expect(error.statusCode).to.equal(424);
            expect(error.message).to.equal('ProctorEdu video not loaded');
            expect(error.options).to.deep.equal({
                internalCode: '424_VNL',
                openId: 'open-id',
                videoId: 'video'
            });
        });

        it('should throw error when ffmpeg processing was failed', function *() {
            const incorrectVideo = fs.readFileSync('tests/models/data/webm/example.txt');

            nockProctorEdu.file({ id: 'video', code: 200, response: incorrectVideo });

            const videosData = [
                {
                    id: 'video',
                    eventSaveTime: 123,
                    source: 'webcam'
                }
            ];

            const error = yield catchError(ProctorEdu.downloadVideos.bind(ProctorEdu, videosData, 'open-id'));

            expect(error.statusCode).to.equal(500);
            expect(error.message).to.equal('Processing video with ffmpeg was failed');
            expect(error.options.internalCode).to.equal('500_PFF');
            expect(error.options.openId).to.equal('open-id');
            expect(error.options.videoId).to.equal('video');

            const inputDirFiles = fs.readdirSync(inputFilesPath);

            expect(inputDirFiles).to.deep.equal([]);
        });

        // In addition to vp8 the codec can be h264
        // test ~3 sec on mac because converting is a long operation
        it('should correct process webm video with codec h264', function *() {
            const videoH264 = fs.readFileSync('tests/models/data/webm/h264.webm');

            nockProctorEdu.file({ id: 'videoH264', code: 200, response: videoH264 });

            const videosData = [
                {
                    id: 'videoH264',
                    eventSaveTime: 60000,
                    source: 'webcam'
                }
            ];

            const actual = yield ProctorEdu.downloadVideos(videosData, 'open-id');

            expect(actual).to.deep.equal([
                {
                    videoId: 'videoH264',
                    startTime: 1080,
                    source: 'webcam',
                    videoPath: `${inputFilesPath}/videoH264.webm`,
                    duration: 58920
                }
            ]);

            const inputDirFiles = fs.readdirSync(inputFilesPath).sort();

            expect(inputDirFiles).to.deep.equal(['videoH264.webm']);
        });
    });

    describe('`getConcatenatedVideos`', () => {
        before(() => {
            mockMailer();
            ProctorEdu = require('models/proctoring/proctorEdu');
        });

        after(() => {
            mockery.disable();
            mockery.deregisterAll();
        });

        beforeEach(() => {
            sinon.stub(ProctorEdu, 'concatVideos', groups => {
                return groups.reduce((videosByName, group) => {
                    videosByName[group.name] = {
                        path: `${outputFilesPath}/${group.name}.webm`,
                        duration: 120000
                    };

                    return videosByName;
                }, {});
            });

            sinon.stub(ProctorEdu, '_downloadVideoThroughFfmpeg', (video, videoId) => {
                return Promise.resolve({
                    videoPath: `models/proctoring/input/${videoId}.webm`,
                    duration: 60000
                });
            });
        });

        afterEach(() => {
            nock.cleanAll();
            ProctorEdu.concatVideos.restore();
            ProctorEdu._downloadVideoThroughFfmpeg.restore();

            removeDir(inputFilesPath);
        });

        it('should return saved videos data', function *() {
            const response = [
                {
                    attach: [
                        {
                            id: 'video1',
                            filename: 'webcam.webm',
                            mimetype: 'video/webm',
                            createdAt: '2020-05-13T09:30:46.565Z' // 1589362246565
                        },
                        {
                            id: 'video2',
                            filename: 'screen.webm',
                            mimetype: 'video/webm',
                            createdAt: '2020-05-13T09:30:46.528Z' // 1589362246528
                        }
                    ]
                },
                {
                    attach: [
                        {
                            id: 'video3',
                            filename: 'webcam.webm',
                            mimetype: 'video/webm',
                            createdAt: '2020-05-13T09:31:46.405Z' // 1589362306405
                        },
                        {
                            id: 'video4',
                            filename: 'screen.webm',
                            mimetype: 'video/webm',
                            createdAt: '2020-05-13T09:31:46.400Z' // 1589362306400
                        }
                    ]
                }
            ];

            nockProctorEdu.events({ openId: 'open-id', response });
            nockProctorEdu.file({ id: 'video1', code: 200, response: 'video1' });
            nockProctorEdu.file({ id: 'video2', code: 200, response: 'video2' });
            nockProctorEdu.file({ id: 'video3', code: 200, response: 'video3' });
            nockProctorEdu.file({ id: 'video4', code: 200, response: 'video4' });

            const actual = yield ProctorEdu.getConcatenatedVideos('open-id', 12345);

            const actualWithoutNames = actual.map(group => _.omit(group, ['name', 'videoPath']));
            const isCorrectPaths = actual.every(data => data.videoPath === `models/proctoring/output/${data.name}`);

            expect(isCorrectPaths).to.be.true;
            expect(actualWithoutNames).to.deep.equal([
                {
                    startTime: 1589362186565,
                    duration: 120000,
                    source: 'webcam'
                },
                {
                    startTime: 1589362186528,
                    duration: 120000,
                    source: 'screen'
                }
            ]);
        });

        it('should return `[]` when videos list is empty', function *() {
            nockProctorEdu.events({ openId: 'open-id', response: [] });

            const actual = yield ProctorEdu.getConcatenatedVideos('open-id', 12345);

            expect(actual).to.deep.equal([]);
        });

        it('should throw error when videos data not loaded', function *() {
            nockProctorEdu.events({ openId: 'open-id', code: 500 });

            const error = yield catchError(ProctorEdu.getConcatenatedVideos.bind(ProctorEdu, 'open-id', 12345));

            expect(error.statusCode).to.equal(424);
            expect(error.message).to.equal('ProctorEdu videos data not loaded');
            expect(error.options).to.deep.equal({
                internalCode: '424_VDL',
                openId: 'open-id'
            });
        });
    });
});
