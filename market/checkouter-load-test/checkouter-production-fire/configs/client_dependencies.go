package configs

import (
	"a.yandex-team.ru/library/go/yandex/tvm"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/abstractions"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/util"
)

type ClientDependencies struct {
	RequestProcessor abstractions.RequestProcessor
	CategoriesReader util.CategoriesReader
	TicketFunc       func(tvmSrcID tvm.ClientID, tvmDstID tvm.ClientID, secret string) func(r map[string]string) error
	SecretResolver   func(config util.SecretConfig) string
	YQL              abstractions.YQL
}
