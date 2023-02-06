package graph

import (
	"context"
	"fmt"
	"strconv"
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/daysoff"
	"a.yandex-team.ru/market/combinator/pkg/express"
	"a.yandex-team.ru/market/combinator/pkg/its"
	"a.yandex-team.ru/market/combinator/pkg/settings"
	"a.yandex-team.ru/market/combinator/pkg/timex"
	"a.yandex-team.ru/market/combinator/pkg/util"
)

func makeDD(a map[int]bool) daysoff.DisabledDatesMap {
	dd := make(daysoff.DisabledDatesMap)
	for k, v := range a {
		dd[k] = daysoff.DisabledDate{IsHoliday: v}
	}
	return dd
}

func CreateSimpleSchedule() []DaySchedule {
	return []DaySchedule{
		{
			DayFloat: 1.0,
			From:     "00:00:00",
			To:       "23:59:59",
		},
		{
			DayFloat: 2.0,
			From:     "00:00:00",
			To:       "23:59:59",
		},
		{
			DayFloat: 3.0,
			From:     "00:00:00",
			To:       "23:59:59",
		},
		{
			DayFloat: 4.0,
			From:     "00:00:00",
			To:       "23:59:59",
		},
		{
			DayFloat: 5.0,
			From:     "00:00:00",
			To:       "23:59:59",
		},
		{
			DayFloat: 6.0,
			From:     "00:00:00",
			To:       "23:59:59",
		},
		{
			DayFloat: 7.0,
			From:     "00:00:00",
			To:       "23:59:59",
		},
	}
}

func CreateWeekdaySchedule() []DaySchedule {
	return []DaySchedule{
		{
			DayFloat: 1.0,
			From:     "12:00:00",
			To:       "22:00:00",
		},
		{
			DayFloat: 2.0,
			From:     "12:00:00",
			To:       "22:00:00",
		},
		{
			DayFloat: 3.0,
			From:     "12:00:00",
			To:       "22:00:00",
		},
		{
			DayFloat: 4.0,
			From:     "12:00:00",
			To:       "22:00:00",
		},
		{
			DayFloat: 5.0,
			From:     "12:00:00",
			To:       "22:00:00",
		},
	}
}

func CreateScheduleRecursion() []DaySchedule {
	return []DaySchedule{
		{
			DayFloat: 7.0,
			From:     "00:00:00",
			To:       "23:59:59",
		},
	}
}

func CreateScheduleManyIntervals() []DaySchedule {
	return []DaySchedule{
		{
			DayFloat: 3.0,
			From:     "02:03:00",
			To:       "07:59:59",
		},
		{
			DayFloat: 3.0,
			From:     "11:00:00",
			To:       "19:35:00",
		},
	}
}

func CreateScheduleDeferredIntervals() []DaySchedule {
	result := []DaySchedule{}
	// Create hour slots everyday schedule
	// From 08:00 - 09:00
	// To   19:00 - 20:00
	for day := 1; day < 8; day++ {
		dayF := float64(day)
		for hour := 8; hour < 20; hour++ {
			newSC := DaySchedule{
				DayFloat: dayF,
				From:     fmt.Sprintf("%02d:00:00", hour),
				To:       fmt.Sprintf("%02d:00:00", hour+1),
			}
			result = append(result, newSC)
		}
	}
	return result
}

func CreateScheduleFromToEqual() []DaySchedule {
	return []DaySchedule{
		{
			DayFloat: 1.0,
			From:     "06:00:00",
			To:       "06:00:00",
		},
	}
}

func CreateScheduleFromToEqualLastDay() []DaySchedule {
	return []DaySchedule{
		{
			DayFloat: 1.0,
			From:     "23:59:59",
			To:       "23:59:59",
		},
	}
}

func CreateEmptySchedule() []DaySchedule {
	return []DaySchedule{}
}

func CreateFullWeekSchedule() []DaySchedule {
	return []DaySchedule{
		{
			DayFloat: 1.0,
			From:     "12:00:00",
			To:       "22:00:00",
		},
		{
			DayFloat: 2.0,
			From:     "12:00:00",
			To:       "22:00:00",
		},
		{
			DayFloat: 3.0,
			From:     "12:00:00",
			To:       "22:00:00",
		},
		{
			DayFloat: 4.0,
			From:     "12:00:00",
			To:       "22:00:00",
		},
		{
			DayFloat: 5.0,
			From:     "12:00:00",
			To:       "22:00:00",
		},
		{
			DayFloat: 6.0,
			From:     "12:00:00",
			To:       "22:00:00",
		},
		{
			DayFloat: 7.0,
			From:     "12:00:00",
			To:       "22:00:00",
		},
	}
}

type DurationData struct {
	inputDur    time.Duration
	yearDaysOff []int
	expDur      time.Duration
}

func TestCalcYearDay(t *testing.T) {
	dt := time.Date(2021, 12, 31, 11, 22, 33, 0, time.UTC)
	require.Equal(t, 365, calcYearDay(dt, 0))
	require.Equal(t, 1, calcYearDay(dt, 1))
}

func TestApplyDuration(t *testing.T) {
	fullWeekSchedules := [][]DaySchedule{
		CreateSimpleSchedule(),
		CreateEmptySchedule(),
		CreateFullWeekSchedule(),
	}
	for _, fws := range fullWeekSchedules {
		s, err := newSimpleSchedule(fws)
		require.NoError(t, err)
		// For all week schedules we have no additional duration
		durs := []DurationData{
			{
				inputDur: time.Duration(10) * time.Minute,
				expDur:   time.Duration(10) * time.Minute,
			},
			{
				inputDur: time.Duration(25) * time.Hour,
				expDur:   time.Duration(25) * time.Hour,
			},
			{
				inputDur: time.Duration(72) * time.Hour,
				expDur:   time.Duration(72) * time.Hour,
			},
			{
				inputDur: time.Duration(720) * time.Hour,
				expDur:   time.Duration(720) * time.Hour,
			},
		}
		start := time.Now()
		for _, dur := range durs {
			expTime := start.Add(dur.expDur)
			actTime := s.ApplyDuration(start, dur.inputDur, ApplyOptions{})
			timesAreEqual(t, expTime, actTime)
		}
	}
}

func TestApplyDurationWithWeekendAndDaysOff(t *testing.T) {
	start, err := time.Parse(time.RFC3339, "2021-06-04T07:04:05+09:00") // Friday
	require.NoError(t, err)

	s, err := newSimpleSchedule(CreateWeekdaySchedule())
	require.NoError(t, err)
	durs := []DurationData{
		{
			inputDur: time.Duration(10) * time.Minute,
			expDur:   time.Duration(10) * time.Minute,
		},
		{
			inputDur: time.Duration(24) * time.Hour,
			expDur:   time.Duration(24+48) * time.Hour, // 1 working days + 2 weekend days
		},
		{
			inputDur: time.Duration(72) * time.Hour,
			expDur:   time.Duration(72+48) * time.Hour, // 3 working days + 2 weekend days
		},
		{
			inputDur: time.Duration(144) * time.Hour,
			expDur:   time.Duration(144+48*2) * time.Hour, // 6 working days + 4 weekend days
		},
		{
			inputDur: time.Duration(24) * time.Hour,
			yearDaysOff: []int{
				start.Add(72 * time.Hour).YearDay(),
				start.Add(96 * time.Hour).YearDay(),
			},
			expDur: time.Duration(24+96) * time.Hour,
		},
	}
	for _, dur := range durs {
		ddMap := make(daysoff.DisabledDatesMap)
		for _, ydo := range dur.yearDaysOff {
			ddMap[ydo] = daysoff.DisabledDate{IsHoliday: true}
		}
		opt := ApplyOptions{daysOffGrouped: ddMap}
		expTime := start.Add(dur.expDur)
		actTime := s.ApplyDuration(start, dur.inputDur, opt)
		timesAreEqual(t, expTime, actTime)
	}
}

func TestJumpToBeginningNextWorkday(t *testing.T) {
	start, err := time.Parse(time.RFC3339, "2021-06-04T07:04:05+09:00") // Friday
	require.NoError(t, err)

	s, err := newSimpleSchedule(CreateWeekdaySchedule())
	require.NoError(t, err)

	type testData struct {
		daysSkip int32
		daysOff  []time.Time
		expTime  time.Time
	}

	dayDur := 24 * time.Hour
	baseDateTime := time.Date(start.Year(), start.Month(), start.Day(), 12, 0, 0, 0, start.Location())

	data := []testData{
		{
			daysSkip: 0,
			expTime:  start,
		},
		{
			daysSkip: 1,
			expTime:  baseDateTime.Add(3 * dayDur),
		},
		{
			daysSkip: 3,
			expTime:  baseDateTime.Add(5 * dayDur),
		},
		{
			daysSkip: 6,
			expTime:  baseDateTime.Add(10 * dayDur),
		},
		{
			daysSkip: 1,
			daysOff: []time.Time{
				baseDateTime.Add(3 * dayDur),
				baseDateTime.Add(4 * dayDur),
				baseDateTime.Add(5 * dayDur),
			},
			expTime: baseDateTime.Add(6 * dayDur),
		},
	}
	for _, test := range data {
		ddMap := make(daysoff.DisabledDatesMap)
		for _, do := range test.daysOff {
			ddMap[do.YearDay()] = daysoff.DisabledDate{IsHoliday: true}
		}
		opt := ApplyOptions{daysOffGrouped: ddMap}
		actTime := s.JumpToBeginningNextWorkday(start, test.daysSkip, opt)
		timesAreEqual(t, test.expTime, actTime)
	}
}

func timesAreEqual(t *testing.T, exp, act time.Time) {
	require.Equal(t, exp.Unix(), act.Unix()) // for readable error
	require.Equal(t, exp, act)               // for zones and location equality
}

func TestEmptySchedule(t *testing.T) {
	s, err := newSimpleSchedule([]DaySchedule{})
	require.NoError(t, err)
	require.NotNil(t, s)
	require.Equal(t, s.Trivial, true)
}

func TestSimpleCase(t *testing.T) {
	s, err := newSimpleSchedule(CreateSimpleSchedule())
	require.NoError(t, err)

	ao := ApplyOptions{useRightBorder: false}
	now := time.Now().Round(time.Second)
	res, err := s.ApplyNew(context.Background(), now, ao)
	require.NoError(t, err)
	timesAreEqual(t, now, res)
}

func TestSimpleCaseOnlySchedule(t *testing.T) {
	s, err := newSimpleSchedule(CreateSimpleSchedule())
	require.NoError(t, err)

	now := time.Now().Round(time.Second)
	res, _, err := s.ApplyScheduleNew(context.Background(), now, 5, false, false) // some totalDepth (less than limit)
	require.NoError(t, err)
	timesAreEqual(t, now, res)
}

func TestWeekdayCaseOnlySchedule(t *testing.T) {
	s, err := newSimpleSchedule(CreateWeekdaySchedule())
	require.NoError(t, err)

	start, err := time.Parse(time.RFC3339, "2020-06-02T22:04:05+09:00")
	require.NoError(t, err)
	expected, err := time.Parse(time.RFC3339, "2020-06-03T12:00:00+09:00")
	require.NoError(t, err)
	res, _, err := s.ApplyScheduleNew(context.Background(), start, 5, false, false) // some totalDepth (less than limit)
	require.NoError(t, err)
	timesAreEqual(t, expected, res)
}

func TestSimpleCaseWithDisabled(t *testing.T) {
	daysOffGrouped := makeDD(map[int]bool{
		util.StrDateToInt("2020-06-02"): true,
		util.StrDateToInt("2020-06-03"): true,
	})
	s, err := NewSchedule(
		CreateSimpleSchedule(),
		false,
	)
	require.NoError(t, err)

	start, err := time.Parse(time.RFC3339, "2020-06-02T15:04:05+09:00")
	require.NoError(t, err)
	expectedRes, err := time.Parse(time.RFC3339, "2020-06-04T00:00:00+09:00")
	require.NoError(t, err)

	ao := ApplyOptions{
		useRightBorder: false,
		daysOffGrouped: daysOffGrouped,
	}
	res, err := s.ApplyNew(context.Background(), start, ao)
	require.NoError(t, err)
	timesAreEqual(t, expectedRes, res)
}

func TestSimpleCaseWithDisabledLogsticDay(t *testing.T) {
	/*
	  Check disable dates usage, when we have logisticDay start setting.
	*/
	daysOffGrouped := makeDD(map[int]bool{
		util.StrDateToInt("2020-06-02"): true,
		util.StrDateToInt("2020-06-03"): true,
	})
	{
		s, err := NewSchedule(
			CreateSimpleSchedule(),
			false,
		)
		require.NoError(t, err)

		// It is 2020-06-02 logistic day
		start, err := time.Parse(time.RFC3339, "2020-06-02T11:04:05+09:00")
		require.NoError(t, err)
		expectedRes, err := time.Parse(time.RFC3339, "2020-06-04T11:00:00+09:00")
		require.NoError(t, err)

		res, err := s.ApplyNew(context.Background(), start, ApplyOptions{
			daysOffGrouped:   daysOffGrouped,
			logisticDayStart: timex.DayTime{Hour: 11, Minute: 0, Second: 0},
		})
		require.NoError(t, err)
		timesAreEqual(t, expectedRes, res)
	}
	{
		s, err := NewSchedule(
			CreateSimpleSchedule(),
			false,
		)
		require.NoError(t, err)

		// It is 2020-06-01 logistic day, so no disabled dates here
		start, err := time.Parse(time.RFC3339, "2020-06-02T10:04:05+09:00")
		require.NoError(t, err)
		expectedRes, err := time.Parse(time.RFC3339, "2020-06-02T10:04:05+09:00")
		require.NoError(t, err)

		res, err := s.ApplyNew(context.Background(), start, ApplyOptions{
			logisticDayStart: timex.DayTime{Hour: 11},
		})
		require.NoError(t, err)
		timesAreEqual(t, expectedRes, res)
	}
}

func TestWeekdayWithDisabledLogsticDay(t *testing.T) {
	/*
	  First of all we apply schedule and got to 2020-06-01 12:00:00
	  After that we skip first week weekdays because of disabled days,
	  then we skip weekend because of schedule.
	  Finally, we skip Monday and Tuesday because of disabled days.
	*/
	daysOffGrouped := makeDD(map[int]bool{
		util.StrDateToInt("2020-06-01"): false, // Monday
		util.StrDateToInt("2020-06-02"): false, // Tuesday
		util.StrDateToInt("2020-06-03"): false, // Wednesday
		util.StrDateToInt("2020-06-04"): false, // Thursday
		util.StrDateToInt("2020-06-05"): false, // Friday
		// next week
		util.StrDateToInt("2020-06-08"): false, // Monday
		util.StrDateToInt("2020-06-09"): false, // Tuesday
	})
	{
		// Schedule for weekdays only, day starts in 12:00 and ends in 22:00
		s, err := NewSchedule(
			CreateWeekdaySchedule(),
			false,
		)
		require.NoError(t, err)

		start, err := time.Parse(time.RFC3339, "2020-05-30T17:05:03+09:00") // Saturday
		require.NoError(t, err)
		expectedRes, err := time.Parse(time.RFC3339, "2020-06-10T12:00:00+09:00") // Wednesday
		require.NoError(t, err)

		res, err := s.ApplyNew(context.Background(), start, ApplyOptions{
			daysOffGrouped:   daysOffGrouped,
			logisticDayStart: timex.DayTime{Hour: 11},
		})
		require.NoError(t, err)
		timesAreEqual(t, expectedRes, res)
	}
}

func TestSimpleCaseWithLinehaulDD(t *testing.T) {
	/*
	  Check disable dates merge (case of movement with linehaul).
	*/
	linehaulDD := makeDD(map[int]bool{
		util.StrDateToInt("2020-06-03"): true,
	})
	daysOffGrouped := makeDD(map[int]bool{
		util.StrDateToInt("2020-06-02"): true,
		util.StrDateToInt("2020-06-04"): true,
	})
	{
		s, err := NewSchedule(
			CreateSimpleSchedule(),
			false,
		)
		require.NoError(t, err)

		start, err := time.Parse(time.RFC3339, "2020-06-02T11:04:05+09:00")
		require.NoError(t, err)
		expectedRes, err := time.Parse(time.RFC3339, "2020-06-05T11:00:00+09:00")
		require.NoError(t, err)

		res, err := s.ApplyNew(context.Background(), start, ApplyOptions{
			daysOffGrouped:   daysOffGrouped,
			linehaulDD:       linehaulDD,
			logisticDayStart: timex.DayTime{Hour: 11},
		})
		require.NoError(t, err)
		timesAreEqual(t, expectedRes, res)
	}
}

func TestRecursion(t *testing.T) {
	s, err := newSimpleSchedule(CreateScheduleRecursion())
	require.NoError(t, err)

	start, err := time.Parse(time.RFC3339, "2020-06-01T15:04:05+05:00")
	require.NoError(t, err)
	expectedRes, err := time.Parse(time.RFC3339, "2020-06-07T00:00:00+05:00")
	require.NoError(t, err)

	ao := ApplyOptions{
		useRightBorder: false,
	}
	res, err := s.ApplyNew(context.Background(), start, ao)
	require.NoError(t, err)
	timesAreEqual(t, expectedRes, res)
}

func TestManyIntervals(t *testing.T) {
	s, err := newSimpleSchedule(CreateScheduleManyIntervals())
	require.NoError(t, err)

	// Before first interval
	start, err := time.Parse(time.RFC3339, "2020-06-02T12:34:11+06:00")
	require.NoError(t, err)
	expectedRes, err := time.Parse(time.RFC3339, "2020-06-03T02:03:00+06:00")
	require.NoError(t, err)

	aoFalse := ApplyOptions{
		useRightBorder: false,
	}
	aoTrue := ApplyOptions{
		useRightBorder: true,
	}
	res, err := s.ApplyNew(context.Background(), start, aoFalse)
	require.NoError(t, err)
	timesAreEqual(t, expectedRes, res)

	expectedResRight, err := time.Parse(time.RFC3339, "2020-06-03T07:59:59+06:00")
	require.NoError(t, err)

	res, err = s.ApplyNew(context.Background(), start, aoTrue)
	require.NoError(t, err)
	timesAreEqual(t, expectedResRight, res)

	// In first interval
	start, err = time.Parse(time.RFC3339, "2020-06-03T04:45:23+06:00")
	require.NoError(t, err)
	expectedRes, err = time.Parse(time.RFC3339, "2020-06-03T04:45:23+06:00")
	require.NoError(t, err)

	res, err = s.ApplyNew(context.Background(), start, aoFalse)
	require.NoError(t, err)
	timesAreEqual(t, expectedRes, res)

	expectedResRight, err = time.Parse(time.RFC3339, "2020-06-03T07:59:59+06:00")
	require.NoError(t, err)

	res, err = s.ApplyNew(context.Background(), start, aoTrue)
	require.NoError(t, err)
	timesAreEqual(t, expectedResRight, res)

	// Before second interval
	start, err = time.Parse(time.RFC3339, "2020-06-03T10:05:01+06:00")
	require.NoError(t, err)
	expectedRes, err = time.Parse(time.RFC3339, "2020-06-03T11:00:00+06:00")
	require.NoError(t, err)

	res, err = s.ApplyNew(context.Background(), start, aoFalse)
	require.NoError(t, err)
	timesAreEqual(t, expectedRes, res)

	expectedResRight, err = time.Parse(time.RFC3339, "2020-06-03T19:35:00+06:00")
	require.NoError(t, err)

	res, err = s.ApplyNew(context.Background(), start, aoTrue)
	require.NoError(t, err)
	timesAreEqual(t, expectedResRight, res)

	// In second interval
	start, err = time.Parse(time.RFC3339, "2020-06-03T16:51:05+06:00")
	require.NoError(t, err)
	expectedRes, err = time.Parse(time.RFC3339, "2020-06-03T16:51:05+06:00")
	require.NoError(t, err)

	res, err = s.ApplyNew(context.Background(), start, aoFalse)
	require.NoError(t, err)
	timesAreEqual(t, expectedRes, res)

	expectedResRight, err = time.Parse(time.RFC3339, "2020-06-03T19:35:00+06:00")
	require.NoError(t, err)

	res, err = s.ApplyNew(context.Background(), start, aoTrue)
	require.NoError(t, err)
	timesAreEqual(t, expectedResRight, res)

	// After second interval
	start, err = time.Parse(time.RFC3339, "2020-06-03T20:03:10+06:00")
	require.NoError(t, err)
	expectedRes, err = time.Parse(time.RFC3339, "2020-06-10T02:03:00+06:00")
	require.NoError(t, err)

	res, err = s.ApplyNew(context.Background(), start, aoFalse)
	require.NoError(t, err)
	timesAreEqual(t, expectedRes, res)

	expectedResRight, err = time.Parse(time.RFC3339, "2020-06-10T07:59:59+06:00")
	require.NoError(t, err)

	res, err = s.ApplyNew(context.Background(), start, aoTrue)
	require.NoError(t, err)
	timesAreEqual(t, expectedResRight, res)
}

func TestEndAtPreviousDay(t *testing.T) {
	s, err := NewSchedule(CreateScheduleFromToEqual(), true)
	require.NoError(t, err)

	start, err := time.Parse(time.RFC3339, "2020-08-31T04:34:11+03:00")
	require.NoError(t, err)

	ao := ApplyOptions{
		useRightBorder: false,
	}
	res, err := s.ApplyNew(context.Background(), start, ao)
	require.NoError(t, err)
	timesAreEqual(t, start, res)

	endPreviousDay := s.IsEndAtPreviousDay(start)
	require.Equal(t, true, endPreviousDay)
}

func TestEqualFromToIntervalHandlong(t *testing.T) {
	s, err := newSimpleSchedule(CreateScheduleFromToEqual())
	require.NoError(t, err)
	require.Equal(
		t,
		s.Windows[1][0].From,
		timex.DayTime{Hour: 6, Minute: 0, Second: 0},
	)
	require.Equal(
		t,
		s.Windows[1][0].To,
		timex.DayTime{Hour: 6, Minute: 0, Second: 1},
	)

	s, err = newSimpleSchedule(CreateScheduleFromToEqualLastDay())
	require.NoError(t, err)
	require.Equal(
		t,
		s.Windows[1][0].From,
		timex.DayTime{Hour: 23, Minute: 59, Second: 58},
	)
	require.Equal(
		t,
		s.Windows[1][0].To,
		timex.DayTime{Hour: 23, Minute: 59, Second: 59},
	)
}

func TestApplyWindow(t *testing.T) {
	startTime, err := time.Parse(time.RFC3339, "2020-08-31T04:34:11+03:00")
	require.NoError(t, err)
	expectedEndTime, err := time.Parse(time.RFC3339, "2020-08-31T10:20:30+03:00")
	require.NoError(t, err)

	sw := ScheduleWindow{
		From: timex.DayTime{Hour: 10, Minute: 20, Second: 30},
		To:   timex.DayTime{Hour: 11, Minute: 21, Second: 31},
	}

	endTime, err := sw.Apply(startTime)
	require.NoError(t, err)
	timesAreEqual(t, expectedEndTime, endTime)

	// After interval
	startTime, err = time.Parse(time.RFC3339, "2020-08-31T10:20:31+03:00")
	require.NoError(t, err)

	endTime, err = sw.Apply(startTime)
	require.NoError(t, err)
	timesAreEqual(t, startTime, endTime)
}

func TestApplyIntervalWindow(t *testing.T) {
	startTime, err := time.Parse(time.RFC3339, "2020-08-31T04:34:11+03:00")
	require.NoError(t, err)
	expectedEndTime, err := time.Parse(time.RFC3339, "2020-08-31T10:20:30+03:00")
	require.NoError(t, err)

	sw := ScheduleWindow{
		From: timex.DayTime{Hour: 10, Minute: 20, Second: 30},
		To:   timex.DayTime{Hour: 11, Minute: 21, Second: 31},
	}

	endTime, in, err := sw.ApplyInterval(startTime)
	require.NoError(t, err)
	timesAreEqual(t, expectedEndTime, endTime)

	require.Equal(t, sw.From.Hour, in.From.Hour)
	require.Equal(t, sw.From.Minute, in.From.Minute)
	require.Equal(t, sw.To.Hour, in.To.Hour)
	require.Equal(t, sw.To.Minute, in.To.Minute)

	// After interval
	startTime, err = time.Parse(time.RFC3339, "2020-08-31T10:20:31+03:00")
	require.NoError(t, err)

	endTime, in, err = sw.ApplyInterval(startTime)
	require.NoError(t, err)
	timesAreEqual(t, startTime, endTime)

	require.Equal(t, sw.From.Hour, in.From.Hour)
	require.Equal(t, sw.From.Minute, in.From.Minute)
	require.Equal(t, sw.To.Hour, in.To.Hour)
	require.Equal(t, sw.To.Minute, in.To.Minute)
}

func TestApplySchedule(t *testing.T) {
	startTime, err := time.Parse(time.RFC3339, "2020-08-31T04:34:11+03:00")
	require.NoError(t, err)
	expectedEndTime, err := time.Parse(time.RFC3339, "2020-09-02T02:03:00+03:00")
	require.NoError(t, err)
	s, err := newSimpleSchedule(CreateScheduleManyIntervals())
	require.NoError(t, err)

	ao := ApplyOptions{
		useRightBorder: false,
	}
	endTime, err := s.ApplyNew(context.Background(), startTime, ao)
	require.NoError(t, err)
	timesAreEqual(t, expectedEndTime, endTime)
}

func TestApplyIntervalSchedule(t *testing.T) {
	startTime, err := time.Parse(time.RFC3339, "2020-08-31T04:34:11+03:00")
	require.NoError(t, err)
	expectedEndTime1, err := time.Parse(time.RFC3339, "2020-09-02T02:03:00+03:00")
	require.NoError(t, err)
	expectedEndTime2, err := time.Parse(time.RFC3339, "2020-09-02T11:00:00+03:00")
	require.NoError(t, err)
	s, err := newSimpleSchedule(CreateScheduleManyIntervals())
	require.NoError(t, err)

	endTimeInterval, err := s.ApplyInterval(context.Background(), startTime, false, false, false, nil, nil)
	require.NoError(t, err)
	require.Equal(t, 2, len(endTimeInterval))

	et := endTimeInterval[0]
	timesAreEqual(t, expectedEndTime1, et.EndTime)
	require.Equal(
		t,
		et.Interval.From,
		timex.DayTime{Hour: 2, Minute: 3, Second: 0},
	)
	require.Equal(
		t,
		et.Interval.To,
		timex.DayTime{Hour: 7, Minute: 59, Second: 0},
	)

	et = endTimeInterval[1]
	timesAreEqual(t, expectedEndTime2, et.EndTime)
	require.Equal(
		t,
		et.Interval.From,
		timex.DayTime{Hour: 11, Minute: 0, Second: 0},
	)
	require.Equal(
		t,
		et.Interval.To,
		timex.DayTime{Hour: 19, Minute: 35, Second: 0},
	)
}

func TestApplyIntervalForDeferred(t *testing.T) {
	startTimeMiddle, err := time.Parse(time.RFC3339, "2020-09-02T11:34:11+03:00")
	require.NoError(t, err)
	startTimeExact, err := time.Parse(time.RFC3339, "2020-09-02T12:00:00+03:00")
	require.NoError(t, err)

	s, err := newSimpleSchedule(CreateScheduleDeferredIntervals())
	require.NoError(t, err)

	expectedIntervals := []Interval{}
	for hour := 12; hour < 20; hour++ {
		interval := Interval{
			From: timex.DayTime{Hour: int8(hour), Minute: 0, Second: 0},
			To:   timex.DayTime{Hour: int8(hour + 1), Minute: 0, Second: 0},
		}
		expectedIntervals = append(expectedIntervals, interval)
	}

	startTimes := []time.Time{startTimeMiddle, startTimeExact}
	for _, startTime := range startTimes {
		endTimeInterval, err := s.ApplyInterval(
			context.Background(),
			startTime,
			false, // turnOffDisableDates
			false, // useOnlyHolidayDates
			true,  // deferredUseNextInterval
			nil,   // daysOffGrouped
			nil,   // tails
		)
		require.NoError(t, err)
		require.Equal(t, len(expectedIntervals), len(endTimeInterval))

		for i := range endTimeInterval {
			require.Equal(t, expectedIntervals[i], endTimeInterval[i].Interval)
		}
	}
}

func TestLinehaulDisabled(t *testing.T) {
	scLastMile, err := NewSchedule(
		CreateSimpleSchedule(),
		false,
	)
	require.NoError(t, err)
	{
		pb := NewPathBuilder()
		sc, err := NewSchedule(
			CreateSimpleSchedule(),
			false,
		)
		require.NoError(t, err)
		// Only one Delivery service wtihout delivery type
		linehaul := pb.AddLinehaul(
			pb.MakeDeliveryService(
				pb.WithDeliveryType(DeliveryTypeUnknown),
				pb.WithSchedule(*sc),
				pb.WithServiceID(-1001),
			),
			pb.MakeLastMileService(
				pb.WithSchedule(*scLastMile),
				pb.WithServiceID(-1002),
			),
		)
		daysOffGrouped := daysoff.NewServicesHashed()
		daysOffGrouped.DaysOffGrouped[-1001] = daysoff.NewDaysOffGroupedFromStrings([]string{"2020-06-01"})
		daysOffGrouped.DaysOffGrouped[-1002] = daysoff.NewDaysOffGroupedFromStrings(
			[]string{
				"2020-06-07",
				"2020-06-08",
				"2020-06-09",
				"2020-06-10",
			})
		courResultGroupedDaysOff := LinehaulDisabledDates(
			pb.graph.GetNodeByID(linehaul),
			DeliveryTypeCourier,
			daysOffGrouped,
		)
		require.Equal(t, 1, len(courResultGroupedDaysOff))

		pickupResultGroupeDaysOff := LinehaulDisabledDates(
			pb.graph.GetNodeByID(linehaul),
			DeliveryTypePickup,
			daysOffGrouped,
		)
		require.Equal(t, 1, len(pickupResultGroupeDaysOff))
	}
	{
		scCourier, err := NewSchedule(
			CreateSimpleSchedule(),
			false,
		)
		require.NoError(t, err)
		scPickup, err := NewSchedule(
			CreateSimpleSchedule(),
			false,
		)
		require.NoError(t, err)
		pb := NewPathBuilder()
		// Separate Delivery services for courier and pickup
		linehaul := pb.AddLinehaul(
			pb.MakeDeliveryService(
				pb.WithDeliveryType(DeliveryTypeCourier),
				pb.WithSchedule(*scCourier),
				pb.WithServiceID(-1001),
			),
			pb.MakeDeliveryService(
				pb.WithDeliveryType(DeliveryTypePickup),
				pb.WithSchedule(*scPickup),
				pb.WithServiceID(-1002),
			),
			pb.MakeLastMileService(
				pb.WithSchedule(*scLastMile),
				pb.WithServiceID(-1003),
			),
		)
		daysOffGrouped := daysoff.NewServicesHashed()
		daysOffGrouped.DaysOffGrouped[-1001] = daysoff.NewDaysOffGroupedFromStrings(
			[]string{
				"2020-06-02",
				"2020-06-03",
			})
		daysOffGrouped.DaysOffGrouped[-1002] = daysoff.NewDaysOffGroupedFromStrings(
			[]string{
				"2020-06-04",
				"2020-06-05",
				"2020-06-06",
			})
		daysOffGrouped.DaysOffGrouped[-1003] = daysoff.NewDaysOffGroupedFromStrings(
			[]string{
				"2020-06-07",
				"2020-06-08",
				"2020-06-09",
				"2020-06-10",
			})
		courResultGroupedDaysOff := LinehaulDisabledDates(
			pb.graph.GetNodeByID(linehaul),
			DeliveryTypeCourier,
			daysOffGrouped,
		)
		require.Equal(t, 2, len(courResultGroupedDaysOff))

		pickupResultGroupeDaysOff := LinehaulDisabledDates(
			pb.graph.GetNodeByID(linehaul),
			DeliveryTypePickup,
			daysOffGrouped,
		)
		require.Equal(t, 3, len(pickupResultGroupeDaysOff))
	}
}

func TestScheduleFactory(t *testing.T) {
	factory := NewScheduleFactory()
	{
		sc1, _ := factory.NewSchedule(CreateSimpleSchedule(), false)
		sc2, _ := factory.NewSchedule(CreateSimpleSchedule(), false)
		require.Equal(t, 1, int(sc1.Index))
		require.Equal(t, 1, int(sc2.Index))
		require.Equal(t, sc1, sc2)
	}
	{
		sc1, _ := factory.NewSchedule(CreateSimpleSchedule(), true)
		sc2, _ := factory.NewSchedule(CreateSimpleSchedule(), true)
		require.Equal(t, 2, int(sc1.Index))
		require.Equal(t, 2, int(sc2.Index))
		require.Equal(t, sc1, sc2)
	}
	{
		// Разные дни.
		s1 := []DaySchedule{{DayFloat: 1, From: "10:00:00", To: "22:00:00"}}
		s2 := []DaySchedule{{DayFloat: 2, From: "10:00:00", To: "22:00:00"}}
		sc1, _ := factory.NewSchedule(s1, false)
		sc2, _ := factory.NewSchedule(s2, false)
		require.Equal(t, 3, int(sc1.Index))
		require.Equal(t, 4, int(sc2.Index))
	}
}

func TestPropagateInterval(t *testing.T) {
	createIntervals := func(
		startCallCourier time.Time,
		startTimeFrom time.Time,
		startTimeTo time.Time,
		limitDate time.Time,
		enableOvernightExpressIntervals bool,
	) (deliveryIntervals []express.IntervalCallCourier) {
		yearDay := calcYearDay(startTimeTo, 0)
		fromDuration := startTimeFrom.Sub(startCallCourier)
		toDuration := startTimeTo.Sub(startCallCourier)
		for (!enableOvernightExpressIntervals && !limitDate.Before(startTimeTo) && yearDay == calcYearDay(startTimeTo, 0)) ||
			(enableOvernightExpressIntervals && !limitDate.Before(startCallCourier) && yearDay == calcYearDay(startCallCourier, 0)) {
			deliveryIntervals = append(deliveryIntervals, express.IntervalCallCourier{
				CallCourier: startCallCourier,
				From:        startTimeFrom,
				To:          startTimeTo,
			})
			startTimeFrom = startTimeFrom.Add(time.Hour / 2).Truncate(time.Hour / 2)
			startCallCourier = startTimeFrom.Add(-fromDuration)
			startTimeTo = startCallCourier.Add(toDuration)
		}
		return
	}
	itsFlags := []struct {
		enableOvernightExpressIntervals bool
		useDeliveryTimeAnalytics        bool
		rightExtraExpressDeliveryTime   int
	}{
		// TODO(login-off-vit): Закомментированные кейсы пока не имеет смысле тестировать без моканья аналитики
		{false, false, 0},
		{false, false, 10},
		//{false, true, 0},
		//{false, true, 10},
		{true, false, 0},
		{true, false, 10},
		//{true, true, 0},
		//{true, true, 10},
	}
	for _, flags := range itsFlags {
		fmt.Printf("{\"enable_overnight_express_intervals\": %s,\"use_delivery_time_analytics\": %s, \"right_extra_express_delivery_time\": %d}\n",
			strconv.FormatBool(flags.enableOvernightExpressIntervals),
			strconv.FormatBool(flags.useDeliveryTimeAnalytics),
			flags.rightExtraExpressDeliveryTime,
		)
		propagateSettings, _ := its.NewStringSettingsHolder(
			fmt.Sprintf("{\"enable_overnight_express_intervals\": %s,\"use_delivery_time_analytics\": %s, \"right_extra_express_delivery_time\": %d}",
				strconv.FormatBool(flags.enableOvernightExpressIntervals),
				strconv.FormatBool(flags.useDeliveryTimeAnalytics),
				flags.rightExtraExpressDeliveryTime,
			),
		)
		ctx := settings.ContextWithSettings(context.Background(), settings.New(propagateSettings.GetSettings(), ""))
		// Круглосуточное(isTrivial) расписание
		{
			fmt.Println("Круглосуточное(isTrivial) расписание")
			s, _ := newSimpleSchedule(CreateSimpleSchedule())
			wantDeliveryIntervals := createIntervals(
				time.Date(2021, 7, 15, 16, 20, 0, 0, time.UTC),
				time.Date(2021, 7, 15, 17, 40, 0, 0, time.UTC),
				time.Date(2021, 7, 15, 18, 20, 0, 0, time.UTC),
				time.Date(2021, 7, 16, 0, 0, 0, 0, time.UTC),
				flags.enableOvernightExpressIntervals,
			)
			result := s.PropagateInterval(ctx, express.IntervalCallCourier{
				CallCourier: time.Date(2021, 7, 15, 16, 20, 0, 0, time.UTC),
				From:        time.Date(2021, 7, 15, 17, 40, 0, 0, time.UTC),
				To:          time.Date(2021, 7, 15, 18, 20, 0, 0, time.UTC),
			}, nil, nil, 0, 0)
			require.Equal(t, wantDeliveryIntervals, result)
		}
		// HACK(login-off-vit): Тестируем переход слотов экспресса на следующие сутки пока только для круглосуточного расписания
		if flags.enableOvernightExpressIntervals {
			continue
		}
		// Ежедневное расписание 12-22. startTime в окне расписания
		{
			fmt.Println("Ежедневное расписание 12-22. startTime в окне расписания")
			s, err := NewSchedule(
				CreateFullWeekSchedule(),
				false,
			)
			require.NoError(t, err)
			wantDeliveryIntervals := createIntervals(
				time.Date(2021, 7, 15, 16, 20, 0, 0, time.UTC),
				time.Date(2021, 7, 15, 17, 40, 0, 0, time.UTC),
				time.Date(2021, 7, 15, 18, 20, 0, 0, time.UTC),
				time.Date(2021, 7, 15, 22, 0, 0, 0, time.UTC),
				flags.enableOvernightExpressIntervals,
			)
			result := s.PropagateInterval(ctx, express.IntervalCallCourier{
				CallCourier: time.Date(2021, 7, 15, 16, 20, 0, 0, time.UTC),
				From:        time.Date(2021, 7, 15, 17, 40, 0, 0, time.UTC),
				To:          time.Date(2021, 7, 15, 18, 20, 0, 0, time.UTC),
			}, nil, nil, 0, 0)
			require.Equal(t, wantDeliveryIntervals, result)
		}
		// Ежедневное расписание 12-22. startTime до окна расписания
		{
			fmt.Println("Ежедневное расписание 12-22. startTime до окна расписания")
			s, err := NewSchedule(
				CreateFullWeekSchedule(),
				false,
			)
			require.NoError(t, err)
			wantDeliveryIntervals := createIntervals(
				time.Date(2021, 7, 15, 12, 0, 0, 0, time.UTC),
				time.Date(2021, 7, 15, 12, 40, 0, 0, time.UTC),
				time.Date(2021, 7, 15, 13, 20, 0, 0, time.UTC),
				time.Date(2021, 7, 15, 22, 0, 0, 0, time.UTC),
				flags.enableOvernightExpressIntervals,
			)
			result := s.PropagateInterval(ctx, express.IntervalCallCourier{
				CallCourier: time.Date(2021, 7, 15, 10, 0, 0, 0, time.UTC),
				From:        time.Date(2021, 7, 15, 10, 40, 0, 0, time.UTC),
				To:          time.Date(2021, 7, 15, 11, 20, 0, 0, time.UTC),
			}, nil, nil, 0, 0)
			require.Equal(t, wantDeliveryIntervals, result)
		}
		// Ежедевное расписание 12-22. startTime после окна расписания
		{
			fmt.Println("Ежедевное расписание 12-22. startTime после окна расписания")
			s, err := NewSchedule(
				CreateFullWeekSchedule(),
				false,
			)
			require.NoError(t, err)
			result := s.PropagateInterval(ctx, express.IntervalCallCourier{
				CallCourier: time.Date(2021, 7, 15, 22, 20, 0, 0, time.UTC),
				From:        time.Date(2021, 7, 15, 22, 40, 0, 0, time.UTC),
				To:          time.Date(2021, 7, 15, 23, 20, 0, 0, time.UTC),
			}, nil, nil, 0, 0)
			require.Len(t, result, 0)
		}
		// Ежедевное расписание 12-16, 17-22. startTime в первом окне расписания
		{
			fmt.Println("Ежедевное расписание 12-16, 17-22. startTime в первом окне расписания")
			ds := []DaySchedule{
				{
					DayFloat: 4.0,
					From:     "12:00:00",
					To:       "16:00:00",
				},
				{
					DayFloat: 4.0,
					From:     "17:00:00",
					To:       "22:00:00",
				},
			}
			s, err := NewSchedule(ds, false)
			require.NoError(t, err)
			wantDeliveryIntervals := createIntervals(
				time.Date(2021, 7, 15, 12, 40, 0, 0, time.UTC),
				time.Date(2021, 7, 15, 13, 40, 0, 0, time.UTC),
				time.Date(2021, 7, 15, 14, 20, 0, 0, time.UTC),
				time.Date(2021, 7, 15, 16, 0, 0, 0, time.UTC),
				flags.enableOvernightExpressIntervals,
			)
			wantDeliveryIntervals = append(wantDeliveryIntervals,
				createIntervals(
					time.Date(2021, 7, 15, 17, 0, 0, 0, time.UTC),
					time.Date(2021, 7, 15, 18, 0, 0, 0, time.UTC),
					time.Date(2021, 7, 15, 18, 40, 0, 0, time.UTC),
					time.Date(2021, 7, 15, 22, 0, 0, 0, time.UTC),
					flags.enableOvernightExpressIntervals,
				)...,
			)
			result := s.PropagateInterval(ctx, express.IntervalCallCourier{
				CallCourier: time.Date(2021, 7, 15, 12, 40, 0, 0, time.UTC),
				From:        time.Date(2021, 7, 15, 13, 40, 0, 0, time.UTC),
				To:          time.Date(2021, 7, 15, 14, 20, 0, 0, time.UTC),
			}, nil, nil, 0, 0)
			require.Equal(t, wantDeliveryIntervals, result)
		}
		// Ежедевное расписание 0-23:30. Изначальный интервал 9:20-10:00. Корнер кейс 23:20-0:00
		{
			fmt.Println("Ежедевное расписание 0-23:30. Изначальный интервал 9:20-10:00. Корнер кейс 23:20-0:00")
			ds := []DaySchedule{
				{
					DayFloat: 4.0,
					From:     "0:00:00",
					To:       "23:30:00",
				},
			}
			s, err := NewSchedule(ds, false)
			require.NoError(t, err)
			wantDeliveryIntervals := createIntervals(
				time.Date(2021, 7, 15, 8, 40, 0, 0, time.UTC),
				time.Date(2021, 7, 15, 9, 20, 0, 0, time.UTC),
				time.Date(2021, 7, 15, 10, 0, 0, 0, time.UTC),
				time.Date(2021, 7, 15, 23, 30, 0, 0, time.UTC),
				flags.enableOvernightExpressIntervals,
			)
			result := s.PropagateInterval(ctx, express.IntervalCallCourier{
				CallCourier: time.Date(2021, 7, 15, 8, 40, 0, 0, time.UTC),
				From:        time.Date(2021, 7, 15, 9, 20, 0, 0, time.UTC),
				To:          time.Date(2021, 7, 15, 10, 0, 0, 0, time.UTC),
			}, nil, nil, 0, 0)
			require.Equal(t, wantDeliveryIntervals, result)
		}
		// Ежедевное расписание 0-23:30. Подпёрт перескок на следующие сутки
		{
			fmt.Println("Ежедевное расписание 0-23:30. Подпёрт перескок на следующие сутки")
			ds := []DaySchedule{
				{
					DayFloat: 4.0,
					From:     "0:00:00",
					To:       "23:30:00",
				},
			}
			s, err := NewSchedule(ds, false)
			require.NoError(t, err)
			wantDeliveryIntervals := createIntervals(
				time.Date(2021, 7, 15, 8, 40, 0, 0, time.UTC),
				time.Date(2021, 7, 15, 9, 50, 0, 0, time.UTC),
				time.Date(2021, 7, 15, 10, 30, 0, 0, time.UTC),
				time.Date(2021, 7, 15, 23, 30, 0, 0, time.UTC),
				flags.enableOvernightExpressIntervals,
			)
			result := s.PropagateInterval(ctx, express.IntervalCallCourier{
				CallCourier: time.Date(2021, 7, 15, 8, 40, 0, 0, time.UTC),
				From:        time.Date(2021, 7, 15, 9, 50, 0, 0, time.UTC),
				To:          time.Date(2021, 7, 15, 10, 30, 0, 0, time.UTC),
			}, nil, nil, 0, 0)
			require.Equal(t, wantDeliveryIntervals, result)
		}
		// Ежедевное расписание 8:30-23:00. Изначальный интервал за пределами расписания с переходом через сутки
		{
			fmt.Println("Ежедевное расписание 8:30-23:00. Изначальный интервал за пределами расписания с переходом через сутки")
			ds := []DaySchedule{
				{
					DayFloat: 4.0,
					From:     "8:30:00",
					To:       "23:00:00",
				},
			}
			s, err := NewSchedule(ds, false)
			require.NoError(t, err)
			result := s.PropagateInterval(ctx, express.IntervalCallCourier{
				CallCourier: time.Date(2021, 7, 15, 20, 8, 4, 0, time.UTC),
				From:        time.Date(2021, 7, 15, 23, 25, 0, 0, time.UTC),
				To:          time.Date(2021, 7, 15, 0, 5, 0, 0, time.UTC),
			}, nil, nil, 0, 0)
			require.Len(t, result, 0)
		}
	}
}

func TestAdjustTimeToWindowsInPast(t *testing.T) {
	{
		s, _ := newSimpleSchedule([]DaySchedule{})
		wantTime := time.Date(2021, 7, 28, 17, 0, 0, 0, time.UTC)
		result, ok := s.AdjustTimeToWindowsInPast(wantTime)
		require.True(t, ok)
		require.Equal(t, wantTime, result)
	}
	{
		s, _ := newSimpleSchedule(CreateSimpleSchedule())
		wantTime := time.Date(2021, 7, 28, 17, 0, 0, 0, time.UTC)
		result, ok := s.AdjustTimeToWindowsInPast(wantTime)
		require.True(t, ok)
		require.Equal(t, wantTime, result)
	}
	{
		s, err := NewSchedule(
			CreateFullWeekSchedule(),
			false,
		)
		require.NoError(t, err)

		// После окна расписания -> правый край расписания
		result, ok := s.AdjustTimeToWindowsInPast(time.Date(2021, 7, 28, 23, 0, 0, 0, time.UTC))
		require.True(t, ok)
		wantTime := time.Date(2021, 7, 28, 22, 0, 0, 0, time.UTC)
		require.Equal(t, wantTime, result)

		// Правый край окна расписания
		result, ok = s.AdjustTimeToWindowsInPast(wantTime)
		require.True(t, ok)
		require.Equal(t, wantTime, result)

		// Левый край окна расписания
		wantTime = time.Date(2021, 7, 28, 12, 0, 0, 0, time.UTC)
		result, ok = s.AdjustTimeToWindowsInPast(wantTime)
		require.True(t, ok)
		require.Equal(t, wantTime, result)

		// Ранее всех окон расписания в этот день
		require.Equal(t, wantTime, result)
		wantTime = time.Date(2021, 7, 28, 11, 59, 0, 0, time.UTC)
		_, ok = s.AdjustTimeToWindowsInPast(wantTime)
		require.False(t, ok)
	}
	{
		ds := []DaySchedule{
			{
				DayFloat: 4.0,
				From:     "12:00:00",
				To:       "16:00:00",
			},
			{
				DayFloat: 4.0,
				From:     "17:00:00",
				To:       "22:00:00",
			},
		}
		s, err := NewSchedule(ds, false)
		require.NoError(t, err)

		// Между окнами
		result, ok := s.AdjustTimeToWindowsInPast(time.Date(2021, 7, 29, 16, 30, 0, 0, time.UTC))
		require.True(t, ok)
		wantTime := time.Date(2021, 7, 29, 16, 0, 0, 0, time.UTC)
		require.Equal(t, wantTime, result)

		// Ранее всех окон в расписании
		_, ok = s.AdjustTimeToWindowsInPast(time.Date(2021, 7, 29, 11, 30, 0, 0, time.UTC))
		require.False(t, ok)
	}
}

func TestIntersects(t *testing.T) {
	//    |------|
	//    |------|
	{
		sw1 := ScheduleWindow{
			From: timex.DayTime{Hour: 10, Minute: 30, Second: 0},
			To:   timex.DayTime{Hour: 12, Minute: 40, Second: 0},
		}
		sw2 := ScheduleWindow{
			From: timex.DayTime{Hour: 10, Minute: 30, Second: 0},
			To:   timex.DayTime{Hour: 12, Minute: 40, Second: 0},
		}
		result := sw1.Intersects(sw2)
		require.True(t, result)
		result = sw2.Intersects(sw1)
		require.True(t, result)
	}
	//     |------|        |        |-----|
	//        |------|     |    |------|
	{
		sw1 := ScheduleWindow{
			From: timex.DayTime{Hour: 10, Minute: 30, Second: 0},
			To:   timex.DayTime{Hour: 12, Minute: 40, Second: 0},
		}
		sw2 := ScheduleWindow{
			From: timex.DayTime{Hour: 11, Minute: 30, Second: 0},
			To:   timex.DayTime{Hour: 13, Minute: 40, Second: 0},
		}
		result := sw1.Intersects(sw2)
		require.True(t, result)
		result = sw2.Intersects(sw1)
		require.True(t, result)
	}
	//      |------|          |           |------|
	//             |-----|    |     |-----|
	{
		sw1 := ScheduleWindow{
			From: timex.DayTime{Hour: 10, Minute: 30, Second: 0},
			To:   timex.DayTime{Hour: 12, Minute: 40, Second: 0},
		}
		sw2 := ScheduleWindow{
			From: timex.DayTime{Hour: 12, Minute: 40, Second: 0},
			To:   timex.DayTime{Hour: 13, Minute: 40, Second: 0},
		}
		result := sw1.Intersects(sw2)
		require.False(t, result)
		result = sw2.Intersects(sw1)
		require.False(t, result)
	}
	//     |--------|     |      |---|
	//        |---|       |   |--------|
	{
		sw1 := ScheduleWindow{
			From: timex.DayTime{Hour: 10, Minute: 30, Second: 0},
			To:   timex.DayTime{Hour: 12, Minute: 40, Second: 0},
		}
		sw2 := ScheduleWindow{
			From: timex.DayTime{Hour: 11, Minute: 40, Second: 0},
			To:   timex.DayTime{Hour: 12, Minute: 20, Second: 0},
		}
		result := sw1.Intersects(sw2)
		require.True(t, result)
		result = sw2.Intersects(sw1)
		require.True(t, result)
	}
	//     |-------|            |           |------|
	//               |-----|    |   |-----|
	{
		sw1 := ScheduleWindow{
			From: timex.DayTime{Hour: 10, Minute: 30, Second: 0},
			To:   timex.DayTime{Hour: 12, Minute: 40, Second: 0},
		}
		sw2 := ScheduleWindow{
			From: timex.DayTime{Hour: 14, Minute: 40, Second: 0},
			To:   timex.DayTime{Hour: 16, Minute: 50, Second: 0},
		}
		result := sw1.Intersects(sw2)
		require.False(t, result)
		result = sw2.Intersects(sw1)
		require.False(t, result)
	}
}

func TestCompareAndGetEarlier(t *testing.T) {
	{
		dt1 := timex.DayTime{Hour: 10, Minute: 5, Second: 0}
		dt2 := timex.DayTime{Hour: 8, Minute: 3, Second: 0}
		res := dt1.CompareAndGetEarlier(dt2)
		require.Equal(t, dt2, res)
		res = dt2.CompareAndGetEarlier(dt1)
		require.Equal(t, dt2, res)
	}
	{
		dt1 := timex.DayTime{Hour: 11, Minute: 35, Second: 0}
		dt2 := timex.DayTime{Hour: 11, Minute: 35, Second: 0}
		res := dt1.CompareAndGetEarlier(dt2)
		require.Equal(t, dt2, res)
		res = dt2.CompareAndGetEarlier(dt1)
		require.Equal(t, dt2, res)
	}
}

func TestCompareAndGetLater(t *testing.T) {
	{
		dt1 := timex.DayTime{Hour: 10, Minute: 5, Second: 0}
		dt2 := timex.DayTime{Hour: 8, Minute: 3, Second: 0}
		res := dt1.CompareAndGetLater(dt2)
		require.Equal(t, dt1, res)
		res = dt2.CompareAndGetLater(dt1)
		require.Equal(t, dt1, res)
	}
	{
		dt1 := timex.DayTime{Hour: 11, Minute: 35, Second: 0}
		dt2 := timex.DayTime{Hour: 11, Minute: 35, Second: 0}
		res := dt1.CompareAndGetLater(dt2)
		require.Equal(t, dt2, res)
		res = dt2.CompareAndGetLater(dt1)
		require.Equal(t, dt2, res)
	}
}

func TestCrossWithOtherWindowsAndGetEarliest(t *testing.T) {
	// Оба расписания пустые
	{
		ssw1 := SScheduleWindows{}
		ssw2 := SScheduleWindows{}
		result, ok := ssw1.CrossWithOtherWindowsAndGetEarliest(ssw2)
		require.False(t, ok)
		require.Equal(t, ScheduleWindow{}, result)
	}
	// Одно из расписаний пустое
	{
		ssw1 := SScheduleWindows{
			ScheduleWindow{
				From: timex.DayTime{Hour: 10},
				To:   timex.DayTime{Hour: 12},
			},
		}
		ssw2 := SScheduleWindows{}
		result, ok := ssw1.CrossWithOtherWindowsAndGetEarliest(ssw2)
		require.False(t, ok)
		require.Equal(t, ScheduleWindow{}, result)

		result, ok = ssw2.CrossWithOtherWindowsAndGetEarliest(ssw1)
		require.False(t, ok)
		require.Equal(t, ScheduleWindow{}, result)
	}

	// Расписания не имеют пересекающихся окон
	{
		ssw1 := SScheduleWindows{
			ScheduleWindow{
				From: timex.DayTime{Hour: 10},
				To:   timex.DayTime{Hour: 12},
			},
			ScheduleWindow{
				From: timex.DayTime{Hour: 16},
				To:   timex.DayTime{Hour: 17},
			},
		}
		ssw2 := SScheduleWindows{
			ScheduleWindow{
				From: timex.DayTime{Hour: 13},
				To:   timex.DayTime{Hour: 15},
			},
			ScheduleWindow{
				From: timex.DayTime{Hour: 18},
				To:   timex.DayTime{Hour: 19},
			},
			ScheduleWindow{
				From: timex.DayTime{Hour: 21},
				To:   timex.DayTime{Hour: 22},
			},
		}
		result, ok := ssw1.CrossWithOtherWindowsAndGetEarliest(ssw2)
		require.False(t, ok)
		require.Equal(t, ScheduleWindow{}, result)

		result, ok = ssw2.CrossWithOtherWindowsAndGetEarliest(ssw1)
		require.False(t, ok)
		require.Equal(t, ScheduleWindow{}, result)
	}
	// Пересекающиеся окна первые
	{
		ssw1 := SScheduleWindows{
			ScheduleWindow{
				From: timex.DayTime{Hour: 10},
				To:   timex.DayTime{Hour: 12},
			},
			ScheduleWindow{
				From: timex.DayTime{Hour: 16},
				To:   timex.DayTime{Hour: 17},
			},
		}
		ssw2 := SScheduleWindows{
			ScheduleWindow{
				From: timex.DayTime{Hour: 11},
				To:   timex.DayTime{Hour: 15},
			},
			ScheduleWindow{
				From: timex.DayTime{Hour: 18},
				To:   timex.DayTime{Hour: 19},
			},
			ScheduleWindow{
				From: timex.DayTime{Hour: 21},
				To:   timex.DayTime{Hour: 22},
			},
		}
		wantSsw := ScheduleWindow{
			From: timex.DayTime{Hour: 11},
			To:   timex.DayTime{Hour: 12},
		}

		result, ok := ssw1.CrossWithOtherWindowsAndGetEarliest(ssw2)
		require.True(t, ok)
		require.Equal(t, wantSsw, result)

		result, ok = ssw2.CrossWithOtherWindowsAndGetEarliest(ssw1)
		require.True(t, ok)
		require.Equal(t, wantSsw, result)
	}
	// Пересекающиеся окна не первые
	{
		ssw1 := SScheduleWindows{
			ScheduleWindow{
				From: timex.DayTime{Hour: 9},
				To:   timex.DayTime{Hour: 10},
			},
			ScheduleWindow{
				From: timex.DayTime{Hour: 16},
				To:   timex.DayTime{Hour: 17},
			},
		}
		ssw2 := SScheduleWindows{
			ScheduleWindow{
				From: timex.DayTime{Hour: 11},
				To:   timex.DayTime{Hour: 12},
			},
			ScheduleWindow{
				From: timex.DayTime{Hour: 13},
				To:   timex.DayTime{Hour: 14},
			},
			ScheduleWindow{
				From: timex.DayTime{Hour: 16, Minute: 30},
				To:   timex.DayTime{Hour: 22},
			},
		}
		wantSsw := ScheduleWindow{
			From: timex.DayTime{Hour: 16, Minute: 30},
			To:   timex.DayTime{Hour: 17},
		}

		result, ok := ssw1.CrossWithOtherWindowsAndGetEarliest(ssw2)
		require.True(t, ok)
		require.Equal(t, wantSsw, result)

		result, ok = ssw2.CrossWithOtherWindowsAndGetEarliest(ssw1)
		require.True(t, ok)
		require.Equal(t, wantSsw, result)
	}
	// Окна расписания не пересекаются, а "соприкасаются" в одну минуту
	// Результат -> нет пересечения
	{
		ssw1 := SScheduleWindows{
			ScheduleWindow{
				From: timex.DayTime{Hour: 9},
				To:   timex.DayTime{Hour: 10},
			},
			// Такие интервалов быть не должно, но пусть протестируется дополнительно что ничего не поломает
			ScheduleWindow{
				From: timex.DayTime{Hour: 12},
				To:   timex.DayTime{Hour: 12},
			},
			ScheduleWindow{
				From: timex.DayTime{Hour: 16},
				To:   timex.DayTime{Hour: 17},
			},
		}
		ssw2 := SScheduleWindows{
			ScheduleWindow{
				From: timex.DayTime{Hour: 10},
				To:   timex.DayTime{Hour: 12},
			},
			ScheduleWindow{
				From: timex.DayTime{Hour: 13},
				To:   timex.DayTime{Hour: 16},
			},
		}

		result, ok := ssw1.CrossWithOtherWindowsAndGetEarliest(ssw2)
		require.False(t, ok)
		require.Equal(t, ScheduleWindow{}, result)

		result, ok = ssw2.CrossWithOtherWindowsAndGetEarliest(ssw1)
		require.False(t, ok)
		require.Equal(t, ScheduleWindow{}, result)
	}
	// Первые расписания не пересекаются, а "соприкасаются" в одну минуту
	// Результат -> пересечение вторых окон
	{
		ssw1 := SScheduleWindows{
			ScheduleWindow{
				From: timex.DayTime{Hour: 9},
				To:   timex.DayTime{Hour: 10},
			},
			ScheduleWindow{
				From: timex.DayTime{Hour: 16},
				To:   timex.DayTime{Hour: 17},
			},
		}
		ssw2 := SScheduleWindows{
			ScheduleWindow{
				From: timex.DayTime{Hour: 10},
				To:   timex.DayTime{Hour: 12},
			},
			ScheduleWindow{
				From: timex.DayTime{Hour: 16},
				To:   timex.DayTime{Hour: 17},
			},
		}

		wantSsw := ScheduleWindow{
			From: timex.DayTime{Hour: 16},
			To:   timex.DayTime{Hour: 17},
		}

		result, ok := ssw1.CrossWithOtherWindowsAndGetEarliest(ssw2)
		require.True(t, ok)
		require.Equal(t, wantSsw, result)

		result, ok = ssw2.CrossWithOtherWindowsAndGetEarliest(ssw1)
		require.True(t, ok)
		require.Equal(t, wantSsw, result)
	}
}

func TestGetWindowsByTimeWeekday(t *testing.T) {
	{
		var s Schedule
		result := s.GetWindowsByTimeWeekday(time.Date(2021, 8, 5, 0, 0, 0, 0, time.UTC))
		require.Nil(t, result)
	}
	{
		ds := []DaySchedule{
			{
				DayFloat: 4.0,
				From:     "12:00:00",
				To:       "16:00:00",
			},
			{
				DayFloat: 4.0,
				From:     "17:00:00",
				To:       "22:00:00",
			},
		}
		wantWindows := []ScheduleWindow{
			{
				From: timex.DayTime{Hour: 12},
				To:   timex.DayTime{Hour: 16},
			},
			{
				From: timex.DayTime{Hour: 17},
				To:   timex.DayTime{Hour: 22},
			},
		}
		s, err := NewSchedule(ds, false)
		require.NoError(t, err)
		result := s.GetWindowsByTimeWeekday(time.Date(2021, 8, 5, 9, 22, 4, 0, time.UTC))
		require.Equal(t, wantWindows, result)

		result = s.GetWindowsByTimeWeekday(time.Date(2021, 8, 6, 9, 22, 4, 0, time.UTC))
		require.Nil(t, result)
	}
}

func CreateComplexSchedule() []DaySchedule {
	return []DaySchedule{
		{
			DayFloat: 1.0,
			From:     "08:00:00",
			To:       "12:00:00",
		},
		{
			DayFloat: 1.0,
			From:     "13:00:00",
			To:       "17:00:00",
		},
		{
			DayFloat: 2.0,
			From:     "08:00:00",
			To:       "12:00:00",
		},
		{
			DayFloat: 2.0,
			From:     "13:00:00",
			To:       "17:00:00",
		},
		{
			DayFloat: 3.0,
			From:     "08:00:00",
			To:       "12:00:00",
		},
		{
			DayFloat: 3.0,
			From:     "13:00:00",
			To:       "17:00:00",
		},
		{
			DayFloat: 4.0,
			From:     "10:00:00",
			To:       "18:00:00",
		},
		{
			DayFloat: 5.0,
			From:     "12:00:00",
			To:       "21:00:00",
		},
		{
			DayFloat: 6.0,
			From:     "10:00:00",
			To:       "18:00:00",
		},
		{
			DayFloat: 7.0,
			From:     "08:00:00",
			To:       "12:00:00",
		},
		{
			DayFloat: 7.0,
			From:     "13:00:00",
			To:       "17:00:00",
		},
	}
}

func TestRepresentation(t *testing.T) {
	{
		var s *Schedule
		require.Equal(t, "nil", s.CalcRepresentation())
	}
	{
		s, _ := NewSchedule(CreateSimpleSchedule(), false)
		require.Equal(t, "Mon-Sun 0:00-23:59", s.CalcRepresentation())
	}
	{
		s, _ := NewSchedule(CreateWeekdaySchedule(), false)
		require.Equal(t, "Mon-Fri 12:00-22:00", s.CalcRepresentation())
	}
	{
		s, _ := NewSchedule(CreateScheduleRecursion(), false)
		require.Equal(t, "Sun 0:00-23:59", s.CalcRepresentation())
	}
	{
		s, _ := NewSchedule(CreateScheduleManyIntervals(), false)
		require.Equal(t, "Wed 2:03-7:59 11:00-19:35", s.CalcRepresentation())
	}
	{
		s, _ := NewSchedule(CreateScheduleFromToEqual(), false)
		require.Equal(t, "Mon 6:00-6:00", s.CalcRepresentation())
	}
	{
		s, _ := NewSchedule(CreateScheduleFromToEqualLastDay(), false)
		require.Equal(t, "Mon 23:59-23:59", s.CalcRepresentation())
	}
	{
		s, _ := NewSchedule(CreateEmptySchedule(), false)
		require.Equal(t, "", s.CalcRepresentation())
	}
	{
		s, _ := NewSchedule(CreateFullWeekSchedule(), false)
		require.Equal(t, "Mon-Sun 12:00-22:00", s.CalcRepresentation())
	}
	{
		s, _ := NewSchedule(CreateComplexSchedule(), false)
		require.Equal(
			t,
			"Mon-Wed,Sun 8:00-12:00 13:00-17:00 Thu,Sat 10:00-18:00 Fri 12:00-21:00",
			s.CalcRepresentation(),
		)
	}
}
