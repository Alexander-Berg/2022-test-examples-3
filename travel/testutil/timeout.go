package testutil

import (
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/core/xerrors"
)

var ErrTimeout = xerrors.New("Timeout on a function call")

func CallWithTimeout(process func(), timeout time.Duration) error {
	resultChan := make(chan struct{}, 1)
	go func() {
		process()
		resultChan <- struct{}{}
	}()

	select {
	case <-resultChan:
		return nil
	case <-time.After(timeout):
		return ErrTimeout
	}
}

func TestWithTimeout(timeout time.Duration, test func(*testing.T)) func(*testing.T) {
	return func(t *testing.T) {
		var err error
		defer func() {
			require.NoErrorf(t, err, "the test timeout has been reached")
		}()
		err = CallWithTimeout(func() { test(t) }, timeout)
	}
}
