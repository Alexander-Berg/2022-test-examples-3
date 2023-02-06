package weather

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/ptr"
	"a.yandex-team.ru/travel/notifier/internal/models"
	"a.yandex-team.ru/travel/notifier/internal/orders"
	"a.yandex-team.ru/travel/notifier/internal/service/pretrip/blocks/ui"
	"a.yandex-team.ru/travel/notifier/internal/service/pretrip/interfaces"
	"a.yandex-team.ru/travel/notifier/internal/structs"
	travel_commons_proto "a.yandex-team.ru/travel/proto"
)

type routePointsExtractorMock struct {
	interfaces.RoutePointsExtractor
	mock.Mock
}

func (s *routePointsExtractorMock) ExtractArrivalSettlementID(orderInfo *orders.OrderInfo) (int, error) {
	args := s.Called(orderInfo)
	return args.Int(0), args.Error(1)
}

type weatherProviderMock struct {
	mock.Mock
}

func (c *weatherProviderMock) GetWeather(ctx context.Context, geoID int, lang string) (structs.WeatherForCity, error) {
	return structs.WeatherForCity{
		Info: &structs.WeatherInfo{
			URL:   "detailed_weather_url",
			GeoID: 213,
		},
		Forecasts: []structs.WeatherForecast{
			{
				Date: "2000-01-01",
			},
			{
				Date: "2021-01-21",
				DayParts: map[string]structs.WeatherDayPart{
					"day": {
						TempAvg:   3,
						Icon:      "day_icon",
						Condition: "overcast",
					},
					"night": {
						TempAvg:   -2,
						Icon:      "night_icon",
						Condition: "clear",
					},
				},
			},
			{
				Date: "2021-01-22",
				DayParts: map[string]structs.WeatherDayPart{
					"day": {
						TempAvg:   4,
						Icon:      "day_icon",
						Condition: "overcast",
					},
					"night": {
						TempAvg:   0,
						Icon:      "night_icon",
						Condition: "clear",
					},
				},
			},
		},
	}, nil
}

type testSettlementDataProvider struct {
	interfaces.SettlementDataProvider
}

func (c *testSettlementDataProvider) GetGeoID(i int) (int, bool) {
	return i, true
}

func (c *testSettlementDataProvider) GetTitleTranslation(int) (*travel_commons_proto.TTranslationCaseRu, bool) {
	return &travel_commons_proto.TTranslationCaseRu{
		Prepositional:       "Qqq City",
		LocativePreposition: "in",
	}, true
}

func (c *testSettlementDataProvider) GetSettlementTimeZone(int) *time.Location {
	return time.UTC
}

func TestProvider(t *testing.T) {
	ctx := context.Background()
	weatherProvider := &weatherProviderMock{}
	t.Run(
		"sample weather", func(t *testing.T) {
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
			routePointsExtractor.On("ExtractArrivalSettlementID", orderInfo).Return(213, nil)
			cityTitlesAndLocationsProvider := &testSettlementDataProvider{}
			provider := NewProvider(
				DefaultKeyset,
				routePointsExtractor,
				weatherProvider,
				cityTitlesAndLocationsProvider,
			)

			block, err := provider.GetBlock(
				ctx, orderInfo, models.Notification{
					Subtype: models.NotificationWeekBefore,
				},
			)

			require.NoError(t, err)
			require.NotNil(t, block)
			require.IsType(t, ui.WeatherBlock{}, block)
			uiblock := block.(ui.WeatherBlock)
			expected := ui.WeatherBlock{
				Type:  "WEATHER",
				Title: "Погода in Qqq City",
				Items: []ui.WeatherBlockItem{
					{
						Day:       "чт",
						IsWeekend: false,
						Date:      "21 янв",
						Temperature: ui.WeatherTemperature{
							Day:   "днём +3",
							Night: "ночью -2",
						},
						Conditions: ui.WeatherConditions{
							Icon:        "https://yastatic.net/weather/i/icons/funky/png/dark/48/day_icon.png",
							Description: "Пасмурно",
						},
					},
					{
						Day:       "пт",
						IsWeekend: false,
						Date:      "22 янв",
						Temperature: ui.WeatherTemperature{
							Day:   "+4",
							Night: "0",
						},
						Conditions: ui.WeatherConditions{
							Icon:        "https://yastatic.net/weather/i/icons/funky/png/dark/48/day_icon.png",
							Description: "Пасмурно",
						},
					},
				},
				Action: ui.SecondaryAction{
					Theme: "SECONDARY",
					URL:   "detailed_weather_url",
					Text:  "Подробный прогноз на 10 дней",
				},
			}
			require.Equal(t, expected, uiblock)
		},
	)
}
