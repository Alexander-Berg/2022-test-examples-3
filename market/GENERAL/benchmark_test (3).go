package haversine

import (
	"testing"

	"a.yandex-team.ru/market/combinator/pkg/geometry/s2"
	"a.yandex-team.ru/market/combinator/pkg/units"
)

func BenchmarkDistance(b *testing.B) {
	Moscow := units.GpsCoords{Latitude: 55.753274, Longitude: 37.619402}
	Piterx := units.GpsCoords{Latitude: 59.950979, Longitude: 30.317186}

	b.Run("DistanceKm", func(b *testing.B) {
		for i := 0; i < b.N; i++ {
			_ = DistanceKm(Moscow, Piterx)
		}
	})
	b.Run("s2.DistanceM", func(b *testing.B) {
		for i := 0; i < b.N; i++ {
			_ = s2.DistanceM(Moscow, Piterx)
		}
	})
}
