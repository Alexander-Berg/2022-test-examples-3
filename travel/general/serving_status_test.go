package servingstatus

import (
	"context"
	"testing"
	"time"

	"github.com/jonboulle/clockwork"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	healthpb "google.golang.org/grpc/health/grpc_health_v1"

	"a.yandex-team.ru/travel/library/go/logging"
	"a.yandex-team.ru/travel/library/go/testutil"
)

type servingStatusSetterMock struct {
	mock.Mock
}

func (s *servingStatusSetterMock) SetServingStatus(serviceName string, status healthpb.HealthCheckResponse_ServingStatus) {
	s.Called(serviceName, status)
}

func TestService(t *testing.T) {
	getService := func() *Service {
		logger, _ := logging.New(&logging.DefaultConfig)
		serviceName := "test_service"
		servingStatusSetter := func(string, healthpb.HealthCheckResponse_ServingStatus) {}
		updateInterval := time.Second
		clock := clockwork.NewFakeClock()
		return NewService(logger, serviceName, servingStatusSetter, updateInterval, clock)
	}

	t.Run(
		"getServingStatus/no required conditions", func(t *testing.T) {
			s := getService()

			result := s.getServingStatus()

			assert.Equal(t, healthpb.HealthCheckResponse_SERVING, result)
		},
	)

	t.Run(
		"getServingStatus/all conditions are met", func(t *testing.T) {
			i := 0
			s := getService().
				Requires(func() bool { i++; return i == 1 }).
				Requires(func() bool { i++; return i == 2 })

			result := s.getServingStatus()

			assert.Equal(t, healthpb.HealthCheckResponse_SERVING, result)
		},
	)

	t.Run(
		"getServingStatus/some conditions aren't met", func(t *testing.T) {
			i := 0
			s := getService().
				Requires(func() bool { i++; return i == 1 }).
				Requires(func() bool { i++; return i == 1 })

			result := s.getServingStatus()

			assert.Equal(t, healthpb.HealthCheckResponse_NOT_SERVING, result)
		},
	)

	t.Run(
		"getServingStatus/some conditions panic", func(t *testing.T) {
			i := 0
			s := getService().
				Requires(func() bool { i++; return i == 1 }).
				Requires(func() bool { return i/(i-1) == 1 })

			result := s.getServingStatus()

			assert.Equal(t, healthpb.HealthCheckResponse_UNKNOWN, result)
		},
	)

	t.Run(
		"MonitorServingStatus/calls setter periodically", func(t *testing.T) {
			s := getService()
			servingStatusSetter := servingStatusSetterMock{}
			servingStatusSetter.On(
				"SetServingStatus",
				mock.AnythingOfType("string"),
				mock.AnythingOfType("HealthCheckResponse_ServingStatus"),
			).Return()
			s.servingStatusSetter = servingStatusSetter.SetServingStatus
			fakeClock := s.clock.(clockwork.FakeClock)

			s.MonitorServingStatus(context.Background())

			fakeClock.BlockUntil(1)
			servingStatusSetter.AssertNumberOfCalls(t, "SetServingStatus", 1)

			fakeClock.Advance(s.updateInterval)

			fakeClock.BlockUntil(1)
			servingStatusSetter.AssertNumberOfCalls(t, "SetServingStatus", 2)
		},
	)

	t.Run(
		"MonitorServingStatus/doesn't call setter if context is cancelled", func(t *testing.T) {
			s := getService()
			servingStatusSetter := servingStatusSetterMock{}
			servingStatusSetter.On(
				"SetServingStatus",
				mock.AnythingOfType("string"),
				mock.AnythingOfType("HealthCheckResponse_ServingStatus"),
			).Return()
			s.servingStatusSetter = servingStatusSetter.SetServingStatus
			fakeClock := s.clock.(clockwork.FakeClock)
			ctx, cancel := context.WithCancel(context.Background())

			s.MonitorServingStatus(ctx)

			fakeClock.BlockUntil(1)
			servingStatusSetter.AssertNumberOfCalls(t, "SetServingStatus", 1)

			cancel()
			fakeClock.Advance(s.updateInterval)

			isTimeouted := testutil.CallWithTimeout(func() { fakeClock.BlockUntil(1) }, s.updateInterval)
			assert.Equal(t, isTimeouted, testutil.ErrTimeout)
		},
	)
}
