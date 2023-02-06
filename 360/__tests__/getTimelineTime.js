import moment from 'moment';

import getTimelineTime from '../getTimelineTime';

describe('timeline/utils/getTimelineTime', () => {
  const timelineDate = moment('2021-08-09').valueOf();
  const timelineLeft = 716;
  const timelineWidth = 951;

  test('должен правильно рассчитать время для точки, попадающей в таймлайн', () => {
    expect(getTimelineTime(818, timelineDate, timelineLeft, timelineWidth)).toEqual(1628490600000);
  });
  test('должен правильно рассчитать время для точки, попадающей в таймлайн, если передали округление в другую сторону', () => {
    expect(getTimelineTime(818, timelineDate, timelineLeft, timelineWidth, 'ceil')).toEqual(
      1628491500000
    );
  });
  test('должен правильно рассчитать время для точки, которая левее таймлайна', () => {
    expect(getTimelineTime(500, timelineDate, timelineLeft, timelineWidth)).toEqual(1628485200000);
  });
  test('должен правильно рассчитать время для точки, которая правее таймлайна', () => {
    expect(getTimelineTime(1300, timelineDate, timelineLeft, timelineWidth)).toEqual(1628520300000);
  });
});
