import moment from 'moment';

import {isReservationBusy} from '../reservation';

const intervals = [
  {start: Number(moment('2018-01-01T10:00')), end: Number(moment('2018-01-01T10:30'))},
  {start: Number(moment('2018-01-01T11:00')), end: Number(moment('2018-01-01T12:00'))},
  {
    isReservation: true,
    start: Number(moment('2018-01-01T12:00')),
    end: Number(moment('2018-01-01T12:30'))
  }
];

describe('spaceship/utils/reservation', () => {
  test('должен находить пересечения', () => {
    expect(isReservationBusy({start: 0, end: intervals[0].start + 1}, intervals)).toBeTruthy();
    expect(
      isReservationBusy({start: intervals[1].end - 1, end: intervals[1].end}, intervals)
    ).toBeTruthy();
    expect(
      isReservationBusy({start: intervals[0].end, end: intervals[1].end}, intervals)
    ).toBeTruthy();
    expect(
      isReservationBusy({start: intervals[0].start, end: intervals[1].end}, intervals)
    ).toBeTruthy();
  });

  test('должен находить отсутствие пересечений', () => {
    expect(isReservationBusy({start: 0, end: intervals[0].start}, intervals)).toBeFalsy();
    expect(
      isReservationBusy({start: intervals[0].end, end: intervals[1].start}, intervals)
    ).toBeFalsy();
    expect(
      isReservationBusy({start: intervals[1].end, end: intervals[1].end + 1}, intervals)
    ).toBeFalsy();
  });
});
