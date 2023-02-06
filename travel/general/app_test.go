package app

import (
	"fmt"
	"testing"

	persqueueRecipe "a.yandex-team.ru/kikimr/public/sdk/go/persqueue/recipe"
	"a.yandex-team.ru/library/go/core/log/zap"
	"a.yandex-team.ru/library/go/test/yatest"
	"a.yandex-team.ru/travel/library/go/metrics"
	"a.yandex-team.ru/travel/library/go/resourcestorage"
	"github.com/mitchellh/copystructure"
	"github.com/stretchr/testify/assert"
	"go.uber.org/goleak"
	uzap "go.uber.org/zap"
	"go.uber.org/zap/zaptest"
	"go.uber.org/zap/zaptest/observer"

	"a.yandex-team.ru/travel/buses/backend/internal/api/blacklist"
	"a.yandex-team.ru/travel/buses/backend/internal/api/offerstorage"
	"a.yandex-team.ru/travel/buses/backend/internal/common/connector"
	commonGRPC "a.yandex-team.ru/travel/buses/backend/internal/common/grpc"
	ytclient "a.yandex-team.ru/travel/buses/backend/internal/common/yt"
	workerApp "a.yandex-team.ru/travel/buses/backend/internal/worker/app"
	pb "a.yandex-team.ru/travel/buses/backend/proto"
	wpb "a.yandex-team.ru/travel/buses/backend/proto/worker"
)

func NewTestApp(t *testing.T, cfg *Config) (*App, func(), error) {

	if cfg == nil {
		cfg = &DefaultConfig
	}
	cp, err := copystructure.Copy(cfg)
	if err != nil {
		return nil, func() {}, fmt.Errorf("NewTestApp: %w", err)
	}

	testCfg := cp.(*Config)
	testCfg.Connector = connector.MockedConfig
	testCfg.Blacklist = blacklist.MockedConfig
	testCfg.Storage = resourcestorage.MockedS3StorageConfig
	testCfg.DictRasp.ResourceDir = "rasp_data"
	testCfg.LogbrokerConsumer.TestEnv = persqueueRecipe.New(t)
	testCfg.Billing.DictPath = yatest.SourcePath("travel/buses/settings/commerce/rates.yaml")
	testCfg.Worker = commonGRPC.MockedClientConfig
	testCfg.YtLockClient = ytclient.MockedYtClientConfig
	testCfg.OfferStorage = offerstorage.Config{Enabled: false}
	logger := zap.Logger{L: zaptest.NewLogger(t, zaptest.Level(uzap.ErrorLevel))}

	commonGRPC.InitBufConn()

	metricsRegistry := metrics.NewAppMetricsRegistryWithPrefix("app")
	app, err := NewApp(testCfg, &logger, metricsRegistry)
	if err != nil {
		return nil, func() {}, fmt.Errorf("NewTestApp: %w", err)
	}
	workerCfg := workerApp.DefaultConfig
	workerCfg.Connector = connector.MockedConfig
	workerCfg.Logbroker.TestEnv = persqueueRecipe.New(t)
	workerCfg.UnifiedAgentClient.Enabled = false

	wApp, err := workerApp.NewApp(&workerCfg, metricsRegistry, &logger)
	if err != nil {
		return nil, func() {}, fmt.Errorf("NewTestApp: %w", err)
	}
	err = wApp.Run()
	if err != nil {
		return nil, func() {}, fmt.Errorf("NewTestApp: %w", err)
	}

	err = app.Run()
	if err != nil {
		return nil, func() {}, fmt.Errorf("NewTestApp: %w", err)
	}

	grpcServer, err := commonGRPC.NewServer(&commonGRPC.MockedServerConfig, app.metricsRegistry, &logger)
	if err != nil {
		return nil, func() {}, fmt.Errorf("NewServer: %w", err)
	}
	wpb.RegisterWorkerServiceServer(grpcServer.GetCoreServer(), wApp)
	go func() {
		_ = grpcServer.Serve()
	}()

	return app, func() {
		app.Close()
		wApp.Close()
		grpcServer.Stop()
		commonGRPC.CloseBufConn()
		_ = logger.L.Sync()
		goleak.VerifyNone(t)
	}, err
}

func TestNewApp(t *testing.T) {
	app, appClose, err := NewTestApp(t, nil)
	if !assert.NoError(t, err) {
		return
	}
	defer appClose()

	status := app.ReloadResources()
	if !assert.Equal(t, pb.EStatus_STATUS_OK, status.Status) {
		return
	}
}

func setupLogsCapture() (*zap.Logger, *observer.ObservedLogs) {
	core, logs := observer.New(uzap.InfoLevel)
	logger := &zap.Logger{L: uzap.New(core)}
	return logger, logs
}
