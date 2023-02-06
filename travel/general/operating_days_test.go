package dtutil

import "testing"

func TestOperatingDays_ShiftDays(t *testing.T) {
	type args struct {
	}
	tests := []struct {
		name string
		od   OperatingDays
		days int
		want OperatingDays
	}{
		{
			"Not shifted at all",
			1347,
			0,
			1347,
		},
		{
			"Shifted one day back",
			1347,
			-1,
			2367,
		},
		{
			"Shifted two days back",
			1347,
			-2,
			1256,
		},
		{
			"Shifted one day forward",
			1347,
			1,
			1245,
		},
		{
			"Shifted two days forward",
			1347,
			2,
			2356,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := tt.od.ShiftDays(tt.days); got != tt.want {
				t.Errorf("ShiftDays() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestOperatingDays_Intersect(t *testing.T) {
	type args struct {
	}
	tests := []struct {
		name string
		od   OperatingDays
		od2  OperatingDays
		want OperatingDays
	}{
		{
			"both the same",
			12457,
			12457,
			12457,
		},
		{
			"one in common",
			1234,
			4567,
			4,
		},
		{
			"none in common",
			123,
			567,
			0,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := tt.od.Intersect(tt.od2); got != tt.want {
				t.Errorf("Intersect() = %v, want %v", got, tt.want)
			}
		})
	}
}
