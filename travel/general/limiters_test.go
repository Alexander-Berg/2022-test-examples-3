package messagecollector

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/library/go/syncutil"
)

const sleepTime = time.Millisecond

type slowDispatcher struct{}

func (d *slowDispatcher) Dispatch([]byte) error {
	time.Sleep(sleepTime)
	return nil
}

func TestGroupLimiters(t *testing.T) {
	var dispatcher MessageDispatcher = new(slowDispatcher)
	dispatcher = NewLimitingDispatcher(dispatcher, NewInflightLimiter(10)) // примерно 10000 rps
	dispatcher = NewLimitingDispatcher(dispatcher, NewRPSLimiter(10000))

	wg := syncutil.WaitGroup{}
	for i := 0; i < 1000; i++ {
		wg.Go(func() {
			_ = dispatcher.Dispatch(nil)
		})
	}
	wg.Wait()
}

func TestInflightLimiter(t *testing.T) {
	var dispatcher MessageDispatcher = new(slowDispatcher)
	dispatcher = NewLimitingDispatcher(dispatcher, NewInflightLimiter(10)) // примерно 10000 rps

	start := time.Now()
	wg := syncutil.WaitGroup{}
	for i := 0; i < 1000; i++ {
		wg.Go(func() {
			_ = dispatcher.Dispatch(nil)
		})
	}
	wg.Wait()
	assert.Greater(t, time.Since(start), 100*time.Millisecond)
}

func TestRPSLimiter(t *testing.T) {
	var dispatcher MessageDispatcher = new(slowDispatcher)
	dispatcher = NewLimitingDispatcher(dispatcher, NewRPSLimiter(10000)) // примерно 10000 rps

	start := time.Now()
	wg := syncutil.WaitGroup{}
	for i := 0; i < 1000; i++ {
		wg.Go(func() {
			_ = dispatcher.Dispatch(nil)
		})
	}
	wg.Wait()
	assert.Greater(t, time.Since(start), 100*time.Millisecond)
}
