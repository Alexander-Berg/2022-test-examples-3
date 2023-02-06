package deferredcourier

import (
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/geometry/s2"
	"a.yandex-team.ru/market/combinator/pkg/units"
)

func requireHardcodedPolygons(t *testing.T, polygons map[int64][]units.GpsCoords) {
	for pointLmsID, vertices := range polygons {
		polygon, err := s2.NewSimplePolygon(vertices)
		require.NoError(t, err, pointLmsID)
		require.NotNil(t, polygon, pointLmsID)

		for _, v := range vertices {
			require.True(t, polygon.ContainsPoint(v))
			require.NotPanics(t, func() {
				polygon.ContainsPoint(units.GpsCoords{
					Latitude:  42.0,
					Longitude: 42.0,
				})
			})
		}
	}
}

func TestHardcodedPolygonsTesting(t *testing.T) {
	requireHardcodedPolygons(t, polygonsTesting)
}

func TestHardcodedPolygonsProduction(t *testing.T) {
	requireHardcodedPolygons(t, polygonsProduction)
}

func testPolygonSpecificPoints(t *testing.T, polygons polygonStorage, points map[int64]units.GpsCoords, expected bool) {
	for pointsLmsID, point := range points {
		polygon, ok := polygons[pointsLmsID]
		require.True(t, ok, pointsLmsID)
		require.Equal(t, expected, polygon.ContainsPoint(point), pointsLmsID)
	}
}

func TestPolygonSpecificPointsProduction(t *testing.T) {
	// https://yql.yandex-team.ru/Operations/YO2uEvMBw-ULBR_2OBG0CloEsg1ZxzEv20bRgFQ_LiU=
	testPolygonSpecificPoints(t, yGoWarehousesProduction, map[int64]units.GpsCoords{
		10001792268: {Latitude: 55.736873, Longitude: 37.63606},
		10001792270: {Latitude: 55.737811, Longitude: 37.57165},
		10001792281: {Latitude: 55.759106, Longitude: 37.548788},
		10001792283: {Latitude: 55.750687, Longitude: 37.59745},
		10001792285: {Latitude: 55.647452, Longitude: 37.723313},
		10001792287: {Latitude: 55.744992, Longitude: 37.615066},
		10001792289: {Latitude: 55.778406, Longitude: 37.493299},
		10001792293: {Latitude: 55.759304, Longitude: 37.579601},
		10001792295: {Latitude: 55.657687, Longitude: 37.771535},
		10001792296: {Latitude: 55.778482, Longitude: 37.691809},
		10001792298: {Latitude: 55.771028, Longitude: 37.632745},
		10001792299: {Latitude: 55.767392, Longitude: 37.559415},
		10001792301: {Latitude: 55.743598, Longitude: 37.545653},
		10001792303: {Latitude: 55.771773, Longitude: 37.603424},
		10001792304: {Latitude: 55.7559, Longitude: 37.650918},
		10001792305: {Latitude: 55.761411, Longitude: 37.636473},
		10001792306: {Latitude: 55.653787, Longitude: 37.608059},
		10001792307: {Latitude: 55.769367, Longitude: 37.684066},
		10001792308: {Latitude: 55.738389, Longitude: 37.655257},
		10001792310: {Latitude: 55.725326, Longitude: 37.567051},
		10001800222: {Latitude: 55.733472, Longitude: 37.682062},
		10001800223: {Latitude: 55.764455, Longitude: 37.669845},
		10001800225: {Latitude: 55.785321, Longitude: 37.572118},
		10001800230: {Latitude: 55.706568, Longitude: 37.597082},
		10001800231: {Latitude: 55.775677, Longitude: 37.64321},
		10001800232: {Latitude: 55.718202, Longitude: 37.672855},
		10001800233: {Latitude: 55.755079, Longitude: 37.661141},
		10001800235: {Latitude: 55.71878, Longitude: 37.646139},
		10001800240: {Latitude: 55.70561, Longitude: 37.61565},
		10001800242: {Latitude: 55.787553, Longitude: 37.616144},
		10001800245: {Latitude: 55.715657, Longitude: 37.621929},
		10001800247: {Latitude: 55.784536, Longitude: 37.600361},
		10001800259: {Latitude: 55.748235, Longitude: 37.678092},
		10001800263: {Latitude: 55.757496, Longitude: 37.610125},
		10001800266: {Latitude: 55.724667, Longitude: 37.63738},
	}, true)

	for pointsLmsID, polygon := range yGoWarehousesProduction {
		require.Equal(
			t,
			false,
			polygon.ContainsPoint(units.GpsCoords{Latitude: 55.821063, Longitude: 37.643093}), // ВДНХ
			pointsLmsID,
		)
	}
}
