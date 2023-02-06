package configs

import (
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/model"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/util"
	"net/url"
	"time"
)

type GunConfig struct {
	// уникальное поле, позволяет отличать пушку запущенную с одним конфигом от другой
	Addresses                       []map[string]string `validate:"required"`
	CartDurationSec                 int                 `validate:"gte=1"`
	CartRepeats                     int                 `validate:"gte=1"`
	CartToReportDegradation         bool
	CategoriesPerReportRequest      int
	CheckouterBaseURL               *url.URL
	ChooseOfferRepeats              int `validate:"gte=1"`
	ChooseOfferThreshold            int
	ClientDependencies              *ClientDependencies `config:"-"`
	DeliveryServices                map[string]string
	DeliveryType                    string
	Distribution                    map[string]float32
	DoNotShootGo                    bool
	Environment                     string
	FlashShopPromoID                string
	ForceUnfreeze                   bool
	Handles                         []model.Handle
	HandlesCommonDelayMs            int
	ID                              *int
	InitCashbackStocks              bool
	InitStocksAttempts              int
	LogChooseDeliveryOptionsInInit  bool
	LogChooseDeliveryOptionsInShoot bool
	MaxRetriesForShoot              int                       `validate:"gte=1"`
	MaxWeight                       float32                   `validate:"gt=0"`
	OffersDistribution              []model.OfferDistribution `validate:"required"`
	CartsDistribution               []model.CartDistribution
	OnlyInitStocks                  bool
	PercentOfCashbackOrders         int
	PercentOfDropshipOrders         int
	RegionID                        int
	ReportNumDoc                    int
	Rps                             int
	RpsLimiterRate                  float32
	ShipmentDay                     int
	ShootingDelay                   time.Duration `validate:"required"`
	ShopID                          int
	StockStorageLimit               int           `validate:"gte=1"`
	StockStorageRetry               int           `validate:"gte=1"`
	StocksUpdateInterval            time.Duration `validate:"gte=1000000000"`
	StocksRequiredRate              float64
	SupplierTypes                   []string
	TotalOrders                     int `validate:"gte=1"`
	TvmSecret                       util.SecretConfig
	UseIdxAPI                       bool
	WarehouseIDs                    map[string][]int
	YQLQueryID                      string
	YQLToken                        util.SecretConfig
	StockStorageBatch               int `validate:"gte=1"`
}

type LoyaltyGunConfig struct {
	ID                 int
	Environment        string
	TvmSecret          util.SecretConfig
	PromoID            int
	ClientDependencies *ClientDependencies `config:"-"`
}
