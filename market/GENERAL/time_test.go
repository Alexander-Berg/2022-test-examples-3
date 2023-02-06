package timex

import (
	"testing"
	"time"

	"github.com/stretchr/testify/require"
)

func TestReplaceClock(t *testing.T) {
	clock2duration := func(hour, min, sec int) time.Duration {
		return time.Hour*time.Duration(hour) + time.Minute*time.Duration(min) + time.Second*time.Duration(sec)
	}
	type Spec struct {
		t              time.Time
		hour, min, sec int
		want           time.Time
	}
	specs := []Spec{
		Spec{
			t:    time.Date(2021, 8, 18, 11, 22, 33, 0, time.UTC),
			want: time.Date(2021, 8, 18, 0, 0, 0, 0, time.UTC),
		},
		Spec{
			t:    time.Date(2021, 8, 18, 11, 22, 33, 0, time.UTC),
			hour: 1,
			min:  2,
			sec:  3,
			want: time.Date(2021, 8, 18, 1, 2, 3, 0, time.UTC),
		},
	}
	for _, spec := range specs {
		require.Equal(t, spec.want, ReplaceClock(spec.t, spec.hour, spec.min, spec.sec))
		require.Equal(t, spec.want, StripUpToDay(spec.t).Add(clock2duration(spec.hour, spec.min, spec.sec)))
	}
}

func TestAddDaysAndTrimClock(t *testing.T) {
	type Spec struct {
		t    time.Time
		days int
		want time.Time
	}
	specs := []Spec{
		Spec{
			t:    time.Date(2021, 8, 18, 11, 22, 33, 0, time.UTC),
			days: 0,
			want: time.Date(2021, 8, 18, 0, 0, 0, 0, time.UTC),
		},
		Spec{
			t:    time.Date(2021, 8, 18, 11, 22, 33, 0, time.UTC),
			days: 1,
			want: time.Date(2021, 8, 19, 0, 0, 0, 0, time.UTC),
		},
	}
	for _, spec := range specs {
		require.Equal(t, spec.want, AddDaysAndTrimClock(spec.t, spec.days))
		require.Equal(t, spec.want, StripUpToDay(spec.t.AddDate(0, 0, spec.days)))
	}
}
