package errutil

import (
	"fmt"
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/library/go/core/xerrors"
)

func ExampleNewTracedError() {
	// Safe to pass a nil. An error will be created internally saying "TracedError"
	var err error
	err = NewTracedError(nil)
	fmt.Println(err.Error())

	// Passing xerrors.Errorf() instance is the quickest option to get stack frame/trace
	err = NewTracedError(xerrors.Errorf("wrap me"))
	fmt.Println(err.Error())

	// Or you can implement customError by just embedding TracedError in struct
	type customError struct {
		*TracedError
	}
	err = customError{NewTracedError(xerrors.Errorf("make me custom"))}
	fmt.Println(err.Error())

	// Don't forget, that you can use Fields to pass them to sentry.
	err = NewTracedError(xerrors.Errorf("wrap me"), Field("key", "value"))
	// Using Error interface does not print fields to keep error message clean
	fmt.Println(err.Error())
}

func Test_TracedErrorSafeNil(t *testing.T) {
	claim := assert.New(t)
	err := NewTracedError(nil)
	claim.IsType(&TracedError{}, err)
	claim.EqualError(err, "TracedError")

	// And stack frame is in right position
	tracer := err.StackTrace()
	claim.NotNil(tracer)
	frames := tracer.Frames()
	claim.Len(frames, 1)
	frame := frames[0]
	claim.Equal("Test_TracedErrorSafeNil", frameFunctionName(frame.Function))
}

func Test_TracedErrorWithFields(t *testing.T) {
	claim := assert.New(t)
	err := NewTracedError(xerrors.Errorf("wrap me"), Field("key", "value"))
	claim.EqualError(err, "wrap me")
	claim.Equal(map[string]interface{}{"key": "value"}, err.Fields())
}

func Test_ImplementCustomType(t *testing.T) {
	claim := assert.New(t)
	type customError struct {
		*TracedError
	}
	err := customError{NewTracedError(xerrors.Errorf("make me custom"))}
	claim.IsType(customError{}, err)
	claim.EqualError(err, "make me custom")

	// And we still have access to stackframes from xerrors
	tracer := err.StackTrace()
	claim.NotNil(tracer)
	frames := tracer.Frames()
	claim.Len(frames, 1)
	frame := frames[0]
	claim.Equal("Test_ImplementCustomType", frameFunctionName(frame.Function))

}

func frameFunctionName(function string) string {
	if len(function) == 0 {
		return function
	}
	split := strings.Split(function, ".")
	return split[len(split)-1]
}
