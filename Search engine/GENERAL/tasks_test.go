package tasks

import (
	"sync"
	"testing"
	"time"
)

func TestLoop(t *testing.T) {
	ticks := make(chan time.Time, 2)
	l := NewLoop(ticks)

	var n int
	var wg sync.WaitGroup
	wg.Add(1)

	l.Add(TaskFunc(func() { n++ }))
	l.Add(TaskFunc(func() {
		n++
		wg.Done()
	}))

	ticks <- time.Now()
	ticks <- time.Now()

	l.Start()
	wg.Wait()

	if n != 2 {
		t.Errorf("want %d, got %d", 2, n)
	}
}
