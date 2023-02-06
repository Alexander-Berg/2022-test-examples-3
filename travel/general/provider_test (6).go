package greeting

import (
	"context"
	"testing"

	"github.com/golang/protobuf/proto"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"

	contentadmin "a.yandex-team.ru/travel/marketing/content/v1"
	"a.yandex-team.ru/travel/notifier/internal/models"
	"a.yandex-team.ru/travel/notifier/internal/orders"
	"a.yandex-team.ru/travel/notifier/internal/service/pretrip/blocks/ui"
	"a.yandex-team.ru/travel/notifier/internal/service/pretrip/interfaces"
	"a.yandex-team.ru/travel/proto"
)

type routePointsExtractorMock struct {
	interfaces.RoutePointsExtractor
	mock.Mock
}

func (s *routePointsExtractorMock) ExtractArrivalSettlementID(orderInfo *orders.OrderInfo) (int, error) {
	args := s.Called(orderInfo)
	return args.Int(0), args.Error(1)
}

type cityImagesProviderMock struct {
	mock.Mock
}

func (c *cityImagesProviderMock) GetCityImageByID(context.Context, int) (*contentadmin.CityImage, error) {
	result := contentadmin.CityImage{}
	_ = proto.UnmarshalText("image_url:\"qqq.qqq.qqq/image\"", &result)
	return &result, nil
}

type testCityTitlesProvider struct {
	interfaces.SettlementDataProvider
}

func (c *testCityTitlesProvider) GetTitleTranslation(int) (*travel_commons_proto.TTranslationCaseRu, bool) {
	result := travel_commons_proto.TTranslationCaseRu{}
	_ = proto.UnmarshalText("Nominative:\"qqq\"", &result)
	return &result, true
}

func (c *testCityTitlesProvider) GetAccusativeTitleWithPreposition(int) (string, bool) {
	return "в Москву", true
}

func (c *testCityTitlesProvider) GetPreposition(int) string {
	return "в"
}

func TestProvider(t *testing.T) {
	ctx := context.Background()
	cityImagesProvider := &cityImagesProviderMock{}
	cityTitlesProvider := &testCityTitlesProvider{}
	t.Run(
		"adhoc block", func(t *testing.T) {
			routePointsExtractor := &routePointsExtractorMock{}
			orderInfo := &orders.OrderInfo{}
			routePointsExtractor.On("ExtractArrivalSettlementID", orderInfo).Return(1, nil)
			provider := NewProvider(
				DefaultKeyset,
				routePointsExtractor,
				cityImagesProvider,
				cityTitlesProvider,
			)

			block, err := provider.GetBlock(ctx, orderInfo, models.Notification{Subtype: models.NotificationAdhoc})

			require.NoError(t, err)
			require.NotNil(t, block)
			require.IsType(t, ui.Greeting{}, block)
			require.Equal(t, block.(ui.Greeting).Overlaying, false)
			require.Equal(t, block.(ui.Greeting).Title, "qqq ждёт вас")
		},
	)

	t.Run(
		"day-before block", func(t *testing.T) {
			routePointsExtractor := &routePointsExtractorMock{}
			orderInfo := &orders.OrderInfo{}
			routePointsExtractor.On("ExtractArrivalSettlementID", orderInfo).Return(1, nil)
			provider := NewProvider(
				DefaultKeyset,
				routePointsExtractor,
				cityImagesProvider,
				cityTitlesProvider,
			)

			block, err := provider.GetBlock(ctx, orderInfo, models.Notification{Subtype: models.NotificationDayBefore})

			require.NoError(t, err)
			require.NotNil(t, block)
			require.IsType(t, ui.Greeting{}, block)
			require.Equal(t, block.(ui.Greeting).Overlaying, true)
		},
	)
}
