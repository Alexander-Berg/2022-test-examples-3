import {reachGoal} from '../../yaMetrika';
import {createOrderGoalHandler} from '../createOrderGoalHandler';

jest.mock('../../yaMetrika', () => ({
    ...require.requireActual('../../yaMetrika'),
    reachGoal: jest.fn(),
}));

import {
    PLANE_TYPE,
    BUS_TYPE,
    TRAIN_TYPE,
    SUBURBAN_TYPE,
} from '../../transportType';
import {YBUS} from '../../segments/tariffSources';

describe('createOrderGoalHandler', () => {
    it('Для самолета должны быть достигнуты 2 цели', () => {
        const handler = createOrderGoalHandler(PLANE_TYPE);

        handler();

        expect(reachGoal.mock.calls.length).toBe(2);
        expect(reachGoal.mock.calls[0]).toEqual(['order_plane_click']);
        expect(reachGoal.mock.calls[1]).toEqual(['redirect_to_partner']);
    });

    it('Для автобуса с источником YBUS должны быть достигнуты 3 цели', () => {
        const handler = createOrderGoalHandler(BUS_TYPE, YBUS);

        handler();

        expect(reachGoal.mock.calls.length).toBe(3);
        expect(reachGoal.mock.calls[0]).toEqual(['order_bus_click']);
        expect(reachGoal.mock.calls[1]).toEqual(['order_ybus_click']);
        expect(reachGoal.mock.calls[2]).toEqual(['redirect_to_partner']);
    });

    it('Для автобуса с источником отличным от YBUS должна быть достигнута 1 цель', () => {
        const handler = createOrderGoalHandler(BUS_TYPE);

        handler();

        expect(reachGoal.mock.calls.length).toBe(1);
        expect(reachGoal.mock.calls[0]).toEqual(['order_bus_click']);
    });

    it('Для поезда для которого доступна внутренняя продажа должна быть достигнута 1 цель', () => {
        const handler = createOrderGoalHandler(TRAIN_TYPE, null, false);

        handler();

        expect(reachGoal.mock.calls.length).toBe(1);
        expect(reachGoal.mock.calls[0]).toEqual(['order_train_click']);
    });

    it('Для поезда для которого не доступна внутренняя продажа должны быть достигнуты 2 цели', () => {
        const handler = createOrderGoalHandler(TRAIN_TYPE, null, true);

        handler();

        expect(reachGoal.mock.calls.length).toBe(2);
        expect(reachGoal.mock.calls[0]).toEqual(['order_train_click']);
        expect(reachGoal.mock.calls[1]).toEqual(['order_ufs_link_click']);
    });

    it('Для типов отличных от bus, train и plane должна быть достигнута 1 цель', () => {
        const handler = createOrderGoalHandler(SUBURBAN_TYPE);

        handler();

        expect(reachGoal.mock.calls.length).toBe(1);
        expect(reachGoal.mock.calls[0]).toEqual(['order_suburban_click']);
    });

    it(
        'Для поезда с бейджиком "самый дешевый" должна быть достигнута ' +
            'цель `клик по поезду с бейджиком "самый дешевый"`',
        () => {
            const handler = createOrderGoalHandler(null, null, null, {
                cheapest: true,
            });

            handler();

            expect(reachGoal.mock.calls.length).toEqual(2);
            expect(reachGoal.mock.calls[1]).toEqual([
                'click_on_cheapest_badges_train',
            ]);
        },
    );

    it(
        'Для поезда с бейджиком "дешевый" должна быть достигнута ' +
            'цель `клик по поезду с бейджиком "дешевый"`',
        () => {
            const handler = createOrderGoalHandler(null, null, null, {
                cheap: true,
            });

            handler();

            expect(reachGoal.mock.calls.length).toEqual(2);
            expect(reachGoal.mock.calls[1]).toEqual([
                'click_on_cheap_badges_train',
            ]);
        },
    );

    it('Для сегментов пересадок будет достигнуто две цели', () => {
        const handler = createOrderGoalHandler(
            TRAIN_TYPE,
            undefined,
            undefined,
            undefined,
            undefined,
            undefined,
            true,
        );

        handler();

        expect(reachGoal.mock.calls.length).toEqual(2);
        expect(reachGoal.mock.calls[1]).toEqual([
            'transfersWithPrices_clickBuyButtonTransferSegment',
        ]);
    });

    it('Для интерлайнов будет достигнуто две цели', () => {
        const handler = createOrderGoalHandler(
            TRAIN_TYPE,
            undefined,
            undefined,
            undefined,
            undefined,
            true,
        );

        handler();

        expect(reachGoal.mock.calls.length).toEqual(2);
        expect(reachGoal.mock.calls[1]).toEqual([
            'transfersWithPrices_clickBuyButtonInterline',
        ]);
    });
});
