package configs

import (
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/configs/stocktype"
	"github.com/stretchr/testify/require"
	"testing"
)

func TestOptions_IsAllowedWarehouseID(t *testing.T) {
	o := Options{
		GunConfig: GunConfig{
			WarehouseIDs: map[string][]int{
				stocktype.Fulfillment.String():         {1, 2},
				stocktype.FulfillmentCashback.String(): {1, 2},
				stocktype.Dropship.String():            {},
				stocktype.DropshipCashback.String():    {},
			},
		},
	}

	require.True(t, o.IsAllowedWarehouseID(2, stocktype.Fulfillment, stocktype.FulfillmentCashback))
	require.True(t, o.IsAllowedWarehouseID(1, stocktype.Fulfillment, stocktype.FulfillmentCashback))
	require.False(t, o.IsAllowedWarehouseID(-1, stocktype.Fulfillment, stocktype.FulfillmentCashback))
	require.False(t, o.IsAllowedWarehouseID(3, stocktype.Fulfillment, stocktype.FulfillmentCashback))

	require.True(t, o.IsAllowedWarehouseID(2, stocktype.Dropship, stocktype.DropshipCashback))
	require.True(t, o.IsAllowedWarehouseID(1, stocktype.Dropship, stocktype.DropshipCashback))
	require.True(t, o.IsAllowedWarehouseID(-1, stocktype.Dropship, stocktype.DropshipCashback))
	require.True(t, o.IsAllowedWarehouseID(3, stocktype.Dropship, stocktype.DropshipCashback))

}
