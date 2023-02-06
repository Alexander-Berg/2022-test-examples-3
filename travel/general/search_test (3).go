package app

import (
	"context"
	"testing"
	"time"

	"a.yandex-team.ru/library/go/test/assertpb"
	"a.yandex-team.ru/travel/library/go/vault"
	tpb "a.yandex-team.ru/travel/proto"
	"github.com/stretchr/testify/assert"

	connectorMock "a.yandex-team.ru/travel/buses/backend/internal/common/connector/mock"
	"a.yandex-team.ru/travel/buses/backend/internal/common/dict"
	ilogbroker "a.yandex-team.ru/travel/buses/backend/internal/common/logbroker"
	"a.yandex-team.ru/travel/buses/backend/internal/common/utils"
	pb "a.yandex-team.ru/travel/buses/backend/proto"
	wpb "a.yandex-team.ru/travel/buses/backend/proto/worker"
)

func TestAppSearch(t *testing.T) {

	const (
		timeout = 10 * time.Second
	)

	app, appClose, err := NewTestApp(t, nil, nil)
	if !assert.NoError(t, err) {
		return
	}
	defer appClose()

	searchScenario := connectorMock.GetSearchScenario()

	searchConsumer, err := ilogbroker.NewConsumer(
		ilogbroker.ConsumerConfig{TestEnv: app.cfg.Logbroker.TestEnv},
		ilogbroker.SearchResultTopic, "", time.Time{},
		vault.NewYavSecretsResolver(), nil, app.logger)
	if !assert.NoError(t, err) {
		return
	}
	assert.NoError(t, searchConsumer.Run(app.ctx))
	searchChannel := searchConsumer.NewChannel()

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
	departure1 := tomorrow.Add(time.Hour).Unix()
	supplier1, _ := dict.GetSupplier(dict.GetSuppliersList()[0])

	t.Run("Empty search result", func(t *testing.T) {
		searchScenario.Clear()
		searchScenario.SetDefault([]*pb.TRide{})

		searchResponse, _ := app.Search(context.Background(), &wpb.TSearchRequest{
			Header:     &wpb.TRequestHeader{Priority: wpb.ERequestPriority_REQUEST_PRIORITY_NORMAL},
			SupplierId: supplier1.ID,
			From:       &p2,
			To:         &p1,
			Date:       date1,
		})
		if !assert.Equal(t, tpb.EErrorCode_EC_OK, searchResponse.Header.Code) ||
			!assert.Equal(t, uint32(1), searchResponse.QueuePosition) {
			return
		}

		searchResult := &wpb.TSearchResult{}
		err = searchChannel.ReadWithDeadline(searchResult, time.Now().Add(timeout))
		if !assert.NoError(t, err) ||
			!assert.Equal(t, tpb.EErrorCode_EC_OK, searchResult.Header.Code) ||
			!assert.Empty(t, searchResult.Rides) {
			return
		}
	})

	t.Run("Not empty search result", func(t *testing.T) {
		searchScenario.Clear()
		searchScenario.SetDefault([]*pb.TRide{})

		rides := []*pb.TRide{
			{
				Id:            "1",
				Status:        pb.ERideStatus_RIDE_STATUS_SALE,
				SupplierId:    supplier1.ID,
				From:          &p1,
				To:            &p2,
				DepartureTime: departure1,
				Price:         &tpb.TPrice{Amount: 10, Currency: tpb.ECurrency_C_RUB},
				FreeSeats:     5,
			},
			{
				Id:            "2",
				Status:        pb.ERideStatus_RIDE_STATUS_CANCELED,
				SupplierId:    supplier1.ID,
				From:          &p1,
				To:            &p2,
				DepartureTime: departure1,
				Price:         &tpb.TPrice{Amount: 10, Currency: tpb.ECurrency_C_RUB},
				FreeSeats:     5,
			},
			{
				Id:            "3",
				Status:        pb.ERideStatus_RIDE_STATUS_SALE,
				SupplierId:    supplier1.ID,
				From:          &p1,
				To:            &p2,
				DepartureTime: departure1,
				Price:         &tpb.TPrice{Amount: 100, Currency: tpb.ECurrency_C_RUB},
				FreeSeats:     5,
			},
		}
		searchScenario.Add(supplier1.ID, &p1, &p2, date1, rides)

		searchResponse, _ := app.Search(context.Background(), &wpb.TSearchRequest{
			Header:     &wpb.TRequestHeader{Priority: wpb.ERequestPriority_REQUEST_PRIORITY_NORMAL},
			SupplierId: supplier1.ID,
			From:       &p1,
			To:         &p2,
			Date:       date1,
		})
		if !assert.Equal(t, tpb.EErrorCode_EC_OK, searchResponse.Header.Code) ||
			!assert.Equal(t, uint32(1), searchResponse.QueuePosition) {
			return
		}

		searchResult := &wpb.TSearchResult{}
		err = searchChannel.ReadWithDeadline(searchResult, time.Now().Add(timeout))
		if !assert.NoError(t, err) ||
			!assert.Equal(t, tpb.EErrorCode_EC_OK, searchResult.Header.Code) ||
			!assertpb.Equal(t, rides, searchResult.Rides) {
			return
		}
	})
}
