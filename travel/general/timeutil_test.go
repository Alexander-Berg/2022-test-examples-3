package timeutil

import (
	"testing"
	"time"
)

func TestCoalesce(t *testing.T) {
	type args struct {
		times []time.Time
	}
	tests := []struct {
		name string
		args args
		want time.Time
	}{
		{
			"empty input",
			args{},
			time.Time{},
		},
		{
			"single empty value",
			args{[]time.Time{time.Time{}}},
			time.Time{},
		},
		{
			"single non-empty value",
			args{[]time.Time{time.Date(2000, 1, 1, 12, 34, 0, 0, time.UTC)}},
			time.Date(2000, 1, 1, 12, 34, 0, 0, time.UTC),
		},
		{
			"multiple values, first is empty",
			args{[]time.Time{time.Time{}, time.Date(2000, 1, 1, 12, 34, 0, 0, time.UTC)}},
			time.Date(2000, 1, 1, 12, 34, 0, 0, time.UTC),
		},
		{
			"multiple empty values",
			args{[]time.Time{time.Time{}, time.Time{}, time.Time{}, time.Time{}}},
			time.Time{},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := Coalesce(tt.args.times...); got != tt.want {
				t.Errorf("Coalesce() = %v, want %v", got, tt.want)
			}
		})
	}
}
