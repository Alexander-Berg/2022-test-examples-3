package repositories

import (
	"context"
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/core/log/nop"
	"a.yandex-team.ru/travel/avia/avia_statistics/api/internal/landingcityto"
)

func TestCityToMonthAndYearPricesRepository(t *testing.T) {
	logger := &nop.Logger{}
	data := []landingcityto.CityToMonthAndYearPriceInfo{
		{
			ToID:                    1,
			NationalVersion:         "ru",
			Currency:                "RUR",
			YearMedianPrice:         5000,
			PopularMonthYear:        2021,
			PopularMonth:            7,
			PopularMonthMedianPrice: 7000,
			MinMonthYear:            2021,
			MinMonth:                3,
			MinMonthMedianPrice:     3000,
			MaxMonthYear:            2021,
			MaxMonth:                11,
			MaxMonthMedianPrice:     11000,
		},
		{
			ToID:                    2,
			NationalVersion:         "ru",
			Currency:                "RUR",
			YearMedianPrice:         5020,
			PopularMonthYear:        2021,
			PopularMonth:            6,
			PopularMonthMedianPrice: 7020,
			MinMonthYear:            2021,
			MinMonth:                2,
			MinMonthMedianPrice:     3020,
			MaxMonthYear:            2021,
			MaxMonth:                10,
			MaxMonthMedianPrice:     11020,
		},
	}
	dataGetter := func() ([]landingcityto.CityToMonthAndYearPriceInfo, error) { return data, nil }
	s := NewCityToMonthAndYearPricesRepository(logger, dataGetter)
	_ = s.Update()
	t.Run(
		"Test entry found", func(t *testing.T) {
			result, err := s.Get(context.TODO(), 1, "ru")
			require.NoError(t, err, "There are should be no errors")
			require.Equal(t, data[0], *result)
		},
	)

	t.Run(
		"Test not found", func(t *testing.T) {
			result, err := s.Get(context.TODO(), 70, "ru")
			require.Nil(t, result, "Result should be nil")
			require.Error(t, err, "Error should be returned")
			require.Equal(t, landingcityto.ErrorNotFound, err, "Error should ErrorNotFound")
		},
	)
}
