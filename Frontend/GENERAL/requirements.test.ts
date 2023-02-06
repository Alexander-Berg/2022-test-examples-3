import { ApiTariff, TariffClass } from '../../../taxi-order-lib/utils/api/zone-info/types';
import { buildSelectedRequirementValues, buildUnsupportedEstimateRequirements, buildUnsupportedMessage } from './helpers';

const API_TARIFF = {
    class: TariffClass.Econom,
    name: 'Эконом',
    supported_requirements: [
        {
            label: 'Требование1',
            name: 'first',
            type: 'boolean'
        },
        {
            label: 'Требование2',
            name: 'second',
            type: 'boolean'
        }
    ]
} as ApiTariff;

describe('Requirements', () => {
    it('Should transform to selected values', () => {
        expect(buildSelectedRequirementValues({
            a: {
                value: true,
                tariff: TariffClass.Econom,
            },
        })).toStrictEqual({
            a: true
        });

        expect(buildSelectedRequirementValues({
            nosmoking: {
                value: true,
                tariff: TariffClass.Business
            },
            child: {
                value: [1, 1],
                tariff: TariffClass.Business,
            }
        })).toStrictEqual({
            nosmoking: true,
            child: [1, 1]
        });
    });

    it('Should return [] if no tariff', () => {
        expect(buildUnsupportedEstimateRequirements(undefined, {
            a: {
                value: true,
                tariff: TariffClass.Business
            }
        })).toStrictEqual([]);
    });

    it('Should return [] if no selected requirements', () => {
        expect(buildUnsupportedEstimateRequirements(API_TARIFF, {})).toStrictEqual([]);
    });

    it('Should find unsupported requirements', () => {
        expect(buildUnsupportedEstimateRequirements(API_TARIFF, {
            first: {
                value: true,
                tariff: TariffClass.Econom,
            },
            unsupported: {
                value: true,
                tariff: TariffClass.Econom
            }
        })).toStrictEqual([{ name: 'unsupported', tariff: TariffClass.Econom }]);
    });

    it('Should ignore false value in unsupported requirement', () => {
        expect(buildUnsupportedEstimateRequirements(API_TARIFF, {
            first: {
                value: true,
                tariff: TariffClass.Econom,
            },
            unsupported: {
                value: false,
                tariff: TariffClass.Econom
            }
        })).toStrictEqual([]);
    });

    it('Should return undefined if have not unsupported requirements', () => {
        expect(buildUnsupportedMessage([], [API_TARIFF])).toEqual(undefined);
    });

    it('Should return some message if have unknown tariff in requirements', () => {
        expect(buildUnsupportedMessage([{
            name: 'a',
            tariff: undefined
        }], [API_TARIFF])).toEqual('Недоступны некоторые пожелания, выбранные в других тарифах');
    });

    it('Should return some message if have more then 1 unsupported tariff requirements', () => {
        expect(buildUnsupportedMessage([
            {
                name: 'a',
                tariff: TariffClass.Econom
            },
            {
                name: 'b',
                tariff: TariffClass.Business
            },
        ],
        [API_TARIFF])).toEqual('Недоступны некоторые пожелания, выбранные в других тарифах');
    });

    it('Should return some message if no tariff', () => {
        expect(buildUnsupportedMessage([
            {
                name: 'a',
                tariff: TariffClass.Business
            },
            {
                name: 'b',
                tariff: TariffClass.Business
            },
        ],
        [API_TARIFF])).toEqual('Недоступны некоторые пожелания, выбранные в других тарифах');
    });

    it('Should return message with econom ', () => {
        expect(buildUnsupportedMessage([
            {
                name: 'a',
                tariff: TariffClass.Econom
            },
            {
                name: 'b',
                tariff: TariffClass.Econom
            },
        ],
        [API_TARIFF])).toEqual('Недоступны некоторые пожелания, выбранные в тарифе Эконом');
    });
});
