package blacklist

import (
	"context"
	"strings"
	"testing"
	"time"

	"a.yandex-team.ru/library/go/core/log/zap"
	"a.yandex-team.ru/toolbox/bunker"
	"github.com/stretchr/testify/assert"
	uzap "go.uber.org/zap"
	"go.uber.org/zap/zaptest/observer"

	"a.yandex-team.ru/travel/buses/backend/internal/api/filters"
	"a.yandex-team.ru/travel/buses/backend/internal/common/logging"
	pb "a.yandex-team.ru/travel/buses/backend/proto"
)

func setupLogsCapture() (*zap.Logger, *observer.ObservedLogs) {
	core, logs := observer.New(uzap.InfoLevel)
	logger := &zap.Logger{L: uzap.New(core)}
	return logger, logs
}

type BunkerClientMock struct {
	catResult  string
	treeResult string
}

func (mock *BunkerClientMock) Cat(_ context.Context, _ string) (string, error) {
	return mock.catResult, nil
}

func (mock *BunkerClientMock) Tree(_ context.Context, _ string) (string, error) {
	return mock.treeResult, nil
}

func (*BunkerClientMock) Ping() error {
	return nil
}

func setupBunkerFetcher(treeResult string) (*bunkerFetcher, *observer.ObservedLogs) {
	var (
		logger, logs = setupLogsCapture()
		fetcher      = &bunkerFetcher{
			cfg:    &Config{BunkerPath: "blacklistPath", BunkerNode: "blacklistNode", Expiration: time.Hour},
			logger: logger,
			bunkerOptions: []bunker.Option{
				bunker.WithCustomClient(&BunkerClientMock{
					treeResult: treeResult,
					catResult:  "dummyData",
				}),
			},
		}
	)
	return fetcher, logs
}

func Test_bunkerFetcher_run_dataCallback(t *testing.T) {
	var (
		fetcher, logs = setupBunkerFetcher(`[{
			"fullName": "/blacklistPath/blacklistNode",
			"mime": "dummyMime"
		}]`)

		ctx, ctxCancel = context.WithCancel(context.Background())
		dataChan       = make(chan string, 1)
	)
	defer ctxCancel()

	fetcher.run(ctx, func(data string) {
		dataChan <- data
	})

	select {
	case data := <-dataChan:
		assert.Equal(t, "dummyData", data)
	case <-time.After(time.Millisecond):
		t.Error("no data in channel")
	}

	assert.Equal(t, 0, logs.Len())
}

func Test_bunkerFetcher_run_dataCallback_error(t *testing.T) {
	var (
		fetcher, logs = setupBunkerFetcher(`[]`)

		ctx, ctxCancel = context.WithCancel(context.Background())
		dataChan       = make(chan string, 1)
	)
	defer ctxCancel()

	fetcher.run(ctx, func(data string) {
		dataChan <- data
	})

	select {
	case <-dataChan:
		t.Error("unexpected data in channel")
	case <-time.After(time.Millisecond):
	}

	if logEntries := logs.TakeAll(); assert.Equal(t, 1, len(logEntries)) {
		assert.Equal(t, uzap.ErrorLevel, logEntries[0].Level)
		assert.Equal(t, "no blacklist at node blacklistNode", logEntries[0].Message)
	}
}

func TestBlacklist_setRulesData(t *testing.T) {
	var (
		logger, _ = logging.New(&logging.DefaultConfig)
		blacklist = Blacklist{logger: logger}
		err       error
	)

	err = blacklist.setRulesData("invalid json")
	if assert.Error(t, err) {
		assert.True(t, strings.HasPrefix(err.Error(), "invalid rules json: "))
		assert.Len(t, blacklist.rules, 0)
	}

	err = blacklist.setRulesData(`[{"unknown": "field"}]`)
	if assert.NoError(t, err) {
		assert.Len(t, blacklist.rules, 0)
	}

	err = blacklist.setRulesData(`[{"carrier": "carrier name"}]`)
	if assert.NoError(t, err) && assert.Len(t, blacklist.rules, 1) && assert.Len(t, blacklist.rules[0], 1) {
		var predicate = blacklist.rules[0][0]
		assert.False(t, predicate(&filters.SearchInfo{}, &pb.TRide{}))
		assert.True(t, predicate(&filters.SearchInfo{}, &pb.TRide{CarrierName: "carrier name"}))
	}
}

func TestBlacklist_Apply(t *testing.T) {
	var blacklist = Blacklist{
		rules: rules{
			{
				func(searchInfo *filters.SearchInfo, _ *pb.TRide) bool {
					return searchInfo.DeparturePK.Id == 1
				},
				func(searchInfo *filters.SearchInfo, _ *pb.TRide) bool {
					return searchInfo.ArrivalPK.Id == 2
				},
			},
			{
				func(_ *filters.SearchInfo, ride *pb.TRide) bool {
					return ride.Id == "badId"
				},
			},
		},
	}

	assert.False(t, blacklist.Apply(
		&filters.SearchInfo{DeparturePK: &pb.TPointKey{Id: 1}, ArrivalPK: &pb.TPointKey{Id: 2}},
		&pb.TRide{},
	))
	assert.True(t, blacklist.Apply(
		&filters.SearchInfo{DeparturePK: &pb.TPointKey{Id: 1}, ArrivalPK: &pb.TPointKey{}},
		&pb.TRide{},
	))
	assert.True(t, blacklist.Apply(
		&filters.SearchInfo{DeparturePK: &pb.TPointKey{}, ArrivalPK: &pb.TPointKey{Id: 2}},
		&pb.TRide{},
	))

	assert.False(t, blacklist.Apply(
		&filters.SearchInfo{DeparturePK: &pb.TPointKey{}, ArrivalPK: &pb.TPointKey{}},
		&pb.TRide{Id: "badId"},
	))
	assert.True(t, blacklist.Apply(
		&filters.SearchInfo{DeparturePK: &pb.TPointKey{}, ArrivalPK: &pb.TPointKey{}},
		&pb.TRide{Id: "goodId"},
	))
}

func TestBlacklist_RunFetcher(t *testing.T) {
	var (
		logger, logs = setupLogsCapture()
		fetcher      = &dummyFetcher{}
		blacklist    = Blacklist{logger: logger, fetcher: fetcher}
	)
	blacklist.RunFetcher(context.Background())

	fetcher.dataCallback("dummyData")

	if logEntries := logs.TakeAll(); assert.Equal(t, 1, len(logEntries)) {
		assert.Equal(t, uzap.ErrorLevel, logEntries[0].Level)
		assert.True(t, strings.HasPrefix(logEntries[0].Message, "Blacklist.RunFetcher: error getting rules: "))
	}
	assert.Len(t, blacklist.rules, 0)

	fetcher.dataCallback(`[{"carrier": "carrier name"}]`)

	assert.Equal(t, 0, logs.Len())
	assert.Len(t, blacklist.rules, 1)
}
