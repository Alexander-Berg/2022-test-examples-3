import {getPopularFlights} from 'selectors/avia/utils/getPopularFlights';
import {getGroupVariant} from 'selectors/avia/utils/__mocks__/mocks';
import {EAviaVariantGroupType} from 'selectors/avia/utils/denormalization/variantGroup';

const getFlight = (forwardNumber: string, backwardNumber?: string) =>
    getGroupVariant({
        forwardNumber,
        backwardNumber,
    });

describe('getPopularFlights', () => {
    it('нет данных - вернёт пустой массив', () => {
        expect(
            getPopularFlights([], {
                hasForward: true,
                hasBackward: false,
                forward: {
                    'SU 342': ['SU 342'],
                },
                backward: {},
            }),
        ).toEqual([]);
    });

    it('если нет информации по популярным направлениям "туда" - вернём пустой массив', () => {
        expect(
            getPopularFlights([getFlight('SU 342')], {
                hasForward: false,
                hasBackward: true,
                forward: {},
                backward: {
                    'US 243': ['US 243'],
                },
            }),
        ).toEqual([]);
    });

    describe('поиск в одном направлении: ', () => {
        const POPULAR_FLIGHTS = {
            hasForward: true,
            hasBackward: false,
            forward: {
                'SU 342': ['SU 342'],
            },
            backward: {},
        };

        it('среди вариантов нет популярных - вернём пустой массив', () => {
            expect(
                getPopularFlights(
                    [getFlight('SU 341'), getFlight('US 243')],
                    POPULAR_FLIGHTS,
                ),
            ).toEqual([]);
        });

        it('среди вариантов есть популярные перелёты - вернем массив с популярными перелётами', () => {
            const popularFlight = getFlight('SU 342');
            const unpopularFlight = getFlight('US 343');

            expect(
                getPopularFlights(
                    [popularFlight, unpopularFlight],
                    POPULAR_FLIGHTS,
                ),
            ).toEqual([
                {
                    ...popularFlight,
                    type: EAviaVariantGroupType.popular,
                },
            ]);
        });
    });

    describe('поиск туда-обратно: ', () => {
        const POPULAR_FLIGHTS = {
            hasForward: true,
            hasBackward: true,
            forward: {
                'SU 342': ['SU 342'],
            },
            backward: {
                'US 243': ['US 243'],
            },
        };

        it('есть варианты, которые содержат популярные перелёты только в одну сторону - вернём пустой массив', () => {
            expect(
                getPopularFlights(
                    [
                        getFlight('SU 342', 'SU 344'),
                        getFlight('SU 344', 'US 243'),
                    ],
                    POPULAR_FLIGHTS,
                ),
            ).toEqual([]);
        });

        it('есть варианты, которые содержат популярные перелёты в обе стороны - вернём массив с популярными перелётами', () => {
            const popularFlight = getFlight('SU 342', 'US 243');
            const unpopularFlight = getFlight('SU 342', 'SU 344');

            expect(
                getPopularFlights(
                    [unpopularFlight, popularFlight],
                    POPULAR_FLIGHTS,
                ),
            ).toEqual([
                {
                    ...popularFlight,
                    type: EAviaVariantGroupType.popular,
                },
            ]);
        });
    });
});
