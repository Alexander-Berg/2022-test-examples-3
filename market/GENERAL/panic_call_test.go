package runtimex

import (
	"runtime"
	"strings"
	"testing"

	"github.com/stretchr/testify/require"
)

func TestGetPanicCall(t *testing.T) {
	doPanic := func() int {
		panic("lala")
	}
	var frame runtime.Frame
	func() {
		defer func() {
			if rv := recover(); rv != nil {
				frame = GetPanicCall()
			}
		}()
		doPanic()
	}()
	require.True(t, strings.HasSuffix(frame.Function, "TestGetPanicCall.func1"), frame.Function)
	require.True(t, strings.HasSuffix(frame.File, "panic_call_test.go"), frame.File)
}
