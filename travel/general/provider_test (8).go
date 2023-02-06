package hotels

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

type hotelsProviderMock struct{}

func (c *hotelsProviderMock) GetHotels(ctx context.Context, geoID int, orderID string, limit int) (*structs.HotelPayload, error) {
	return &structs.HotelPayload{
		RegionGeoID: 213,
		RequestParams: structs.RequestParams{
			CheckinDate:  structs.NewDate(2021, 1, 1),
			CheckoutDate: structs.NewDate(2021, 1, 2),
			Nights:       1,
			Adults:       2,
			ChildrenAges: []int{1, 2},
		},
		HotelsList: []structs.HotelsBlockItem{
			{
				Hotel: structs.Hotel{
					Name:     "Апарт-отель Геленджик 6*",
					Category: structs.HotelCategory{Name: "отель"},
					Rating:   5.1,
					Images: []structs.HotelImage{
						{
							URLTemplate: "https://static.com/image.jpg/%s",
							Sizes:       []structs.HotelImageSizes{{Size: "unknown"}, {Size: "XXL"}, {Size: "orig"}},
						},
					},
					TotalTextReviewCount: 10,
					MainAmenities:        []structs.MainAmenity{{ID: "id1", Name: "amenity1"}, {ID: "id2", Name: "amenity2"}},
				},
				MinPrice: &structs.HotelPrice{
					Value:    1000,
					Currency: "RUB",
				},
				LandingURL: "city/gelendzhik",
				Badges:     []structs.Badge{{ID: "id1", Text: "badge1"}, {ID: "id2", Text: "badge2"}},
			},
		},
	}, nil
}

type testSettlementDataProvider struct {
	interfaces.SettlementDataProvider
}

func (c *testSettlementDataProvider) GetGeoID(i int) (int, bool) {
	return 213, true
}

func (c *testSettlementDataProvider) GetSettlementTimeZone(int) *time.Location {
	return time.UTC
}

func (c *testSettlementDataProvider) GetTitleTranslation(int) (*travel_commons_proto.TTranslationCaseRu, bool) {
	return &travel_commons_proto.TTranslationCaseRu{Genitive: "Москвы"}, true
}

func TestProvider(t *testing.T) {
	ctx := context.Background()
	travelPortalURL := "https://travel-test.yandex.ru"
	hotelsProvider := &hotelsProviderMock{}
	t.Run(
		"sample hotel", func(t *testing.T) {
			routePointsExtractor := &routePointsExtractorMock{}
			orderInfo := &orders.OrderInfo{}
			routePointsExtractor.On("ExtractArrivalSettlementID", orderInfo).Return(1, nil)
			settlementTitleTranslationProvider := &testSettlementDataProvider{}
			provider := NewProvider(
				DefaultKeyset,
				routePointsExtractor,
				hotelsProvider,
				settlementTitleTranslationProvider,
				travelPortalURL,
			)

			block, err := provider.GetBlock(ctx, orderInfo, models.Notification{})

			require.NoError(t, err)
			require.NotNil(t, block)
			require.IsType(t, ui.CarouselBlock{}, block)
			uiblock := block.(ui.CarouselBlock)
			require.NotEmpty(t, uiblock.Title)
			require.NotEmpty(t, uiblock.Action.Text)
			expected := ui.CarouselBlock{
				Type:     "CAROUSEL",
				Title:    "Не забудьте забронировать отель",
				Subtitle: "Подборка предложений отелей Москвы",
				Items: []ui.CarouselBlockItemInterface{
					&ui.HotelCarouselBlockItem{
						CarouselBlockItemBase: ui.CarouselBlockItemBase{
							Image: "https://static.com/image.jpg/orig",
							Title: "Апарт-отель Геленджик 6*",
							URL: travelPortalURL +
								"/hotels/city/gelendzhik?adults=2&checkinDate=2021-01-01&checkoutDate=2021-01-02&childrenAges=1%2C2" +
								"&utm_campaign=email&utm_medium=PreTrip&utm_source=carousel",
						},
						Rating:        5.1,
						Reviews:       "10 отзывов",
						OrderInfo:     []string{"amenity1", "amenity2"},
						Price:         "От 1000 ₽",
						Accommodation: "отель",
					},
				},
				Action: ui.SecondaryAction{
					Theme: "SECONDARY",
					URL: travelPortalURL +
						"/hotels/search?adults=2&checkinDate=2021-01-01&checkoutDate=2021-01-02&childrenAges=1%2C2" +
						"&geoId=213&utm_campaign=email&utm_medium=PreTrip&utm_source=carousel",
					Text: "Посмотреть все места",
				},
			}
			require.Equal(t, expected, uiblock)
		},
	)
}
