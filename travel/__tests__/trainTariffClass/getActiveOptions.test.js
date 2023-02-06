import {TRAIN_TYPE} from '../../../transportType';
import {COMPARTMENT, SUITE, SITTING} from '../../../segments/tariffClasses';

import CurrencyCode from '../../../../interfaces/CurrencyCode';

import trainTariffClass from '../../trainTariffClass';

const transport = {
    code: TRAIN_TYPE,
};
const currency = CurrencyCode.rub;
const sitting = {
    [SITTING]: {
        nationalPrice: {
            currency,
            value: 50,
        },
    },
};
const compartment = {
    [COMPARTMENT]: {
        nationalPrice: {
            currency,
            value: 200,
        },
    },
};
const suite = {
    [SUITE]: {
        nationalPrice: {
            currency,
            value: 10000,
        },
    },
};

describe('trainTariffClass', () => {
    describe('getActiveOptions', () => {
        it('Получение списка доступных опций', () => {
            const segments = [
                {
                    title: 'Moscow - Omsk',
                    transport,
                    tariffs: {
                        classes: {
                            ...sitting,
                        },
                    },
                },
                {
                    title: 'Moscow - Omsk',
                    transport,
                    tariffs: {
                        classes: {
                            ...compartment,
                            ...suite,
                        },
                    },
                },
            ];
            const filtersData = {};
            const newOptionsForSegments = [SITTING, COMPARTMENT, SUITE];
            const result = trainTariffClass.getActiveOptions({
                filtersData,
                segments,
            });

            expect(result.sort()).toEqual(newOptionsForSegments.sort());
        });
    });
});
