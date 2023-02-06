package flow

import (
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/configs"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/model"
	"github.com/stretchr/testify/require"
	"testing"
)

func TestComplexOfferDistribution(t *testing.T) {
	distribution := initOrdersDistribution(60, []model.CartDistribution{
		{
			InternalCarts:      1,
			OrdersDistribution: 0.5,
		},
		{
			InternalCarts:      5,
			OrdersDistribution: 0.5,
		},
	}, []model.OfferDistribution{{
		OffersCount:        1,
		OrdersDistribution: 1,
	}})
	ammoCount, distSum := calculateAmmoCount(distribution)
	require.Equal(t, 36, ammoCount)
	require.Equal(t, float32(0.6), distSum)
}

func TestDefaultOfferDistribution(t *testing.T) {
	distribution := initOrdersDistribution(60, []model.CartDistribution{
		{
			InternalCarts:      1,
			OrdersDistribution: 1,
		},
	}, []model.OfferDistribution{{
		OffersCount:        1,
		OrdersDistribution: 1,
	}})
	ammoCount, distSum := calculateAmmoCount(distribution)
	require.Equal(t, 60, ammoCount)
	require.Equal(t, float32(1.0), distSum)
}

func TestEmptyBatch(t *testing.T) {
	offers := splitOffers([]Offer{}, 1)
	require.Equal(t, [][]Offer{}, offers)
}

func TestSingleBatch(t *testing.T) {
	amount1 := Offer{Quantity: 1}
	amount2 := Offer{Quantity: 2}
	offers := splitOffers([]Offer{amount1, amount2}, 1)
	require.Equal(t, [][]Offer{{amount1}, {amount2}}, offers)
}

func TestHugeBatch(t *testing.T) {
	amount1 := Offer{Quantity: 1}
	offers := splitOffers([]Offer{amount1}, 100)
	require.Equal(t, [][]Offer{{amount1}}, offers)
}

func TestStocksValidationWithSimpleDistributionsOk(t *testing.T) {
	options := configs.Options{
		GunConfig: configs.GunConfig{
			ChooseOfferThreshold: 1,
			StocksRequiredRate:   1.0,
			TotalOrders:          4,
			OffersDistribution: []model.OfferDistribution{{
				OffersCount:        1,
				OrdersDistribution: 1,
			}},
			CartsDistribution: []model.CartDistribution{{
				InternalCarts:      1,
				OrdersDistribution: 1,
			}},
		},
	}

	stocks := createStocksWithQuantities([]int{1, 1, 1, 1})
	err := validateStocksHaveEnoughQuantity(options, stocks, 4.0, false)
	require.Equal(t, nil, err)
}

func TestStocksValidationWithSimpleDistributionsFail(t *testing.T) {
	options := configs.Options{
		GunConfig: configs.GunConfig{
			ChooseOfferThreshold: 1,
			StocksRequiredRate:   1.0,
			TotalOrders:          5,
			OffersDistribution: []model.OfferDistribution{{
				OffersCount:        1,
				OrdersDistribution: 1,
			}},
			CartsDistribution: []model.CartDistribution{{
				InternalCarts:      1,
				OrdersDistribution: 1,
			}},
		},
	}

	stocks := createStocksWithQuantities([]int{1, 1, 1, 1})
	err := validateStocksHaveEnoughQuantity(options, stocks, 5.0, false)
	require.True(t, err != nil)
}

func TestStocksValidationWithComplexDistributionsOk(t *testing.T) {
	options := configs.Options{
		GunConfig: configs.GunConfig{
			ChooseOfferThreshold: 1,
			StocksRequiredRate:   1.0,
			TotalOrders:          8,
			OffersDistribution: []model.OfferDistribution{
				{
					OffersCount:        1,
					OrdersDistribution: 0.5,
				},
				{
					OffersCount:        2,
					OrdersDistribution: 0.5,
				},
			},
			CartsDistribution: []model.CartDistribution{
				{
					InternalCarts:      1,
					OrdersDistribution: 0.5,
				},
				{
					InternalCarts:      2,
					OrdersDistribution: 0.5,
				},
			},
		},
	}
	// [1], [1], [1, 1]
	// [2], [2], [2, 2]
	stocks := createStocksWithQuantities([]int{9, 9})
	err := validateStocksHaveEnoughQuantity(options, stocks, 8.0, false)
	require.Equal(t, nil, err)
}

func TestStocksValidationWithComplexDistributionsFail(t *testing.T) {
	options := configs.Options{
		GunConfig: configs.GunConfig{
			ChooseOfferThreshold: 1,
			StocksRequiredRate:   1.0,
			TotalOrders:          8,
			OffersDistribution: []model.OfferDistribution{
				{
					OffersCount:        1,
					OrdersDistribution: 0.5,
				},
				{
					OffersCount:        2,
					OrdersDistribution: 0.5,
				},
			},
			CartsDistribution: []model.CartDistribution{
				{
					InternalCarts:      1,
					OrdersDistribution: 0.5,
				},
				{
					InternalCarts:      2,
					OrdersDistribution: 0.5,
				},
			},
		},
	}
	// [1], [1], [1, 1]
	// [2], [2], [2, 2]
	stocks := createStocksWithQuantities([]int{8, 9})
	err := validateStocksHaveEnoughQuantity(options, stocks, 8.0, false)
	require.True(t, err != nil)
}

func createStocksWithQuantities(quantitites []int) []Offer {
	stocks := make([]Offer, len(quantitites))
	for i, q := range quantitites {
		stocks[i] = Offer{Quantity: q}
	}
	return stocks
}

func calculateAmmoCount(distribution []*model.OrdersDistribution) (int, float32) {
	ammoCount := 0
	distSum := float32(0.0)
	for _, dist := range distribution {
		ammoCount += dist.Checkouts
		distSum += dist.Distribution
	}
	return ammoCount, distSum
}
