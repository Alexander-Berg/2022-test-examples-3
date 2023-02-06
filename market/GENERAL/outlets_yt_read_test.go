package outlets

import (
	"bytes"
	"fmt"
	"strconv"
	"strings"
	"testing"
	"unsafe"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/market/combinator/pkg/units"
	yu "a.yandex-team.ru/market/combinator/pkg/ytutil"
)

func TestReadYtOutlet(t *testing.T) {
	testOutlets := []struct {
		lmsID                     int64
		deliveryServiceID         uint64
		deliveryServiceOutletCode string
		isActive                  bool
		mbiID                     uint64
		pointType                 OutletType
		postCode                  int
	}{
		{10000000001, 50, "MPBR_КДР", false, 1, Depot, 350000},
		{10000000002, 51, "MPBR_КДЖ", true, 2, Post, 630007},
		{10000000003, 52, "MPBR_КДТ", true, 3, Mixed, 196105},
	}

	ous := NewOutletStorage()

	rows := make([]string, 0, len(testOutlets))
	for _, to := range testOutlets {
		rows = append(
			rows,
			fmt.Sprintf(
				`{"lms_id"=%d;"region_id"=213;"gps_coord"="55.752004,37.617734";"delivery_service_id"=%d;"delivery_service_outlet_code"="%s";"is_active"=%%%t;"type"="%s";"post_code"="%s";};`,
				to.lmsID, to.deliveryServiceID, to.deliveryServiceOutletCode, to.isActive, to.pointType.String(), strconv.Itoa(to.postCode)),
		)
	}
	buf := bytes.NewBufferString(strings.Join(rows, "\n"))
	reader, err := yu.NewFileReader2(buf)
	require.NoError(t, err)

	err = ous.readOutlet(reader, nil, false)
	require.NoError(t, err)

	require.Equal(t, len(ous.Outlets), 2)

	for _, expected := range testOutlets {
		_, ok := ous.Outlets[expected.lmsID]
		require.Equal(t, ok, expected.isActive)
		if !ok {
			continue
		}
		out, _ := ous.GetOutlet(expected.lmsID)
		require.Equal(t, expected.lmsID, out.ID)
		require.Equal(t, expected.pointType, out.Type)
		require.Equal(t, uint32(expected.postCode), out.PostCode)
	}
}

func TestMake(t *testing.T) {
	regionMap := geobase.NewExample()
	ous := Make([]Outlet{
		Outlet{
			ID:       1,
			Type:     Post,
			PostCode: 111_111,
			RegionID: geobase.RegionID(213),
			GpsCoords: units.GpsCoords{
				Latitude:  55.752004,
				Longitude: 37.617734,
			},
		},
		Outlet{
			ID:       2,
			Type:     Post,
			PostCode: 111_111,
			RegionID: geobase.RegionID(213),
			GpsCoords: units.GpsCoords{
				Latitude:  55.752004,
				Longitude: 37.617734,
			},
		},
		Outlet{
			ID:       3,
			Type:     PostTerm,
			PostCode: 111_111,
			RegionID: geobase.RegionID(2),
			GpsCoords: units.GpsCoords{
				Latitude:  55.752004,
				Longitude: 37.617734,
			},
		},
	}, &regionMap, nil)
	regions, ok := ous.GetRegionsByPostCode(111_111)
	require.True(t, ok)
	require.Equal(t, []geobase.RegionID{213, 2}, regions)
}

func TestMakeNoRegion(t *testing.T) {
	regionMap := geobase.NewExample()
	ous := Make([]Outlet{
		Outlet{
			ID:       1,
			Type:     Post,
			PostCode: 111_111,
		},
	}, &regionMap, nil)
	_, ok := ous.GetRegionsByPostCode(111_111)
	require.False(t, ok)
}

func TestMakePostCodeRegionsMap(t *testing.T) {
	codeRegionList := []CodeRegion{
		{uint32(222), geobase.RegionID(2)},
		{uint32(222), geobase.RegionID(213)},
		{uint32(222), geobase.RegionID(2)},
		{uint32(111), geobase.RegionID(213)},
		{uint32(111), geobase.RegionID(2)},
		{uint32(111), geobase.RegionID(213)},
	}
	code2regions := makePostCodeRegionsMap(codeRegionList)
	require.Len(t, code2regions, 2)
	require.Equal(t, []geobase.RegionID{213, 2}, code2regions[111])
	require.Equal(t, []geobase.RegionID{2, 213}, code2regions[222])
}

func TestMakeSortedByLatGpsOutlets(t *testing.T) {
	outletsSlice := []Outlet{
		Outlet{
			ID:       1,
			IsActive: true,
			GpsCoords: units.GpsCoords{
				Latitude:  0.3,
				Longitude: 0.3,
			},
		},
		Outlet{
			ID:       2,
			IsActive: true,
			GpsCoords: units.GpsCoords{
				Latitude:  0.2,
				Longitude: 0.2,
			},
		},
		Outlet{
			ID:       3,
			IsActive: true,
			GpsCoords: units.GpsCoords{
				Latitude:  0.1,
				Longitude: 0.1,
			},
		},
	}
	sortOutletsByLat(outletsSlice)
	// Аутлет с неопределенной координатой должен быть отброшен
	require.Len(t, outletsSlice, 3)
	// Проверка сортировки по первой координате
	require.Equal(t, outletsSlice[0].ID, int64(3))
	require.Equal(t, outletsSlice[1].ID, int64(2))
	require.Equal(t, outletsSlice[2].ID, int64(1))
}

func TestReadReset(t *testing.T) {
	table := []string{
		`{"lms_id"=1;"region_id"=213;"gps_coord"="55.752004,37.617734";"is_active"=%true;"is_market_branded"=%false;};`,
		`{"lms_id"=2;"region_id"=213;"gps_coord"="55.752004,37.617734";"is_active"=%true;"is_market_branded"=%true;};`,
		`{"lms_id"=3;"region_id"=213;"gps_coord"="55.752004,37.617734";"is_active"=%true;"is_market_branded"=%false;};`,
	}
	buf := bytes.NewBufferString(strings.Join(table, "\n"))
	reader, err := yu.NewFileReader2(buf)
	require.NoError(t, err)

	ous := NewOutletStorage()
	err = ous.readOutlet(reader, nil, false)
	require.NoError(t, err)
	require.Equal(t, 3, len(ous.OutletsSlice))

	o, _ := ous.GetOutlet(1)
	require.False(t, o.IsMarketBranded)
	o, _ = ous.GetOutlet(2)
	require.True(t, o.IsMarketBranded)
	o, _ = ous.GetOutlet(3)
	require.False(t, o.IsMarketBranded)
}

func TestReadWithoutDSBS(t *testing.T) {
	table := []string{
		`{"lms_id"=1;"region_id"=213;"gps_coord"="55.752004,37.617734";"is_active"=%true;"is_market_branded"=%false;};`,
		`{"lms_id"=2;"region_id"=213;"gps_coord"="55.752004,37.617734";"is_active"=%true;"is_market_branded"=%true;};`,
		`{"lms_id"=3;"region_id"=213;"gps_coord"="55.752004,37.617734";"is_active"=%true;"is_market_branded"=%true;"dsbs_point_id"=123};`,
		`{"lms_id"=4;"region_id"=213;"gps_coord"="55.752004,37.617734";"is_active"=%true;"is_market_branded"=%false;};`,
		`{"lms_id"=5;"region_id"=213;"gps_coord"="55.752004,37.617734";"is_active"=%true;"is_market_branded"=%false;"dsbs_point_id"=456};`,
	}
	buf := bytes.NewBufferString(strings.Join(table, "\n"))
	reader, err := yu.NewFileReader2(buf)
	require.NoError(t, err)

	ous := NewOutletStorage()
	err = ous.readOutlet(reader, nil, true)
	require.NoError(t, err)
	require.Equal(t, 3, len(ous.OutletsSlice))

	o, _ := ous.GetOutlet(1)
	require.False(t, o.IsMarketBranded)
	o, _ = ous.GetOutlet(2)
	require.True(t, o.IsMarketBranded)
	o, _ = ous.GetOutlet(4)
	require.False(t, o.IsMarketBranded)
}

func TestMemSize(t *testing.T) {
	require.Equal(t, 96, int(unsafe.Sizeof(Outlet{})))
}
