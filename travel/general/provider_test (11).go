package trainorder

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/travel/library/go/renderer"
	"a.yandex-team.ru/travel/notifier/internal/models"
	"a.yandex-team.ru/travel/notifier/internal/orders"
	"a.yandex-team.ru/travel/notifier/internal/service/pretrip/blocks/ui"
	"a.yandex-team.ru/travel/notifier/internal/service/pretrip/interfaces"
	"a.yandex-team.ru/travel/notifier/internal/travelapi"
	"a.yandex-team.ru/travel/proto/dicts/rasp"
)

type testStationDataProvider struct {
	interfaces.StationDataProvider
}

func (p *testStationDataProvider) GetStationByID(id int) (*rasp.TStation, bool) {
	switch {
	case id == 2000003:
		return &rasp.TStation{
			Id:                  2000003,
			PopularTitleDefault: "Севастополь-пасс",
			TimeZoneId:          100,
		}, true
	case id == 9602494:
		return &rasp.TStation{
			Id:                  9602494,
			PopularTitleDefault: "Киевский вокзал",
			TimeZoneId:          1,
		}, true
	default:
		return &rasp.TStation{}, false
	}
}

func (p *testStationDataProvider) GetSettlementByStationID(stationID int) (*rasp.TSettlement, bool) {
	switch {
	case stationID == 2000003:
		return &rasp.TSettlement{Id: 1, TitleDefault: "Севастополь"}, true
	case stationID == 9602494:
		return &rasp.TSettlement{Id: 2, TitleDefault: "Москва"}, true
	case stationID == 9602495:
		return &rasp.TSettlement{Id: 2, TitleDefault: "Москва"}, true
	default:
		return nil, false
	}
}

type testTimeZoneDataProvider struct{}

func (p *testTimeZoneDataProvider) Get(timeZoneID int) (*time.Location, bool) {
	switch {
	case timeZoneID == 1:
		result, _ := time.LoadLocation("Europe/Moscow")
		return result, true
	case timeZoneID == 100:
		return time.UTC, true
	default:
		return nil, false
	}
}

type additionalInfoProvider struct {
	mock.Mock
}

func (p *additionalInfoProvider) GetAdditionalOrderInfo(ctx context.Context, orderID string) (*travelapi.AdditionalOrderInfo, error) {
	args := p.Called(ctx, orderID)
	return args.Get(0).(*travelapi.AdditionalOrderInfo), args.Error(1)
}

func TestProvider(t *testing.T) {
	ctx := context.Background()
	testCases := []struct {
		name           string
		trainOrderInfo travelapi.TrainOrderInfo
		expectedResult renderer.Block
	}{
		{
			name: "day-long single seat order",
			trainOrderInfo: travelapi.TrainOrderInfo{
				StationFromID: "2000003",
				StationToID:   "9602494",
				Departure:     "2021-04-18T23:08:00Z",
				Arrival:       "2021-04-19T19:08:00Z",
				TrainNumber:   "008C",
				BrandTitle:    "«Таврия»",
				CarNumber:     "02",
				CarType:       "platzkarte",
				PlaceNumbers:  []string{"010"},
			},
			expectedResult: ui.TrainOrderBlock{
				Type:        ui.BlockTypeTrainTicket.String(),
				Title:       "Ваш билет на поезд",
				OrderNumber: "Номер заказа Pretty-123",
				Points: []string{
					"Севастополь",
					"Москва",
				},
				Download: &ui.SecondaryAction{
					Text:  "Скачать",
					Theme: "SECONDARY",
					URL:   "test-portal-url/download/trains/ticket/OrderID-123",
				},
				Duration:  "20 ч 0 мин",
				TrainName: "008C «Таврия»",
				TrainInfo: "2-й вагон&#160;&#183; Плацкарт&#160;&#183; Место 010",
				Departure: ui.TrainStationInfo{
					Station: "Севастополь-пасс",
					Time:    "23:08",
					Date:    "18&nbsp;апреля, воскресенье",
				},
				Arrival: ui.TrainStationInfo{
					Station:      "Киевский вокзал",
					Time:         "22:08",
					Date:         "19&nbsp;апреля, понедельник",
					NextDateHint: "сл. день, 19&nbsp;апреля",
				},
				Facilities: []string{},
			},
		},
		{
			name: "2-days-long multi seat order in no-name train",
			trainOrderInfo: travelapi.TrainOrderInfo{
				StationFromID:             "2000003",
				StationToID:               "9602494",
				TrainStartSettlementTitle: "Севастополь",
				TrainEndSettlementTitle:   "Санкт-Петербург",
				Departure:                 "2021-04-18T23:08:00Z",
				Arrival:                   "2021-04-20T23:09:00Z",
				TrainNumber:               "008C",
				CarNumber:                 "02",
				CarType:                   "suite",
				PlaceNumbers:              []string{"010", "015"},
			},
			expectedResult: ui.TrainOrderBlock{
				Type:        ui.BlockTypeTrainTicket.String(),
				Title:       "Ваш билет на поезд",
				OrderNumber: "Номер заказа Pretty-123",
				Points: []string{
					"Севастополь",
					"Москва",
				},
				Download: &ui.SecondaryAction{
					Text:  "Скачать",
					Theme: "SECONDARY",
					URL:   "test-portal-url/download/trains/ticket/OrderID-123",
				},
				Duration:  "2 д 0 ч 1 мин",
				TrainName: "008C Севастополь - Санкт-Петербург",
				TrainInfo: "2-й вагон&#160;&#183; СВ&#160;&#183; Места 010, 015",
				Departure: ui.TrainStationInfo{
					Station: "Севастополь-пасс",
					Time:    "23:08",
					Date:    "18&nbsp;апреля, воскресенье",
				},
				Arrival: ui.TrainStationInfo{
					Station:      "Киевский вокзал",
					Time:         "02:09",
					Date:         "21&nbsp;апреля, среда",
					NextDateHint: "+3 дня, 21&nbsp;апреля",
				},
				Facilities: []string{},
			},
		},
	}
	for _, testCase := range testCases {
		t.Run(
			testCase.name, func(t *testing.T) {
				orderInfoProvider := &additionalInfoProvider{}
				orderInfo := &orders.OrderInfo{
					ID: "OrderID-123",
				}
				additionalInfo := &travelapi.AdditionalOrderInfo{
					OrderID:  "Order-123",
					PrettyID: "Pretty-123",
					TrainOrderInfos: []travelapi.TrainOrderInfo{
						testCase.trainOrderInfo,
					},
				}

				orderInfoProvider.On("GetAdditionalOrderInfo", ctx, orderInfo.ID).Return(additionalInfo, nil)
				provider := NewProvider(
					DefaultKeyset,
					&testStationDataProvider{},
					&testTimeZoneDataProvider{},
					orderInfoProvider,
					"test-portal-url",
				)

				block, err := provider.GetBlock(ctx, orderInfo, models.Notification{})

				require.NoError(t, err)
				require.NotNil(t, block)
				require.IsType(t, ui.TrainOrderBlock{}, block)
				uiblock := block.(ui.TrainOrderBlock)
				require.Equal(t, testCase.expectedResult, uiblock)
			},
		)
	}
}

func TestGetBlockPoints(t *testing.T) {
	testCases := []struct {
		name             string
		departureStation *rasp.TStation
		arrivalStation   *rasp.TStation
		expectedResult   []string
		expectedError    bool
	}{
		{
			name: "normal case",
			departureStation: &rasp.TStation{
				Id: 2000003,
			},
			arrivalStation: &rasp.TStation{
				Id: 9602494,
			},
			expectedResult: []string{"Севастополь", "Москва"},
		},
		{
			name: "same settlement",
			departureStation: &rasp.TStation{
				Id:           9602494,
				TitleDefault: "stationFrom",
			},
			arrivalStation: &rasp.TStation{
				Id:           9602495,
				TitleDefault: "stationTo",
			},
			expectedResult: []string{"stationFrom", "stationTo"},
		},
		{
			name: "no settlements, but stations have titles",
			departureStation: &rasp.TStation{
				Id:                  3,
				PopularTitleDefault: "station3popular",
				TitleDefault:        "station3default",
			},
			arrivalStation: &rasp.TStation{
				Id:           4,
				TitleDefault: "station4default",
			},
			expectedResult: []string{"station3popular", "station4default"},
		},
		{
			name: "no settlements, and stations don't have titles",
			departureStation: &rasp.TStation{
				Id: 3,
			},
			arrivalStation: &rasp.TStation{
				Id: 4,
			},
			expectedResult: nil,
			expectedError:  true,
		},
	}
	for _, testCase := range testCases {
		t.Run(
			testCase.name, func(t *testing.T) {
				provider := NewProvider(
					DefaultKeyset,
					&testStationDataProvider{},
					&testTimeZoneDataProvider{},
					&additionalInfoProvider{},
					"test-portal-url",
				)

				result, err := provider.getBlockPoints(testCase.departureStation, testCase.arrivalStation)

				if testCase.expectedError {
					require.Error(t, err)
					require.Contains(t, err.Error(), "empty title")
				} else {
					require.NoError(t, err)
					require.NotNil(t, result)
					require.Equal(t, testCase.expectedResult, result)
				}
			},
		)
	}
}
