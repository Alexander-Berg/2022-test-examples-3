package app

import (
	"errors"
	"fmt"
	"testing"

	persqueueRecipe "a.yandex-team.ru/kikimr/public/sdk/go/persqueue/recipe"
	"a.yandex-team.ru/library/go/core/log/zap"
	"a.yandex-team.ru/library/go/core/metrics/solomon"
	tpb "a.yandex-team.ru/travel/proto"
	"github.com/mitchellh/copystructure"
	"github.com/stretchr/testify/assert"
	"go.uber.org/goleak"
	uzap "go.uber.org/zap"
	"go.uber.org/zap/zaptest"
	"go.uber.org/zap/zaptest/observer"

	"a.yandex-team.ru/travel/buses/backend/internal/common/connector"
	wpb "a.yandex-team.ru/travel/buses/backend/proto/worker"
)

func NewTestApp(t *testing.T, cfg *Config, logger *zap.Logger) (*App, func(), error) {

	if cfg == nil {
		cfg = &DefaultConfig
	}
	cp, err := copystructure.Copy(cfg)
	if err != nil {
		return nil, func() {}, fmt.Errorf("NewTestApp: %w", err)
	}
	testCfg := cp.(*Config)

	registry := solomon.NewRegistry(nil)

	testCfg.Connector = connector.MockedConfig
	testCfg.Logbroker.TestEnv = persqueueRecipe.New(t)
	testCfg.UnifiedAgentClient.Enabled = false
	if logger == nil {
		logger = &zap.Logger{L: zaptest.NewLogger(t, zaptest.Level(uzap.ErrorLevel))}
	}

	app, err := NewApp(testCfg, registry, logger)
	if err != nil {
		return nil, func() {}, fmt.Errorf("NewTestApp: %w", err)
	}
	err = app.Run()
	if err != nil {
		return nil, func() {}, fmt.Errorf("NewTestApp: %w", err)
	}
	return app, func() {
		app.Close()
		_ = logger.L.Sync()
		goleak.VerifyNone(t)
	}, nil
}

func TestNewApp(t *testing.T) {
	_, appClose, err := NewTestApp(t, nil, nil)
	if !assert.NoError(t, err) {
		return
	}
	appClose()
}

func setupLogsCapture() (*zap.Logger, *observer.ObservedLogs) {
	core, logs := observer.New(uzap.ErrorLevel)
	logger := &zap.Logger{L: uzap.New(core)}
	return logger, logs
}

var errorTestCases = [...]struct {
	Name           string
	Error          error
	ExpectedHeader *wpb.TResponseHeader
}{
	{
		Name:  "error",
		Error: errors.New("doh"),
		ExpectedHeader: &wpb.TResponseHeader{
			Code: tpb.EErrorCode_EC_GENERAL_ERROR,
			Error: &tpb.TError{
				Code:    tpb.EErrorCode_EC_GENERAL_ERROR,
				Message: "doh",
			},
			Explanation: &wpb.TExplanation{ConnectorResponseCode: 1},
		},
	},
	{
		Name: "ErrWithMetadata",
		Error: connector.ErrWithMetadata{
			Code:    123,
			Message: "message",
		},
		ExpectedHeader: &wpb.TResponseHeader{
			Code: tpb.EErrorCode_EC_ABORTED,
			Error: &tpb.TError{
				Code:    tpb.EErrorCode_EC_ABORTED,
				Message: "connector error: message [Code = 123]",
			},
			Explanation: &wpb.TExplanation{ConnectorResponseCode: 2},
		},
	},
	{
		Name:  "ErrUnavailable",
		Error: connector.NewErrUnavailable(errors.New("doh")),
		ExpectedHeader: &wpb.TResponseHeader{
			Code: tpb.EErrorCode_EC_UNAVAILABLE,
			Error: &tpb.TError{
				Code:    tpb.EErrorCode_EC_UNAVAILABLE,
				Message: "connector unavailable: doh",
			},
			Explanation: &wpb.TExplanation{ConnectorResponseCode: 3},
		},
	},
}
