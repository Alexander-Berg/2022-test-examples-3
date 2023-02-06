package repositories

import (
	"context"
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/core/log/nop"
	"a.yandex-team.ru/library/go/ptr"
	"a.yandex-team.ru/travel/avia/avia_statistics/api/internal/seolanding/models"
)

func TestMinPricesByAirlineRepository(t *testing.T) {
	logger := &nop.Logger{}
	data := []models.MinPriceByAirline{
		{
			FromID:          1,
			ToID:            2,
			AirlineID:       3,
			NationalVersion: "ru",
			MinPrice:        ptr.Uint32(1000),
			Currency:        "RUR",
		},
	}
	dataGetter := func() ([]models.MinPriceByAirline, error) { return data, nil }
	s := NewMinPricesByAirlineRepository(logger, dataGetter)
	_ = s.Update()
	t.Run(
		"Test entry found", func(t *testing.T) {
			result, err := s.Get(context.TODO(), 1, 2, "ru", 3)
			require.NoError(t, err, "There are should be no errors")
			require.Equal(t, data[0], *result)
		},
	)

	t.Run(
		"Test not found", func(t *testing.T) {
			result, err := s.Get(context.TODO(), 2, 3, "ru", 4)
			require.Nil(t, result, "Result should be nil")
			require.Error(t, err, "Error should be returned")
			require.Equal(t, models.ErrorNotFound, err, "Error should ErrorNotFound")
		},
	)
}
