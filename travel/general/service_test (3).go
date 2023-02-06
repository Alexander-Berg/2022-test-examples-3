package hotels

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"google.golang.org/genproto/googleapis/type/date"
	"google.golang.org/grpc/metadata"
	"google.golang.org/protobuf/types/known/wrapperspb"

	"a.yandex-team.ru/library/go/core/log/nop"
	v1 "a.yandex-team.ru/travel/app/backend/api/common/v1"
	hotelsAPI "a.yandex-team.ru/travel/app/backend/api/hotels/v1"
	"a.yandex-team.ru/travel/app/backend/internal/lib/travelapiclient/models"
	"a.yandex-team.ru/travel/app/backend/internal/lib/travelapiclient/testdata"
)

type travelAPIClientMock struct {
	mock.Mock
}

func (c *travelAPIClientMock) GetHotelOffers(ctx context.Context, request *models.GetHotelOffersRequest) (*models.GetHotelOffersResponse, error) {
	args := c.Called(ctx, request)
	return args.Get(0).(*models.GetHotelOffersResponse), args.Error(1)
}

func (c *travelAPIClientMock) GetSimilarHotels(ctx context.Context, request *models.GetSimilarHotelsRequest) (*models.GetSimilarHotelsResponse, error) {
	panic("implement me")
}

func (c *travelAPIClientMock) GetHotelImages(ctx context.Context, request *models.GetHotelImagesRequest) (*models.GetHotelImagesResponse, error) {
	args := c.Called(ctx, request)
	return args.Get(0).(*models.GetHotelImagesResponse), args.Error(1)
}

func (c *travelAPIClientMock) LogSuggest(ctx context.Context, selectedID string, sessionID string, requestIndex int, isManualClick bool, isTrustedUser bool) error {
	panic("implement me")
}

func (c *travelAPIClientMock) Suggest(ctx context.Context, query string, limit int, lang string, domain string, sessionID string, requestIndex int) (*models.SuggestResponse, error) {
	return &models.SuggestResponse{Groups: []models.SuggestGroup{
		{
			Name: "Отели к вашему путешествию",
			Items: []models.SuggestItem{
				{
					ID:          "personalized-order-146-ru",
					Name:        "Симферополь",
					Description: "Республика Крым, Россия",
					RedirectParams: &models.RedirectParam{
						BBox:      "",
						GeoID:     intP(146),
						HotelSlug: "",
						OfferSearchParams: &models.OfferSearchParams{
							Adults:       1,
							CheckinDate:  models.Date{Time: time.Date(2021, 12, 12, 0, 0, 0, 0, time.UTC)},
							CheckoutDate: models.Date{Time: time.Date(2021, 12, 13, 0, 0, 0, 0, time.UTC)},
							ChildrenAges: []int{},
						},
						Permalink:      nil,
						SelectedSortID: "",
						SortOrigin:     "",
						Type:           models.SuggestTypeCrossSale,
					},
				},
			},
		},
		{
			Name: "Вы искали",
			Items: []models.SuggestItem{
				{
					ID:          "personalized-search-22-ru",
					Name:        "Калининград",
					Description: "Калининградская область, Россия",
					RedirectParams: &models.RedirectParam{
						BBox:      "",
						GeoID:     intP(22),
						HotelSlug: "",
						OfferSearchParams: &models.OfferSearchParams{
							Adults:       1,
							CheckinDate:  models.Date{Time: time.Date(2021, 12, 23, 0, 0, 0, 0, time.UTC)},
							CheckoutDate: models.Date{Time: time.Date(2021, 12, 24, 0, 0, 0, 0, time.UTC)},
							ChildrenAges: []int{},
						},
						Permalink:      nil,
						SelectedSortID: "",
						SortOrigin:     "",
						Type:           models.SuggestTypeHistory,
					},
				},
			},
		},
		{
			Name: "Отели рядом",
			Items: []models.SuggestItem{
				{
					ID:          "hotels-nearby-ru",
					Name:        "Отели рядом",
					Description: "",
					RedirectParams: &models.RedirectParam{
						BBox:              "",
						GeoID:             nil,
						HotelSlug:         "",
						OfferSearchParams: nil,
						Permalink:         nil,
						SelectedSortID:    "near-first",
						SortOrigin:        "",
						Type:              models.SuggestTypeHotelsNearby,
					},
				},
			},
		},
		{
			Name: "Регион",
			Items: []models.SuggestItem{
				{
					ID:          "region-213-ru",
					Name:        "Москва",
					Description: "Россия",
					RedirectParams: &models.RedirectParam{
						BBox:              "",
						GeoID:             intP(213),
						HotelSlug:         "",
						OfferSearchParams: nil,
						Permalink:         nil,
						SelectedSortID:    "",
						SortOrigin:        "",
						Type:              models.SuggestTypeRegion,
					},
				},
				{
					ID:          "region-2-ru",
					Name:        "Санкт-Петербург",
					Description: "Россия, Санкт-Петербург и Ленинградская область",
					RedirectParams: &models.RedirectParam{
						BBox:              "",
						GeoID:             intP(2),
						HotelSlug:         "",
						OfferSearchParams: nil,
						Permalink:         nil,
						SelectedSortID:    "",
						SortOrigin:        "",
						Type:              models.SuggestTypeRegion,
					},
				},
			},
		},
		{
			Name: "Гостиница",
			Items: []models.SuggestItem{
				{
					ID:          "hotel-19703708637-ru",
					Name:        "Сочи Парк Отель",
					Description: "Гостиница · Россия, Краснодарский край, Сочи, Континентальный проспект, 6",
					RedirectParams: &models.RedirectParam{
						BBox:              "",
						GeoID:             nil,
						HotelSlug:         "sochi/sochi-park-otel",
						OfferSearchParams: nil,
						Permalink:         uintP(19703708637),
						SelectedSortID:    "",
						SortOrigin:        "",
						Type:              models.SuggestTypeHotel,
					},
				},
				{
					ID:          "hotel-1046552916-ru",
					Name:        "ФГБУ Объединенный санаторий Сочи Управления делами Президента Российской Федерации",
					Description: "Санаторий · Россия, Краснодарский край, Сочи, Виноградная улица, 27",
					RedirectParams: &models.RedirectParam{
						BBox:              "",
						GeoID:             nil,
						HotelSlug:         "sochi/fgbu-ob-edinennyi-sanatorii-sochi-upravleniia-delami-prezidenta-rossiiskoi-federatsii",
						OfferSearchParams: nil,
						Permalink:         uintP(1046552916),
						SelectedSortID:    "",
						SortOrigin:        "",
						Type:              models.SuggestTypeHotel,
					},
				},
			},
		},
	}}, nil
}

func (c *travelAPIClientMock) SearchHotels(ctx context.Context, request models.SearchHotelsRequest) (*models.SearchHotelsResponse, error) {
	panic("unimplemented")
}

func (c *travelAPIClientMock) GetHotelsCounters(ctx context.Context, request *models.GetCountersRequest) (*models.GetCountersResponse, error) {
	panic("unimplemented")
}

func (c *travelAPIClientMock) GetHotelInfo(ctx context.Context, request *models.GetHotelInfoRequest) (*models.GetHotelInfoResponse, error) {
	args := c.Called(ctx, request)
	return args.Get(0).(*models.GetHotelInfoResponse), args.Error(1)
}

type translationServiceForTests struct{}

func (t translationServiceForTests) GetKeysets() map[string][]string {
	return map[string][]string{}
}

func (translationServiceForTests) Get(ctx context.Context, key TranslationKey) string {
	return ""
}
func (translationServiceForTests) GetFunc(ctx context.Context) func(key TranslationKey) string {
	return func(key TranslationKey) string {
		return ""
	}
}

func intP(i int64) *int64 {
	return &i
}

func uintP(i uint64) *uint64 {
	return &i
}

func TestMapping(t *testing.T) {
	src := Service{client: &travelAPIClientMock{}}
	req := hotelsAPI.HotelsSuggestReq{
		Query:        "",
		Limit:        7,
		RequestIndex: 0,
	}
	ctx := prepareLanguageContext(context.Background(), "ru-RU")
	rsp, err := src.Suggest(ctx, &req)
	assert.NoError(t, err)
	assert.Len(t, rsp.Groups, 4)
	assert.Len(t, rsp.Groups[0].Items, 1)
	assert.Len(t, rsp.Groups[1].Items, 1)
	assert.Len(t, rsp.Groups[2].Items, 2)
	assert.Len(t, rsp.Groups[3].Items, 2)
	assert.Equal(t, rsp.Groups[0].Title, "Отели к вашему путешествию")
	assert.Equal(t, rsp.Groups[1].Title, "Вы искали")
	assert.Equal(t, rsp.Groups[2].Title, "Регион")
	assert.Equal(t, rsp.Groups[3].Title, "Гостиница")
	assert.Equal(t, rsp.Groups[0].Items[0].Id, "personalized-order-146-ru")
	assert.Equal(t, rsp.Groups[0].Items[0].Title, "Симферополь")
	assert.Equal(t, rsp.Groups[0].Items[0].Subtitle, "Республика Крым, Россия")
	assert.Nil(t, rsp.Groups[0].Items[0].Data.Hotel)
	assert.NotNil(t, rsp.Groups[0].Items[0].Data.Region)
	assert.NotNil(t, rsp.Groups[0].Items[0].Data.SearchParams)
	assert.Equal(t, rsp.Groups[0].Items[0].Data.Region.GeoId, int64(146))
	assert.Equal(t, rsp.Groups[0].Items[0].Data.SearchParams.Adults, uint32(1))
	assert.Equal(t, rsp.Groups[0].Items[0].Data.SearchParams.CheckinDate.Year, int32(2021))
	assert.Equal(t, rsp.Groups[0].Items[0].Data.SearchParams.CheckinDate.Month, int32(12))
	assert.Equal(t, rsp.Groups[0].Items[0].Data.SearchParams.CheckinDate.Day, int32(12))
	assert.Equal(t, rsp.Groups[0].Items[0].Data.SearchParams.CheckoutDate.Year, int32(2021))
	assert.Equal(t, rsp.Groups[0].Items[0].Data.SearchParams.CheckoutDate.Month, int32(12))
	assert.Equal(t, rsp.Groups[0].Items[0].Data.SearchParams.CheckoutDate.Day, int32(13))
	assert.Empty(t, rsp.Groups[0].Items[0].Data.SearchParams.ChildrenAges)
	assert.Equal(t, rsp.Groups[1].Items[0].Id, "personalized-search-22-ru")
	assert.Equal(t, rsp.Groups[1].Items[0].Title, "Калининград")
	assert.Equal(t, rsp.Groups[1].Items[0].Subtitle, "Калининградская область, Россия")
	assert.Nil(t, rsp.Groups[1].Items[0].Data.Hotel)
	assert.NotNil(t, rsp.Groups[1].Items[0].Data.Region)
	assert.NotNil(t, rsp.Groups[1].Items[0].Data.SearchParams)
	assert.Equal(t, rsp.Groups[1].Items[0].Data.Region.GeoId, int64(22))
	assert.Equal(t, rsp.Groups[1].Items[0].Data.SearchParams.Adults, uint32(1))
	assert.Equal(t, rsp.Groups[1].Items[0].Data.SearchParams.CheckinDate.Year, int32(2021))
	assert.Equal(t, rsp.Groups[1].Items[0].Data.SearchParams.CheckinDate.Month, int32(12))
	assert.Equal(t, rsp.Groups[1].Items[0].Data.SearchParams.CheckinDate.Day, int32(23))
	assert.Equal(t, rsp.Groups[1].Items[0].Data.SearchParams.CheckoutDate.Year, int32(2021))
	assert.Equal(t, rsp.Groups[1].Items[0].Data.SearchParams.CheckoutDate.Month, int32(12))
	assert.Equal(t, rsp.Groups[1].Items[0].Data.SearchParams.CheckoutDate.Day, int32(24))
	assert.Empty(t, rsp.Groups[1].Items[0].Data.SearchParams.ChildrenAges)
	assert.Equal(t, rsp.Groups[2].Items[0].Id, "region-213-ru")
	assert.Equal(t, rsp.Groups[2].Items[0].Title, "Москва")
	assert.Equal(t, rsp.Groups[2].Items[0].Subtitle, "Россия")
	assert.Nil(t, rsp.Groups[2].Items[0].Data.Hotel)
	assert.NotNil(t, rsp.Groups[2].Items[0].Data.Region)
	assert.Nil(t, rsp.Groups[2].Items[0].Data.SearchParams)
	assert.Equal(t, rsp.Groups[2].Items[0].Data.Region.GeoId, int64(213))
	assert.Equal(t, rsp.Groups[2].Items[1].Id, "region-2-ru")
	assert.Equal(t, rsp.Groups[2].Items[1].Title, "Санкт-Петербург")
	assert.Equal(t, rsp.Groups[2].Items[1].Subtitle, "Россия, Санкт-Петербург и Ленинградская область")
	assert.Nil(t, rsp.Groups[2].Items[1].Data.Hotel)
	assert.NotNil(t, rsp.Groups[2].Items[1].Data.Region)
	assert.Nil(t, rsp.Groups[2].Items[1].Data.SearchParams)
	assert.Equal(t, rsp.Groups[2].Items[1].Data.Region.GeoId, int64(2))

	assert.Equal(t, rsp.Groups[3].Items[0].Id, "hotel-19703708637-ru")
	assert.Equal(t, rsp.Groups[3].Items[0].Title, "Сочи Парк Отель")
	assert.Equal(t, rsp.Groups[3].Items[0].Subtitle, "Гостиница · Россия, Краснодарский край, Сочи, Континентальный проспект, 6")
	assert.NotNil(t, rsp.Groups[3].Items[0].Data.Hotel)
	assert.Nil(t, rsp.Groups[3].Items[0].Data.Region)
	assert.Nil(t, rsp.Groups[3].Items[0].Data.SearchParams)
	assert.Equal(t, rsp.Groups[3].Items[0].Data.Hotel.Permalink, uint64(19703708637))
	assert.Equal(t, rsp.Groups[3].Items[0].Data.Hotel.Slug, "sochi/sochi-park-otel")
	assert.Equal(t, rsp.Groups[3].Items[1].Id, "hotel-1046552916-ru")
	assert.Equal(t, rsp.Groups[3].Items[1].Title, "ФГБУ Объединенный санаторий Сочи Управления делами Президента Российской Федерации")
	assert.Equal(t, rsp.Groups[3].Items[1].Subtitle, "Санаторий · Россия, Краснодарский край, Сочи, Виноградная улица, 27")
	assert.NotNil(t, rsp.Groups[3].Items[1].Data.Hotel)
	assert.Nil(t, rsp.Groups[3].Items[1].Data.Region)
	assert.Nil(t, rsp.Groups[3].Items[1].Data.SearchParams)
	assert.Equal(t, rsp.Groups[3].Items[1].Data.Hotel.Permalink, uint64(1046552916))
	assert.Equal(t, rsp.Groups[3].Items[1].Data.Hotel.Slug, "sochi/fgbu-ob-edinennyi-sanatorii-sochi-upravleniia-delami-prezidenta-rossiiskoi-federatsii")
}

func prepareLanguageContext(ctx context.Context, acceptLanguageValue string) context.Context {
	md := metadata.MD{
		"grpcgateway-accept-language": []string{acceptLanguageValue},
	}
	return metadata.NewIncomingContext(ctx, md)
}

func TestConvertImageSize(t *testing.T) {
	res := convertImageSize(hotelsAPI.ImageSize_IMAGE_SIZE_XXL)

	assert.Equal(t, "XXL", res)
}

func TestBuildImageURL(t *testing.T) {
	imgTemplate := "https://avatars.mds.yandex.net/get-altay/2378041/2a000001746310ceb88cafdd16021be24780/%s"
	imageSize := "XXL"
	res := buildImageURL(&imgTemplate, &imageSize)

	assert.Equal(t, "https://avatars.mds.yandex.net/get-altay/2378041/2a000001746310ceb88cafdd16021be24780/XXL", res)
}

func TestConvertBbox(t *testing.T) {
	assert.Equal(t, &models.BoundingBox{
		Coordinates: []models.Coordinates{
			{
				Longitude: 37.421752533039125,
				Latitude:  55.62746978839288,
			},
			{
				Longitude: 37.756977000000006,
				Latitude:  55.847358251133244,
			},
		},
	}, convertBbox(&hotelsAPI.BoundingBox{
		LeftDown: &hotelsAPI.Coordinates{
			Longitude: 37.421752533039125,
			Latitude:  55.62746978839288,
		},
		UpRight: &hotelsAPI.Coordinates{
			Longitude: 37.756977000000006,
			Latitude:  55.847358251133244,
		},
	}))
}

func TestGetHotelInfo(t *testing.T) {
	slug := "slug"
	cfg := Config{
		Reviews: ReviewsConfig{
			AvatarSuffix:      "{size}",
			AvatarSizeDefault: "islands-75",
		},
		HotelInfo: HotelInfo{
			PhraseLimit: 3,
			ReviewLimit: 2,
			ImageLimit:  1,
		},
	}
	ctx := context.Background()
	tcMock := new(travelAPIClientMock)
	request := &models.GetHotelInfoRequest{
		ReviewPagingParams: &models.PagingParams{
			Limit:  int(cfg.HotelInfo.ReviewLimit),
			Offset: 0,
		},
		ReviewSort: "byRelevanceOrg",
		ReviewPhrase: &models.PhraseReq{
			Limit:  int(cfg.HotelInfo.PhraseLimit),
			Filter: "",
		},
		ImageParams: &models.HotelImageParams{
			PagingParams: &models.PagingParams{
				Limit:  int(cfg.HotelInfo.ImageLimit),
				Offset: 0,
			},
			OnlyTop: true,
			Sizes:   []string{"orig"},
		},
		QueryData: &models.GetHotelInfoQueryData{
			HotelSlug: slug,
		},
	}

	response := testdata.GetHotelInfoResponse
	tcMock.On("GetHotelInfo", ctx, request).Return(&response, nil)

	service := New(&nop.Logger{}, tcMock, cfg, translationServiceForTests{})
	req := &hotelsAPI.GetHotelInfoReq{
		QueryData: &hotelsAPI.HotelSuggestData{Slug: slug},
	}
	rsp, err := service.GetHotelInfo(ctx, req)
	require.NoError(t, err)

	require.Equal(t, &hotelsAPI.GetHotelInfoRsp{
		Hotel: &hotelsAPI.Hotel{
			Permalink: 1686701236,
			Name:      "Бета Измайлово",
			Coordinates: &hotelsAPI.Coordinates{
				Latitude:  55.789562,
				Longitude: 37.74772,
			},
			Address:         &wrapperspb.StringValue{Value: "Россия, Москва, Измайловское шоссе, 71, корп. 2Б"},
			Stars:           &wrapperspb.UInt32Value{Value: 3},
			Rating:          &wrapperspb.DoubleValue{Value: 4.7},
			TotalImageCount: &wrapperspb.UInt32Value{Value: 265},
			Features: []*hotelsAPI.Feature{
				{
					Name: &wrapperspb.StringValue{Value: "Wi-Fi"},
					Icon: hotelsAPI.FeatureIcon_FEATURE_ICON_WI_FI,
				},
				{
					Name: &wrapperspb.StringValue{Value: "Парковка"},
					Icon: hotelsAPI.FeatureIcon_FEATURE_ICON_CAR_PARK,
				},
				{
					Name: &wrapperspb.StringValue{Value: "Кондиционер в номере"},
					Icon: hotelsAPI.FeatureIcon_FEATURE_ICON_AIR_CONDITIONING,
				},
				{
					Name: &wrapperspb.StringValue{Value: "Оплата картой"},
					Icon: hotelsAPI.FeatureIcon_FEATURE_ICON_PAYMENT_BY_CREDIT_CARD,
				},
			},
			IsYandexHotel: &wrapperspb.BoolValue{Value: false},
			GeoInfo: &hotelsAPI.Hotel_GeoInfo{
				GeoFeatures: []*hotelsAPI.Hotel_GeoFeature{
					{
						Icon:         hotelsAPI.Hotel_GEO_FEATURE_ICON_MAIN_DISTANCE,
						Name:         "8,8\u2006км до центра",
						DistanceText: "",
					},
					{
						Icon:         hotelsAPI.Hotel_GEO_FEATURE_ICON_TRANSPORT_DISTANCE,
						Name:         "Измайлово",
						DistanceText: "257\u00a0м",
					},
				},
			},
			TotalTextReviewCount: &wrapperspb.Int32Value{
				Value: 3451,
			},
			IsFavorite: &wrapperspb.BoolValue{
				Value: false,
			},
			Images: []*hotelsAPI.Image{
				{
					UrlTemplate: "https://avatars.mds.yandex.net/get-altay/879259/2a0000016156f0d392e7d8da7592698bd77d/%s",
					Sizes: []*hotelsAPI.Image_Size{
						{
							Identifier: "XXXS",
							Width:      50,
							Height:     33,
						},
					},
					Url: "https://avatars.mds.yandex.net/get-altay/879259/2a0000016156f0d392e7d8da7592698bd77d/",
				},
			},
			HotelUrl:               "/hotels/moscow/beta-izmailovo/?adults=0&checkinDate=&checkoutDate=&childrenAges=&searchPagePollingId=&seed=app-search",
			Badges:                 nil,
			PollingFinished:        nil,
			OfferInfo:              nil,
			Rubric:                 nil,
			IsTopHotel:             nil,
			FeatureGroups:          nil,
			IsPlusAvailable:        &wrapperspb.BoolValue{Value: true},
			DisplayedLocationGeoId: nil,
		},
		SearchParams: &hotelsAPI.OfferSearchParams{
			CheckinDate: &date.Date{
				Year:  2022,
				Month: 7,
				Day:   10,
			},
			CheckoutDate: &date.Date{
				Year:  2022,
				Month: 7,
				Day:   11,
			},
			Adults:       1,
			ChildrenAges: []uint32{},
		},
		OffersInfo: &hotelsAPI.OffersInfo{
			PollingFinished: true,
			OfferSearchProgress: &hotelsAPI.OfferSearchProgress{
				Finished:         true,
				PartnersTotal:    8,
				PartnersComplete: 8,
				FinishedPartners: []hotelsAPI.PartnerId{
					hotelsAPI.PartnerId_PARTNER_ID_HOTELSCOMBINED,
					hotelsAPI.PartnerId_PARTNER_ID_OSTROVOK,
					hotelsAPI.PartnerId_PARTNER_ID_EXPEDIA,
					hotelsAPI.PartnerId_PARTNER_ID_BNOVO,
					hotelsAPI.PartnerId_PARTNER_ID_BRONEVIK,
					hotelsAPI.PartnerId_PARTNER_ID_BOOKING,
					hotelsAPI.PartnerId_PARTNER_ID_TRAVELLINE,
					hotelsAPI.PartnerId_PARTNER_ID_HOTELS101,
				},
				PendingPartners: []hotelsAPI.PartnerId{},
			},
			NextPollingRequestDelayMs: &wrapperspb.Int32Value{Value: 0},
			AggregatedOfferInfo: &hotelsAPI.AggregatedOfferInfo{
				MinPrice: &v1.Price{
					Currency: "RUB",
					Value:    2400,
				},
				MaxPrice: &v1.Price{
					Currency: "RUB",
					Value:    9675,
				},
				PansionAggregate:          hotelsAPI.PansionAggregate_PANSION_AGGREGATE_PANSION_AVAILABLE,
				CancellationInfoAggregate: hotelsAPI.CancellationInfoAggregate_CANCELLATION_INFO_AGGREGATE_FULLY_REFUNDABLE_AVAILABLE,
			},
			MainOffers: []*hotelsAPI.OfferInfo{
				{
					PansionInfo: &hotelsAPI.PansionInfo{
						PansionType: hotelsAPI.PansionType_PANSION_TYPE_ROOM_ONLY,
						PansionName: &wrapperspb.StringValue{Value: "Без питания"},
					},
					CancellationInfo: &hotelsAPI.CancellationInfo{
						HasFreeCancellation: &wrapperspb.BoolValue{Value: true},
						RefundType:          hotelsAPI.RefundType_REFUND_TYPE_FULLY_REFUNDABLE,
						RefundRules: []*hotelsAPI.RefundRule{
							{
								Type:     hotelsAPI.RefundType_REFUND_TYPE_FULLY_REFUNDABLE,
								StartsAt: nil,
								EndsAt:   &wrapperspb.StringValue{Value: "2022-07-09 11:00:00 +0000 UTC"},
								Penalty:  nil,
							},
						},
						Name: "",
					},
					YandexPlusInfo: &hotelsAPI.YandexPlusInfo{
						Eligible: false,
						Points:   240,
					},
					Price:              &wrapperspb.UInt64Value{Value: 2400},
					StrikeThroughPrice: nil,
					Name:               "[T!] Эконом с широкой кроватью • WiFi в номере",
					RoomId:             "8fa9de6dc854b5d20141fe32f146524c",
					OperatorId:         "44",
					RedirectInfo: &hotelsAPI.RedirectInfo{
						XRedirect: "https://xredirect-test.yandex.ru/redir?OfferId=33ac3466-81db-40e2-8460-455307ca5028&OfferIdHash=4226045642&ProtoLabel=OgMxMjNILGDgEnDkpOuVBni0iaSkBoIBJDMzYWMzNDY2LTgxZGItNDBlMi04NDYwLTQ1NTMwN2NhNTAyOKIBAzExObgBDcIBCjIwMjItMDctMTDIAQHSAQEx6AH-__________8B-gENdHJhdmVsLnBvcnRhbIICIHRyYXZlbC1wb3J0YWwtaG90ZWwtcGFnZS1kZXNrdG9wkAIJsgIvMTY1NjQxMDc1Mjc2Nzg0MS03MDg4MzY1MDAtYWRkcnMtdXBwZXItc3RhYmxlLTS6AiNkODUzMjQwMy1mYTIxMjc5MS1mZjJkZDI0Zi01MTFjOGE2ZvoCFXRyYXZlbC1wb3J0YWwtZGVza3RvcIIDFXRyYXZlbC1wb3J0YWwtZGVza3RvcAw",
						IsBoy:     true,
					},
					Badges: []*hotelsAPI.HotelBadge{
						{
							Id:   nil,
							Text: &wrapperspb.StringValue{Value: "240 баллов"},
							Type: hotelsAPI.HotelBadgeType_HOTEL_BADGE_TYPE_YANDEX_PLUS,
						},
					},
				},
			},
			PartnerOffers: []*hotelsAPI.HotelPartnerOffersInfo{
				{
					OperatorId:                "4",
					PansionAggregate:          hotelsAPI.PansionAggregate_PANSION_AGGREGATE_PANSION_AVAILABLE,
					CancellationInfoAggregate: 0,
					DefaultOffer: &hotelsAPI.OfferInfo{
						PansionInfo: &hotelsAPI.PansionInfo{
							PansionType: hotelsAPI.PansionType_PANSION_TYPE_BED_AND_BREAKFAST,
							PansionName: &wrapperspb.StringValue{Value: "Завтрак включён"},
						},
						CancellationInfo: &hotelsAPI.CancellationInfo{
							HasFreeCancellation: &wrapperspb.BoolValue{Value: false},
							RefundType:          hotelsAPI.RefundType_REFUND_TYPE_NON_REFUNDABLE,
							RefundRules:         []*hotelsAPI.RefundRule{},
							Name:                "",
						},
						YandexPlusInfo:     nil,
						Price:              &wrapperspb.UInt64Value{Value: 3699},
						StrikeThroughPrice: nil,
						Name:               "[T!] Случайно-сгенерированный стандартный двухместный номер (2 отдельные кровати)",
						RoomId:             "",
						OperatorId:         "4",
						RedirectInfo: &hotelsAPI.RedirectInfo{
							XRedirect: "https://xredirect-test.yandex.ru/redir?OfferId=e1a74416-bcee-423b-ac34-6f6ed5cde556&OfferIdHash=971193549&ProtoLabel=OgMxMjNIBGDzHHDmpOuVBni0iaSkBoIBJGUxYTc0NDE2LWJjZWUtNDIzYi1hYzM0LTZmNmVkNWNkZTU1NqIBBzg2ODUzODK4AR7CAQoyMDIyLTA3LTEwyAEB0gEBMegB_v__________AfoBDXRyYXZlbC5wb3J0YWyCAiB0cmF2ZWwtcG9ydGFsLWhvdGVsLXBhZ2UtZGVza3RvcJACCbICLzE2NTY0MTA3NTI3Njc4NDEtNzA4ODM2NTAwLWFkZHJzLXVwcGVyLXN0YWJsZS00ugIjNDRhYzUwYjktNWNlYjRkMzktODk4ODI5N2MtNTVjZmIxYTP6AhV0cmF2ZWwtcG9ydGFsLWRlc2t0b3CCAxV0cmF2ZWwtcG9ydGFsLWRlc2t0b3DD",
							IsBoy:     false,
						},
						Badges: []*hotelsAPI.HotelBadge{},
					},
					DefaultOfferPansion: &hotelsAPI.PansionInfo{
						PansionType: hotelsAPI.PansionType_PANSION_TYPE_BED_AND_BREAKFAST,
						PansionName: &wrapperspb.StringValue{Value: "Завтрак включён"},
					},
					DefaultOfferCancellationInfo: nil,
				},
			},
			Rooms: []*hotelsAPI.HotelRoom{
				{
					Id:   "55891996b657a4cec5a953673eafcc58",
					Name: "Эконом с двумя раздельными кроватями",
					Images: []*hotelsAPI.Image{
						{
							UrlTemplate: "https://avatars.mds.yandex.net/get-travel-rooms/3595101/2a0000017338fdcbeaf7fdfb4a3e798ac087/%s",
							Sizes: []*hotelsAPI.Image_Size{
								{
									Identifier: "orig",
									Width:      900,
									Height:     600,
								},
							},
							Url: "https://avatars.mds.yandex.net/get-travel-rooms/3595101/2a0000017338fdcbeaf7fdfb4a3e798ac087/",
							SizeOrig: &hotelsAPI.Image_Size{
								Identifier: "orig",
								Width:      900,
								Height:     600,
							},
						},
					},
					Description: "Номер с двумя раздельными кроватями, оснащенный отдельной ванной комнатой. Общая площадь номера составляет  22 кв.м.",
					BedGroups: []*hotelsAPI.RoomBedGroup{
						{
							Items: []*hotelsAPI.RoomBedItem{
								{
									Icon:     hotelsAPI.RoomBedIcon_ROOM_BED_ICON_SINGLE,
									Quantity: 2,
								},
							},
						},
					},
					Area: &hotelsAPI.RoomArea{
						Value: 22,
						Unit:  hotelsAPI.RoomAreaUnits_ROOM_AREA_UNITS_SQUARE_METERS,
					},
					MainAmenities: []*hotelsAPI.RoomAmenity{
						{
							Name: "Бесплатный Wi‑Fi",
							IconValue: &hotelsAPI.RoomAmenity_Icon{
								Icon: hotelsAPI.RoomAmenityIcon_ROOM_AMENITY_WIFI,
							},
						},
					},
					AmenityGroups: []*hotelsAPI.RoomAmenityGroup{
						{
							Name: "Интернет и телефония",
							Amenities: []*hotelsAPI.RoomAmenity{
								{
									Name: "Телефон",
									IconValue: &hotelsAPI.RoomAmenity_Icon{
										Icon: hotelsAPI.RoomAmenityIcon_ROOM_AMENITY_UNKNOWN,
									},
								},
							},
						},
						{
							Name: "Сон",
							Amenities: []*hotelsAPI.RoomAmenity{
								{
									Name: "Двухъярусные кровати",
									IconValue: &hotelsAPI.RoomAmenity_BedItem{
										BedItem: nil,
									},
								},
								{
									Name: "Восемь односпальных кроватей",
									IconValue: &hotelsAPI.RoomAmenity_BedItem{
										BedItem: &hotelsAPI.RoomBedItem{
											Icon:     hotelsAPI.RoomBedIcon_ROOM_BED_ICON_SINGLE,
											Quantity: 8,
										},
									},
								},
							},
						},
					},
				},
			},
			Operators: map[string]*hotelsAPI.HotelsOperator{
				"44": {
					Id:      "44",
					Name:    "Яндекс.Путешествия",
					IconUrl: "https://yastatic.net/s3/travel-indexer/icons/travel.svg",
				},
			},
			OfferCount:     5,
			OperatorsCount: 2,
		},
		RatingsInfo: &hotelsAPI.RatingsInfo{
			Teaser: "100% гостей понравилось питание",
			FeatureRatings: []*hotelsAPI.FeatureRatingInfo{
				{
					Name:            "Питание",
					PositivePercent: 100,
				},
			},
		},
		ReviewsInfo: &hotelsAPI.ReviewsRsp{
			HasMore: true,
			Phrases: &hotelsAPI.ReviewQuickFilterPhrase{
				Selected: "",
				Available: []*hotelsAPI.Keyphrase{
					{
						Name:        "хорошая гостиница",
						ReviewCount: 677,
					},
				},
				TotalCount: 10,
			},
			Sort: &hotelsAPI.ReviewQuickFilterSort{
				Selected: hotelsAPI.ReviewSort_REVIEW_SORT_RELEVANT_FIRST,
				Available: []*hotelsAPI.ReviewQuickSorter{
					{
						Id: hotelsAPI.ReviewSort_REVIEW_SORT_RELEVANT_FIRST,
					},
					{
						Id: hotelsAPI.ReviewSort_REVIEW_SORT_TIME_DESC,
					},
					{
						Id: hotelsAPI.ReviewSort_REVIEW_SORT_RATING_ASC,
					},
					{
						Id: hotelsAPI.ReviewSort_REVIEW_SORT_RATING_DESC,
					},
				},
			},
			Reviews: []*hotelsAPI.ReviewRsp{
				{
					Author: &hotelsAPI.Author{
						AvatarUrl:  "https://avatars.mds.yandex.net/get-yapic/15298/enc-922ab6ca0a767bd2e12332ee27f071e8/islands-75",
						Level:      "Знаток города 7 уровня",
						Name:       "Дмитрий Аввакумов",
						ProfileUrl: "https://reviews.yandex.ru/user/0gwkz2j6mzxnwtjfyyphaxa8a8",
					},
					BusinessComment: "",
					CommentCount:    1,
					Id:              "5lQsa6r6kSkv6uyhufS5BUP01aNV-1g",
					Images: []*hotelsAPI.ImageReview{
						{
							Id: "A6WyBexdv1B8hlD5YB1dHUXeYfa7Ef",
							Image: &hotelsAPI.Image{
								UrlTemplate: "https://avatars.mds.yandex.net/get-altay/5482016/2a0000017e3e2cb38551e583bcf33d12c374/%s",
								Sizes:       nil,
								Url:         "",
								SizeOrig:    nil,
							},
							Moderation: &hotelsAPI.Moderation{
								Status: hotelsAPI.ModerationStatus_MODERATION_STATUS_ACCEPTED,
							},
							Tags: nil,
						},
					},
					PhraseMatch:       nil,
					Moderation:        nil,
					Rating:            3,
					Snippet:           "Гостиница по местоположению очень удобная, здесь метро Партизанская и МЦК Измайлово буквально в 50 метрах...",
					Text:              "Гостиница по местоположению очень удобная, здесь метро Партизанская и МЦК Измайлово буквально в 50 метрах, рядом есть блошиный рынок Измайловского Кремля, много точек быстрого питания и крупный торговый центр\n\nВнутри же гостиница тянет на 2* не более\nОбязательно всем иметь паспорт!\nИначе не заселят\nДизайн холла в стиле 70х годов ХХ века\nНомера исключительно с одноместными кроватями\nРай для командировочных \nОтдохнуть здесь не получится, лучше взять Альфу или Гамму! \nПитание на 3 из 10 баллов, лучше сходить в Якиторию или грузинский ресторанчик неподалёку, очень много молодых пьяных людей вечером здесь, не рекомендую данный корпус, советую Гамму или Дельту для спокойного отдыха, однако не пользуйтесь баней, там грязно! Всем добра!",
					TotalDislikeCount: 2,
					TotalLikeCount:    5,
					UpdatedAt:         "",
					UserReaction:      hotelsAPI.ReviewReactionType_REVIEW_REACTION_NONE,
				},
			},
			TotalReviewCount: 3451,
			UserReview:       nil,
		},
		HotelDescription: "абвгд",
	}, rsp)
}

func TestGetHotelImages(t *testing.T) {
	slug := "slug"
	ctx := context.Background()
	tcMock := new(travelAPIClientMock)
	request := &models.GetHotelImagesRequest{
		QueryData: &models.GetHotelInfoQueryData{
			HotelSlug: slug,
		},
		ImageParams: &models.HotelImageParams{
			PagingParams: &models.PagingParams{},
			OnlyTop:      true,
			Sizes:        []string{"orig"},
		},
	}

	response := testdata.GetHotelImagesResponse
	tcMock.On("GetHotelImages", ctx, request).Return(&response, nil)

	service := New(&nop.Logger{}, tcMock, Config{}, translationServiceForTests{})
	req := &hotelsAPI.GetHotelImagesReq{HotelId: &hotelsAPI.HotelID{Value: &hotelsAPI.HotelID_HotelSlug{
		HotelSlug: slug,
	}}}
	rsp, err := service.GetHotelImages(ctx, req)
	require.NoError(t, err)

	require.Equal(t, &hotelsAPI.GetHotelImagesRsp{
		Images: []*hotelsAPI.Image{
			{
				UrlTemplate: "https://avatars.mds.yandex.net/get-altay/6221595/2a0000018042a24fa7b7f218389b79796adb/%s",
				Url:         "https://avatars.mds.yandex.net/get-altay/6221595/2a0000018042a24fa7b7f218389b79796adb/",
				Sizes: []*hotelsAPI.Image_Size{
					{
						Identifier: "XS",
						Width:      100,
						Height:     66,
					},
					{
						Identifier: "orig",
						Width:      2048,
						Height:     1351,
					},
				},
				SizeOrig: &hotelsAPI.Image_Size{
					Identifier: "orig",
					Width:      2048,
					Height:     1351,
				},
			},
			{
				UrlTemplate: "https://avatars.mds.yandex.net/get-altay/2378041/2a0000017526d61fdc3279ce161af20d2cea/%s",
				Url:         "https://avatars.mds.yandex.net/get-altay/2378041/2a0000017526d61fdc3279ce161af20d2cea/",
				Sizes: []*hotelsAPI.Image_Size{
					{
						Identifier: "XL",
						Width:      800,
						Height:     534,
					},
				},
			},
		},
	}, rsp)
}

func TestGetHotelOffers(t *testing.T) {
	slug := "slug"
	ctx := context.Background()
	tcMock := new(travelAPIClientMock)
	request := &models.GetHotelOffersRequest{
		QueryData: &models.GetHotelInfoQueryData{
			HotelSlug: slug,
		},
		Params: &models.SearchParams{
			Adults:       1,
			CheckinDate:  "2022-01-01",
			CheckoutDate: "2022-01-02",
		},
	}

	response := testdata.GetHotelOffersResponse
	tcMock.On("GetHotelOffers", ctx, request).Return(&response, nil)

	service := New(&nop.Logger{}, tcMock, Config{}, translationServiceForTests{})
	req := &hotelsAPI.GetHotelOffersReq{
		HotelId: &hotelsAPI.HotelID{Value: &hotelsAPI.HotelID_HotelSlug{HotelSlug: slug}},
		Params: &hotelsAPI.OfferSearchParams{
			Adults:       1,
			CheckinDate:  &date.Date{Year: 2022, Month: 1, Day: 1},
			CheckoutDate: &date.Date{Year: 2022, Month: 1, Day: 2},
		},
	}
	rsp, err := service.GetHotelOffers(ctx, req)
	require.NoError(t, err)

	require.Equal(t, &hotelsAPI.GetHotelOffersRsp{
		OffersInfo: &hotelsAPI.OffersInfo{
			PollingFinished: true,
			OfferSearchProgress: &hotelsAPI.OfferSearchProgress{
				Finished:         true,
				PartnersTotal:    3,
				PartnersComplete: 3,
				PendingPartners:  []hotelsAPI.PartnerId{},
				FinishedPartners: []hotelsAPI.PartnerId{
					hotelsAPI.PartnerId_PARTNER_ID_BOOKING, hotelsAPI.PartnerId_PARTNER_ID_OSTROVOK, hotelsAPI.PartnerId_PARTNER_ID_TRAVELLINE,
				},
			},
			Rooms: []*hotelsAPI.HotelRoom{
				{
					Id:          "afe68e4a2566d1954eb9cf02259aebc1",
					Name:        "Койко-место в общем 8-местном номере",
					Description: "Койко-место 80×190см в двухуровневой кровати в восьмиместном номере, выключатель у изголовья кровати, постельные принадлежности, 2 полотенца, стул для каждого, вешалки для одежды, розетки по количеству койко-мест, кухня обустроенная всей необходимой техникой  посудой и мебелью. На кухне телевизор. В хостеле есть wi-fi. Несколько санитарно- гигиенических помещений. Есть отдельная система хранения вещей.",
					Images: []*hotelsAPI.Image{
						{
							UrlTemplate: "https://avatars.mds.yandex.net/get-travel-rooms/3613454/2a0000017f9f9b2f13246d613ab82d8728a8/%s",
							SizeOrig: &hotelsAPI.Image_Size{
								Identifier: "orig",
								Width:      1440,
								Height:     1080,
							},
							Url: "https://avatars.mds.yandex.net/get-travel-rooms/3613454/2a0000017f9f9b2f13246d613ab82d8728a8/",
							Sizes: []*hotelsAPI.Image_Size{
								{
									Identifier: "orig",
									Width:      1440,
									Height:     1080,
								},
							},
						},
					},
					AmenityGroups: []*hotelsAPI.RoomAmenityGroup{
						{
							Name: "Удобства",
							Amenities: []*hotelsAPI.RoomAmenity{
								{
									Name: "Обслуживание номеров (услуги горничной)",
									IconValue: &hotelsAPI.RoomAmenity_Icon{
										Icon: hotelsAPI.RoomAmenityIcon_ROOM_AMENITY_UNKNOWN, //TODO(adurnev) нет такой групы почему?
									},
								},
							},
						},
						{
							Name: "Сон",
							Amenities: []*hotelsAPI.RoomAmenity{
								{
									Name: "Двухъярусные кровати",
									IconValue: &hotelsAPI.RoomAmenity_BedItem{
										BedItem: nil,
									},
								},
								{
									Name: "Восемь односпальных кроватей",
									IconValue: &hotelsAPI.RoomAmenity_BedItem{
										BedItem: &hotelsAPI.RoomBedItem{
											Icon:     hotelsAPI.RoomBedIcon_ROOM_BED_ICON_SINGLE,
											Quantity: 8,
										},
									},
								},
							},
						},
					},
					MainAmenities: []*hotelsAPI.RoomAmenity{
						{
							Name: "Вид во двор",
							IconValue: &hotelsAPI.RoomAmenity_Icon{
								Icon: hotelsAPI.RoomAmenityIcon_ROOM_AMENITY_COURT_VIEW,
							},
						},
						{
							Name: "Бесплатный Wi‑Fi",
							IconValue: &hotelsAPI.RoomAmenity_Icon{
								Icon: hotelsAPI.RoomAmenityIcon_ROOM_AMENITY_WIFI,
							},
						},
					},
					BedGroups: []*hotelsAPI.RoomBedGroup{
						{
							Items: []*hotelsAPI.RoomBedItem{
								{
									Icon:     hotelsAPI.RoomBedIcon_ROOM_BED_ICON_DOUBLE,
									Quantity: 1,
								},
							},
						},
						{
							Items: []*hotelsAPI.RoomBedItem{
								{
									Icon:     hotelsAPI.RoomBedIcon_ROOM_BED_ICON_SINGLE,
									Quantity: 2,
								},
							},
						},
					},
					Area: &hotelsAPI.RoomArea{
						Value: 24,
						Unit:  hotelsAPI.RoomAreaUnits_ROOM_AREA_UNITS_SQUARE_METERS,
					},
				},
			},
			NextPollingRequestDelayMs: &wrapperspb.Int32Value{Value: 0},
			AggregatedOfferInfo: &hotelsAPI.AggregatedOfferInfo{
				MinPrice: &v1.Price{
					Value:    55000,
					Currency: "RUB",
				},
				MaxPrice: &v1.Price{
					Value:    55000,
					Currency: "RUB",
				},
				PansionAggregate:          hotelsAPI.PansionAggregate_PANSION_AGGREGATE_UNKNOWN,
				CancellationInfoAggregate: hotelsAPI.CancellationInfoAggregate_CANCELLATION_INFO_AGGREGATE_FULLY_REFUNDABLE_AVAILABLE,
			},
			MainOffers: []*hotelsAPI.OfferInfo{
				{
					PansionInfo: &hotelsAPI.PansionInfo{
						PansionType: hotelsAPI.PansionType_PANSION_TYPE_ROOM_ONLY,
						PansionName: &wrapperspb.StringValue{Value: "Без питания"},
					},
					CancellationInfo: &hotelsAPI.CancellationInfo{
						HasFreeCancellation: &wrapperspb.BoolValue{Value: true},
						RefundType:          hotelsAPI.RefundType_REFUND_TYPE_FULLY_REFUNDABLE,
						RefundRules: []*hotelsAPI.RefundRule{
							{
								Type:     hotelsAPI.RefundType_REFUND_TYPE_FULLY_REFUNDABLE,
								StartsAt: nil,
								EndsAt:   &wrapperspb.StringValue{Value: "2022-07-09 12:00:00 +0000 UTC"},
								Penalty:  nil,
							},
						},
						Name: "",
					},
					YandexPlusInfo: &hotelsAPI.YandexPlusInfo{
						Eligible: false,
						Points:   278,
					},
					Price:              &wrapperspb.UInt64Value{Value: 2780},
					StrikeThroughPrice: nil,
					Name:               "[T!] Стандартный • WiFi в номере • Онлайн-регистрация • Спецпредложения от партнеров",
					RoomId:             "3951579e2881b550e01f5ca33e562392",
					OperatorId:         "44",
					RedirectInfo: &hotelsAPI.RedirectInfo{
						XRedirect: "https://xredirect-test.yandex.ru/redir?OfferId=1058d4f9-bf63-40df-ae98-4b92a74168b5&OfferIdHash=349230869&ProtoLabel=OgMxMjNILGDcFXCBjI-WBniz55mQBYIBJDEwNThkNGY5LWJmNjMtNDBkZi1hZTk4LTRiOTJhNzQxNjhiNaIBAzEwMLgBDcIBCjIwMjItMDctMTDIAQHSAQEx6AH-__________8B-gENdHJhdmVsLnBvcnRhbIICIHRyYXZlbC1wb3J0YWwtaG90ZWwtcGFnZS1kZXNrdG9wkAIJsgIwMTY1NzAwMDkyNTc0MjU3OS0zNDA5NjAwNTYzLWFkZHJzLXVwcGVyLXN0YWJsZS00ugIiYWNiYWY4MzMtZTJmZGU0MmItNzZlOGQ1ZC00OGJlYTlhNvoCFXRyYXZlbC1wb3J0YWwtZGVza3RvcIIDFXRyYXZlbC1wb3J0YWwtZGVza3RvcGk",
						IsBoy:     true,
					},
					Badges: []*hotelsAPI.HotelBadge{
						{
							Id:   nil,
							Text: &wrapperspb.StringValue{Value: "278 баллов"},
							Type: hotelsAPI.HotelBadgeType_HOTEL_BADGE_TYPE_YANDEX_PLUS,
						},
					},
				},
			},
			Operators: map[string]*hotelsAPI.HotelsOperator{
				"2": {
					Id:      "2",
					Name:    "Booking.com",
					IconUrl: "https://yastatic.net/s3/travel-indexer/icons/booking.svg",
				},
				"4": {
					Id:      "4",
					Name:    "Ostrovok.ru",
					IconUrl: "https://yastatic.net/s3/travel-indexer/icons/ostrovok.svg",
				},
			},
			OfferCount:     1,
			OperatorsCount: 1,
			PartnerOffers: []*hotelsAPI.HotelPartnerOffersInfo{
				{
					DefaultOfferCancellationInfo: &hotelsAPI.CancellationInfo{
						HasFreeCancellation: &wrapperspb.BoolValue{Value: true},
						RefundType:          hotelsAPI.RefundType_REFUND_TYPE_FULLY_REFUNDABLE,
						RefundRules: []*hotelsAPI.RefundRule{
							{
								Type:   hotelsAPI.RefundType_REFUND_TYPE_FULLY_REFUNDABLE,
								EndsAt: &wrapperspb.StringValue{Value: "2022-07-09 08:59:00 +0000 UTC"},
							},
						},
					},
					OperatorId:                "4",
					CancellationInfoAggregate: hotelsAPI.CancellationInfoAggregate_CANCELLATION_INFO_AGGREGATE_FULLY_REFUNDABLE_AVAILABLE,
					PansionAggregate:          hotelsAPI.PansionAggregate_PANSION_AGGREGATE_UNKNOWN,
					DefaultOffer: &hotelsAPI.OfferInfo{
						Name: "Кровать в общем номере (женский номер) (общая ванная комната) (8 кроватей, дополнительная кровать (без питания) включена)",
						CancellationInfo: &hotelsAPI.CancellationInfo{
							Name: "",
							RefundRules: []*hotelsAPI.RefundRule{
								{
									Type:   hotelsAPI.RefundType_REFUND_TYPE_FULLY_REFUNDABLE,
									EndsAt: &wrapperspb.StringValue{Value: "2022-07-09 08:59:00 +0000 UTC"},
								},
							},
							RefundType:          hotelsAPI.RefundType_REFUND_TYPE_FULLY_REFUNDABLE,
							HasFreeCancellation: &wrapperspb.BoolValue{Value: true},
						},
						OperatorId: "4",
						Badges:     []*hotelsAPI.HotelBadge{},
						RedirectInfo: &hotelsAPI.RedirectInfo{
							XRedirect: "https://travel.yandex.ru/redir?DebugPortalHost=travel.yandex.ru&OfferId=47cde497-5173-4b07-8605-54192d8fc84d&OfferIdHash=2617783070&ProtoLabel=CgZ5YW5kZXgSBnNlYXJjaBo-Y246c3NhX2h0bF9rNTAuaG90ZWwuY2l0eS5sZG5nLmh0bC1tYWluLWt3X3J1X2FsbHxjaWQ6NzM0MDYyNjkikwFhaWQ6MTIwMjI3Njg4OTV8Ymk6MTIwMjI3Njg4OTV8Y3Q6dHlwZTF8Z2lkOjQ4ODY5MDM5ODB8cHN0OjF8cHN0OnByZW1pdW18ZHQ6ZGVza3RvcHxwbNGBOm5vbmV8cGxjdDpzZWFyY2h8Y2djaTowfHJuOtCc0L7RgdC60LLQsHxyaWQ6MjEzfHJ0aWQ6fG1haW4qNGt3OtC-0YLQtdC70Lgg0LrRgNCw0YHQvdC-0LPQvtGA0YHQunxraWQ6MzgxNzAwNjQ0NzE6EzQ1MjI5Mzg3NDE2Mzk2NDgxMTlIBGDYrQNwx4KglAZ419WdzYsDggEkNDdjZGU0OTctNTE3My00YjA3LTg2MDUtNTQxOTJkOGZjODRkogEHOTk5MTY3MKoBCjE0OTA4NTQ1MzG4AR7CAQoyMDIyLTA3LTEwyAEL0gEBNOgBAfIBEzQ1MjI5Mzg3NDE2Mzk2NDgxMTn6AQ10cmF2ZWwucG9ydGFsggIgdHJhdmVsLXBvcnRhbC1ob3RlbC1wYWdlLWRlc2t0b3CQAgmaAgdkZXNrdG9wqgIgNWU0NjAzNDdjN2MyOGMxZDZjNzQ2MzEzY2FlNTVkNjayAiBlOWQ4NWY5MDA4MDAwZjMyODU3ZDIxZWUzM2Q5OTkyN7oCIzY1NmE5NTY5LTU3NGFlMGNkLTNlYjc1YzNhLThkYWM4NmVh4gITMTYzOTY0ODEyNDE1NDk4NzM0MOoCG_OyIMqbIuucI7HjIqDAId6zHfyuHYbpH8OVI_ICSP___________wH___________8B____________Af___________wH___________8B____________Af___________wFfRvoCFXRyYXZlbC1wb3J0YWwtZGVza3RvcIIDFXRyYXZlbC1wb3J0YWwtZGVza3RvcJIDEzQ1MjI5Mzg3NDE2Mzk2NDgxMTnCAyoxYmYyODMxNTE5NTk5ZTlkZjE3MTBmNjc3OGMwOTQtMC1uZXdzZWFyY2gf",
							IsBoy:     false,
						},
						RoomId: "",
						Price:  &wrapperspb.UInt64Value{Value: 55000},
						PansionInfo: &hotelsAPI.PansionInfo{
							PansionName: &wrapperspb.StringValue{Value: "Без питания"},
							PansionType: hotelsAPI.PansionType_PANSION_TYPE_ROOM_ONLY,
						},
						YandexPlusInfo:     nil,
						StrikeThroughPrice: nil,
					},
				},
			},
		},
	}, rsp)
}
