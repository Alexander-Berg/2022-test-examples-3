package handler

import (
	"context"
	"testing"

	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"google.golang.org/genproto/googleapis/type/date"
	"google.golang.org/grpc"
	"google.golang.org/grpc/metadata"

	"a.yandex-team.ru/library/go/yandex/blackbox"
	v1 "a.yandex-team.ru/travel/app/backend/api/avia/v1"
	personalizationAPI "a.yandex-team.ru/travel/app/backend/api/personalization/v1"
	"a.yandex-team.ru/travel/app/backend/internal/common"
	"a.yandex-team.ru/travel/app/backend/internal/points"
	"a.yandex-team.ru/travel/app/backend/internal/references/factories"
	personalsearch "a.yandex-team.ru/travel/avia/personalization/api/personal_search/v2"
	"a.yandex-team.ru/travel/library/go/logging"
)

type aviaPersonalizationClientMock struct {
	mock.Mock
}

func (a *aviaPersonalizationClientMock) GetAviaHistory(ctx context.Context, in *personalsearch.TGetAviaHistoryRequestV2, opts ...grpc.CallOption) (*personalsearch.TGetPersonalSearchResponseV2, error) {
	args := a.MethodCalled("GetAviaHistory", ctx, in)
	return args.Get(0).(*personalsearch.TGetPersonalSearchResponseV2), args.Error(1)
}

func (a *aviaPersonalizationClientMock) GetAviaSuggest(ctx context.Context, in *personalsearch.TGetAviaSuggestRequestV2, opts ...grpc.CallOption) (*personalsearch.TGetPersonalSearchResponseV2, error) {
	args := a.MethodCalled("GetAviaSuggest", ctx, in)
	return args.Get(0).(*personalsearch.TGetPersonalSearchResponseV2), args.Error(1)
}

func (a *aviaPersonalizationClientMock) GetHotelsSuggest(ctx context.Context, in *personalsearch.TGetHotelsSuggestRequestV2, opts ...grpc.CallOption) (*personalsearch.TGetPersonalSearchResponseV2, error) {
	args := a.MethodCalled("GetHotelsSuggest", ctx, in)
	return args.Get(0).(*personalsearch.TGetPersonalSearchResponseV2), args.Error(1)
}

func TestAviaShortcuts_OK(t *testing.T) {
	ctx := prepareLanguageContext(context.Background(), "ru-RU")
	ctx = prepareAuthenticatedContext(ctx)

	aviaPersonalizationClient := new(aviaPersonalizationClientMock)
	aviaPersonalizationClient.Mock.On("GetAviaHistory", ctx, &personalsearch.TGetAviaHistoryRequestV2{
		PassportId: "123",
		Limit:      100,
	}).Return(&personalsearch.TGetPersonalSearchResponseV2{
		Entries: []*personalsearch.TPersonalSearchEntryV2{
			mskToKznAviaSearchEntry(),
		},
	}, nil)
	logger, _ := logging.New(&logging.DefaultConfig)
	registry := factories.CreateDefaultRegistry(logger)
	handler := NewGRPCPersonalizationHandler(logger, aviaPersonalizationClient, points.NewParser(registry))
	rsp, err := handler.SearchShortcuts(ctx, &personalizationAPI.SearchShortcutsReq{
		EnabledShortcuts: []personalizationAPI.ShortcutsType{personalizationAPI.ShortcutsType_SHORTCUT_TYPE_AVIA},
	})
	require.NoError(t, err)
	require.EqualValues(t, len(rsp.AviaShortcuts), 1)
	require.Equal(t, rsp.AviaShortcuts[0].SearchParams.ServiceClass, v1.ServiceClass_SERVICE_CLASS_ECONOMY)
	require.Equal(t, rsp.AviaShortcuts[0].SearchParams.Passengers, &v1.Passengers{
		Adults: 1,
	})
	require.Equal(t, rsp.AviaShortcuts[0].SearchParams.PointKeyFrom, "c213")
	require.Equal(t, rsp.AviaShortcuts[0].SearchParams.PointKeyTo, "c43")
	require.Equal(t, rsp.AviaShortcuts[0].SearchParams.DateForward, &date.Date{
		Year:  2022,
		Month: 6,
		Day:   28,
	})
	require.Equal(t, rsp.AviaShortcuts[0].TitleFrom, "Москва")
	require.Equal(t, rsp.AviaShortcuts[0].TitleTo, "Казань")
}

func TestAviaSearchHistory_EnOk(t *testing.T) {
	ctx := prepareLanguageContext(context.Background(), "en-US")
	ctx = prepareAuthenticatedContext(ctx)

	aviaPersonalizationClient := new(aviaPersonalizationClientMock)
	aviaPersonalizationClient.Mock.On("GetAviaHistory", ctx, &personalsearch.TGetAviaHistoryRequestV2{
		PassportId: "123",
		Limit:      100,
	}).Return(&personalsearch.TGetPersonalSearchResponseV2{
		Entries: []*personalsearch.TPersonalSearchEntryV2{
			mskToKznAviaSearchEntry(),
		},
	}, nil)
	logger, _ := logging.New(&logging.DefaultConfig)
	registry := factories.CreateDefaultRegistry(logger)
	handler := NewGRPCPersonalizationHandler(logger, aviaPersonalizationClient, points.NewParser(registry))
	rsp, err := handler.SearchShortcuts(ctx, &personalizationAPI.SearchShortcutsReq{
		EnabledShortcuts: []personalizationAPI.ShortcutsType{personalizationAPI.ShortcutsType_SHORTCUT_TYPE_AVIA},
	})
	require.NoError(t, err)
	require.EqualValues(t, len(rsp.AviaShortcuts), 1)
	require.Equal(t, rsp.AviaShortcuts[0].TitleFrom, "Moscow")
	require.Equal(t, rsp.AviaShortcuts[0].TitleTo, "Kazan")
}

func TestAviaSearchHistory_IgnoreInvalid(t *testing.T) {
	ctx := prepareLanguageContext(context.Background(), "ru-RU")
	ctx = prepareAuthenticatedContext(ctx)

	aviaPersonalizationClient := new(aviaPersonalizationClientMock)
	invalidClassEntry := mskToKznAviaSearchEntry()
	invalidClassEntry.GetAvia().AviaClass = "invalid"
	invalidPointEntry := mskToKznAviaSearchEntry()
	invalidPointEntry.GetAvia().PointTo.PointCode = "ccc0"
	unknownPointEntry := mskToKznAviaSearchEntry()
	unknownPointEntry.GetAvia().PointFrom.PointCode = "c999"
	aviaPersonalizationClient.Mock.On("GetAviaHistory", ctx, &personalsearch.TGetAviaHistoryRequestV2{
		PassportId: "123",
		Limit:      100,
	}).Return(&personalsearch.TGetPersonalSearchResponseV2{
		Entries: []*personalsearch.TPersonalSearchEntryV2{
			invalidClassEntry,
			invalidPointEntry,
			unknownPointEntry,
			mskToKznAviaSearchEntry(),
		},
	}, nil)
	logger, _ := logging.New(&logging.DefaultConfig)
	registry := factories.CreateDefaultRegistry(logger)
	handler := NewGRPCPersonalizationHandler(logger, aviaPersonalizationClient, points.NewParser(registry))
	rsp, err := handler.SearchShortcuts(ctx, &personalizationAPI.SearchShortcutsReq{
		EnabledShortcuts: []personalizationAPI.ShortcutsType{personalizationAPI.ShortcutsType_SHORTCUT_TYPE_AVIA},
	})
	require.NoError(t, err)
	require.EqualValues(t, len(rsp.AviaShortcuts), 1)
}

func TestHotelsShortcuts_OK(t *testing.T) {
	ctx := prepareLanguageContext(context.Background(), "ru-RU")
	ctx = prepareAuthenticatedContext(ctx)

	aviaPersonalizationClient := new(aviaPersonalizationClientMock)
	aviaPersonalizationClient.Mock.On("GetHotelsSuggest", ctx, &personalsearch.TGetHotelsSuggestRequestV2{
		PassportId:    "123",
		OrdersLimit:   100,
		SearchesLimit: 100,
	}).Return(&personalsearch.TGetPersonalSearchResponseV2{
		Entries: []*personalsearch.TPersonalSearchEntryV2{
			mskHotelSearchEntry(),
		},
	}, nil)
	logger, _ := logging.New(&logging.DefaultConfig)
	registry := factories.CreateDefaultRegistry(logger)
	handler := NewGRPCPersonalizationHandler(logger, aviaPersonalizationClient, points.NewParser(registry))
	rsp, err := handler.SearchShortcuts(ctx, &personalizationAPI.SearchShortcutsReq{
		EnabledShortcuts: []personalizationAPI.ShortcutsType{personalizationAPI.ShortcutsType_SHORTCUT_TYPE_HOTELS},
	})
	require.NoError(t, err)
	require.EqualValues(t, len(rsp.AviaShortcuts), 0)
	require.EqualValues(t, len(rsp.HotelsShortcuts), 1)
	require.EqualValues(t, rsp.HotelsShortcuts[0].Adults, 2)
	require.Equal(t, rsp.HotelsShortcuts[0].ChildrenAges, []uint32{5, 8})
	require.Equal(t, rsp.HotelsShortcuts[0].PointKey, "c213")
	require.Equal(t, rsp.HotelsShortcuts[0].CheckinDate, &date.Date{
		Year:  2022,
		Month: 6,
		Day:   28,
	})
	require.Equal(t, rsp.HotelsShortcuts[0].CheckoutDate, &date.Date{
		Year:  2022,
		Month: 7,
		Day:   5,
	})
	require.Equal(t, rsp.HotelsShortcuts[0].PointTitle, "Москва")
}

func prepareLanguageContext(ctx context.Context, acceptLanguageValue string) context.Context {
	md := metadata.MD{
		"grpcgateway-accept-language": []string{acceptLanguageValue},
	}
	return metadata.NewIncomingContext(ctx, md)
}

func prepareAuthenticatedContext(ctx context.Context) context.Context {
	return context.WithValue(ctx, common.AuthMarker, common.AuthInfo{
		Authenticated: true,
		User: &blackbox.User{
			ID:    123,
			Login: "test_user",
		},
	})
}

func mskToKznAviaSearchEntry() *personalsearch.TPersonalSearchEntryV2 {
	return &personalsearch.TPersonalSearchEntryV2{
		Entry: &personalsearch.TPersonalSearchEntryV2_Avia{Avia: &personalsearch.TAviaEntry{
			AviaClass: "economy",
			PointFrom: &personalsearch.TGeoPoint{
				PointCode: "c213",
			},
			PointTo: &personalsearch.TGeoPoint{
				PointCode: "c43",
			},
			When: "2022-06-28",
			Travelers: &personalsearch.TTravelers{
				Adults: 1,
			},
		}},
	}
}

func mskHotelSearchEntry() *personalsearch.TPersonalSearchEntryV2 {
	return &personalsearch.TPersonalSearchEntryV2{
		Entry: &personalsearch.TPersonalSearchEntryV2_Hotel{Hotel: &personalsearch.THotelEntry{
			PointTo: &personalsearch.TGeoPoint{
				PointCode: "c213",
				GeoId:     213,
			},
			CheckInDate:  "2022-06-28",
			CheckOutDate: "2022-07-05",
			Travelers: &personalsearch.TTravelers{
				Adults:       2,
				ChildrenAges: []int32{5, 8},
			},
		}},
	}
}
