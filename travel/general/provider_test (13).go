package direction

import (
	"context"
	"encoding/json"
	"strconv"
	"testing"
	"time"

	"github.com/golang/protobuf/ptypes"
	pts "github.com/golang/protobuf/ptypes/timestamp"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/core/log/zap"
	"a.yandex-team.ru/library/go/ptr"
	"a.yandex-team.ru/library/go/units"
	travel "a.yandex-team.ru/travel/proto"
	"a.yandex-team.ru/travel/proto/dicts/rasp"
	api "a.yandex-team.ru/travel/trains/search_api/api/tariffs"
	"a.yandex-team.ru/travel/trains/search_api/internal/direction/filters"
	"a.yandex-team.ru/travel/trains/search_api/internal/direction/models"
	"a.yandex-team.ru/travel/trains/search_api/internal/direction/query"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/clock"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/consts"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/date/runmask"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/dict/factories"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/dict/registry"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/express"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/geo"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/helpers"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/i18n"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/lang"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/points"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/regioncapital"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/schedule"
	schedulefactories "a.yandex-team.ru/travel/trains/search_api/internal/pkg/schedule/factories"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/tariffs"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/testutils"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/traincity"
)

func SkipTestGetDirections(t *testing.T) {
	clock.FreezeAt(time.Date(2021, 3, 16, 12, 0, 0, 0, time.UTC))
	defer clock.SetReal()

	testInst := buildTestProvider(t)
	departureDate := shortDate(2021, 3, 16)

	russia := &rasp.TCountry{Id: consts.RussiaID}
	departure := factories.NewSettlementFactory(testInst.repoRegistry).
		WithSlug("departure").
		WithTitle("departureTitle").
		WithCountry(russia).
		Create()
	departureStation := factories.NewStationFactory(testInst.repoRegistry).
		WithSettlement(departure).
		WithTitle("departureStationTitle").
		Create()
	arrival := factories.NewSettlementFactory(testInst.repoRegistry).
		WithSlug("arrival").
		WithTitle("arrivalTitle").
		WithCountry(russia).
		Create()
	arrivalStation := factories.NewStationFactory(testInst.repoRegistry).
		WithSettlement(arrival).
		WithTitle("arrivalStationTitle").
		Create()
	thread := schedulefactories.NewScheduleFactory(testInst.repoRegistry).
		AddStop(departureStation, time.Hour).
		AddStop(arrivalStation, time.Hour).
		Create()
	thread.Number = "012Num"
	thread.YearDays = runmask.MaskByDate(departureDate).RawMask()

	factories.NewStationCodeFactory(testInst.repoRegistry).
		WithCode("1001").
		WithStation(departureStation).
		Create()
	factories.NewStationCodeFactory(testInst.repoRegistry).
		WithCode("1002").
		WithStation(arrivalStation).
		Create()

	require.NoError(t, testInst.scheduleRepository.UpdateCache())
	require.NoError(t, testInst.trainCityRepository.UpdateCache())
	require.NoError(t, testInst.expressRepository.UpdateCache())

	ctx := context.Background()
	testInst.tariffProvider.On("Select",
		ctx, []int32{1001}, []int32{1002},
		departureDate, departureDate.Add(units.Day),
	).Return(getTariffInfo(), nil)

	response, err := testInst.provider.GetDirections(ctx, &query.RawDirectionQuery{
		DeparturePointKey: "c" + strconv.Itoa(int(departure.Id)),
		ArrivalPointKey:   "c" + strconv.Itoa(int(arrival.Id)),
		DepartureDate:     "2021-03-16",
		MainReqID:         helpers.OptString("100"),
		Language:          "ru",
		TLD:               "ru",
	})

	require.NoError(t, err)
	require.IsType(t, response, &models.DirectionResponse{}, response.ErrMsg())
	require.False(t, response.IsError() || response.IsEmpty(), response.ErrMsg())

	directionResp := response.(*models.DirectionResponse)

	responseURL := "https://travel-test.yandex.ru/trains/departure--arrival/?when=2021-03-16&wizardReqId=100"
	orderURL := "https://travel-test.yandex.ru/trains/order/?fromId=c1&fromName=&toId=c3&toName=&transportType=train&when=2021-03-16&wizardReqId=100"
	minimumDuration := json.Number("120.0")
	assert.Equal(t, &models.DirectionResponse{
		Title: models.TitleResponse{
			HL: "Translated: trains_schedule, 16 Translated: March",
		},
		WizardReqID:        helpers.OptString("100"),
		FoundDepartureDate: "2021-03-16",
		MinimumPrice: &models.PriceResponse{
			Value:    10000,
			Currency: "USD",
		},
		MinimumDuration: &minimumDuration,
		PathItems: []models.URLResponse{
			{
				Text:     "travel-test.yandex.ru/trains/",
				TouchURL: "https://travel-test.yandex.ru/trains/?wizardReqId=100",
				URL:      "https://travel-test.yandex.ru/trains/?wizardReqId=100",
			},
			{
				Text:     "Translated: train_tickets_from_departure_point_to_arrival_point",
				TouchURL: responseURL,
				URL:      responseURL,
			},
		},
		SearchTouchURL: responseURL,
		SearchURL:      responseURL,
		Total:          1,
		SearchProps: map[string]interface{}{
			"train_common_wizard":      "0",
			"train_pp_wizard":          "1",
			"train_wizard_api_timeout": 0,
			"train_wizard_type":        "pp",
		},
		Segments: []*models.SegmentResponse{
			{
				Train: models.TrainResponse{
					Number:            "012Num",
					DisplayNumber:     "12Num",
					HasDynamicPricing: true,
					TwoStorey:         true,
					IsSuburban:        false,
					CoachOwners:       []string{"РЖД", "RRR"},
					Title:             "",
					Brand:             nil,
					ThreadType:        "unknown",
					FirstCountryCode:  "RU",
					LastCountryCode:   "BY",
					Provider:          ptr.String("P1"),
					RawTrainName:      nil,
				},
				Departure: models.DestinationResponse{
					Station: &models.PointResponse{
						Key:   "c2",
						Title: "departureStationTitle",
					},
					Settlement: &models.PointResponse{
						Key:   "c1",
						Title: "departureTitle",
					},
					LocalDatetime: models.DateTimeResponse{
						Value:    "2021-03-16T00:00:00+00:00",
						TimeZone: "UTC",
					},
				},
				Arrival: models.DestinationResponse{
					Station: &models.PointResponse{
						Key:   "c4",
						Title: "arrivalStationTitle",
					},
					Settlement: &models.PointResponse{
						Key:   "c3",
						Title: "arrivalTitle",
					},
					LocalDatetime: models.DateTimeResponse{
						Value:    "2021-03-16T02:00:00+00:00",
						TimeZone: "UTC",
					},
				},
				Duration: "120.0",
				Places: models.PlacesResponse{
					Records: []models.PlaceRecordsResponse{
						{
							Count:                11,
							CoachType:            "suite",
							MaxSeatsInTheSameCar: 13,
							ServiceClass:         "1Э",
							Price: &models.PriceResponse{
								Value:    10000,
								Currency: "USD",
							},
							PriceDetails: models.PriceDetailResponse{
								Fee:           "1002.12",
								SeveralPrices: false,
								ServicePrice:  "1003.00",
								TicketPrice:   "1004.40",
							},
						},
						{
							Count:                21,
							CoachType:            "compartment",
							MaxSeatsInTheSameCar: 23,
							ServiceClass:         "2Б",
							Price: &models.PriceResponse{
								Value:    20000,
								Currency: "RUB",
							},
							PriceDetails: models.PriceDetailResponse{
								Fee:           "2002.22",
								SeveralPrices: true,
								ServicePrice:  "2003.00",
								TicketPrice:   "2004.40",
							},
						},
					},
					UpdatedAt: &models.DateTimeResponse{
						Value:    "2020-11-22T23:24:25.000001+00:00",
						TimeZone: "UTC",
					},
					ElectronicTicket: ptr.Bool(true),
				},
				BrokenClasses: map[string][]uint32{
					"compartment": {1, 2},
					"unknown":     {5},
				},
				Facilities: nil,
				MinimumPrice: &models.PriceResponse{
					Value:    10000,
					Currency: "USD",
				},
				OrderTouchURL: orderURL,
				OrderURL:      orderURL,
				IsTheFastest:  false,
				IsTheCheapest: false,
			},
		},
		Query: models.QueryResponse{
			DeparturePoint: &models.PointResponse{
				Key:   "c1",
				Title: "departureTitle",
			},
			ArrivalPoint: &models.PointResponse{
				Key:   "c3",
				Title: "arrivalTitle",
			},
			DepartureDate: ptr.String("2021-03-16"),
			Language:      "ru",
			OrderBy:       "best",
		},

		Filters: map[string]interface{}{
			"brand":          []models.BrandFilterResponse{},
			"place_count":    []struct{}{},
			"facility":       []struct{}{},
			"arrival_time":   []struct{}{},
			"price":          []struct{}{},
			"departure_time": []struct{}{},
			"coach_type": []models.CoachTypeFilterResponse{
				{
					Value:        "common",
					Available:    false,
					Selected:     false,
					MinimumPrice: nil,
				},
				{
					Value:     "compartment",
					Available: true,
					Selected:  false,
					MinimumPrice: &models.PriceResponse{
						Value:    20000,
						Currency: "RUB",
					},
				},
				{
					Value:        "platzkarte",
					Available:    false,
					Selected:     false,
					MinimumPrice: nil,
				},
				{
					Value:        "sitting",
					Available:    false,
					Selected:     false,
					MinimumPrice: nil,
				},
				{
					Value:        "soft",
					Available:    false,
					Selected:     false,
					MinimumPrice: nil,
				},
				{
					Value:     "suite",
					Available: true,
					Selected:  false,
					MinimumPrice: &models.PriceResponse{
						Value:    10000,
						Currency: "USD",
					},
				},
				{
					Value:        "unknown",
					Available:    false,
					Selected:     false,
					MinimumPrice: nil,
				},
			},
		},
		ErrorCode: nil,
		ErrorMsg:  nil,
	}, directionResp)
}

type testInstances struct {
	logger              *zap.Logger
	repoRegistry        *registry.RepositoryRegistry
	geobaseClient       *geo.FakeGeobaseClient
	scheduleRepository  *schedule.Repository
	tariffProvider      *tariffs.TrainTariffProviderMock
	provider            *Provider
	trainCityRepository *traincity.Repository
	expressRepository   *express.Repository
}

func buildTestProvider(t *testing.T) (test testInstances) {
	test.logger = testutils.NewLogger(t)
	test.repoRegistry = registry.NewRepositoryRegistry(test.logger)
	test.geobaseClient = geo.NewFakeGeobaseClient()
	regionCapitalRepo := regioncapital.NewRepository(test.logger, test.geobaseClient, test.repoRegistry)
	pointParser := points.NewParser(test.repoRegistry, regionCapitalRepo)
	test.scheduleRepository = schedule.NewRepository(test.logger, test.repoRegistry)
	test.tariffProvider = new(tariffs.TrainTariffProviderMock)
	test.trainCityRepository = traincity.NewRepository(test.logger, test.repoRegistry)
	test.expressRepository = express.NewRepository(test.logger, test.repoRegistry, test.trainCityRepository)

	trainTitleTranslator := i18n.NewTrainTitleTranslator(i18n.FakeKeyset)
	timeTranslator := i18n.NewTimeTranslator(i18n.WithFakeForms())
	linguisticsTranslator := i18n.NewLinguisticsTranslator(
		test.geobaseClient,
		i18n.FakeKeyset,
		make(map[lang.Lang]lang.Lang),
		make(map[lang.LinguisticForm]lang.LinguisticForm),
	)
	translatableFactory := i18n.NewTranslatableFactory(linguisticsTranslator, i18n.FakeKeyset)
	test.provider = NewProvider(
		&DefaultConfig,
		test.logger,
		pointParser,
		test.repoRegistry,
		test.scheduleRepository,
		test.tariffProvider,
		filters.NewFactory(test.logger, translatableFactory),
		test.expressRepository,
		trainTitleTranslator,
		timeTranslator,
		translatableFactory,
	)
	return
}

func getTariffInfo() []*api.DirectionTariffInfo {
	createdAt := createTimestamp(2020, 1, 2, 3, 4, 5, 6789)
	updatedAt := createTimestamp(2020, 11, 22, 23, 24, 25, 1234)
	return []*api.DirectionTariffInfo{
		{
			DeparturePointExpressId: 2,
			ArrivalPointExpressId:   4,
			DepartureDate:           createDateProto(2021, 3, 16),
			Data: []*api.DirectionTariffTrain{
				{
					Arrival:          createDatetimeProto(2021, 3, 16, 2, 0, 0),
					ArrivalStationId: 4,
					BrokenClasses: &api.TariffBrokenClasses{
						Unknown:     []uint32{5},
						Compartment: []uint32{1, 2},
					},
					CoachOwners:        []string{"РЖД", "RRR"},
					Departure:          createDatetimeProto(2021, 3, 16, 0, 0, 0),
					DepartureStationId: 2,
					DisplayNumber:      "12Num",
					ElectronicTicket:   true,
					FirstCountryCode:   "RU",
					HasDynamicPricing:  true,
					IsSuburban:         false,
					LastCountryCode:    "BY",
					Number:             "012Num",
					Places: []*api.TrainPlace{
						{
							CoachType:            "suite",
							Count:                11,
							LowerCount:           12,
							MaxSeatsInTheSameCar: 13,
							Price: &travel.TPrice{
								Currency:  travel.ECurrency_C_USD,
								Amount:    1000010,
								Precision: 2,
							},
							PriceDetails: &api.TrainPlacePriceDetails{
								Fee: &travel.TPrice{
									Currency:  travel.ECurrency_C_USD,
									Amount:    100212,
									Precision: 2,
								},
								ServicePrice: &travel.TPrice{
									Currency:  travel.ECurrency_C_USD,
									Amount:    10030,
									Precision: 1,
								},
								SeveralPrices: false,
								TicketPrice: &travel.TPrice{
									Currency:  travel.ECurrency_C_USD,
									Amount:    10044,
									Precision: 1,
								},
							},
							ServiceClass: "1Э",
							UpperCount:   15,
						},
						{
							CoachType:            "compartment",
							Count:                21,
							LowerCount:           22,
							MaxSeatsInTheSameCar: 23,
							Price: &travel.TPrice{
								Currency:  travel.ECurrency_C_RUB,
								Amount:    2000020,
								Precision: 2,
							},
							PriceDetails: &api.TrainPlacePriceDetails{
								Fee: &travel.TPrice{
									Currency:  travel.ECurrency_C_RUB,
									Amount:    200222,
									Precision: 2,
								},
								ServicePrice: &travel.TPrice{
									Currency:  travel.ECurrency_C_RUB,
									Amount:    20030,
									Precision: 1,
								},
								SeveralPrices: true,
								TicketPrice: &travel.TPrice{
									Currency:  travel.ECurrency_C_RUB,
									Amount:    20044,
									Precision: 1,
								},
							},
							ServiceClass: "2Б",
							UpperCount:   25,
						},
					},
					Provider:     "P1",
					RawTrainName: "",
					TitleDict: &rasp.TThreadTitle{
						TitleParts: []*rasp.TThreadTitlePart{
							{
								SettlementId: 2,
							},
							{
								StationId: 33,
							},
						},
						Type:          rasp.TThreadTitle_TYPE_DEFAULT,
						TransportType: rasp.TTransport_TYPE_TRAIN,
					},
					TwoStorey: true,
				},
			},
			CreatedAt: createTimeProto(createdAt),
			UpdatedAt: createTimeProto(updatedAt),
		},
	}
}

func shortDate(year int, month int, day int) time.Time {
	return time.Date(year, time.Month(month), day, 0, 0, 0, 0, time.UTC)
}

func createTimestamp(year int, month int, day int, hour int, min int, sec int, nsec int) time.Time {
	return time.Date(year, time.Month(month), day, hour, min, sec, nsec, time.UTC)
}

func createDateProto(year int, month int, day int) *travel.TDate {
	return &travel.TDate{
		Year:  int32(year),
		Month: int32(month),
		Day:   int32(day),
	}
}

func createTimeProto(ts time.Time) *pts.Timestamp {
	protoTS, _ := ptypes.TimestampProto(ts)
	return protoTS
}

func createDatetime(year int, month int, day int, hour int, min int, sec int) time.Time {
	return time.Date(year, time.Month(month), day, hour, min, sec, 0, time.UTC)
}

func createDatetimeProto(year int, month int, day int, hour int, min int, sec int) *pts.Timestamp {
	return createTimeProto(createDatetime(year, month, day, hour, min, sec))
}
