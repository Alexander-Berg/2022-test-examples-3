package audioguide

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/travel/notifier/internal/models"
	"a.yandex-team.ru/travel/notifier/internal/orders"
	"a.yandex-team.ru/travel/notifier/internal/service/pretrip/blocks/ui"
	"a.yandex-team.ru/travel/notifier/internal/service/pretrip/interfaces"
	"a.yandex-team.ru/travel/notifier/internal/structs"
)

type routePointsExtractorMock struct {
	interfaces.RoutePointsExtractor
	mock.Mock
}

func (s *routePointsExtractorMock) ExtractArrivalSettlementID(orderInfo *orders.OrderInfo) (int, error) {
	args := s.Called(orderInfo)
	return args.Int(0), args.Error(1)
}

type audioguidesProviderMock struct {
	mock.Mock
}

func (c *audioguidesProviderMock) GetAudioGuides(context.Context, int) (structs.AudioGuidesForCity, error) {
	return structs.AudioGuidesForCity{
		DirectURL: "direct_url",
		Tours: []structs.AudioGuide{
			{
				Name:     "tour_name",
				ImageURL: "tour_image",
				TourURL:  "tour_url",
				Type:     "museum",
				Category: "tour_category",
				Duration: 2700,
			},
		},
	}, nil
}

type testSettlementDataExtractor struct {
	interfaces.SettlementDataProvider
}

func (c *testSettlementDataExtractor) GetGeoID(i int) (int, bool) {
	return i, true
}

func (c *testSettlementDataExtractor) GetSettlementTimeZone(int) *time.Location {
	return time.UTC
}

func TestProvider(t *testing.T) {
	ctx := context.Background()
	audioguidesProvider := &audioguidesProviderMock{}
	settlementDataExtractor := &testSettlementDataExtractor{}
	t.Run(
		"sample tour", func(t *testing.T) {
			routePointsExtractor := &routePointsExtractorMock{}
			orderInfo := &orders.OrderInfo{}
			routePointsExtractor.On("ExtractArrivalSettlementID", orderInfo).Return(1, nil)
			provider := NewProvider(
				DefaultKeyset,
				routePointsExtractor,
				audioguidesProvider,
				settlementDataExtractor,
			)

			block, err := provider.GetBlock(ctx, orderInfo, models.Notification{})

			require.NoError(t, err)
			require.NotNil(t, block)
			require.IsType(t, ui.CarouselBlock{}, block)
			uiblock := block.(ui.CarouselBlock)
			require.Greater(t, len(uiblock.Title), 0)
			require.Greater(t, len(uiblock.Subtitle), 0)
			require.Greater(t, len(uiblock.Action.Text), 0)
			uiblock.Title = ""
			uiblock.Subtitle = ""
			uiblock.Action.Text = ""
			expected := ui.CarouselBlock{
				Type: "CAROUSEL",
				Items: []ui.CarouselBlockItemInterface{
					ui.CommonCarouselBlockItem{
						CarouselBlockItemBase: ui.CarouselBlockItemBase{
							Image: "tour_image",
							Title: "tour_name",
							URL:   "tour_url?utm_campaign=YT&utm_medium=PreTrip&utm_source=YandexTravel",
						},
						Description: "Музей",
						Info: ui.CarouselIconAndText{
							Icon: durationStaticImageURL,
							Text: "45 минут",
						},
					},
				},
				Action: ui.SecondaryAction{
					Theme: "SECONDARY",
					URL:   "direct_url?utm_campaign=YT&utm_medium=PreTrip&utm_source=YandexTravel",
				},
			}
			require.Equal(t, expected, uiblock)
		},
	)
}

func TestAddCommonQueryParams(t *testing.T) {
	t.Run(
		"sample tour URL", func(t *testing.T) {
			result, err := addCommonQueryParams("http://tour.url")

			require.NoError(t, err)
			require.Equal(t, "http://tour.url?utm_campaign=YT&utm_medium=PreTrip&utm_source=YandexTravel", result)
		},
	)
}
