package actions

import (
	"context"

	"github.com/stretchr/testify/suite"

	bconfig "a.yandex-team.ru/billing/library/go/billingo/pkg/config"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/extracontext"
	"a.yandex-team.ru/market/logistics/wms/robokotov/internal/commands"
	"a.yandex-team.ru/market/logistics/wms/robokotov/internal/core"
)

func setupContext() (commands.AppContext, error) {
	loader, _ := bconfig.PrepareLoader()
	config := core.Config{}

	ctx := commands.AppContext{
		Context: extracontext.NewWithParent(context.Background()),
	}

	err := loader.Load(ctx, &config)
	if err != nil {
		return ctx, err
	}

	return ctx, nil
}

type ActionTestSuite struct {
	suite.Suite
	ctx commands.AppContext
}

func (s *ActionTestSuite) SetupTest() {
	var err error

	s.ctx, err = setupContext()
	if err != nil {
		s.T().Fatal(err)
	}
}

func (s *ActionTestSuite) TearDownSuite() {
}
