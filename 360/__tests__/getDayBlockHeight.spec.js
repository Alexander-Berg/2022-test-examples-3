import {Map, List} from 'immutable';

import {getDayBlockHeight, getDayBlockHeightTouch, memoizeResolvers} from '../getDayBlockHeight';

describe('getDayBlockHeight', () => {
  test('должен возвращать 0, если событий нет', () => {
    const daySummary = {
      common: new Map(),
      allDay: new Map()
    };

    expect(getDayBlockHeight(daySummary)).toBe(0);
  });
  test('должен возвращать 95 при одном событии, отсутствии переговорок и участников, день не сегодня', () => {
    const daySummary = {
      common: new Map({uuid: {resources: new Map(), attendees: new Map()}}),
      allDay: new Map()
    };

    expect(getDayBlockHeight(daySummary)).toBe(95);
  });
  test('должен возвращать 110 при одном событии, отсутствии переговорок и участников, день сегодня', () => {
    const daySummary = {
      common: new Map({uuid: {resources: new Map(), attendees: new Map()}}),
      allDay: new Map(),
      isToday: true
    };

    expect(getDayBlockHeight(daySummary)).toBe(110);
  });
  test('должен возвращать 110 при одном событии с участниками и отсутствии переговорок', () => {
    const daySummary = {
      common: new Map({
        uuid: {resources: new Map(), attendees: new Map({attendee: {}, attendee2: {}})}
      }),
      allDay: new Map()
    };

    expect(getDayBlockHeight(daySummary)).toBe(110);
  });
  test('должен возвращать 110 при одном событии с одной переговоркой', () => {
    const daySummary = {
      common: new Map({uuid: {resources: new Map({resource: {}}), attendees: new Map()}}),
      allDay: new Map()
    };

    expect(getDayBlockHeight(daySummary)).toBe(110);
  });
  test('должен возвращать 125 при одном событии с двумя переговорками', () => {
    const daySummary = {
      common: new Map({
        uuid: {resources: new Map({resource: {}, resource2: {}}), attendees: new Map()}
      }),
      allDay: new Map(),
      isToday: true
    };

    expect(getDayBlockHeight(daySummary)).toBe(125);
  });
  test('должен возвращать 130 при двух событиях, отсутствии переговорок и участников', () => {
    const daySummary = {
      common: new Map({
        uuid: {resources: new Map(), attendees: new Map()},
        uuid2: {resources: new Map(), attendees: new Map()}
      }),
      allDay: new Map(),
      isToday: true
    };

    expect(getDayBlockHeight(daySummary)).toBe(130);
  });
});
describe('getDayBlockHeight memoizeResolver', () => {
  test('должен возвращать одинаковый ключ для разных мап одного размера', () => {
    const day1 = {
      common: new Map({
        a: {resources: new List(), attendees: new List()},
        b: {resources: new List(), attendees: new List()}
      }),
      allDay: new Map({
        a: {resources: new List(), attendees: new List()},
        b: {resources: new List(), attendees: new List()}
      })
    };

    const day2 = {
      common: new Map({
        a: {resources: new List(), attendees: new List()},
        b: {resources: new List(), attendees: new List()}
      }),
      allDay: new Map({
        a: {resources: new List(), attendees: new List()},
        b: {resources: new List(), attendees: new List()}
      })
    };

    const key1 = memoizeResolvers.desktop(day1);
    const key2 = memoizeResolvers.desktop(day2);

    expect(key1).toEqual(key2);
  });

  test('не должен возвращать одинаковый ключ для мап другого размера', () => {
    const day1 = {
      common: new Map({
        a: {resources: new List(), attendees: new List()},
        b: {resources: new List(), attendees: new List()}
      }),
      allDay: new Map({
        a: {resources: new List(), attendees: new List()},
        b: {resources: new List(), attendees: new List()},
        c: {resources: new List(), attendees: new List()}
      })
    };

    const day2 = {
      common: new Map({
        a: {resources: new List(), attendees: new List()},
        b: {resources: new List(), attendees: new List()}
      }),
      allDay: new Map({
        a: {resources: new List(), attendees: new List()},
        b: {resources: new List(), attendees: new List()}
      })
    };

    const key1 = memoizeResolvers.desktop(day1);
    const key2 = memoizeResolvers.desktop(day2);

    expect(key1).not.toEqual(key2);
  });

  test('не должен возвращать одинаковый ключ, если один из дней сегодня', () => {
    const day1 = {
      common: new Map({
        a: {resources: new List(), attendees: new List()},
        b: {resources: new List(), attendees: new List()}
      }),
      allDay: new Map({
        a: {resources: new List(), attendees: new List()},
        b: {resources: new List(), attendees: new List()}
      }),
      isToday: true
    };

    const day2 = {
      common: new Map({
        a: {resources: new List(), attendees: new List()},
        b: {resources: new List(), attendees: new List()}
      }),
      allDay: new Map({
        a: {resources: new List(), attendees: new List()},
        b: {resources: new List(), attendees: new List()}
      })
    };

    const key1 = memoizeResolvers.desktop(day1);
    const key2 = memoizeResolvers.desktop(day2);

    expect(key1).not.toEqual(key2);
  });

  test('должен вернуть одинаковый ключ для любого количества участников > 0', () => {
    const day1 = {
      common: new Map({
        a: {resources: new List(), attendees: new List([{}, {}, {}, {}, {}, {}])}
      }),
      allDay: new Map({})
    };

    const day2 = {
      common: new Map({
        a: {resources: new List(), attendees: new List([{}, {}])}
      }),
      allDay: new Map({})
    };

    const key1 = memoizeResolvers.desktop(day1);
    const key2 = memoizeResolvers.desktop(day2);

    expect(key1).toEqual(key2);
  });

  test('должен вернуть одинаковый ключ если в одном дне есть участники, а в другом одна переговорка', () => {
    const day1 = {
      common: new Map({
        a: {resources: new List(), attendees: new List([{}, {}])}
      }),
      allDay: new Map({})
    };

    const day2 = {
      common: new Map({
        a: {resources: new List([{}]), attendees: new List()}
      }),
      allDay: new Map({})
    };

    const key1 = memoizeResolvers.desktop(day1);
    const key2 = memoizeResolvers.desktop(day2);

    expect(key1).toEqual(key2);
  });

  test('не должен вернуть одинаковый ключ если в одном дне есть участники, а в другом несколько переговорок', () => {
    const day1 = {
      common: new Map({
        a: {resources: new List(), attendees: new List([{}, {}, {}])},
        b: {resources: new List(), attendees: new List()}
      }),
      allDay: new Map({
        a: {resources: new List(), attendees: new List()},
        b: {resources: new List(), attendees: new List()}
      })
    };

    const day2 = {
      common: new Map({
        a: {resources: new List([{}, {}]), attendees: new List()}
      }),
      allDay: new Map({})
    };

    const key1 = memoizeResolvers.desktop(day1);
    const key2 = memoizeResolvers.desktop(day2);

    expect(key1).not.toEqual(key2);
  });

  test('должен вернуть разные ключи: в одном дне есть участники в разных встречах, в другом участники в одной встрече', () => {
    const day1 = {
      common: new Map({
        a: {resources: new List(), attendees: new List([{}, {}, {}])},
        b: {resources: new List(), attendees: new List([{}])}
      }),
      allDay: new Map({})
    };

    const day2 = {
      common: new Map({
        a: {resources: new List(), attendees: new List([{}, {}, {}, {}])},
        b: {resources: new List(), attendees: new List()}
      }),
      allDay: new Map({})
    };

    const key1 = memoizeResolvers.desktop(day1);
    const key2 = memoizeResolvers.desktop(day2);

    expect(key1).not.toEqual(key2);
  });
});

describe('getDayBlockHeightTouch', () => {
  test('должен возвращать 0, если событий нет', () => {
    const daySummary = {
      common: new Map(),
      allDay: new Map()
    };

    expect(getDayBlockHeightTouch(daySummary)).toBe(0);
  });
  test('должен возвращать 108 при двух обычных событиях', () => {
    const daySummary = {
      common: new Map({a: 1, b: 2}),
      allDay: new Map()
    };

    expect(getDayBlockHeightTouch(daySummary)).toBe(108);
  });
  test('должен возвращать 116 при двух событиях на весь день', () => {
    const daySummary = {
      common: new Map(),
      allDay: new Map({a: 1, b: 2})
    };

    expect(getDayBlockHeightTouch(daySummary)).toBe(108);
  });
  test('должен возвращать 188 при двух обычных и двух на весь день', () => {
    const daySummary = {
      common: new Map({a: 1, b: 2}),
      allDay: new Map({a: 1, b: 2})
    };

    expect(getDayBlockHeightTouch(daySummary)).toBe(188);
  });
});
describe('getDayBlockHeightTouch memoizeResolver', () => {
  test('должен возвращать одинаковый ключ для разных мап одного размера', () => {
    const day1 = {
      common: new Map({a: 1, b: 2}),
      allDay: new Map({a: 1, b: 2})
    };

    const day2 = {
      common: new Map({a: 1, b: 2}),
      allDay: new Map({a: 1, b: 2})
    };

    const key1 = memoizeResolvers.touch(day1);
    const key2 = memoizeResolvers.touch(day2);

    expect(key1).toEqual(key2);
  });

  test('должен возвращать разный ключ для мап другого размера', () => {
    const day1 = {
      common: new Map({a: 1, b: 2}),
      allDay: new Map({a: 1, b: 2, c: 3})
    };

    const day2 = {
      common: new Map({a: 1, b: 2}),
      allDay: new Map({a: 1, b: 2})
    };

    const key1 = memoizeResolvers.touch(day1);
    const key2 = memoizeResolvers.touch(day2);

    expect(key1).not.toEqual(key2);
  });
});
