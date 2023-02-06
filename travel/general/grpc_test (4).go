package handler

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	d "google.golang.org/genproto/googleapis/type/date"
	"google.golang.org/grpc/metadata"
	"google.golang.org/protobuf/types/known/wrapperspb"

	"a.yandex-team.ru/library/go/core/log/nop"
	hotelsApi "a.yandex-team.ru/travel/app/backend/api/hotels/v1"
	"a.yandex-team.ru/travel/app/backend/internal/cityimages"
	service "a.yandex-team.ru/travel/app/backend/internal/hotels"
	"a.yandex-team.ru/travel/app/backend/internal/lib/travelapiclient/models"
	"a.yandex-team.ru/travel/app/backend/internal/lib/travelapiclient/testdata"
)

type travelAPIClientMock struct {
	mock.Mock
}

func (c *travelAPIClientMock) GetHotelFavorites(ctx context.Context, res *models.GetHotelFavoritesReq) (*models.GetHotelFavoritesRsp, error) {
	args := c.Called(ctx, res)
	return args.Get(0).(*models.GetHotelFavoritesRsp), args.Error(1)
}

func (c *travelAPIClientMock) AddHotelFavorites(ctx context.Context, res *models.AddHotelFavoritesReq) error {
	args := c.Called(ctx, res)
	return args.Error(0)
}

func (c *travelAPIClientMock) RemoveHotelFavorites(ctx context.Context, res *models.RemoveHotelFavoritesReq) error {
	args := c.Called(ctx, res)
	return args.Error(0)
}

type translationServiceMock struct {
	mock.Mock
}

func (t *translationServiceMock) GetKeysets() map[string][]string {
	args := t.Called()
	return args.Get(0).(map[string][]string)
}

func (t *translationServiceMock) Get(ctx context.Context, key service.TranslationKey) string {
	args := t.Called(ctx, key)
	return args.Get(0).(string)
}
func (t *translationServiceMock) GetFunc(ctx context.Context) func(key service.TranslationKey) string {
	args := t.Called(ctx)
	return args.Get(0).(func(key service.TranslationKey) string)
}

var permalink = uint64(1686701236)
var date = time.Date(2022, 5, 10, 11, 0, 0, 0, time.UTC)
var favoritesRsp = testdata.GetHotelFavoritesRsp

func TestGet_EmptyReq(t *testing.T) {
	ctx := prepareLanguageContext(context.Background(), "ru-RU")
	tcMock := new(travelAPIClientMock)
	req := &models.GetHotelFavoritesReq{
		PagingParams: &models.PagingParams{
			Offset: 0, Limit: 100,
		},
	}
	tcMock.On("GetHotelFavorites", ctx, req).Return(&favoritesRsp, nil)
	cityImages := cityimages.NewService(&nop.Logger{}, cityimages.DefaultConfig, nil)
	tsMock := new(translationServiceMock)
	tsMock.On("GetFunc", ctx).Return(func(key service.TranslationKey) string {
		return "Бесплатная отмена"
	})
	hotelService := service.New(&nop.Logger{}, nil, service.DefaultConfig, tsMock)
	handler := NewGRPCHotelFavoritesHandler(&service.DefaultConfig.Favorites, &nop.Logger{}, tcMock, cityImages, hotelService)

	response, err := handler.Get(ctx, &hotelsApi.GetFavoritesReq{})
	require.NoError(t, err)

	from := d.Date{
		Year:  2022,
		Month: 5,
		Day:   11,
	}
	to := d.Date{
		Year:  2022,
		Month: 5,
		Day:   12,
	}

	assert.Equal(t, &hotelsApi.GetFavoritesRsp{
		TotalHotelCount:    1,
		SelectedCategoryId: "all",
		SearchParams: &hotelsApi.OfferSearchParams{
			CheckinDate:  &from,
			CheckoutDate: &to,
			Adults:       uint32(2),
			ChildrenAges: nil,
		},
		Hotels: []*hotelsApi.Hotel{
			{
				Permalink: int64(permalink),
				Name:      "Бета Измайлово",
				Coordinates: &hotelsApi.Coordinates{
					Latitude:  55.789562,
					Longitude: 37.74772,
				},
				Address:         wrapperspb.String("Россия, Москва, Измайловское шоссе, 71, корп. 2Б"),
				Stars:           wrapperspb.UInt32(3),
				Rating:          wrapperspb.Double(4.7),
				TotalImageCount: wrapperspb.UInt32(265),
				Features: []*hotelsApi.Feature{
					{
						//Id:   wrapperspb.String("wi_fi"),
						Name: wrapperspb.String("Wi-Fi"),
						Icon: hotelsApi.FeatureIcon_FEATURE_ICON_WI_FI,
					},
					{
						//Id:   wrapperspb.String("car_park"),
						Name: wrapperspb.String("Парковка"),
						Icon: hotelsApi.FeatureIcon_FEATURE_ICON_CAR_PARK,
					},
					{
						//Id:   wrapperspb.String("air_conditioning"),
						Name: wrapperspb.String("Кондиционер в номере"),
						Icon: hotelsApi.FeatureIcon_FEATURE_ICON_AIR_CONDITIONING,
					},
					{
						//Id:   wrapperspb.String("payment_by_credit_card"),
						Name: wrapperspb.String("Оплата картой"),
						Icon: hotelsApi.FeatureIcon_FEATURE_ICON_PAYMENT_BY_CREDIT_CARD,
					},
				},
				OfferInfo: &hotelsApi.OfferInfo{
					StrikeThroughPrice: nil,
					Name:               "[T!] Эконом с двумя раздельными кроватями • WiFi в номере",
					OperatorId:         "44",
					RoomId:             "479cb643-03fa-49b3-a2f2-7e55f2959c4",
					RedirectInfo: &hotelsApi.RedirectInfo{
						XRedirect: "https://xredirect-test.yandex.ru/redir?OfferId=479cb643-03fa-49b3-a2f2-7e55f2959c4b&OfferIdHash=2627619024&ProtoLabel=OgMxMjNILGDEE3DV-eeTBni0iaSkBoIBJDQ3OWNiNjQzLTAzZmEtNDliMy1hMmYyLTdlNTVmMjk1OWM0YqIBAzExObgBDcIBCjIwMjItMDUtMTHIAQHSAQEy6AH-__________8B-gENdHJhdmVsLnBvcnRhbIICJHRyYXZlbC1wb3J0YWwtZmF2b3JpdGVzLXBhZ2UtZGVza3RvcJACCrICMDE2NTIxNjIyODg0NDM5NDUtMzc2OTU2NTQyOC1hZGRycy11cHBlci1zdGFibGUtMboCIzQ3YzRkOWY1LTI4MGVmODliLTdmMGI4MjU4LTRmNzk5MmU1-gIVdHJhdmVsLXBvcnRhbC1kZXNrdG9wggMNcHJpY2UtY2hlY2tlcpk",
						IsBoy:     true,
					},
					PansionInfo: &hotelsApi.PansionInfo{
						PansionType: hotelsApi.PansionType_PANSION_TYPE_ROOM_ONLY,
						PansionName: wrapperspb.String("Без питания"),
					},
					YandexPlusInfo: &hotelsApi.YandexPlusInfo{
						Points: 250,
					},
					Badges: []*hotelsApi.HotelBadge{
						{
							Type: hotelsApi.HotelBadgeType_HOTEL_BADGE_TYPE_YANDEX_PLUS,
							Text: wrapperspb.String("250 баллов"),
						},
					},
					Price: wrapperspb.UInt64(2500),
					CancellationInfo: &hotelsApi.CancellationInfo{
						HasFreeCancellation: wrapperspb.Bool(true),
						RefundType:          hotelsApi.RefundType_REFUND_TYPE_FULLY_REFUNDABLE,
						RefundRules: []*hotelsApi.RefundRule{
							{
								Type:   hotelsApi.RefundType_REFUND_TYPE_FULLY_REFUNDABLE,
								EndsAt: wrapperspb.String("2022-05-10 11:00:00 +0000 UTC"),
							},
							{
								Type:     hotelsApi.RefundType_REFUND_TYPE_NON_REFUNDABLE,
								StartsAt: wrapperspb.String("2022-05-10 11:00:00 +0000 UTC"),
							},
						},
						Name: "Бесплатная отмена",
					},
				},
				PollingFinished: wrapperspb.Bool(true),
				IsYandexHotel:   wrapperspb.Bool(false),
				IsPlusAvailable: wrapperspb.Bool(true),
				GeoInfo: &hotelsApi.Hotel_GeoInfo{
					GeoFeatures: []*hotelsApi.Hotel_GeoFeature{
						{
							Name: "8,8\u2006км до центра",
							Icon: hotelsApi.Hotel_GEO_FEATURE_ICON_MAIN_DISTANCE,
						},
					},
				},
				TotalTextReviewCount: wrapperspb.Int32(3451),
				IsFavorite:           wrapperspb.Bool(true),
				Images: []*hotelsApi.Image{
					{
						UrlTemplate: "https://avatars.mds.yandex.net/get-altay/879259/2a0000016156f0d392e7d8da7592698bd77d/%s",
						Sizes: []*hotelsApi.Image_Size{
							{
								Identifier: "XXXS",
								Width:      50,
								Height:     33,
							},
						},
						Url: "https://avatars.mds.yandex.net/get-altay/879259/2a0000016156f0d392e7d8da7592698bd77d/M",
					},
				},
				HotelUrl: "/hotels/moscow/beta-izmailovo/?",
			},
		},
	}, response)
}

func TestGetCategories_EmptyReq(t *testing.T) {
	ctx := context.Background()
	tcMock := new(travelAPIClientMock)
	req := &models.GetHotelFavoritesReq{
		PagingParams: &models.PagingParams{
			Limit: 0, Offset: 0,
		},
	}

	tcMock.On("GetHotelFavorites", ctx, req).Return(&favoritesRsp, nil)
	cityImages := cityimages.NewService(&nop.Logger{}, cityimages.DefaultConfig, nil)
	handler := NewGRPCHotelFavoritesHandler(&service.DefaultConfig.Favorites, &nop.Logger{}, tcMock, cityImages, nil)

	response, err := handler.GetCategories(ctx, &hotelsApi.GetFavoritesCategoriesReq{})
	require.NoError(t, err)

	assert.Equal(t, &hotelsApi.GetFavoritesCategoriesRsp{
		Categories: []*hotelsApi.Categories{
			{
				Id:         "213",
				Name:       "Москва",
				HotelCount: 1,
				ImageUrl:   "https://avatars.mds.yandex.net/get-rasp/1521905/ac391683-c731-429c-a7b6-517410d595c1/travel-avia-desktop",
			},
		},
	}, response)
}

func TestAdd(t *testing.T) {
	ctx := context.Background()
	tcMock := new(travelAPIClientMock)
	permalink := uint64(1686701236)
	req := &models.AddHotelFavoritesReq{
		Permalink: &permalink,
	}
	tcMock.On("AddHotelFavorites", ctx, req).Return(nil)
	cityImages := cityimages.NewService(&nop.Logger{}, cityimages.DefaultConfig, nil)
	handler := NewGRPCHotelFavoritesHandler(&service.DefaultConfig.Favorites, &nop.Logger{}, tcMock, cityImages, nil)

	response, err := handler.Add(ctx, &hotelsApi.AddFavoritesReq{
		Permalink: permalink,
	})
	require.NoError(t, err)

	assert.Empty(t, response)
}

func TestRemove(t *testing.T) {
	ctx := context.Background()
	tcMock := new(travelAPIClientMock)
	permalink := uint64(1686701236)
	req := &models.RemoveHotelFavoritesReq{
		Permalink: &permalink,
	}
	tcMock.On("RemoveHotelFavorites", ctx, req).Return(nil)
	cityImages := cityimages.NewService(&nop.Logger{}, cityimages.DefaultConfig, nil)
	handler := NewGRPCHotelFavoritesHandler(&service.DefaultConfig.Favorites, &nop.Logger{}, tcMock, cityImages, nil)

	response, err := handler.Remove(ctx, &hotelsApi.RemoveFavoritesReq{
		Value: &hotelsApi.RemoveFavoritesReq_Permalink{
			Permalink: permalink,
		},
	})
	require.NoError(t, err)

	assert.Empty(t, response)
}

func prepareLanguageContext(ctx context.Context, acceptLanguageValue string) context.Context {
	md := metadata.MD{
		"grpcgateway-accept-language": []string{acceptLanguageValue},
	}
	return metadata.NewIncomingContext(ctx, md)
}
