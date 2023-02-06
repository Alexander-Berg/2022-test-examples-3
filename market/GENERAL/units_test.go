package units

import (
	"testing"

	"github.com/stretchr/testify/require"
)

func TestDimensions(t *testing.T) {
	d321 := NewDimensions(2, 3, 1)
	require.Equal(t, Dimensions{1, 2, 3}, d321)

	d422 := NewDimensions(2, 2, 4)
	d222 := NewDimensions(2, 2, 2)
	require.True(t, d321.Contains(&d321))
	require.True(t, d422.Contains(&d321))
	require.False(t, d222.Contains(&d321))
}

// TestGpsToPixelXY проверяет получение пиксельных координат по GPS координатам
func TestGpsToPixelXY(t *testing.T) {
	testGps := &GpsCoords{
		Latitude:  55.751667,
		Longitude: 37.617778,
	}
	testZoom := uint8(10)

	pixelX, pixelY := latLongToPixelXY(testGps.Latitude, testGps.Longitude, testZoom)
	require.Equal(t, int64(158464), pixelX)
	require.Equal(t, int64(81952), pixelY)

	lat, lon := pixelXYToLatLong(pixelX, pixelY, testZoom)
	result := &GpsCoords{
		Latitude:  lat,
		Longitude: lon,
	}
	require.True(t, testGps.Equal(result, 0.001))
}

// TestGpsAndZoomToQuadCode проверяет преобразование тайловых координат по зуму в QuadCode и обратно
func TestGpsAndZoomToQuadCode(t *testing.T) {
	testGps := &GpsCoords{
		Latitude:  55.751667,
		Longitude: 37.617778,
	}
	testZoom := uint8(20)
	pixelX, pixelY := latLongToPixelXY(testGps.Latitude, testGps.Longitude, testZoom)
	tileX, tileY := pixelXYToTileXY(pixelX, pixelY)
	quadCode := tileXYToQuadKey(tileX, tileY, testZoom)
	resultTileX, resultTileY, resultZoom := quadKeyToTileXY(quadCode)
	require.Equal(t, resultTileX, tileX)
	require.Equal(t, resultTileY, tileY)
	require.Equal(t, resultZoom, testZoom)
	quadCode = tileXYToQuadKey(0, 0, 1)
	require.Equal(t, uint64(0), quadCode.Key)
	quadCode = tileXYToQuadKey(1, 0, 1)
	require.Equal(t, uint64(0x100000000000), quadCode.Key)
	quadCode = tileXYToQuadKey(2, 0, 2)
	require.Equal(t, uint64(0x100000000000), quadCode.Key)
	quadCode = tileXYToQuadKey(3, 0, 2)
	require.Equal(t, uint64(0x140000000000), quadCode.Key)
}

// TestAdjustZoom проверяет получение quadKey с кода детализированного зума к менее детализированному
func TestAdjustZoom(t *testing.T) {
	// Точки расположены достаточно близко, чтобы на некотором зуме находиться в одном тайле
	testGps1 := GpsCoords{
		Latitude:  55.0001,
		Longitude: 37.0001,
	}
	testGps2 := GpsCoords{
		Latitude:  55.0001,
		Longitude: 37.0002,
	}
	testZoom := uint8(23)
	qk1 := GpsToQuadKey(testGps1, testZoom)
	qk2 := GpsToQuadKey(testGps2, testZoom)
	require.Equal(t, uint64(0x18d12eb579b8), qk1.Key)
	require.Equal(t, uint64(0x18d12eb579bd), qk2.Key)
	require.Equal(t, testZoom, qk1.Zoom)
	require.Equal(t, testZoom, qk2.Zoom)
	// Начиная с некоторого зума, координаты попадают в один тайл
	adjustetQk1 := adjustQuadKeyToZoom(qk1, 20)
	adjustetQk2 := adjustQuadKeyToZoom(qk2, 20)
	require.Equal(t, adjustetQk1, adjustetQk2)
	require.Equal(t, uint64(0x18d12eb57980), adjustetQk1.Key)
	require.Equal(t, uint8(20), adjustetQk1.Zoom)
	require.Equal(t, qk1.Key&adjustetQk1.Key, adjustetQk1.Key)
	adjustetQk1 = adjustQuadKeyToZoom(qk1, 1)
	require.Equal(t, uint64(0x100000000000), adjustetQk1.Key)
	require.Equal(t, uint8(1), adjustetQk1.Zoom)
}
