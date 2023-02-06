package tarifficator

import (
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/enums"
	"a.yandex-team.ru/market/combinator/pkg/geobase"
)

var tariffsForTest []*TariffRT
var regionKey FromToRegions

func TestFindMaxOptions(t *testing.T) {
	regionMap := geobase.NewExample()
	tariffsFinder := NewTariffsFinder()
	for _, tf := range tariffsForTest {
		tariffsFinder.Add(tf)
	}
	tariffsFinder.Finish(&regionMap)
	// find correct max rule
	{
		results := tariffsFinder.FindMaxOptions(regionKey, nil)
		require.Len(t, results, 1)
		require.NotNil(t, results[0].tariff)
		require.Equal(t, float64(35000), results[0].tariff.WeightMax)
	}
}

func TestGetMaxDimensionsForTariffs(t *testing.T) {
	resp := GetMaxDimensionsForTariffs(nil)
	require.NotNil(t, resp)
	require.Nil(t, resp.MaxDimensions)

	resp = GetMaxDimensionsForTariffs(tariffsForTest)
	require.NotNil(t, resp)
	require.Len(t, resp.MaxDimensions, 3)
	require.Equal(t, uint32(25), resp.MaxDimensions[0])
	require.Equal(t, uint32(40), resp.MaxDimensions[1])
	require.Equal(t, uint32(50), resp.MaxDimensions[2])
	require.Equal(t, uint32(25000), resp.MaxWeight)
	require.Equal(t, uint32(80), resp.MaxDimsum)
}

func init() {
	regionKey = FromToRegions{From: 213, To: 213}
	tariffsForTest = []*TariffRT{
		{
			ID:                1,
			DeliveryServiceID: 12345,
			DeliveryMethod:    enums.DeliveryMethodCourier,
			ProgramTypeList:   ProgramTypeList(ProgramMarketDelivery | ProgramBeruCrossdock),
			RuleAttrs: RuleAttrs{
				WeightMax:       32222,
				HeightMax:       50,
				LengthMax:       40,
				WidthMax:        30,
				DimSumMax:       200,
				SortedDimLimits: [3]uint32{30, 40, 50},
			},
			Type: RuleTypeGlobal,
		},
		{
			ID:                1,
			FromToRegions:     regionKey,
			DeliveryServiceID: 12345,
			Option: Option{
				Cost:    42,
				DaysMin: 1,
				DaysMax: 2,
			},
			RuleAttrs: RuleAttrs{
				WeightMax: 25000,
			},
			Type: RuleTypePayment,
		},
		{
			ID:                1,
			FromToRegions:     regionKey,
			DeliveryServiceID: 12345,
			Option: Option{
				Cost:    42,
				DaysMin: 1,
				DaysMax: 2,
			},
			RuleAttrs: RuleAttrs{
				WeightMax: 35000,
			},
			Type: RuleTypePayment,
		},
		{
			ID:                1,
			FromToRegions:     regionKey,
			DeliveryServiceID: 12345,
			Option: Option{
				Cost:    42,
				DaysMin: 1,
				DaysMax: 2,
			},
			RuleAttrs: RuleAttrs{
				WeightMax: 30000,
			},
			Type: RuleTypePayment,
		},
		{
			ID:                1,
			FromToRegions:     regionKey,
			DeliveryServiceID: 12345,
			Points: []Point{
				Point{ID: 22},
				Point{ID: 33},
				Point{ID: 44},
			},
			RuleAttrs: RuleAttrs{
				WeightMax:       25000,
				HeightMax:       50,
				LengthMax:       40,
				WidthMax:        25,
				DimSumMax:       80,
				SortedDimLimits: [3]uint32{25, 40, 50},
			},
			Type: RuleTypeForPoint,
		},
	}
}
