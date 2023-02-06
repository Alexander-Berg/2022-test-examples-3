package navigation

import (
	"context"
	"testing"

	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/travel/notifier/internal/models"
	"a.yandex-team.ru/travel/notifier/internal/orders"
	"a.yandex-team.ru/travel/notifier/internal/service/pretrip/blocks/ui"
	"a.yandex-team.ru/travel/notifier/internal/service/pretrip/interfaces"
)

type routePointsExtractorMock struct {
	interfaces.RoutePointsExtractor
	mock.Mock
}

func (s *routePointsExtractorMock) ExtractArrivalSettlementID(orderInfo *orders.OrderInfo) (int, error) {
	args := s.Called(orderInfo)
	return args.Int(0), args.Error(1)
}

func TestProvider(t *testing.T) {
	ctx := context.Background()
	t.Run(
		"GetBlock/settlement without subway returns block with commuter rail item only", func(t *testing.T) {
			routePointsExtractor := &routePointsExtractorMock{}
			orderInfo := &orders.OrderInfo{}
			routePointsExtractor.On("ExtractArrivalSettlementID", orderInfo).Return(1, nil)
			provider := NewProvider(DefaultKeyset, routePointsExtractor, WithSubwaySettlements([]int{2}))

			block, err := provider.GetBlock(ctx, orderInfo, models.Notification{})

			require.NoError(t, err)
			require.NotNil(t, block)
			require.IsType(t, ui.UsefulBlock{}, block)
			require.Len(t, block.(ui.UsefulBlock).Items, 1)
			require.Equal(t, block.(ui.UsefulBlock).Items[0].Title, "Электрички")
		},
	)

	t.Run(
		"GetBlock/settlement with subway returns block with both items", func(t *testing.T) {
			routePointsExtractor := &routePointsExtractorMock{}
			orderInfo := &orders.OrderInfo{}
			routePointsExtractor.On("ExtractArrivalSettlementID", orderInfo).Return(1, nil)
			provider := NewProvider(DefaultKeyset, routePointsExtractor, WithSubwaySettlements([]int{1}))

			block, err := provider.GetBlock(ctx, orderInfo, models.Notification{})

			require.NoError(t, err)
			require.NotNil(t, block)
			require.IsType(t, ui.UsefulBlock{}, block)
			require.Len(t, block.(ui.UsefulBlock).Items, 2)
			require.Equal(t, block.(ui.UsefulBlock).Items[0].Title, "Метро")
			require.Equal(t, block.(ui.UsefulBlock).Items[1].Title, "Электрички")
		},
	)
}
