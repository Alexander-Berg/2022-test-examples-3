package checkprice

import (
	"fmt"
	"github.com/stretchr/testify/require"
	"testing"
)

func TestGetDaysToFlightBucket(t *testing.T) {
	testCases := []struct {
		daysToFlight   int
		expectedBucket uint
	}{
		{-100, 1},
		{-1, 1},

		{0, 1},
		{1, 1},
		{2, 2},
		{3, 2},
		{4, 4},
		{5, 4},
		{6, 4},
		{7, 4},
		{8, 8},
		{9, 8},

		{15, 8},
		{16, 16},
		{17, 16},

		{31, 16},
		{32, 32},
		{33, 32},

		{63, 32},
		{64, 64},
		{65, 64},

		{100, 64},
		{500, 64},
		{700, 64},
	}
	for _, tc := range testCases {
		t.Run(fmt.Sprintf("days to flight %v", tc.daysToFlight), func(t *testing.T) {
			require.Equal(t, tc.expectedBucket, getDaysToFlightBucket(tc.daysToFlight))
		})
	}
}

func TestGetPointTypeAndIDNoError(t *testing.T) {
	testCases := []struct {
		pointKey          string
		expectedPointType string
		expectedID        uint
	}{
		{"c213", "c", 213},
		{"s9654321", "s", 9654321},
	}
	for _, tc := range testCases {
		t.Run(fmt.Sprintf("point key %v", tc.pointKey), func(t *testing.T) {
			pointType, id, err := getPointTypeAndID(tc.pointKey)
			require.NoError(t, err)
			require.Equal(t, tc.expectedPointType, pointType)
			require.EqualValues(t, tc.expectedID, id)
		})
	}
}

func TestGetPointTypeAndIDError(t *testing.T) {
	testCases := []struct {
		pointKey string
	}{
		{"l213"},
		{"cs123"},
		{"123"},
		{"1"},
		{""},
		{"c"},
		{"s"},
	}
	for _, tc := range testCases {
		t.Run(fmt.Sprintf("point key %v", tc.pointKey), func(t *testing.T) {
			_, _, err := getPointTypeAndID(tc.pointKey)
			require.Error(t, err)
		})
	}
}
