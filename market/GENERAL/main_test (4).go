package main

import (
	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/core/log/zap"
	"a.yandex-team.ru/library/go/core/xerrors"
	"a.yandex-team.ru/market/sre/services/remon/internal/runner"
	"a.yandex-team.ru/yt/go/ypath"
	"a.yandex-team.ru/yt/go/yt"
	"a.yandex-team.ru/yt/go/yterrors"
	"context"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"testing"
	"time"
)

type YTTestClient struct {
	yt.Client
	mock.Mock
}

type YTTestTX struct {
	finishChan <-chan struct{}
	yt.Tx
}

func (tx YTTestTX) LockNode(
	ctx context.Context,
	path ypath.YPath,
	mode yt.LockMode,
	options *yt.LockNodeOptions,
) (res yt.LockResult, err error) {
	return
}

func (tx YTTestTX) Finished() <-chan struct{} {
	return tx.finishChan
}

func (tx YTTestTX) Abort() error {
	return nil
}

func (yc *YTTestClient) BeginTx(ctx context.Context, options *yt.StartTxOptions) (tx yt.Tx, err error) {
	args := yc.Called(ctx, options)
	a := args.Get(0).(yt.Tx)
	return a, args.Error(1)
}

type TestCollector struct {
	runner.Collector
	mock.Mock
}

func (c *TestCollector) Collect(ctx context.Context, pool *runner.PoolGroup) {
	_ = c.Called(ctx, pool)
}

func TestRun(t *testing.T) {
	r := &runner.Runner{}
	testClient := new(YTTestClient)

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	r.YTClient = testClient
	r.Context = ctx
	logger, _ := zap.New(zap.ConsoleConfig(log.DebugLevel))
	testCollector := TestCollector{}
	r.Logger = logger
	r.Config = runner.DefaultConfig()
	r.Config.Collector.LockTime = time.Second
	r.Collectors = []runner.Collector{&testCollector}

	testTX := YTTestTX{}
	finishChan := make(chan struct{}, 10)
	testTX.finishChan = finishChan

	testClient.On("BeginTx", mock.Anything, mock.Anything).Return(testTX, nil).Twice()

	collected := false

	testCollector.On("Collect", mock.Anything, mock.Anything).Run(func(args mock.Arguments) {
		collected = true
	}).Return()

	end := time.Now().Add(time.Second)
	run(r, time.Now())
	assert.Equal(t, true, collected)
	assert.Equal(t, true, time.Now().After(end))

	collected = false
	end = time.Now().Add(time.Second)
	done := make(chan bool, 10)
	go func() {
		run(r, time.Now())
		done <- true
	}()
	time.Sleep(time.Millisecond * 500)
	finishChan <- struct{}{}

	<-done
	assert.Equal(t, true, collected)
	assert.Equal(t, true, time.Now().Before(end))

	testClient.
		On("BeginTx", mock.Anything, mock.Anything).
		Return(testTX, yterrors.Err(yterrors.CodeConcurrentTransactionLockConflict, "Locked")).
		Once()

	collected = false
	end = time.Now().Add(time.Second)
	run(r, time.Now())
	assert.Equal(t, false, collected)
	assert.Equal(t, true, time.Now().Before(end))

	testClient.
		On("BeginTx", mock.Anything, mock.Anything).
		Return(testTX, xerrors.New("test")).
		Once()

	collected = false
	end = time.Now().Add(time.Second)
	run(r, time.Now())
	assert.Equal(t, false, collected)
	assert.Equal(t, true, time.Now().Before(end))
}
