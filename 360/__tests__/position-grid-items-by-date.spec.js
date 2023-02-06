import moment from 'moment';

import EventRecord from 'features/events/EventRecord';
import TodoItemRecord from 'features/todo/TodoItemRecord';

import GridItemPosition from '../GridItemPosition';
import TodoGroup from '../TodoGroup';
import MoreGroup from '../MoreGroup';
import positionGridItemsByDate from '../position-grid-items-by-date';

const event = (start, end, props) => new EventRecord({start, end, ...props});
const todo = dueDate => new TodoItemRecord({dueDate});
const todoGroup = (dueDate, props) => new TodoGroup({dueDate: Number(moment(dueDate)), ...props});
const more = (date, props) => new MoreGroup({date: Number(moment(date)), ...props});

describe('position-grid-items-by-date', () => {
  it('должен возвращать массив позиций', () => {
    const start = moment('2000-01-01');
    const end = moment('2000-01-08');
    const events = [event('2000-01-01', '2000-01-02')];

    const itemsPos = positionGridItemsByDate(start, end, {events});

    expect(itemsPos).toHaveLength(1);
    expect(itemsPos[0]).toBeInstanceOf(GridItemPosition);
  });

  it('должен возвращать пустой массив позиций, когда входящий массив событий пуст', () => {
    const start = moment('2000-01-01');
    const end = moment('2000-01-08');
    const events = [];

    const itemsPos = positionGridItemsByDate(start, end, {events});

    expect(itemsPos).toEqual([]);
  });

  it('должен группировать дела по дате выполнения', () => {
    const start = moment('2000-01-01');
    const end = moment('2000-01-08');
    const todos = [
      todo('2000-01-01'),
      todo('2000-01-02'),
      todo('2000-01-03'),
      todo('2000-01-02T01:00:00'),
      todo('2000-01-03T01:00:00'),
      todo('2000-01-03T02:00:00')
    ];

    const itemsPos = positionGridItemsByDate(start, end, {todos});

    expect(itemsPos).toHaveLength(3);
    expect(itemsPos).toMatchObject([
      {item: todoGroup('2000-01-01', {count: 1})},
      {item: todoGroup('2000-01-02', {count: 2})},
      {item: todoGroup('2000-01-03', {count: 3})}
    ]);
  });

  describe('фильтрация по дате', () => {
    it('должен отфильтровывать события, находящиеся целиком вне периода', () => {
      const start = moment('2000-01-10');
      const end = moment('2000-01-17');
      const events = [
        event('2000-01-07', '2000-01-10'), // A. целиком вне периода (раньше)
        event('2000-01-07', '2000-01-13'), // B. частично внутри периода
        event('2000-01-13', '2000-01-15'), // C. целиком внутри периода
        event('2000-01-15', '2000-01-20'), // D. частично внутри периода
        event('2000-01-17', '2000-01-20') //  E. целиком вне периода (позже)
      ];

      const itemsPos = positionGridItemsByDate(start, end, {events});

      expect(itemsPos).toHaveLength(3);
      expect(itemsPos).toMatchObject([
        {item: events[1]}, // B
        {item: events[2]}, // C
        {item: events[3]} //  D
      ]);
    });

    it('должен отфильтровывать события, время начала которых позже времени конца', () => {
      const start = moment('2000-01-01');
      const end = moment('2000-01-08');
      const events = [
        event('2000-01-02', '2000-01-01'), // A
        event('2000-01-01', '2000-01-02'), // B
        event('2000-01-02', '2000-01-02') //  C
      ];

      const itemsPos = positionGridItemsByDate(start, end, {events});

      expect(itemsPos).toHaveLength(2);
      expect(itemsPos).toMatchObject([
        {item: events[1]}, // B
        {item: events[2]} //  C
      ]);
    });

    it('должен отфильтровывать дела, находящиеся целиком вне периода', () => {
      const start = moment('2000-01-10');
      const end = moment('2000-01-17');
      const todos = [
        todo('2000-01-07'), // A. вне периода (раньше)
        todo('2000-01-13'), // B. внутри периода
        todo('2000-01-17') //  C. вне периода (позже)
      ];

      const itemsPos = positionGridItemsByDate(start, end, {todos});

      expect(itemsPos).toHaveLength(1);
      expect(itemsPos).toMatchObject([
        {item: todoGroup('2000-01-13', {count: 1})} // B
      ]);
    });

    it('не должен отфильтровывать события нулевой длительности на границе суток', () => {
      const start = moment('2000-01-01');
      const end = moment('2000-01-02');
      const events = [
        event('2000-01-01T00:00:00', '2000-01-01T00:00:00'), // A.
        event('2000-01-02T00:00:00', '2000-01-02T00:00:00') //  B. следующие сутки
      ];

      const itemsPos = positionGridItemsByDate(start, end, {events});

      expect(itemsPos).toHaveLength(1);
      expect(itemsPos).toMatchObject([
        {item: events[0]} // A
      ]);
    });
  });

  describe('сортировка', () => {
    it('должен сортировать элементы по типу', () => {
      const start = moment('2000-01-01');
      const end = moment('2000-01-08');
      const events = [
        event('2000-01-03', '2000-01-04'), // B
        event('2000-01-01', '2000-01-02') //  A
      ];
      const todos = [
        todo('2000-01-04'), // Z
        todo('2000-01-01'), // X
        todo('2000-01-02') //  Y
      ];

      const itemsPos = positionGridItemsByDate(start, end, {events, todos});

      expect(itemsPos).toMatchObject([
        {item: todoGroup('2000-01-01', {count: 1}), column: 0}, // X
        {item: todoGroup('2000-01-02', {count: 1}), column: 1}, // Y
        {item: todoGroup('2000-01-04', {count: 1}), column: 3}, // Z

        {item: events[1], column: 0}, // A
        {item: events[0], column: 2} //  B
      ]);
    });

    it('должен сортировать события по номеру первой колонки', () => {
      const start = moment('2000-01-01');
      const end = moment('2000-01-08');
      const events = [
        event('2000-01-02', '2000-01-03'), // B
        event('2000-01-03', '2000-01-04'), // C
        event('2000-01-01', '2000-01-02') //  A
      ];

      const itemsPos = positionGridItemsByDate(start, end, {events});

      expect(itemsPos).toMatchObject([
        {item: events[2], column: 0}, // A
        {item: events[0], column: 1}, // B
        {item: events[1], column: 2} //  C
      ]);
    });

    it('должен сортировать дела по номеру первой колонки', () => {
      const start = moment('2000-01-01');
      const end = moment('2000-01-08');
      const todos = [
        todo('2000-01-02'), // B
        todo('2000-01-03'), // C
        todo('2000-01-01') //  A
      ];

      const itemsPos = positionGridItemsByDate(start, end, {todos});

      expect(itemsPos).toMatchObject([
        {item: todoGroup('2000-01-01', {count: 1}), column: 0}, // A
        {item: todoGroup('2000-01-02', {count: 1}), column: 1}, // B
        {item: todoGroup('2000-01-03', {count: 1}), column: 2} //  C
      ]);
    });

    it('должен сортировать события по колличеству занимаемых ячеек', () => {
      const start = moment('2000-01-01');
      const end = moment('2000-01-08');
      const events = [
        event('2000-01-01T01:00:00', '2000-01-02T01:00:00'), // B
        event('2000-01-01', '2000-01-03'), // A
        event('2000-01-01', '2000-01-02') //  C
      ];

      const itemsPos = positionGridItemsByDate(start, end, {events});

      // события B и C имеют одинаковую длительность, но B занимает две ячейки, а С - одну.
      // поэтому B должен стоять выше C.
      expect(itemsPos).toMatchObject([
        {item: events[1], column: 0, row: 0, spanColumns: 2}, // A
        {item: events[0], column: 0, row: 1, spanColumns: 2}, // B
        {item: events[2], column: 0, row: 2, spanColumns: 1} //  C
      ]);
    });

    it('должен сортировать события по времени начала', () => {
      const start = moment('2000-01-01');
      const end = moment('2000-01-08');
      const events = [
        event('2000-01-01T01:00:00', '2000-01-01T02:00:00'), // B
        event('2000-01-01T02:00:00', '2000-01-01T04:00:00'), // C
        event('2000-01-01T00:00:00', '2000-01-02T00:00:00') //  A
      ];

      const itemsPos = positionGridItemsByDate(start, end, {events});

      expect(itemsPos).toMatchObject([
        {item: events[2], column: 0, row: 0}, // A
        {item: events[0], column: 0, row: 1}, // B
        {item: events[1], column: 0, row: 2} //  C
      ]);
    });

    it('должен сортировать события по длительности', () => {
      const start = moment('2000-01-01');
      const end = moment('2000-01-08');
      const events = [
        event('2000-01-01T00:00:00', '2000-01-01T02:00:00'), // B
        event('2000-01-01T00:00:00', '2000-01-01T01:00:00'), // C
        event('2000-01-01T00:00:00', '2000-01-02T00:00:00') //  A
      ];

      const itemsPos = positionGridItemsByDate(start, end, {events});

      expect(itemsPos).toMatchObject([
        {item: events[2], column: 0, row: 0}, // A
        {item: events[0], column: 0, row: 1}, // B
        {item: events[1], column: 0, row: 2} //  C
      ]);
    });

    it('должен сортировать события по id', () => {
      const start = moment('2000-01-01');
      const end = moment('2000-01-08');
      const events = [
        event('2000-01-01', '2000-01-02', {id: 2}), // C
        event('2000-01-01', '2000-01-02', {id: 0}), // A
        event('2000-01-01', '2000-01-02', {id: 1}) //  B
      ];

      const itemsPos = positionGridItemsByDate(start, end, {events});

      expect(itemsPos).toMatchObject([
        {item: events[1], column: 0, row: 0}, // A
        {item: events[2], column: 0, row: 1}, // B
        {item: events[0], column: 0, row: 2} //  C
      ]);
    });

    it('должен сортировать событие в конец списка, когда его id это строка', () => {
      const start = moment('2000-01-01');
      const end = moment('2000-01-08');
      const events = [
        event('2000-01-01', '2000-01-02', {id: 3}), // C
        event('2000-01-01', '2000-01-02', {id: 1}), // A
        event('2000-01-01', '2000-01-02', {id: 'draft'}), // X
        event('2000-01-01', '2000-01-02', {id: 2}) //  B
      ];

      const itemsPos = positionGridItemsByDate(start, end, {events});

      expect(itemsPos).toMatchObject([
        {item: events[1], column: 0, row: 0}, // A
        {item: events[3], column: 0, row: 1}, // B
        {item: events[0], column: 0, row: 2}, // C
        {item: events[2], column: 0, row: 3} //  X
      ]);
    });

    it('должен сортировать событие в конец списка, когда его id равен null', () => {
      const start = moment('2000-01-01');
      const end = moment('2000-01-08');
      const events = [
        event('2000-01-01', '2000-01-02', {id: 2}), // B
        event('2000-01-01', '2000-01-02', {id: null}), // X
        event('2000-01-01', '2000-01-02', {id: 1}), //  A
        event('2000-01-01', '2000-01-02', {id: 3}) //  C
      ];

      const itemsPos = positionGridItemsByDate(start, end, {events});

      expect(itemsPos).toMatchObject([
        {item: events[2], column: 0, row: 0}, // A
        {item: events[0], column: 0, row: 1}, // B
        {item: events[3], column: 0, row: 2}, // C
        {item: events[1], column: 0, row: 3} //  X
      ]);
    });

    it('должен сортировать события по uuid', () => {
      const start = moment('2000-01-01');
      const end = moment('2000-01-08');
      const events = [
        event('2000-01-01', '2000-01-02', {id: 0, layerId: 0, instanceStartTs: 1}), // B
        event('2000-01-01', '2000-01-02', {id: 0, layerId: 1, instanceStartTs: 0}), // C
        event('2000-01-01', '2000-01-02', {id: 0, layerId: 0, instanceStartTs: 0}) //  A
      ];

      const itemsPos = positionGridItemsByDate(start, end, {events});

      expect(itemsPos).toMatchObject([
        {item: events[2], column: 0, row: 0}, // A
        {item: events[0], column: 0, row: 1}, // B
        {item: events[1], column: 0, row: 2} //  C
      ]);
    });
  });

  describe('вычисление стартовой колонки (column)', () => {
    it('должен вычислять колонку для события, когда его начало совпадает с началом дня', () => {
      const start = moment('2000-01-01');
      const end = moment('2000-01-08');
      const events = [event('2000-01-02', '2000-01-03')];

      const itemsPos = positionGridItemsByDate(start, end, {events});

      expect(itemsPos[0]).toMatchObject({column: 1});
    });

    it('должен вычислять колонку для события, когда его начало НЕ совпадает с началом дня', () => {
      const start = moment('2000-01-01');
      const end = moment('2000-01-08');
      const events = [event('2000-01-02T00:01:00', '2000-01-03')];

      const itemsPos = positionGridItemsByDate(start, end, {events});

      expect(itemsPos[0]).toMatchObject({column: 1});
    });

    it('должен вычислять колонку для события, когда оно начинается раньше периода', () => {
      const start = moment('2000-01-02');
      const end = moment('2000-01-09');
      const events = [event('2000-01-01', '2000-01-03')];

      const itemsPos = positionGridItemsByDate(start, end, {events});

      expect(itemsPos[0]).toMatchObject({column: 0});
    });

    it('должен вычислять колонку для группы дел', () => {
      const start = moment('2000-01-01');
      const end = moment('2000-01-08');
      const todos = [todo('2000-01-07')];

      const itemsPos = positionGridItemsByDate(start, end, {todos});

      expect(itemsPos[0]).toMatchObject({column: 6});
    });
  });

  describe('вычисление колличества ячеек в ширину (spanColumns)', () => {
    it('должен вычислять ширину события, когда его конец совпадает с началом дня', () => {
      const start = moment('2000-01-01');
      const end = moment('2000-01-08');
      const events = [event('2000-01-01', '2000-01-02')];

      const itemsPos = positionGridItemsByDate(start, end, {events});

      expect(itemsPos[0]).toMatchObject({spanColumns: 1});
    });

    it('должен вычислять ширину события, когда его конец НЕ совпадает с началом дня', () => {
      const start = moment('2000-01-01');
      const end = moment('2000-01-08');
      const events = [event('2000-01-01', '2000-01-02T00:00:01')];

      const itemsPos = positionGridItemsByDate(start, end, {events});

      expect(itemsPos[0]).toMatchObject({spanColumns: 2});
    });

    it('должен вычислять ширину события, когда оно начинается раньше периода', () => {
      const start = moment('2000-01-10');
      const end = moment('2000-01-17');
      const events = [event('2000-01-09', '2000-01-11')];

      const itemsPos = positionGridItemsByDate(start, end, {events});

      expect(itemsPos[0]).toMatchObject({spanColumns: 1});
    });

    it('должен вычислять ширину события, когда оно заканчивается позже периода', () => {
      const start = moment('2000-01-01');
      const end = moment('2000-01-08');
      const events = [event('2000-01-07', '2000-01-09')];

      const itemsPos = positionGridItemsByDate(start, end, {events});

      expect(itemsPos[0]).toMatchObject({spanColumns: 1});
    });

    it('должен располагать группу дел в одной ячейке', () => {
      const start = moment('2000-01-01');
      const end = moment('2000-01-08');
      const todos = [todo('2000-01-01'), todo('2000-01-01')];

      const itemsPos = positionGridItemsByDate(start, end, {todos});

      expect(itemsPos).toMatchObject([{item: todoGroup('2000-01-01', {count: 2}), spanColumns: 1}]);
    });

    it('должен располагать группу скрытых элементов в одной ячейке', () => {
      const start = moment('2000-01-01');
      const end = moment('2000-01-08');
      const events = [event('2000-01-01', '2000-01-02'), event('2000-01-01', '2000-01-02')];

      const itemsPos = positionGridItemsByDate(start, end, {events}, {rowsLimit: 1});

      expect(itemsPos).toMatchObject([{item: more('2000-01-01', {count: 2}), spanColumns: 1}]);
    });

    it('должен выставлять минимальную ширину событию нулевой длительности', () => {
      const start = moment('2000-01-01');
      const end = moment('2000-01-02');
      const events = [event('2000-01-01T00:00:00', '2000-01-01T00:00:00')];

      const itemsPos = positionGridItemsByDate(start, end, {events});

      expect(itemsPos).toMatchObject([{spanColumns: 1}]);
    });
  });

  describe('вычисление номера строки (row)', () => {
    it('должен вычислять строку, когда несколько событий начинаются в один день', () => {
      const start = moment('2000-01-01');
      const end = moment('2000-01-08');
      const events = [
        event('2000-01-01', '2000-01-02'),
        event('2000-01-01', '2000-01-03'),
        event('2000-01-01T01:00:00', '2000-01-01T02:00:00')
      ];

      const itemsPos = positionGridItemsByDate(start, end, {events});

      expect(itemsPos).toMatchObject([
        {column: 0, row: 0},
        {column: 0, row: 1},
        {column: 0, row: 2}
      ]);
    });

    it('должен размещать группу дел на первой строке', () => {
      const start = moment('2000-01-01');
      const end = moment('2000-01-08');
      const events = [event('2000-01-01', '2000-01-02')];
      const todos = [todo('2000-01-01')];

      const itemsPos = positionGridItemsByDate(start, end, {events, todos});

      expect(itemsPos).toMatchObject([
        {item: todoGroup('2000-01-01', {count: 1}), column: 0, row: 0},
        {item: events[0], column: 0, row: 1}
      ]);
    });

    it('должен занимать первую строку во всех колонках под дела когда они есть', () => {
      const start = moment('2000-01-01');
      const end = moment('2000-01-08');
      const events = [
        event('2000-01-01', '2000-01-02'), // A
        event('2000-01-03', '2000-01-04'), // B
        event('2000-01-07', '2000-01-08') //  C
      ];
      const todos = [todo('2000-01-03')]; // X

      const itemsPos = positionGridItemsByDate(start, end, {events, todos});

      // только B находится в одной колонке с делом,
      // но A и C также получают смещение на вторую строку
      expect(itemsPos).toMatchObject([
        {item: todoGroup('2000-01-03', {count: 1}), column: 2, row: 0}, // X
        {item: events[0], column: 0, row: 1}, // A
        {item: events[1], column: 2, row: 1}, // B
        {item: events[2], column: 6, row: 1} //  C
      ]);
    });

    it('должен вычислять строку, когда событие предыдущего дня налезает на текущий день', () => {
      /* -----------------
        | A | A | C | C |
        |   | B | B |   |
        -----------------*/
      const start = moment('2000-01-01');
      const end = moment('2000-01-08');
      const events = [
        event('2000-01-01', '2000-01-03'), // A
        event('2000-01-02', '2000-01-04'), // B
        event('2000-01-03', '2000-01-05') //  C
      ];

      const itemsPos = positionGridItemsByDate(start, end, {events});

      expect(itemsPos).toMatchObject([
        {column: 0, row: 0, spanColumns: 2}, // A
        {column: 1, row: 1, spanColumns: 2}, // B
        {column: 2, row: 0, spanColumns: 2} //  C
      ]);
    });

    it('должен располагать предпросмотр события на первой строчке поверх других событий', () => {
      const start = moment('2000-01-01');
      const end = moment('2000-01-08');
      const events = [
        event('2000-01-01', '2000-01-03'), // A
        event('2000-01-01', '2000-01-02'), // B
        event('2000-01-02', '2000-01-03'), // C
        event('2000-01-01', '2000-01-03', {isPreview: true}) // X
      ];

      const itemsPos = positionGridItemsByDate(start, end, {events});

      expect(itemsPos).toMatchObject([
        {column: 0, row: 0, spanColumns: 2}, // A
        {column: 0, row: 0, spanColumns: 2}, // X
        {column: 0, row: 1, spanColumns: 1}, // B
        {column: 1, row: 1, spanColumns: 1} //  C
      ]);
    });

    it('должен располагать предпросмотр события на первой строчке поверх дел', () => {
      const start = moment('2000-01-01');
      const end = moment('2000-01-08');
      const events = [event('2000-01-01', '2000-01-02', {isPreview: true})];
      const todos = [todo('2000-01-01')];

      const itemsPos = positionGridItemsByDate(start, end, {events, todos});

      expect(itemsPos).toMatchObject([
        {item: todoGroup('2000-01-01', {count: 1}), column: 0, row: 0, spanColumns: 1},
        {item: events[0], column: 0, row: 0, spanColumns: 1}
      ]);
    });

    it('должен располагать предпросмотр существующего события на той же строке', () => {
      const start = moment('2000-01-01');
      const end = moment('2000-01-08');
      const events = [
        event('2000-01-01', '2000-01-03', {id: 1, layerId: 1, instanceStartTs: 1}), // A
        event('2000-01-01', '2000-01-02', {id: 2, layerId: 2, instanceStartTs: 2}), // B
        event('2000-01-02', '2000-01-03', {id: 3, layerId: 3, instanceStartTs: 3}), // C
        event('2000-01-01', '2000-01-03', {isPreview: true, previewOriginalUuid: '2:2:2:null'}) // X
      ];

      const itemsPos = positionGridItemsByDate(start, end, {events});

      expect(itemsPos).toMatchObject([
        {item: events[0], column: 0, spanColumns: 2, row: 0}, // A
        {item: events[3], column: 0, spanColumns: 2, row: 1}, // X. на той же строке что и B
        {item: events[1], column: 0, spanColumns: 1, row: 1}, // B
        {item: events[2], column: 1, spanColumns: 1, row: 1} //  C
      ]);
    });

    it('должен располагать предпросмотр экземпляра серии на той же строке', () => {
      const start = moment('2000-01-01');
      const end = moment('2000-01-08');
      const events = [
        event('2000-01-01', '2000-01-03', {id: 1, layerId: 1, instanceStartTs: 1}), // A
        event('2000-01-01', '2000-01-02', {id: 1, layerId: 1, instanceStartTs: 2}), // B
        event('2000-01-02', '2000-01-03', {id: 1, layerId: 1, instanceStartTs: 3}), // C
        event('2000-01-01', '2000-01-03', {isPreview: true, previewOriginalUuid: '1:1:2:null'}) // X
      ];

      const itemsPos = positionGridItemsByDate(start, end, {events});

      expect(itemsPos).toMatchObject([
        {item: events[0], column: 0, spanColumns: 2, row: 0}, // A
        {item: events[3], column: 0, spanColumns: 2, row: 1}, // X. на той же строке что и B
        {item: events[1], column: 0, spanColumns: 1, row: 1}, // B
        {item: events[2], column: 1, spanColumns: 1, row: 1} //  C
      ]);
    });

    it('должен располагать предпросмотр на первой строке, когда сущ. событие не найдено', () => {
      const start = moment('2000-01-01');
      const end = moment('2000-01-08');
      const events = [
        event('2000-01-01', '2000-01-03', {id: 1, layerId: 1, instanceStartTs: 1}), // A
        event('2000-01-01', '2000-01-02', {id: 2, layerId: 2, instanceStartTs: 2}), // B
        event('2000-01-02', '2000-01-03', {id: 3, layerId: 3, instanceStartTs: 3}), // C
        event('2000-01-01', '2000-01-03', {isPreview: true, previewOriginalUuid: 'x:x:x:null'}) // X
      ];

      const itemsPos = positionGridItemsByDate(start, end, {events});

      expect(itemsPos).toMatchObject([
        {item: events[0], column: 0, spanColumns: 2, row: 0}, // A
        {item: events[3], column: 0, spanColumns: 2, row: 0}, // X. на первой строке
        {item: events[1], column: 0, spanColumns: 1, row: 1}, // B
        {item: events[2], column: 1, spanColumns: 1, row: 1} //  C
      ]);
    });
  });

  describe('ограничение по колличеству строк (rowsLimit)', () => {
    it('НЕ должен скрывать события, когда высота колонки меньше ограничения', () => {
      const start = moment('2000-01-01');
      const end = moment('2000-01-08');
      const events = [event('2000-01-01', '2000-01-02')];

      const itemsPos = positionGridItemsByDate(start, end, {events}, {rowsLimit: 3});

      expect(itemsPos).toMatchObject([{item: events[0]}]);
    });

    it('НЕ должен скрывать события, когда высота колонки равна ограничению', () => {
      const start = moment('2000-01-01');
      const end = moment('2000-01-08');
      const events = [
        event('2000-01-01', '2000-01-02', {id: 0}), // A
        event('2000-01-01', '2000-01-02', {id: 1}), // B
        event('2000-01-01', '2000-01-02', {id: 2}) //  C
      ];

      const itemsPos = positionGridItemsByDate(start, end, {events}, {rowsLimit: 3});

      expect(itemsPos).toMatchObject([
        {item: events[0]}, // A
        {item: events[1]}, // B
        {item: events[2]} //  C
      ]);
    });

    it('должен скрывать события, когда высота колонки больше ограничения', () => {
      const start = moment('2000-01-01');
      const end = moment('2000-01-08');
      const events = [
        event('2000-01-01', '2000-01-02', {id: 1}),
        event('2000-01-01', '2000-01-02', {id: 2}),
        event('2000-01-01', '2000-01-02', {id: 3}), // скрыто (место под кнопку "ещё").
        event('2000-01-01', '2000-01-02', {id: 4}) //  скрыто.
      ];

      const itemsPos = positionGridItemsByDate(start, end, {events}, {rowsLimit: 3});

      expect(itemsPos).toHaveLength(3);
      expect(itemsPos).toMatchObject([
        {item: events[0], column: 0, row: 0},
        {item: events[1], column: 0, row: 1},
        {item: more('2000-01-01', {count: 2}), column: 0, row: 2}
      ]);
    });

    it('должен скрывать события, не поместившиеся в колонку по высоте', () => {
      /* ---------------------
        | A | A | E | E | E |
        |   | D | D | D | D |
        |   | B | B |   |   |
        |   |   | C | C |   |
        ---------------------*/
      const start = moment('2000-01-01');
      const end = moment('2000-01-08');
      const events = [
        event('2000-01-01', '2000-01-03'), // A
        event('2000-01-02', '2000-01-04'), // B
        event('2000-01-03', '2000-01-05'), // C
        event('2000-01-02', '2000-01-06'), // D
        event('2000-01-03', '2000-01-06') //  E
      ];

      const itemsPos = positionGridItemsByDate(start, end, {events}, {rowsLimit: 2});

      expect(itemsPos).toHaveLength(6);
      expect(itemsPos).toMatchObject([
        {item: events[0], column: 0, row: 0}, // A

        {item: more('2000-01-02', {count: 2}), column: 1, row: 1},
        {item: more('2000-01-03', {count: 3}), column: 2, row: 1},
        {item: more('2000-01-04', {count: 2}), column: 3, row: 1},
        {item: more('2000-01-05', {count: 1}), column: 4, row: 1},

        {item: events[4], column: 2, row: 0} // E
      ]);
    });

    it('должен учитывать занятость всех колонок занимаемых событием', () => {
      /* -----------------
        | A | A | B | B |
        |   | C | C |   |
        -----------------*/
      const start = moment('2000-01-01');
      const end = moment('2000-01-08');
      const events = [
        event('2000-01-01', '2000-01-03'), // A скрыто (2ой колонке нужно место под "ещё")
        event('2000-01-02', '2000-01-04'), // B
        event('2000-01-03', '2000-01-05') //  C
      ];

      const itemsPos = positionGridItemsByDate(start, end, {events}, {rowsLimit: 1});

      expect(itemsPos).toMatchObject([
        {item: more('2000-01-01', {count: 1}), column: 0, row: 0},
        {item: more('2000-01-02', {count: 2}), column: 1, row: 0},
        {item: more('2000-01-03', {count: 2}), column: 2, row: 0},
        {item: more('2000-01-04', {count: 1}), column: 3, row: 0}
      ]);
    });
  });

  describe('освобождение последней строки (clearLastRow)', () => {
    it('НЕ должен скрывать события, когда высота колонки меньше ограничения', () => {
      /* -------------
        | A | A | A |
        |   |   |   |
        -------------*/
      const start = moment('2000-01-01');
      const end = moment('2000-01-08');
      const events = [
        event('2000-01-01', '2000-01-04') // A
      ];

      const itemsPos = positionGridItemsByDate(
        start,
        end,
        {events},
        {rowsLimit: 2, clearLastRow: true}
      );

      expect(itemsPos).toMatchObject([
        {item: events[0], column: 0, row: 0} // A
      ]);
    });

    it('должен скрывать события, когда высота колонки равна ограничению', () => {
      /* -------------
        | A | A |   |
        |   | B | B |
        -------------*/
      const start = moment('2000-01-01');
      const end = moment('2000-01-08');
      const events = [
        event('2000-01-01', '2000-01-03'), // A
        event('2000-01-02', '2000-01-04') //  B
      ];

      const itemsPos = positionGridItemsByDate(
        start,
        end,
        {events},
        {rowsLimit: 2, clearLastRow: true}
      );

      expect(itemsPos).toMatchObject([
        {item: events[0], column: 0, row: 0},
        {item: more('2000-01-02', {count: 1}), column: 1, row: 1},
        {item: more('2000-01-03', {count: 1}), column: 2, row: 1}
      ]);
    });

    it('должен скрывать события, когда высота колонки больше ограничения', () => {
      /* -------------
        | A | A |   |
        |___|_B_|_B_|
        |   | C |   |
        -------------*/
      const start = moment('2000-01-01');
      const end = moment('2000-01-08');
      const events = [
        event('2000-01-01', '2000-01-03'), // A
        event('2000-01-02', '2000-01-04'), // B
        event('2000-01-02', '2000-01-03') //  C
      ];

      const itemsPos = positionGridItemsByDate(
        start,
        end,
        {events},
        {rowsLimit: 2, clearLastRow: true}
      );

      expect(itemsPos).toMatchObject([
        {item: events[0], column: 0, row: 0},
        {item: more('2000-01-02', {count: 2}), column: 1, row: 1},
        {item: more('2000-01-03', {count: 1}), column: 2, row: 1}
      ]);
    });
  });
});
