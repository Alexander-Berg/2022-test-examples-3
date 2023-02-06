package main

import (
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/clients"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/configs"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/configs/stocktype"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/util"
	"github.com/yandex/pandora/cli"
	coreimport "github.com/yandex/pandora/core/import"
	"github.com/yandex/pandora/core/register"
	"net/url"
	"time"
)

func main() {
	fs := coreimport.GetFs()
	coreimport.Import(fs)

	register.Provider("main_provider", CustomAmmoProviderInit, func() CustomAmmoProviderConfig {

		return CustomAmmoProviderConfig{
			ReadonlyUIDs: []int{},
		}
	})
	register.Provider("loyalty_provider", NewLoyaltyProvider)
	// Поставил временное значение, для совместимости с существующими конфигами
	defaultID := 0
	dependencies := configs.ClientDependencies{
		RequestProcessor: clients.NewRequestProcessor(),
		TicketFunc:       util.GetAddTicketFunc,
		SecretResolver:   util.ResolveSecret,
		CategoriesReader: clients.ReadCashbackCategories,
		YQL:              &clients.YQLImpl{URL: url.URL{Scheme: "https", Host: "yql.yandex.net", Path: "api/v2"}},
	}
	register.Gun("loyalty_gun", NewLoyaltyGun, func() configs.LoyaltyGunConfig {
		return configs.LoyaltyGunConfig{
			ClientDependencies: &dependencies,
		}
	})
	register.Gun("main_gun", GunInit, func() configs.GunConfig {
		return configs.GunConfig{
			//
			ID:                              &defaultID,
			CartToReportDegradation:         true,
			UseIdxAPI:                       false,
			CartDurationSec:                 1,
			CartRepeats:                     1,
			LogChooseDeliveryOptionsInInit:  true,
			LogChooseDeliveryOptionsInShoot: false,
			ChooseOfferRepeats:              5,
			ChooseOfferThreshold:            20,
			//Чтобы первые подборы прошли успшено, в конце недели поправим в ЦУМе
			//ми можно будет изменить дефолтное значение
			OnlyInitStocks:     false,
			DoNotShootGo:       true,
			MaxWeight:          5.0,
			StocksRequiredRate: 1.0,
			StockStorageRetry:  3,
			StockStorageLimit:  1000,
			MaxRetriesForShoot: 3,
			RegionID:           213,
			ShipmentDay:        -1,
			WarehouseIDs: map[string][]int{
				stocktype.Dropship.String():            {145},
				stocktype.DropshipCashback.String():    {145},
				stocktype.Fulfillment.String():         {},
				stocktype.FulfillmentCashback.String(): {},
			},
			InitCashbackStocks:      false,
			PercentOfCashbackOrders: 0,
			FlashShopPromoID:        "",
			InitStocksAttempts:      3,
			StocksUpdateInterval:    time.Second,
			Environment:             configs.Testing,
			TvmSecret: util.SecretConfig{
				Type: util.Undefined,
			},
			YQLToken: util.SecretConfig{
				Type: util.Undefined,
			},
			ClientDependencies: &dependencies,
			ReportNumDoc:       60,
			SupplierTypes:      []string{"1"},
			StockStorageBatch:  15,
		}
	})

	cli.Run()
}
