package outlets

import (
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/market/combinator/pkg/geometry/s2"
	"a.yandex-team.ru/market/combinator/pkg/units"
)

func TestGetClosestOutlets(t *testing.T) {
	testOutlets := func(t *testing.T, req []*Outlet, resp []*Outlet) {
		require.NotEqual(t, req, nil)
		require.Equal(t, len(req), len(resp))
		for i := range req {
			require.Equal(t, req[i], resp[i])
		}
	}

	outlets := []Outlet{
		{
			ID:                         1,
			GpsCoords:                  units.GpsCoords{Latitude: 55.746755652728794, Longitude: 37.58262060835406},
			DeferredCourierRadius:      200,
			IsDeferredCourierAvailable: true,
		},
		{
			ID:                         2,
			GpsCoords:                  units.GpsCoords{Latitude: 55.74794028553011, Longitude: 37.58242388000314},
			DeferredCourierRadius:      100,
			IsDeferredCourierAvailable: true,
		},
		{
			ID:                         3,
			GpsCoords:                  units.GpsCoords{Latitude: 55.74855964604117, Longitude: 37.582515467030305},
			DeferredCourierRadius:      100,
			IsDeferredCourierAvailable: true,
		},
		{ //Невалидная точка. Должна отброситься
			ID:                         4,
			GpsCoords:                  units.GpsCoords{Latitude: 91.74855964604117, Longitude: -180.582515467030305},
			DeferredCourierRadius:      1000000000,
			IsDeferredCourierAvailable: true,
		},
	}
	regionMap := geobase.NewExample()
	os := Make(outlets, &regionMap, nil)
	require.NotEqual(t, nil, os)
	{
		//точка попадает в радиус всех 3-х аутлетов
		res := os.GetClosestOutlets(units.GpsCoords{Latitude: 55.747812395387584, Longitude: 37.58255832601})
		testOutlets(t, []*Outlet{&outlets[1], &outlets[2], &outlets[0]}, res)
	}
	{
		//точка вне радуиса 3-его аутлето, но в радиусе первых 2-х
		res := os.GetClosestOutlets(units.GpsCoords{Latitude: 55.7471781202387, Longitude: 37.58229546952653})
		testOutlets(t, []*Outlet{&outlets[0], &outlets[1]}, res)
	}
	{
		//точка совпадает с 3-м аутлетом. Не в радиусе 1 аутлета.
		res := os.GetClosestOutlets(units.GpsCoords{Latitude: 55.74855964604117, Longitude: 37.582515467030305})
		testOutlets(t, []*Outlet{&outlets[2], &outlets[1]}, res)
	}
	{
		//точка вышла за радуис 1 аутлета на 3-4 метра. Ни в радуисе ни одного аутлета.
		res := os.GetClosestOutlets(units.GpsCoords{Latitude: 55.74499107587273, Longitude: 37.58338928785989})
		testOutlets(t, []*Outlet{}, res)
	}
	{
		//точка в Антарктиде
		res := os.GetClosestOutlets(units.GpsCoords{Latitude: -72.81072668780259, Longitude: 27.67442573211056})
		testOutlets(t, []*Outlet{}, res)
	}
	{
		//невалидные координаты на входе
		res := os.GetClosestOutlets(units.GpsCoords{Latitude: -112.81072668780259, Longitude: 180.67442573211056})
		testOutlets(t, []*Outlet{}, res)
	}
}

func TestMkadPolygon(t *testing.T) {
	spb, err := s2.NewSimplePolygon(MkadPolygon)
	require.NoError(t, err)
	require.True(t, spb.ContainsPoint(units.GpsCoords{
		Latitude:  55.755819,
		Longitude: 37.617644,
	}))
	require.False(t, spb.ContainsPoint(units.GpsCoords{
		Latitude:  59.939099,
		Longitude: 30.315877,
	}))
}

func TestSpbPolygon(t *testing.T) {
	spb, err := s2.NewSimplePolygon(spbPolygon)
	require.NoError(t, err)
	require.True(t, spb.ContainsPoint(units.GpsCoords{
		Latitude:  59.939099,
		Longitude: 30.315877,
	}))
	require.False(t, spb.ContainsPoint(units.GpsCoords{
		Latitude:  55.755819,
		Longitude: 37.617644,
	}))
}
