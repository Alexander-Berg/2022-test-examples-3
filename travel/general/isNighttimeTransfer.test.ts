import {isNighttimeTransfer} from './isNighttimeTransfer';

describe('isNighttimeTransfer', () => {
    // Cases was taken from here
    // https://i.stack.imgur.com/0c6q0.png

    test('night time range IS BEFORE', () => {
        expect(
            isNighttimeTransfer({
                arrivalDate: '2019-06-28T21:35:00',
                departureDate: '2019-06-28T22:35:00',
            }),
        ).toBe(false);
    });

    test('night time range IS START TOUCHING', () => {
        expect(
            isNighttimeTransfer({
                arrivalDate: '2019-06-28T21:35:00',
                departureDate: '2019-06-29T00:00:00',
            }),
        ).toBe(true);
    });

    test('night time range IS START INSIDE', () => {
        expect(
            isNighttimeTransfer({
                arrivalDate: '2019-06-28T21:35:00',
                departureDate: '2019-06-29T01:00:00',
            }),
        ).toBe(true);
    });

    test('night time range IS INSIDE START TOUCHING', () => {
        expect(
            isNighttimeTransfer({
                arrivalDate: '2019-06-29T00:00:00',
                departureDate: '2019-06-29T07:00:00',
            }),
        ).toBe(true);
    });

    test('night time range IS ENCLOSING START TOUCHING', () => {
        expect(
            isNighttimeTransfer({
                arrivalDate: '2019-06-29T00:00:00',
                departureDate: '2019-06-29T04:00:00',
            }),
        ).toBe(true);
    });

    test('night time range IS ENCLOSING', () => {
        expect(
            isNighttimeTransfer({
                arrivalDate: '2019-06-29T02:00:00',
                departureDate: '2019-06-29T04:00:00',
            }),
        ).toBe(true);
    });

    test('night time range IS ENCLOSING END TOUCHING', () => {
        expect(
            isNighttimeTransfer({
                arrivalDate: '2019-06-29T02:00:00',
                departureDate: '2019-06-29T06:00:00',
            }),
        ).toBe(true);
    });

    test('night time range IS EXACT MATCH', () => {
        expect(
            isNighttimeTransfer({
                arrivalDate: '2019-06-29T00:00:00',
                departureDate: '2019-06-29T06:00:00',
            }),
        ).toBe(true);
    });

    test('night time range IS INSIDE', () => {
        expect(
            isNighttimeTransfer({
                arrivalDate: '2019-06-28T23:00:00',
                departureDate: '2019-06-29T07:00:00',
            }),
        ).toBe(true);
    });

    test('night time range IS INSIDE END TOUCHING', () => {
        expect(
            isNighttimeTransfer({
                arrivalDate: '2019-06-28T23:00:00',
                departureDate: '2019-06-29T06:00:00',
            }),
        ).toBe(true);
    });

    test('night time range IS END INSIDE', () => {
        expect(
            isNighttimeTransfer({
                arrivalDate: '2019-06-29T04:00:00',
                departureDate: '2019-06-29T08:00:00',
            }),
        ).toBe(true);
    });

    test('night time range IS END TOUCHING', () => {
        expect(
            isNighttimeTransfer({
                arrivalDate: '2019-06-29T06:00:00',
                departureDate: '2019-06-29T08:00:00',
            }),
        ).toBe(false);
    });

    test('night time range IS BEFORE', () => {
        expect(
            isNighttimeTransfer({
                arrivalDate: '2019-06-29T07:00:00',
                departureDate: '2019-06-29T08:00:00',
            }),
        ).toBe(false);
    });

    test('with day change', () => {
        expect(
            isNighttimeTransfer({
                arrivalDate: '2019-06-29T23:00:00',
                departureDate: '2019-06-30T08:00:00',
            }),
        ).toBe(true);
    });
});
