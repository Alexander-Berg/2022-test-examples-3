package logging

import (
	"fmt"
	"testing"
	"time"

	"github.com/stretchr/testify/require"
)

func TestDurationMsFormat(t *testing.T) {
	const duration = time.Second
	const expectedKey = "duration_ms"
	expectedValue := duration.Milliseconds()

	actual := DurationMs(duration)

	require.Equal(t, expectedKey, actual.Key)
	require.Equal(t, expectedValue, actual.Integer)
}

func TestTsumEventFormat(t *testing.T) {
	const expectedName = "parsing"
	expectedKey := fmt.Sprintf("event.%s", expectedName)
	expectedValue := time.Now().UnixNano() / int64(time.Millisecond)

	actual := TsumEvent(expectedName, time.Now())

	require.Equal(t, expectedKey, actual.Key)
	require.Equal(t, expectedValue, actual.Integer)
}

func TestTestIDs(t *testing.T) {
	const expectedKey = "test_ids"
	const expectedValue = "1,2,3"

	actual := TestIDs(1, 2, 3)

	require.Equal(t, expectedKey, actual.Key)
	require.Equal(t, expectedValue, actual.String)
}
