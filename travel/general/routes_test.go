package repositories

import (
	"context"
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/core/log/nop"
	"a.yandex-team.ru/travel/avia/avia_statistics/api/internal/seolanding/models"
)

func TestRoutesRepository(t *testing.T) {
	logger := &nop.Logger{}
	data := []models.Route{
		{
			FromID:          1,
			ToID:            2,
			NationalVersion: "ru",
		},
	}
	dataGetter := func() ([]models.Route, error) { return data, nil }
	s := NewRoutesRepository(logger, dataGetter)
	_ = s.Update()
	t.Run(
		"Test entry found", func(t *testing.T) {
			route := models.Route{FromID: 1, ToID: 2, NationalVersion: "ru"}

			result := s.Contains(context.TODO(), route)

			require.True(t, result, "repository should contain route %+v", route)
		},
	)

	t.Run(
		"Test not found", func(t *testing.T) {
			route := models.Route{FromID: 3, ToID: 2, NationalVersion: "ru"}

			result := s.Contains(context.TODO(), route)

			require.False(t, result, "repository shouldn't contain route %+v", route)
		},
	)
}
