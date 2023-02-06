package hotelorder

import (
	"context"
	"testing"

	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/travel/notifier/internal/models"
	"a.yandex-team.ru/travel/notifier/internal/orders"
	"a.yandex-team.ru/travel/notifier/internal/service/pretrip/blocks/ui"
	"a.yandex-team.ru/travel/notifier/internal/travelapi"
)

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
		name             string
		checkInBegin     string
		checkInEnd       string
		expectedCheckIn  string
		checkOutEnd      string
		expectedCheckOut string
	}{
		{
			name:             "both checkin begin and checkin end times",
			checkInBegin:     "16:00",
			checkInEnd:       "20:00",
			expectedCheckIn:  "Заезд с 16:00 до 20:00",
			checkOutEnd:      "12:00",
			expectedCheckOut: "выезд до 12:00",
		},
		{
			name:             "checkin begin time only",
			checkInBegin:     "16:00",
			expectedCheckIn:  "Заезд с 16:00",
			checkOutEnd:      "12:00",
			expectedCheckOut: "выезд до 12:00",
		},
		{
			name:             "checkin end time only",
			checkInEnd:       "20:00",
			expectedCheckIn:  "Заезд до 20:00",
			checkOutEnd:      "12:00",
			expectedCheckOut: "выезд до 12:00",
		},
		{
			name:             "checkin any time",
			checkInBegin:     "в любое время",
			expectedCheckIn:  "Заезд в любое время",
			checkOutEnd:      "12:00",
			expectedCheckOut: "выезд до 12:00",
		},
		{
			name:             "checkin any time from",
			checkInBegin:     "15:00",
			expectedCheckIn:  "Заезд с 15:00",
			checkOutEnd:      "12:00",
			expectedCheckOut: "выезд до 12:00",
		},
		{
			name:             "checkout any time",
			checkInBegin:     "16:00",
			checkInEnd:       "20:00",
			expectedCheckIn:  "Заезд с 16:00 до 20:00",
			checkOutEnd:      "в любое время",
			expectedCheckOut: "выезд в любое время",
		},
		{
			name:             "no checkin hence checkout is capitalized",
			checkInBegin:     "",
			checkInEnd:       "",
			expectedCheckIn:  "",
			checkOutEnd:      "в любое время",
			expectedCheckOut: "Выезд в любое время",
		},
	}
	for _, testCase := range testCases {
		t.Run(
			testCase.name, func(t *testing.T) {
				infoProvider := &additionalInfoProvider{}
				orderInfo := &orders.OrderInfo{}
				additionalInfo := &travelapi.AdditionalOrderInfo{
					HotelOrderInfos: []travelapi.HotelOrderInfo{
						{
							Name:             "Апарт-отель Геленджик 6*",
							Address:          "Мыс Идокопас, строение №1",
							CheckInBeginTime: testCase.checkInBegin,
							CheckInEndTime:   testCase.checkInEnd,
							CheckOutTime:     testCase.checkOutEnd,
							DocumentURL:      "https://smwhere.com/smthng.pdf",
							ImageURLTemplate: "https://storage.com/image/%s",
							CheckInDate:      "2021-03-14",
							CheckOutDate:     "2021-12-17",
						},
					},
				}

				infoProvider.On("GetAdditionalOrderInfo", ctx, orderInfo.ID).Return(additionalInfo, nil)
				provider := NewProvider(
					DefaultKeyset,
					infoProvider,
				)

				block, err := provider.GetBlock(ctx, orderInfo, models.Notification{})

				require.NoError(t, err)
				require.NotNil(t, block)
				require.IsType(t, ui.HotelOrder{}, block)
				uiblock := block.(ui.HotelOrder)
				expected := ui.HotelOrder{
					Type:      ui.BlockTypeBooking.String(),
					Title:     "Ваш отель",
					HotelName: additionalInfo.HotelOrderInfos[0].Name,
					GeoPosition: ui.Link{
						URL:  "#",
						Text: additionalInfo.HotelOrderInfos[0].Address,
					},
					Dates:    "14 марта, вс – 17 декабря, пт.",
					Checkin:  testCase.expectedCheckIn,
					Checkout: testCase.expectedCheckOut,
					Download: &ui.SecondaryAction{
						Text:  "Скачать ваучер",
						Theme: ui.SecondaryActionTheme,
						URL:   "https://smwhere.com/smthng.pdf",
					},
					Image: "https://storage.com/image/L",
				}
				require.Equal(t, expected, uiblock)
			},
		)
	}
}
