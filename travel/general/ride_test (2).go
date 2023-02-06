package utils

import (
	"fmt"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"

	pb "a.yandex-team.ru/travel/buses/backend/proto"
)

type callbackMock struct {
	mock.Mock
}

func (m *callbackMock) Call() string {
	args := m.MethodCalled("Call")
	return args.String(0)
}

func TestLazyStringer(t *testing.T) {
	m := callbackMock{}
	m.On("Call").Return("result")

	s := LazyStringer(m.Call)
	m.AssertNotCalled(t, "Call")

	assert.Equal(t, "result = result", fmt.Sprintf("result = %s", s))
	m.AssertCalled(t, "Call")
}

func TestRideStringer(t *testing.T) {
	ride := pb.TRide{
		Id:            "ride.Id",
		SupplierId:    42,
		DepartureTime: time.Date(2000, 1, 1, 12, 0, 0, 0, time.UTC).Unix(),
		ArrivalTime:   time.Date(2000, 1, 1, 15, 0, 0, 0, time.UTC).Unix(),
		From:          &pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_STATION, Id: 1},
		To:            &pb.TPointKey{Type: pb.EPointKeyType_POINT_KEY_TYPE_STATION, Id: 2},
	}

	assert.Equal(t,
		"ride = [Id: ride.Id, SupplierId: 42, From: [s1], To: [s2], DepartureTime: [2000-01-01T12:00:00], ArrivalTime: [2000-01-01T15:00:00]]",
		fmt.Sprintf("ride = %s", RideStringer(&ride)),
	)
}
