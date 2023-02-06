import {TRAIN_COACH_TYPE} from 'projects/trains/constants/coachType';

import trainTariffClassManager from '../../trainTariffClass';

describe('trainTariffClass.formatOptions', () => {
    it('Вернет подходящие для отображения опции', () => {
        expect(
            trainTariffClassManager.formatOptions([
                TRAIN_COACH_TYPE.PLATZKARTE,
                TRAIN_COACH_TYPE.SITTING,
            ]),
        ).toEqual([
            {text: 'плацкарт', value: 'platzkarte'},
            {text: 'сидячие', value: 'sitting'},
        ]);
    });
});
