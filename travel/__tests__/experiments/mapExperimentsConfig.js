const {
    mapExperimentsConfig,
} = require('../../experiments/utils/mapExperimentsConfig');

describe('mapExperimentsConfig', () => {
    it('Находит эксперимент', () => {
        const experimentConfig = {
            testExperimentId: 'UAAS_EXPERIMENT_ID:experiment_value',
        };

        const parsedExperiment = {
            UAAS_EXPERIMENT_ID: 'experiment_value',
        };

        const expected = {
            testExperimentId: true,
        };

        const actual = mapExperimentsConfig(experimentConfig, parsedExperiment);

        expect(actual).toEqual(expected);
    });

    it('Не находит эксперимент', () => {
        const experimentConfig = {
            testExperimentId: 'UAAS_EXPERIMENT_ID:experiment_value',
        };

        const parsedExperiment = {
            UAAS_EXPERIMENT_ID: 'another_experiment_value',
        };

        const expected = {};

        const actual = mapExperimentsConfig(experimentConfig, parsedExperiment);

        expect(actual).toEqual(expected);
    });
});
