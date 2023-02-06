package probes

import (
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/library/go/testutil"
)

func TestState_Ready(t *testing.T) {
	state := NewState(testutil.NewLogger(t))

	first := true
	OnReady(func() error {
		if !first {
			return fmt.Errorf("first check failed")
		}
		return nil
	})(state)

	second := true
	OnReady(func() error {
		if !second {
			return fmt.Errorf("second check failed")
		}
		return nil
	})(state)

	assert.NoError(t, state.Ready())

	first, second = false, true
	assert.Error(t, state.Ready())

	first, second = true, true
	assert.NoError(t, state.Ready())

	first, second = true, false
	assert.Error(t, state.Ready())
}

func TestState_Stop(t *testing.T) {
	state := NewState(testutil.NewLogger(t))
	assert.NoError(t, state.Ready())
	state.Stop(0)
	assert.Error(t, state.Ready())
}

func TestState_SuppressingPanics(t *testing.T) {
	state := NewState(testutil.NewLogger(t))

	OnStopBefore(func() {
		panic("fail")
	})(state)

	assert.NoError(t, state.Ready())
	assert.NotPanics(t, func() {
		state.Stop(0)
	})
	assert.Error(t, state.Ready())
}
