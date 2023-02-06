jest.disableAutomock();

import composeRenderData from '../composeRenderData';

const directionsSample = [
    {title: 'title1', connected: true},
    {title: 'title2', connected: false},
    {title: 'title3', connected: true},
    {title: 'title4', connected: false},
    {title: 'title5', connected: true},
];

describe('composeRenderData', () => {
    it('возвращает все направления (isSuburbanZone: true, showAll: false)', () => {
        const isSuburbanZone = true;
        const directions = directionsSample;
        const showAll = false;

        expect(
            composeRenderData({
                isSuburbanZone,
                directions,
                showAll,
            }),
        ).toEqual({
            directionsToShow: directions,
            shownAll: true,
            notConnectedDirectionsCount: 0,
        });
    });

    it('возвращает все направления (isSuburbanZone: true, showAll: false, amountToShow: 3)', () => {
        const isSuburbanZone = true;
        const directions = directionsSample;
        const showAll = false;
        const amountToShow = 3;

        expect(
            composeRenderData({
                isSuburbanZone,
                directions,
                showAll,
                amountToShow,
            }),
        ).toEqual({
            directionsToShow: [directions[0], directions[1], directions[2]],
            shownAll: false,
            notConnectedDirectionsCount: 2,
        });
    });

    it('возвращает пустой массив (isSuburbanZone: true, showAll: false)', () => {
        const isSuburbanZone = true;
        const directions = [];
        const showAll = false;

        expect(
            composeRenderData({
                isSuburbanZone,
                directions,
                showAll,
            }),
        ).toEqual({
            directionsToShow: [],
            shownAll: true,
            notConnectedDirectionsCount: 0,
        });
    });

    it(
        'возвращает только `connected` направления ' +
            '(isSuburbanZone: false, showAll: false)',
        () => {
            const isSuburbanZone = false;
            const directions = directionsSample;
            const showAll = false;

            expect(
                composeRenderData({
                    isSuburbanZone,
                    directions,
                    showAll,
                }),
            ).toEqual({
                directionsToShow: [directions[0], directions[2], directions[4]],
                shownAll: false,
                notConnectedDirectionsCount: 2,
            });
        },
    );

    it(
        'возвращает первые 2 `connected` направления ' +
            '(isSuburbanZone: false, showAll: false, amountToShow: 2)',
        () => {
            const isSuburbanZone = false;
            const directions = directionsSample;
            const showAll = false;
            const amountToShow = 2;

            expect(
                composeRenderData({
                    isSuburbanZone,
                    directions,
                    showAll,
                    amountToShow,
                }),
            ).toEqual({
                directionsToShow: [directions[0], directions[2]],
                shownAll: false,
                notConnectedDirectionsCount: 3,
            });
        },
    );

    it('возвращает пустой массив (isSuburbanZone: false, showAll: false)', () => {
        const isSuburbanZone = false;
        const directions = [];
        const showAll = false;

        expect(
            composeRenderData({
                isSuburbanZone,
                directions,
                showAll,
            }),
        ).toEqual({
            directionsToShow: [],
            shownAll: true,
            notConnectedDirectionsCount: 0,
        });
    });

    it(
        'возвращает сначала `connected`, затем `notConnected` направления ' +
            '(isSuburbanZone: false, showAll: true)',
        () => {
            const isSuburbanZone = false;
            const directions = directionsSample;
            const showAll = true;

            expect(
                composeRenderData({
                    isSuburbanZone,
                    directions,
                    showAll,
                }),
            ).toEqual({
                directionsToShow: [
                    directions[0],
                    directions[2],
                    directions[4],
                    directions[1],
                    directions[3],
                ],
                shownAll: true,
                notConnectedDirectionsCount: 0,
            });
        },
    );
});
