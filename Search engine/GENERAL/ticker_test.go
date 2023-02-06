package ticker

import (
	"testing"
	"time"
)

var PerMinuteTests = []struct {
	Rules     int
	Interval  time.Duration
	PerMinute float64
}{
	{60, time.Minute, 60},
	{120, time.Minute, 120},
	{120, 2 * time.Minute, 60},
	{0, time.Minute, 0},
	{120, 0, 0},
	{0, 0, 0},
}

func TestPerMinute(t *testing.T) {
	for _, tt := range PerMinuteTests {
		v := PerMinute(tt.Rules, tt.Interval)
		if v != tt.PerMinute {
			t.Errorf("%v rules with %v interval: want %v, got %v",
				tt.Rules, tt.Interval, tt.PerMinute, v)
		}
	}
}

func TestTicker_sendsNoTicksWithZeroLimit(t *testing.T) {
	d := 10 * time.Millisecond
	tt := New(Config{
		PerMinute: float64(time.Minute / d),
		Limit:     0,
	})
	defer tt.Stop()

	select {
	case <-tt.C:
		t.Errorf("got unexpected value")
	case <-time.After(2 * d): // wait a bit more
	}

	select {
	case <-tt.Limit:
	case <-time.After(d):
		t.Errorf("limit channel is not closed")
	}
}

func TestTicker_sendsNTicksEqualToLimit(t *testing.T) {
	d := 10 * time.Millisecond
	tt := New(Config{
		PerMinute: float64(time.Minute / d),
		Limit:     3,
	})
	defer tt.Stop()

	<-tt.C
	<-tt.C
	<-tt.C

	select {
	case <-tt.C:
		t.Errorf("got unexpected value")
	case <-time.After(2 * d): // wait a bit more
	}

	select {
	case <-tt.Limit:
	case <-time.After(d):
		t.Errorf("limit channel is not closed")
	}
}
