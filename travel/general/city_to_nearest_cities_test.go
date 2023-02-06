package repositories

import (
	"context"
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/core/log/nop"
	"a.yandex-team.ru/library/go/ptr"
	"a.yandex-team.ru/travel/avia/avia_statistics/api/internal/landingcityto"
)

func TestCityToNearestCitiesRepository(t *testing.T) {
	logger := &nop.Logger{}
	crosslinkData := []landingcityto.CityToRouteCrosslink{
		{
			ToID:            2,
			CrosslinkFromID: 100,
			CrosslinkToID:   200,
			NationalVersion: "ru",
			Price:           nil,
			Currency:        nil,
			Date:            nil,
		},
		{
			ToID:            2,
			CrosslinkFromID: 300,
			CrosslinkToID:   400,
			NationalVersion: "ru",
			Price:           ptr.Uint32(1000),
			Currency:        ptr.String("RUR"),
			Date:            ptr.String("2020-10-01"),
		},
		{
			ToID:            2,
			CrosslinkFromID: 500,
			CrosslinkToID:   600,
			NationalVersion: "ru",
			Price:           ptr.Uint32(2000),
			Currency:        ptr.String("RUR"),
			Date:            ptr.String("2020-10-02"),
		},
		{
			ToID:            3,
			CrosslinkFromID: 500,
			CrosslinkToID:   600,
			NationalVersion: "ru",
			Price:           nil,
			Currency:        nil,
			Date:            nil,
		},
	}
	crosslinkDataGetter := func() ([]landingcityto.CityToRouteCrosslink, error) { return crosslinkData, nil }
	crosslinkRepository := NewCityToRouteCrosslinksRepository(logger, crosslinkDataGetter)
	_ = crosslinkRepository.Update()

	data := []landingcityto.CityToNearestCities{
		{
			ToID:            1,
			NationalVersion: "ru",
			NearestCityIds:  []uint32{2, 3},
		},
	}
	dataGetter := func() ([]landingcityto.CityToNearestCities, error) { return data, nil }
	r := NewCityToNearestCitiesRepository(logger, dataGetter)
	_ = r.Update()

	t.Run(
		"Test entry found", func(t *testing.T) {
			result, err := r.Get(context.TODO(), 1, "ru", crosslinkRepository.Get)
			require.NoError(t, err, "There are should be no errors")
			expected := []landingcityto.CityToNearestCityWithPrice{
				{
					ID:       2,
					Price:    ptr.Uint32(1000),
					Currency: ptr.String("RUR"),
					Date:     ptr.String("2020-10-01"),
				},
				{
					ID:       3,
					Price:    nil,
					Currency: nil,
					Date:     nil,
				},
			}
			require.Equal(t, expected, result)
		},
	)
	t.Run(
		"Test not found", func(t *testing.T) {
			result, err := r.Get(context.TODO(), 70, "ru", crosslinkRepository.Get)
			require.Nil(t, result, "Result should be nil")
			require.Error(t, err, "Error should be returned")
			require.Equal(t, landingcityto.ErrorNotFound, err, "Error should ErrorNotFound")
		},
	)
}
