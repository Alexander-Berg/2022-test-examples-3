package repositories

import (
	"context"
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/core/log/nop"
	"a.yandex-team.ru/travel/avia/avia_statistics/api/internal/seolanding/models"
)

func TestReturnTicketPricesRepository(t *testing.T) {
	data := []models.ReturnTicketPrice{
		{
			FromID:          1,
			ToID:            2,
			NationalVersion: "ru",
			Price:           1000,
			Currency:        "RUR",
		},
	}
	logger := &nop.Logger{}
	dataGetter := func() ([]models.ReturnTicketPrice, error) { return data, nil }
	s := NewReturnTicketPricesRepository(logger, dataGetter)
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
