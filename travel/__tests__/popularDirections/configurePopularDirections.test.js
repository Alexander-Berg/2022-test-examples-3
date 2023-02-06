jest.disableAutomock();

import {configurePopularDirections} from '../../popularDirections';

describe('configurePopularDirections', () => {
    it('Не для ботов должно вернуться максимум 5 записей', () => {
        expect(
            configurePopularDirections(
                {
                    to: {points: Array(10)},
                    from: {points: Array(10)},
                },
                false,
            ).to.points.length,
        ).toBe(5);

        expect(
            configurePopularDirections(
                {
                    to: {points: Array(10)},
                    from: {points: Array(10)},
                },
                false,
            ).from.points.length,
        ).toBe(5);

        expect(
            configurePopularDirections(
                {
                    to: {points: Array(3)},
                    from: {points: Array(3)},
                },
                false,
            ).to.points.length,
        ).toBe(3);

        expect(
            configurePopularDirections(
                {
                    to: {points: Array(3)},
                    from: {points: Array(3)},
                },
                false,
            ).from.points.length,
        ).toBe(3);
    });

    it('Для ботов должно вернуться максимум 10 записей', () => {
        expect(
            configurePopularDirections(
                {
                    to: {points: Array(10)},
                    from: {points: Array(10)},
                },
                true,
            ).to.points.length,
        ).toBe(10);

        expect(
            configurePopularDirections(
                {
                    to: {points: Array(10)},
                    from: {points: Array(10)},
                },
                true,
            ).from.points.length,
        ).toBe(10);

        expect(
            configurePopularDirections(
                {
                    to: {points: Array(15)},
                    from: {points: Array(12)},
                },
                true,
            ).to.points.length,
        ).toBe(10);

        expect(
            configurePopularDirections(
                {
                    to: {points: Array(15)},
                    from: {points: Array(12)},
                },
                true,
            ).from.points.length,
        ).toBe(10);

        expect(
            configurePopularDirections(
                {
                    to: {points: Array(3)},
                    from: {points: Array(3)},
                },
                false,
            ).to.points.length,
        ).toBe(3);

        expect(
            configurePopularDirections(
                {
                    to: {points: Array(3)},
                    from: {points: Array(3)},
                },
                false,
            ).from.points.length,
        ).toBe(3);
    });
});
