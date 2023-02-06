'use strict';

const fs = require('fs-extra');
const path = require('path');
const os = require('os');
const LayoutsParser = require('../../src/server/layouts-parser.js');
const Logger = require('../../src/server/logger');

const logger = new Logger('reqId', 'login');

describe('layouts-parser', () => {
    let lp, sandbox, tmpDir;

    beforeEach(() => {
        sandbox = sinon.createSandbox();
        tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'sbs-'));
        lp = new LayoutsParser(tmpDir, logger);
    });

    afterEach(() => {
        fs.removeSync(tmpDir);
        sandbox.restore();
    });

    describe('filter():', () => {
        it('фильтрует макеты-картинки', () => {
            lp = new LayoutsParser(__dirname + '/fixtures/layouts');

            return lp.filter().then((data) => {
                const dir = data.dir;
                const expectedFilesList = [
                    '0_0.jpg',
                    '0_1.jpg',
                    '0_bad.jpg',
                    '1-0.jpg',
                    '1-1.jpg',
                    '1-bad.jpg',
                    '1_bad.jpg',
                    '2_bad.jpg',
                    '2_0.jpg',
                    '2_1.jpg',
                    '10_bad.jpg',
                ].sort();
                const actualFilesList = fs.readdirSync(dir).map((key) => data.hash.find((h) => h.key === key).origFileName).sort();

                fs.removeSync(dir);

                return assert.deepEqual(actualFilesList, expectedFilesList);
            });
        });

        it('правильно обрабатывает ханипоты для систем с id больше 9', () => {
            lp = new LayoutsParser(__dirname + '/fixtures/layouts');

            return lp.filter().then((data) => {
                const dir = data.dir;
                const actualFilesList = fs.readdirSync(dir).map((key) => data.hash.find((h) => h.key === key).origFileName).sort();

                fs.removeSync(dir);

                return assert.ok(actualFilesList.includes('10_bad.jpg'));
            });
        });

        it('в упрощенном режиме проверят только расширения файлов', () => {
            lp = new LayoutsParser(__dirname + '/fixtures/layouts');

            return lp.filter(true).then((data) => {
                const dir = data.dir;
                const expectedFilesList = [
                    '0_0.jpg',
                    '0_1.jpg',
                    '0_bad.jpg',
                    '1-0.jpg',
                    '1-1.jpg',
                    '1_bad.jpg',
                    '1-bad.jpg',
                    '2_0.jpg',
                    '2_1.jpg',
                    '2_bad.jpg',
                    '10_bad.jpg',
                    'compare_z_0.jpg',
                    'sanjD7_compare_1_0.jpg',
                ].sort();
                const actualFilesList = fs.readdirSync(dir).map((key) => data.hash.find((h) => h.key === key).origFileName).sort();

                fs.removeSync(dir);

                return assert.deepEqual(actualFilesList, expectedFilesList);
            });
        });
    });

    describe('getGrid()', () => {
        it('правильно формирует сетку макетов', () => {
            const fixture = [
                { '0_0.jpg': 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_0_0.jpg' },
                { '0_1.jpg': 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_0_1.jpg' },
                { '1-1.jpg': 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_1_1.jpg' },
                { '1-0.jpg': 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_1_0.jpg' },
                { '2_0.jpg': 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_2_0.jpg' },
                { '2_1.jpg': 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_2_1.jpg' },
                { '0_bad.jpg': 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_jcslj623_0_bad.jpg' },
                { '1_bad.jpg': 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_zc0lj423_1_bad.jpg' },
                { '2_bad.jpg': 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_zc0lj423_2_bad.jpg' },
            ];
            const grid = [
                {
                    screens: [
                        { origUrl: 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_0_0.jpg', fileName: '0_0.jpg' },
                        { origUrl: 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_0_1.jpg', fileName: '0_1.jpg' },
                    ],
                    honeypots: [
                        { origUrl: 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_jcslj623_0_bad.jpg', fileName: '0_bad.jpg' },
                    ],
                },
                {
                    screens: [
                        { origUrl: 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_1_0.jpg', fileName: '1-0.jpg' },
                        { origUrl: 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_1_1.jpg', fileName: '1-1.jpg' },
                    ],
                    honeypots: [
                        { origUrl: 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_zc0lj423_1_bad.jpg', fileName: '1_bad.jpg' },
                    ],
                },
                {
                    screens: [
                        { origUrl: 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_2_0.jpg', fileName: '2_0.jpg' },
                        { origUrl: 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_2_1.jpg', fileName: '2_1.jpg' },
                    ],
                    honeypots: [
                        { origUrl: 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_zc0lj423_2_bad.jpg', fileName: '2_bad.jpg' },
                    ],
                },
            ];

            return assert.deepEqual(lp.getGrid(fixture), grid);
        });

        it('правильно обрабатывает отсутствие нулевой строки', () => {
            const fixture = [
                { '1-1.jpg': 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_1_1.jpg' },
                { '1-0.jpg': 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_1_0.jpg' },
                { '2_0.jpg': 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_2_0.jpg' },
                { '2_1.jpg': 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_2_1.jpg' },
                { '1_bad.jpg': 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_zc0lj423_1_bad.jpg' },
                { '2_bad.jpg': 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_zc0lj423_2_bad.jpg' },
            ];
            const grid = [
                {
                    screens: [
                        { origUrl: 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_1_0.jpg', fileName: '1-0.jpg' },
                        { origUrl: 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_1_1.jpg', fileName: '1-1.jpg' },
                    ],
                    honeypots: [
                        { origUrl: 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_zc0lj423_1_bad.jpg', fileName: '1_bad.jpg' },
                    ],
                },
                {
                    screens: [
                        { origUrl: 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_2_0.jpg', fileName: '2_0.jpg' },
                        { origUrl: 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_2_1.jpg', fileName: '2_1.jpg' },
                    ],
                    honeypots: [
                        { origUrl: 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_zc0lj423_2_bad.jpg', fileName: '2_bad.jpg' },
                    ],
                },
            ];

            return assert.deepEqual(lp.getGrid(fixture), grid);
        });

        it('правильно обрабатывает отсутствие нулевого столбца', () => {
            const fixture = [
                { '0_1.jpg': 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_0_1.jpg' },
                { '1-1.jpg': 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_1_1.jpg' },
                { '2_1.jpg': 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_2_1.jpg' },
                { '0_bad.jpg': 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_jcslj623_0_bad.jpg' },
                { '1_bad.jpg': 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_zc0lj423_1_bad.jpg' },
                { '2_bad.jpg': 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_zc0lj423_2_bad.jpg' },
            ];
            const grid = [
                {
                    screens: [
                        { origUrl: 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_0_1.jpg', fileName: '0_1.jpg' },
                    ],
                    honeypots: [
                        { origUrl: 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_jcslj623_0_bad.jpg', fileName: '0_bad.jpg' },
                    ],
                },
                {
                    screens: [
                        { origUrl: 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_1_1.jpg', fileName: '1-1.jpg' },
                    ],
                    honeypots: [
                        { origUrl: 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_zc0lj423_1_bad.jpg', fileName: '1_bad.jpg' },
                    ],
                },
                {
                    screens: [
                        { origUrl: 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_2_1.jpg', fileName: '2_1.jpg' },
                    ],
                    honeypots: [
                        { origUrl: 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_zc0lj423_2_bad.jpg', fileName: '2_bad.jpg' },
                    ],
                },
            ];

            return assert.deepEqual(lp.getGrid(fixture), grid);
        });

        it('правильно обрабатывает пустые ячейки в матрице (отсутствующие макеты)', () => {
            const fixture = [
                { '0_0.jpg': 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_0_0.jpg' },
                { '0_1.jpg': 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_0_1.jpg' },
                { '1-0.jpg': 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_1_0.jpg' },
                { '1-1.jpg': 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_1_1.jpg' },
                { '2_1.jpg': 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_2_1.jpg' },
                { '0_bad.jpg': 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_jcszhj253_0_bad.jpg' },
            ];
            const grid = [
                {
                    screens: [
                        { origUrl: 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_0_0.jpg', fileName: '0_0.jpg' },
                        { origUrl: 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_0_1.jpg', fileName: '0_1.jpg' },
                    ],
                    honeypots: [
                        { origUrl: 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_jcszhj253_0_bad.jpg', fileName: '0_bad.jpg' },
                    ],
                },
                {
                    screens: [
                        { origUrl: 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_1_0.jpg', fileName: '1-0.jpg' },
                        { origUrl: 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_1_1.jpg', fileName: '1-1.jpg' },
                    ],
                    honeypots: [
                        { fileName: '1_bad' },
                    ],
                },
                {
                    screens: [
                        { fileName: '2_0' },
                        { origUrl: 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_2_1.jpg', fileName: '2_1.jpg' },
                    ],
                    honeypots: [
                        { fileName: '2_bad' },
                    ],
                },
            ];

            return assert.deepEqual(lp.getGrid(fixture), grid);
        });

        it('в упрощенном режиме формирует сетку из одного экрана', () => {
            const fixture = [
                { 'compare_0_0.jpg': 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_0_0.jpg' },
                { 'compare_0_1.jpg': 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_0_1.jpg' },
                { 'compare_1_0.jpg': 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_1_0.jpg' },
                { 'compare_2_1.jpg': 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_2_1.jpg' },
                { 'compare_1_bad.jpg': 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_b_1.jpg' },
            ];
            const grid = [
                {
                    screens: [
                        { origUrl: 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_0_0.jpg', fileName: 'compare_0_0.jpg' },
                        { origUrl: 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_0_1.jpg', fileName: 'compare_0_1.jpg' },
                        { origUrl: 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_1_0.jpg', fileName: 'compare_1_0.jpg' },
                        { origUrl: 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_2_1.jpg', fileName: 'compare_2_1.jpg' },
                    ],
                    honeypots: [
                        { origUrl: 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_b_1.jpg', fileName: 'compare_1_bad.jpg' },
                    ],
                },
            ];

            return assert.deepEqual(lp.getGrid(fixture, true), grid);
        });

        it('не отправляет файлы с "bad" середине имени файла в ханипоты, только заканчивающиеся на _bad', () => {
            const fixture = [
                { 'compare_bad-google.jpg': 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_bad-google.jpg' },
                { 'compare_bad-yandex.jpg': 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_bad-yandex.jpg' },
                { 'compare_good-google.jpg': 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_good-google.jpg' },
                { 'compare_good-yandex.jpg': 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_good-yandex.jpg' },
                { 'compare_rambler_bad.jpg': 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_rambler_bad.jpg' },
                { 'compare_yahoo_bad.jpg': 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_yahoo_bad.jpg' },
            ];
            const grid = [
                {
                    screens: [
                        { origUrl: 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_bad-google.jpg', fileName: 'compare_bad-google.jpg' },
                        { origUrl: 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_bad-yandex.jpg', fileName: 'compare_bad-yandex.jpg' },
                        { origUrl: 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_good-google.jpg', fileName: 'compare_good-google.jpg' },
                        { origUrl: 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_good-yandex.jpg', fileName: 'compare_good-yandex.jpg' },
                    ],
                    honeypots: [
                        { origUrl: 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_rambler_bad.jpg', fileName: 'compare_rambler_bad.jpg' },
                        { origUrl: 'https://s3.mdst.yandex.net/samadhi-layouts/u3ALpv/compare_yahoo_bad.jpg', fileName: 'compare_yahoo_bad.jpg' },
                    ],
                },
            ];

            return assert.deepEqual(lp.getGrid(fixture, true), grid);
        });
    });
});
