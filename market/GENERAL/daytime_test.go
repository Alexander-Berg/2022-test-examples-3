package timex

import (
	"testing"
	"time"

	"github.com/stretchr/testify/require"
)

func TestAdd(t *testing.T) {
	cases := []struct {
		src  DayTime
		add  time.Duration
		want DayTime
	}{
		{
			DayTime{10, 00, 1},
			-10 * time.Hour,
			DayTime{0, 0, 1},
		},
		{
			DayTime{0, 30, 1},
			-60 * time.Minute,
			DayTime{23, 30, 1},
		},
		{
			DayTime{0, 0, 1},
			-60 * time.Second,
			DayTime{23, 59, 1},
		},
		{
			DayTime{10, 00, 1},
			-11 * time.Hour,
			DayTime{23, 0, 1},
		},
		{
			DayTime{10, 00, 1},
			-90 * time.Second,
			DayTime{9, 58, 31},
		},
		{
			DayTime{10, 00, 1},
			-90 * time.Minute,
			DayTime{8, 30, 1},
		},
		{
			DayTime{10, 00, 1},
			90 * time.Minute,
			DayTime{11, 30, 1},
		},
		{
			DayTime{10, 0, 0},
			time.Hour,
			DayTime{11, 0, 0},
		},
		{
			DayTime{10, 0, 0},
			time.Minute,
			DayTime{10, 1, 0},
		},
		{
			DayTime{10, 0, 0},
			time.Second,
			DayTime{10, 0, 1},
		},
		{
			DayTime{10, 0, 1},
			180 * time.Second,
			DayTime{10, 3, 1},
		},
		{
			DayTime{10, 3, 1},
			180 * time.Minute,
			DayTime{13, 3, 1},
		},
		{
			DayTime{10, 3, 1},
			30 * time.Hour,
			DayTime{16, 3, 1},
		},
	}
	for _, c := range cases {
		result := c.src.AddWithinDay(c.add)
		require.Equal(t, c.want, result)
	}
}

func TestRoundUpTo30(t *testing.T) {
	cases := []struct {
		src  DayTime
		want DayTime
	}{
		{
			DayTime{0, 0, 0},
			DayTime{0, 0, 0},
		},
		{
			DayTime{0, 1, 0},
			DayTime{0, 30, 0},
		},
		{
			DayTime{0, 31, 0},
			DayTime{1, 0, 0},
		},
		{
			DayTime{23, 30, 0},
			DayTime{23, 30, 0},
		},
		{
			DayTime{23, 31, 0},
			DayTime{0, 0, 0},
		},
	}
	for _, c := range cases {
		c.src.RoundUpTo30()
		require.Equal(t, c.want, c.src)
	}
}

func TestSubAlwaysPositive(t *testing.T) {
	cases := []struct {
		x    DayTime
		y    DayTime
		want time.Duration
	}{
		{
			DayTime{11, 0, 0},
			DayTime{10, 0, 0},
			time.Hour,
		},
		{
			DayTime{0, 0, 0},
			DayTime{23, 30, 0},
			30 * time.Minute,
		},
		{
			DayTime{0, 20, 0},
			DayTime{23, 30, 0},
			50 * time.Minute,
		},
	}
	for _, c := range cases {
		require.Equal(t, c.want, c.x.SubAlwaysPositive(c.y))
	}
}
