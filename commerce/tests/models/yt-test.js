const { yt } = require('yandex-config');
const { expect } = require('chai');
let log = require('logger');
const mockery = require('mockery');
const moment = require('moment');
const nock = require('nock');
const sinon = require('sinon');

let YT = require('models/yt');

const catchError = require('tests/helpers/catchError').generator;
const mockCache = require('tests/helpers/cache');
const nockYT = require('tests/helpers/yt');

describe('YT Model', () => {
    const correctAnswer = { statusCode: 200 };
    const writeBody = 'write-body';
    const heavyProxy = 'heavy-proxy';
    const rows = [
        { trialId: '123', userPhoto: '456', videos: '...' },
        { trialId: '321', userPhoto: '654', videos: '...' }
    ];
    const directory = ['2018-07-31_13:26:35', '2018-08-03_13:45:42'];
    const tables = [
        {
            name: '2018-07-31_13:26:35',
            rows
        },
        {
            name: '2018-08-03_13:45:42',
            rows
        }
    ];
    const map = rows
        .map(JSON.stringify)
        .join('\n');

    before(() => {
        mockCache();

        YT = require('models/yt');
        log = require('logger');
    });

    after(() => {
        mockery.disable();
        mockery.deregisterAll();
    });

    beforeEach(() => {
        sinon.spy(log, 'warn');
    });

    afterEach(() => {
        log.warn.restore();
        nock.cleanAll();
    });

    describe('`upload`', () => {
        it('should correctly upload trials', function *() {
            const { lightNock, heavyNock } = nockYT({
                create: { response: correctAnswer },
                proxy: { response: [heavyProxy] },
                write: { response: writeBody }
            });

            const body = yield YT.upload(rows);

            expect(body).to.deep.equal(writeBody);

            expect(lightNock.isDone()).to.be.true;
            expect(heavyNock.isDone()).to.be.true;
        });

        it('should throw 500 when can not create YT table', function *() {
            const { lightNock } = nockYT({
                create: { code: 500 }
            });

            const error = yield catchError(YT.upload.bind(YT, rows));

            expect(error.statusCode).to.equal(500);
            expect(error.message).to.equal('Table not created');
            expect(error.options).to.deep.equal({ internalCode: '500_TNC' });

            expect(log.warn.calledOnce).to.be.true;
            expect(lightNock.isDone()).to.be.true;
        });

        it('should throw 500 when can not get heavy proxy', function *() {
            const { lightNock } = nockYT({
                create: { response: correctAnswer },
                proxy: { code: 500 }
            });

            const error = yield catchError(YT.upload.bind(YT, rows));

            expect(error.statusCode).to.equal(500);
            expect(error.message).to.equal('No available YT heavy proxy');
            expect(error.options).to.deep.equal({ internalCode: '500_NAP' });

            expect(log.warn.calledOnce).to.be.true;
            expect(lightNock.isDone()).to.be.true;
        });

        it('should throw 500 when there is no available proxy', function *() {
            const { lightNock } = nockYT({
                create: { response: correctAnswer },
                proxy: { response: [] }
            });

            const error = yield catchError(YT.upload.bind(YT, rows));

            expect(error.statusCode).to.equal(500);
            expect(error.message).to.equal('No available YT heavy proxy');
            expect(error.options).to.deep.equal({ internalCode: '500_NAP' });

            expect(log.warn.notCalled).to.be.true;
            expect(lightNock.isDone()).to.be.true;
        });

        it('should throw 500 when can not write to YT', function *() {
            const { lightNock } = nockYT({
                create: { response: correctAnswer },
                proxy: { response: [heavyProxy] },
                write: { code: 500 }
            });

            const error = yield catchError(YT.upload.bind(YT, rows));

            expect(error.statusCode).to.equal(500);
            expect(error.message).to.equal('Can not write data to YT');
            expect(error.options).to.deep.equal({ internalCode: '500_CWD' });

            expect(log.warn.calledOnce).to.be.true;
            expect(lightNock.isDone()).to.be.true;
        });
    });

    describe('loadResults', () => {
        it('should correctly load results from YT', function *() {
            const { lightNock, heavyNock } = nockYT({
                proxy: { response: [heavyProxy], times: 2 },
                list: { response: directory },
                read: { response: map, times: 2 }
            });

            const actual = yield YT.loadResults();

            expect(actual).to.deep.equal(tables);

            expect(lightNock.isDone()).to.be.true;
            expect(heavyNock.isDone()).to.be.true;
        });

        it('should throw 500 when can not list YT tables', function *() {
            const { lightNock } = nockYT({
                list: { code: 500 }
            });

            const error = yield catchError(YT.loadResults.bind(YT));

            expect(error.statusCode).to.equal(500);
            expect(error.message).to.equal('Can not list YT tables');
            expect(error.options).to.deep.equal({ internalCode: '500_CLT' });

            expect(log.warn.calledOnce).to.be.true;
            expect(lightNock.isDone()).to.be.true;
        });

        it('should throw 500 when can not read YT table', function *() {
            const { lightNock, heavyNock } = nockYT({
                proxy: { response: [heavyProxy], times: 2 },
                list: { response: directory },
                read: { code: 500, times: 2 }
            });

            const error = yield catchError(YT.loadResults.bind(YT));

            expect(error.statusCode).to.equal(500);
            expect(error.message).to.match(/^Can not read data from YT table: .+$/);
            expect(error.options).to.deep.equal({ internalCode: '500_CRD' });

            expect(log.warn.calledTwice).to.be.true;
            expect(lightNock.isDone()).to.be.true;
            expect(heavyNock.isDone()).to.be.true;
        });

        it('should throw 500 when can not get YT heavy proxy', function *() {
            const { lightNock } = nockYT({
                list: { response: directory },
                proxy: { code: 500, times: 2 }
            });

            const error = yield catchError(YT.loadResults.bind(YT));

            expect(error.statusCode).to.equal(500);
            expect(error.message).to.equal('No available YT heavy proxy');
            expect(error.options).to.deep.equal({ internalCode: '500_NAP' });

            expect(log.warn.calledTwice).to.be.true;
            expect(lightNock.isDone()).to.be.true;
        });

        it('should throw 500 when there is no available YT heavy proxy', function *() {
            const { lightNock } = nockYT({
                proxy: { response: [], times: 2 },
                list: { response: directory }
            });

            const error = yield catchError(YT.loadResults.bind(YT));

            expect(error.statusCode).to.equal(500);
            expect(error.message).to.equal('No available YT heavy proxy');
            expect(error.options).to.deep.equal({ internalCode: '500_NAP' });

            expect(log.warn.notCalled).to.be.true;
            expect(lightNock.isDone()).to.be.true;
        });
    });

    describe('`moveToArchive`', () => {
        it('should move tables to archive', function *() {
            const { lightNock, heavyNock } = nockYT({
                proxy: { response: [heavyProxy], times: 2 },
                create: { response: correctAnswer, times: 2 },
                write: { response: writeBody, times: 2 },
                remove: { response: correctAnswer, times: 2 }
            });

            const actual = yield YT.moveToArchive(tables);
            const expected = [correctAnswer, correctAnswer]
                .map(JSON.stringify);

            expect(actual).to.deep.equal(expected);
            expect(lightNock.isDone()).to.be.true;
            expect(heavyNock.isDone()).to.be.true;
        });

        it('should throw 500 when can not create YT table', function *() {
            const { lightNock } = nockYT({
                create: { code: 500, times: 2 }
            });

            const error = yield catchError(YT.moveToArchive.bind(YT, tables));

            expect(error.statusCode).to.equal(500);
            expect(error.message).to.equal('Table not created');
            expect(error.options).to.deep.equal({ internalCode: '500_TNC' });

            expect(log.warn.calledTwice).to.be.true;
            expect(lightNock.isDone()).to.be.true;
        });

        it('should throw 500 when can not write data to YT table', function *() {
            const { lightNock, heavyNock } = nockYT({
                create: { response: correctAnswer, times: 2 },
                proxy: { response: [heavyProxy], times: 2 },
                write: { code: 500, times: 2 }
            });

            const error = yield catchError(YT.moveToArchive.bind(YT, tables));

            expect(error.statusCode).to.equal(500);
            expect(error.message).to.equal('Can not write data to YT');
            expect(error.options).to.deep.equal({ internalCode: '500_CWD' });

            expect(log.warn.calledTwice).to.be.true;
            expect(lightNock.isDone()).to.be.true;
            expect(heavyNock.isDone()).to.be.true;
        });

        it('should throw 500 when can not remove YT table', function *() {
            const { lightNock, heavyNock } = nockYT({
                create: { response: correctAnswer, times: 2 },
                proxy: { response: [heavyProxy], times: 2 },
                write: { response: writeBody, times: 2 },
                remove: { code: 500, times: 2 }
            });

            const error = yield catchError(YT.moveToArchive.bind(YT, tables));

            expect(error.statusCode).to.equal(500);
            expect(error.message).to.match(/Can not remove table/);
            expect(error.options).to.deep.equal({ internalCode: '500_CRT' });

            expect(log.warn.calledTwice).to.be.true;
            expect(lightNock.isDone()).to.be.true;
            expect(heavyNock.isDone()).to.be.true;
        });

        it('should throw 500 when can not get YT heavy proxy', function *() {
            const { lightNock } = nockYT({
                create: { response: correctAnswer, times: 2 },
                proxy: { code: 500, times: 2 }
            });

            const error = yield catchError(YT.moveToArchive.bind(YT, tables));

            expect(error.statusCode).to.equal(500);
            expect(error.message).to.equal('No available YT heavy proxy');
            expect(error.options).to.deep.equal({ internalCode: '500_NAP' });

            expect(log.warn.calledTwice).to.be.true;
            expect(lightNock.isDone()).to.be.true;
        });

        it('should throw 500 when there is no available YT heavy proxy', function *() {
            const { lightNock } = nockYT({
                proxy: { response: [], times: 2 },
                create: { response: correctAnswer, times: 2 }
            });

            const error = yield catchError(YT.moveToArchive.bind(YT, tables));

            expect(error.statusCode).to.equal(500);
            expect(error.message).to.equal('No available YT heavy proxy');
            expect(error.options).to.deep.equal({ internalCode: '500_NAP' });

            expect(log.warn.notCalled).to.be.true;
            expect(lightNock.isDone()).to.be.true;
        });
    });

    describe('aggregateResults', () => {
        it('should return empty object for empty rows', () => {
            const otherTables = [{ name: '2018-07-31_13:26:35', rows: [] }];
            const actual = YT.aggregateResults(otherTables);

            expect(actual).to.deep.equal({});
        });

        it('should return most frequent answer for interval', () => {
            const otherTables = [
                {
                    name: '2018-07-31_13:26:35',
                    rows: [
                        {
                            trialId: 1,
                            start: 0,
                            end: 30,
                            violations: false,
                            isRevision: false
                        },
                        {
                            trialId: 1,
                            start: 0,
                            end: 30,
                            violations: true,
                            isRevision: false
                        },
                        {
                            trialId: 1,
                            start: 0,
                            end: 30,
                            violations: true,
                            isRevision: false
                        }
                    ]
                }
            ];

            const actual = YT.aggregateResults(otherTables);

            expect(actual[1].isViolationsExist).to.be.true;
            expect(actual[1].isRevision).to.be.false;
        });

        it('should group by trialId', () => {
            const otherTables = [
                {
                    name: '2018-07-31_13:26:35',
                    rows: [
                        {
                            trialId: 1,
                            start: 0,
                            end: 30,
                            violations: false,
                            isRevision: false
                        },
                        {
                            trialId: 2,
                            start: 0,
                            end: 30,
                            violations: true,
                            isRevision: true
                        }
                    ]
                }
            ];

            const actual = YT.aggregateResults(otherTables);

            expect(actual[1].isViolationsExist).to.be.false;
            expect(actual[1].isRevision).to.be.false;

            expect(actual[2].isViolationsExist).to.be.true;
            expect(actual[2].isRevision).to.be.true;
        });

        it('should set violation if some interval has violation for one trialId', () => {
            const otherTables = [
                {
                    name: '2018-07-31_13:26:35',
                    rows: [
                        {
                            trialId: 1,
                            start: 0,
                            end: 30,
                            violations: false,
                            isRevision: false
                        },
                        {
                            trialId: 1,
                            start: 31,
                            end: 60,
                            violations: true,
                            isRevision: false
                        }
                    ]
                }
            ];

            const actual = YT.aggregateResults(otherTables);

            expect(actual[1].isViolationsExist).to.be.true;
            expect(actual[1].isRevision).to.be.false;
        });

        it('should take rows from different tables', () => {
            const otherTables = [
                {
                    name: '2018-07-31_13:26:35',
                    rows: [
                        {
                            trialId: 1,
                            start: 0,
                            end: 30,
                            violations: false,
                            isRevision: false
                        }
                    ]
                },
                {
                    name: '2018-08-31_13:26:35',
                    rows: [
                        {
                            trialId: 2,
                            start: 60,
                            end: 120,
                            violations: true,
                            isRevision: false
                        }
                    ]
                }
            ];

            const actual = YT.aggregateResults(otherTables);

            expect(actual[1].isViolationsExist).to.be.false;
            expect(actual[1].isRevision).to.be.false;

            expect(actual[2].isViolationsExist).to.be.true;
            expect(actual[2].isRevision).to.be.false;
        });

        it('should return tolokers answers', () => {
            const otherTables = [
                {
                    name: '2018-07-31_13:26:35',
                    rows: [
                        {
                            trialId: 1,
                            start: 0,
                            end: 30,
                            violations: false,
                            isRevision: false,
                            'no_vio_audio_problems': false,
                            'no_vio_no_relate': false,
                            'no_vio_other': false,
                            'no_vio_other_text': null,
                            'no_vio_video_problems': false,
                            'vio_cheating': false,
                            'vio_diff_user': false,
                            'vio_other': false,
                            'vio_other_people': false,
                            'vio_other_text': null,
                            'vio_tips': false,
                            'vio_walk_away_screen': false
                        },
                        {
                            trialId: 1,
                            start: 0,
                            end: 30,
                            violations: false,
                            isRevision: false,
                            'no_vio_audio_problems': true,
                            'no_vio_no_relate': true,
                            'no_vio_other': true,
                            'no_vio_other_text': 'no vio',
                            'no_vio_video_problems': true,
                            'vio_cheating': true,
                            'vio_diff_user': true,
                            'vio_other': true,
                            'vio_other_people': true,
                            'vio_other_text': 'vio',
                            'vio_tips': true,
                            'vio_walk_away_screen': true
                        },
                        {
                            trialId: 1,
                            start: 31,
                            end: 60,
                            violations: false,
                            isRevision: false,
                            'no_vio_audio_problems': false,
                            'no_vio_no_relate': false,
                            'no_vio_other': false,
                            'no_vio_other_text': null,
                            'no_vio_video_problems': false,
                            'vio_cheating': false,
                            'vio_diff_user': false,
                            'vio_other': false,
                            'vio_other_people': false,
                            'vio_other_text': null,
                            'vio_tips': false,
                            'vio_walk_away_screen': false
                        }
                    ]
                }
            ];

            const actual = YT.aggregateResults(otherTables);

            expect(actual[1].intervals).to.deep.equal([
                {
                    start: 0,
                    end: 30,
                    hasViolations: false,
                    answers: [
                        {
                            noVioAudioProblems: false,
                            noVioNoRelate: false,
                            noVioOther: false,
                            noVioOtherText: null,
                            noVioVideoProblems: false,
                            vioCheating: false,
                            vioDiffUser: false,
                            vioOther: false,
                            vioOtherPeople: false,
                            vioOtherText: null,
                            vioTips: false,
                            vioWalkAwayScreen: false,
                            violations: false
                        },
                        {
                            noVioAudioProblems: true,
                            noVioNoRelate: true,
                            noVioOther: true,
                            noVioOtherText: 'no vio',
                            noVioVideoProblems: true,
                            vioCheating: true,
                            vioDiffUser: true,
                            vioOther: true,
                            vioOtherPeople: true,
                            vioOtherText: 'vio',
                            vioTips: true,
                            vioWalkAwayScreen: true,
                            violations: false
                        }
                    ]
                },
                {
                    start: 31,
                    end: 60,
                    hasViolations: false,
                    answers: [
                        {
                            noVioAudioProblems: false,
                            noVioNoRelate: false,
                            noVioOther: false,
                            noVioOtherText: null,
                            noVioVideoProblems: false,
                            vioCheating: false,
                            vioDiffUser: false,
                            vioOther: false,
                            vioOtherPeople: false,
                            vioOtherText: null,
                            vioTips: false,
                            vioWalkAwayScreen: false,
                            violations: false
                        }
                    ]
                }
            ]);
        });
    });

    describe('`uploadReports`', () => {
        const reportsData = {
            certificatesDetailed: [
                {
                    period: 'Июнь 2019',
                    directProCertificatesCount: 1,
                    directProSuccessRate: 100,
                    directProTrialsCount: 1,
                    metrikaCertificatesCount: 0,
                    metrikaSuccessRate: 0,
                    metrikaTrialsCount: 1
                }
            ],
            certificationSummary: [
                {
                    period: 'Июнь 2019',
                    trialsCount: 2,
                    certificatesCount: 2,
                    successRate: 100
                }
            ],
            loginsCertificatesSummary: [
                {
                    period: 'Июнь 2019',
                    loginsWithCerts: 1,
                    loginsWithSeveralCerts: 0,
                    multiCertsRate: 0,
                    popularCertsCombination: ''
                }
            ]
        };

        it('should write reports data to tables', function *() {
            const { lightNock, heavyNock } = nockYT({
                proxy: { response: [heavyProxy], times: 3 },
                write: { response: writeBody, times: 3 }
            });

            yield YT.uploadReports(reportsData);

            expect(log.warn.called).to.be.false;
            expect(lightNock.isDone()).to.be.true;
            expect(heavyNock.isDone()).to.be.true;
        });

        it('should throw 500 when can not write to YT', function *() {
            const { lightNock, heavyNock } = nockYT({
                proxy: { response: [heavyProxy] },
                write: { code: 500 }
            });

            const error = yield catchError(YT.uploadReports.bind(YT, reportsData));

            expect(error.statusCode).to.equal(500);
            expect(error.message).to.equal('Can not write data to YT');
            expect(error.options).to.deep.equal({ internalCode: '500_CWD' });

            expect(log.warn.calledOnce).to.be.true;
            expect(lightNock.isDone()).to.be.true;
            expect(heavyNock.isDone()).to.be.true;
        });
    });

    describe(`getTablesByDirs`, () => {
        it('should get and return correct tables data', function *() {
            const { lightNock: firstLightNock } = nockYT({
                list: {
                    response: [
                        {
                            $value: 'one',
                            $attributes: {
                                'creation_time': '2018-07-31T10:00:00.000Z'
                            }
                        },
                        {
                            $value: 'two',
                            $attributes: {
                                'creation_time': '2018-11-31T10:00:00.000Z'
                            }
                        }
                    ]
                },
                query: {
                    path: `${yt.path}/archive/input`,
                    attributes: ['creation_time']
                }
            });

            const { lightNock: secondLightNock } = nockYT({
                list: {
                    response: [
                        {
                            $value: 'three',
                            $attributes: {
                                'creation_time': '2020-07-07T10:00:00.000Z'
                            }
                        }
                    ]
                },
                query: {
                    path: `${yt.path}/archive/output`,
                    attributes: ['creation_time']
                }
            });

            const { lightNock: thirdLightNock } = nockYT({
                list: {
                    response: [
                        {
                            $value: 'four',
                            $attributes: {
                                'creation_time': '2019-07-31T10:00:00.000Z'
                            }
                        },
                        {
                            $value: 'five',
                            $attributes: {
                                'creation_time': '2019-01-01T10:00:00.000Z'
                            }
                        }
                    ]
                },
                query: {
                    path: `${yt.path}/input`,
                    attributes: ['creation_time']
                }
            });

            const { lightNock: fourthLightNock } = nockYT({
                list: {
                    response: [
                        {
                            $value: 'six',
                            $attributes: {
                                'creation_time': '2018-07-31T10:00:00.000Z'
                            }
                        }
                    ]
                },
                query: {
                    path: `${yt.path}/output`,
                    attributes: ['creation_time']
                }
            });

            const { lightNock: fifthLigthNock } = nockYT({
                list: {
                    response: []
                },
                query: {
                    path: `${yt.path}/logs`,
                    attributes: ['creation_time']
                }
            });

            const actual = yield YT.getTablesByDirs(yt.clean.dirPaths);

            expect(actual).to.deep.equal([
                {
                    dirPath: 'archive/input',
                    tableName: 'one',
                    creationTime: '2018-07-31T10:00:00.000Z'
                },
                {
                    dirPath: 'archive/input',
                    tableName: 'two',
                    creationTime: '2018-11-31T10:00:00.000Z'
                },
                {
                    dirPath: 'archive/output',
                    tableName: 'three',
                    creationTime: '2020-07-07T10:00:00.000Z'
                },
                {
                    dirPath: 'input',
                    tableName: 'four',
                    creationTime: '2019-07-31T10:00:00.000Z'
                },
                {
                    dirPath: 'input',
                    tableName: 'five',
                    creationTime: '2019-01-01T10:00:00.000Z'
                },
                {
                    dirPath: 'output',
                    tableName: 'six',
                    creationTime: '2018-07-31T10:00:00.000Z'
                }
            ]);

            expect(firstLightNock.isDone()).to.be.true;
            expect(secondLightNock.isDone()).to.be.true;
            expect(thirdLightNock.isDone()).to.be.true;
            expect(fourthLightNock.isDone()).to.be.true;
            expect(fifthLigthNock.isDone()).to.be.true;
        });
    });

    describe(`getExpiredTables`, () => {
        it('should get only old tables', () => {
            const allTables = [
                {
                    dirPath: 'archive/input',
                    tableName: 'one',
                    creationTime: '2018-07-31T10:00:00.000Z'
                },
                {
                    dirPath: 'archive/input',
                    tableName: 'two',
                    creationTime: moment().subtract(3, 'month').toISOString()
                },
                {
                    dirPath: 'logs',
                    tableName: 'three',
                    creationTime: '2019-07-07T10:00:00.000Z'
                },
                {
                    dirPath: 'input',
                    tableName: 'four',
                    creationTime: moment().subtract(5, 'day').toISOString()
                }
            ];

            const actual = YT.getExpiredTables(allTables);

            expect(actual).to.deep.equal([
                {
                    dirPath: 'archive/input',
                    tableName: 'one',
                    creationTime: '2018-07-31T10:00:00.000Z'
                },
                {
                    dirPath: 'logs',
                    tableName: 'three',
                    creationTime: '2019-07-07T10:00:00.000Z'
                }
            ]);
        });

        it('should return `[]` when all tables are new enough', () => {
            const allTables = [
                {
                    dirPath: 'input',
                    tableName: 'one',
                    creationTime: moment().subtract(3, 'month').toISOString()
                },
                {
                    dirPath: 'output',
                    tableName: 'two',
                    creationTime: moment().subtract(5, 'day').toISOString()
                }
            ];

            const actual = YT.getExpiredTables(allTables);

            expect(actual).to.deep.equal([]);
        });

        it('should return `[]` when tables list is empty', () => {
            const actual = YT.getExpiredTables([]);

            expect(actual).to.deep.equal([]);
        });
    });

    describe(`removeTables`, () => {
        it('should remove tables successfully', function *() {
            const { lightNock } = nockYT({
                remove: { response: correctAnswer, times: 2 }
            });

            const oldTables = [
                {
                    dirPath: 'input',
                    tableName: 'one'
                },
                {
                    dirPath: 'output',
                    tableName: 'two'
                }
            ];

            yield YT.removeTables(oldTables);

            expect(log.warn.called).to.be.false;

            expect(lightNock.isDone()).to.be.true;
        });

        it('should do nothing when tables are empty', function *() {
            yield YT.removeTables([]);

            expect(log.warn.called).to.be.false;
        });

        it('should throw 500 when can not remove YT table', function *() {
            const oldTables = [
                {
                    dirPath: 'input',
                    tableName: 'one'
                }
            ];

            const { lightNock } = nockYT({
                remove: { code: 500 }
            });

            const error = yield catchError(YT.removeTables.bind(YT, oldTables));

            expect(error.statusCode).to.equal(500);
            expect(error.message).to.match(/Can not remove table/);
            expect(error.options).to.deep.equal({ internalCode: '500_CRT' });

            expect(log.warn.calledOnce).to.be.true;
            expect(lightNock.isDone()).to.be.true;
        });
    });

    describe('`createTable`', () => {
        it('should create table successfully', function *() {
            const { lightNock } = nockYT({
                create: { response: correctAnswer }
            });

            yield YT.createTable({
                mapNode: 'reports/comdep',
                tableName: 'trials'
            });

            expect(log.warn.called).to.be.false;
            expect(lightNock.isDone()).to.be.true;
        });

        it('should throw 500 when can not create YT table', function *() {
            const { lightNock } = nockYT({
                create: { code: 500 }
            });

            const error = yield catchError(YT.createTable.bind(YT, {
                mapNode: 'reports/comdep',
                tableName: 'trials'
            }));

            expect(error.statusCode).to.equal(500);
            expect(error.message).to.equal('Table not created');
            expect(error.options).to.deep.equal({ internalCode: '500_TNC' });

            expect(log.warn.calledOnce).to.be.true;
            expect(lightNock.isDone()).to.be.true;
        });
    });
});
