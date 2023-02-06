import {duration} from 'moment';

import roundDuration from '../roundDuration';

describe('roundDuration', () => {
  it('должен округлять длительность до ближайшего шага вперёд', () => {
    const dur = duration(42, 'minutes');
    const step = duration(15, 'minutes');

    const result = roundDuration(dur, step);

    expect(result).toEqual(duration(45, 'minutes'));
  });

  it('должен округлять длительность до ближайшего шага назад', () => {
    const dur = duration(36, 'minutes');
    const step = duration(15, 'minutes');

    const result = roundDuration(dur, step);

    expect(result).toEqual(duration(30, 'minutes'));
  });

  it('должен принимать длительность и шаг в миллисекундах', () => {
    const durMs = duration(42, 'minutes').asMilliseconds();
    const stepMs = duration(15, 'minutes').asMilliseconds();

    const result = roundDuration(durMs, stepMs);

    expect(result).toEqual(duration(45, 'minutes'));
  });
});
