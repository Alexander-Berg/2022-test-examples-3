import {TRAIN_BONUS_CARDS} from 'projects/trains/constants/bonusCards';

import {IBonusCardDTO} from 'server/api/TravelersApi/types/IBonusCardDTO';

import {getPatchedTravelerLoyaltyCards} from 'projects/trains/lib/order/traveler/patchServerResponse/getPatchedTravelerLoyaltyCards';

import {EBonusCardType} from 'server/api/TravelersApi/enums/EBonusCardType';

describe('getPatchedTravelerLoyaltyCards', () => {
    it('Преобразует тип карт лояльности', () => {
        expect(
            getPatchedTravelerLoyaltyCards([
                {
                    type: EBonusCardType.RZD_BONUS,
                    number: '123',
                },
                {
                    type: EBonusCardType.UNIVERSAL_ROAD,
                    number: '321',
                },
            ] as IBonusCardDTO[]),
        ).toEqual([
            {
                type: TRAIN_BONUS_CARDS.BONUS_CARD,
                number: '123',
            },
            {
                type: TRAIN_BONUS_CARDS.ROAD_CARD,
                number: '321',
            },
        ]);
    });

    it('Две карты одного типа - вернем последнюю', () => {
        expect(
            getPatchedTravelerLoyaltyCards([
                {
                    type: EBonusCardType.RZD_BONUS,
                    number: '123',
                },
                {
                    type: EBonusCardType.RZD_BONUS,
                    number: '321',
                },
            ] as IBonusCardDTO[]),
        ).toEqual([
            {
                type: TRAIN_BONUS_CARDS.BONUS_CARD,
                number: '321',
            },
        ]);
    });
});
