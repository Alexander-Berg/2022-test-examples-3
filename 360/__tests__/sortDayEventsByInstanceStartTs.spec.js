import {Map, Seq} from 'immutable';

import sortDayEvents from '../sortDayEvents';

describe('sortDayEventsByInstanceStartTs', () => {
  test('должен вернуть отсортированный ArraySeq', () => {
    const events = new Map([
      ['uuid1', {id: 1, instanceStartTs: 2}],
      ['uuid2', {id: 2, instanceStartTs: 1}]
    ]);

    const result = sortDayEvents(events);

    expect(result).toBeInstanceOf(Seq);
    expect(result.get(0).instanceStartTs).toBe(1);
    expect(result.get(1).instanceStartTs).toBe(2);
  });
});
