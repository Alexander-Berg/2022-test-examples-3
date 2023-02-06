package main

import (
	"a.yandex-team.ru/library/go/yandex/tvm"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/abstractions"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/configs"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/util"
	"context"
	"github.com/stretchr/testify/require"
	"github.com/yandex/pandora/core"
	"go.uber.org/zap"
	"testing"
	"time"
)

type dummyAggregator struct {
}

func (d *dummyAggregator) Report(_ core.Sample) {

}

func (d *dummyAggregator) Run(_ context.Context, _ core.AggregatorDeps) error {
	return nil
}

type dummyYQL struct {
}

func (d dummyYQL) ExecuteQuery(query string, requestProcessor abstractions.RequestProcessor, ctx abstractions.ShootContext, token string) (*[]abstractions.ResultRow, error) {
	return &[]abstractions.ResultRow{}, nil
}

func (d dummyYQL) GetOperations(requestProcessor abstractions.RequestProcessor, ctx abstractions.ShootContext, token string) (*[]abstractions.OperationDto, error) {
	return &[]abstractions.OperationDto{}, nil
}

func (d dummyYQL) PostOperation(body []byte, requestProcessor abstractions.RequestProcessor, ctx abstractions.ShootContext, token string) (*abstractions.OperationDto, error) {
	return &abstractions.OperationDto{}, nil
}

func (d dummyYQL) GetOperation(id string, requestProcessor abstractions.RequestProcessor, ctx abstractions.ShootContext, token string) (*abstractions.OperationDto, error) {
	return &abstractions.OperationDto{}, nil
}

func (d dummyYQL) GetOperationData(id string, requestProcessor abstractions.RequestProcessor, ctx abstractions.ShootContext, token string) (*[]abstractions.ResultRow, error) {
	return &[]abstractions.ResultRow{}, nil
}

var id = 1

func TestGunBind(t *testing.T) {
	gunConfig := configs.GunConfig{ClientDependencies: &configs.ClientDependencies{
		CategoriesReader: func(file string) map[int]bool {
			return map[int]bool{}
		},
		TicketFunc: func(tvmSrcID tvm.ClientID, tvmDstID tvm.ClientID, secret string) func(r map[string]string) error {
			return func(r map[string]string) error {
				return nil
			}
		},
		SecretResolver: func(config util.SecretConfig) string {
			return ""
		},
		YQL: dummyYQL{},
	},
		CartRepeats:       1,
		Environment:       configs.Testing,
		ID:                &id,
		ShootingDelay:     5 * time.Second,
		StockStorageBatch: 1,
		StockStorageRetry: 10,
	}
	gun := GunInit(gunConfig)
	var aggregator core.Aggregator = &dummyAggregator{}
	err := gun.Bind(aggregator, core.GunDeps{
		Ctx:        context.Background(),
		Log:        zap.L(),
		InstanceID: 0,
	})
	// надо подумать как замокать RequestProcessor, чтоб протестировать взаимодействие
	// пока кажется, что можно рефлекцией разбирать поля или иметь замоканные ответы в виде строк и скармливать их json.Unmarshal
	require.NoError(t, err)
}
