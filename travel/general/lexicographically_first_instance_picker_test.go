package healthcheckbalancer

import (
	"sync"
	"testing"

	"github.com/stretchr/testify/require"
	"google.golang.org/grpc/balancer"

	"a.yandex-team.ru/library/go/core/log/nop"
)

func TestLexicographicallyFirstInstancePicker_Pick(t *testing.T) {
	hostsManager := testManager{
		hostStates: map[string]hostState{
			"5":   {isHealthy: true},
			"4":   {isHealthy: false},
			"abc": {isHealthy: true},
			"10":  {isHealthy: true},
			"1":   {isHealthy: false},
			"6":   {isHealthy: false},
			"7":   {isHealthy: false},
			"aaa": {isHealthy: true},
		},
	}

	picker := lexicographicallyFirstInstancePicker{
		subConns: []namedSubConn{
			{host: "5", subConn: &TestSubConn{"5"}},
			{host: "4", subConn: &TestSubConn{"4"}},
			{host: "abc", subConn: &TestSubConn{"abc"}},
			{host: "10", subConn: &TestSubConn{"10"}},
			{host: "1", subConn: &TestSubConn{"1"}},
			{host: "6", subConn: &TestSubConn{"6"}},
			{host: "7", subConn: &TestSubConn{"7"}},
			{host: "aaa", subConn: &TestSubConn{"aaa"}},
		},
		hostListManager: &hostsManager,
		mu:              sync.Mutex{},
		logger:          &nop.Logger{},
	}

	pickResult, err := picker.Pick(balancer.PickInfo{})

	require.NoError(t, err)
	require.Equal(t, &TestSubConn{"10"}, pickResult.SubConn)

	hostsManager.hostStates["1"] = hostState{isHealthy: true}
	pickResult, err = picker.Pick(balancer.PickInfo{})

	require.NoError(t, err)
	require.Equal(t, &TestSubConn{"1"}, pickResult.SubConn)

	hostsManager.hostStates["1"] = hostState{isHealthy: false}
	pickResult, err = picker.Pick(balancer.PickInfo{})

	require.NoError(t, err)
	require.Equal(t, &TestSubConn{"10"}, pickResult.SubConn)

	picker.subConns = append(picker.subConns, namedSubConn{host: "0", subConn: &TestSubConn{"0"}})
	hostsManager.hostStates["0"] = hostState{isHealthy: true}

	pickResult, err = picker.Pick(balancer.PickInfo{})

	require.NoError(t, err)
	require.Equal(t, &TestSubConn{"0"}, pickResult.SubConn)
}
