package repositories

import (
	"context"
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/core/log/nop"
	"a.yandex-team.ru/travel/avia/avia_statistics/api/internal/seolanding/models"
)

func TestRouteInfosRepository(t *testing.T) {
	logger := &nop.Logger{}
	data := []models.RouteInfo{
		{
			FromID:       1,
			ToID:         2,
			Distance:     100,
			Duration:     123,
			FromAirports: []int{1, 2},
			ToAirports:   []int{3, 4},
		},
	}
	dataGetter := func() ([]models.RouteInfo, error) { return data, nil }
	s := NewRouteInfosRepository(logger, dataGetter)
	_ = s.Update()
	t.Run(
		"Test entry found", func(t *testing.T) {
			result, err := s.Get(context.TODO(), 1, 2)
			require.NoError(t, err, "There are should be no errors")
			require.Equal(t, data[0], *result)
		},
	)

	t.Run(
		"Test not found", func(t *testing.T) {
			result, err := s.Get(context.TODO(), 2, 3)
			require.Nil(t, result, "Result should be nil")
			require.Error(t, err, "Error should be returned")
			require.Equal(t, models.ErrorNotFound, err, "Error should ErrorNotFound")
		},
	)
}
