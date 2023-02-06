import moment from 'moment';

import isArchivedOrder from 'projects/account/pages/Order/utilities/ticketStatuses/isArchivedOrder';

const departure = '2019-07-05T11:18:00Z';
const momentDeparture = moment(departure);

describe('isArchivedOrder', () => {
    it('Поезд ещё не отправился со станции посадки - вернёт false', () => {
        expect(
            isArchivedOrder(
                departure,
                Number(momentDeparture.clone().subtract(1, 'days')),
            ),
        ).toBe(false);
    });

    it('Со времени отправления поезда прошло < 10 дней - вернёт false', () => {
        expect(
            isArchivedOrder(
                departure,
                Number(momentDeparture.clone().add(1, 'days')),
            ),
        ).toBe(false);
    });

    it('Со времени отправления поезда прошло >= 10 дней - вернёт true', () => {
        expect(
            isArchivedOrder(
                departure,
                Number(momentDeparture.clone().add(10, 'days')),
            ),
        ).toBe(true);
    });
});
