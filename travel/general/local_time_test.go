package dtutil

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

func TestLocalTime(t *testing.T) {
	type args struct {
	}
	moscowTz, err := time.LoadLocation("Europe/Moscow")
	assert.NoError(t, err, "unable to load timezone for Moscow")
	tests := []struct {
		name          string
		dateIndex     int
		scheduledTime int32
		tz            *time.Location
		wanted        time.Time
	}{
		{
			"utc tz",
			DateCache.IndexOfStringDateP("2021-07-02"),
			0,
			time.UTC,
			time.Date(2021, time.July, 2, 0, 0, 0, 0, time.UTC),
		},
		{
			"non-utc tz",
			DateCache.IndexOfStringDateP("2021-07-02"),
			328,
			moscowTz,
			time.Date(2021, time.July, 2, 3, 28, 0, 0, moscowTz),
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := LocalTime(tt.dateIndex, tt.scheduledTime, tt.tz); got != tt.wanted {
				t.Errorf("LocalTime() = %v, wanted %v", got, tt.wanted)
			}
		})
	}
}

func TestFormatWithTz(t *testing.T) {
	type args struct {
	}
	moscowTz, err := time.LoadLocation("Europe/Moscow")
	assert.NoError(t, err, "unable to load timezone for Moscow")
	tests := []struct {
		name          string
		dateIndex     int
		scheduledTime int32
		tz            *time.Location
		wanted        string
	}{
		{
			"utc tz",
			DateCache.IndexOfStringDateP("2021-07-02"),
			0,
			time.UTC,
			"2021-07-02T00:00:00Z",
		},
		{
			"non-utc tz",
			DateCache.IndexOfStringDateP("2021-07-02"),
			328,
			moscowTz,
			"2021-07-02T03:28:00+03:00",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := FormatWithTz(tt.dateIndex, tt.scheduledTime, tt.tz); got != tt.wanted {
				t.Errorf("FormatWithTz() = %v, wanted %v", got, tt.wanted)
			}
		})
	}
}
