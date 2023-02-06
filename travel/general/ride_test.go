package app

import (
	"fmt"
	"testing"
	"time"

	"a.yandex-team.ru/library/go/test/assertpb"
	tpb "a.yandex-team.ru/travel/proto"
	"github.com/golang/protobuf/proto"
	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/buses/backend/internal/common/connector/mock"
	"a.yandex-team.ru/travel/buses/backend/internal/common/dict"
	pb "a.yandex-team.ru/travel/buses/backend/proto"
	wpb "a.yandex-team.ru/travel/buses/backend/proto/worker"
)

func TestApp_searchRideSubstitution(t *testing.T) {
	app := App{}

	t.Run("should find correct ride", func(t *testing.T) {
		ride, status := app.chooseRide(
			&pb.TRide{
				Id:            "outdated",
				DepartureTime: time.Date(2000, 1, 1, 12, 30, 0, 0, time.UTC).Unix(),
				From:          &pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_STATION, Id: 1},
				To:            &pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_STATION, Id: 2},
			},
			Rides{
				&pb.TRide{
					Id:            "different DepartureTime",
					DepartureTime: time.Date(2000, 1, 1, 12, 0, 0, 0, time.UTC).Unix(),
					From:          &pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_STATION, Id: 1},
					To:            &pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_STATION, Id: 2},
				},
				&pb.TRide{
					Id:            "different From",
					DepartureTime: time.Date(2000, 1, 1, 12, 30, 0, 0, time.UTC).Unix(),
					From:          &pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_STATION, Id: 10},
					To:            &pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_STATION, Id: 2},
				},
				&pb.TRide{
					Id:            "different To",
					DepartureTime: time.Date(2000, 1, 1, 12, 30, 0, 0, time.UTC).Unix(),
					From:          &pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_STATION, Id: 1},
					To:            &pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_STATION, Id: 20},
				},
				&pb.TRide{
					Id:            "actual",
					DepartureTime: time.Date(2000, 1, 1, 12, 30, 0, 0, time.UTC).Unix(),
					From:          &pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_STATION, Id: 1},
					To:            &pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_STATION, Id: 2},
				},
			},
		)

		if assert.Equal(t, pb.EStatus_STATUS_RIDE_SUBSTITUTION.String(), status.SubstitutionStatus.Status.String()) {
			assert.Equal(t, "actual", ride.Id)
		}
	})

	t.Run("should error when correct ride is not found", func(t *testing.T) {
		ride, status := app.chooseRide(
			&pb.TRide{
				Id:            "outdated",
				DepartureTime: time.Date(2000, 1, 1, 12, 30, 0, 0, time.UTC).Unix(),
				From:          &pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_STATION, Id: 1},
				To:            &pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_STATION, Id: 2},
			},
			Rides{
				&pb.TRide{
					Id:            "different DepartureTime",
					DepartureTime: time.Date(2000, 1, 1, 12, 0, 0, 0, time.UTC).Unix(),
					From:          &pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_STATION, Id: 1},
					To:            &pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_STATION, Id: 2},
				},
				&pb.TRide{
					Id:            "different From",
					DepartureTime: time.Date(2000, 1, 1, 12, 30, 0, 0, time.UTC).Unix(),
					From:          &pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_STATION, Id: 10},
					To:            &pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_STATION, Id: 2},
				},
				&pb.TRide{
					Id:            "different To",
					DepartureTime: time.Date(2000, 1, 1, 12, 30, 0, 0, time.UTC).Unix(),
					From:          &pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_STATION, Id: 1},
					To:            &pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_STATION, Id: 20},
				},
			},
		)

		if assert.False(t, status.RideStatus.Ok(), "FindActualRide error: actual ride is not found") {
			assert.Nil(t, ride)
		}
	})
}

func TestApp_addBookParams(t *testing.T) {
	logger, logs := setupLogsCapture()
	app := &App{cfg: &Config{}, logger: logger}

	supplier, err := dict.GetSupplier(dict.GetSuppliersList()[0])
	if !assert.NoError(t, err) {
		return
	}
	dummyRide := &pb.TRide{
		Id:            fmt.Sprintf("%s:rideID", supplier.Name),
		SupplierId:    supplier.ID,
		DepartureTime: time.Date(2000, 1, 1, 12, 0, 0, 0, time.UTC).Unix(),
		Price:         &tpb.TPrice{Currency: tpb.ECurrency_C_RUB, Amount: 12345},
		Fee:           &tpb.TPrice{Currency: tpb.ECurrency_C_RUB, Amount: 100},
	}

	t.Run("refinement forces substitute", func(t *testing.T) {
		clientMock, teardownClientMock := mock.SetupClientMock()
		defer teardownClientMock()

		ride := proto.Clone(dummyRide).(*pb.TRide)
		rideRefinement := &pb.TRideRefinement{
			DepartureTime: time.Date(2000, 1, 1, 12, 15, 0, 0, time.UTC).Unix(),
		}

		clientMock.On("GetBookParams", dummyRide.Id).Return(
			&pb.TBookParams{}, rideRefinement, &wpb.TExplanation{}, nil)
		_, substitute, status := app.bookParams(ride)
		clientMock.AssertExpectations(t)

		assert.True(t, substitute)
		assert.Equal(t, pb.EStatus_STATUS_RIDE_REFINEMENT_ERROR, status.Status)
		assert.Equal(t,
			fmt.Sprintf(
				"App.bookParams: departure time is changed from %d to %d for %s",
				ride.DepartureTime, rideRefinement.DepartureTime, ride.Id,
			),
			status.Message,
		)
		assert.Equal(t, 0, logs.Len())
	})

	t.Run("refinement updates ride", func(t *testing.T) {
		clientMock, teardownClientMock := mock.SetupClientMock()
		defer teardownClientMock()

		ride := proto.Clone(dummyRide).(*pb.TRide)
		rideRefinement := &pb.TRideRefinement{
			From:             &pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_STATION, Id: 1},
			FromDesc:         "Station 1",
			To:               &pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_STATION, Id: 2},
			ToDesc:           "Station 2",
			DepartureTime:    time.Date(2000, 1, 1, 12, 0, 0, 0, time.UTC).Unix(),
			ArrivalTime:      time.Date(2000, 1, 1, 21, 0, 0, 0, time.UTC).Unix(),
			Price:            &tpb.TPrice{Currency: tpb.ECurrency_C_RUB, Amount: 6789},
			Fee:              &tpb.TPrice{Currency: tpb.ECurrency_C_RUB, Amount: 200},
			RefundConditions: "RefundConditions",
		}

		clientMock.On("GetBookParams", dummyRide.Id).Return(
			&pb.TBookParams{}, rideRefinement, &wpb.TExplanation{}, nil)
		_, substitute, status := app.bookParams(ride)
		clientMock.AssertExpectations(t)

		assert.False(t, substitute)
		assert.True(t, status.Ok())
		assert.Equal(t, 0, logs.Len())
		assertpb.Equal(t, ride, &pb.TRide{
			Id:               dummyRide.Id,
			SupplierId:       supplier.ID,
			From:             &pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_STATION, Id: 1},
			FromDesc:         "Station 1",
			To:               &pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_STATION, Id: 2},
			ToDesc:           "Station 2",
			DepartureTime:    time.Date(2000, 1, 1, 12, 0, 0, 0, time.UTC).Unix(),
			ArrivalTime:      time.Date(2000, 1, 1, 21, 0, 0, 0, time.UTC).Unix(),
			Price:            &tpb.TPrice{Currency: tpb.ECurrency_C_RUB, Amount: 6789},
			Fee:              &tpb.TPrice{Currency: tpb.ECurrency_C_RUB, Amount: 200},
			RefundConditions: "RefundConditions",
		})
	})
}
