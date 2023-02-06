package app

import (
	"context"
	"net/url"
	"testing"
	"time"

	"a.yandex-team.ru/library/go/test/assertpb"
	tpb "a.yandex-team.ru/travel/proto"
	"github.com/go-chi/chi/v5/middleware"
	"github.com/stretchr/testify/assert"

	connectorMock "a.yandex-team.ru/travel/buses/backend/internal/common/connector/mock"
	"a.yandex-team.ru/travel/buses/backend/internal/common/dict"
	ipb "a.yandex-team.ru/travel/buses/backend/internal/common/proto"
	"a.yandex-team.ru/travel/buses/backend/internal/common/utils"
	pb "a.yandex-team.ru/travel/buses/backend/proto"
	wpb "a.yandex-team.ru/travel/buses/backend/proto/worker"
)

func TestAppWizard(t *testing.T) {

	app, appClose, err := NewTestApp(t, nil)
	if !assert.Equal(t, err, nil) {
		return
	}
	defer appClose()

	searchScenario := connectorMock.GetSearchScenario()

	geoID1 := int32(54)  //ekb +5
	geoID2 := int32(213) //msk +3
	p1 := pb.TPointKey{
		Type: pb.EPointKeyType_POINT_KEY_TYPE_SETTLEMENT,
		Id:   uint32(geoID1),
	}
	p2 := pb.TPointKey{
		Type: pb.EPointKeyType_POINT_KEY_TYPE_SETTLEMENT,
		Id:   uint32(geoID2),
	}
	const (
		day       = time.Hour * 24
		reqID     = "reqID"
		mainReqID = "mainReqID"
	)
	tomorrow := time.Now().Add(day).Truncate(day)
	date1 := utils.ConvertTimeToProtoDate(tomorrow)
	departure1 := tomorrow.Add(time.Hour).Unix()
	arrival1 := tomorrow.Add(time.Hour * 2).Unix()
	supplier1, _ := dict.GetSupplier(dict.GetSuppliersList()[0])
	supplier2, _ := dict.GetSupplier(dict.GetSuppliersList()[1])

	for _, supplierID := range []uint32{supplier1.ID, supplier2.ID} {
		app.segmentsCache.SetSegments(supplierID, []*wpb.TSegment{{From: &p1, To: &p2}}, time.Now())
	}

	t.Run("No rides", func(t *testing.T) {
		searchScenario.Clear()
		searchScenario.SetDefault([]*pb.TRide{})

		status := &StatusWithMessage{Status: pb.EStatus_STATUS_NOT_READY}
		for status.Status == pb.EStatus_STATUS_NOT_READY {
			ctx, ctxCancel := context.WithCancel(context.Background())
			_, status = app.Wizard(geoID2, &p2, geoID1, &p1, date1, "ru", "", ctx)
			ctxCancel()
			time.Sleep(100 * time.Millisecond)
		}
		if !assert.Equal(t, pb.EStatus_STATUS_NOT_FOUND, status.Status) {
			return
		}
	})

	t.Run("Has rides by rasp id", func(t *testing.T) {
		searchScenario.Clear()
		searchScenario.SetDefault([]*pb.TRide{})
		rides := []*pb.TRide{
			{
				Id:            "1",
				Status:        pb.ERideStatus_RIDE_STATUS_SALE,
				Supplier:      &pb.TSupplier{ID: supplier1.ID},
				From:          &p1,
				To:            &p2,
				DepartureTime: departure1,
				ArrivalTime:   arrival1,
				Price:         &tpb.TPrice{Amount: 10, Currency: tpb.ECurrency_C_RUB},
				FreeSeats:     5,
			},
			{
				Id:            "2",
				Status:        pb.ERideStatus_RIDE_STATUS_SALE,
				Supplier:      &pb.TSupplier{ID: supplier2.ID},
				From:          &p1,
				To:            &p2,
				DepartureTime: departure1,
				ArrivalTime:   arrival1,
				Price:         &tpb.TPrice{Amount: 100, Currency: tpb.ECurrency_C_RUB},
				FreeSeats:     5,
			},
		}
		searchScenario.Add(supplier1.ID, &p1, &p2, date1, rides)

		ctx, ctxCancel := context.WithCancel(context.Background())
		defer ctxCancel()
		ctx = context.WithValue(ctx, middleware.RequestIDKey, reqID)

		status := NewStatusWithMessage(pb.EStatus_STATUS_NOT_READY, "")
		response := &ipb.TWizardResponse{}
		for !status.Ok() {
			if !assert.Equal(t, pb.EStatus_STATUS_NOT_READY.String(), status.Status.String(), status.String()) {
				return
			}
			response, status = app.Wizard(0, &p1, 0, &p2, date1, "ru", "", ctx)
			time.Sleep(100 * time.Millisecond)
		}

		rideWithFee, err := app.billingDict.RideWithYandexFee(rides[0])
		if assert.NoError(t, err) {
			assert.Equal(t, (1+2)*3600, int(response.MeanTime))
			assert.Equal(t, rideWithFee.Price.Currency, response.MinPrice.Currency)
			assert.Equal(t, rideWithFee.Price.Amount, response.MinPrice.Amount)
			assert.Equal(t, 1, len(response.Rides))
			assert.Equal(t, len(response.Rides), len(response.Additives))
			assert.Equal(t, "1", response.Rides[0].Id)
			assert.NotEmpty(t, response.Favicon)
			assert.NotEmpty(t, response.GreenUrl)
			assert.NotEmpty(t, response.SerpUrl)
			assert.Equal(t, 2, len(response.SiteLinks))
			assert.NotEmpty(t, response.SiteLinks[0].Url)
			assert.NotEmpty(t, response.SiteLinks[0].Type)
			assert.NotEmpty(t, response.ToName)
			assert.NotEmpty(t, response.FromName)
			assertpb.Equal(t, date1, response.SearchDate)
			assertpb.Equal(t, date1, response.OriginalDate)
			assert.Equal(t, "Екатеринбург — Москва: билеты на автобус", response.Title)

			greenURL, err := url.Parse(response.GreenUrl)
			if assert.NoError(t, err) {
				assert.Equal(t, reqID, greenURL.Query().Get("req_id"))
				assert.Equal(t, reqID, greenURL.Query().Get("wizardReqId"))
			}
		}
	})

	t.Run("Has rides by geo id", func(t *testing.T) {

		ctx, ctxCancel := context.WithCancel(context.Background())
		defer ctxCancel()
		ctx = context.WithValue(ctx, middleware.RequestIDKey, reqID)

		status := NewStatusWithMessage(pb.EStatus_STATUS_NOT_READY, "")
		response := &ipb.TWizardResponse{}
		for !status.Ok() {
			if !assert.Equal(t, pb.EStatus_STATUS_NOT_READY.String(), status.Status.String()) {
				return
			}
			response, status = app.Wizard(geoID1, nil, geoID2, nil, date1, "ru", mainReqID, ctx)
			time.Sleep(100 * time.Millisecond)
		}

		assert.Equal(t, (1+2)*3600, int(response.MeanTime))
		assert.Equal(t, 1, len(response.Rides))
		assert.Equal(t, len(response.Rides), len(response.Additives))
		assert.Equal(t, "1", response.Rides[0].Id)
		assert.NotEmpty(t, response.Favicon)
		assert.NotEmpty(t, response.GreenUrl)
		assert.NotEmpty(t, response.SerpUrl)
		assert.Equal(t, 2, len(response.SiteLinks))
		assert.NotEmpty(t, response.SiteLinks[0].Url)
		assert.NotEmpty(t, response.SiteLinks[0].Type)
		assert.NotEmpty(t, response.ToName)
		assert.NotEmpty(t, response.FromName)
		assertpb.Equal(t, date1, response.SearchDate)
		assertpb.Equal(t, date1, response.OriginalDate)
		assert.Equal(t, "Екатеринбург — Москва: билеты на автобус", response.Title)

		serpURL, err := url.Parse(response.SerpUrl)
		assert.NoError(t, err)
		assert.Equal(t, reqID, serpURL.Query().Get("req_id"))
		assert.Equal(t, mainReqID, serpURL.Query().Get("wizardReqId"))
	})
}
