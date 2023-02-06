package common

import (
	"testing"
	"time"

	"github.com/stretchr/testify/require"
)

func TestFormatISO8601(t *testing.T) {
	testCases := [][]string{
		{"2022-02-16T12:26:38+05:00", "2022-02-16T12:26:38+05:00"},
		{"2022-02-16T12:26:38-04:30", "2022-02-16T12:26:38-04:30"},
		{"2022-02-16T12:26:38Z", "2022-02-16T12:26:38+00:00"},
	}
	for _, tc := range testCases {
		original := tc[0]
		expected := tc[1]

		ts, err := time.Parse(time.RFC3339, original)
		require.NoError(t, err)
		require.Equal(t, expected, FormatISO8601(ts))
	}
}
