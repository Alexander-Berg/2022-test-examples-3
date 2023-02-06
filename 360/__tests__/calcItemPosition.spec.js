import moment from 'moment';

import calcItemPosition from '../calcItemPosition';

describe('timeline/utils/calcItemPosition', () => {
  const timelineDate = moment('2017-09-21').valueOf();

  test('должен правильно рассчитать позиции для промежутка, попадющего в таймлайн', () => {
    const item = {
      start: moment('2017-09-21T10:13:00').valueOf(),
      end: moment('2017-09-21T18:17:00').valueOf()
    };

    expect(calcItemPosition(item, timelineDate)).toEqual({
      left: '13.854166666666668%',
      right: '35.72916666666666%'
    });
  });
  test('должен правильно рассчитать позиции для промежутка большего, чем таймлайн', () => {
    const item = {
      start: moment('2010-09-21T10:00:00').valueOf(),
      end: moment('2020-09-21T18:00:00').valueOf()
    };

    expect(calcItemPosition(item, timelineDate)).toEqual({
      left: '0%',
      right: '0%'
    });
  });
  test('должен правильно рассчитать позиции для промежутка без длительности', () => {
    const item = {
      start: moment('2017-09-21T15:18:00').valueOf(),
      end: moment('2017-09-21T15:18:00').valueOf()
    };

    expect(calcItemPosition(item, timelineDate)).toEqual({
      left: '45.625%',
      right: '54.37499999999999%'
    });
  });
  test('должен правильно рассчитать позиции для промежутка слева от таймлайна', () => {
    const item = {
      start: moment('2017-09-21T00:00:00').valueOf(),
      end: moment('2017-09-21T01:00:00').valueOf()
    };

    expect(calcItemPosition(item, timelineDate)).toEqual({
      left: '0%',
      right: '100%'
    });
  });
  test('должен правильно рассчитать позиции для промежутка справа от таймлайна', () => {
    const item = {
      start: moment('2017-09-22T00:00:00').valueOf(),
      end: moment('2017-09-22T00:30:00').valueOf()
    };

    expect(calcItemPosition(item, timelineDate)).toEqual({
      left: '100%',
      right: '0%'
    });
  });
});
