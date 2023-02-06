package filters

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/buses/backend/internal/common/dict/rasp"
	"a.yandex-team.ru/travel/buses/backend/internal/common/logging"
	"a.yandex-team.ru/travel/buses/backend/internal/common/utils"
	pb "a.yandex-team.ru/travel/buses/backend/proto"
)

func TestRideStatusRule(t *testing.T) {

	t.Run("RideStatusRule", func(t *testing.T) {
		rule := RideStatusRule{}
		if !rule.Apply(&SearchInfo{}, &pb.TRide{Status: pb.ERideStatus_RIDE_STATUS_SALE}) {
			t.Error("expected false")
			return
		}
		if rule.Apply(&SearchInfo{}, &pb.TRide{Status: pb.ERideStatus_RIDE_STATUS_UNKNOWN}) {
			t.Error("expected true")
			return
		}
	})
}

func TestDepartureInFutureRule(t *testing.T) {

	logger, _ := logging.New(&logging.DefaultConfig)

	t.Run("DepartureInFutureRule", func(t *testing.T) {
		location, _ := time.LoadLocation("Europe/Moscow")
		locationYekat, _ := time.LoadLocation("Asia/Yekaterinburg")
		now := time.Now().In(location)
		rule := NewDepartureInFutureRule(rasp.NewRepo(&rasp.DefaultConfig, logger), location)
		ruleYekat := NewDepartureInFutureRule(rasp.NewRepo(&rasp.DefaultConfig, logger), locationYekat)
		if !assert.True(t, rule.Apply(
			&SearchInfo{
				DeparturePK: &pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_SETTLEMENT, Id: 1},
			},
			&pb.TRide{
				DepartureTime: utils.ConvertTimeToRideSeconds(now.Add(time.Hour)),
			})) ||
			!assert.False(t, rule.Apply(
				&SearchInfo{
					DeparturePK: &pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_SETTLEMENT, Id: 1},
				},
				&pb.TRide{
					DepartureTime: utils.ConvertTimeToRideSeconds(now.Add(-time.Minute)),
				})) ||
			!assert.False(t, ruleYekat.Apply(
				&SearchInfo{
					DeparturePK: &pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_SETTLEMENT, Id: 1},
				},
				&pb.TRide{
					DepartureTime: utils.ConvertTimeToRideSeconds(now.Add(time.Hour)),
				})) {
			return
		}
	})
}

func TestRidesFilter(t *testing.T) {

	const (
		rideID1 = "rideID1"
		rideID2 = "rideID2"
		rideID3 = "rideID3"
	)

	logger, _ := logging.New(&logging.DefaultConfig)

	inRides := []*pb.TRide{
		{
			Id:        rideID1,
			Status:    pb.ERideStatus_RIDE_STATUS_CANCELED,
			FreeSeats: 10,
		},
		{
			Id:        rideID2,
			Status:    pb.ERideStatus_RIDE_STATUS_SALE,
			FreeSeats: 10,
		},
		{
			Id:        rideID3,
			Status:    pb.ERideStatus_RIDE_STATUS_SALE,
			FreeSeats: 0,
		},
	}

	t.Run("RidesFilter", func(t *testing.T) {

		ridesFilter := NewRidesFilter(&RideStatusRule{}, &FreeSeatsRule{})
		outRides := ridesFilter.Filter(nil, nil, inRides, logger)
		if len(outRides) != 1 || outRides[0].Id != rideID2 {
			t.Errorf("filter fails: expected [%s], got %v", rideID2, outRides)
			return
		}
	})

	t.Run("RidesFilter for empty riles list", func(t *testing.T) {

		ridesFilter := NewRidesFilter()
		outRides := ridesFilter.Filter(nil, nil, inRides, logger)
		if len(outRides) != len(inRides) {
			t.Errorf("filter fails: expected %v, got %v", inRides, outRides)
			return
		}
	})
}
