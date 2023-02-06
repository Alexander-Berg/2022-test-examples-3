package schedule

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/test/assertpb"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/consts"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/date/daytime"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/dict/factories"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/dict/registry"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/testutils"
)

func TestStationDateTime(t *testing.T) {
	maker, segment := prepareData(t)

	startDateTime := getDt(10, 10, consts.DefaultLocation)
	segment.thread.TzStartTime = daytime.FromTime(startDateTime).Int32()
	scheduleSegments := maker.SegmentsByThreadRuns(&segment, startDateTime)

	require.Len(t, scheduleSegments, 100)

	scheduleSegment := scheduleSegments[0]
	assertpb.Equal(t, segment.thread, scheduleSegment.Thread)
	assertpb.Equal(t, segment.departure, scheduleSegment.Departure)
	assertpb.Equal(t, segment.arrival, scheduleSegment.Arrival)

	assertDates(t, getDt(10, 10, consts.DefaultLocation), scheduleSegment.ThreadStartDt)
	assertDates(t, getDt(10, 12, consts.DefaultLocation), scheduleSegment.DepartureDateTime)
	assertDates(t, getDt(10, 15, consts.DefaultLocation), scheduleSegment.ArrivalDateTime)
}

func TestFirstRunBeforeStartDateTime(t *testing.T) {
	maker, segment := prepareData(t)

	startDateTime := getDt(10, 10, consts.DefaultLocation)
	segment.thread.TzStartTime = daytime.FromTime(startDateTime.Add(-5 * time.Hour)).Int32() // should be before startDateTime
	scheduleSegments := maker.SegmentsByThreadRuns(&segment, startDateTime)

	require.Len(t, scheduleSegments, 100)

	scheduleSegment := scheduleSegments[0]
	assertDates(t, getDt(11, 5, consts.DefaultLocation), scheduleSegment.ThreadStartDt)
}

func prepareData(t *testing.T) (*SegmentProvider, rawSegment) {
	logger := testutils.NewLogger(t)
	repoReg := registry.NewRepositoryRegistry(logger)
	maker := SegmentProvider{registry: repoReg}

	tz := factories.NewTimeZoneFactory(repoReg).WithCode(consts.DefaultTZCode).Create()
	thread := factories.NewThreadFactory(repoReg).
		Create()
	departure := factories.NewThreadStationFactory(repoReg).
		WithTimeZone(tz).
		WithDeparture(2 * time.Hour).
		WithThread(thread).
		Create()
	arrival := factories.NewThreadStationFactory(repoReg).
		WithTimeZone(tz).
		WithArrival(5 * time.Hour).
		WithThread(thread).
		Create()

	return &maker, rawSegment{
		thread:    thread,
		departure: departure,
		arrival:   arrival,
	}
}

func getDt(day int, hour int, l *time.Location) time.Time {
	return time.Date(2020, 1, day, hour, 0, 0, 0, l)
}

func assertDates(t *testing.T, lhs time.Time, rhs time.Time) {
	assert.Equal(t, lhs.String(), rhs.String())
}
