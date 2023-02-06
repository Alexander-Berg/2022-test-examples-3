import {TRAIN_TYPE, BUS_TYPE} from '../../transportType';

import {TrainPartner} from '../../order/trainPartners';

import addTrainPartnersToSegment from '../addTrainPartnersToSegment';

describe('addTrainPartnersToSegment', () => {
    it('Должен добавить партнеров по продаже поездов к поездатому сегменту', () => {
        const segment = {
            transport: {code: TRAIN_TYPE},
        };
        const trainPartners = [TrainPartner.im];

        const result = addTrainPartnersToSegment({segment, trainPartners});

        expect(result.trainPartners).toBe(trainPartners);
        expect(result).toBe(segment);
    });

    it('Не должен добавлять партнеров по продаже поездов к НЕ поездатым сегментам', () => {
        const segment = {
            transport: {code: BUS_TYPE},
        };
        const trainPartners = [TrainPartner.im];

        const result = addTrainPartnersToSegment({segment, trainPartners});

        expect(typeof result.trainPartners).toBe('undefined');
        expect(result).toBe(segment);
    });

    it('Если тип транспорта сегмента не определен, то вернет сегмент обратно', () => {
        const segment = {};

        const result = addTrainPartnersToSegment({segment});

        expect(result).toBe(segment);
    });
});
