import { getCarsSchedulePopupTitle } from 'features/CarsSchedulePopup/helpers/getCarsSchedulePopupTitle/getCarsSchedulePopupTitle';

import { CarOfferStatusSchema } from 'entities/Car/types/CarOfferStatusSchema';

describe('getCarsSchedulePopupTitle', function () {
    beforeEach(function () {
        jest.useFakeTimers().setSystemTime(new Date('2022-05-05').getTime());
    });

    afterEach(function () {
        jest.useRealTimers();
    });

    it('should works with Draft status', function () {
        expect(
            getCarsSchedulePopupTitle('draft', new Date('2022-01-01'), new Date('2022-01-20')),
        ).toMatchInlineSnapshot(`"Ride finished"`);

        expect(
            getCarsSchedulePopupTitle('draft', new Date('2022-05-01'), new Date('2022-05-10')),
        ).toMatchInlineSnapshot(`"Ride in progress"`);

        expect(
            getCarsSchedulePopupTitle('draft', new Date('2022-05-10'), new Date('2022-05-15')),
        ).toMatchInlineSnapshot(`"Draft booking"`);
    });

    it('should works with Confirmed status', function () {
        expect(
            getCarsSchedulePopupTitle('confirmed', new Date('2022-01-01'), new Date('2022-01-20')),
        ).toMatchInlineSnapshot(`"Ride finished"`);

        expect(
            getCarsSchedulePopupTitle('confirmed', new Date('2022-05-01'), new Date('2022-05-10')),
        ).toMatchInlineSnapshot(`"Ride in progress"`);

        expect(
            getCarsSchedulePopupTitle('confirmed', new Date('2022-05-10'), new Date('2022-05-15')),
        ).toMatchInlineSnapshot(`"Confirmed booking"`);
    });

    it('should works with Paid status', function () {
        expect(getCarsSchedulePopupTitle('paid', new Date('2022-01-01'), new Date('2022-01-20'))).toMatchInlineSnapshot(
            `"Ride finished"`,
        );

        expect(getCarsSchedulePopupTitle('paid', new Date('2022-05-01'), new Date('2022-05-10'))).toMatchInlineSnapshot(
            `"Ride in progress"`,
        );

        expect(getCarsSchedulePopupTitle('paid', new Date('2022-05-10'), new Date('2022-05-15'))).toMatchInlineSnapshot(
            `"Confirmed and paid booking"`,
        );
    });

    it('should works with Service status', function () {
        expect(
            getCarsSchedulePopupTitle('service', new Date('2022-01-01'), new Date('2022-01-20')),
        ).toMatchInlineSnapshot(`"Car in service"`);

        expect(getCarsSchedulePopupTitle('service', new Date('2022-01-01'), undefined)).toMatchInlineSnapshot(
            `"Car in service"`,
        );

        expect(
            getCarsSchedulePopupTitle('service', new Date('2022-05-01'), new Date('2022-05-10')),
        ).toMatchInlineSnapshot(`"Car in service"`);

        expect(
            getCarsSchedulePopupTitle('service', new Date('2022-05-10'), new Date('2022-05-15')),
        ).toMatchInlineSnapshot(`"Car in service"`);

        expect(getCarsSchedulePopupTitle('service')).toMatchInlineSnapshot(`"Car in service"`);
    });

    it('should works with unknown status', function () {
        expect(
            getCarsSchedulePopupTitle(
                'unknown' as CarOfferStatusSchema,
                new Date('2022-01-01'),
                new Date('2022-01-20'),
            ),
        ).toMatchInlineSnapshot(`"Ride finished"`);

        expect(
            getCarsSchedulePopupTitle(
                'unknown' as CarOfferStatusSchema,
                new Date('2022-05-01'),
                new Date('2022-05-10'),
            ),
        ).toMatchInlineSnapshot(`"Ride in progress"`);

        expect(
            getCarsSchedulePopupTitle(
                'unknown' as CarOfferStatusSchema,
                new Date('2022-05-10'),
                new Date('2022-05-15'),
            ),
        ).toMatchInlineSnapshot(`"Draft booking"`);
    });
});
