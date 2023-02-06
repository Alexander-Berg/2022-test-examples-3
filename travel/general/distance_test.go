package geo

import (
	"testing"

	"github.com/stretchr/testify/require"
)

func TestGetDistanceKM(t *testing.T) {
	t.Run("Moscow-Yekaterinburg", func(t *testing.T) {
		moscowLat, moscowLon := 55.753676, 37.619899
		yekaterinburgLat, yekaterinburgLon := 56.838607, 60.605514
		expectedDistance := 1416.639

		distance := GetDistanceKM(moscowLat, moscowLon, yekaterinburgLat, yekaterinburgLon)

		require.InDelta(t, distance, expectedDistance, 0.01)
	})
}
