package repositories

import (
	"context"
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/core/log/nop"
	"a.yandex-team.ru/travel/avia/avia_statistics/api/internal/seolanding/models"
)

func TestMedianPricesRepository(t *testing.T) {
	logger := &nop.Logger{}
	data := []models.MedianPrices{
		{
			FromID:           1,
			ToID:             2,
			NationalVersion:  "ru",
			Month:            1,
			Year:             2020,
			YearMedianPrice:  1000,
			MonthMedianPrice: 2000,
			Currency:         "RUR",
		},
	}
	dataGetter := func() ([]models.MedianPrices, error) { return data, nil }
	s := NewMedianPricesRepository(logger, dataGetter)
	_ = s.Update()
	t.Run(
		"Test entry found", func(t *testing.T) {
			result, err := s.Get(context.TODO(), 1, 2, "ru")
			require.NoError(t, err, "There are should be no errors")
			require.Equal(t, data[0], *result)
		},
	)

	t.Run(
		"Test not found", func(t *testing.T) {
			result, err := s.Get(context.TODO(), 2, 3, "ru")
			require.Nil(t, result, "Result should be nil")
			require.Error(t, err, "Error should be returned")
			require.Equal(t, models.ErrorNotFound, err, "Error should ErrorNotFound")
		},
	)
}
