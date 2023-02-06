package taskhttp

import (
	"context"
	"net/http"
	"net/http/httptest"
	"rex/common/model"
	"sync/atomic"
	"testing"
	"time"

	"go.uber.org/zap"
)

func TestJob_Run(t *testing.T) {
	var counter int64

	ts := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		atomic.AddInt64(&counter, 1)
		w.WriteHeader(200)
	}))
	defer ts.Close()

	req, _ := http.NewRequest("GET", ts.URL, nil)

	canRun := make(chan struct{}, 1)
	results := make(chan *TaskResult)
	ctx, cancel := context.WithCancel(context.Background())

	task := &Task{
		Ctx:     ctx,
		Client:  http.DefaultClient,
		Request: req,
		Timeout: time.Second,
		Results: results,
		Logger:  zap.NewExample(),
		Metrics: model.NoMetrics(),
	}

	go task.Run()
	canRun <- struct{}{}

	var res *TaskResult

	select {
	case res = <-results:
	case <-time.After(time.Second):
		t.Fatal("waited for result too long")
	}

	if counter != 1 {
		t.Errorf("want 1 request, got %d", counter)
	}
	if res.Response == nil {
		t.Errorf("got nil response")
	}
	if res.Err != nil {
		t.Errorf("want nil, got %s", res.Err)
	}

	// Если использовать defer, то канал закрывается
	// раньше, чем в него шлётся результат.
	cancel()
	close(canRun)
}
