package s2

import (
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/units"
)

var testPolygonVertices []units.GpsCoords

func init() {
	testPolygonVertices = []units.GpsCoords{
		{
			Latitude:  55.752221,
			Longitude: 37.581345,
		},
		{
			Latitude:  55.749661,
			Longitude: 37.586618,
		},
		{
			Latitude:  55.751185,
			Longitude: 37.585424,
		},
		{
			Latitude:  55.752228,
			Longitude: 37.588494,
		},
	}
}

func TestPointInPolygon(t *testing.T) {
	polygon, err := NewSimplePolygon(testPolygonVertices)
	require.NoError(t, err)
	for i, point := range []units.GpsCoords{
		{
			Latitude:  55.751587,
			Longitude: 37.584168,
		},
		{
			Latitude:  55.750324,
			Longitude: 37.585667,
		},
		{
			Latitude:  55.752047,
			Longitude: 37.586753,
		},
		{
			Latitude:  55.752109,
			Longitude: 37.582064,
		},
	} {
		require.True(t, polygon.ContainsPoint(point), i)
	}
}

func TestPointOutsidePolygon(t *testing.T) {
	polygon, err := NewSimplePolygon(testPolygonVertices)
	require.NoError(t, err)
	for i, point := range []units.GpsCoords{
		{
			Latitude:  55.751003,
			Longitude: 37.586542,
		},
		{
			Latitude:  55.752731,
			Longitude: 37.584606,
		},
		{
			Latitude:  55.750423,
			Longitude: 37.581433,
		},
		{
			Latitude:  55.750423,
			Longitude: 37.588865,
		},
	} {
		require.False(t, polygon.ContainsPoint(point), i)
	}
}

func TestPointOnPolygonVertex(t *testing.T) {
	polygon, err := NewSimplePolygon(testPolygonVertices)
	require.NoError(t, err)
	for i, point := range testPolygonVertices {
		require.True(t, polygon.ContainsPoint(point), i)
	}
}

func TestDegeneratePolygon(t *testing.T) {
	vertices := []units.GpsCoords{
		{
			Latitude:  55.750875,
			Longitude: 37.583447,
		},
		{
			Latitude:  55.751680,
			Longitude: 37.583624,
		},
	}
	_, err := NewSimplePolygon(vertices)
	require.Errorf(t, err, "expected at least 3 vertices, but got 2")
}
