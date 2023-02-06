package tours

import (
	"context"
	"github.com/stretchr/testify/require"
	"testing"

	"a.yandex-team.ru/travel/avia/weekendtour/internal/models"
	"a.yandex-team.ru/travel/library/go/logging"
)

func TestService_ReturnsNoToursByDefault(t *testing.T) {
	logger, err := logging.New(&logging.DefaultConfig)
	if err != nil {
		panic(err)
	}

	t.Run(
		"Check default response",
		func(t *testing.T) {
			service := NewService(logger)

			testCases := []struct {
				request          models.WeekendTourRequest
				expectedResponse models.WeekendTourResponse
			}{
				{
					request:          models.WeekendTourRequest{},
					expectedResponse: models.WeekendTourResponse{},
				},
			}
			for _, tc := range testCases {
				result, err := service.GetTours(context.Background(), tc.request)
				require.NoError(t, err)
				require.Equal(t, result, tc.expectedResponse)
			}
		},
	)
}
