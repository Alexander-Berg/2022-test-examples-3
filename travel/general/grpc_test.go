package handler

import (
	"context"
	"net"
	"net/http"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"google.golang.org/genproto/googleapis/type/date"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/metadata"
	"google.golang.org/grpc/status"

	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/core/log/ctxlog"
	"a.yandex-team.ru/library/go/core/log/nop"
	"a.yandex-team.ru/library/go/ptr"
	aviaAPI "a.yandex-team.ru/travel/app/backend/api/avia/v1"
	commonAPI "a.yandex-team.ru/travel/app/backend/api/common/v1"
	"a.yandex-team.ru/travel/app/backend/internal/avia"
	"a.yandex-team.ru/travel/app/backend/internal/lib/aviabackendclient"
	"a.yandex-team.ru/travel/app/backend/internal/lib/aviasuggestclient"
	"a.yandex-team.ru/travel/app/backend/internal/lib/aviatdapiclient"
	"a.yandex-team.ru/travel/app/backend/internal/lib/clientscommon"
	exp3pb "a.yandex-team.ru/travel/app/backend/internal/lib/exp3matcher/proto/v1"
	"a.yandex-team.ru/travel/avia/library/go/services/featureflag"
	"a.yandex-team.ru/travel/library/go/geobase"
	"a.yandex-team.ru/travel/library/go/unifiedagent"
)

var (
	featureFlags        = featureflag.NewFlags(nil, nil)
	featureFlagsStorage = featureflag.NewMockStorage(&featureFlags)
)

type TimeoutError struct {
	error
}

func (e TimeoutError) Timeout() bool {
	return true
}

func (e TimeoutError) Temporary() bool {
	return true
}

func (e TimeoutError) Error() string {
	return "timeout Error!"
}

var exp3tdAPIConfig = &exp3pb.TdApiConfig{
	InstantSearchEnabled: true,
}

type tdapiClientMock struct {
	mock.Mock
}

func (c *tdapiClientMock) InitSearch(
	ctx context.Context,
	nationalVersion string,
	lang string,
	adults uint32,
	children uint32,
	infants uint32,
	dateForward aviatdapiclient.Date,
	dateBackward *aviatdapiclient.Date,
	serviceClass aviatdapiclient.ServiceClass,
	pointFrom string,
	pointTo string,
) (*aviatdapiclient.InitSearchRsp, error) {
	args := c.Called(ctx, nationalVersion, lang, adults, children, infants, dateForward, dateBackward, serviceClass, pointFrom, pointTo)
	return args.Get(0).(*aviatdapiclient.InitSearchRsp), args.Error(1)
}

func (c *tdapiClientMock) SearchResult(ctx context.Context, qid string, exp3tdAPIConfig *exp3pb.TdApiConfig) (*aviatdapiclient.SearchResultRsp, error) {
	args := c.Called(ctx, qid, exp3tdAPIConfig)
	return args.Get(0).(*aviatdapiclient.SearchResultRsp), args.Error(1)
}

type backendClientMock struct {
	mock.Mock
}

func (c *backendClientMock) GeoLookup(ctx context.Context, nationalVersion, lang string, geoID int32) (*aviabackendclient.GeoLookupRsp, error) {
	args := c.Called(ctx, nationalVersion, lang, geoID)
	return args.Get(0).(*aviabackendclient.GeoLookupRsp), args.Error(1)
}

func (c *backendClientMock) TopFlights(ctx context.Context, nationalVersion, lang string, fromKey, toKey, date string, limit int) (*aviabackendclient.TopFlightsRsp, error) {
	args := c.Called(ctx, nationalVersion, lang, fromKey, toKey, date, limit)
	return args.Get(0).(*aviabackendclient.TopFlightsRsp), args.Error(1)
}

type suggestClientMock struct {
	mock.Mock
}

func (s *suggestClientMock) Suggest(
	ctx context.Context,
	nationalVersion string,
	lang string,
	field aviasuggestclient.FieldType,
	query string,
	otherQuery string,
	otherPoint string,
) (*aviasuggestclient.SuggestResponse, error) {
	args := s.Called(ctx, nationalVersion, lang, field, query, otherQuery, otherPoint)
	return args.Get(0).(*aviasuggestclient.SuggestResponse), args.Error(1)
}

type aviaConfigGetterMock struct {
	mock.Mock
}

func (c *aviaConfigGetterMock) GetAviaConfig(ctx context.Context) *exp3pb.GetAviaConfigRspData {
	args := c.Called(ctx)
	return args.Get(0).(*exp3pb.GetAviaConfigRspData)
}

var exp3aviaConfig = &exp3pb.GetAviaConfigRspData{
	TdApiConfig: exp3tdAPIConfig,
}

func TestInitSearch_Success(t *testing.T) {
	ctx := prepareLanguageContext(context.Background(), "ru-RU")
	tdapiMock := new(tdapiClientMock)
	forward := aviatdapiclient.Date{Time: time.Date(2022, 1, 20, 0, 0, 0, 0, time.UTC)}
	tdapiMock.On("InitSearch", ctx, "ru", "ru", uint32(1), uint32(0), uint32(0), forward, (*aviatdapiclient.Date)(nil), aviatdapiclient.EconomyServiceClass, "c213", "c2").Return(&aviatdapiclient.InitSearchRsp{
		Status: aviatdapiclient.SuccessResponseStatus,
		Data: aviatdapiclient.InitSearchData{
			QID: "220119-234926-681.travelapp.plane.c213_c2_2022-10-10_None_economy_1_0_0_ru.ru",
		},
	}, nil)

	aviaConfigMock := new(aviaConfigGetterMock)
	aviaConfigMock.On("GetAviaConfig", ctx).Return(exp3aviaConfig)

	handler := NewGRPCAviaHandler(
		&nop.Logger{},
		"",
		tdapiMock,
		nil,
		avia.DefaultConfig,
		nil,
		featureFlagsStorage,
		unifiedagent.NewDummyClient(),
		&geobase.StubGeobase{},
		nil,
		aviaConfigMock,
	)

	req := aviaAPI.InitSearchReq{
		ServiceClass: aviaAPI.ServiceClass_SERVICE_CLASS_ECONOMY,
		Passengers:   &aviaAPI.Passengers{Adults: 1},
		PointKeyFrom: "c213",
		PointKeyTo:   "c2",
		DateForward:  &date.Date{Year: 2022, Month: 1, Day: 20},
	}

	rsp, err := handler.InitSearch(ctx, &req)

	require.NoError(t, err)
	require.Equal(t, "220119-234926-681.travelapp.plane.c213_c2_2022-10-10_None_economy_1_0_0_ru.ru", rsp.Qid)
	tdapiMock.AssertExpectations(t)
}

func TestInitSearch_BadRequest400(t *testing.T) {
	ctx := prepareLanguageContext(context.Background(), "ru-RU")

	tdapiMock := new(tdapiClientMock)
	forward := aviatdapiclient.Date{Time: time.Date(2022, 1, 20, 0, 0, 0, 0, time.UTC)}
	tdapiMock.On("InitSearch", ctx, "ru", "ru", uint32(1), uint32(0), uint32(0), forward, (*aviatdapiclient.Date)(nil), aviatdapiclient.EconomyServiceClass, "c213", "c2").Return(
		(*aviatdapiclient.InitSearchRsp)(nil),
		clientscommon.StatusError{
			Status: http.StatusBadRequest,
		},
	)

	aviaConfigMock := new(aviaConfigGetterMock)
	aviaConfigMock.
		On("GetAviaConfig", ctx).
		Return(exp3aviaConfig)
	handler := NewGRPCAviaHandler(
		&nop.Logger{},
		"",
		tdapiMock,
		nil,
		avia.DefaultConfig,
		nil,
		featureFlagsStorage,
		unifiedagent.NewDummyClient(),
		&geobase.StubGeobase{},
		nil,
		aviaConfigMock,
	)
	req := aviaAPI.InitSearchReq{
		ServiceClass: aviaAPI.ServiceClass_SERVICE_CLASS_ECONOMY,
		Passengers:   &aviaAPI.Passengers{Adults: 1},
		PointKeyFrom: "c213",
		PointKeyTo:   "c2",
		DateForward:  &date.Date{Year: 2022, Month: 1, Day: 20},
	}

	_, err := handler.InitSearch(ctx, &req)

	var res interface{ GRPCStatus() *status.Status }
	if assert.ErrorAs(t, err, &res) {
		assert.Equal(t, codes.InvalidArgument, res.GRPCStatus().Code())
	}
	tdapiMock.AssertExpectations(t)
}

func TestInitSearch_Timeout(t *testing.T) {
	ctx := prepareLanguageContext(context.Background(), "ru-RU")

	tdapiMock := new(tdapiClientMock)
	forward := aviatdapiclient.Date{Time: time.Date(2022, 1, 20, 0, 0, 0, 0, time.UTC)}
	tdapiMock.On("InitSearch", ctx, "ru", "ru", uint32(1), uint32(0), uint32(0), forward, (*aviatdapiclient.Date)(nil), aviatdapiclient.EconomyServiceClass, "c213", "c2").Return(
		(*aviatdapiclient.InitSearchRsp)(nil),
		clientscommon.ResponseError.Wrap((net.Error)(&TimeoutError{})),
	)

	aviaConfigMock := new(aviaConfigGetterMock)
	aviaConfigMock.
		On("GetAviaConfig", ctx).
		Return(exp3aviaConfig)
	handler := NewGRPCAviaHandler(
		&nop.Logger{},
		"",
		tdapiMock,
		nil,
		avia.DefaultConfig,
		nil,
		featureFlagsStorage,
		unifiedagent.NewDummyClient(),
		&geobase.StubGeobase{},
		nil,
		aviaConfigMock,
	)
	req := aviaAPI.InitSearchReq{
		ServiceClass: aviaAPI.ServiceClass_SERVICE_CLASS_ECONOMY,
		Passengers:   &aviaAPI.Passengers{Adults: 1},
		PointKeyFrom: "c213",
		PointKeyTo:   "c2",
		DateForward:  &date.Date{Year: 2022, Month: 1, Day: 20},
	}

	_, err := handler.InitSearch(ctx, &req)

	var res interface{ GRPCStatus() *status.Status }
	if assert.ErrorAs(t, err, &res) {
		assert.Equal(t, codes.DeadlineExceeded, res.GRPCStatus().Code())
	}
	tdapiMock.AssertExpectations(t)
}

func TestSuggest_Success(t *testing.T) {
	ctx := prepareLanguageContext(context.Background(), "ru-RU")
	suggestMock := new(suggestClientMock)
	suggestMock.On("Suggest", ctx, "ru", "ru", aviasuggestclient.FieldType(aviasuggestclient.FieldTo), "москв", "Екатеринбург", "с54").Return(
		&aviasuggestclient.SuggestResponse{
			Query: "москв",
			Suggests: []aviasuggestclient.Suggest{
				{
					Level:                0,
					Title:                "Москва",
					PointKey:             "c213",
					PointCode:            "MOW",
					RegionTitle:          "Москва и Московская область",
					CityTitle:            "Москва",
					CountryTitle:         "Россия",
					Missprint:            0,
					Hidden:               aviasuggestclient.NewBool(false),
					HaveAirport:          aviasuggestclient.NewBool(true),
					HaveNotHiddenAirport: aviasuggestclient.NewBool(true),
					Nested:               nil,
				},
			},
		},
		nil,
	)

	aviaConfigMock := new(aviaConfigGetterMock)
	aviaConfigMock.On("GetAviaConfig", ctx).Return(exp3aviaConfig)

	handler := NewGRPCAviaHandler(
		&nop.Logger{},
		"",
		nil,
		suggestMock,
		avia.DefaultConfig,
		nil,
		featureFlagsStorage,
		unifiedagent.NewDummyClient(),
		&geobase.StubGeobase{},
		nil,
		aviaConfigMock,
	)
	req := aviaAPI.SuggestReq{
		ClientCityGeoid: 54,
		FieldType:       aviaAPI.SuggestFieldType_SUGGEST_FIELD_TYPE_TO,
		Query:           "москв",
		OtherQuery:      "Екатеринбург",
		OtherPointKey:   "с54",
	}

	actual, err := handler.Suggest(ctx, &req)

	require.NoError(t, err)
	require.Equal(t, "москв", actual.Query)
	require.Len(t, actual.Suggests, 1)
	require.Equal(t, &aviaAPI.Suggest{
		Level:          0,
		Title:          "Москва",
		PointKey:       "c213",
		PointType:      commonAPI.GeoPointType_GEO_POINT_TYPE_SETTLEMENT,
		PointAviaCode:  "MOW",
		RegionTitle:    "Москва и Московская область",
		CityTitle:      "Москва",
		CountryTitle:   "Россия",
		Misprint:       0,
		HaveAirport:    true,
		NestedSuggests: []*aviaAPI.Suggest{},
	}, actual.Suggests[0])
	suggestMock.AssertExpectations(t)
}

var blagoveschinskID uint64 = 77
var zeaID uint64 = 11379
var tdAPISuccessResult = &aviatdapiclient.SearchResultRsp{
	Status: "success",
	Data: aviatdapiclient.SearchResultData{
		Variants: aviatdapiclient.SearchResultVariants{
			Fares: []aviatdapiclient.SearchResultFare{
				{
					Prices: []aviatdapiclient.SearchResultPrice{
						{
							FareFamiliesHash: "f827cf462f62848df37c5e1e94a4da74",
							Charter:          false,
							FareCodes:        [][][]string{{{"Y"}, {}}},
							Baggage:          [][]string{{"1p1p23d"}, {}},
							Selfconnect:      false,
							PartnerCode:      "superkassa",
							Tariff: aviatdapiclient.Price{
								Currency: "RUR",
								Value:    2897,
							},
							TariffNational: aviatdapiclient.Price{
								Currency: "RUR",
								Value:    2897,
							},
							FareFamilies: nil,
							Boy:          true,
						},
						{
							FareFamiliesHash: "f827cf462f62848df37c5e1e94a4da74",
							Charter:          false,
							FareCodes:        [][][]string{{{"Y"}, {}}},
							Baggage:          [][]string{{"1p1p23d"}, {}},
							Selfconnect:      false,
							PartnerCode:      "biletdv",
							Tariff: aviatdapiclient.Price{
								Currency: "RUR",
								Value:    3140,
							},
							TariffNational: aviatdapiclient.Price{
								Currency: "RUR",
								Value:    3140,
							},
							FareFamilies: nil,
						},
						{
							FareFamiliesHash: "f827cf462f62848df37c5e1e94a4da74",
							Charter:          false,
							FareCodes:        [][][]string{{{"Y"}, {}}},
							Baggage:          [][]string{{"1p1p23d"}, {}},
							Selfconnect:      false,
							PartnerCode:      "ozon",
							Tariff: aviatdapiclient.Price{
								Currency: "RUR",
								Value:    2925,
							},
							TariffNational: aviatdapiclient.Price{
								Currency: "RUR",
								Value:    2925,
							},
							FareFamilies: nil,
						},
					},
					Route: [][]string{{"2204260910HZ26822204261020"}, {}},
				},
				{
					Prices: []aviatdapiclient.SearchResultPrice{
						{
							FareFamiliesHash: "f827cf462f62848df37c5e1e94a4da74",
							Charter:          false,
							FareCodes:        [][][]string{{{"Y"}, {}}},
							Baggage:          [][]string{{"1p1p23d"}, {}},
							Selfconnect:      false,
							PartnerCode:      "citytravel1",
							Tariff: aviatdapiclient.Price{
								Currency: "RUR",
								Value:    3329,
							},
							TariffNational: aviatdapiclient.Price{
								Currency: "RUR",
								Value:    3329,
							},
							FareFamilies: nil,
						},
					},
					Route: [][]string{{"2204261910HZ55822204262020"}, {}},
				},
			},
		},
		Reference: aviatdapiclient.SearchResultReference{
			Partners: []aviatdapiclient.Partner{
				{
					ID:      133,
					Code:    "citytravel1",
					Title:   "City.Travel",
					LogoSVG: "https://example.net/133.svg",
					LogoPNG: "https://example.net/133.png",
				},
				{
					ID:      83,
					Code:    "superkassa",
					Title:   "Superkassa",
					LogoSVG: "https://example.net/83.svg",
					LogoPNG: "https://example.net/83.png",
				},
				{
					ID:      1,
					Code:    "ozon",
					Title:   "OZON",
					LogoSVG: "https://example.net/1.svg",
					LogoPNG: "https://example.net/1.png",
				},
				{
					ID:      19,
					Code:    "biletdv",
					Title:   "БилетДВ",
					LogoSVG: "https://example.net/19.svg",
					LogoPNG: "https://example.net/19.png",
				},
			},
			FareFamilies: nil,
			Settlements: []aviatdapiclient.Settlement{
				{
					ID:              zeaID,
					AviaCode:        "ЗЕЯ",
					Title:           "Зея",
					TitleGenitive:   "Зеи",
					TitleLocative:   "Зеи",
					TitleAccusative: "Зею",
					Preposition:     "в",
				},
				{
					ID:              blagoveschinskID,
					AviaCode:        "BQS",
					Title:           "Благовещенск",
					TitleGenitive:   "Благовещенска",
					TitleLocative:   "Благовещенске",
					TitleAccusative: "Благовещенск",
					Preposition:     "в",
				},
			},
			Alliances: nil,
			CompanyTariffs: []aviatdapiclient.CompanyTariff{
				{
					ID:             87,
					Published:      true,
					CarryOn:        true,
					CarryOnNorm:    5,
					BaggageAllowed: false,
					BaggageNorm:    23,
				},
			},
			AviaCompanies: []aviatdapiclient.AviaCompany{
				{
					ID:                   50,
					CarryOnLength:        55,
					CarryOnWidth:         40,
					CarryOnHeight:        20,
					CostType:             "hybrid",
					CarryOnSizeBucket:    "regular",
					BaggageRules:         "Какие-то правила.",
					BaggageDimensionsSum: 158,
				},
			},
			Stations: []aviatdapiclient.Station{
				{
					ID:              9635355,
					SettlementID:    &zeaID,
					AviaCode:        "ЗЕЯ",
					Title:           "Зея",
					TransportType:   "plane",
					TitleGenitive:   "Зеи",
					TitleLocative:   "Зеи",
					TitleAccusative: "Зею",
					Preposition:     "в",
				},
				{
					ID:              9623271,
					SettlementID:    &blagoveschinskID,
					AviaCode:        "BQS",
					Title:           "Благовещенск",
					TransportType:   "plane",
					TitleGenitive:   "Благовещенска",
					TitleLocative:   "Благовещенске",
					TitleAccusative: "Благовещенск",
					Preposition:     "в",
				},
			},
			Companies: []aviatdapiclient.Company{
				{
					ID:         50,
					AllianceID: nil,
					Title:      "Аврора",
					LogoSVG:    "https://example.com/50.svg",
					LogoPNG:    "https://example.com/50.png",
					Color:      "#00457c",
				},
			},
			Flights: []aviatdapiclient.Flight{
				{
					Key:    "2204260910HZ26822204261020",
					Number: "HZ 2682",
					Departure: aviatdapiclient.DateTime{
						Local:    "2022-04-26T09:10:00",
						Timezone: "Asia/Yakutsk",
						Offset:   540,
					},
					Arrival: aviatdapiclient.DateTime{
						Local:    "2022-04-26T10:20:00",
						Timezone: "Asia/Yakutsk",
						Offset:   540,
					},
					CompanyID:            50,
					OperatingAviaCompony: aviatdapiclient.OperatingAviaCompany{},
					CompanyTariffID:      87,
					AviaCompanyID:        nil,
					StationFromID:        9623271,
					StationToID:          9635355,
				},
				{
					Key:    "2204261910HZ55822204262020",
					Number: "HZ 5582",
					Departure: aviatdapiclient.DateTime{
						Local:    "2022-04-26T19:10:00",
						Timezone: "Asia/Yakutsk",
						Offset:   540,
					},
					Arrival: aviatdapiclient.DateTime{
						Local:    "2022-04-26T20:20:00",
						Timezone: "Asia/Yakutsk",
						Offset:   540,
					},
					CompanyID:            50,
					OperatingAviaCompony: aviatdapiclient.OperatingAviaCompany{},
					CompanyTariffID:      87,
					AviaCompanyID:        ptr.Uint64(50),
					StationFromID:        9623271,
					StationToID:          9635355,
				},
			},
			BaggageTariffs: map[string]aviatdapiclient.BaggageTariff{
				"1p1p23d": {
					Included: aviatdapiclient.BaggageTariffDetails{
						Count:  1,
						Source: "partner",
					},
					Pieces: aviatdapiclient.BaggageTariffDetails{
						Count:  1,
						Source: "partner",
					},
					Weight: aviatdapiclient.BaggageTariffDetails{
						Count:  23,
						Source: "db",
					},
				},
			},
		},
		Progress: aviatdapiclient.SearchResultProgress{
			Current: 28,
			Total:   28,
		},
	},
}
var filtersSuccessResult = &aviaAPI.SearchFiltersRsp{
	QuickBaggage: &aviaAPI.SearchFiltersRsp_QuickBaggageFilter{
		State: &aviaAPI.SearchFiltersRsp_BoolFilterState{
			Enabled: false,
			Value:   false,
		},
		MinPriceNoBaggage: &commonAPI.Price{
			Currency: "RUB",
			Value:    2897,
		},
		MinPriceWithBaggage: nil,
	},
	QuickTransfer: &aviaAPI.SearchFiltersRsp_QuickTransferFilter{
		State: &aviaAPI.SearchFiltersRsp_BoolFilterState{
			Enabled: false,
			Value:   true,
		},
		MinPriceNoTransfer: &commonAPI.Price{
			Currency: "RUB",
			Value:    2897,
		},
		MinPriceWithTransfer: nil,
	},
	QuickRefund: nil,
	Transfer: &aviaAPI.SearchFiltersRsp_TransferFilter{
		NoTransfer: &aviaAPI.SearchFiltersRsp_BoolFilterState{
			Enabled: false,
			Value:   true,
		},
		OneTransferOrLess: &aviaAPI.SearchFiltersRsp_BoolFilterState{
			Enabled: false,
			Value:   true,
		},
		NoNightTransfer: &aviaAPI.SearchFiltersRsp_BoolFilterState{
			Enabled: true,
			Value:   false,
		},
		NoAirportChange: &aviaAPI.SearchFiltersRsp_BoolFilterState{
			Enabled: true,
			Value:   false,
		},
		TransferDuration: nil,
		Airports:         nil,
	},
	Airport: &aviaAPI.SearchFiltersRsp_AirportFilter{
		Forward: &aviaAPI.SearchFiltersRsp_DirectionAirports{
			Departure: &aviaAPI.SearchFiltersRsp_DepartureOrArrivalAirports{
				SettlementId: 77,
				Airports: []*aviaAPI.SearchFiltersRsp_Airport{
					{
						State: &aviaAPI.SearchFiltersRsp_BoolFilterState{
							Enabled: true,
							Value:   false,
						},
						StationId:       9623271,
						StationTitle:    "Благовещенск",
						SettlementTitle: "",
						AviaCode:        "BQS",
					},
				},
				Title: "Вылет из Благовещенска",
			},
			Arrival: &aviaAPI.SearchFiltersRsp_DepartureOrArrivalAirports{
				SettlementId: 11379,
				Airports: []*aviaAPI.SearchFiltersRsp_Airport{
					{
						State: &aviaAPI.SearchFiltersRsp_BoolFilterState{
							Enabled: true,
							Value:   false,
						},
						StationId:       9635355,
						StationTitle:    "Зея",
						SettlementTitle: "",
						AviaCode:        "ЗЕЯ",
					},
				},
				Title: "Прилёт в Зею",
			},
		},
		Backward: nil,
	},
	Aviacompany: &aviaAPI.SearchFiltersRsp_AviacompanyFilter{
		AviacompanyCombinations: &aviaAPI.SearchFiltersRsp_BoolFilterState{
			Enabled: true,
			Value:   false,
		},
		Aviacompanies: []*aviaAPI.SearchFiltersRsp_AviacompanyState{
			{
				AviacompanyId: 50,
				State: &aviaAPI.SearchFiltersRsp_BoolFilterState{
					Enabled: true,
					Value:   false,
				},
			},
		},
	},
	DepartureAndArrival: &aviaAPI.SearchFiltersRsp_DepartureAndArrivalFilter{
		ForwardDeparture: &aviaAPI.SearchFiltersRsp_DepartureOrArrivalState{
			SettlementId: blagoveschinskID,
			StationId:    0,
			All: &aviaAPI.SearchFiltersRsp_DepartureOrArrivalInterval{
				MinDatetime: "2022-04-26T09:10:00+09:00",
				MaxDatetime: "2022-04-26T19:10:00+09:00",
			},
			Selected: &aviaAPI.SearchFiltersRsp_DepartureOrArrivalInterval{
				MinDatetime: "2022-04-26T09:10:00+09:00",
				MaxDatetime: "2022-04-26T19:10:00+09:00",
			},
			Title: "Вылет из Благовещенска",
		},
		ForwardArrival: &aviaAPI.SearchFiltersRsp_DepartureOrArrivalState{
			SettlementId: zeaID,
			StationId:    0,
			All: &aviaAPI.SearchFiltersRsp_DepartureOrArrivalInterval{
				MinDatetime: "2022-04-26T10:20:00+09:00",
				MaxDatetime: "2022-04-26T20:20:00+09:00",
			},
			Selected: &aviaAPI.SearchFiltersRsp_DepartureOrArrivalInterval{
				MinDatetime: "2022-04-26T10:20:00+09:00",
				MaxDatetime: "2022-04-26T20:20:00+09:00",
			},
			Title: "Прилёт в Зею",
		},
		BackwardDeparture: nil,
		BackwardArrival:   nil,
	},
}

var referenceResult = &aviaAPI.SearchResultReference{
	Flights: map[string]*aviaAPI.SearchResultReference_Flight{
		"2204260910HZ26822204261020": {
			Key:             "2204260910HZ26822204261020",
			AviaCompanyId:   50,
			Number:          "HZ 2682",
			StationFromId:   9623271,
			StationToId:     9635355,
			Departure:       "2022-04-26T09:10:00+09:00",
			Arrival:         "2022-04-26T10:20:00+09:00",
			DurationMinutes: 70,
			DateChanged:     false,
		},
		"2204261910HZ55822204262020": {
			Key:             "2204261910HZ55822204262020",
			AviaCompanyId:   50,
			Number:          "HZ 5582",
			StationFromId:   9623271,
			StationToId:     9635355,
			Departure:       "2022-04-26T19:10:00+09:00",
			Arrival:         "2022-04-26T20:20:00+09:00",
			DurationMinutes: 70,
			DateChanged:     false,
		},
	},
	Partners: map[string]*aviaAPI.SearchResultReference_Partner{
		"biletdv": {
			Code:    "biletdv",
			Title:   "БилетДВ",
			LogoSvg: "https://example.net/19.svg",
			LogoPng: "https://example.net/19.png",
		},
		"citytravel1": {
			Code:    "citytravel1",
			Title:   "City.Travel",
			LogoSvg: "https://example.net/133.svg",
			LogoPng: "https://example.net/133.png",
		},
		"ozon": {
			Code:    "ozon",
			Title:   "OZON",
			LogoSvg: "https://example.net/1.svg",
			LogoPng: "https://example.net/1.png",
		},
		"superkassa": {
			Code:    "superkassa",
			Title:   "Superkassa",
			LogoSvg: "https://example.net/83.svg",
			LogoPng: "https://example.net/83.png",
		},
	},
	Settlements: map[uint64]*aviaAPI.SearchResultReference_Settlement{
		blagoveschinskID: {
			Id:               blagoveschinskID,
			Title:            "Благовещенск",
			TitleGenitive:    "Благовещенска",
			TitleLocative:    "Благовещенске",
			TitleAccusative:  "Благовещенск",
			TitlePreposition: "в",
		},
		zeaID: {
			Id:               zeaID,
			Title:            "Зея",
			TitleGenitive:    "Зеи",
			TitleLocative:    "Зеи",
			TitleAccusative:  "Зею",
			TitlePreposition: "в",
		},
	},
	Stations: map[uint64]*aviaAPI.SearchResultReference_Station{
		9623271: {
			Id:               9623271,
			AviaCode:         "BQS",
			SettlementId:     blagoveschinskID,
			Title:            "Благовещенск",
			TitleGenitive:    "Благовещенска",
			TitleLocative:    "Благовещенске",
			TitleAccusative:  "Благовещенск",
			TitlePreposition: "в",
		},
		9635355: {
			Id:               9635355,
			AviaCode:         "ЗЕЯ",
			SettlementId:     zeaID,
			Title:            "Зея",
			TitleGenitive:    "Зеи",
			TitleLocative:    "Зеи",
			TitleAccusative:  "Зею",
			TitlePreposition: "в",
		},
	},
	AviaCompanies: map[uint64]*aviaAPI.SearchResultReference_AviaCompany{
		50: {
			Id:      50,
			Title:   "Аврора",
			LogoSvg: "https://example.com/50.svg",
			LogoPng: "https://example.com/50.png",
			Color:   "#00457c",
		},
	},
	Alliances: map[uint64]*aviaAPI.SearchResultReference_Alliance{},
}

var qidExample = "220421-145911-315.travelapp.plane.c77_c11379_2022-04-26_None_economy_1_0_0_ru.ru"

var searchResultSuccess = &aviaAPI.SearchResultRsp{
	Qid:       qidExample,
	Reference: referenceResult,
	Snippets: []*aviaAPI.Snippet{
		{
			Key: "02316b368f95919c17048b9a1a9d9a90d6743ee7",
			Forward: []string{
				"2204260910HZ26822204261020",
			},
			Backward: []string{},
			Variant: &aviaAPI.Snippet_Variant{
				Key:         "0",
				PartnerCode: "superkassa",
				Price: &commonAPI.Price{
					Currency: "RUB",
					Value:    2897,
				},
				Baggage: &aviaAPI.Snippet_Baggage{
					Included:       false,
					OptionalWeight: nil,
					OptionalPieces: nil,
				},
				CarryOn: &aviaAPI.Snippet_CarryOn{
					Included:       false,
					OptionalWeight: nil,
				},
				Refund: &aviaAPI.Snippet_Refund{
					Availability: 1,
					Price:        nil,
				},
				OrderRelativeUrl: "/avia/order/?adult_seats=1&children_seats=0&forward=HZ+2682.2022-04-26T09%3A10&fromId=c77&infant_seats=0&klass=economy&oneway=1&toId=c11379&when=2022-04-26",
			},
			Badges: []*aviaAPI.Snippet_Badge{
				{
					Type: aviaAPI.Snippet_BADGE_TYPE_BEST_PRICE,
				},
				{
					Type: aviaAPI.Snippet_BADGE_TYPE_BOOK_ON_YANDEX,
				},
			},
			Transfers: &aviaAPI.Snippet_Transfers{
				ForwardTransfers:  []*aviaAPI.Snippet_Transfer{},
				BackwardTransfers: []*aviaAPI.Snippet_Transfer{},
			},
			ForwardDurationMinutes:  70,
			BackwardDurationMinutes: 0,
		},
		{
			Key: "8e91127581f17240bede46b241c0fe04f1bdcf88",
			Forward: []string{
				"2204261910HZ55822204262020",
			},
			Backward: []string{},
			Variant: &aviaAPI.Snippet_Variant{
				Key:         "0",
				PartnerCode: "citytravel1",
				Price: &commonAPI.Price{
					Currency: "RUB",
					Value:    3329,
				},
				Baggage: &aviaAPI.Snippet_Baggage{
					Included:       false,
					OptionalWeight: nil,
					OptionalPieces: nil,
				},
				CarryOn: &aviaAPI.Snippet_CarryOn{
					Included:       false,
					OptionalWeight: nil,
				},
				Refund: &aviaAPI.Snippet_Refund{
					Availability: 1,
					Price:        nil,
				},
				OrderRelativeUrl: "/avia/order/?adult_seats=1&children_seats=0&forward=HZ+5582.2022-04-26T19%3A10&fromId=c77&infant_seats=0&klass=economy&oneway=1&toId=c11379&when=2022-04-26",
			},
			Badges: []*aviaAPI.Snippet_Badge{},
			Transfers: &aviaAPI.Snippet_Transfers{
				ForwardTransfers:  []*aviaAPI.Snippet_Transfer{},
				BackwardTransfers: []*aviaAPI.Snippet_Transfer{},
			},
			ForwardDurationMinutes:  70,
			BackwardDurationMinutes: 0,
		},
	},
	Progress: &aviaAPI.SearchProgress{
		Current: 28,
		Total:   28,
	},
	SnippetsCount: 2,
	Sort:          aviaAPI.SearchSort_SEARCH_SORT_RECOMMENDED_FIRST,
	Filters:       filtersSuccessResult,
}

func TestSearchResult_Success(t *testing.T) {
	ctx := prepareLanguageContext(context.Background(), "ru-RU")
	updatedCtx := ctxlog.WithFields(ctx, log.String("qid", qidExample))
	tdapiMock := new(tdapiClientMock)
	tdapiMock.On("SearchResult", updatedCtx, qidExample, exp3tdAPIConfig).Return(tdAPISuccessResult, nil)

	gatewayMock := new(backendClientMock)
	gatewayMock.On("GeoLookup", updatedCtx, "ru", "ru", 54).Return(nil, nil)
	gatewayMock.On("TopFlights", updatedCtx, "ru", "ru", "c77", "c11379", "2022-04-26", 100).Return(&aviabackendclient.TopFlightsRsp{
		Status: "topFlights",
		Data:   [][]aviabackendclient.TopFlightElem{{}},
	}, nil)

	aviaConfigMock := new(aviaConfigGetterMock)
	aviaConfigMock.On("GetAviaConfig", updatedCtx).Return(exp3aviaConfig)

	handler := NewGRPCAviaHandler(
		&nop.Logger{},
		"",
		tdapiMock,
		nil,
		avia.DefaultConfig,
		gatewayMock,
		featureFlagsStorage,
		unifiedagent.NewDummyClient(),
		&geobase.StubGeobase{},
		nil,
		aviaConfigMock,
	)
	handler.cache.RedisClient = nil

	actual, err := handler.SearchResult(ctx, &aviaAPI.SearchResultReq{
		Qid: qidExample,
	})

	require.NoError(t, err)
	searchResultSuccess.ExpiresAt = actual.ExpiresAt
	assert.Equal(t, searchResultSuccess, actual)
	tdapiMock.AssertExpectations(t)
}

func TestSearchResult_UnknownCompany(t *testing.T) {
	ctx := prepareLanguageContext(context.Background(), "ru-RU")
	updatedCtx := ctxlog.WithFields(ctx, log.String("qid", qidExample))
	tdapiMock := new(tdapiClientMock)
	for key := range tdAPISuccessResult.Data.Reference.Flights {
		tdAPISuccessResult.Data.Reference.Flights[key].AviaCompanyID = nil
	}
	tdapiMock.On("SearchResult", updatedCtx, qidExample, exp3tdAPIConfig).Return(tdAPISuccessResult, nil)

	gatewayMock := new(backendClientMock)
	gatewayMock.On("GeoLookup", updatedCtx, "ru", "ru", 54).Return(nil, nil)
	gatewayMock.On("TopFlights", updatedCtx, "ru", "ru", "c77", "c11379", "2022-04-26", 100).Return(&aviabackendclient.TopFlightsRsp{
		Status: "topFlights",
		Data:   [][]aviabackendclient.TopFlightElem{{}},
	}, nil)

	aviaConfigMock := new(aviaConfigGetterMock)
	aviaConfigMock.On("GetAviaConfig", updatedCtx).Return(exp3aviaConfig)

	handler := NewGRPCAviaHandler(
		&nop.Logger{},
		"",
		tdapiMock,
		nil,
		avia.DefaultConfig,
		gatewayMock,
		featureFlagsStorage,
		unifiedagent.NewDummyClient(),
		&geobase.StubGeobase{},
		nil,
		aviaConfigMock,
	)
	handler.cache.RedisClient = nil

	actual, err := handler.SearchResult(ctx, &aviaAPI.SearchResultReq{
		Qid: qidExample,
	})

	require.NoError(t, err)
	searchResultSuccess.ExpiresAt = actual.ExpiresAt
	assert.Equal(t, searchResultSuccess, actual)
	tdapiMock.AssertExpectations(t)
}

func TestUpdateFilters(t *testing.T) {
	qid := "220421-145911-315.travelapp.plane.c77_c11379_2022-04-26_None_economy_1_0_0_ru.ru"
	ctx := prepareLanguageContext(context.Background(), "ru-RU")
	updatedCtx := ctxlog.WithFields(ctx, log.String("qid", qid))
	tdapiMock := new(tdapiClientMock)
	tdapiMock.On("SearchResult", updatedCtx, qid, exp3tdAPIConfig).Return(tdAPISuccessResult, nil)

	gatewayMock := new(backendClientMock)
	gatewayMock.On("GeoLookup", updatedCtx, "ru", "ru", 54).Return(nil, nil)
	gatewayMock.On("TopFlights", updatedCtx, "ru", "ru", "c77", "c11379", "2022-04-26", 100).Return(&aviabackendclient.TopFlightsRsp{
		Status: "topFlights",
		Data:   [][]aviabackendclient.TopFlightElem{{}},
	}, nil)

	aviaConfigMock := new(aviaConfigGetterMock)
	aviaConfigMock.On("GetAviaConfig", updatedCtx).Return(exp3aviaConfig)

	handler := NewGRPCAviaHandler(
		&nop.Logger{},
		"",
		tdapiMock,
		nil,
		avia.DefaultConfig,
		gatewayMock,
		featureFlagsStorage,
		unifiedagent.NewDummyClient(),
		&geobase.StubGeobase{},
		nil,
		aviaConfigMock,
	)
	handler.cache.RedisClient = nil

	actual, err := handler.UpdateFilters(ctx, &aviaAPI.UpdateFiltersReq{
		Qid: qid,
	})

	require.NoError(t, err)
	assert.Equal(t, &aviaAPI.UpdateFiltersRsp{
		Filters:   filtersSuccessResult,
		Reference: referenceResult,
	}, actual)
	tdapiMock.AssertExpectations(t)
}

func TestSearchResult_Timeout(t *testing.T) {
	qid := "220421-145911-315.travelapp.plane.c77_c11379_2022-04-26_None_economy_1_0_0_ru.ru"
	ctx := prepareLanguageContext(context.Background(), "ru-RU")
	updatedCtx := ctxlog.WithFields(ctx, log.String("qid", qid))

	tdapiMock := new(tdapiClientMock)
	tdapiMock.On("SearchResult", updatedCtx, qid, exp3tdAPIConfig).Return((*aviatdapiclient.SearchResultRsp)(nil), TimeoutError{})

	aviaConfigMock := new(aviaConfigGetterMock)
	aviaConfigMock.
		On("GetAviaConfig", updatedCtx).
		Return(&exp3pb.GetAviaConfigRspData{TdApiConfig: &exp3pb.TdApiConfig{InstantSearchEnabled: true}})

	handler := NewGRPCAviaHandler(
		&nop.Logger{},
		"",
		tdapiMock,
		nil,
		avia.DefaultConfig,
		nil,
		featureFlagsStorage,
		unifiedagent.NewDummyClient(),
		&geobase.StubGeobase{},
		nil,
		aviaConfigMock,
	)
	handler.cache.RedisClient = nil

	actual, err := handler.SearchResult(ctx, &aviaAPI.SearchResultReq{
		Qid: qid,
	})

	require.NoError(t, err)
	assert.Empty(t, actual.Snippets)
	assert.Equal(t, &aviaAPI.SearchResultRsp{
		Qid: qid,
		Reference: &aviaAPI.SearchResultReference{
			Flights:       map[string]*aviaAPI.SearchResultReference_Flight{},
			Partners:      map[string]*aviaAPI.SearchResultReference_Partner{},
			Settlements:   map[uint64]*aviaAPI.SearchResultReference_Settlement{},
			Stations:      map[uint64]*aviaAPI.SearchResultReference_Station{},
			AviaCompanies: map[uint64]*aviaAPI.SearchResultReference_AviaCompany{},
			Alliances:     map[uint64]*aviaAPI.SearchResultReference_Alliance{},
		},
		Snippets: []*aviaAPI.Snippet{},
		Progress: &aviaAPI.SearchProgress{
			Current: 0,
			Total:   100,
		},
		ExpiresAt:     actual.ExpiresAt,
		SnippetsCount: 0,
		Sort:          aviaAPI.SearchSort_SEARCH_SORT_RECOMMENDED_FIRST,
		Filters: &aviaAPI.SearchFiltersRsp{
			QuickBaggage:  &aviaAPI.SearchFiltersRsp_QuickBaggageFilter{State: &aviaAPI.SearchFiltersRsp_BoolFilterState{}},
			QuickTransfer: &aviaAPI.SearchFiltersRsp_QuickTransferFilter{State: &aviaAPI.SearchFiltersRsp_BoolFilterState{}},
			QuickRefund:   nil,
			Transfer: &aviaAPI.SearchFiltersRsp_TransferFilter{
				NoTransfer:        &aviaAPI.SearchFiltersRsp_BoolFilterState{},
				OneTransferOrLess: &aviaAPI.SearchFiltersRsp_BoolFilterState{},
				NoNightTransfer:   &aviaAPI.SearchFiltersRsp_BoolFilterState{Enabled: true},
				NoAirportChange:   &aviaAPI.SearchFiltersRsp_BoolFilterState{Enabled: true},
			},
			Airport: &aviaAPI.SearchFiltersRsp_AirportFilter{
				Forward: &aviaAPI.SearchFiltersRsp_DirectionAirports{
					Departure: &aviaAPI.SearchFiltersRsp_DepartureOrArrivalAirports{
						SettlementId: 77,
						Airports:     []*aviaAPI.SearchFiltersRsp_Airport{},
						Title:        "Вылет",
					},
					Arrival: &aviaAPI.SearchFiltersRsp_DepartureOrArrivalAirports{
						SettlementId: 11379,
						Airports:     []*aviaAPI.SearchFiltersRsp_Airport{},
						Title:        "Прилёт",
					},
				},
				Backward: nil,
			},
			Aviacompany: &aviaAPI.SearchFiltersRsp_AviacompanyFilter{
				AviacompanyCombinations: &aviaAPI.SearchFiltersRsp_BoolFilterState{Enabled: true},
				Aviacompanies:           []*aviaAPI.SearchFiltersRsp_AviacompanyState{},
			},
			DepartureAndArrival: &aviaAPI.SearchFiltersRsp_DepartureAndArrivalFilter{
				ForwardDeparture: &aviaAPI.SearchFiltersRsp_DepartureOrArrivalState{
					SettlementId: 77,
					StationId:    0,
					All:          nil,
					Selected:     nil,
					Title:        "Вылет",
				},
				ForwardArrival: &aviaAPI.SearchFiltersRsp_DepartureOrArrivalState{
					SettlementId: 11379,
					StationId:    0,
					All:          nil,
					Selected:     nil,
					Title:        "Прилёт",
				},
			},
		},
	}, actual)

	tdapiMock.AssertExpectations(t)
}

func prepareLanguageContext(ctx context.Context, acceptLanguageValue string) context.Context {
	md := metadata.MD{
		"grpcgateway-accept-language": []string{acceptLanguageValue},
	}
	return metadata.NewIncomingContext(ctx, md)
}
