package runmask

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/library/go/units"
)

func TestCheckDateMask(t *testing.T) {
	{
		date := createYearDay(2020, 1, 12)
		expected := [12]int32{}
		expected[0] = 1 << 11
		mask := MaskByDate(date)

		assert.Equal(t, expected, mask.mask)
	}
	{
		date := createYearDay(2020, 3, 3)
		expected := [12]int32{}
		expected[2] = 1 << 2
		mask := MaskByDate(date)

		assert.Equal(t, expected, mask.mask)
	}
}

func TestMaskByDateForEveryDay(t *testing.T) {
	date := createYearDay(2020, 1, 12)
	for i := 0; i < 400; i++ {
		mask := MaskByDate(date)
		assert.Equal(t, 12, len(mask.mask))
		assert.True(t, mask.RunsAt(date))
		for _, days := range []time.Duration{-1, 1, 10} {
			assert.False(t, mask.RunsAt(date.Add(days*units.Day)))
		}

		date = date.Add(units.Day)
	}
}

func createYearDay(year int, month time.Month, day int) time.Time {
	return time.Date(year, month, day, 0, 0, 0, 0, time.UTC)
}
