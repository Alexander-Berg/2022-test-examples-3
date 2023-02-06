package worker

import (
	"context"
	"testing"
	"time"
)

type TestTask struct {
	maxRPS         float64
	maxConcurrency uint32
	calls          int
}

func (t *TestTask) MaxRPS() float64 {
	return t.maxRPS
}

func (t *TestTask) MaxConcurrency() uint32 {
	return t.maxConcurrency
}

func (t *TestTask) Do() {
	time.Sleep(10 * time.Millisecond)
	t.calls++
}

func (t *TestTask) Calls() int {
	return t.calls
}

func TestWorker(t *testing.T) {

	t.Run("TestWorker. Scheduling with RPC limit", func(t *testing.T) {
		testTask := TestTask{maxRPS: 1000, maxConcurrency: 10000, calls: 0}
		worker := NewWorker(&testTask)
		ctx, ctxCancel := context.WithCancel(context.Background())
		defer ctxCancel()
		worker.Run(ctx)
		cnt := 0
		for i := 0; i < 10; i++ {
			time.Sleep(100 * time.Millisecond)
			if cnt == testTask.Calls() {
				t.Errorf("no runned tasks during %d period", i)
				return
			}
			cnt = testTask.Calls()
		}
		if int(testTask.MaxRPS()*0.8) > testTask.Calls() || testTask.Calls() > int(testTask.MaxRPS()*1.2) {
			t.Errorf("done %d tasks, expected about %d", testTask.Calls(), int(testTask.MaxRPS()))
			return
		}
	})

	t.Run("TestWorker. Scheduling with concurrency limit", func(t *testing.T) {
		testTask := TestTask{maxRPS: 10000, maxConcurrency: 10, calls: 0}
		worker := NewWorker(&testTask)
		ctx, ctxCancel := context.WithCancel(context.Background())
		defer ctxCancel()
		worker.Run(ctx)
		cnt := 0
		for i := 0; i < 10; i++ {
			time.Sleep(100 * time.Millisecond)
			if cnt == testTask.Calls() {
				t.Errorf("no runned tasks during %d period", i)
				return
			}
			cnt = testTask.Calls()
		}
		if 800 > testTask.Calls() || testTask.Calls() > 1200 {
			t.Errorf("done %d tasks, expected about %d", testTask.Calls(), 1000)
			return
		}
	})
}
