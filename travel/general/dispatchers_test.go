package messagecollector

import (
	"fmt"
	"testing"

	"github.com/cenkalti/backoff/v4"
	"github.com/stretchr/testify/assert"
)

type testLimiter struct {
	label   string
	putFunc func(string)
}

func (t *testLimiter) BeforeDispatch() {
	t.putFunc(fmt.Sprintf("+%s", t.label))
}

func (t *testLimiter) AfterDispatch() {
	t.putFunc(fmt.Sprintf("-%s", t.label))
}

type testErrMessageDispatcher struct {
	putFunc func(string)
}

func (t *testErrMessageDispatcher) Dispatch([]byte) error {
	t.putFunc("err")
	return fmt.Errorf("generated error")
}

func TestBuildDispatcher(t *testing.T) {
	lables := make([]string, 0)
	putFunc := func(l string) {
		lables = append(lables, l)
	}

	dispatcher := &testErrMessageDispatcher{
		putFunc: putFunc,
	}
	limiters := []Limiter{
		&testLimiter{
			label:   "l1",
			putFunc: putFunc,
		},
		&testLimiter{
			label:   "l2",
			putFunc: putFunc,
		},
	}

	backoffPolicy := backoff.WithMaxRetries(
		backoff.NewConstantBackOff(0),
		2,
	)

	assert.Error(t, BuildDispatcher(dispatcher, limiters, backoffPolicy).Dispatch(nil))
	assert.Equal(t, []string{
		"+l2", "+l1", "err", "-l1", "-l2",
		"+l2", "+l1", "err", "-l1", "-l2",
		"+l2", "+l1", "err", "-l1", "-l2",
	}, lables)
}
