package filters

import (
	"context"
	"testing"
	"time"

	pb "a.yandex-team.ru/travel/buses/backend/proto"
)

func TestBannedRideStorage(t *testing.T) {

	const (
		rideID1 = "rideID1"
		rideID2 = "rideID2"
		rideID3 = "rideID3"
	)

	t.Run("BannedRideStorage. Expiring must be ordered and unaffected", func(t *testing.T) {
		storage := NewBannedRideIDStorage(time.Millisecond * 100)
		ctx, ctxCancel := context.WithCancel(context.Background())
		defer ctxCancel()

		storage.Run(ctx)

		storage.Register(rideID1)
		storage.Register(rideID3)
		time.Sleep(time.Millisecond * 50)
		storage.Register(rideID2)
		storage.Register(rideID3) // must not affect

		ok1 := storage.Exists(rideID1)
		ok2 := storage.Exists(rideID2)
		ok3 := storage.Exists(rideID3)
		if !ok1 || !ok2 {
			t.Errorf("Some record expired too early: %v %v %v", ok1, ok2, ok3)
			return
		}
		time.Sleep(time.Millisecond * 60)
		ok1 = storage.Exists(rideID1)
		ok3 = storage.Exists(rideID3)
		if ok1 || ok3 {
			t.Errorf("Some record is not expired at the time: %v %v", ok1, ok3)
			return
		}

		ok2 = storage.Exists(rideID2)
		if !ok2 {
			t.Errorf("Record is expired too early, key: %v", rideID2)
			return
		}

		time.Sleep(time.Millisecond * 60)
		ok2 = storage.Exists(rideID2)
		if ok2 {
			t.Errorf("Record is not expired at the time, key: %v", rideID2)
			return
		}
	})
}

func TestBannedRidesRule(t *testing.T) {

	const (
		rideID1 = "rideID1"
		rideID2 = "rideID2"
		rideID3 = "rideID3"
	)

	t.Run("BannedRidesRule", func(t *testing.T) {
		rule := NewBannedRidesRule(time.Millisecond * 100)
		ctx, ctxCancel := context.WithCancel(context.Background())
		defer ctxCancel()

		rule.Run(ctx)

		rule.Register(&pb.TRide{Id: rideID1})
		rule.Register(&pb.TRide{Id: rideID1})
		rule.Register(&pb.TRide{Id: rideID2})
		rule.Register(&pb.TRide{Id: rideID1})

		if rule.Len() != 2 {
			t.Errorf("expected len=%d, got len=%d", 2, rule.Len())
			return
		}
		if rule.Apply(&SearchInfo{}, &pb.TRide{Id: rideID1}) {
			t.Error("expected true")
			return
		}
		if !rule.Apply(&SearchInfo{}, &pb.TRide{Id: rideID3}) {
			t.Error("expected false")
			return
		}
	})
}
