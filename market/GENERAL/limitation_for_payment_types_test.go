package hardconfig

import (
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/enums"
	"a.yandex-team.ru/market/combinator/pkg/geobase"
)

func TestDecodeLimitationForPaymentTypes(t *testing.T) {
	limitation, err := NewLimitationForPaymentTypes("limitation_test.yaml")
	require.NoError(t, err, "decode from to limitation err")
	require.NotNil(t, limitation, "limitation is empty")

	pickup := "PICKUP"
	maxPrice0 := uint32(0)
	maxPrice20 := uint32(20)
	regionSPB := geobase.RegionID(geobase.RegionSaintPetersburg)
	needValues := []LimitationForPaymentTypes{
		{
			PartnerID:        1003937,
			MaxPostpaidPrice: &maxPrice20,
		},
		{
			PartnerID:       1003937,
			MaxPrepaidPrice: &maxPrice20,
		},
		{
			PartnerID:        1003937,
			Location:         []geobase.RegionID{regionSPB},
			MaxPrepaidPrice:  &maxPrice20,
			MaxPostpaidPrice: &maxPrice0,
		},
		{
			PartnerID:            1003937,
			LogisticPoints:       []int64{1000556473830},
			DeliveryType:         &pickup,
			deliveryType:         enums.DeliveryMethodPickup,
			DisabledPaymentTypes: []string{"CARD_ALLOWED"},
			disabledPaymentTypes: enums.MethodCardAllowed,
		},
	}

	require.Contains(t, limitation, uint64(1003937))

	for i, limitationItem := range limitation[1003937] {
		if i <= 3 {
			continue
		}
		require.Equal(t, limitationItem, needValues[i])
	}
}

func TestLimitationForPaymentTypesFilter(t *testing.T) {
	limitation, _ := NewLimitationForPaymentTypes("limitation_test.yaml")
	require.NotNil(t, limitation, "limitation is empty")

	f := HardConfig{
		LimitationForPaymentTypes: limitation,
	}

	result := f.LimitationForPaymentTypes.Filter(1003937, geobase.RegionChain{
		geobase.Region{ID: geobase.RegionHamovniki},
	}, nil, enums.DeliveryMethodUnknown, 10_000, enums.MethodCashAllowed)
	require.Equal(t, result, enums.MethodCashAllowed)

	result = f.LimitationForPaymentTypes.Filter(1003937, geobase.RegionChain{
		geobase.Region{ID: geobase.RegionHamovniki},
	}, nil, enums.DeliveryMethodUnknown, 20_000, enums.MethodCashAllowed)
	require.Equal(t, result, enums.MethodCashAllowed)

	result = f.LimitationForPaymentTypes.Filter(1003937, geobase.RegionChain{
		geobase.Region{ID: geobase.RegionHamovniki},
	}, nil, enums.DeliveryMethodUnknown, 30_000, enums.MethodCashAllowed|enums.MethodPrepayAllowed|enums.MethodCardAllowed)
	require.Equal(t, result, enums.MethodUnknown)

	result = f.LimitationForPaymentTypes.Filter(1003937, geobase.RegionChain{
		geobase.Region{ID: geobase.RegionSaintPetersburg},
	}, nil, enums.DeliveryMethodUnknown, 10_000, enums.MethodCashAllowed|enums.MethodPrepayAllowed|enums.MethodCardAllowed)
	require.Equal(t, result, enums.MethodPrepayAllowed)

	result = f.LimitationForPaymentTypes.Filter(1003937, geobase.RegionChain{
		geobase.Region{ID: geobase.RegionHamovniki},
	}, nil, enums.DeliveryMethodUnknown, 10_000, enums.MethodCashAllowed|enums.MethodPrepayAllowed|enums.MethodCardAllowed)
	require.Equal(t, result, enums.MethodCashAllowed|enums.MethodPrepayAllowed|enums.MethodCardAllowed)

	result = f.LimitationForPaymentTypes.Filter(1003937, geobase.RegionChain{
		geobase.Region{ID: geobase.RegionHamovniki},
	}, &logisticPoint, enums.DeliveryMethodPickup, 10_000, enums.MethodCashAllowed|enums.MethodPrepayAllowed|enums.MethodCardAllowed)
	require.Equal(t, result, enums.MethodCashAllowed|enums.MethodPrepayAllowed)
}

func TestLimitationForPaymentTypesAllowedList(t *testing.T) {
	limitation, _ := NewLimitationForPaymentTypes("limitation_test.yaml")
	require.NotNil(t, limitation, "limitation is empty")

	f := HardConfig{
		LimitationForPaymentTypes: limitation,
	}

	result := f.LimitationForPaymentTypes.GetGlobalAllowedPaymentMethods(55741)
	require.Equal(t, result, enums.MethodCardAllowed|enums.MethodPrepayAllowed)

	result = f.LimitationForPaymentTypes.GetGlobalAllowedPaymentMethods(8888)
	require.Equal(t, result, enums.MethodUnknown)

	result = f.LimitationForPaymentTypes.GetGlobalAllowedPaymentMethods(7777)
	require.Equal(t, result, enums.MethodCardAllowed|enums.MethodPrepayAllowed)

	result = f.LimitationForPaymentTypes.GetGlobalAllowedPaymentMethods(6666)
	require.Equal(t, result, enums.MethodUnknown)
}

func TestComplianceWithOld(t *testing.T) {
	limitation, err := NewLimitationForPaymentTypes("limitation_for_payment_types.yaml")
	require.NoError(t, err)

	f := HardConfig{
		LimitationForPaymentTypes: limitation,
	}

	dsPrepayMap := map[int64]bool{
		1006360: true,
		55741:   true,
		1005471: true,
		1006308: true,
		1006419: true,
		1006422: true,
		1006425: true,
		1006428: true,
		63158:   true,
		88423:   true,
		92224:   true,
		92261:   true,
		92271:   true,
		92283:   true,
		92291:   true,
	}
	dsPrepayAndCardMap := map[int64]bool{
		1005528: true,
	}
	dsPrepayAndCashMap := map[int64]bool{
		1005486: true,
	}

	for id := range dsPrepayMap {
		paymentTypes := f.LimitationForPaymentTypes.GetGlobalAllowedPaymentMethods(id)
		require.Equal(t, paymentTypes, enums.MethodPrepayAllowed)
	}

	for id := range dsPrepayAndCardMap {
		paymentTypes := f.LimitationForPaymentTypes.GetGlobalAllowedPaymentMethods(id)
		require.Equal(t, paymentTypes, enums.MethodPrepayAllowed|enums.MethodCardAllowed)
	}

	for id := range dsPrepayAndCashMap {
		paymentTypes := f.LimitationForPaymentTypes.GetGlobalAllowedPaymentMethods(id)
		require.Equal(t, paymentTypes, enums.MethodPrepayAllowed|enums.MethodCashAllowed)
	}
}
