package s2

import (
	"testing"

	"github.com/golang/geo/s2"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/units"
)

func TestDistance(t *testing.T) {
	Moscow := units.GpsCoords{Latitude: 55.753274, Longitude: 37.619402}
	MoscowHistoryMuseum := units.GpsCoords{Latitude: 55.755246, Longitude: 37.617779}
	Piterx := units.GpsCoords{Latitude: 59.950979, Longitude: 30.317186}

	require.InDelta(t, 635300, DistanceM(Moscow, Piterx), 100)
	require.InDelta(t, 635.3, DistanceKm(Moscow, Piterx), 0.1)

	MoscowLatLng := s2.LatLngFromDegrees(Moscow.Latitude, Moscow.Longitude)
	PiterxLatLng := s2.LatLngFromDegrees(Piterx.Latitude, Piterx.Longitude)

	require.InDelta(t, 635300, FastDistanceM(MoscowLatLng, PiterxLatLng), 100)
	require.InDelta(t, 635.3, FastDistanceKm(MoscowLatLng, PiterxLatLng), 0.1)

	require.True(t, IsNear(Moscow, Moscow, 1)) // Kremlin is near Kremlin (less than 1 m)

	require.False(t, IsNear(Moscow, MoscowHistoryMuseum, 241))
	require.False(t, IsNear(MoscowHistoryMuseum, Moscow, 241))

	require.True(t, IsNear(Moscow, MoscowHistoryMuseum, 243))
	require.True(t, IsNear(MoscowHistoryMuseum, Moscow, 243))

	require.False(t, IsNear(Moscow, Piterx, 10000))
	require.False(t, IsNear(Piterx, Moscow, 10000))
}
