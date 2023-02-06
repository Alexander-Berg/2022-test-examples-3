package lms

import (
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/market/combinator/pkg/geobase"
	tr "a.yandex-team.ru/market/combinator/pkg/tarifficator"
)

type OptionTestCase struct {
	to          geobase.RegionID
	weight      uint32
	options     []*tr.TariffRT
	daysLimit   *uint32
	priceLimit  *uint32
	expectedIDs []uint64
}

func TestChooseBestOption(t *testing.T) {
	daysLimit := uint32(2)
	priceLimit := uint32(100)
	cases := []OptionTestCase{
		// Time limit case
		{
			to:     213,
			weight: 11,
			options: []*tr.TariffRT{
				&tr.TariffRT{
					ID:                1,
					DeliveryServiceID: 1,
					Option: tr.Option{
						Cost:    30,
						DaysMax: 1,
					},
				},
				&tr.TariffRT{
					ID:                2,
					DeliveryServiceID: 2,
					Option: tr.Option{
						Cost:    10,
						DaysMax: 4,
					},
				},
				&tr.TariffRT{
					ID:                3,
					DeliveryServiceID: 3,
					Option: tr.Option{
						Cost:    60,
						DaysMax: 3,
					},
				},
				&tr.TariffRT{
					ID:                4,
					DeliveryServiceID: 4,
					Option: tr.Option{
						Cost:    1000,
						DaysMax: 2,
					},
				},
			},
			daysLimit: &daysLimit,
			expectedIDs: []uint64{
				1, 4, 3, 2,
			},
		},
		// Price limit case
		{
			to:     213,
			weight: 12,
			options: []*tr.TariffRT{
				&tr.TariffRT{
					ID:                1,
					DeliveryServiceID: 1,
					Option: tr.Option{
						Cost:    50,
						DaysMax: 5,
					},
				},
				&tr.TariffRT{
					ID:                2,
					DeliveryServiceID: 2,
					Option: tr.Option{
						Cost:    110,
						DaysMax: 4,
					},
				},
				&tr.TariffRT{
					ID:                3,
					DeliveryServiceID: 3,
					Option: tr.Option{
						Cost:    120,
						DaysMax: 2,
					},
				},
				&tr.TariffRT{
					ID:                4,
					DeliveryServiceID: 4,
					Option: tr.Option{
						Cost:    10,
						DaysMax: 1,
					},
				},
			},
			priceLimit: &priceLimit,
			expectedIDs: []uint64{
				4, 1, 2, 3,
			},
		},
		// Two limits case
		{
			to:     213,
			weight: 12,
			options: []*tr.TariffRT{
				&tr.TariffRT{
					ID:                1,
					DeliveryServiceID: 1,
					Option: tr.Option{
						Cost:    130,
						DaysMax: 4,
					},
				},
				&tr.TariffRT{
					ID:                2,
					DeliveryServiceID: 2,
					Option: tr.Option{
						Cost:    120,
						DaysMax: 6,
					},
				},
				&tr.TariffRT{
					ID:                3,
					DeliveryServiceID: 3,
					Option: tr.Option{
						Cost:    30,
						DaysMax: 6,
					},
				},
				&tr.TariffRT{
					ID:                4,
					DeliveryServiceID: 4,
					Option: tr.Option{
						Cost:    45,
						DaysMax: 4,
					},
				},
				&tr.TariffRT{
					ID:                5,
					DeliveryServiceID: 5,
					Option: tr.Option{
						Cost:    140,
						DaysMax: 1,
					},
				},
				&tr.TariffRT{
					ID:                6,
					DeliveryServiceID: 6,
					Option: tr.Option{
						Cost:    110,
						DaysMax: 2,
					},
				},
				&tr.TariffRT{
					ID:                7,
					DeliveryServiceID: 7,
					Option: tr.Option{
						Cost:    10,
						DaysMax: 1,
					},
				},
				&tr.TariffRT{
					ID:                8,
					DeliveryServiceID: 8,
					Option: tr.Option{
						Cost:    50,
						DaysMax: 1,
					},
				},
				&tr.TariffRT{
					ID:                9,
					DeliveryServiceID: 9,
					Option: tr.Option{
						Cost:    40,
						DaysMax: 2,
					},
				},
			},
			daysLimit:  &daysLimit,
			priceLimit: &priceLimit,
			expectedIDs: []uint64{
				9, 8, 7, 6, 5, 4, 3, 2, 1,
			},
		},
		// No such region
		{
			to:     322,
			weight: 11,
		},
	}
	info := LmsMetaInfo{}
	info.RegionToCond = make(RegionToCondMap)
	info.DServiceRating = make(DServiceToRating)
	info.DServiceRating[uint64(7)] = 2
	info.DServiceRating[uint64(8)] = 1
	info.DServiceRating[uint64(9)] = 1
	for id, tt := range cases {
		info.RegionToCond[tt.to] = []DCondition{
			{
				Days:     tt.daysLimit,
				Price:    tt.priceLimit,
				WeightLo: 0,
				WeightUp: tt.weight + 1,
			},
		}
		_, tariffs := info.ChooseBestOption(
			tt.to,
			tt.weight,
			tt.options,
		)
		for i, trf := range tariffs {
			assert.Equal(
				t,
				trf.ID,
				tt.expectedIDs[i],
				fmt.Sprintf("during test case %d, real tariffs: %+v", id, tariffs),
			)
		}
	}
}
