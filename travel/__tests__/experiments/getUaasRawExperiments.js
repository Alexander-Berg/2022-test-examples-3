const {
    getUaasRawExperiments,
} = require('../../experiments/utils/getUaasRawExperiments');

const HANDLER = 'TRAVEL';

const experimentFactory = experiment => {
    const data = {
        HANDLER: experiment.handler,
        CONTEXT: {
            MAIN: {
                source: experiment.source,
            },
        },
    };

    return Buffer.from(JSON.stringify([data])).toString('base64');
};

const experimentsGroupFactory = experimentsGroup => {
    return experimentsGroup.map(experimentFactory).join(',');
};

describe('getUaasRawExperiments', () => {
    it('Находит 1 эксперимент', () => {
        const data = [
            {
                source: {
                    TRAVEL_test_key: 'test_value',
                },
                handler: HANDLER,
            },
        ];

        const expected = {
            TRAVEL_test_key: 'test_value',
        };

        const actual = getUaasRawExperiments(
            experimentsGroupFactory(data),
            HANDLER,
        );

        expect(actual).toEqual(expected);
    });

    it('Находит 1 эксперимент для фронта и эксперименты для бекендов', () => {
        const data = [
            {
                source: {
                    TRAVEL_test_key: 'test_value',
                    back_flags: 'test_back_flag_1=test_value_1',
                },
                handler: HANDLER,
            },
        ];

        const expected = {
            TRAVEL_test_key: 'test_value',
            back_flags: 'test_back_flag_1=test_value_1',
        };

        const actual = getUaasRawExperiments(
            experimentsGroupFactory(data),
            HANDLER,
        );

        expect(actual).toEqual(expected);
    });

    it('Не находит эксперимент при отсутсвии портального хендлера', () => {
        const data = [
            {
                source: {
                    TRAVEL_test_key: 'test_value',
                },
            },
        ];

        const expected = {};

        const actual = getUaasRawExperiments(
            experimentsGroupFactory(data),
            HANDLER,
        );

        expect(actual).toEqual(expected);
    });

    it('Находит 2 эксперимента из 2 групп экспериментов', () => {
        const data = [
            {
                source: {
                    TRAVEL_test_key_1: 'test_value_1',
                },
                handler: HANDLER,
            },
            {
                source: {
                    TRAVEL_test_key_2: 'test_value_2',
                },
                handler: HANDLER,
            },
        ];

        const expected = {
            TRAVEL_test_key_1: 'test_value_1',
            TRAVEL_test_key_2: 'test_value_2',
        };

        const actual = getUaasRawExperiments(
            experimentsGroupFactory(data),
            HANDLER,
        );

        expect(actual).toEqual(expected);
    });

    it('Находит 2 эксперимента из 2 групп экспериментов и конкатенирует эксперименты бекенда в одно поле', () => {
        const data = [
            {
                source: {
                    TRAVEL_test_key_1: 'test_value_1',
                    back_flags: 'test_back_flag_1=test_value_1',
                },
                handler: HANDLER,
            },
            {
                source: {
                    TRAVEL_test_key_2: 'test_value_2',
                    back_flags: 'test_back_flag_2=test_value_2',
                },
                handler: HANDLER,
            },
        ];

        const expected = {
            TRAVEL_test_key_1: 'test_value_1',
            TRAVEL_test_key_2: 'test_value_2',
            back_flags:
                'test_back_flag_1=test_value_1,test_back_flag_2=test_value_2',
        };

        const actual = getUaasRawExperiments(
            experimentsGroupFactory(data),
            HANDLER,
        );

        expect(actual).toEqual(expected);
    });
});
