package healthcheckbalancer

import (
	"math"
	"sync"
	"testing"
	"time"

	"github.com/stretchr/testify/require"
	"google.golang.org/grpc/balancer"
	"google.golang.org/grpc/resolver"

	"a.yandex-team.ru/library/go/core/log/nop"
)

type testManager struct {
	hostStates map[string]hostState
}

func (t *testManager) updateHosts(hosts []string) {
	panic("implement me")
}

func (t *testManager) isHealthy(host string) bool {
	return t.hostStates[host].isHealthy
}

func (t *testManager) getLatency(host string) time.Duration {
	return t.hostStates[host].lastLatency
}

// TestSubConn implements the SubConn interface, to be used in tests.
type TestSubConn struct {
	id string
}

// UpdateAddresses panics.
func (tsc *TestSubConn) UpdateAddresses([]resolver.Address) { panic("not implemented") }

// Connect is a no-op.
func (tsc *TestSubConn) Connect() {}

// String implements stringer to print human friendly error message.
func (tsc *TestSubConn) String() string {
	return tsc.id
}

func TestClosestInstancePicker_Pick(t *testing.T) {
	hostsManager := testManager{
		hostStates: map[string]hostState{
			"1": {isHealthy: true, lastLatency: 10},
			"2": {isHealthy: false, lastLatency: math.MaxInt64},
			"3": {isHealthy: true, lastLatency: 5},
			"4": {isHealthy: false, lastLatency: math.MaxInt64},
			"5": {isHealthy: true, lastLatency: 2},
			"6": {isHealthy: false, lastLatency: math.MaxInt64},
			"7": {isHealthy: false, lastLatency: math.MaxInt64},
		},
	}

	picker := closestInstancePicker{
		subConnsListByHost: map[string]*subConnsList{
			"7": newSubConnsList(&TestSubConn{"7"}),
			"2": newSubConnsList(&TestSubConn{"2"}),
			"3": newSubConnsList(&TestSubConn{"3"}),
			"1": newSubConnsList(&TestSubConn{"1"}),
			"4": newSubConnsList(&TestSubConn{"4"}),
			"5": newSubConnsList(&TestSubConn{"5"}),
			"6": newSubConnsList(&TestSubConn{"6"}),
		},
		hostListManager: &hostsManager,
		mu:              sync.Mutex{},
		logger:          &nop.Logger{},
	}

	pickResult, err := picker.Pick(balancer.PickInfo{})

	require.NoError(t, err)
	require.Equal(t, &TestSubConn{"5"}, pickResult.SubConn)
}
