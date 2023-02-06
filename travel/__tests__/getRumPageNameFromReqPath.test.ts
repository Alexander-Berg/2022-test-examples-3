import {ERumPage} from 'components/Rum/types';

import getRumPageNameFromReqPath from 'components/Rum/utilities/getRumPageNameFromReqPath';

describe('getRumPageNameFromReqPath', () => {
    it('Главная страница', () => {
        expect(getRumPageNameFromReqPath('/', {})).toBe(ERumPage.AVIA_MAIN);
    });

    describe('Авиа', () => {
        it('Главная страница', () => {
            expect(getRumPageNameFromReqPath('/avia', {})).toBe(
                ERumPage.AVIA_MAIN,
            );

            expect(getRumPageNameFromReqPath('/avia/', {})).toBe(
                ERumPage.AVIA_MAIN,
            );
        });

        it('Поиск', () => {
            expect(getRumPageNameFromReqPath('/avia/search/result', {})).toBe(
                ERumPage.AVIA_DATE_SEARCH,
            );

            expect(getRumPageNameFromReqPath('/avia/search/result/', {})).toBe(
                ERumPage.AVIA_DATE_SEARCH,
            );
        });

        it('Направление', () => {
            expect(getRumPageNameFromReqPath('/avia/routes/msk--spb', {})).toBe(
                ERumPage.AVIA_ROUTE,
            );

            expect(
                getRumPageNameFromReqPath('/avia/routes/msk--spb/', {}),
            ).toBe(ERumPage.AVIA_ROUTE);
        });

        it('Рейс', () => {
            expect(getRumPageNameFromReqPath('/avia/flights/SU-1404', {})).toBe(
                ERumPage.AVIA_FLIGHT,
            );

            expect(
                getRumPageNameFromReqPath('/avia/flights/SU-1404/', {}),
            ).toBe(ERumPage.AVIA_FLIGHT);
        });
    });

    describe('Поезда', () => {
        it('Главная страница', () => {
            expect(getRumPageNameFromReqPath('/trains', {})).toBe(
                ERumPage.TRAINS_MAIN,
            );

            expect(getRumPageNameFromReqPath('/trains/', {})).toBe(
                ERumPage.TRAINS_MAIN,
            );
        });

        it('Поиск на дату', () => {
            expect(
                getRumPageNameFromReqPath('/trains/msk--spb', {
                    when: 'tomorrow',
                }),
            ).toBe(ERumPage.TRAINS_DATE_SEARCH);

            expect(
                getRumPageNameFromReqPath('/trains/msk--spb/', {
                    when: 'tomorrow',
                }),
            ).toBe(ERumPage.TRAINS_DATE_SEARCH);
        });

        it('Направление', () => {
            expect(getRumPageNameFromReqPath('/trains/msk--spb', {})).toBe(
                ERumPage.TRAINS_DIRECTION_SEARCH,
            );

            expect(getRumPageNameFromReqPath('/trains/msk--spb/', {})).toBe(
                ERumPage.TRAINS_DIRECTION_SEARCH,
            );
        });

        it('Популярные направления', () => {
            expect(
                getRumPageNameFromReqPath('/trains/popular-routes', {}),
            ).toBe(ERumPage.TRAINS_POPULAR_ROUTES);

            expect(
                getRumPageNameFromReqPath('/trains/popular-routes/', {}),
            ).toBe(ERumPage.TRAINS_POPULAR_ROUTES);
        });
    });

    describe('Отели', () => {
        it('Главная страница', () => {
            expect(getRumPageNameFromReqPath('/hotels', {})).toBe(
                ERumPage.HOTELS_MAIN,
            );

            expect(getRumPageNameFromReqPath('/hotels/', {})).toBe(
                ERumPage.HOTELS_MAIN,
            );
        });

        it('Поиск на дату', () => {
            expect(getRumPageNameFromReqPath('/hotels/search', {})).toBe(
                ERumPage.HOTELS_DATE_SEARCH,
            );

            expect(getRumPageNameFromReqPath('/hotels/search/', {})).toBe(
                ERumPage.HOTELS_DATE_SEARCH,
            );
        });

        it('Отель', () => {
            expect(getRumPageNameFromReqPath('/hotels/hotel', {})).toBe(
                ERumPage.HOTELS_HOTEL,
            );

            expect(getRumPageNameFromReqPath('/hotels/hotel/', {})).toBe(
                ERumPage.HOTELS_HOTEL,
            );

            expect(
                getRumPageNameFromReqPath('/hotels/moscow/radisson', {}),
            ).toBe(ERumPage.HOTELS_HOTEL);

            expect(
                getRumPageNameFromReqPath('/hotels/moscow/radisson/', {}),
            ).toBe(ERumPage.HOTELS_HOTEL);
        });

        it('Регион', () => {
            expect(getRumPageNameFromReqPath('/hotels/moscow', {})).toBe(
                ERumPage.HOTELS_GEO_REGION,
            );

            expect(getRumPageNameFromReqPath('/hotels/moscow/', {})).toBe(
                ERumPage.HOTELS_GEO_REGION,
            );
        });
    });

    describe('Автобусы', () => {
        it('Главная страница', () => {
            expect(getRumPageNameFromReqPath('/buses', {})).toBe(
                ERumPage.BUSES_MAIN,
            );

            expect(getRumPageNameFromReqPath('/buses/', {})).toBe(
                ERumPage.BUSES_MAIN,
            );
        });

        it('Поиск на дату', () => {
            expect(
                getRumPageNameFromReqPath('/buses/msk--spb', {
                    date: 'tomorrow',
                }),
            ).toBe(ERumPage.BUSES_DATE_SEARCH);

            expect(
                getRumPageNameFromReqPath('/buses/msk--spb/', {
                    date: 'tomorrow',
                }),
            ).toBe(ERumPage.BUSES_DATE_SEARCH);
        });

        it('Направление', () => {
            expect(getRumPageNameFromReqPath('/buses/msk--spb', {})).toBe(
                ERumPage.BUSES_DIRECTION_SEARCH,
            );

            expect(getRumPageNameFromReqPath('/buses/msk--spb/', {})).toBe(
                ERumPage.BUSES_DIRECTION_SEARCH,
            );
        });

        it('Город', () => {
            expect(getRumPageNameFromReqPath('/buses/msk', {})).toBe(
                ERumPage.BUSES_CITY,
            );

            expect(getRumPageNameFromReqPath('/buses/msk/', {})).toBe(
                ERumPage.BUSES_CITY,
            );
        });
    });
});
