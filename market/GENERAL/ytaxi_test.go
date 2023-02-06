package httpclient

import (
	"fmt"
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/units"
)

func TestResourceFeatureMarshaling(t *testing.T) {
	const featureJSONTemplate = `{"class_name":"physical","resource_physical_feature":{"weight":%d,"d_x":%d,"d_y":%d,"d_z":%d,"rotatable":%t}}`

	feature := ResourceFeature{
		Weight:     1000,
		Dimensions: [3]uint32{100, 70, 50},
		CargoTypes: []int32{100, 200},
	}

	expectedFeatureJSON := fmt.Sprintf(
		featureJSONTemplate,
		feature.Weight,
		feature.Dimensions[0],
		feature.Dimensions[1],
		feature.Dimensions[2],
		true,
	)

	actualFeatureJSON, err := feature.MarshalJSON()
	require.NoError(t, err)
	require.Equal(t, expectedFeatureJSON, string(actualFeatureJSON))
}

func TestWeightAndDimensionsMarshaling(t *testing.T) {
	const wadJSONTemplate = `{"weight":%d,"dx":%d,"dy":%d,"dz":%d}`

	wad := WeightAndDimensions{
		Weight:     1000,
		Dimensions: [3]uint32{100, 70, 50},
	}

	expectedWadJSON := fmt.Sprintf(
		wadJSONTemplate,
		wad.Weight,
		wad.Dimensions[0],
		wad.Dimensions[1],
		wad.Dimensions[2],
	)

	actualWadJSON, err := wad.MarshalJSON()
	require.NoError(t, err)
	require.Equal(t, expectedWadJSON, string(actualWadJSON))
}

func TestStationInfoMarshaling(t *testing.T) {
	gps := units.GpsCoords{Latitude: 55.753274, Longitude: 37.619402}
	from := time.Date(2021, time.August, 18, 12, 0, 0, 0, time.UTC)
	to := time.Date(2021, time.August, 9, 13, 0, 0, 0, time.UTC)

	// custom_location
	{
		cl := NewCustomLocationInfo(gps, from, to)

		expectedClJSON := fmt.Sprintf(
			`{"type":"custom_location","custom_location":{"latitude":%f,"longitude":%f},"interval":{"from":%d,"to":%d}}`,
			gps.Latitude,
			gps.Longitude,
			from.Unix(),
			to.Unix(),
		)

		actualClJSON, err := cl.MarshalJSON()
		require.NoError(t, err)
		require.Equal(t, expectedClJSON, string(actualClJSON))
	}

	// platform_station
	{
		pointLmsID := int64(10000000000)
		es := NewExternalStationInfo(
			pointLmsID,
			true,
			gps,
			from,
			to,
		)

		expectedEsJSON := fmt.Sprintf(
			`{"type":"market_station","market_station":{"lms_id":%d},"interval":{"from":%d,"to":%d}}`,
			pointLmsID,
			from.Unix(),
			to.Unix(),
		)

		actualEsJSON, err := es.MarshalJSON()
		require.NoError(t, err)
		require.Equal(t, expectedEsJSON, string(actualEsJSON))
	}
}
