import moment from 'moment';

import {MINUTE, HOUR} from 'constants/Durations';
import EventRecord from 'features/events/EventRecord';

import GridItemPosition from '../GridItemPosition';
import positionGridItemsByTime from '../position-grid-items-by-time';

const dateStr = '2000-01-01';
const date = moment(dateStr);
const event = (startTime, endTime, props) =>
  new EventRecord({start: dateStr + 'T' + startTime, end: dateStr + 'T' + endTime, ...props});
const eventWithDate = (start, end, props) => new EventRecord({start, end, ...props});

describe('position-grid-items-by-time', () => {
  it('должен возвращать массив позиций', () => {
    const events = [event('01:00:00', '02:00:00')];

    const itemsPos = positionGridItemsByTime(date, {events});

    expect(itemsPos).toHaveLength(1);
    expect(itemsPos[0]).toBeInstanceOf(GridItemPosition);
  });

  it('должен возвращать пустой массив позиций, когда входящий массив событий пуст', () => {
    const events = [];

    const itemsPos = positionGridItemsByTime(date, {events});

    expect(itemsPos).toEqual([]);
  });

  describe('фильтрация по дате', () => {
    it('должен отфильтровывать события, время начала которых позже времени конца', () => {
      const events = [
        event('00:02:00', '00:01:00', {id: 'A'}), // A
        event('00:01:00', '00:02:00', {id: 'B'}), // B
        event('00:02:00', '00:02:00', {id: 'C'}) //  C
      ];

      const itemsPos = positionGridItemsByTime(date, {events});

      expect(itemsPos).toHaveLength(2);
      expect(itemsPos).toMatchObject([
        {item: events[1]}, // B
        {item: events[2]} //  C
      ]);
    });

    it('должен отфильтровывать события, находящиеся целиком вне периода', () => {
      const events = [
        eventWithDate('2018-08-24T23:00:00', '2018-08-25T00:00:00'), // A. целиком вне периода
        eventWithDate('2018-08-24T23:00:00', '2018-08-25T01:00:00'), // B. частично вне периода
        eventWithDate('2018-08-25T00:00:00', '2018-08-26T00:00:00'), // C. целиком внутри периода
        eventWithDate('2018-08-25T23:00:00', '2018-08-26T01:00:00'), // D. частично вне периода
        eventWithDate('2018-08-26T00:00:00', '2018-08-26T01:00:00') //  E. целиком вне периода
      ];

      const itemsPos = positionGridItemsByTime(moment('2018-08-25'), {events});

      expect(itemsPos).toHaveLength(3);
      expect(itemsPos).toMatchObject([
        {item: events[1]}, // B
        {item: events[2]}, // C
        {item: events[3]} //  D
      ]);
    });

    it('не должен отфильтровывать события нулевой продолжительности на границе суток', () => {
      const events = [
        eventWithDate('2000-01-01T00:00:00', '2000-01-01T00:00:00'), // A.
        eventWithDate('2000-01-02T00:00:00', '2000-01-02T00:00:00') //  B. следующие сутки
      ];

      const itemsPos = positionGridItemsByTime(moment('2000-01-01'), {events});

      expect(itemsPos).toHaveLength(1);
      expect(itemsPos).toMatchObject([
        {item: events[0]} // A
      ]);
    });
  });

  describe('сортировка', () => {
    it('должен сортировать события по времени начала', () => {
      const events = [
        eventWithDate('2000-01-02T01:05:00', '2000-01-02T02:05:00'), // D
        eventWithDate('2000-01-02T00:00:00', '2000-01-02T05:00:00'), // B
        eventWithDate('2000-01-02T01:00:00', '2000-01-02T02:00:00'), // C
        eventWithDate('2000-01-01T23:00:00', '2000-01-02T05:00:00') //  A
      ];

      const itemsPos = positionGridItemsByTime(
        moment('2000-01-02'),
        {events},
        {rowDurationMs: 1 * HOUR, groupingThresholdMs: 1 * MINUTE}
      );

      expect(itemsPos).toMatchObject([
        {item: events[3], row: 0}, // A
        {item: events[1], row: 0}, // B
        {item: events[2], row: 1}, // C
        {item: events[0], row: 1} //  D
      ]);
    });

    it('должен сортировать события по длительности', () => {
      const events = [
        eventWithDate('2000-01-01T19:00:00', '2000-01-01T19:30:00'), // D. 0.5ч
        eventWithDate('2000-01-01T19:00:00', '2000-01-02T00:00:00'), // B. 5ч
        eventWithDate('2000-01-01T19:00:00', '2000-01-01T23:00:00'), // C. 4ч
        eventWithDate('2000-01-01T19:00:00', '2000-01-02T01:00:00') //  A. 6ч
      ];

      const itemsPos = positionGridItemsByTime(
        moment('2000-01-01'),
        {events},
        {rowDurationMs: 1 * HOUR, groupingThresholdMs: 1 * MINUTE}
      );

      expect(itemsPos).toMatchObject([
        {item: events[3], spanRows: 5}, // A
        {item: events[1], spanRows: 5}, // B
        {item: events[2], spanRows: 4}, // C
        {item: events[0], spanRows: 1} //  D
      ]);
    });

    it('должен сортировать события по id', () => {
      const events = [
        event('00:00:00', '01:00:00', {id: 1}), // B
        event('00:00:00', '01:00:00', {id: 2}), // C
        event('00:00:00', '01:00:00', {id: 0}) //  A
      ];

      const itemsPos = positionGridItemsByTime(date, {events});

      expect(itemsPos).toMatchObject([
        {item: events[2], column: 0}, // A
        {item: events[0], column: 1}, // B
        {item: events[1], column: 2} //  C
      ]);
    });

    it('должен сортировать событие в конец списка, когда его id это строка', () => {
      const events = [
        event('00:00:00', '01:00:00', {id: 1}), // B
        event('00:00:00', '01:00:00', {id: 2}), // C
        event('00:00:00', '01:00:00', {id: 'draft'}), // X
        event('00:00:00', '01:00:00', {id: 0}) //  A
      ];

      const itemsPos = positionGridItemsByTime(date, {events});

      expect(itemsPos).toMatchObject([
        {item: events[3], column: 0}, // A
        {item: events[0], column: 1}, // B
        {item: events[1], column: 2}, // C
        {item: events[2], column: 3} //  X
      ]);
    });

    it('должен сортировать событие в конец списка, когда его id равен null', () => {
      const events = [
        event('00:00:00', '01:00:00', {id: 1}), // B
        event('00:00:00', '01:00:00', {id: null}), // X
        event('00:00:00', '01:00:00', {id: 2}), // C
        event('00:00:00', '01:00:00', {id: 0}) //  A
      ];

      const itemsPos = positionGridItemsByTime(date, {events});

      expect(itemsPos).toMatchObject([
        {item: events[3], column: 0}, // A
        {item: events[0], column: 1}, // B
        {item: events[2], column: 2}, // C
        {item: events[1], column: 3} //  X
      ]);
    });

    it('должен сортировать события по uuid', () => {
      const events = [
        event('00:00:00', '01:00:00', {id: 0, layerId: 0, instanceStartTs: 1}), // B
        event('00:00:00', '01:00:00', {id: 0, layerId: 1, instanceStartTs: 0}), // C
        event('00:00:00', '01:00:00', {id: 0, layerId: 0, instanceStartTs: 0}) //  A
      ];

      const itemsPos = positionGridItemsByTime(date, {events});

      expect(itemsPos).toMatchObject([
        {item: events[2], column: 0}, // A
        {item: events[0], column: 1}, // B
        {item: events[1], column: 2} //  C
      ]);
    });
  });

  describe('вычисление номера строки (row)', () => {
    it('должен вычислять строку, когда начало события кратно длительности строки', () => {
      const events = [event('00:01:00', '00:02:00')];

      const itemsPos = positionGridItemsByTime(date, {events}, {rowDurationMs: 1 * MINUTE});

      expect(itemsPos[0].row).toEqual(1);
    });

    it('должен вычислять строку, когда начало события НЕ кратно длительности строки', () => {
      const events = [event('00:06:00', '00:07:00')];

      const itemsPos = positionGridItemsByTime(date, {events}, {rowDurationMs: 5 * MINUTE});

      expect(itemsPos[0].row).toEqual(1);
    });

    it('должен обрезать начало события, когда оно выходит за границы суток', () => {
      const events = [
        eventWithDate('2018-08-24T23:00:00', '2018-08-25T00:00:00'), // A. целиком вне периода
        eventWithDate('2018-08-24T23:00:00', '2018-08-25T01:00:00'), // B. частично вне периода
        eventWithDate('2018-08-25T00:00:00', '2018-08-26T00:00:00'), // C. целиком внутри периода
        eventWithDate('2018-08-25T23:00:00', '2018-08-26T01:00:00'), // D. частично вне периода
        eventWithDate('2018-08-26T00:00:00', '2018-08-26T01:00:00') //  E. целиком вне периода
      ];

      const itemsPos = positionGridItemsByTime(
        moment('2018-08-25'),
        {events},
        {rowDurationMs: 1 * HOUR}
      );

      expect(itemsPos).toHaveLength(3);
      expect(itemsPos).toMatchObject([
        {row: 0}, // B
        {row: 0}, // C
        {row: 23} // D
      ]);
    });
  });

  describe('вычисление колличества строк в высоту (spanRows)', () => {
    it('должен вычислять высоту, когда длительность события больше длительности строки', () => {
      const events = [event('00:01:00', '00:03:00')];

      const itemsPos = positionGridItemsByTime(
        date,
        {events},
        {rowDurationMs: 1 * MINUTE, minItemDurationMs: 1 * MINUTE}
      );

      expect(itemsPos[0].spanRows).toEqual(2);
    });

    it('должен вычислять высоту, когда длительность события меньше длительности строки', () => {
      const events = [event('00:17:00', '00:20:00')];

      const itemsPos = positionGridItemsByTime(date, {events}, {rowDurationMs: 15 * MINUTE});

      expect(itemsPos[0].spanRows).toEqual(1);
    });

    it('должен вычислять высоту, когда длительность события НЕ кратна длительности строки', () => {
      const events = [event('00:05:00', '00:12:00')];

      const itemsPos = positionGridItemsByTime(
        date,
        {events},
        {rowDurationMs: 5 * MINUTE, minItemDurationMs: 1 * MINUTE}
      );

      expect(itemsPos[0].spanRows).toEqual(2);
    });

    it('должен обрезать конец события, когда он выходит за границы суток', () => {
      const events = [
        eventWithDate('2018-08-24T23:00:00', '2018-08-25T00:00:00'), // A. целиком вне периода
        eventWithDate('2018-08-24T23:00:00', '2018-08-25T01:00:00'), // B. частично вне периода
        eventWithDate('2018-08-25T00:00:00', '2018-08-26T00:00:00'), // C. целиком внутри периода
        eventWithDate('2018-08-25T23:00:00', '2018-08-26T01:00:00'), // D. частично вне периода
        eventWithDate('2018-08-26T00:00:00', '2018-08-26T01:00:00') //  E. целиком вне периода
      ];

      const itemsPos = positionGridItemsByTime(
        moment('2018-08-25'),
        {events},
        {rowDurationMs: 1 * HOUR}
      );

      expect(itemsPos).toHaveLength(3);
      expect(itemsPos).toMatchObject([
        {spanRows: 1}, //  B
        {spanRows: 24}, // C
        {spanRows: 1} //   D
      ]);
    });

    it('должен расширять событие по длительности, когда оно меньше минимальной', () => {
      const events = [
        event('00:00:00', '00:20:00'), // A
        event('00:00:00', '00:15:00'), // B
        event('00:00:00', '00:05:00') //  C
      ];

      const itemsPos = positionGridItemsByTime(
        date,
        {events},
        {rowDurationMs: 5 * MINUTE, minItemDurationMs: 15 * MINUTE}
      );

      expect(itemsPos).toMatchObject([
        {spanRows: 4}, // A
        {spanRows: 3}, // B
        {spanRows: 3} //  C
      ]);
    });
  });

  describe('вычисление колличества колонок в ширину (spanColumns)', () => {
    it('должен вычислять ширину, когда события НЕ пересекаются по времени', () => {
      const events = [
        event('00:00:00', '01:00:00'), // A
        event('01:00:00', '02:00:00') //  B
      ];

      const itemsPos = positionGridItemsByTime(date, {events});

      expect(itemsPos).toMatchObject([
        {columnsTotal: 1, spanColumns: 1}, // A
        {columnsTotal: 1, spanColumns: 1} //  B
      ]);
    });

    it('должен вычислять ширину, когда события пересекаются по времени', () => {
      const events = [
        event('00:00:00', '02:00:00'), // A
        event('00:30:00', '02:00:00') //  B
      ];

      const itemsPos = positionGridItemsByTime(date, {events}, {groupingThresholdMs: 45 * MINUTE});

      expect(itemsPos).toMatchObject([
        {columnsTotal: 2, spanColumns: 1}, // A
        {columnsTotal: 2, spanColumns: 1} //  B
      ]);
    });

    it('должен растягивать событие по ширине, вплоть до правого края', () => {
      /* _________________ 
       |     |           |
       |  A  |     B     |  Событие B растянуто до правого края.
       |     |___________|
       |     |     |     |
       |     |  C  |  D  |
       |_____|_____|_____|*/
      const events = [
        event('00:00:00', '01:00:00'), // A
        event('00:00:00', '00:30:00'), // B
        event('00:30:00', '01:00:00'), // C
        event('00:30:00', '01:00:00') //  D
      ];

      const itemsPos = positionGridItemsByTime(date, {events}, {groupingThresholdMs: 45 * MINUTE});

      expect(itemsPos).toMatchObject([
        {columnsTotal: 3, spanColumns: 1}, // A
        {columnsTotal: 3, spanColumns: 2}, // B
        {columnsTotal: 3, spanColumns: 1}, // C
        {columnsTotal: 3, spanColumns: 1} //  D
      ]);
    });

    it('должен растягивать событие по ширине, вплоть до пересекающего события', () => {
      /* _________________ 
       |     |     |     |_____
       |  A  |  B  |  C  |     |
       |     |_____|_____|  D  |
       |     |           |_____|  Событие E растянуто до пересекающего D.
       |     |     E     |
       |_____|___________| */
      const events = [
        event('00:00:00', '01:00:00'), // A
        event('00:00:00', '00:30:00'), // B
        event('00:00:00', '00:30:00'), // C
        event('00:10:00', '00:40:00'), // D
        event('00:30:00', '01:00:00') //  E
      ];

      const itemsPos = positionGridItemsByTime(date, {events}, {groupingThresholdMs: 45 * MINUTE});

      expect(itemsPos).toMatchObject([
        {columnsTotal: 4, spanColumns: 1}, // A
        {columnsTotal: 4, spanColumns: 1}, // B
        {columnsTotal: 4, spanColumns: 1}, // C
        {columnsTotal: 4, spanColumns: 1}, // D
        {columnsTotal: 4, spanColumns: 2} //  E
      ]);
    });
  });

  describe('вычисление номера колонки (column, columnsTotal)', () => {
    it('не должен группировать события, когда они не пересекаются по времени', () => {
      const events = [
        event('01:00:00', '02:00:00'), // A
        event('02:00:00', '03:00:00') //  B
      ];

      const itemsPos = positionGridItemsByTime(date, {events});

      expect(itemsPos).toMatchObject([
        {column: 0, columnsTotal: 1}, // A
        {column: 0, columnsTotal: 1} //  B
      ]);
    });

    it('должен группировать события учитывая порог группировки по времени старта', () => {
      const events = [
        event('00:00:00', '02:00:00', {id: 0}), // A. инициирует первую группу
        event('00:00:00', '02:00:00', {id: 1}), // B. начинаются в одно время
        event('00:29:00', '02:00:00', {id: 2}), // C. начало раньше порога группировки
        event('00:30:00', '02:00:00', {id: 3}), // D. начало равно порогу
        event('01:10:00', '02:00:00', {id: 4}) //  E. начало позже порога
      ];

      const itemsPos = positionGridItemsByTime(date, {events}, {groupingThresholdMs: 30 * MINUTE});

      expect(itemsPos).toMatchObject([
        {item: events[0], column: 0, columnsTotal: 3}, // A
        {item: events[1], column: 1, columnsTotal: 3}, // B
        {item: events[2], column: 2, columnsTotal: 3}, // C

        // не попали в первую группу
        {item: events[3], column: 0, columnsTotal: 1}, // D
        {item: events[4], column: 0, columnsTotal: 1} //  E
      ]);
    });

    it('должен группировать элементы дочерних групп', () => {
      /* ------------
       |           |
       |     A     |
       |___________|
       |     | C A |
       | B A |_____|  B и C образуют дочернуюю группу
       |     |  A  |
       -------------*/
      const events = [
        event('00:00:00', '01:00:00'), // A
        event('00:30:00', '01:00:00'), // B
        event('00:30:00', '00:50:00') //  С
      ];

      const itemsPos = positionGridItemsByTime(
        date,
        {events},
        {rowDurationMs: 10 * MINUTE, groupingThresholdMs: 10 * MINUTE}
      );

      expect(itemsPos).toMatchObject([
        {columnsTotal: 1, column: 0}, // A
        {columnsTotal: 2, column: 0}, // B
        {columnsTotal: 2, column: 1} //  C
      ]);
    });

    it('не должен группировать предпросмотр события с другими элементами', () => {
      const events = [
        event('01:00:00', '02:00:00'), //                    A
        event('01:00:00', '02:00:00', {isPreview: true}), // B
        event('01:00:00', '02:00:00') //                     C
      ];

      const itemsPos = positionGridItemsByTime(date, {events});

      expect(itemsPos).toMatchObject([
        {column: 0, columnsTotal: 2}, // A
        {column: 1, columnsTotal: 2}, // C
        {column: 0, columnsTotal: 1} //  B
      ]);
    });

    it('должен группировать события, когда они начинаются раньше периода', () => {
      const events = [
        eventWithDate('2000-01-01T23:00:00', '2000-01-02T01:00:00'), // A
        eventWithDate('2000-01-02T00:15:00', '2000-01-02T01:00:00'), // B
        eventWithDate('2000-01-02T00:00:00', '2000-01-02T01:00:00') //  C
      ];

      const itemsPos = positionGridItemsByTime(
        moment('2000-01-02'),
        {events},
        {rowDurationMs: 5 * MINUTE, groupingThresholdMs: 30 * MINUTE}
      );

      expect(itemsPos).toMatchObject([
        {column: 0, columnsTotal: 3},
        {column: 1, columnsTotal: 3},
        {column: 2, columnsTotal: 3}
      ]);
    });

    it('должен располагать события группы в одной колонке, когда они не пересекаются', () => {
      /* ___________
       |     |     |
       |     |  B  |
       |  A  |_____|
       |     |     |
       |     |  C  |
       |_____|_____|*/
      const events = [
        event('00:00:00', '01:00:00'), // A
        event('00:00:00', '00:30:00'), // B
        event('00:30:00', '01:00:00') //  C
      ];

      const itemsPos = positionGridItemsByTime(
        date,
        {events},
        {rowDurationMs: 10 * MINUTE, groupingThresholdMs: 45 * MINUTE}
      );

      expect(itemsPos).toMatchObject([
        {item: events[0], columnsTotal: 2, column: 0}, // A
        {item: events[1], columnsTotal: 2, column: 1}, // B
        {item: events[2], columnsTotal: 2, column: 1} //  C
      ]);
    });

    it('должен располагать событие в свободной колонке, вплотную к левому краю', () => {
      /* _____
       |     |
       |  A  |_____
       |_____|     |
       |     |  B  |  Событие C располагается вплотную к левому краю.
       |  C  |_____|
       |_____|*/
      const events = [
        event('00:00:00', '00:30:00'), // A
        event('00:20:00', '00:50:00'), // B
        event('00:30:00', '01:00:00') //  C
      ];

      const itemsPos = positionGridItemsByTime(
        date,
        {events},
        {rowDurationMs: 10 * MINUTE, groupingThresholdMs: 45 * MINUTE}
      );

      expect(itemsPos).toMatchObject([
        {columnsTotal: 2, column: 0}, // A
        {columnsTotal: 2, column: 1}, // B
        {columnsTotal: 2, column: 0} //  C
      ]);
    });

    it('должен располагать событие в свободной колонке, вплотную к пересекающему событию', () => {
      /* ___________
       |     |     |_____
       |     |  B  |     |
       |  A  |_____|  C  |
       |     |     |_____|  Событие D располагается вплотную к пересекающему A.
       |     |  D  |
       |_____|_____|*/
      const events = [
        event('00:00:00', '01:00:00'), // A
        event('00:00:00', '00:30:00'), // B
        event('00:20:00', '00:50:00'), // C
        event('00:30:00', '01:00:00') //  D
      ];

      const itemsPos = positionGridItemsByTime(
        date,
        {events},
        {rowDurationMs: 10 * MINUTE, groupingThresholdMs: 45 * MINUTE}
      );

      expect(itemsPos).toMatchObject([
        {columnsTotal: 3, column: 0}, // A
        {columnsTotal: 3, column: 1}, // B
        {columnsTotal: 3, column: 2}, // C
        {columnsTotal: 3, column: 1} //  D
      ]);
    });
  });

  describe('вычисление глубины и высоты групп в дереве (groupDepth/groupHeight)', () => {
    it('должен вычислять дерево групп, когда события не пересекаются', () => {
      const events = [
        event('01:00:00', '02:00:00'), // A
        event('02:00:00', '03:00:00') //  B
      ];

      const itemsPos = positionGridItemsByTime(date, {events});

      expect(itemsPos).toMatchObject([
        {groupDepth: 0, groupHeight: 0}, // A
        {groupDepth: 0, groupHeight: 0} //  B
      ]);
    });

    it('должен вычислять дерево групп для событий одной группы', () => {
      const events = [
        event('01:00:00', '02:00:00'), // A. первая группа
        event('01:00:00', '02:00:00'), // B. первая группа
        event('01:30:00', '02:00:00'), // C. вторая группа
        event('01:45:00', '02:00:00') //  D. вторая группа
      ];

      const itemsPos = positionGridItemsByTime(date, {events}, {groupingThresholdMs: 30 * MINUTE});

      expect(itemsPos).toMatchObject([
        {groupDepth: 0, groupHeight: 1}, // A
        {groupDepth: 0, groupHeight: 1}, // B
        {groupDepth: 1, groupHeight: 0}, // C
        {groupDepth: 1, groupHeight: 0} //  D
      ]);
    });

    it('должен вычислять дерево групп, когда есть дочерние группы', () => {
      /* ------------
       |           |
       |     A     |
       |___________|
       |           |
       |    B A    |
       |___________|
       |           |
       |   C B A   |
       |___________|
       |           |
       |    D A    |
       |           |
       -------------*/
      const events = [
        event('00:00:00', '01:00:00'), // A
        event('00:15:00', '00:45:00'), // B
        event('00:30:00', '00:45:00'), // С
        event('00:45:00', '01:00:00') //  D
      ];

      const itemsPos = positionGridItemsByTime(
        date,
        {events},
        {rowDurationMs: 5 * MINUTE, groupingThresholdMs: 15 * MINUTE}
      );

      expect(itemsPos).toMatchObject([
        {groupDepth: 0, groupHeight: 2}, // A
        {groupDepth: 1, groupHeight: 1}, // B
        {groupDepth: 2, groupHeight: 0}, // C
        {groupDepth: 1, groupHeight: 0} //  D
      ]);
    });

    it('должен расширять временной диапазон группы при добавлении элемента', () => {
      /* ------------
       |           |
       |     A     |
       |_____      |
       |     |_____|  B инициирует первую дочернюю группу A
       | B A |     |  
       |_____| C A |  С должен её расширять,
       |     |     |
       |     |     |
       |_____|_____|
       |     .     |
       |   D C A   |  чтобы D в неё попадал (A -> (B|C) -> D)
       |     .     |
       -------------*/
      const events = [
        event('00:00:00', '01:00:00'), // A
        event('00:15:00', '00:30:00'), // B
        event('00:20:00', '01:00:00'), // С
        event('00:45:00', '01:00:00') //  D
      ];

      const itemsPos = positionGridItemsByTime(
        date,
        {events},
        {rowDurationMs: 5 * MINUTE, groupingThresholdMs: 15 * MINUTE}
      );

      expect(itemsPos).toMatchObject([
        {groupDepth: 0, groupHeight: 2}, // A
        {groupDepth: 1, groupHeight: 1}, // B
        {groupDepth: 1, groupHeight: 1}, // C
        {groupDepth: 2, groupHeight: 0} //  D
      ]);
    });

    it('должен расширять временной диапазон группы при добавлении дочерней группы', () => {
      /* ------------
       |           |
       |     A     |
       |___________|
       |           |
       |    B A    |  B инициирует первую дочернюю группу A 
       |___________|
       |...........|
       |    C  A   |  С должен её расширять,
       |___________|
       |...........|
       |   D C A   |  чтобы D в неё попадал (A -> B -> C -> D)
       |           |
       -------------*/
      const events = [
        event('00:00:00', '01:00:00'), // A
        event('00:15:00', '00:35:00'), // B
        event('00:30:00', '00:50:00'), // С
        event('00:45:00', '01:00:00') //  D
      ];

      const itemsPos = positionGridItemsByTime(
        date,
        {events},
        {rowDurationMs: 5 * MINUTE, groupingThresholdMs: 15 * MINUTE}
      );

      expect(itemsPos).toMatchObject([
        {groupDepth: 0, groupHeight: 3}, // A
        {groupDepth: 1, groupHeight: 2}, // B
        {groupDepth: 2, groupHeight: 1}, // C
        {groupDepth: 3, groupHeight: 0} //  D
      ]);
    });

    it('не должен добавлять события в группу предпросмотра', () => {
      const events = [
        event('01:00:00', '02:00:00'), //                    A
        event('01:15:00', '02:00:00', {isPreview: true}), // B
        event('01:30:00', '02:00:00') //                     C
      ];

      const itemsPos = positionGridItemsByTime(date, {events}, {groupingThresholdMs: 10 * MINUTE});

      expect(itemsPos).toMatchObject([
        {groupDepth: 0, groupHeight: 1}, // A
        {groupDepth: 1, groupHeight: 0}, // C
        {groupDepth: 0, groupHeight: 0} //  B
      ]);
    });
  });

  describe('вычисление перекрытых строк элемента (nonOverlapRows)', () => {
    it('должен вычислять перекрытие, когда элементы не пересекаются по времени', () => {
      const events = [
        event('01:00:00', '02:00:00'), // A
        event('02:00:00', '02:30:00') //  B
      ];

      const itemsPos = positionGridItemsByTime(date, {events}, {rowDurationMs: 10 * MINUTE});

      expect(itemsPos).toMatchObject([
        {nonOverlapRows: 6}, // A
        {nonOverlapRows: 3} //  B
      ]);
    });

    it('должен вычислять перекрытие, когда элементы пересекаются по времени', () => {
      /* ----------------
        |              |
        |      A       |  неперекрытые строки A
        |______________|
        |              |
        |     B A      |  B перекрывает A
        |              |
        ----------------*/
      const events = [
        event('00:00:00', '01:00:00'), // A
        event('00:30:00', '01:00:00') //  B
      ];

      const itemsPos = positionGridItemsByTime(
        date,
        {events},
        {rowDurationMs: 10 * MINUTE, groupingThresholdMs: 30 * MINUTE}
      );

      expect(itemsPos).toMatchObject([
        {row: 0, spanRows: 6, nonOverlapRows: 3}, // A
        {row: 3, spanRows: 3, nonOverlapRows: 3} //  B
      ]);
    });

    it('должен вычислять перекрытие, когда события пересекаются частично', () => {
      /* ----------------
        |              |
        |      A       |  неперекрытые строки A
        |--------------|
        |              |
        |     B A      |  B перекрывает A частично
        |              |
        |--------------|
        |      A       |  эта строка A не перекрыта, но мы её игнорируем
        ----------------*/
      const events = [
        event('00:00:00', '01:00:00'), // A
        event('00:25:00', '00:55:00') //  B
      ];

      const itemsPos = positionGridItemsByTime(
        date,
        {events},
        {rowDurationMs: 10 * MINUTE, groupingThresholdMs: 10 * MINUTE}
      );

      expect(itemsPos).toMatchObject([
        {row: 0, spanRows: 6, nonOverlapRows: 2}, // A
        {row: 2, spanRows: 3, nonOverlapRows: 3} //  B
      ]);
    });

    it('должен вычислять перекрытие, когда нижележащая группа состоит из неск элементов', () => {
      /* ---------------
        |             |
        |      A      |  неперекрытые строки A
        |             |
        |-------------|
        |      |      |
        | B  A | C  A |  B и C перекрывают A
        |      |      |
        ---------------*/
      const events = [
        event('00:00:00', '01:00:00'), // A
        event('00:30:00', '01:00:00'), // B
        event('00:30:00', '01:00:00') //  С
      ];

      const itemsPos = positionGridItemsByTime(
        date,
        {events},
        {rowDurationMs: 10 * MINUTE, groupingThresholdMs: 10 * MINUTE}
      );

      expect(itemsPos).toMatchObject([
        {row: 0, spanRows: 6, nonOverlapRows: 3}, // A
        {row: 3, spanRows: 3, nonOverlapRows: 3}, // B
        {row: 3, spanRows: 3, nonOverlapRows: 3} //  C
      ]);
    });

    it('должен вычислять перекрытие, когда элементы нижележащей отличаются по высоте', () => {
      /* ------------------
        |                |
        |        A       |  неперекрытые строки A
        |________________|
        |        |  C A  |  B и C перекрывают A
        |  B  A  |_______|  
        |        |   A   |  C занимает меньше строк чем B
        ------------------*/
      const events = [
        event('00:00:00', '01:00:00'), // A
        event('00:30:00', '01:00:00'), // B
        event('00:30:00', '00:50:00') //  С
      ];

      const itemsPos = positionGridItemsByTime(
        date,
        {events},
        {rowDurationMs: 10 * MINUTE, groupingThresholdMs: 10 * MINUTE}
      );

      expect(itemsPos).toMatchObject([
        {row: 0, spanRows: 6, nonOverlapRows: 3}, // A
        {row: 3, spanRows: 3, nonOverlapRows: 3}, // B
        {row: 3, spanRows: 2, nonOverlapRows: 2} //  C
      ]);
    });

    it('должен вычислять перекрытие, когда элементы нижележащей группы начинаются в разное время', () => {
      /* ----------------
        |              |
        |      A       |  неперекрытые строки A
        |______        |
        |      |_______|
        | B  A |       |  B и C перекрывают A
        |      | C  A  |  C начинается позже B
        ----------------*/
      const events = [
        event('00:00:00', '01:00:00'), // A
        event('00:30:00', '01:00:00'), // B
        event('00:40:00', '01:00:00') //  С
      ];

      const itemsPos = positionGridItemsByTime(
        date,
        {events},
        {rowDurationMs: 10 * MINUTE, groupingThresholdMs: 20 * MINUTE}
      );

      expect(itemsPos).toMatchObject([
        {row: 0, spanRows: 6, nonOverlapRows: 3}, // A
        {row: 3, spanRows: 3, nonOverlapRows: 3}, // B
        {row: 4, spanRows: 2, nonOverlapRows: 2} //  C
      ]);
    });

    it('должен вычислять перекрытие, когда группы имеют разное количество колонок', () => {
      /* ---------------
        |             |_____________
        |     A       |             |
        |_______      |      B      |
        |       |_____|_____        |
        |  C A  |     .     |_______|
        |       | D A . D B |  E B  |
        -----------------------------*/
      const events = [
        event('00:00:00', '01:00:00'), // A
        event('00:10:00', '01:00:00'), // B
        event('00:30:00', '01:00:00'), // С
        event('00:40:00', '01:00:00'), // D
        event('00:50:00', '01:00:00') //  E
      ];

      const itemsPos = positionGridItemsByTime(
        date,
        {events},
        {
          rowDurationMs: 10 * MINUTE,
          minItemDurationMs: 1 * MINUTE,
          groupingThresholdMs: 25 * MINUTE
        }
      );

      expect(itemsPos).toMatchObject([
        {row: 0, spanRows: 6, nonOverlapRows: 3}, // A
        {row: 1, spanRows: 5, nonOverlapRows: 3}, // B
        {row: 3, spanRows: 3, nonOverlapRows: 3}, // C
        {row: 4, spanRows: 2, nonOverlapRows: 2}, // D
        {row: 5, spanRows: 1, nonOverlapRows: 1} //  E
      ]);
    });

    it('должен вычислять перекрытие, когда перекрываются не все члены группы', () => {
      /* -----------------
        |       |       |
        |   A   |   B   |  B состоит в группе, которую перекрывает дочерняя группа с C,
        |       |_______|  но сам B не перекрывается элементами дочерней группы
        |_______|_______|   
        |       .       |
        |      C A      |
        |       .       |
        |_______._______|
      */
      const events = [
        event('00:00:00', '02:00:00'), // A
        event('00:00:00', '00:45:00'), // B
        event('01:00:00', '02:00:00') //  C
      ];

      const itemsPos = positionGridItemsByTime(
        date,
        {events},
        {rowDurationMs: 15 * MINUTE, groupingThresholdMs: 15 * MINUTE}
      );

      expect(itemsPos).toMatchObject([
        {row: 0, spanRows: 8, nonOverlapRows: 4}, // A
        {row: 0, spanRows: 3, nonOverlapRows: 3}, // B
        {row: 4, spanRows: 4, nonOverlapRows: 4} //  C
      ]);
    });
  });
});
