import {convertTimeToMinutes, convertMinutesToTime, convertPeriodsToIntervals, convertIntervalsToPeriods} from './date'

import moment from 'moment'

const TIME_EXAMPLES = {
  '10:30': 630,
  '01:10': 70,
  '23:59': 1439,
  '00:00': 0
}

const DAYS_INTERVALS = [
  {
    date: '2019-12-30',
    from: 70,
    to: 100
  },
  {
    date: '2019-12-31',
    from: 500,
    to: 1000
  },
  {
    date: '2020-1-1',
    from: 500,
    to: 1000
  },
  {
    date: '2020-1-2',
    from: 500,
    to: 1000
  },
  {
    date: '2020-1-3',
    from: 50,
    to: 100
  },
  {
    date: '2020-1-4',
    from: 500,
    to: 100
  },
  {
    date: '2020-1-5',
    from: 50,
    to: 100
  },
  {
    date: '2020-1-5',
    from: 5000,
    to: 10000
  },
  {
    date: '2020-1-5',
    from: 7000,
    to: 17000
  },
  {
    date: '2020-1-6',
    from: 5000,
    to: 10000
  }
]

const DAYS_RANGES = [
  {
    dateFrom: new Date('2019-12-30'),
    dateTo: new Date('2019-12-30'),
    timeFrom: 70,
    timeTo: 100
  },
  {
    dateFrom: new Date('2020-1-30'),
    dateTo: new Date('2020-2-2'),
    timeFrom: 50,
    timeTo: 100
  },
  {
    dateFrom: new Date('2019-12-28'),
    dateTo: new Date('2019-12-30'),
    timeFrom: 50,
    timeTo: 100
  }
]

describe('date', () => {
  describe('call #convertIntervalsToPeriods()', () => {
    it('should return correct minutes', () => {
      Object.entries(TIME_EXAMPLES).forEach(([time, minute]) => {
        expect(convertTimeToMinutes(time)).toBe(minute)
      })
    })

    it('should return 0 minutes', () => {
      expect(convertTimeToMinutes('0:0')).toBe(0)
    })
  })

  describe('call #convertMinutesToTime()', () => {
    it('should return correct minutes', () => {
      Object.entries(TIME_EXAMPLES).forEach(([time, minute]) => {
        expect(convertMinutesToTime(minute)).toBe(time)
      })
    })
  })

  describe('call #convertIntervalsToPeriods()', () => {
    it('empty intervals', () => {
      expect(convertIntervalsToPeriods([])).toEqual([])
    })
    it('one interval', () => {
      const dataSet = DAYS_INTERVALS.slice(1, 2)

      expect(convertIntervalsToPeriods(dataSet)).toEqual([
        {
          dateFrom: moment(dataSet[0].date).toDate(),
          dateTo: moment(dataSet[0].date).toDate(),
          timeFrom: dataSet[0].from,
          timeTo: dataSet[0].to
        }
      ])
    })
    it('not-time-synced-intervals', () => {
      const dataSet = DAYS_INTERVALS.slice(0, 2)

      expect(convertIntervalsToPeriods(dataSet)).toEqual([
        {
          dateFrom: moment(dataSet[0].date).toDate(),
          dateTo: moment(dataSet[0].date).toDate(),
          timeFrom: dataSet[0].from,
          timeTo: dataSet[0].to
        },
        {
          dateFrom: moment(dataSet[1].date).toDate(),
          dateTo: moment(dataSet[1].date).toDate(),
          timeFrom: dataSet[1].from,
          timeTo: dataSet[1].to
        }
      ])
    })
    it('time-synced-intervals', () => {
      const dataSet = DAYS_INTERVALS.slice(2, 4)

      expect(convertIntervalsToPeriods(dataSet)).toEqual([
        {
          dateFrom: moment(dataSet[0].date).toDate(),
          dateTo: moment(dataSet[dataSet.length - 1].date).toDate(),
          timeFrom: dataSet[0].from,
          timeTo: dataSet[0].to
        }
      ])
    })
    it('31 december & 1 january', () => {
      const dataSet = DAYS_INTERVALS.slice(1, 3)

      expect(convertIntervalsToPeriods(dataSet)).toEqual([
        {
          dateFrom: moment(dataSet[0].date).toDate(),
          dateTo: moment(dataSet[dataSet.length - 1].date).toDate(),
          timeFrom: dataSet[0].from,
          timeTo: dataSet[0].to
        }
      ])
    })

    it('many different intervals', () => {
      const dataSet = DAYS_INTERVALS.slice(0, 7)

      expect(convertIntervalsToPeriods(dataSet)).toEqual([
        {
          dateFrom: moment(dataSet[0].date).toDate(),
          dateTo: moment(dataSet[0].date).toDate(),
          timeFrom: dataSet[0].from,
          timeTo: dataSet[0].to
        },
        {
          dateFrom: moment(dataSet[1].date).toDate(),
          dateTo: moment(dataSet[3].date).toDate(),
          timeFrom: dataSet[1].from,
          timeTo: dataSet[1].to
        },
        {
          dateFrom: moment(dataSet[4].date).toDate(),
          dateTo: moment(dataSet[4].date).toDate(),
          timeFrom: dataSet[4].from,
          timeTo: dataSet[4].to
        },
        {
          dateFrom: moment(dataSet[5].date).toDate(),
          dateTo: moment(dataSet[5].date).toDate(),
          timeFrom: dataSet[5].from,
          timeTo: dataSet[5].to
        },
        {
          dateFrom: moment(dataSet[6].date).toDate(),
          dateTo: moment(dataSet[6].date).toDate(),
          timeFrom: dataSet[6].from,
          timeTo: dataSet[6].to
        }
      ])
    })
    it('two intervals in one day', () => {
      const dataSet = DAYS_INTERVALS.slice(7)

      expect(convertIntervalsToPeriods(dataSet)).toEqual([
        {
          dateFrom: moment(dataSet[0].date).toDate(),
          dateTo: moment(dataSet[2].date).toDate(),
          timeFrom: dataSet[0].from,
          timeTo: dataSet[0].to
        },
        {
          dateFrom: moment(dataSet[1].date).toDate(),
          dateTo: moment(dataSet[1].date).toDate(),
          timeFrom: dataSet[1].from,
          timeTo: dataSet[1].to
        }
      ])
    })
  })
  describe('call #convertPeriodsToIntervals()', () => {
    it('one interval', () => {
      const dataSet = DAYS_RANGES[0]

      expect(convertPeriodsToIntervals(dataSet)).toEqual([
        {
          date: '2019-12-30',
          from: dataSet.timeFrom,
          to: dataSet.timeTo
        }
      ])
    })
    it('range between months', () => {
      const dataSet = DAYS_RANGES[1]

      expect(convertPeriodsToIntervals(dataSet)).toEqual([
        {
          date: '2020-01-30',
          from: dataSet.timeFrom,
          to: dataSet.timeTo
        },
        {
          date: '2020-01-31',
          from: dataSet.timeFrom,
          to: dataSet.timeTo
        },
        {
          date: '2020-02-01',
          from: dataSet.timeFrom,
          to: dataSet.timeTo
        },
        {
          date: '2020-02-02',
          from: dataSet.timeFrom,
          to: dataSet.timeTo
        }
      ])
    })
  })
  it('range to 30 december (outstanding case with Date)', () => {
    const dataSet = DAYS_RANGES[2]

    expect(convertPeriodsToIntervals(dataSet)).toEqual([
      {
        date: '2019-12-28',
        from: dataSet.timeFrom,
        to: dataSet.timeTo
      },
      {
        date: '2019-12-29',
        from: dataSet.timeFrom,
        to: dataSet.timeTo
      },
      {
        date: '2019-12-30',
        from: dataSet.timeFrom,
        to: dataSet.timeTo
      }
    ])
  })
})
