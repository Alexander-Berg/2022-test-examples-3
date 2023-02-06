package eats

import (
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/units"
)

func TestEatsShopToWarehouse(t *testing.T) {
	data := `{"shop_id":3425345,"warehouse_id":48578}
{"shop_id":3425345,"warehouse_id":48579}
{"shop_id":1,"warehouse_id":2,"extra_field":"test"}
`
	eatsShops, err := ReadFromString(data)
	require.NoError(t, err)
	require.Len(t, eatsShops.ShopIDToEatsWarehouseID, 2)
	{
		warehouseID, ok := eatsShops.GetWarehouseIDByShopID(3425345)
		require.True(t, ok)
		require.Equal(t, int64(48579), warehouseID)
	}
	{
		warehouseID, ok := eatsShops.GetWarehouseIDByShopID(1)
		require.True(t, ok)
		require.Equal(t, int64(2), warehouseID)
	}
}

func TestEatsWithMock(t *testing.T) {
	gpsCoords := &units.GpsCoords{
		Latitude:  0,
		Longitude: 0,
	}

	resp, err := FetchEatsWarehouses(nil, *gpsCoords, nil, nil, nil, nil)
	require.NoError(t, err)
	require.NotNil(t, resp)
	require.Empty(t, resp.EatsWarehouses)

	// some non-nil values => return mock response without warehouses
	gpsCoords.Latitude = 55
	gpsCoords.Longitude = 36
	shops := &Shops{ShopIDToEatsWarehouseID: map[int64]int64{}}
	resp, err = FetchEatsWarehouses(nil, *gpsCoords, nil, shops, nil, nil)
	require.NoError(t, err)
	require.NotNil(t, resp)
	require.NotEmpty(t, resp.EatsWarehouses)

	// set a test warehouses map
	shops = &Shops{ShopIDToEatsWarehouseID: map[int64]int64{1719669: 1337}}
	resp, err = FetchEatsWarehouses(nil, *gpsCoords, nil, shops, nil, nil)
	require.NoError(t, err)
	require.NotNil(t, resp)
	require.NotEmpty(t, resp.EatsWarehouses)
	require.Equal(t, int64(1337), resp.EatsWarehouses[0].WarehouseId)
}
