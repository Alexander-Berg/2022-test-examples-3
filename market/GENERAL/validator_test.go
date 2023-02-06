package graph

import (
	"strings"
	"testing"

	"github.com/stretchr/testify/require"
)

func TestCheckInEpsilon(t *testing.T) {
	{
		expected, actual, epsilon := 10.0, 9.0, 0.1
		require.NoError(t, checkInEpsilon(expected, actual, epsilon))
	}
	{
		expected, actual, epsilon := -10.0, -9.0, 0.1
		require.NoError(t, checkInEpsilon(expected, actual, epsilon))
	}
	{
		expected, actual, epsilon := 10.0, 9.0, 0.05
		err := checkInEpsilon(expected, actual, epsilon)
		require.Error(t, err)
		require.True(t, strings.HasPrefix(err.Error(), "relative error is too high"))
	}
}
