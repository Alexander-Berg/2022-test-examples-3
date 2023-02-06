const _ = require('lodash');

const getScenarioData = require('../../../../../src/server/data-adapters/nirvana/scenario/scenario-data');
const { getMultipartPollSection } = require('../../../../../src/server/data-adapters/nirvana/scenario/poll-section');
const {
    CONFIG_INPUT,
    SCENARIO_DATA_OUTPUT,
    SCENARIO_BATCH_CONFIG,
    MULTIPART_POLL_INPUT_SBS_MODE,
    MULTIPART_POLL_OUTPUT_SBS_MODE,
    MULTIPART_POLL_INPUT_A_MODE,
    MULTIPART_POLL_OUTPUT_A_MODE,
} = require('./fixtures');

describe('nirvana/scenario/scenario-data', function() {
    let experiment;

    beforeEach(function() {
        experiment = _.cloneDeep(CONFIG_INPUT).config;
    });

    it('должен возвращать корректные настройки сценария', function() {
        assert.deepEqual(getScenarioData({
            systems: experiment.systems,
            experimentMode: experiment.experimentMode,
            showDuration: experiment.showDuration,
        } ), SCENARIO_DATA_OUTPUT);
    });

    it('должен возвращать корректные настройки сценария, если showDuration не задан', function() {
        experiment.showDuration.mode = 'default';
        const pages = SCENARIO_DATA_OUTPUT['scenario-list'][0]['pages'];

        pages.forEach((page) => {
            delete page['show-duration'];
        });

        assert.deepEqual(getScenarioData({
            systems: experiment.systems,
            experimentMode: experiment.experimentMode,
            showDuration: experiment.showDuration,
        } ), SCENARIO_DATA_OUTPUT);
    });

    it('у каждого элемента коллекции scenario-list должны быть все необходимые ключи в батч режиме', function() {
        const result = getScenarioData(SCENARIO_BATCH_CONFIG);
        const KEYS = ['pages', 'question-groups', 'scenario-id', 'scenario-text'];

        result['scenario-list'].forEach((item) => assert.hasAllKeys(item, KEYS));
    });

    it('должен возвращать корректный scenario-mode для батч режима', function() {
        const MODE = 'batch-sbs';
        const result = getScenarioData(SCENARIO_BATCH_CONFIG);
        assert.equal(result['scenario-mode'], MODE);
    });

    it('должен возвращать корректный тип сценария', function() {
        experiment.systems.prototypes.type = 'figma';
        const expectedValue = 'figma';

        const scenarioList = getScenarioData({
            systems: experiment.systems,
            experimentMode: experiment.experimentMode,
            showDuration: experiment.showDuration,
        })['scenario-list'];

        scenarioList[0].pages.forEach((page) => {
            assert.equal(page['src-type'], expectedValue);
        });
    });

    describe('getMultipartPollSection', function() {
        it('должен использовать общий вопрос, если заданный режим эксперимента - SbS', function() {
            assert.deepEqual(
                getMultipartPollSection('sbs', MULTIPART_POLL_INPUT_SBS_MODE.questionGroups),
                MULTIPART_POLL_OUTPUT_SBS_MODE['question-groups'],
            );
        });

        it('должен использовать настройки questionGroups, если заданный режим эксперимента - A', function() {
            assert.deepEqual(
                getMultipartPollSection('a-mode', MULTIPART_POLL_INPUT_A_MODE.questionGroups),
                MULTIPART_POLL_OUTPUT_A_MODE['question-groups'],
            );
        });
    });
});
