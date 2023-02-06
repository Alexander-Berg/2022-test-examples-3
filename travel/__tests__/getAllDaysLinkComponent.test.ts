import {TransportType} from '../../transportType';

import {getAllDaysLinkComponent} from '../getAllDaysLinkComponent';

import OrderCalendarDayLink from '../../../components/OrderCalendarDayLink/OrderCalendarDayLink';
import PlaneCalendarDayLink from '../../../components/PlaneCalendarDayLink/PlaneCalendarDayLink';
import ThreadCalendarDayLink from '../../../components/ThreadCalendarDayLink/ThreadCalendarDayLink';

describe('getAllDaysLinkComponent', () => {
    describe('suburban', () => {
        it('with trainPurchaseNumbers', () => {
            expect(
                getAllDaysLinkComponent({
                    transportType: TransportType.suburban,
                    trainPurchaseNumbers: ['072'],
                }),
            ).toBe(OrderCalendarDayLink);
        });

        it('without trainPurchaseNumbers', () => {
            expect(
                getAllDaysLinkComponent({
                    transportType: TransportType.suburban,
                }),
            ).toBe(ThreadCalendarDayLink);
        });
    });

    describe('others', () => {
        it('train', () => {
            expect(
                getAllDaysLinkComponent({
                    transportType: TransportType.train,
                }),
            ).toBe(OrderCalendarDayLink);
        });

        it('plane', () => {
            expect(
                getAllDaysLinkComponent({
                    transportType: TransportType.plane,
                }),
            ).toBe(PlaneCalendarDayLink);
        });

        it('bus', () => {
            expect(
                getAllDaysLinkComponent({
                    transportType: TransportType.bus,
                }),
            ).toBe(ThreadCalendarDayLink);
        });

        it('water', () => {
            expect(
                getAllDaysLinkComponent({
                    transportType: TransportType.water,
                }),
            ).toBe(ThreadCalendarDayLink);
        });
    });
});
