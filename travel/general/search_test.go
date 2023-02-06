package app

import (
	"context"
	"fmt"
	"sort"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/library/go/test/assertpb"
	tpb "a.yandex-team.ru/travel/proto"

	"a.yandex-team.ru/travel/buses/backend/internal/api/cache"
	connectorMock "a.yandex-team.ru/travel/buses/backend/internal/common/connector/mock"
	"a.yandex-team.ru/travel/buses/backend/internal/common/dict"
	"a.yandex-team.ru/travel/buses/backend/internal/common/logging"
	ipb "a.yandex-team.ru/travel/buses/backend/internal/common/proto"
	"a.yandex-team.ru/travel/buses/backend/internal/common/utils"
	pb "a.yandex-team.ru/travel/buses/backend/proto"
	wpb "a.yandex-team.ru/travel/buses/backend/proto/worker"
)

var (
	times = map[string]int64{ // 2020-02-04 HH:MM
		"10:45": 1585824300,
		"11:00": 1585825200,
		"12:30": 1585830600,
		"15:00": 1585839600,
		"16:00": 1585843200,
		"17:00": 1585846800,
	}

	rusetID          uint32 = 11
	unitikiID        uint32 = 13
	etrafficID       uint32 = 10
	yugavtotransID   uint32 = 15
	rusetName               = "ruset"
	unitikiName             = "unitiki-new"
	etrafficName            = "etraffic"
	yugavtotransName        = "yugavtotrans"

	testRides = []pb.TRide{
		{
			Id:            "0",
			DepartureTime: times["10:45"], // 2020-04-02 10:45
			From: &pb.TPointKey{
				Type: pb.EPointKeyType_POINT_KEY_TYPE_STATION,
				Id:   989898,
			},
			FreeSeats: 23,
			Price: &tpb.TPrice{
				Amount:   100000,
				Currency: tpb.ECurrency_C_RUB,
			},
		},
		{
			Id:            "1",
			DepartureTime: times["11:00"],
			From: &pb.TPointKey{
				Type: pb.EPointKeyType_POINT_KEY_TYPE_SETTLEMENT,
				Id:   213,
			},
			FreeSeats: 21,
			Price: &tpb.TPrice{
				Amount:   110000,
				Currency: tpb.ECurrency_C_RUB,
			},
		},
		{
			Id:            "2",
			DepartureTime: times["12:30"],
			From:          &pb.TPointKey{},
			FreeSeats:     20,
			Price: &tpb.TPrice{
				Amount:   120000,
				Currency: tpb.ECurrency_C_RUB,
			},
		},
		{
			Id:            "3",
			DepartureTime: times["11:00"],
			From: &pb.TPointKey{
				Type: pb.EPointKeyType_POINT_KEY_TYPE_STATION,
				Id:   965544,
			},
			FreeSeats: 20,
			Price: &tpb.TPrice{
				Amount:   120000,
				Currency: tpb.ECurrency_C_RUB,
			},
		},
		{
			Id:            "4",
			DepartureTime: times["11:00"],
			From:          &pb.TPointKey{},
			FreeSeats:     20,
			Price: &tpb.TPrice{
				Amount:   120000,
				Currency: tpb.ECurrency_C_RUB,
			},
		},
		{
			Id:            "5",
			DepartureTime: times["12:30"],
			From: &pb.TPointKey{
				Type: pb.EPointKeyType_POINT_KEY_TYPE_SETTLEMENT,
				Id:   213,
			},
			FreeSeats: 20,
			Price: &tpb.TPrice{
				Amount:   120000,
				Currency: tpb.ECurrency_C_RUB,
			},
		},
		{
			Id:            "6",
			DepartureTime: times["15:00"],
			From: &pb.TPointKey{
				Type: pb.EPointKeyType_POINT_KEY_TYPE_STATION,
				Id:   965544,
			},
			FreeSeats: 20, // free seats priority
			Price: &tpb.TPrice{
				Amount:   120000,
				Currency: tpb.ECurrency_C_RUB,
			},
		},
		{
			Id:            "7",
			DepartureTime: times["15:00"],
			From: &pb.TPointKey{
				Type: pb.EPointKeyType_POINT_KEY_TYPE_STATION,
				Id:   965544,
			},
			FreeSeats: 0, // free seats depriority
			Price: &tpb.TPrice{
				Amount:   120000,
				Currency: tpb.ECurrency_C_RUB,
			},
		},
		{
			Id:            "8",
			DepartureTime: times["15:00"],
			From: &pb.TPointKey{
				Type: pb.EPointKeyType_POINT_KEY_TYPE_STATION,
				Id:   965545,
			},
			FreeSeats: 10,
			Price: &tpb.TPrice{
				Amount:   120000,
				Currency: tpb.ECurrency_C_RUB,
			}, // price depriority
		},
		{
			Id:            "9",
			DepartureTime: times["15:00"],
			From: &pb.TPointKey{
				Type: pb.EPointKeyType_POINT_KEY_TYPE_STATION,
				Id:   965545,
			},
			FreeSeats: 5,
			Price: &tpb.TPrice{
				Amount:   100000,
				Currency: tpb.ECurrency_C_RUB,
			}, // price priority
		},
		// 10,11 - supplier priority test
		{
			Id:            "10",
			DepartureTime: times["15:00"],
			From: &pb.TPointKey{
				Type: pb.EPointKeyType_POINT_KEY_TYPE_STATION,
				Id:   965546,
			},
			FreeSeats: 10,
			Price: &tpb.TPrice{
				Amount:   100000,
				Currency: tpb.ECurrency_C_RUB,
			},
			SupplierId: unitikiID,
		},
		{
			Id:            "11",
			DepartureTime: times["15:00"],
			From: &pb.TPointKey{
				Type: pb.EPointKeyType_POINT_KEY_TYPE_STATION,
				Id:   965546,
			},
			FreeSeats: 10,
			Price: &tpb.TPrice{
				Amount:   100000,
				Currency: tpb.ECurrency_C_RUB,
			},
			SupplierId: rusetID,
		},
		// 12,13 - test free seats with -1 value
		{
			Id:            "12",
			DepartureTime: times["16:00"],
			From: &pb.TPointKey{
				Type: pb.EPointKeyType_POINT_KEY_TYPE_STATION,
				Id:   965544,
			},
			FreeSeats: 0,
			Price: &tpb.TPrice{
				Amount:   120000,
				Currency: tpb.ECurrency_C_RUB,
			},
			SupplierId: unitikiID,
		},
		{
			Id:            "13",
			DepartureTime: times["16:00"],
			From: &pb.TPointKey{
				Type: pb.EPointKeyType_POINT_KEY_TYPE_STATION,
				Id:   965544,
			},
			FreeSeats: -1,
			Price: &tpb.TPrice{
				Amount:   120000,
				Currency: tpb.ECurrency_C_RUB,
			},
			SupplierId: unitikiID,
		},
		// 14,15 - test revenue priority
		{
			Id:            "14",
			DepartureTime: times["17:00"],
			From: &pb.TPointKey{
				Type: pb.EPointKeyType_POINT_KEY_TYPE_STATION,
				Id:   965544,
			},
			FreeSeats: 10,
			Price: &tpb.TPrice{
				Amount:   120000,
				Currency: tpb.ECurrency_C_RUB,
			},
			SupplierId: etrafficID, // revenue 0.01
		},
		{
			Id:            "15",
			DepartureTime: times["17:00"],
			From: &pb.TPointKey{
				Type: pb.EPointKeyType_POINT_KEY_TYPE_STATION,
				Id:   965544,
			},
			FreeSeats: 10,
			Price: &tpb.TPrice{
				Amount:   120000,
				Currency: tpb.ECurrency_C_RUB,
			},
			SupplierId: yugavtotransID, // revenue 10
		},
	}
)

func makeRidesSlice(indexes []int) []*pb.TRide {
	rides := make([]*pb.TRide, 0, len(indexes))
	for _, i := range indexes {
		rides = append(rides, &testRides[i])
	}
	return rides
}

func checkResult(expectedIdxs []int, deduplicated []*pb.TRide) error {
	expectedRides := makeRidesSlice(expectedIdxs)
	if len(expectedIdxs) != len(deduplicated) {
		return fmt.Errorf("unexpected result, must be %v, got %v", expectedRides, deduplicated)
	}

	for i := range deduplicated {
		if expectedRides[i].Id != deduplicated[i].Id {
			return fmt.Errorf("unexpected result, must be %v, got %v", expectedRides, deduplicated)
		}
	}
	return nil
}

func TestDeduplicate(t *testing.T) {
	logger, err := logging.New(&logging.DefaultConfig)
	if err != nil {
		t.Errorf("failed to create logger: %s", err.Error())
	}
	etrafficBilling := PartnerData{
		Rates: RatesData{
			Revenue: 0.01,
		},
	}
	yugavtotransBilling := PartnerData{
		Rates: RatesData{
			Revenue: 10,
		},
	}

	billingData := &BillingData{
		Partners: map[string]PartnerData{
			etrafficName:     etrafficBilling,
			yugavtotransName: yugavtotransBilling,
		},
	}
	app := App{
		logger:      logger,
		billingDict: billingData,
	}
	etrafficRevenue := billingData.GetRevenue(etrafficName)
	yugavtotransRevenue := billingData.GetRevenue(yugavtotransName)
	assert.True(t, etrafficRevenue > 0 && yugavtotransRevenue > 0 && yugavtotransRevenue > etrafficRevenue)
	assert.True(t, billingData.GetRevenue(unitikiName) == 0 && billingData.GetRevenue(rusetName) == 0)

	t.Run("Rides sort must sort by all properties", func(t *testing.T) {
		var testRides = makeRidesSlice([]int{11, 8, 2, 9, 7, 5, 10, 4, 3, 6, 0, 1})
		sortedRides := make([]*pb.TRide, len(testRides))
		copy(sortedRides, testRides)
		var expected = []int{0, 3, 1, 4, 5, 2, 11, 10, 9, 8, 6, 7}
		ridesSorter := RidesSorter{
			rides:   sortedRides,
			billing: billingData,
		}
		sort.Sort(ridesSorter)
		if err := checkResult(expected, sortedRides); err != nil {
			t.Errorf("sorter error, must be '%v', got %v", makeRidesSlice(expected), sortedRides)
		}
	})

	t.Run("Deduplicate must return nil on nil input", func(t *testing.T) {
		var rides []*pb.TRide = nil
		deduplicated := app.deduplicate(rides)
		if deduplicated != nil {
			t.Errorf("Deduplicate returns unexpected value, must be 'nil', got %v", deduplicated)
		}
	})

	t.Run("Deduplicate must do nothing on unique sorted rides", func(t *testing.T) {
		var ridesDoNothing = []*pb.TRide{&testRides[0], &testRides[1], &testRides[2]}
		deduplicated := app.deduplicate(ridesDoNothing)
		if len(deduplicated) != 3 || deduplicated[0].Id != "0" || deduplicated[1].Id != "1" || deduplicated[2].Id != "2" {
			t.Errorf("Deduplicate made unexpected changes, must be '%v', got %v", ridesDoNothing, deduplicated)
		}
	})

	t.Run("Deduplicate must sort by depart unique rides", func(t *testing.T) {
		var ridesUnsorted = []*pb.TRide{&testRides[2], &testRides[1], &testRides[0]}
		deduplicated := app.deduplicate(ridesUnsorted)
		if len(deduplicated) != 3 || deduplicated[0].Id != "0" || deduplicated[1].Id != "1" || deduplicated[2].Id != "2" {
			t.Errorf("Deduplicate did not sort rides, got %v", deduplicated)
		}
	})

	t.Run("Deduplicate must correctly drop by fromID", func(t *testing.T) {
		var testRides = makeRidesSlice([]int{2, 5, 4, 3, 0, 1})
		var expected = []int{0, 3, 5}
		deduplicated := app.deduplicate(testRides)
		if err := checkResult(expected, deduplicated); err != nil {
			t.Errorf("Incorrect dropping by fromID, %v", err)
		}
	})

	t.Run("Deduplicate must correctly drop by rating", func(t *testing.T) {
		var testRides = makeRidesSlice([]int{6, 7, 8, 9, 10, 11, 12, 13, 14, 15})
		var expected = []int{11, 9, 6, 13, 15}
		deduplicated := app.deduplicate(testRides)
		if err := checkResult(expected, deduplicated); err != nil {
			t.Errorf("Incorrect dropping by rating, %v", err)
		}
	})
}

func TestFees(t *testing.T) {
	etrafficSupplierID := uint32(10)
	departurePointKey := pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_SETTLEMENT, Id: 1}
	arrivalPointKey := pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_SETTLEMENT, Id: 2}
	departureDate := tpb.TDate{Year: 2000, Month: 1, Day: 2}
	departureTime := utils.ConvertTimeToRideSeconds(time.Now().Add(10 * time.Hour))

	app, appClose, err := NewTestApp(t, nil)
	if !assert.Equal(t, err, nil) {
		return
	}
	defer appClose()

	app.cfg.Suppliers = []uint32{etrafficSupplierID}
	app.segmentsCache.SetSegments(etrafficSupplierID, []*wpb.TSegment{
		{From: &departurePointKey, To: &arrivalPointKey},
	}, time.Now())

	app.searchCache.Set(
		cache.NewSearchKey(
			etrafficSupplierID,
			&departurePointKey,
			&arrivalPointKey,
			&departureDate,
		),
		&ipb.TSearchCacheRecord{
			Status: ipb.ECacheRecordStatus_CACHE_RECORD_STATUS_OK,
			Rides: Rides{
				{
					Id:       "10:1",
					From:     &departurePointKey,
					To:       &arrivalPointKey,
					Status:   pb.ERideStatus_RIDE_STATUS_SALE,
					Supplier: &pb.TSupplier{ID: etrafficID},
					Price: &tpb.TPrice{
						Amount:   100,
						Currency: tpb.ECurrency_C_RUB,
					},
					DepartureTime: departureTime,
					ArrivalTime:   departureTime,
					FreeSeats:     5,
				},
			},
		},
	)
	app.billingDict.Partners = map[string]PartnerData{
		"etraffic": {
			Rates: RatesData{YandexFee: 5},
		},
	}

	rides, ready := app.Search(&departurePointKey, &arrivalPointKey, &departureDate, false,
		pb.ERequestSource_SRS_SEARCH, context.Background())
	if assert.True(t, ready) {
		assertpb.Equal(t, Rides{
			{
				Id:       "10:1",
				From:     &departurePointKey,
				To:       &arrivalPointKey,
				Status:   pb.ERideStatus_RIDE_STATUS_SALE,
				Supplier: &pb.TSupplier{ID: etrafficID},
				Price: &tpb.TPrice{
					Amount:   105,
					Currency: tpb.ECurrency_C_RUB,
				},
				YandexFee: &tpb.TPrice{
					Amount:   5,
					Currency: tpb.ECurrency_C_RUB,
				},
				DepartureTime: departureTime,
				ArrivalTime:   departureTime,
				FreeSeats:     5,
			},
		}, rides)
	}
}

func TestAppSearch(t *testing.T) {

	app, appClose, err := NewTestApp(t, nil)
	if !assert.Equal(t, err, nil) {
		return
	}
	defer appClose()

	p1 := pb.TPointKey{
		Type: pb.EPointKeyType_POINT_KEY_TYPE_SETTLEMENT,
		Id:   213,
	}
	p2 := pb.TPointKey{
		Type: pb.EPointKeyType_POINT_KEY_TYPE_SETTLEMENT,
		Id:   22174,
	}
	day := time.Hour * 24
	tomorrow := time.Now().Add(day).Truncate(day).UTC()
	date1 := utils.ConvertTimeToProtoDate(tomorrow)
	departure1 := tomorrow.Add(time.Hour)
	departure2 := tomorrow.Add(time.Hour * 2)
	departure3 := tomorrow.Add(-time.Hour * 48)
	supplier1, _ := dict.GetSupplier(dict.GetSuppliersList()[0])
	supplier2, _ := dict.GetSupplier(dict.GetSuppliersList()[1])
	duration := time.Hour

	for _, supplierID := range []uint32{supplier1.ID, supplier2.ID} {
		app.segmentsCache.SetSegments(supplierID, []*wpb.TSegment{
			{From: &p1, To: &p2},
		}, time.Now())
	}

	t.Run("Empty search result", func(t *testing.T) {
		searchScenario := connectorMock.GetSearchScenario()
		defer searchScenario.Clear()
		searchScenario.SetDefault([]*pb.TRide{})

		var rides []*pb.TRide
		ready := false
		for !ready {
			ctx, ctxCancel := context.WithCancel(context.Background())
			rides, ready = app.Search(&p2, &p1, date1, true, pb.ERequestSource_SRS_SEARCH, ctx)
			ctxCancel()
			time.Sleep(100 * time.Millisecond)
		}

		if !assert.Len(t, rides, 0) {
			return
		}
	})

	t.Run("Not empty search result", func(t *testing.T) {
		searchScenario := connectorMock.GetSearchScenario()
		defer searchScenario.Clear()
		searchScenario.SetDefault([]*pb.TRide{})
		searchScenario.Add(supplier1.ID, &p1, &p2, date1, []*pb.TRide{
			{
				Id:            "1",
				Status:        pb.ERideStatus_RIDE_STATUS_SALE,
				SupplierId:    supplier1.ID,
				From:          &p1,
				To:            &p2,
				DepartureTime: departure1.Unix(),
				ArrivalTime:   departure1.Add(duration).Unix(),
				Price:         &tpb.TPrice{Amount: 10, Currency: tpb.ECurrency_C_RUB},
				FreeSeats:     5,
			},
			{
				Id:            "2",
				Status:        pb.ERideStatus_RIDE_STATUS_CANCELED,
				SupplierId:    supplier1.ID,
				From:          &p1,
				To:            &p2,
				DepartureTime: departure1.Unix(),
				ArrivalTime:   departure1.Add(duration).Unix(),
				Price:         &tpb.TPrice{Amount: 10, Currency: tpb.ECurrency_C_RUB},
				FreeSeats:     5,
			},
			{
				Id:            "3",
				Status:        pb.ERideStatus_RIDE_STATUS_SALE,
				SupplierId:    supplier1.ID,
				From:          &p1,
				To:            &p2,
				DepartureTime: departure1.Unix(),
				ArrivalTime:   departure1.Add(duration).Unix(),
				Price:         &tpb.TPrice{Amount: 100, Currency: tpb.ECurrency_C_RUB},
				FreeSeats:     5,
			},
		},
		)
		searchScenario.Add(supplier2.ID, &p1, &p2, date1, []*pb.TRide{
			{
				Id:            "4",
				Status:        pb.ERideStatus_RIDE_STATUS_SALE,
				SupplierId:    supplier2.ID,
				From:          &p1,
				To:            &p2,
				DepartureTime: departure1.Unix(),
				ArrivalTime:   departure1.Add(duration).Unix(),
				Price:         &tpb.TPrice{Amount: 50, Currency: tpb.ECurrency_C_RUB},
				FreeSeats:     5,
			},
			{
				Id:            "5",
				Status:        pb.ERideStatus_RIDE_STATUS_SALE,
				SupplierId:    supplier2.ID,
				From:          &p1,
				To:            &p2,
				DepartureTime: departure2.Unix(),
				ArrivalTime:   departure2.Add(duration).Unix(),
				Price:         &tpb.TPrice{Amount: 100, Currency: tpb.ECurrency_C_RUB},
				FreeSeats:     5,
			},
			{
				Id:            "6",
				Status:        pb.ERideStatus_RIDE_STATUS_SALE,
				SupplierId:    supplier2.ID,
				From:          &p1,
				To:            &p2,
				DepartureTime: departure3.Unix(),
				ArrivalTime:   departure3.Add(duration).Unix(),
				Price:         &tpb.TPrice{Amount: 1, Currency: tpb.ECurrency_C_RUB},
				FreeSeats:     5,
			},
		},
		)

		var rides []*pb.TRide
		ready := false
		for !ready {
			ctx, ctxCancel := context.WithCancel(context.Background())
			rides, ready = app.Search(&p1, &p2, date1, true, pb.ERequestSource_SRS_SEARCH, ctx)
			ctxCancel()
			time.Sleep(100 * time.Millisecond)
		}

		ids := make([]string, len(rides))
		for i, ride := range rides {
			assert.Equal(t, int64(duration.Seconds()), ride.Duration)
			ids[i] = ride.Id
		}

		if !assert.Equal(t, []string{"1", "5"}, ids) {
			return
		}
	})
}

func TestAppSearchRange(t *testing.T) {

	app, appClose, err := NewTestApp(t, nil)
	if !assert.Equal(t, err, nil) {
		return
	}
	defer appClose()

	pk1 := &pb.TPointKey{
		Type: pb.EPointKeyType_POINT_KEY_TYPE_SETTLEMENT,
		Id:   1,
	}
	pk2 := &pb.TPointKey{
		Type: pb.EPointKeyType_POINT_KEY_TYPE_SETTLEMENT,
		Id:   2,
	}

	day := time.Hour * 24
	tomorrow := time.Now().Add(day).Truncate(day).UTC()
	tomorrowPlus2D := tomorrow.Add(day * 2)
	searchDate := utils.ConvertTimeToProtoDate(tomorrow)
	expectedDate := utils.ConvertTimeToProtoDate(tomorrowPlus2D)
	supplier, _ := dict.GetSupplier(dict.GetSuppliersList()[0])
	const expectedRideID = "expectedRideID"

	app.segmentsCache.SetSegments(supplier.ID, []*wpb.TSegment{
		{From: pk1, To: pk2},
	}, time.Now())

	t.Run("Empty search result", func(t *testing.T) {
		searchScenario := connectorMock.GetSearchScenario()
		defer searchScenario.Clear()
		searchScenario.SetDefault([]*pb.TRide{})

		var (
			ridesDate *tpb.TDate
			rides     []*pb.TRide
			ready     bool
		)
		for !ready {
			ctx, ctxCancel := context.WithCancel(context.Background())
			ridesDate, rides, ready = app.SearchRange(pk2, pk1, searchDate, 3, pb.ERequestSource_SRS_SEARCH, ctx)
			ctxCancel()
			time.Sleep(100 * time.Millisecond)
		}

		if !assert.Nil(t, ridesDate) || !assert.Len(t, rides, 0) {
			return
		}
	})

	t.Run("Not empty search result", func(t *testing.T) {
		searchScenario := connectorMock.GetSearchScenario()
		defer searchScenario.Clear()
		searchScenario.SetDefault([]*pb.TRide{})
		searchScenario.Add(supplier.ID, pk1, pk2, expectedDate, []*pb.TRide{
			{
				Id:            expectedRideID,
				Status:        pb.ERideStatus_RIDE_STATUS_SALE,
				SupplierId:    supplier.ID,
				From:          pk1,
				To:            pk2,
				DepartureTime: tomorrowPlus2D.Add(time.Hour).Unix(),
				Price:         &tpb.TPrice{Amount: 10, Currency: tpb.ECurrency_C_RUB},
				FreeSeats:     5,
			},
		})

		var (
			ridesDate *tpb.TDate
			rides     []*pb.TRide
			ready     bool
		)
		for !ready {
			ctx, ctxCancel := context.WithCancel(context.Background())
			ridesDate, rides, ready = app.SearchRange(pk1, pk2, searchDate, 3, pb.ERequestSource_SRS_SEARCH, ctx)
			ctxCancel()
			time.Sleep(100 * time.Millisecond)
		}

		ids := make([]string, len(rides))
		for i, ride := range rides {
			ids[i] = ride.Id
		}

		if !assert.Equal(t, []string{expectedRideID}, ids) || !assertpb.Equal(t, ridesDate, expectedDate) {
			return
		}
	})
}
