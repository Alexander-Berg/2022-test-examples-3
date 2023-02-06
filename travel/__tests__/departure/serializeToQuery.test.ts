import {ETimeOfDay} from 'utilities/dateUtils/types';

import departure from '../../departure';

describe('departure.serializeToQuery', () => {
    it('Для дефолтного значения вернёт пустой массив', () => {
        expect(departure.serializeToQuery(departure.getDefaultValue())).toEqual(
            {
                departure: [],
            },
        );
    });

    it('Вернёт сериализованное значение', () => {
        expect(
            departure.serializeToQuery([ETimeOfDay.MORNING, ETimeOfDay.NIGHT]),
        ).toEqual({
            departure: [ETimeOfDay.MORNING, ETimeOfDay.NIGHT],
        });
    });
});
