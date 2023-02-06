package express

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/settings"
	"a.yandex-team.ru/market/combinator/pkg/units"
)

func TestFindDuration(t *testing.T) {
	durations := DistanceDurationList{
		{2_000 * Meter, 20 * time.Minute, 1},
		{5_000 * Meter, 70 * time.Minute, 2},
		{999_000 * Meter, 140 * time.Minute, 3},
	}
	durations.Sort()

	specs := []struct {
		distance Distance
		duration time.Duration
		ok       bool
	}{
		{0 * Meter, durations[0].Duration, true},
		{1_500 * Meter, durations[0].Duration, true},
		{2_000 * Meter, durations[0].Duration, true},
		{4_000 * Meter, durations[1].Duration, true},
		{5_000 * Meter, durations[1].Duration, true},
		{999_000 * Meter, durations[2].Duration, true},
		{999_999_000 * Meter, 0, false},
	}
	for _, spec := range specs {
		duration, ok := durations.findDuration(spec.distance)
		require.Equal(t, spec.ok, ok, spec)
		require.Equal(t, spec.duration, duration, spec)
	}
}

func TestFindInterval(t *testing.T) {
	data := `{"warehouse_id":48578,"latitude":55.774532442174162838,"longitude":37.632735922046940402,"location_id":213,"ready_to_ship_time":40,"radial_zones":[{"radius":2000.0,"zone_id":1.0,"delivery_duration":155.0},{"radius":5000.0,"zone_id":2.0,"delivery_duration":170.0},{"radius":999000.0,"zone_id":3.0,"delivery_duration":190.0}]}
{"warehouse_id":56353,"latitude":59.934393322220728351,"longitude":30.361224040060225349,"location_id":2,"ready_to_ship_time":40,"radial_zones":[{"radius":2000.0,"zone_id":1.0,"delivery_duration":155.0},{"radius":5000.0,"zone_id":2.0,"delivery_duration":170.0},{"radius":999000.0,"zone_id":3.0,"delivery_duration":190.0}]}
`
	express, err := ReadFromString(data, nil)
	require.NoError(t, err)
	require.Len(t, express.Warehouses, 2)
	now := time.Date(2021, 03, 9, 12, 0, 0, 0, time.Local)
	{
		interval, ok := express.FindInterval(context.Background(), 48578, express.Warehouses[48578].Coord, &now, nil, nil)
		require.True(t, ok)
		require.Equal(t, 14, interval.From.Hour())
		require.Equal(t, 35, interval.From.Minute())
		require.Equal(t, 15, interval.To.Hour())
		require.Equal(t, 15, interval.To.Minute())
		require.Equal(t, 12, interval.CallCourier.Hour())
		require.Equal(t, 0, interval.CallCourier.Minute())
	}
	{
		interval, ok := express.FindInterval(context.Background(), 48578, units.GpsCoords{}, &now, nil, nil)
		require.True(t, ok)
		require.Equal(t, 15, interval.From.Hour())
		require.Equal(t, 10, interval.From.Minute())
		require.Equal(t, 15, interval.To.Hour())
		require.Equal(t, 50, interval.To.Minute())
		require.Equal(t, 12, interval.CallCourier.Hour())
		require.Equal(t, 0, interval.CallCourier.Minute())
	}
}

func TestPatchDurationByTimezone(t *testing.T) {
	some := 10 * time.Minute
	require.Equal(t, some, patchDurationByTimezone(some, MoscowOffset))
	require.Equal(t, some+time.Duration(3600)*time.Second, patchDurationByTimezone(some, MoscowOffset+3600))
	// Не работает для tz меньше Москвы!
	require.Equal(t, some, patchDurationByTimezone(some, MoscowOffset-3600))
}

func TestRoundTo5Minute(t *testing.T) {
	for minute := 1; minute <= 5; minute++ {
		now := time.Date(2021, 03, 9, 12, minute, 0, 0, time.Local)
		require.Equal(t, time.Date(2021, 03, 9, 12, 5, 0, 0, time.Local), roundTo5Minute(now))
	}
}

func TestMaxWindowByDistance(t *testing.T) {
	test := func(pairs []int64, defMaxWindow int, distance Distance) time.Duration {
		stx := &settings.Settings{MaxExpressDeliveryWindow: defMaxWindow, MaxExpressDeliveryWindowDistExceed: pairs}
		return MaxWindowByDistance(stx, distance)
	}

	minute := func(m time.Duration) time.Duration {
		return m * time.Minute
	}

	require.Equal(t, minute(0), test([]int64{}, 0, 1))
	require.Equal(t, minute(90), test([]int64{}, 90, 1))
	require.Equal(t, minute(90), test([]int64{15, 120}, 90, 1))
	require.Equal(t, minute(90), test([]int64{15, 120}, 90, 14500))
	require.Equal(t, minute(120), test([]int64{15, 120}, 90, 15000))
	require.Equal(t, minute(120), test([]int64{15, 120, 20, 180}, 90, 19500))
	require.Equal(t, minute(180), test([]int64{15, 120, 20, 180}, 90, 20000))
}
