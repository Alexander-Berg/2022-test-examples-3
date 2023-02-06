package protobuilder

import (
	"testing"
	"time"

	"github.com/jonboulle/clockwork"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/travel/avia/library/go/searchcontext"
	"a.yandex-team.ru/travel/avia/library/proto/common/v1"
	priceprediction "a.yandex-team.ru/travel/avia/library/proto/price_prediction/v1"
	resultproto "a.yandex-team.ru/travel/avia/library/proto/search_result/v1"
	"a.yandex-team.ru/travel/avia/search_results_queue_producer/internal/searchresultscache"
	travelcommon "a.yandex-team.ru/travel/proto"
)

func TestBuild(t *testing.T) {
	queryID, _ := searchcontext.ParseQID("210525-120137-785.wizard.plane.c213_s9600370_2021-05-26_2021-05-30_economy_1_0_0_ru.ru")
	expectedProto := &resultproto.Result{
		Qid:             queryID.QID,
		PointFrom:       &common.Point{Type: common.PointType_POINT_TYPE_SETTLEMENT, Id: 213},
		PointTo:         &common.Point{Type: common.PointType_POINT_TYPE_STATION, Id: 9600370},
		DateForward:     &travelcommon.TDate{Year: 2021, Month: 5, Day: 26},
		DateBackward:    &travelcommon.TDate{Year: 2021, Month: 5, Day: 30},
		ServiceClass:    common.ServiceClass_SERVICE_CLASS_ECONOMY,
		Passengers:      &common.Passengers{Adults: 1, Children: 0, Infants: 0},
		NationalVersion: common.NationalVersion_NATIONAL_VERSION_RU,
		Flights: map[string]*resultproto.Flight{
			"flight_1": {
				Key:            "flight_1",
				Number:         "1111",
				CompanyId:      26,
				StationFromId:  9600213,
				StationToId:    9600393,
				LocalArrival:   "2021-05-26T11:30:00",
				LocalDeparture: "2021-05-26T09:30:00",
				UtcArrival:     "2021-05-26T12:30:00",
				UtcDeparture:   "2021-05-26T08:30:00",
			},
			"flight_2": {
				Key:            "flight_2",
				Number:         "2222",
				CompanyId:      26,
				StationFromId:  9600393,
				StationToId:    9600370,
				LocalArrival:   "2021-05-26T13:30:00",
				LocalDeparture: "2021-05-26T12:00:00",
				UtcArrival:     "2021-05-26T11:30:00",
				UtcDeparture:   "2021-05-26T14:00:00",
			},
			"flight_3": {
				Key:            "flight_3",
				Number:         "3333",
				CompanyId:      26,
				StationFromId:  9600370,
				StationToId:    9600213,
				LocalArrival:   "2021-05-30T12:30:00",
				LocalDeparture: "2021-05-30T12:00:00",
				UtcArrival:     "2021-05-30T00:30:00",
				UtcDeparture:   "2021-05-31T00:30:00",
			},
		},
		Variants: []*resultproto.Variant{
			{
				PartnerCode: "partner_code",
				Charter:     false,
				SelfConnect: false,
				Forward: []*resultproto.FlightSegment{
					{
						FlightKey:  "flight_1",
						FareCode:   "fare_code_1",
						FareFamily: "",
						Baggage:    &common.Baggage{Included: false},
					},
					{
						FlightKey:  "flight_2",
						FareCode:   "fare_code_2",
						FareFamily: "",
						Baggage:    &common.Baggage{Included: false},
					},
				},
				Backward: []*resultproto.FlightSegment{
					{
						FlightKey:  "flight_3",
						FareCode:   "fare_code_3",
						FareFamily: "",
						Baggage:    &common.Baggage{Included: true, Pieces: 1, Weight: 23},
					},
				},
				CreatedAt:             123,
				ExpiredAt:             456,
				Price:                 &common.Price{Currency: "RUR", Value: 12345},
				PricePredictionResult: priceprediction.PricePredictionResult_PRICE_PREDICTION_RESULT_UNKNOWN,
			},
		},
	}
	clock := clockwork.NewFakeClock()

	searchResult := &searchresultscache.SearchResult{
		PartnerCode: "partner_code",
		Variants: searchresultscache.VariantsScanner{
			Value: searchresultscache.Variants{
				QueryTime:        123,
				Created:          456,
				AllVariantsCount: 2,
				Qid:              "210525-120137-785.wizard.plane.c213_s9600370_2021-05-26_2021-05-30_economy_1_0_0_ru.ru",
				Flights: map[string]*searchresultscache.Flight{
					"flight_1": {
						Arrival:       searchresultscache.Time{Local: "2021-05-26T11:30:00", Offset: -60},
						To:            9600393,
						CompanyTariff: 0,
						From:          9600213,
						Key:           "flight_1",
						Company:       26,
						AviaCompany:   26,
						Number:        "1111",
						Departure:     searchresultscache.Time{Local: "2021-05-26T09:30:00", Offset: 60},
					},
					"flight_2": {
						Arrival:       searchresultscache.Time{Local: "2021-05-26T13:30:00", Offset: 120},
						To:            9600370,
						CompanyTariff: 0,
						From:          9600393,
						Key:           "flight_2",
						Company:       26,
						AviaCompany:   26,
						Number:        "2222",
						Departure:     searchresultscache.Time{Local: "2021-05-26T12:00:00", Offset: -120},
					},
					"flight_3": {
						Arrival:       searchresultscache.Time{Local: "2021-05-30T12:30:00", Offset: 720},
						To:            9600213,
						CompanyTariff: 0,
						From:          9600370,
						Key:           "flight_3",
						Company:       26,
						AviaCompany:   26,
						Number:        "3333",
						Departure:     searchresultscache.Time{Local: "2021-05-30T12:00:00", Offset: -750},
					},
				},
				Expire: 789,
				Fares: map[string]searchresultscache.Fare{
					"fare_1": {
						Baggage:       [][]string{{"0d1dN", "0d1d1p"}, {"1d1d23d"}},
						Charter:       nil,
						Selfconnect:   false,
						PriceCategory: "unknown",
						Created:       123,
						Expire:        456,
						FareCodes:     [][]string{{"fare_code_1", "fare_code_2"}, {"fare_code_3"}},
						Route:         [][]string{{"flight_1", "flight_2"}, {"flight_3"}},
						Tariff: searchresultscache.Tariff{
							Currency: "RUR",
							Value:    12345,
						},
					},
				},
			},
		},
		Meta:      searchresultscache.MetaScanner{Value: searchresultscache.Meta{Expire: clock.Now().Add(1 * time.Hour).UTC().Unix()}},
		CreatedAt: 123,
		ExpiresAt: 456,
	}

	searchResultProto := Build(queryID, []*searchresultscache.SearchResult{searchResult}, clock)

	require.Equal(t, expectedProto, searchResultProto)
}

func TestMapBaggage(t *testing.T) {
	testCases := []struct {
		name            string
		baggageCode     string
		expectedBaggage *common.Baggage
	}{
		{
			name:            "1 piece 23 kg",
			baggageCode:     "1d1d23d",
			expectedBaggage: &common.Baggage{Included: true, Pieces: 1, Weight: 23},
		},
		{
			name:            "not included from db",
			baggageCode:     "0d1dN",
			expectedBaggage: &common.Baggage{Included: false},
		},
		{
			name:            "not included from partner",
			baggageCode:     "0d1dN",
			expectedBaggage: &common.Baggage{Included: false},
		},
		{
			name:            "not included",
			baggageCode:     "0dNN",
			expectedBaggage: &common.Baggage{Included: false},
		},
		{
			name:            "1 piece from partner",
			baggageCode:     "1d1pN",
			expectedBaggage: &common.Baggage{Included: true, Pieces: 1},
		},
		{
			name:            "1 piece from database",
			baggageCode:     "1d1dN",
			expectedBaggage: &common.Baggage{Included: true, Pieces: 1},
		},
		{
			name:            "empty",
			baggageCode:     "",
			expectedBaggage: &common.Baggage{Included: false},
		},
	}

	for _, testCase := range testCases {
		t.Run(
			testCase.name, func(t *testing.T) {
				actual := mapBaggage(testCase.baggageCode)

				require.Equal(t, testCase.expectedBaggage.Included, actual.Included)
				require.Equal(t, testCase.expectedBaggage.Pieces, actual.Pieces)
				require.Equal(t, testCase.expectedBaggage.Weight, actual.Weight)
			},
		)
	}
}
