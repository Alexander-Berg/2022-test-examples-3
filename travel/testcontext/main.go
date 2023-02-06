package main

import (
	"context"
	"fmt"
	"sync"

	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/maxprocs"
	"a.yandex-team.ru/travel/library/go/configuration"
	"a.yandex-team.ru/travel/library/go/metrics"

	"a.yandex-team.ru/travel/buses/backend/internal/common/grpc"
	"a.yandex-team.ru/travel/buses/backend/internal/common/logging"
	"a.yandex-team.ru/travel/buses/backend/internal/testcontext/app"
	"a.yandex-team.ru/travel/buses/backend/proto/testcontext"
	"a.yandex-team.ru/travel/library/go/grpcgateway"
)

type Config struct {
	App     *app.Config
	GRPC    *grpc.ServerConfig
	Gateway *grpcgateway.Config
	Logging *logging.Config
}

var cfg = Config{
	App:     &app.DefaultConfig,
	GRPC:    &grpc.DefaultServerConfig,
	Gateway: &grpcgateway.DefaultConfig,
	Logging: &logging.DefaultConfig,
}

const (
	swaggerJSONName = "testcontext_service.swagger.json"
	swaggerPrefix   = "/testcontext"
)

func main() {
	maxprocs.AdjustAuto()

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	loader := configuration.NewDefaultConfitaLoader()
	err := loader.Load(ctx, &cfg)
	if err != nil {
		fmt.Println("failed to load config:", err)
		return
	}
	logger, err := logging.New(cfg.Logging)
	if err != nil {
		logger.Error("failed to create logger", log.Error(err))
		return
	}

	metricsRegistry := metrics.NewRegistryWithDeployTagsAndExplicitHost()
	app, err := app.NewApp(cfg.App, metricsRegistry.WithPrefix("app"), logger)
	if err != nil {
		logger.Error("failed to create App", log.Error(err))
		return
	}
	if err = app.Run(); err != nil {
		logger.Error("failed to run App", log.Error(err))
		return
	}
	defer app.Close()

	grpcServer, err := grpc.NewServer(cfg.GRPC, metricsRegistry.WithPrefix("grpc"), logger)
	if err != nil {
		logger.Error("failed create grpc server", log.Error(err))
		return
	}
	testcontext.RegisterBusesTestContextServiceServer(grpcServer.GetCoreServer(), app)

	gw := grpcgateway.NewGateway(cfg.Gateway,
		grpcgateway.NewService(swaggerJSONName, swaggerPrefix, cfg.GRPC.Address, testcontext.RegisterBusesTestContextServiceHandlerFromEndpoint, nil))

	wg := sync.WaitGroup{}
	wg.Add(1)
	go func() {
		defer wg.Done()
		if err = grpcServer.Serve(); err != nil {
			logger.Fatal("filed to run GRPC server:", log.Error(err))
		}
	}()
	wg.Add(1)
	go func() {
		defer wg.Done()
		logger.Infof("Buses test context gateway starting at: %s", cfg.Gateway.Address)
		err = gw.Run(ctx)
		if err != nil {
			logger.Error(err.Error())
		}
	}()

	wg.Wait()
}
