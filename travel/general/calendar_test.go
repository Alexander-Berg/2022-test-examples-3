package app

import (
	"context"
	"fmt"
	"testing"
	"time"

	"a.yandex-team.ru/library/go/test/assertpb"
	tpb "a.yandex-team.ru/travel/proto"
	"github.com/golang/protobuf/proto"
	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/buses/backend/internal/api/cache"
	ipb "a.yandex-team.ru/travel/buses/backend/internal/common/proto"
	"a.yandex-team.ru/travel/buses/backend/internal/common/utils"
	pb "a.yandex-team.ru/travel/buses/backend/proto"
	wpb "a.yandex-team.ru/travel/buses/backend/proto/worker"
)

func TestCalendar(t *testing.T) {

	const (
		dummySupplierID1 = 1
		dummySupplierID2 = 2
	)

	appCfg := DefaultConfig
	appCfg.Suppliers = []uint32{dummySupplierID1, dummySupplierID2}

	departurePK := &pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_SETTLEMENT, Id: 1}
	arrivalPK := &pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_SETTLEMENT, Id: 2}

	rideDefaults := &pb.TRide{
		Status:    pb.ERideStatus_RIDE_STATUS_SALE,
		From:      &pb.TPointKey{},
		To:        &pb.TPointKey{},
		FreeSeats: 10,
	}

	setSegments := func(app *App, departurePK *pb.TPointKey, arrivalPK *pb.TPointKey, supplierID uint32) {
		app.segmentsCache.SetSegments(supplierID, []*wpb.TSegment{{From: departurePK, To: arrivalPK}}, time.Now())
	}

	setEmptyRides := func(app *App, departurePK *pb.TPointKey, arrivalPK *pb.TPointKey, departureDate *tpb.TDate, supplierID uint32) {
		setSegments(app, departurePK, arrivalPK, supplierID)
		app.searchCache.Set(
			cache.NewSearchKey(supplierID, departurePK, arrivalPK, departureDate),
			&ipb.TSearchCacheRecord{
				Status: ipb.ECacheRecordStatus_CACHE_RECORD_STATUS_OK,
				Rides:  Rides{},
			},
		)
	}

	setRides := func(app *App, departurePK *pb.TPointKey, arrivalPK *pb.TPointKey, rideExtras Rides) {
		var rideGroups = map[uint32]map[time.Time]Rides{}

		for _, rideExtra := range rideExtras {
			var ride = proto.Clone(rideDefaults).(*pb.TRide)
			proto.Merge(ride, rideExtra)

			var supplierRides, supplierRidesFound = rideGroups[ride.SupplierId]
			if !supplierRidesFound {
				supplierRides = map[time.Time]Rides{}
				rideGroups[ride.SupplierId] = supplierRides
			}

			var departureDate = utils.ConvertRideSecondsToTime(ride.DepartureTime, time.UTC).Truncate(24 * time.Hour)
			supplierRides[departureDate] = append(supplierRides[departureDate], ride)
		}

		for supplierID, supplierRides := range rideGroups {
			setSegments(app, departurePK, arrivalPK, supplierID)
			for departureDate, rides := range supplierRides {
				app.searchCache.Set(
					cache.NewSearchKey(supplierID, departurePK, arrivalPK, utils.ConvertTimeToProtoDate(departureDate)),
					&ipb.TSearchCacheRecord{
						Status: ipb.ECacheRecordStatus_CACHE_RECORD_STATUS_OK,
						Rides:  rides,
					},
				)
			}
		}
	}

	t.Run("Calendar should error on big date range", func(t *testing.T) {
		app, appClose, err := NewTestApp(t, &appCfg)
		assert.NoError(t, err)
		defer appClose()

		_, err = app.Calendar(
			departurePK,
			arrivalPK,
			&tpb.TDate{Year: 2100, Month: 1, Day: 1},
			&tpb.TDate{Year: 2100, Month: 12, Day: 1},
			pb.ERequestSource_SRS_CALENDAR,
			context.Background(),
		)

		assert.EqualError(t, err, fmt.Sprintf("more than %v days requested", CalendarMaxDays))
	})

	t.Run("Calendar should find the minimum price", func(t *testing.T) {
		app, appClose, err := NewTestApp(t, &appCfg)
		assert.NoError(t, err)
		defer appClose()

		setRides(app, departurePK, arrivalPK, Rides{
			{
				Id:         "1:1",
				SupplierId: dummySupplierID1,
				DepartureTime: utils.ConvertTimeToRideSeconds(
					time.Date(2100, 1, 5, 10, 0, 0, 0, time.UTC),
				),
				Price: &tpb.TPrice{
					Amount:   100,
					Currency: tpb.ECurrency_C_RUB,
				},
			},
			{
				Id:         "1:2",
				SupplierId: dummySupplierID1,
				DepartureTime: utils.ConvertTimeToRideSeconds(
					time.Date(2100, 1, 5, 11, 0, 0, 0, time.UTC),
				),
				Price: &tpb.TPrice{
					Amount:   50,
					Currency: tpb.ECurrency_C_RUB,
				},
			},
			{
				Id:         "1:3",
				SupplierId: dummySupplierID1,
				DepartureTime: utils.ConvertTimeToRideSeconds(
					time.Date(2100, 1, 5, 12, 0, 0, 0, time.UTC),
				),
				Price: &tpb.TPrice{
					Amount:   10,
					Currency: tpb.ECurrency_C_USD,
				},
			},
			{
				Id:         "1:4",
				Status:     pb.ERideStatus_RIDE_STATUS_CANCELED,
				SupplierId: dummySupplierID1,
				DepartureTime: utils.ConvertTimeToRideSeconds(
					time.Date(2100, 1, 5, 13, 0, 0, 0, time.UTC),
				),
				Price: &tpb.TPrice{
					Amount:   1,
					Currency: tpb.ECurrency_C_USD,
				},
			},
			{
				Id:         "2:1",
				SupplierId: dummySupplierID2,
				DepartureTime: utils.ConvertTimeToRideSeconds(
					time.Date(2100, 1, 5, 14, 0, 0, 0, time.UTC),
				),
				Price: &tpb.TPrice{
					Amount:   60,
					Currency: tpb.ECurrency_C_RUB,
				},
			},
			{
				Id:         "2:2",
				SupplierId: dummySupplierID2,
				DepartureTime: utils.ConvertTimeToRideSeconds(
					time.Date(2100, 1, 5, 15, 0, 0, 0, time.UTC),
				),
				Price: &tpb.TPrice{
					Amount:   5,
					Currency: tpb.ECurrency_C_USD,
				},
			},
		})

		calendar, err := app.Calendar(
			departurePK,
			arrivalPK,
			&tpb.TDate{Year: 2100, Month: 1, Day: 1},
			&tpb.TDate{Year: 2100, Month: 1, Day: 10},
			pb.ERequestSource_SRS_CALENDAR,
			context.Background(),
		)

		assert.NoError(t, err)
		assertpb.Equal(t, []CalendarItem{
			{
				Date:      time.Date(2100, 1, 5, 0, 0, 0, 0, time.UTC),
				RideCount: 5,
				MinPrices: []*tpb.TPrice{
					{
						Amount:   50,
						Currency: tpb.ECurrency_C_RUB,
					}, {
						Amount:   5,
						Currency: tpb.ECurrency_C_USD,
					},
				},
				MinPricesBySupplierIDs: map[uint32][]*tpb.TPrice{
					dummySupplierID1: {
						{
							Amount:   50,
							Currency: tpb.ECurrency_C_RUB,
						}, {
							Amount:   10,
							Currency: tpb.ECurrency_C_USD,
						},
					},
					dummySupplierID2: {
						{
							Amount:   60,
							Currency: tpb.ECurrency_C_RUB,
						}, {
							Amount:   5,
							Currency: tpb.ECurrency_C_USD,
						},
					},
				},
			},
		}, calendar, "Unexpected Calendar output")
	})

	t.Run("Calendar should return empty results", func(t *testing.T) {
		app, appClose, err := NewTestApp(t, &appCfg)
		assert.NoError(t, err)
		defer appClose()

		setEmptyRides(app, departurePK, arrivalPK, &tpb.TDate{Year: 2100, Month: 1, Day: 5}, dummySupplierID1)
		setEmptyRides(app, departurePK, arrivalPK, &tpb.TDate{Year: 2100, Month: 1, Day: 5}, dummySupplierID2)

		calendar, err := app.Calendar(
			departurePK,
			arrivalPK,
			&tpb.TDate{Year: 2100, Month: 1, Day: 1},
			&tpb.TDate{Year: 2100, Month: 1, Day: 10},
			pb.ERequestSource_SRS_CALENDAR,
			context.Background(),
		)

		if assert.NoError(t, err) {
			assertpb.Equal(t, []CalendarItem{
				{
					Date:      time.Date(2100, 1, 5, 0, 0, 0, 0, time.UTC),
					RideCount: 0,
				},
			}, calendar, "Unexpected Calendar output")
		}
	})

	t.Run("Calendar should override empty results", func(t *testing.T) {
		app, appClose, err := NewTestApp(t, &appCfg)
		assert.NoError(t, err)
		defer appClose()

		setEmptyRides(app, departurePK, arrivalPK, &tpb.TDate{Year: 2100, Month: 1, Day: 5}, dummySupplierID1)
		setRides(app, departurePK, arrivalPK, Rides{
			{
				Id:         "2:1",
				SupplierId: dummySupplierID2,
				DepartureTime: utils.ConvertTimeToRideSeconds(
					time.Date(2100, 1, 5, 10, 0, 0, 0, time.UTC),
				),
				Price: &tpb.TPrice{
					Amount:   60,
					Currency: tpb.ECurrency_C_RUB,
				},
			},
			{
				Id:         "2:2",
				SupplierId: dummySupplierID2,
				DepartureTime: utils.ConvertTimeToRideSeconds(
					time.Date(2100, 1, 5, 11, 0, 0, 0, time.UTC),
				),
				Price: &tpb.TPrice{
					Amount:   5,
					Currency: tpb.ECurrency_C_USD,
				},
			},
		})

		calendar, err := app.Calendar(
			departurePK,
			arrivalPK,
			&tpb.TDate{Year: 2100, Month: 1, Day: 1},
			&tpb.TDate{Year: 2100, Month: 1, Day: 10},
			pb.ERequestSource_SRS_CALENDAR,
			context.Background(),
		)

		if assert.NoError(t, err) {
			assertpb.Equal(t, []CalendarItem{
				{
					Date:      time.Date(2100, 1, 5, 0, 0, 0, 0, time.UTC),
					RideCount: 2,
					MinPrices: []*tpb.TPrice{
						{
							Amount:   60,
							Currency: tpb.ECurrency_C_RUB,
						}, {
							Amount:   5,
							Currency: tpb.ECurrency_C_USD,
						},
					},
					MinPricesBySupplierIDs: map[uint32][]*tpb.TPrice{
						dummySupplierID2: {
							{
								Amount:   60,
								Currency: tpb.ECurrency_C_RUB,
							}, {
								Amount:   5,
								Currency: tpb.ECurrency_C_USD,
							},
						},
					},
				},
			}, calendar, "Unexpected Calendar output")
		}
	})

	t.Run("Calendar should return partial non-empty result", func(t *testing.T) {
		app, appClose, err := NewTestApp(t, &appCfg)
		assert.NoError(t, err)
		defer appClose()

		setSegments(app, departurePK, arrivalPK, dummySupplierID1)
		setRides(app, departurePK, arrivalPK, Rides{
			{
				Id:         "2:1",
				SupplierId: dummySupplierID2,
				DepartureTime: utils.ConvertTimeToRideSeconds(
					time.Date(2100, 1, 5, 10, 0, 0, 0, time.UTC),
				),
				Price: &tpb.TPrice{
					Amount:   60,
					Currency: tpb.ECurrency_C_RUB,
				},
			},
		})
		calendar, err := app.Calendar(
			departurePK,
			arrivalPK,
			&tpb.TDate{Year: 2100, Month: 1, Day: 1},
			&tpb.TDate{Year: 2100, Month: 1, Day: 10},
			pb.ERequestSource_SRS_CALENDAR,
			context.Background(),
		)

		if assert.NoError(t, err) {
			assertpb.Equal(t, []CalendarItem{
				{
					Date:      time.Date(2100, 1, 5, 0, 0, 0, 0, time.UTC),
					RideCount: 1,
					MinPrices: []*tpb.TPrice{
						{
							Amount:   60,
							Currency: tpb.ECurrency_C_RUB,
						},
					},
					MinPricesBySupplierIDs: map[uint32][]*tpb.TPrice{
						dummySupplierID2: {
							{
								Amount:   60,
								Currency: tpb.ECurrency_C_RUB,
							},
						},
					},
				},
			}, calendar, "Unexpected Calendar output")
		}
	})

	t.Run("Empty tests: no segments", func(t *testing.T) {
		app, appClose, err := NewTestApp(t, &appCfg)
		assert.NoError(t, err)
		defer appClose()

		calendar, err := app.Calendar(
			departurePK,
			arrivalPK,
			&tpb.TDate{Year: 2100, Month: 1, Day: 1},
			&tpb.TDate{Year: 2100, Month: 1, Day: 10},
			pb.ERequestSource_SRS_CALENDAR,
			context.Background(),
		)

		assert.NoError(t, err)
		assert.Len(t, calendar, 10, "Unexpected Calendar output")
	})

	t.Run("Empty tests: in segments1, in cache1, in segments2", func(t *testing.T) {
		app, appClose, err := NewTestApp(t, &appCfg)
		assert.NoError(t, err)
		defer appClose()

		setEmptyRides(app, departurePK, arrivalPK, &tpb.TDate{Year: 2100, Month: 1, Day: 5}, dummySupplierID1)
		setSegments(app, departurePK, arrivalPK, dummySupplierID2)

		calendar, err := app.Calendar(
			departurePK,
			arrivalPK,
			&tpb.TDate{Year: 2100, Month: 1, Day: 1},
			&tpb.TDate{Year: 2100, Month: 1, Day: 10},
			pb.ERequestSource_SRS_CALENDAR,
			context.Background(),
		)

		assert.NoError(t, err)
		assert.Len(t, calendar, 0, "Unexpected Calendar output")
	})

	t.Run("Empty tests: in segments1, in segments2", func(t *testing.T) {
		app, appClose, err := NewTestApp(t, &appCfg)
		assert.NoError(t, err)
		defer appClose()

		setSegments(app, departurePK, arrivalPK, dummySupplierID1)
		setSegments(app, departurePK, arrivalPK, dummySupplierID2)

		calendar, err := app.Calendar(
			departurePK,
			arrivalPK,
			&tpb.TDate{Year: 2100, Month: 1, Day: 1},
			&tpb.TDate{Year: 2100, Month: 1, Day: 10},
			pb.ERequestSource_SRS_CALENDAR,
			context.Background(),
		)

		assert.NoError(t, err)
		assert.Len(t, calendar, 0, "Unexpected Calendar output")
	})

	t.Run("Empty tests: in segments1, in cache1", func(t *testing.T) {
		app, appClose, err := NewTestApp(t, &appCfg)
		assert.NoError(t, err)
		defer appClose()

		setEmptyRides(app, departurePK, arrivalPK, &tpb.TDate{Year: 2100, Month: 1, Day: 5}, dummySupplierID1)

		calendar, err := app.Calendar(
			departurePK,
			arrivalPK,
			&tpb.TDate{Year: 2100, Month: 1, Day: 1},
			&tpb.TDate{Year: 2100, Month: 1, Day: 10},
			pb.ERequestSource_SRS_CALENDAR,
			context.Background(),
		)

		assert.NoError(t, err)
		assert.Len(t, calendar, 1, "Unexpected Calendar output")
	})

	t.Run("Calendar should handle fees", func(t *testing.T) {
		app, appClose, err := NewTestApp(t, &appCfg)
		assert.NoError(t, err)
		defer appClose()

		const (
			etrafficID      = 10
			rusetID         = 11
			dummySupplierID = 100
		)

		for _, supplierID := range []uint32{etrafficID, rusetID, dummySupplierID} {
			app.segmentsCache.SetSegments(supplierID, []*wpb.TSegment{{From: departurePK, To: arrivalPK}}, time.Now())
		}

		app.cfg.Suppliers = []uint32{etrafficID, rusetID, dummySupplierID}
		app.billingDict = &BillingData{Partners: map[string]PartnerData{
			"etraffic": {
				Rates: RatesData{YandexFee: 5},
			},
			"ruset": {
				Rates: RatesData{YandexFee: 10},
			},
		}}

		setRides(app, departurePK, arrivalPK, Rides{
			{
				Id:         "100:1",
				SupplierId: dummySupplierID,
				Supplier:   &pb.TSupplier{ID: dummySupplierID},
				DepartureTime: utils.ConvertTimeToRideSeconds(
					time.Date(2100, 1, 5, 10, 0, 0, 0, time.UTC),
				),
				Price: &tpb.TPrice{
					Amount:   1000,
					Currency: tpb.ECurrency_C_RUB,
				},
			},
			{
				Id:         "10:1",
				Status:     pb.ERideStatus_RIDE_STATUS_SALE,
				SupplierId: etrafficID,
				Supplier:   &pb.TSupplier{ID: etrafficID},
				DepartureTime: utils.ConvertTimeToRideSeconds(
					time.Date(2100, 1, 5, 11, 0, 0, 0, time.UTC),
				),
				Price: &tpb.TPrice{
					Amount:   100,
					Currency: tpb.ECurrency_C_RUB,
				},
			},
			{
				Id:         "11:1",
				Status:     pb.ERideStatus_RIDE_STATUS_SALE,
				SupplierId: rusetID,
				Supplier:   &pb.TSupplier{ID: rusetID},
				DepartureTime: utils.ConvertTimeToRideSeconds(
					time.Date(2100, 1, 5, 12, 0, 0, 0, time.UTC),
				),
				Price: &tpb.TPrice{
					Amount:   60,
					Currency: tpb.ECurrency_C_RUB,
				},
			},
		})

		calendar, err := app.Calendar(
			departurePK,
			arrivalPK,
			&tpb.TDate{Year: 2100, Month: 1, Day: 1},
			&tpb.TDate{Year: 2100, Month: 1, Day: 10},
			pb.ERequestSource_SRS_CALENDAR,
			context.Background(),
		)

		if assert.NoError(t, err) {
			assertpb.Equal(t, []CalendarItem{
				{
					Date:      time.Date(2100, 1, 5, 0, 0, 0, 0, time.UTC),
					RideCount: 3,
					MinPrices: []*tpb.TPrice{
						{
							Amount:   66,
							Currency: tpb.ECurrency_C_RUB,
						},
					},
					MinPricesBySupplierIDs: map[uint32][]*tpb.TPrice{
						dummySupplierID: {
							{
								Amount:   1000,
								Currency: tpb.ECurrency_C_RUB,
							},
						},
						etrafficID: {
							{
								Amount:   105,
								Currency: tpb.ECurrency_C_RUB,
							},
						},
						rusetID: {
							{
								Amount:   66,
								Currency: tpb.ECurrency_C_RUB,
							},
						},
					},
				},
			}, calendar, "Unexpected Calendar output")
		}
	})
}
