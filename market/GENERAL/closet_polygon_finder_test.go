package s2

import (
	"sync"
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/units"
)

func makeFinder(points []units.GpsCoords, maxResult int) ClosestPointsFinder {
	finder := NewClosestPointsFinder(maxResult)
	for i, point := range points {
		finder.AddGpsToIndex(point, int64(i+1))
	}
	return finder
}

func makeDefaultFinder(maxResult int) ClosestPointsFinder {
	points := []units.GpsCoords{
		units.GpsCoords{Latitude: 55.746755652728794, Longitude: 37.58262060835406},
		units.GpsCoords{Latitude: 55.74794028553011, Longitude: 37.58242388000314},
		units.GpsCoords{Latitude: 55.74855964604117, Longitude: 37.582515467030305},
	}
	return makeFinder(points, maxResult)
}

func TestFindKClosetFast(t *testing.T) {
	{
		finder := makeDefaultFinder(1)

		//точка близко к 1 Gps координатам
		resp := finder.FindKClosetFast(units.GpsCoords{Latitude: 55.74725778285062, Longitude: 37.582714913105086})
		require.Equal(t, 1, len(resp))
		require.Equal(t, int64(1), resp[0])

		//точка близко ко 2 Gps координатам
		resp = finder.FindKClosetFast(units.GpsCoords{Latitude: 55.747993686477884, Longitude: 37.5821050934505})
		require.Equal(t, 1, len(resp))
		require.Equal(t, int64(2), resp[0])

		//точка совпала с 1 Gps координатой
		resp = finder.FindKClosetFast(units.GpsCoords{Latitude: 55.746755652728794, Longitude: 37.58262060835406})
		require.Equal(t, 1, len(resp))
		require.Equal(t, int64(1), resp[0])
	}
	{
		finder := makeDefaultFinder(2)

		//точка ближе к 3 Gps координатам. Запросили только 2 Gps координаты
		resp := finder.FindKClosetFast(units.GpsCoords{Latitude: 55.748329139566465, Longitude: 37.58228450318945})
		require.Equal(t, 2, len(resp))
		require.Equal(t, []int64{3, 2}, resp)
	}
	{
		finder := makeDefaultFinder(3)

		//точка ближе к 3 Gps координатам. Запросили 3 Gps координаты
		resp := finder.FindKClosetFast(units.GpsCoords{Latitude: 55.748329139566465, Longitude: 37.58228450318945})
		require.Equal(t, 3, len(resp))
		require.Equal(t, []int64{3, 2, 1}, resp)

	}
	{
		finder := makeDefaultFinder(4)

		//точка ближе к 3 Gps координатам. Запросили больше Gps координат, чем есть
		resp := finder.FindKClosetFast(units.GpsCoords{Latitude: 55.748329139566465, Longitude: 37.58228450318945})
		require.Equal(t, 3, len(resp))
		require.Equal(t, []int64{3, 2, 1}, resp)

		//невалидные координаты
		resp = finder.FindKClosetFast(units.GpsCoords{Latitude: 127.7465389475708, Longitude: -190.58164335722864})
		require.Equal(t, 0, len(resp))
		require.Empty(t, resp)
	}

	{
		points := []units.GpsCoords{
			units.GpsCoords{Latitude: 192.746755652728794, Longitude: 123.58262060835406},
			units.GpsCoords{Latitude: -12.74794028553011, Longitude: -180.58242388000314},
			units.GpsCoords{Latitude: 55.74855964604117, Longitude: 37.582515467030305},
		}
		finder := makeFinder(points, 3)

		//Невалидные точки должны отбрасываться при добавлении в индекс

		//1 и 2 точки отбросились как невалидные
		resp := finder.FindKClosetFast(units.GpsCoords{Latitude: 55.748329139566465, Longitude: 37.58228450318945})
		require.Equal(t, 1, len(resp))
		require.Equal(t, []int64{3}, resp)
	}
}

func TestRace(t *testing.T) {
	finder := makeDefaultFinder(1)
	var coord units.GpsCoords

	var wg sync.WaitGroup
	wg.Add(1)
	go func(coord units.GpsCoords) {
		finder.FindKClosetFast(coord)
		wg.Done()
	}(coord)
	finder.FindKClosetFast(coord)
	wg.Wait()
}
