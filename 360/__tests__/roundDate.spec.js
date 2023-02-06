import moment, {duration} from 'moment';

import roundDate from '../roundDate';

describe('roundDate', () => {
  it('должен округлять дату до ближайшего шага вперёд', () => {
    const date = moment('2000-01-01T00:42:00');
    const dur = duration(15, 'minutes');

    const result = roundDate(date, dur);

    expect(Number(result)).toEqual(Number(moment('2000-01-01T00:45:00')));
  });

  it('должен округлять дату до ближайшего шага назад', () => {
    const date = moment('2000-01-01T00:36:00');
    const dur = duration(15, 'minutes');

    const result = roundDate(date, dur);

    expect(Number(result)).toEqual(Number(moment('2000-01-01T00:30:00')));
  });
});
