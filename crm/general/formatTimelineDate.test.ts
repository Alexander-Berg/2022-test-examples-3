import { formatTimelineDate } from 'modules/timeline/formatTimelineDate';

describe('formatTimelineDateIntl', () => {
  beforeAll(() => {
    jest.useFakeTimers('modern');
    jest.setSystemTime(new Date('Dec 27 2021 23:30:00 GMT+0000'));
  });

  afterAll(() => {
    jest.useRealTimers();
  });

  const mockFormatDate = jest.fn((date: string) =>
    new Date(date).toLocaleString('en-US', {
      month: 'long',
      weekday: 'long',
      day: 'numeric',
    }),
  );

  it('adds year when not current', () => {
    const date = new Date(Date.UTC(2020, 11, 27)).toString();
    const formatedDate = formatTimelineDate(date, mockFormatDate);
    expect(formatedDate).toEqual('Sunday, December 27 2020');
  });

  it('returns today when is today', () => {
    const date = new Date(Date.UTC(2021, 11, 27)).toString();
    const formatedDate = formatTimelineDate(date, mockFormatDate, 'Today');
    expect(formatedDate).toEqual('Today');
  });

  describe('edge', () => {
    it('today', () => {
      const date = new Date('Dec 28 2021 00:30:00 GMT+0300').toString();
      const formatedDate = formatTimelineDate(date, mockFormatDate, 'Today');
      expect(formatedDate).toEqual('Today');
    });
    it('not today', () => {
      const date = new Date('Dec 28 2021 05:00:00 GMT+0300').toString();
      const formatedDate = formatTimelineDate(date, mockFormatDate, 'Today');
      expect(formatedDate).toEqual('Tuesday, December 28');
    });
  });
});
