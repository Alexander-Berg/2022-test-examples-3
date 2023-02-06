import moment from 'moment';

import {TimelineOptions} from '../../timelineConstants';
import {
  formatTime,
  makeGroups,
  addClippedTime,
  deleteOutside,
  mergeIntervals,
  invertIntervals,
  findIntersection
} from '../processIntervals';

describe('timeline/utils/processIntervals', () => {
  describe('formatTime', () => {
    test('должен возвращать массив интервалов, со start и end в timestamp', () => {
      const intervals = [
        {
          start: '2017-09-21T10:00:00',
          end: '2017-09-21T18:00:00'
        },
        {
          start: '2017-09-21T11:00:00',
          end: '2017-09-21T19:00:00'
        }
      ];
      expect(formatTime(intervals)).toEqual([
        {
          start: moment('2017-09-21T10:00:00').valueOf(),
          end: moment('2017-09-21T18:00:00').valueOf()
        },
        {
          start: moment('2017-09-21T11:00:00').valueOf(),
          end: moment('2017-09-21T19:00:00').valueOf()
        }
      ]);
    });
  });
  describe('deleteOutside', () => {
    const timelineDate = '2017-09-21';

    test('должен вернуть массив интервалов, попадающих в таймлайн', () => {
      const interval1 = {
        start: moment('2017-09-21T10:00:00').valueOf(),
        end: moment('2017-09-21T18:00:00').valueOf()
      };
      const interval2 = {
        start: moment('2017-09-21T11:00:00').valueOf(),
        end: moment('2017-09-21T19:00:00').valueOf()
      };

      expect(deleteOutside(timelineDate, [interval1, interval2])).toEqual([interval1, interval2]);
    });
    test('должен вернуть интервал, совпадающий границами с границами таймлайна', () => {
      const interval = {
        start: moment('2017-09-21T08:00:00').valueOf(),
        end: moment('2017-09-21T23:00:00').valueOf()
      };

      expect(deleteOutside(timelineDate, [interval])).toEqual([interval]);
    });
    test('должен вернуть интервал, выходящий за границы таймлайна с обеих сторон', () => {
      const interval = {
        start: moment('2017-09-21T06:00:00').valueOf(),
        end: moment('2017-09-21T23:30:00').valueOf()
      };

      expect(deleteOutside(timelineDate, [interval])).toEqual([interval]);
    });
    test('должен вернуть интервал, выходящий за границы таймлайна слева', () => {
      const interval = {
        start: moment('2017-09-21T06:00:00').valueOf(),
        end: moment('2017-09-21T22:30:00').valueOf()
      };

      expect(deleteOutside(timelineDate, [interval])).toEqual([interval]);
    });
    test('должен вернуть интервал, выходящий за границы таймлайна справа', () => {
      const interval = {
        start: moment('2017-09-21T19:00:00').valueOf(),
        end: moment('2017-09-21T23:30:00').valueOf()
      };

      expect(deleteOutside(timelineDate, [interval])).toEqual([interval]);
    });
    test('должен удалить интервал, полностью находящийся слева от таймлайна', () => {
      const interval = {
        start: moment('2017-09-21T07:00:00').valueOf(),
        end: moment('2017-09-21T08:00:00').valueOf()
      };

      expect(deleteOutside(timelineDate, [interval])).toEqual([]);
    });
    test('должен удалить интервал, полностью находящийся справа от таймлайна', () => {
      const interval = {
        start: moment('2017-09-22T00:00:00').valueOf(),
        end: moment('2017-09-22T00:10:00').valueOf()
      };

      expect(deleteOutside(timelineDate, [interval])).toEqual([]);
    });
  });
  describe('addClippedTime', () => {
    const timelineDate = '2017-09-21';

    test('должен добавить границы равные границам интервала для интервала меньше, чем таймлайн', () => {
      const start = moment('2017-09-21T10:00:00').valueOf();
      const end = moment('2017-09-21T18:00:00').valueOf();
      const interval = {
        start: start,
        end: end
      };

      expect(addClippedTime(timelineDate, [interval])).toEqual([
        {
          start: start,
          end: end,
          clippedStart: start,
          clippedEnd: end
        }
      ]);
    });
    test('должен добавить границы равные границам таймлайна для интервала больше, чем таймлайн', () => {
      const start = moment('2010-11-30T10:00:00').valueOf();
      const end = moment('2020-02-29T18:00:00').valueOf();
      const interval = {
        start: start,
        end: end
      };

      expect(addClippedTime(timelineDate, [interval])).toEqual([
        {
          start: start,
          end: end,
          clippedStart: moment(timelineDate)
            .hour(TimelineOptions.START_HOUR)
            .valueOf(),
          clippedEnd: moment(timelineDate)
            .hour(TimelineOptions.END_HOUR)
            .valueOf()
        }
      ]);
    });
    test('должен добавить границы равные границам интервала для интервала равному таймлайну', () => {
      const start = moment('2017-09-21T08:00:00').valueOf();
      const end = moment('2017-09-21T23:00:00').valueOf();
      const interval = {
        start: start,
        end: end
      };

      expect(addClippedTime(timelineDate, [interval])).toEqual([
        {
          start: start,
          end: end,
          clippedStart: start,
          clippedEnd: end
        }
      ]);
    });
  });
  describe('makeGroups', () => {
    test('должен правильно рассчитать начало и конец для группы с 2мя пересекающимися интервалами', () => {
      const start1 = moment('2017-09-21T10:00:00').valueOf();
      const end1 = moment('2017-09-21T18:00:00').valueOf();
      const start2 = moment('2017-09-21T16:00:00').valueOf();
      const end2 = moment('2017-09-21T20:00:00').valueOf();
      const interval1 = {
        start: start1,
        end: end1,
        clippedStart: start1,
        clippedEnd: end1
      };
      const interval2 = {
        start: start2,
        end: end2,
        clippedStart: start2,
        clippedEnd: end2
      };

      expect(makeGroups([interval1, interval2])).toEqual([
        {
          start: start1,
          end: end1,
          clippedStart: start1,
          clippedEnd: end1,
          isFirstOfGroup: true
        },
        {
          start: start2,
          end: end2,
          clippedStart: start2,
          clippedEnd: end2,
          isLastOfGroup: true
        }
      ]);
    });
    test(`должен правильно рассчитать начало и конец для группы с 2мя пересекающимися интервалами,
    предварительно отсортировав по началу интервала`, () => {
      const start1 = moment('2017-09-21T16:00:00').valueOf();
      const end1 = moment('2017-09-21T20:00:00').valueOf();
      const start2 = moment('2017-09-21T10:00:00').valueOf();
      const end2 = moment('2017-09-21T18:00:00').valueOf();
      const interval1 = {
        start: start1,
        end: end1,
        clippedStart: start1,
        clippedEnd: end1
      };
      const interval2 = {
        start: start2,
        end: end2,
        clippedStart: start2,
        clippedEnd: end2
      };

      expect(makeGroups([interval1, interval2])).toEqual([
        {
          start: start2,
          end: end2,
          clippedStart: start2,
          clippedEnd: end2,
          isFirstOfGroup: true
        },
        {
          start: start1,
          end: end1,
          clippedStart: start1,
          clippedEnd: end1,
          isLastOfGroup: true
        }
      ]);
    });
    test('должен правильно рассчитать начало и конец для группы из 2ух совпадающих интервалов', () => {
      const start1 = moment('2017-09-21T10:00:00').valueOf();
      const end1 = moment('2017-09-21T18:00:00').valueOf();
      const interval1 = {
        start: start1,
        end: end1,
        clippedStart: start1,
        clippedEnd: end1
      };
      const interval2 = {
        start: start1,
        end: end1,
        clippedStart: start1,
        clippedEnd: end1
      };

      expect(makeGroups([interval1, interval2])).toEqual([
        {
          start: start1,
          end: end1,
          clippedStart: start1,
          clippedEnd: end1,
          isFirstOfGroup: true,
          isLastOfGroup: true
        },
        {
          start: start1,
          end: end1,
          clippedStart: start1,
          clippedEnd: end1,
          isFirstOfGroup: true,
          isLastOfGroup: true
        }
      ]);
    });
    test('должен правильно рассчитать начало и конец для групп 2ух непересекающихся интервалов', () => {
      const start1 = moment('2017-09-21T10:00:00').valueOf();
      const end1 = moment('2017-09-21T12:00:00').valueOf();
      const start2 = moment('2017-09-21T16:00:00').valueOf();
      const end2 = moment('2017-09-21T20:00:00').valueOf();
      const interval1 = {
        start: start1,
        end: end1,
        clippedStart: start1,
        clippedEnd: end1
      };
      const interval2 = {
        start: start2,
        end: end2,
        clippedStart: start2,
        clippedEnd: end2
      };

      expect(makeGroups([interval1, interval2])).toEqual([
        {
          start: start1,
          end: end1,
          clippedStart: start1,
          clippedEnd: end1,
          isFirstOfGroup: true,
          isLastOfGroup: true
        },
        {
          start: start2,
          end: end2,
          clippedStart: start2,
          clippedEnd: end2,
          isFirstOfGroup: true,
          isLastOfGroup: true
        }
      ]);
    });
    test('должен правильно рассчитать начало и конец для группы с 2мя сопрекасающимимся интервалами', () => {
      const start1 = moment('2017-09-21T10:00:00').valueOf();
      const end1 = moment('2017-09-21T11:30:00').valueOf();
      const start2 = moment('2017-09-21T11:30:00').valueOf();
      const end2 = moment('2017-09-21T12:00:00').valueOf();
      const interval1 = {
        start: start1,
        end: end1,
        clippedStart: start1,
        clippedEnd: end1
      };
      const interval2 = {
        start: start2,
        end: end2,
        clippedStart: start2,
        clippedEnd: end2
      };

      expect(makeGroups([interval1, interval2])).toEqual([
        {
          start: start1,
          end: end1,
          clippedStart: start1,
          clippedEnd: end1,
          isFirstOfGroup: true
        },
        {
          start: start2,
          end: end2,
          clippedStart: start2,
          clippedEnd: end2,
          isLastOfGroup: true
        }
      ]);
    });
    test('должен правильно рассчитать начало и конец для группы, состоящей из интервала без длительности', () => {
      const start1 = moment('2017-09-21T15:00:00').valueOf();
      const end1 = moment('2017-09-21T15:00:00').valueOf();
      const interval1 = {
        start: start1,
        end: end1,
        clippedStart: start1,
        clippedEnd: end1
      };

      expect(makeGroups([interval1])).toEqual([
        {
          start: start1,
          end: end1,
          clippedStart: start1,
          clippedEnd: end1,
          isFirstOfGroup: true,
          isLastOfGroup: true
        }
      ]);
    });
    test('должен правильно рассчитать начало и конец для группы интервалов, выходящих за границы таймлайна', () => {
      const start1 = moment('2017-08-22T10:00:00').valueOf();
      const end1 = moment('2017-09-21T12:00:00').valueOf();
      const start2 = moment('2017-09-20T10:00:00').valueOf();
      const end2 = moment('2017-02-22T20:00:00').valueOf();
      const start3 = moment('2017-09-21T16:00:00').valueOf();
      const end3 = moment('2017-05-21T20:00:00').valueOf();
      const clippedStart = moment('2017-09-21T08:00:00').valueOf();
      const clippedEnd = moment('2017-09-21T23:00:00').valueOf();
      const interval1 = {
        start: start1,
        end: end1,
        clippedStart: clippedStart,
        clippedEnd: end1
      };
      const interval2 = {
        start: start2,
        end: end2,
        clippedStart: clippedStart,
        clippedEnd: clippedEnd
      };
      const interval3 = {
        start: start3,
        end: end3,
        clippedStart: start3,
        clippedEnd: clippedEnd
      };

      expect(makeGroups([interval1, interval2, interval3])).toEqual([
        {
          start: start1,
          end: end1,
          clippedStart: clippedStart,
          clippedEnd: end1,
          isFirstOfGroup: true
        },
        {
          start: start2,
          end: end2,
          clippedStart: clippedStart,
          clippedEnd: clippedEnd,
          isFirstOfGroup: true,
          isLastOfGroup: true
        },
        {
          start: start3,
          end: end3,
          clippedStart: start3,
          clippedEnd: clippedEnd,
          isLastOfGroup: true
        }
      ]);
    });
  });

  describe('mergeIntervals', () => {
    test('должен отдавать пустой список, если на входе пустой список', () =>
      expect(mergeIntervals([])).toEqual([]));
    test('должен отдавать список из одного интервала, если на входе список из одного интервала', () => {
      const interval1 = {
        start: moment('2017-09-21T10:00:00').valueOf(),
        end: moment('2017-09-21T12:00:00').valueOf()
      };

      expect(mergeIntervals([interval1])).toEqual([interval1]);
    });
    test('должен сортировать интервалы по start', () => {
      const interval1 = {
        start: moment('2017-09-21T12:00:00').valueOf(),
        end: moment('2017-09-21T14:00:00').valueOf()
      };
      const interval2 = {
        start: moment('2017-09-21T10:00:00').valueOf(),
        end: moment('2017-09-21T11:00:00').valueOf()
      };

      expect(mergeIntervals([interval1, interval2])).toEqual([interval2, interval1]);
    });
    test('должен объединять интервалы с общими подинтервалами', () => {
      const start1 = moment('2017-09-21T11:00:00').valueOf();
      const end1 = moment('2017-09-21T13:00:00').valueOf();
      const start2 = moment('2017-09-21T12:00:00').valueOf();
      const end2 = moment('2017-09-21T14:00:00').valueOf();
      const interval1 = {
        start: start1,
        end: end1
      };
      const interval2 = {
        start: start2,
        end: end2
      };

      expect(mergeIntervals([interval2, interval1])).toEqual([
        {
          start: start1,
          end: end2
        }
      ]);
    });
  });
  describe('invertIntervals', () => {
    test('должен отдавать интервал в весь входной период времени, если на вход не пришел ни один интервал', () => {
      const periodStart = moment('2017-09-21T00:00:00').valueOf();
      const periodEnd = moment('2017-09-22T00:00:00').valueOf();
      expect(invertIntervals([], periodStart, periodEnd)).toEqual([
        {start: periodStart, end: periodEnd}
      ]);
    });
    test('должен отдавать два "крайних" интервала, если на вход пришел один интервал', () => {
      const periodStart = moment('2017-09-21T00:00:00').valueOf();
      const periodEnd = moment('2017-09-22T00:00:00').valueOf();
      const interval = {
        start: moment('2017-09-21T10:00:00').valueOf(),
        end: moment('2017-09-21T11:00:00').valueOf()
      };
      expect(invertIntervals([interval], periodStart, periodEnd)).toEqual([
        {start: periodStart, end: interval.start},
        {start: interval.end, end: periodEnd}
      ]);
    });
    test('должен корректно отдавать инвертированные интервалы', () => {
      const periodStart = moment('2017-09-21T00:00:00').valueOf();
      const periodEnd = moment('2017-09-22T00:00:00').valueOf();
      const interval1 = {
        start: moment('2017-09-21T10:00:00').valueOf(),
        end: moment('2017-09-21T11:00:00').valueOf()
      };
      const interval2 = {
        start: moment('2017-09-21T14:00:00').valueOf(),
        end: moment('2017-09-21T15:00:00').valueOf()
      };
      expect(invertIntervals([interval1, interval2], periodStart, periodEnd)).toEqual([
        {start: periodStart, end: interval1.start},
        {start: interval1.end, end: interval2.start},
        {start: interval2.end, end: periodEnd}
      ]);
    });
    test(`должен отдавать один интервал, между двумя входными, если входные интервалы начинают и заканчиваются в
  начале/конце входного периода`, () => {
      const periodStart = moment('2017-09-21T00:00:00').valueOf();
      const periodEnd = moment('2017-09-22T00:00:00').valueOf();
      const interval1 = {
        start: periodStart,
        end: moment('2017-09-21T11:00:00').valueOf()
      };
      const interval2 = {
        start: moment('2017-09-21T14:00:00').valueOf(),
        end: periodEnd
      };
      expect(invertIntervals([interval1, interval2], periodStart, periodEnd)).toEqual([
        {start: interval1.end, end: interval2.start}
      ]);
    });
  });

  describe('findIntersection', () => {
    test('должен отдавать пересечения двух наборов интервалов', () => {
      const intervals1 = [
        {start: 0, end: 2},
        {start: 5, end: 10},
        {start: 13, end: 23},
        {start: 24, end: 25}
      ];
      const intervals2 = [
        {start: 1, end: 5},
        {start: 8, end: 12},
        {start: 15, end: 24},
        {start: 25, end: 26}
      ];
      const expected = [{start: 1, end: 2}, {start: 8, end: 10}, {start: 15, end: 23}];

      expect(findIntersection(intervals1, intervals2)).toEqual(expected);
    });
    test('должен отдавать пустой список интервалов, если хотя бы один из входных списков пустой', () => {
      const intervals1 = [
        {start: 0, end: 2},
        {start: 5, end: 10},
        {start: 13, end: 23},
        {start: 24, end: 25}
      ];
      const intervals2 = [];

      expect(findIntersection(intervals1, intervals2)).toEqual([]);
    });
    test('должен отдавать пустой список интервалов, если входные списки интервалов не пересекаются', () => {
      const intervals1 = [
        {start: 0, end: 2},
        {start: 5, end: 10},
        {start: 13, end: 23},
        {start: 24, end: 25}
      ];
      const intervals2 = [
        {start: 3, end: 4},
        {start: 11, end: 12},
        {start: 23, end: 24},
        {start: 25, end: 26}
      ];

      expect(findIntersection(intervals1, intervals2)).toEqual([]);
    });
    test('не должен учитывать пересечения границ интервалов', () => {
      const intervals1 = [
        {start: 0, end: 2},
        {start: 4, end: 10},
        {start: 12, end: 23},
        {start: 24, end: 25}
      ];
      const intervals2 = [
        {start: 2, end: 4},
        {start: 10, end: 12},
        {start: 23, end: 24},
        {start: 25, end: 26}
      ];

      expect(findIntersection(intervals1, intervals2)).toEqual([]);
    });
  });
});
