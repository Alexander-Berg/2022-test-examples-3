package app

import (
	"context"
	"fmt"
	"time"

	"a.yandex-team.ru/library/go/core/log/zap"
	"a.yandex-team.ru/library/go/core/metrics"
	travelMetrics "a.yandex-team.ru/travel/library/go/metrics"
	"a.yandex-team.ru/travel/library/go/resourcestorage"
	"a.yandex-team.ru/travel/library/go/vault"

	"a.yandex-team.ru/travel/buses/backend/internal/common/logging"
	ipb "a.yandex-team.ru/travel/buses/backend/internal/common/proto"
	"a.yandex-team.ru/travel/buses/backend/internal/testcontext/cache"
	tcpb "a.yandex-team.ru/travel/buses/backend/proto/testcontext"
)

type Config struct {
	DumpPeriod            time.Duration `config:"app-dumpperiod,required"`
	DumpKeepLastVersions  int           `config:"app-dumpkeeplastversions,required"`
	S3StorageSecret       string        `config:"app-s3storagesecret,required"`
	S3StorageAccessKeyKey string        `config:"app-s3storageaccesskeykey,required"`
	S3StorageSecretKey    string        `config:"app-s3storagesecretkey,required"`
	Storage               resourcestorage.S3StorageConfig
}

var DefaultConfig = Config{
	DumpPeriod:           1 * time.Hour,
	DumpKeepLastVersions: 3,
	Storage:              resourcestorage.DefaultS3StorageConfig,
}

const (
	moduleName                 = "app"
	testContextStorageResource = "testcontext_cache"
)

type App struct {
	tcpb.UnimplementedBusesTestContextServiceServer

	logger    *zap.Logger
	ctx       context.Context
	ctxCancel context.CancelFunc
	cfg       *Config

	appMetrics         *travelMetrics.AppMetrics
	testContextStorage *cache.TestContextStorage
	testContextDumper  *resourcestorage.Dumper
}

func NewApp(cfg *Config, metricsRegistry metrics.Registry, logger *zap.Logger) (*App, error) {
	const errorMessageFormat = "NewApp fails: %w"
	moduleLogger := logging.WithModuleContext(logger, moduleName)

	appMetrics := travelMetrics.NewAppMetrics(metricsRegistry)

	secretResolver := vault.NewYavSecretsResolver()

	testContextStorage := cache.NewTestContextStorage()

	var s3StorageAccessKey, s3StorageSecret string
	var err error
	if cfg.Storage != resourcestorage.MockedS3StorageConfig {
		s3StorageAccessKey, err = secretResolver.GetSecretValue(cfg.S3StorageSecret, cfg.S3StorageAccessKeyKey)
		if err != nil {
			return nil, fmt.Errorf(errorMessageFormat, err)
		}
		s3StorageSecret, err = secretResolver.GetSecretValue(cfg.S3StorageSecret, cfg.S3StorageSecretKey)
		if err != nil {
			return nil, fmt.Errorf(errorMessageFormat, err)
		}
	}

	testContextDumper := resourcestorage.NewDumper(
		testContextStorage, testContextStorageResource,
		resourcestorage.NewS3StorageWriter(cfg.Storage, s3StorageAccessKey, s3StorageSecret),
		cfg.DumpKeepLastVersions, moduleLogger)

	storageReader := resourcestorage.NewS3StorageReader(cfg.Storage, s3StorageAccessKey, s3StorageSecret)

	testContextStorageLoader := resourcestorage.NewLoader(&ipb.TTestContextCacheRecord{},
		testContextStorageResource, storageReader, moduleLogger)
	n, err := testContextStorageLoader.Load(testContextStorage)
	if err != nil {
		moduleLogger.Errorf("Can not load snapshot for %s: %s", testContextStorageResource, err.Error())
	} else {
		moduleLogger.Infof("Loaded %d records for %s", n, testContextStorageResource)
	}

	ctx, ctxCancel := context.WithCancel(context.Background())

	return &App{
		logger:    moduleLogger,
		ctx:       ctx,
		ctxCancel: ctxCancel,
		cfg:       cfg,

		appMetrics: appMetrics,

		testContextStorage: testContextStorage,
		testContextDumper:  testContextDumper,
	}, nil
}

func (a *App) Run() error {
	if a.testContextDumper != nil {
		a.testContextDumper.RunPeriodic(a.cfg.DumpPeriod, a.ctx)
	}
	return nil
}

func (a *App) Close() {
	a.ctxCancel()
}
