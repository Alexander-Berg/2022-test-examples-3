package haversine

import (
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/units"
)

func Test(t *testing.T) {
	Moscow := units.GpsCoords{Latitude: 55.753274, Longitude: 37.619402}
	Piterx := units.GpsCoords{Latitude: 59.950979, Longitude: 30.317186}
	require.InDelta(t, 635.3, DistanceKm(Moscow, Piterx), 0.1)
}
