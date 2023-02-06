package rides

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
		"GetBlock/settlement without taxi and drive returns nil", func(t *testing.T) {
			routePointsExtractor := &routePointsExtractorMock{}
			orderInfo := &orders.OrderInfo{}
			routePointsExtractor.On("ExtractArrivalSettlementID", orderInfo).Return(123456, nil)
			provider := NewProvider(DefaultKeyset, routePointsExtractor)

			block, err := provider.GetBlock(ctx, orderInfo, models.Notification{})

			require.NoError(t, err)
			require.Nil(t, block)
		},
	)

	t.Run(
		"GetBlock/settlement with taxi and without drive returns block with taxi item", func(t *testing.T) {
			routePointsExtractor := &routePointsExtractorMock{}
			orderInfo := &orders.OrderInfo{}
			routePointsExtractor.On("ExtractArrivalSettlementID", orderInfo).Return(1, nil)
			provider := NewProvider(DefaultKeyset, routePointsExtractor, WithTaxiSettlements([]int{1}), WithDriveSettlements([]int{}))

			block, err := provider.GetBlock(ctx, orderInfo, models.Notification{})

			require.NoError(t, err)
			require.NotNil(t, block)
			require.IsType(t, ui.UsefulBlock{}, block)
			require.Len(t, block.(ui.UsefulBlock).Items, 1)
			require.Equal(t, block.(ui.UsefulBlock).Items[0].Title, "Яндекс Go", models.Notification{})
		},
	)

	t.Run(
		"GetBlock/settlement without taxi and with drive returns block with drive item", func(t *testing.T) {
			routePointsExtractor := &routePointsExtractorMock{}
			orderInfo := &orders.OrderInfo{}
			routePointsExtractor.On("ExtractArrivalSettlementID", orderInfo).Return(1, nil)
			provider := NewProvider(DefaultKeyset, routePointsExtractor, WithTaxiSettlements([]int{}), WithDriveSettlements([]int{1}))

			block, err := provider.GetBlock(ctx, orderInfo, models.Notification{})

			require.NoError(t, err)
			require.NotNil(t, block)
			require.IsType(t, ui.UsefulBlock{}, block)
			require.Len(t, block.(ui.UsefulBlock).Items, 1)
			require.Equal(t, block.(ui.UsefulBlock).Items[0].Title, "Драйв", models.Notification{})
		},
	)

	t.Run(
		"GetBlock/settlement with both taxi and drive returns block with both items", func(t *testing.T) {
			routePointsExtractor := &routePointsExtractorMock{}
			orderInfo := &orders.OrderInfo{}
			routePointsExtractor.On("ExtractArrivalSettlementID", orderInfo).Return(1, nil)
			provider := NewProvider(DefaultKeyset, routePointsExtractor, WithTaxiSettlements([]int{1}), WithDriveSettlements([]int{1}))

			block, err := provider.GetBlock(ctx, orderInfo, models.Notification{})

			require.NoError(t, err)
			require.NotNil(t, block)
			require.IsType(t, ui.UsefulBlock{}, block)
			require.Len(t, block.(ui.UsefulBlock).Items, 2)
			require.Equal(t, block.(ui.UsefulBlock).Items[0].Title, "Яндекс Go")
			require.Equal(t, block.(ui.UsefulBlock).Items[1].Title, "Драйв")
		},
	)
}
