package repositories

import (
	"context"
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/core/log/nop"
	"a.yandex-team.ru/travel/avia/avia_statistics/api/internal/seolanding/models"
)

func TestAlternativeRoutesPricesRepository(t *testing.T) {
	logger := &nop.Logger{}
	data := []models.AlternativeRoutePrice{
		{
			FromID:          1,
			ToID:            2,
			AlternativeToID: 3,
			NationalVersion: "ru",
			Price:           1000,
			Currency:        "RUR",
		},
		{
			FromID:          1,
			ToID:            2,
			AlternativeToID: 4,
			NationalVersion: "ru",
			Price:           1000,
			Currency:        "RUR",
		},
	}
	dataGetter := func() ([]models.AlternativeRoutePrice, error) { return data, nil }
	s := NewAlternativeRoutesPricesRepository(logger, dataGetter)
	_ = s.Update()
	t.Run(
		"Test entry found", func(t *testing.T) {
			result, err := s.Get(context.TODO(), 1, 2, "ru")
			require.NoError(t, err, "There are should be no errors")
			require.Equal(t, data, result)
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
