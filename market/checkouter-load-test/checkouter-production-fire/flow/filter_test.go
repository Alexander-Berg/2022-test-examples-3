package flow

import (
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/clients"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/configs"
	"github.com/stretchr/testify/require"
	"testing"
)

func TestMakeDeliveryEmptyOptions(t *testing.T) {
	for _, address := range configs.DefaultAddresses {
		delivery := makeDelivery(options, clients.DeliveryOption{}, address)
		require.Equal(t, "", delivery.ID)
	}
}

func TestMakeDeliveryEmptyDeliveryType(t *testing.T) {
	for _, address := range configs.DefaultAddresses {
		delivery := makeDelivery(options, clients.DeliveryOption{
			ID:   "12",
			Type: "CUSTOM",
		},
			address)
		require.Equal(t, "", delivery.ID)
	}
}

var options = configs.Options{
	GunConfig: configs.GunConfig{RegionID: 213},
}

func TestMakeDeliveryPickup(t *testing.T) {
	for _, address := range configs.DefaultAddresses {
		delivery := makeDelivery(options, clients.DeliveryOption{
			ID:      "23",
			Type:    "PICKUP",
			Outlets: []clients.CartOutlet{{ID: 3}, {ID: 5}, {ID: 1}},
		},
			address)

		require.Equal(t, "23", delivery.ID)
		require.Equal(t, 213, delivery.RegionID)
		require.Equal(t, 3, delivery.Outlet.OutletID)
		require.Nil(t, delivery.Address)
		require.Equal(t, "", delivery.Dates.FromDate)
		require.Equal(t, "", delivery.Dates.ToDate)
		require.Equal(t, "", delivery.Dates.FromTime)
		require.Equal(t, "", delivery.Dates.ToTime)
	}
}

func TestMakeDelivery(t *testing.T) {
	for _, address := range configs.DefaultAddresses {
		delivery := makeDelivery(options, clients.DeliveryOption{
			ID:   "34",
			Type: "DELIVERY",
			DeliveryIntervals: []clients.DeliveryInterval{
				{
					Date: "2021-04-01",
					TimeIntervals: []clients.TimeInterval{
						{
							FromTime: "10:00:00",
							ToTime:   "12:00:00",
						},
						{
							FromTime: "13:00:00",
							ToTime:   "13:30:00",
						},
						{
							FromTime: "13:00:00",
							ToTime:   "11:30:00",
						},
					},
				},
				{
					Date: "2021-05-01",
					TimeIntervals: []clients.TimeInterval{
						{
							FromTime: "17:00:00",
							ToTime:   "18:00:00",
						},
						{
							FromTime: "20:00:00",
							ToTime:   "23:30:00",
						},
					},
				},
				{
					Date: "2021-01-01",
					TimeIntervals: []clients.TimeInterval{
						{
							FromTime: "16:00:00",
							ToTime:   "16:10:00",
						},
					},
				},
			},
		}, address)

		require.Equal(t, "34", delivery.ID)
		require.Equal(t, 213, delivery.RegionID)
		require.Equal(t, map[string]string{
			"country":   "Россия",
			"postcode":  "119034",
			"city":      "Москва",
			"subway":    "Парк Культуры",
			"street":    "Льва Толстого",
			"house":     "18Б",
			"floor":     "2",
			"recipient": "000",
			"phone":     "+77777777777",
		}, delivery.Address)
		require.Equal(t, "2021-04-01", delivery.Dates.FromDate)
		require.Equal(t, "2021-04-01", delivery.Dates.ToDate)
		require.Equal(t, "10:00:00", delivery.Dates.FromTime)
		require.Equal(t, "12:00:00", delivery.Dates.ToTime)
	}
}

func TestMakeDistribution(t *testing.T) {
	distribution, keys := makeDistributionMap(map[string]float32{
		"b": 0.25,
		"a": 0.75,
	})

	require.Equal(t, map[string]rangeType{
		"a": {0.0, 0.75},
		"b": {0.75, 1},
	}, distribution)

	require.Equal(t, []string{"a", "b"}, keys)
}
