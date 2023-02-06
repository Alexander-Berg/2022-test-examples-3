const _ = require('lodash');

const converter = require('../../../../../src/server/data-adapters/nirvana/scenario');
const { CONFIG_INPUT, CONFIG_OUTPUT, SCENARIO_BATCH_CONFIG } = require('./fixtures');

describe('nirvana/scenario', function() {
    let converterInput;

    beforeEach(function() {
        converterInput = _.cloneDeep(CONFIG_INPUT);
    });

    it('должен правильно конвертировать настройки эксепримента в конфиг для Нирваны', function() {
        assert.deepEqual(converter(converterInput), CONFIG_OUTPUT);
    });

    it('должен добавлять корректный assessment-device-type', function() {
        const TOUCH = 'touch';

        converterInput.config.assessmentDeviceType = TOUCH;

        const convertedConfig = converter(converterInput);
        const result = convertedConfig.main['assessment-device-type'];

        assert.equal(result, TOUCH);
    });

    it('должен добавлять корректный pool-id шаблона Толоки для touch экспериментов', function() {
        const PRODUCTION_POOL_ID = 4965066;

        converterInput.config.assessmentDeviceType = 'touch';

        const convertedConfig = converter(converterInput);
        const result = convertedConfig['pool-clone-info'].template.production['pool-id'];

        assert.equal(result, PRODUCTION_POOL_ID);
    });

    it('должен добавлять корректный pool-id шаблона Толоки для экспериментов singleFrameWithVideoStreamSync (CVLab)', () => {
        const CVLAB_POOL_ID = 11414769;

        converterInput.config.isSingleFrameWithVideoStreamSync = true;

        const convertedConfig = converter(converterInput);
        const result = convertedConfig['pool-clone-info'].template.production['pool-id'];

        assert.equal(result, CVLAB_POOL_ID);
    });

    describe('должен правильно определять перекрытие', function() {
        it('должен не изменять, если явно задано пользователем', function() {
            const OVERLAP = 20;

            converterInput.config.overlap = {
                mode: 'edit',
                value: '20',
            };

            assert.equal(converter(converterInput).toloka.overlap, OVERLAP);
        });

        it('должен правильно рассчитывать перекрытие пол кол-ву систем (1 → 150), если явно не задано пользователем', function() {
            const OVERLAP = 150;

            converterInput.config.overlap = {
                mode: 'default',
                value: '',
            };

            converterInput.config.systems.systems.items = [converterInput.config.systems.systems.items[0]];

            assert.equal(converter(converterInput).toloka.overlap, OVERLAP);
        });

        it('должен правильно рассчитывать перекрытие пол кол-ву систем (2 → 85), если явно не задано пользователем', function() {
            const OVERLAP = 85;

            converterInput.config.overlap = {
                mode: 'default',
                value: '',
            };

            assert.equal(converter(converterInput).toloka.overlap, OVERLAP);
        });

        it('должен правильно рассчитывать перекрытие пол кол-ву систем (3 → 85), если явно не задано пользователем', function() {
            const OVERLAP = 85;

            converterInput.config.overlap = {
                mode: 'default',
                value: '',
            };
            converterInput.config.systems.systems.items.push({
                title: 'system3',
            });

            assert.equal(converter(converterInput).toloka.overlap, OVERLAP);
        });

        it('должен правильно рассчитывать перекрытие пол кол-ву систем ((4 >= < 10) → 10), если явно не задано пользователем', function() {
            const OVERLAP = 10;

            converterInput.config.overlap = {
                mode: 'default',
                value: '',
            };
            converterInput.config.systems.systems.items.push({
                title: 'system3',
            });
            converterInput.config.systems.systems.items.push({
                title: 'system4',
            });

            assert.equal(converter(converterInput).toloka.overlap, OVERLAP);
        });

        it('должен правильно рассчитывать перекрытие пол кол-ву систем (>=10 → 1), если явно не задано пользователем', function() {
            const OVERLAP = 1;

            converterInput.config.overlap = {
                mode: 'default',
                value: '',
            };

            for (let i = 0; i < 11; i++) {
                converterInput.config.systems.systems.items.push({
                    title: `system${i}`,
                });
            }

            assert.equal(converter(converterInput).toloka.overlap, OVERLAP);
        });

        it('должен правильно дать перекрытие, если эксперимент в АБ-режиме и пользователь не задал его', function() {
            const AB_MODE_DEFAULT_VALUE = 110;

            converterInput.config.experimentMode = 'ab-mode';
            converterInput.config.overlap = {
                mode: 'default',
                value: '',
            };
            converterInput.config.variants.items.push({
                title: 'system',
                file: 'https://samadhi-layouts.s3.yandex.net/sbs-OKWFZd/index.html',
            });

            assert.equal(converter(converterInput).toloka.overlap, AB_MODE_DEFAULT_VALUE);
        });
    });

    describe('должен правильно определять кол-во заданий, которое может сделать толокер до приостановки в пуле', function() {
        it('должен правильно рассчитывать кол-во заданий по кол-ву систем (<=3 → 1)', function() {
            const ASSIGNMENTS_COUNT = 1;

            converterInput.config.systems.systems.items = [converterInput.config.systems.systems.items[0]];

            assert.equal(converter(converterInput).toloka['assignments-accepted-count'], ASSIGNMENTS_COUNT);
        });

        it('должен правильно рассчитывать кол-во заданий по кол-ву систем (>=4 <=6 → 2)', function() {
            const ASSIGNMENTS_COUNT = 2;

            converterInput.config.systems.systems.items.push({
                title: 'system3',
                file: 'https://samadhi-layouts.s3.yandex.net/sbs-OKWFZd/index.html',
            });

            converterInput.config.systems.systems.items.push({
                title: 'system4',
                file: 'https://samadhi-layouts.s3.yandex.net/sbs-OKWFZd/index.html',
            });

            assert.equal(converter(converterInput).toloka['assignments-accepted-count'], ASSIGNMENTS_COUNT);
        });

        it('должен правильно рассчитывать кол-во заданий по кол-ву систем (>=7 <=10 → 4)', function() {
            const ASSIGNMENTS_COUNT = 4;

            for (let i = 0; i < 6; i++) {
                converterInput.config.systems.systems.items.push({
                    title: `system${i}`,
                    file: 'https://samadhi-layouts.s3.yandex.net/sbs-OKWFZd/index.html',
                });
            }

            assert.equal(converter(converterInput).toloka['assignments-accepted-count'], ASSIGNMENTS_COUNT);
        });

        it('должен правильно рассчитывать кол-во заданий по кол-ву систем (>10 → 8)', function() {
            const ASSIGNMENTS_COUNT = 8;

            for (let i = 0; i < 9; i++) {
                converterInput.config.systems.systems.items.push({
                    title: `system${i}`,
                    file: 'https://samadhi-layouts.s3.yandex.net/sbs-OKWFZd/index.html',
                });
            }

            assert.equal(converter(converterInput).toloka['assignments-accepted-count'], ASSIGNMENTS_COUNT);
        });

        it('должен возвращать 1, если эксперимент в ab-режиме', function() {
            const AB_MODE_ASSIGNMENTS_COUNT = 1;

            converterInput.config.experimentMode = 'ab-mode';

            for (let i = 0; i < 4; i++) {
                converterInput.config.systems.systems.items.push({
                    title: `system${i}`,
                    file: 'https://samadhi-layouts.s3.yandex.net/sbs-OKWFZd/index.html',
                });
            }

            assert.equal(converter(converterInput).toloka['assignments-accepted-count'], AB_MODE_ASSIGNMENTS_COUNT);
        });
    });

    it('должен выставлять ID продакшн-графа Опросов если задан', () => {
        converterInput.pollProdWorkflowId = '907d74aa-9512-4a58-8270-dc085b8fc76e';
        assert.equal(converter(converterInput)['poll-params']['prod-workflow-id'], converterInput.pollProdWorkflowId);
    });

    describe('должен выставлять корректный media-position-mode', () => {
        it('должен выставлять from-start, если не задан флаг isSingleFrameWithVideoStreamSync в параметрах эксперимента', () => {
            assert.equal(converter(converterInput)['poll-params']['media-position-mode'], 'from-start');
        });

        it('должен выставлять single-frame-with-video-stream-sync, если задан флаг isSingleFrameWithVideoStreamSync в параметрах эксперимента', () => {
            converterInput.config.isSingleFrameWithVideoStreamSync = true;
            assert.equal(converter(converterInput)['poll-params']['media-position-mode'], 'single-frame-with-video-stream-sync');
        });
    });

    it('должен выставлять корректное количество сценариев в exp["queries-num"] для батч-сценарных SbS', () => {
        converterInput.config = SCENARIO_BATCH_CONFIG;

        assert.equal(converter(converterInput).exp['queries-num'], SCENARIO_BATCH_CONFIG.variants.items.length);
    });
});
