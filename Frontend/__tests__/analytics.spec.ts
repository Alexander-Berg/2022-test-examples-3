import { IOffer } from '@yandex-turbo/applications/beru.ru/interfaces';
import { getPageCounterDecl, makeGoal, makeBemGoal, makeGoalParamsFromOffer } from '../analytics';
import * as price from '../price';

describe('getPageCounterDecl', () => {
    it('возвращает декларацию счетчика метрики по умолчанию', () => {
        expect(getPageCounterDecl()).toEqual([
            {
                id: '47628343',
                type: 'Yandex',
                params: { isTurbo: true },
            },
            {
                id: '44910898',
                type: 'Yandex',
                params: { isTurbo: true },
            },
        ]);
    });

    it('переданные дополнительные параметры должны корректно прокидываться', () => {
        expect(getPageCounterDecl({ one: 1, two: 2 })).toEqual(expect.arrayContaining([
            expect.objectContaining({
                params: {
                    isTurbo: true,
                    one: 1,
                    two: 2,
                },
            }),
        ]));
    });
});

describe('makeGoal', () => {
    it('возвращает декларацию гола в турбо терминах', () => {
        expect(makeGoal('test')).toEqual([
            {
                id: '47628343',
                name: 'test',
                params: {},
            },
            {
                id: '44910898',
                name: 'test',
                params: {},
            },
        ]);
    });

    it('переданные дополнительные параметры должны корректно прокидываться', () => {
        expect(makeGoal('test', { one: 1, two: 2 })).toEqual(expect.arrayContaining([
            expect.objectContaining({
                params: {
                    one: 1,
                    two: 2,
                },
            }),
        ]));
    });
});

describe('makeBemGoal', () => {
    it('возвращает декларацию гола в bem терминах', () => {
        expect(makeBemGoal('test')).toEqual({
            '47628343': [{
                id: '47628343',
                name: 'test',
                params: {},
            }],
            '44910898': [{
                id: '44910898',
                name: 'test',
                params: {},
            }],
        });
    });

    it('переданные дополнительные параметры должны корректно прокидываться', () => {
        const goal = makeBemGoal('test', { one: 1, two: 2 });
        expect(goal['47628343'][0]).toEqual(expect.objectContaining({
            params: { one: 1, two: 2 },
        }));
        expect(goal['44910898'][0]).toEqual(expect.objectContaining({
            params: { one: 1, two: 2 },
        }));
    });
});

describe('makeGoalParamsFromOffer', () => {
    it('возвращает параметры цели из сущности офера', () => {
        const getPrice = jest.spyOn(price, 'getPrice');

        getPrice.mockReturnValue({ price: 150, oldPrice: 250, percent: 40 });

        expect(makeGoalParamsFromOffer(<IOffer>{
            feeShow: 'feeTest',
            marketSku: '123',
            wareId: '333',
            marketSkuCreator: 'market',
        })).toEqual({
            skuId: '123',
            offerId: '333',
            marketSkuCreator: 'market',
            showPlaceId: 'feeTest',
            price: 150,
            oldPrice: 250,
        });
    });
});
