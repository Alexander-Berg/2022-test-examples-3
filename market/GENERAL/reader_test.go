package export

import (
	"bytes"
	"testing"
	"time"

	"github.com/golang/geo/s2"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/express"
	"a.yandex-team.ru/market/combinator/pkg/units"
	"a.yandex-team.ru/market/combinator/pkg/ytutil"
)

func TestCalcDeliveryIntervalStart(t *testing.T) {
	// yt read --proxy hahn --format '<format=text>yson' '//home/market/production/indexer/combinator/export/express_regions/recent[:#1]'
	yson := (`{"Warehouse"=224;"Regions"=[1u;];"ExpressZones"=[` +
		`{"ZoneId"=1;"DeliveryDay"=0;"DeliveryInterval"={"From"={"Hour"=12u;"Minute"=40u;};"To"={"Hour"=14u;"Minute"=5u;};};};` +
		`{"ZoneId"=2;"DeliveryDay"=1;"DeliveryInterval"={"From"={"Hour"=12u;"Minute"=55u;};"To"={"Hour"=14u;"Minute"=25u;};};};` +
		`{"ZoneId"=406;"DeliveryDay"=0;"DeliveryInterval"={"From"={"Hour"=13u;"Minute"=0u;};"To"={"Hour"=14u;"Minute"=30u;};};};` +
		`{"ZoneId"=408;"DeliveryDay"=1;"DeliveryInterval"={"From"={"Hour"=13u;"Minute"=0u;};"To"={"Hour"=14u;"Minute"=30u;};};};` +
		`{"ZoneId"=209;"DeliveryDay"=0;"DeliveryInterval"={"From"={"Hour"=13u;"Minute"=20u;};"To"={"Hour"=14u;"Minute"=30u;};};};];};`)

	buf := bytes.NewBufferString(yson)
	reader, err := ytutil.NewFileReader2(buf)
	require.NoError(t, err)

	warehouses := map[int64]*express.Warehouse{
		224: &express.Warehouse{
			PartnerID:  224,
			BusinessID: 10,
			RegionID:   1,
			Coord: units.GpsCoords{
				Latitude:  55.774532442174162838,
				Longitude: 37.632735922046940402,
			},
			LatLng:               s2.LatLngFromDegrees(55.774532442174162838, 37.632735922046940402),
			AddTimeToDelivery:    time.Duration(40) * time.Minute,
			Durations:            make(express.DistanceDurationList, 1),
			EnableExpressOutlets: true,
		},
	}

	express := &express.Express{
		Warehouses: warehouses,
	}
	expressRegions, err := read(reader, express)
	require.NoError(t, err)

	warehousesInfo, ok := expressRegions.Warehouses[1]
	require.Equal(t, true, ok)
	require.Equal(t, 1, len(warehousesInfo))

	zones := map[int64]int32{
		1:   760,
		2:   2215,
		209: 800,
		406: 780,
		408: 2220,
	}

	for _, whInfo := range warehousesInfo {
		require.Equal(t, len(zones), len(whInfo.Zones))
		for zoneID, deliveryIntervalStart := range zones {
			zone, ok := whInfo.Zones[zoneID]
			require.Equal(t, true, ok)
			require.Equal(t, deliveryIntervalStart, zone.GetDeliveryIntervalStart())
		}
	}
}
