package trips

import (
	"context"
	"sort"
	"testing"
	"time"

	"github.com/araddon/dateparse"
	"github.com/jonboulle/clockwork"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/ptr"
	"a.yandex-team.ru/library/go/units"
	tripsapi "a.yandex-team.ru/travel/komod/trips/api/trips/v1"
	apimodels "a.yandex-team.ru/travel/komod/trips/internal/components/api/trips/models"
	"a.yandex-team.ru/travel/komod/trips/internal/consts"
	"a.yandex-team.ru/travel/komod/trips/internal/models"
	"a.yandex-team.ru/travel/komod/trips/internal/orders"
	"a.yandex-team.ru/travel/komod/trips/internal/references"
	"a.yandex-team.ru/travel/komod/trips/internal/testutils"
	"a.yandex-team.ru/travel/komod/trips/internal/testutils/builders"
	tripsmodels "a.yandex-team.ru/travel/komod/trips/internal/trips/models"
	"a.yandex-team.ru/travel/library/go/testutil"
	ordercommons "a.yandex-team.ru/travel/orders/proto"
	travel_commons_proto "a.yandex-team.ru/travel/proto"
	"a.yandex-team.ru/travel/proto/dicts/rasp"
)

type mockSpansHelper struct {
	mock.Mock
}

func (m *mockSpansHelper) ReduceTransfers(spans []models.Span) []models.Span {
	args := m.Called(spans)
	return args.Get(0).([]models.Span)
}

func (m *mockSpansHelper) ExtractVisitsRemovingExtremes(spans []models.Span) []models.Visit {
	args := m.Called(spans)
	return args.Get(0).([]models.Visit)
}

func (m *mockSpansHelper) RemoveDuplicatedVisits(visits []models.Visit) []models.Visit {
	args := m.Called(visits)
	return args.Get(0).([]models.Visit)
}

type mockPointImagesExtractor struct {
	mock.Mock
}

func (m *mockPointImagesExtractor) Extract(point models.Point, alias consts.ImageAlias) string {
	args := m.Called(point, alias)
	return args.String(0)
}

func TestMapHotelOrder(t *testing.T) {
	logger := testutil.NewLogger(t)
	helper := &mockSpansHelper{}
	imagesExtractor := &mockPointImagesExtractor{}
	mapper := NewTripsMapper(
		logger,
		helper,
		"http://rasp.media",
		imagesExtractor,
		NewTrainDescriptionBuilder(testutil.NewLogger(t)),
		references.NewCarrierRepository(),
	)
	userTime := dateparse.MustParse("2022-02-24T12:00")
	hotelOrder := &orders.HotelOrder{
		BaseOrder:    orders.NewBaseOrder("1", "passport", ordercommons.EDisplayOrderState_OS_FULFILLED),
		Title:        "title",
		Stars:        3,
		CheckinDate:  dateparse.MustParse("2022-02-25T12:00"),
		CheckoutDate: dateparse.MustParse("2022-02-26T12:00"),
		Address:      "address",
		Coordinates: &travel_commons_proto.TCoordinates{
			Latitude:  1,
			Longitude: 1,
		},
		ImageURLTemplate: "template/%s",
		DocumentURL:      "document_url",
	}
	expected := apimodels.HotelOrder{
		ID:           "1",
		Title:        "title",
		Stars:        3,
		CheckinDate:  dateparse.MustParse("2022-02-25T12:00"),
		DisplayDates: "25 — 26 февраля",
		Address:      "address",
		Image:        "template/S1",
		Coordinates: &travel_commons_proto.TCoordinates{
			Latitude:  1,
			Longitude: 1,
		},
		State:       ordercommons.EDisplayOrderState_OS_FULFILLED,
		DocumentURL: "document_url",
	}

	actual := mapper.mapHotelOrder(hotelOrder, userTime)

	require.Equal(t, expected, actual)
}

func TestMapTrainOrder(t *testing.T) {
	logger := testutil.NewLogger(t)
	helper := &mockSpansHelper{}
	imagesExtractor := &mockPointImagesExtractor{}
	mapper := NewTripsMapper(
		logger,
		helper,
		"http://rasp.media",
		imagesExtractor,
		NewTrainDescriptionBuilder(testutil.NewLogger(t)),
		references.NewCarrierRepository(),
	)
	userTime := dateparse.MustParse("2022-02-24T12:00")
	trainOrder := &orders.TrainOrder{
		BaseOrder: orders.NewBaseOrder("1", "passport", ordercommons.EDisplayOrderState_OS_FULFILLED),
		Route: orders.Route{
			ForwardDeparture:  dateparse.MustParse("2022-02-25T12:00"),
			BackwardDeparture: ptr.Time(dateparse.MustParse("2022-03-02T12:00")),
			FromSettlement:    &rasp.TSettlement{TitleDefault: "Москва"},
			ToSettlement:      &rasp.TSettlement{TitleDefault: "Екатеринбург"},
		},
		Trains: []*orders.Train{{
			TrainInfo: orders.TrainInfo{
				Direction:            orders.TrainDirectionForward,
				Number:               "123",
				BrandTitle:           "brand1",
				StartSettlementTitle: "Москва",
				EndSettlementTitle:   "Казань",
			},
			FromSettlement: &rasp.TSettlement{TitleDefault: "Москва"},
			FromStation:    &rasp.TStation{Id: 1},
			ToSettlement:   &rasp.TSettlement{TitleDefault: "Казань"},
			ToStation:      &rasp.TStation{Id: 2},
		}, {
			TrainInfo: orders.TrainInfo{
				Direction:            orders.TrainDirectionForward,
				Number:               "1234",
				BrandTitle:           "brand1",
				StartSettlementTitle: "Казань",
				EndSettlementTitle:   "Екатеринбург",
			},
			FromSettlement: &rasp.TSettlement{TitleDefault: "Казань"},
			FromStation:    &rasp.TStation{Id: 2},
			ToSettlement:   &rasp.TSettlement{TitleDefault: "Екатеринбург"},
			ToStation:      &rasp.TStation{Id: 3},
		}, {
			TrainInfo: orders.TrainInfo{
				Direction:            orders.TrainDirectionBackward,
				Number:               "456",
				BrandTitle:           "brand2",
				StartSettlementTitle: "Екатеринбург",
				EndSettlementTitle:   "Москва",
			},
			FromSettlement: &rasp.TSettlement{TitleDefault: "Екатеринбург"},
			FromStation:    &rasp.TStation{Id: 5},
			ToSettlement:   &rasp.TSettlement{TitleDefault: "Москва"},
			ToStation:      &rasp.TStation{Id: 1},
		}},
		PrintURL: "printurl",
	}
	expected := apimodels.TrainOrder{
		ID:                  "1",
		Title:               "Москва — Екатеринбург",
		ForwardDeparture:    dateparse.MustParse("2022-02-25T12:00"),
		DisplayDateForward:  "25 февраля, 12:00",
		DisplayDateBackward: "2 марта, 12:00",
		PrintURL:            "printurl",
		Trains: []apimodels.Train{{
			Number:      "123",
			Description: "Поезд 123 Москва — Казань, brand1",
		}, {
			Number:      "1234",
			Description: "Поезд 1234 Казань — Екатеринбург, brand1",
		}, {
			Number:      "456",
			Description: "Поезд 456 Екатеринбург — Москва, brand2",
		}},
		HasTransferWithStationChange: false,
		State:                        ordercommons.EDisplayOrderState_OS_FULFILLED,
	}

	actual := mapper.mapTrainOrder(context.Background(), trainOrder, userTime)

	require.Equal(t, expected, actual)
}

func TestMapTrainOrderWithTransfer(t *testing.T) {
	logger := testutil.NewLogger(t)
	helper := &mockSpansHelper{}
	imagesExtractor := &mockPointImagesExtractor{}
	mapper := NewTripsMapper(
		logger,
		helper,
		"http://rasp.media",
		imagesExtractor,
		NewTrainDescriptionBuilder(testutil.NewLogger(t)),
		references.NewCarrierRepository(),
	)
	userTime := dateparse.MustParse("2022-02-24T12:00")
	trainOrder := &orders.TrainOrder{
		BaseOrder: orders.NewBaseOrder("1", "passport", ordercommons.EDisplayOrderState_OS_FULFILLED),
		Route: orders.Route{
			ForwardDeparture:  dateparse.MustParse("2022-02-25T12:00"),
			BackwardDeparture: ptr.Time(dateparse.MustParse("2022-03-02T12:00")),
			FromSettlement:    &rasp.TSettlement{TitleDefault: "Москва"},
			ToSettlement:      &rasp.TSettlement{TitleDefault: "Екатеринбург"},
		},
		Trains: []*orders.Train{{
			TrainInfo: orders.TrainInfo{
				Direction:            orders.TrainDirectionForward,
				Number:               "123",
				BrandTitle:           "brand1",
				StartSettlementTitle: "Москва",
				EndSettlementTitle:   "Казань",
			},
			FromSettlement: &rasp.TSettlement{TitleDefault: "Москва"},
			FromStation:    &rasp.TStation{Id: 1},
			ToSettlement:   &rasp.TSettlement{TitleDefault: "Казань"},
			ToStation:      &rasp.TStation{Id: 2},
		}, {
			TrainInfo: orders.TrainInfo{
				Direction:            orders.TrainDirectionForward,
				Number:               "1234",
				BrandTitle:           "brand1",
				StartSettlementTitle: "Казань",
				EndSettlementTitle:   "Екатеринбург",
			},
			FromSettlement: &rasp.TSettlement{TitleDefault: "Казань"},
			FromStation:    &rasp.TStation{Id: 4},
			ToSettlement:   &rasp.TSettlement{TitleDefault: "Екатеринбург"},
			ToStation:      &rasp.TStation{Id: 3},
		}, {
			TrainInfo: orders.TrainInfo{
				Direction:            orders.TrainDirectionBackward,
				Number:               "456",
				BrandTitle:           "brand2",
				StartSettlementTitle: "Екатеринбург",
				EndSettlementTitle:   "Москва",
			},
			FromSettlement: &rasp.TSettlement{TitleDefault: "Екатеринбург"},
			FromStation:    &rasp.TStation{Id: 5},
			ToSettlement:   &rasp.TSettlement{TitleDefault: "Москва"},
			ToStation:      &rasp.TStation{Id: 1},
		}},
		PrintURL: "printurl",
	}
	expected := apimodels.TrainOrder{
		ID:                  "1",
		Title:               "Москва — Екатеринбург",
		ForwardDeparture:    dateparse.MustParse("2022-02-25T12:00"),
		DisplayDateForward:  "25 февраля, 12:00",
		DisplayDateBackward: "2 марта, 12:00",
		PrintURL:            "printurl",
		Trains: []apimodels.Train{{
			Number:      "123",
			Description: "Поезд 123 Москва — Казань, brand1",
		}, {
			Number:      "1234",
			Description: "Поезд 1234 Казань — Екатеринбург, brand1",
		}, {
			Number:      "456",
			Description: "Поезд 456 Екатеринбург — Москва, brand2",
		}},
		HasTransferWithStationChange: true,
		State:                        ordercommons.EDisplayOrderState_OS_FULFILLED,
	}

	actual := mapper.mapTrainOrder(context.Background(), trainOrder, userTime)

	require.Equal(t, expected, actual)
}

func TestMapBusOrder(t *testing.T) {
	logger := testutil.NewLogger(t)
	helper := &mockSpansHelper{}
	imagesExtractor := &mockPointImagesExtractor{}
	mapper := NewTripsMapper(
		logger,
		helper,
		"http://rasp.media",
		imagesExtractor,
		NewTrainDescriptionBuilder(testutil.NewLogger(t)),
		references.NewCarrierRepository(),
	)
	userTime := dateparse.MustParse("2022-02-24T12:00")
	busOrder := &orders.BusOrder{
		BaseOrder: orders.NewBaseOrder("1", "passport", ordercommons.EDisplayOrderState_OS_FULFILLED),
		Route: orders.Route{
			ForwardDeparture:  dateparse.MustParse("2022-02-25T12:00"),
			BackwardDeparture: ptr.Time(dateparse.MustParse("2022-03-02T12:00")),
			FromSettlement:    &rasp.TSettlement{TitleDefault: "Москва"},
			ToSettlement:      &rasp.TSettlement{TitleDefault: "Екатеринбург"},
		},
		DownloadBlankToken:   "download_blank_token",
		CarrierName:          "ООО Рога и Копыта",
		Title:                "Верхняя Пышма — Екатеринбург",
		Description:          "Педуниверситет – Южный автовокзал",
		RefundedTicketsCount: 1,
	}
	expected := apimodels.BusOrder{
		ID:                   "1",
		Title:                "Верхняя Пышма — Екатеринбург",
		ForwardDeparture:     dateparse.MustParse("2022-02-25T12:00"),
		DisplayDateForward:   "25 февраля, 12:00",
		DownloadBlankToken:   "download_blank_token",
		Description:          "Педуниверситет – Южный автовокзал",
		CarrierName:          "ООО Рога и Копыта",
		State:                ordercommons.EDisplayOrderState_OS_FULFILLED,
		TripOrderState:       tripsapi.TripOrderState_TRIP_ORDER_STATE_CONFIRMED,
		RefundedTicketsCount: 1,
	}

	actual := mapper.mapBusOrder(busOrder, userTime)

	require.Equal(t, expected, actual)
}

func TestMapAviaOrder(t *testing.T) {
	logger := testutil.NewLogger(t)
	helper := &mockSpansHelper{}
	imagesExtractor := &mockPointImagesExtractor{}
	repo := references.NewCarrierRepository()
	_, err := repo.Write(mustMarshal(&rasp.TCarrier{
		Id:              26,
		Title:           "Аэрофлот",
		RegistrationUrl: "registration on Aeroflot url",
	}))
	if err != nil {
		panic(err)
	}

	mapper := NewTripsMapper(
		logger,
		helper,
		"https://yastat.net/s3/rasp/media",
		imagesExtractor,
		NewTrainDescriptionBuilder(testutil.NewLogger(t)),
		repo,
	)
	userTime, _ := dateparse.ParseLocal("2022-02-24T13:00:00+05:00")
	expectedURL := "registration on Aeroflot url"

	aviaOrder := &orders.AviaOrder{
		BaseOrder: orders.NewBaseOrder("1", "passport", ordercommons.EDisplayOrderState_OS_FULFILLED),
		Route: orders.Route{
			ForwardDeparture:  dateparse.MustParse("2022-02-25T12:00"),
			BackwardDeparture: ptr.Time(dateparse.MustParse("2022-03-02T12:00")),
			FromSettlement:    &rasp.TSettlement{TitleDefault: "Москва"},
			ToSettlement:      &rasp.TSettlement{TitleDefault: "Екатеринбург"},
		},
		PNR: "PNR",
		Carriers: []*rasp.TCarrier{
			{
				Id:              9144,
				SvgLogo:         "pobeda.svg",
				Title:           "Победа",
				LogoBgColor:     "blue",
				RegistrationUrl: "Pobeda registration url",
			},
		},
	}
	expected := apimodels.AviaOrder{
		ID:                  "1",
		Title:               "Москва — Екатеринбург",
		ForwardDeparture:    dateparse.MustParse("2022-02-25T12:00"),
		DisplayDateForward:  "25 февраля, 12:00",
		DisplayDateBackward: "2 марта, 12:00",
		Pnr:                 "PNR",
		RegistrationURL:     expectedURL,
		Companies: []apimodels.Company{{
			Title:   "Победа",
			LogoURL: "https://yastat.net/s3/rasp/media/pobeda.svg",
			Color:   "blue",
		}},
		State: ordercommons.EDisplayOrderState_OS_FULFILLED,
	}

	actual := mapper.mapAviaOrder(aviaOrder, userTime)

	require.Equal(t, expected, actual)
}

func TestOrdersSorting(t *testing.T) {
	clock := clockwork.NewFakeClockAt(time.Now())
	type fakeOrder struct {
		id    int
		state tripsapi.TripOrderState
		start time.Time
	}
	fakeOrders := []fakeOrder{{
		id:    1,
		state: tripsapi.TripOrderState_TRIP_ORDER_STATE_CANCELLED,
		start: clock.Now(),
	}, {
		id:    2,
		state: tripsapi.TripOrderState_TRIP_ORDER_STATE_CANCELLED,
		start: clock.Now().Add(-time.Hour),
	}, {
		id:    3,
		state: tripsapi.TripOrderState_TRIP_ORDER_STATE_CONFIRMED,
		start: clock.Now(),
	}, {
		id:    4,
		state: tripsapi.TripOrderState_TRIP_ORDER_STATE_CONFIRMED,
		start: clock.Now().Add(-time.Hour),
	}}

	expected := []fakeOrder{{
		id:    4,
		state: tripsapi.TripOrderState_TRIP_ORDER_STATE_CONFIRMED,
		start: clock.Now().Add(-time.Hour),
	}, {
		id:    3,
		state: tripsapi.TripOrderState_TRIP_ORDER_STATE_CONFIRMED,
		start: clock.Now(),
	}, {
		id:    2,
		state: tripsapi.TripOrderState_TRIP_ORDER_STATE_CANCELLED,
		start: clock.Now().Add(-time.Hour),
	}, {
		id:    1,
		state: tripsapi.TripOrderState_TRIP_ORDER_STATE_CANCELLED,
		start: clock.Now(),
	}}

	sortFunc := createSortFunc(
		func(i int) tripsapi.TripOrderState { return fakeOrders[i].state },
		func(i int) time.Time { return fakeOrders[i].start },
	)

	sort.SliceStable(fakeOrders, sortFunc)

	require.EqualValues(t, expected, fakeOrders)
}

func TestTripDates(t *testing.T) {
	tripBuilder := builders.NewTrip()

	tests := []struct {
		name     string
		trip     *tripsmodels.Trip
		want     string
		userTime time.Time
	}{
		{
			name: "tomorrow active trip -> date description",
			trip: tripBuilder.Descriptive(1, "2022-01-23").
				FlyTo(2, 3*time.Hour, "1").Build("tripID", "passportID"),
			want:     "Завтра",
			userTime: testutils.ParseTime("2022-01-22"),
		},
		{
			name: "active trip a week later -> date for one active trip",
			trip: tripBuilder.Descriptive(1, "2022-01-23").
				FlyTo(2, 3*time.Hour, "1").Build("tripID", "passportID"),
			want:     "23 янв, 2022",
			userTime: testutils.ParseTime("2022-01-16"),
		},
		{
			name: "discarded trip a week later -> date for one discarded trip",
			trip: tripBuilder.Descriptive(1, "2022-01-26").RegisterOrder("1", false).
				FlyTo(2, 3*time.Hour, "1").Build("tripID", "passportID"),
			want:     "26 янв, 2022",
			userTime: testutils.ParseTime("2022-01-16"),
		},
		{
			name: "2 trips a week later, one of them is discarded -> dates only for active order",
			trip: tripBuilder.Descriptive(1, "2022-01-20").RegisterOrder("1", false).
				FlyTo(213, 2*units.Day, "1").
				Stay(3*units.Day, "2").
				FlyTo(2, units.Day, "3").
				Build("tripID", "passportID"),
			want:     "22 — 26 янв, 2022",
			userTime: testutils.ParseTime("2022-01-16"),
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			displayDate := getDisplayDate(tt.trip, tt.userTime)
			if displayDate != tt.want {
				t.Errorf("getDisplayDate() = %v, want %v", displayDate, tt.want)
			}
		})
	}
}
