package actions

import (
	"context"

	"github.com/stretchr/testify/suite"

	bconfig "a.yandex-team.ru/billing/library/go/billingo/pkg/config"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/extracontext"

	"a.yandex-team.ru/market/dev-exp/services/mdc-server/internal/commands"
	"a.yandex-team.ru/market/dev-exp/services/mdc-server/internal/core"
	"a.yandex-team.ru/market/dev-exp/services/mdc-server/internal/storage/db"
)

func setupContext() (commands.ConfigsContext, func(), error) {
	loader, _ := bconfig.PrepareLoader()
	config := core.AppConfig{}

	ctx := commands.ConfigsContext{
		Context: extracontext.NewWithParent(context.Background()),
	}

	err := loader.Load(ctx, &config)
	if err != nil {
		return ctx, nil, err
	}
	config.Storage.ReconnectRetries = 10

	ctx.Storage = db.NewStorage(config.Storage)
	err = ctx.Storage.Connect(ctx)
	if err != nil {
		return ctx, nil, err
	}

	return ctx, func() {
		_ = ctx.Storage.Disconnect(ctx)
	}, nil
}

type ActionTestSuite struct {
	suite.Suite
	ctx     commands.ConfigsContext
	cleanup func()
}

func (s *ActionTestSuite) SetupTest() {
	var err error

	s.ctx, s.cleanup, err = setupContext()
	if err != nil {
		s.T().Fatal(err)
	}
}

func (s *ActionTestSuite) TearDownSuite() {
	if s.cleanup != nil {
		s.cleanup()
	}
}
