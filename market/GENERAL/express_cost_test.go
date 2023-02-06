package express

import (
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/market/combinator/pkg/util"
)

var expressCostTariffsTest = map[string]map[int64]CostTariff{
	//Test case
	OrdinaryExpress: {
		//max uint32 - 1
		4_294_967_294: {
			DistanceToRide:  2,
			RideToCost:      12,
			IncludedKm:      1,
			CarDeliveryCost: 100,
			Surges: []TimeSurge{
				{
					FromHour: 7,
					ToHour:   13,
					Surge:    [7]float64{1, 2, 3, 4, 5, 6, 7},
				},
				{
					FromHour: 13,
					ToHour:   17,
					Surge:    [7]float64{8, 9, 10, 11, 12, 13, 14},
				},
				{
					FromHour: 17,
					ToHour:   21,
					Surge:    [7]float64{15, 16, 17, 18, 19, 20, 21},
				},
				{
					FromHour: 21,
					ToHour:   7,
					Surge:    [7]float64{22, 23, 24, 25, 26, 27, 28},
				},
			},
		},
	},
	SDDExpress: {
		//max uint32 - 1
		4_294_967_294: {
			DistanceToRide:  2,
			RideToCost:      12,
			IncludedKm:      1,
			CarDeliveryCost: 100,
			Surges: []TimeSurge{
				{
					FromHour: 7,
					ToHour:   13,
					Surge:    [7]float64{0, 0, 0, 0, 0, 0, 0},
				},
				{
					FromHour: 13,
					ToHour:   17,
					Surge:    [7]float64{0, 0, 0, 0, 0, 0, 0},
				},
				{
					FromHour: 17,
					ToHour:   21,
					Surge:    [7]float64{0, 0, 0, 0, 0, 0, 0},
				},
				{
					FromHour: 21,
					ToHour:   7,
					Surge:    [7]float64{0, 0, 0, 0, 0, 0, 0},
				},
			},
		},
	},
}

func TestCalcCost(t *testing.T) {
	chain := geobase.RegionChain{}
	chain = append(chain, geobase.Region{ID: 4_294_967_293})
	chain = append(chain, geobase.Region{ID: 4_294_967_294})
	chain = append(chain, geobase.Region{ID: 4_294_967_295})

	// четверг 23 сентября 2021 14:37:12 день
	require.Equal(
		t,
		1496.0,
		calcCost(
			2,
			nil,
			chain,
			time.Date(2021, 9, 23, 14, 37, 12, 0, time.FixedZone("test", util.EkbTZOffset)),
			expressCostTariffsTest,
			OrdinaryExpress,
		),
	)
	require.Equal(
		t,
		136.0,
		calcCost(
			2,
			nil,
			chain,
			time.Date(2021, 9, 23, 14, 37, 12, 0, time.FixedZone("test", util.EkbTZOffset)),
			expressCostTariffsTest,
			SDDExpress,
		),
	)
	// воскресенье 26 сентября 2021 23:37:12 ночь
	require.Equal(
		t,
		3808.0,
		calcCost(
			2,
			nil,
			chain,
			time.Date(2021, 9, 26, 23, 37, 12, 0, time.FixedZone("test", util.EkbTZOffset)),
			expressCostTariffsTest,
			OrdinaryExpress,
		),
	)
	require.Equal(
		t,
		136.0,
		calcCost(
			2,
			nil,
			chain,
			time.Date(2021, 9, 26, 23, 37, 12, 0, time.FixedZone("test", util.EkbTZOffset)),
			expressCostTariffsTest,
			SDDExpress,
		),
	)
	// воскресенье 26 сентября 2021 07:12:12 утро
	require.Equal(
		t,
		952.0,
		calcCost(
			2,
			nil,
			chain,
			time.Date(2021, 9, 26, 7, 12, 0, 0, time.FixedZone("test", util.EkbTZOffset)),
			expressCostTariffsTest,
			OrdinaryExpress,
		),
	)
	require.Equal(
		t,
		136.0,
		calcCost(
			2,
			nil,
			chain,
			time.Date(2021, 9, 26, 7, 12, 0, 0, time.FixedZone("test", util.EkbTZOffset)),
			expressCostTariffsTest,
			SDDExpress,
		),
	)
	// понедельник 20 сентября 2021 21:00:00 ночь
	require.Equal(
		t,
		2040.0,
		calcCost(
			2,
			chain,
			nil,
			time.Date(2021, 9, 20, 21, 0, 0, 0, time.FixedZone("test", util.EkbTZOffset)),
			expressCostTariffsTest,
			OrdinaryExpress,
		),
	)
	require.Equal(
		t,
		136.0,
		calcCost(
			2,
			chain,
			nil,
			time.Date(2021, 9, 20, 21, 0, 0, 0, time.FixedZone("test", util.EkbTZOffset)),
			expressCostTariffsTest,
			SDDExpress,
		),
	)

	chain = geobase.RegionChain{}
	chain = append(chain, geobase.Region{ID: 4_294_967_293})
	//нет региона в хардкоде
	require.Equal(
		t,
		0.0,
		calcCost(
			2,
			chain,
			nil,
			time.Date(2021, 9, 20, 21, 0, 0, 0, time.FixedZone("test", util.EkbTZOffset)),
			expressCostTariffsTest,
			OrdinaryExpress,
		),
	)
	require.Equal(
		t,
		0.0,
		calcCost(
			2,
			chain,
			nil,
			time.Date(2021, 9, 20, 21, 0, 0, 0, time.FixedZone("test", util.EkbTZOffset)),
			expressCostTariffsTest,
			SDDExpress,
		),
	)
}
