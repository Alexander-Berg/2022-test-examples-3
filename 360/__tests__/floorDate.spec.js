import moment, {duration} from 'moment';

import floorDate from '../floorDate';

describe('floorDate', () => {
  it('должен обрезать дату', () => {
    const date = moment('2000-01-01T00:42:00');
    const dur = duration(15, 'minutes');

    const result = floorDate(date, dur);

    expect(Number(result)).toEqual(Number(moment('2000-01-01T00:30:00')));
  });
});
