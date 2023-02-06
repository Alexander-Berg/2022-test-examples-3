package util

import (
	"github.com/stretchr/testify/require"
	"math/rand"
	"testing"
	"time"
)

func TestGenMatchFuncEmpty(t *testing.T) {
	rand.Seed(time.Now().UnixNano())

	f := GenMatchFunc("")
	for i := 0; i < 100; i++ {
		require.True(t, f(rand.Int()))
	}
}

func TestGenMatchFuncFixed(t *testing.T) {
	f := GenMatchFunc("1,2,10,5")
	require.True(t, f(1))
	require.True(t, f(2))
	require.False(t, f(-1))
	require.False(t, f(0))
	require.True(t, f(1))
	require.False(t, f(3))
	require.True(t, f(5))
	require.True(t, f(10))
	require.False(t, f(0))
}
