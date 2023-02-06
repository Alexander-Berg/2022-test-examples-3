package events

import (
	"context"
	"testing"
	"time"

	"github.com/golang/protobuf/proto"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/ptr"
	"a.yandex-team.ru/travel/notifier/internal/models"
	"a.yandex-team.ru/travel/notifier/internal/orders"
	"a.yandex-team.ru/travel/notifier/internal/service/pretrip/blocks/ui"
	"a.yandex-team.ru/travel/notifier/internal/service/pretrip/interfaces"
	"a.yandex-team.ru/travel/notifier/internal/structs"
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

type eventsProviderMock struct {
	mock.Mock
}

func (c *eventsProviderMock) GetEvents(
	ctx context.Context,
	geoID int,
	startDate time.Time,
	latitude float64,
	longitude float64,
) (structs.EventsForCity, error) {
	return structs.EventsForCity{
		RegionURL: "region_url",
		Events: []structs.Event{
			{
				Name: "праздничное гуляние",
				MinPrice: &structs.Price{
					Value:    14,
					Currency: "RUB",
				},
				DateText: "date_text",
				ImageURL: "image_url",
				EventURL: "event_url",
				Type:     "прогулка",
				Tags:     []string{"тэг 1", "тэг 2", "тэг 3", "тэг 4"},
			},
			{
				Name: "", // shall be skipped because of empty name
				MinPrice: &structs.Price{
					Value:    14,
					Currency: "RUB",
				},
				DateText: "date_text",
				ImageURL: "image_url",
				EventURL: "event_url",
				Type:     "прогулка",
			},
			{
				Name: "праздничное гуляние",
				MinPrice: &structs.Price{
					Value:    14,
					Currency: "RUB",
				},
				DateText: "date_text",
				ImageURL: "", // shall be skipped because of no image
				EventURL: "event_url",
				Type:     "прогулка",
			},
			{
				Name: "праздничное гуляние",
				MinPrice: &structs.Price{
					Value:    14,
					Currency: "RUB",
				},
				DateText: "date_text",
				ImageURL: "image_url",
				EventURL: "", // shall be skipped because of no URL
				Type:     "прогулка",
			},
		},
	}, nil
}

type testSettlementTitleTranslationProvider struct {
	interfaces.SettlementDataProvider
}

func (c *testSettlementTitleTranslationProvider) GetTitleTranslation(int) (*travel_commons_proto.TTranslationCaseRu, bool) {
	result := travel_commons_proto.TTranslationCaseRu{}
	_ = proto.UnmarshalText("Genitive:\"qqq\"", &result)
	return &result, true
}

func (c *testSettlementTitleTranslationProvider) GetGeoID(i int) (int, bool) {
	return i, true
}

func (c *testSettlementTitleTranslationProvider) GetCoordinates(i int) (latitude, longitude float64, found bool) {
	return float64(i) * 0.1, float64(i) * 0.1, true
}

func (c *testSettlementTitleTranslationProvider) GetSettlementTimeZone(int) *time.Location {
	return time.UTC
}

func TestProvider(t *testing.T) {
	ctx := context.Background()
	eventsProvider := &eventsProviderMock{}
	t.Run(
		"sample event", func(t *testing.T) {
			routePointsExtractor := &routePointsExtractorMock{}
			orderInfo := &orders.OrderInfo{
				Type: orders.OrderTypeTrain,
				TrainOrderItems: []*orders.TrainOrderItem{
					{
						ArrivalStation: "9600213",
						DepartureTime:  ptr.Time(time.Date(2021, 1, 21, 21, 41, 0, 0, time.UTC)),
					},
				},
			}
			routePointsExtractor.On("ExtractArrivalSettlementID", orderInfo).Return(1, nil)
			settlementTitleTranslationProvider := &testSettlementTitleTranslationProvider{}
			provider := NewProvider(
				DefaultKeyset,
				routePointsExtractor,
				eventsProvider,
				settlementTitleTranslationProvider,
			)

			block, err := provider.GetBlock(ctx, orderInfo, models.Notification{})

			require.NoError(t, err)
			require.NotNil(t, block)
			require.IsType(t, ui.CarouselBlock{}, block)
			uiblock := block.(ui.CarouselBlock)
			require.Greater(t, len(uiblock.Title), 0)
			require.Greater(t, len(uiblock.Action.Text), 0)
			uiblock.Title = ""
			uiblock.Action.Text = ""
			expected := ui.CarouselBlock{
				Type:     "CAROUSEL",
				Subtitle: "Развлечения qqq — что посмотреть и послушать",
				Items: []ui.CarouselBlockItemInterface{
					ui.CommonCarouselBlockItem{
						CarouselBlockItemBase: ui.CarouselBlockItemBase{
							Image: "image_url",
							Title: "праздничное гуляние",
							URL:   "event_url?utm_campaign=email&utm_medium=PreTrip&utm_source=yandex-travel",
						},
						Description: "От 14 ₽&#160;&#183; прогулка&#160;&#183; тэг 1&#160;&#183; тэг 2&#160;&#183; тэг 3",
						Info: ui.CarouselIconAndText{
							Icon: eventDateStaticImageURL,
							Text: "date_text",
						},
					},
				},
				Action: ui.SecondaryAction{
					Theme: "SECONDARY",
					URL:   "region_url?utm_campaign=email&utm_medium=PreTrip&utm_source=yandex-travel",
				},
			}
			require.Equal(t, expected, uiblock)
		},
	)
}

func TestFilterEvents(t *testing.T) {
	events := []structs.Event{
		{
			ImageURL: "a",
		},
		{
			ImageURL: "ab",
		},
		{
			ImageURL: "abc",
		},
		{
			ImageURL: "b",
		},
		{
			ImageURL: "c",
		},
	}
	predicate := func(e structs.Event) bool {
		return len(e.ImageURL) == 1
	}
	actual := filterEvents(events, predicate, 2)
	expected := []structs.Event{
		{
			ImageURL: "a",
		},
		{
			ImageURL: "b",
		},
	}
	require.Equal(t, expected, actual)
}

func TestAddCommonQueryParams(t *testing.T) {
	t.Run(
		"sample event URL", func(t *testing.T) {
			result, err := addCommonQueryParams("http://event.url")

			require.NoError(t, err)
			require.Equal(t, "http://event.url?utm_campaign=email&utm_medium=PreTrip&utm_source=yandex-travel", result)
		},
	)
}
