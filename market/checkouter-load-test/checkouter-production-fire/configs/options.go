package configs

import (
	"a.yandex-team.ru/library/go/slices"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/abstractions"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/configs/stocktype"
	"context"
	"fmt"
	"github.com/yandex/pandora/core"
	"github.com/yandex/pandora/core/aggregator/netsample"
	"go.uber.org/zap"
	"math"
)

const (
	Testing    = "testing"
	Production = "production"
)

type GunInternal struct {
	Measure func(interface{})
}

type Options struct {
	ShopID           int
	Email            string
	DeliveryServices map[stocktype.StockType]func(int) bool
	SupplierID       int
	GunConfig
}

type SourceOffer struct {
	Name string
	Rate int
}

func (o *Options) GetCashbackRate() float64 {
	if o.InitCashbackStocks {
		return float64(o.PercentOfCashbackOrders) / 100
	}
	return 0
}

func (o *Options) IsAllowedSupplierType(supplierType string) bool {
	for _, t := range o.SupplierTypes {
		if t == supplierType {
			return true
		}
	}
	return false
}

func (o *Options) IsAllowedWarehouseID(warehouseID int, stockTypes ...stocktype.StockType) bool {
	for _, stockType := range stockTypes {
		warehouses := o.WarehouseIDs[stockType.String()]
		if len(warehouses) == 0 {
			return true
		}

		if ok, err := slices.Contains(warehouses, warehouseID); ok && err == nil {
			return true
		}
	}

	return false
}

func (o *Options) TotalOrdersForStockType(stockType stocktype.StockType) int {
	//TODO переписать на единый механихм с учетом еще и кэшебка и упростить при этом ValidateStocks и избавиться от requiredModifier
	var coef float64
	if stockType == stocktype.Fulfillment || stockType == stocktype.FulfillmentCashback {
		coef = float64(100-o.PercentOfDropshipOrders) / 100
	} else if stockType == stocktype.Dropship || stockType == stocktype.DropshipCashback {
		coef = float64(o.PercentOfDropshipOrders) / 100
	} else {
		panic(fmt.Errorf("unknown stock type when calculate total orders: '%v'", stockType.String()))
	}

	return int(math.Ceil(float64(o.TotalOrders) * coef))
}

var defaultServices = map[int]bool{
	9:   true,
	48:  true,
	50:  true,
	106: true,
	107: true,
}

var DefaultAddresses = []map[string]string{{
	"country":   "Россия",
	"postcode":  "119034",
	"city":      "Москва",
	"subway":    "Парк Культуры",
	"street":    "Льва Толстого",
	"house":     "18Б",
	"floor":     "2",
	"recipient": "000",
	"phone":     "+77777777777",
}}

const (
	DefaultShopID     = 431782
	DefaultEmail      = "checkouter-shooting@yandex-team.ru"
	DefaultSupplierID = 10264645
)

func DefaultDeliveryServices(serviceID int) bool {
	val, ok := defaultServices[serviceID]
	return ok && val
}

type CustomAmmo struct {
	ID           int
	UID          int
	ReadonlyUID  int
	UseCoins     bool
	UsePromo     bool
	Promocode    *string
	ShopPromoID  string
	Carts        int
	UseStockType stocktype.StockType
	Offers       int
	UseFlash     bool
}

func (ammo CustomAmmo) IsDropship() bool {
	return ammo.UseStockType == stocktype.Dropship || ammo.UseStockType == stocktype.DropshipCashback
}

type LoyaltyAmmo struct {
	ID  int
	UID int
}

func NewShootContext(meta AmmoMeta, gunDeps core.GunDeps, aggregator core.Aggregator) abstractions.ShootContext {
	return &shootContext{meta: &meta, aggregator: aggregator, GunDeps: gunDeps}
}

func NewGunInitContext(gunDeps core.GunDeps, aggregator core.Aggregator) abstractions.ShootContext {
	return &shootContext{aggregator: aggregator, GunDeps: gunDeps}
}

func (o *Options) MustHaveFulfillmentOrders() bool {
	return o.PercentOfDropshipOrders < 100
}

func (o *Options) MustHaveDropshipOrders() bool {
	return o.PercentOfDropshipOrders > 0
}

type shootContext struct {
	meta *AmmoMeta
	core.GunDeps
	aggregator core.Aggregator
}

func (c *shootContext) Acquire(tag string) *netsample.Sample {
	sample := netsample.Acquire(tag)
	if c.meta != nil {
		sample.SetID(c.meta.ID)
	} else {
		sample.SetID(0)
	}
	return sample
}

func (c *shootContext) WithLogCustomization(customizer func(logger *zap.Logger) *zap.Logger) abstractions.ShootContext {
	newContext := *c
	newContext.Log = customizer(c.Log)
	return &newContext
}

func (c *shootContext) Measure(sample core.Sample) {
	c.aggregator.Report(sample)
}

func (c *shootContext) WithContext(ctx context.Context) abstractions.ShootContext {
	newCtx := *c
	newCtx.Ctx = ctx
	return &newCtx
}

func (c *shootContext) GetLogFields() []zap.Field {
	if c.meta != nil {
		return []zap.Field{zap.Reflect("meta", *c.meta)}
	}
	return []zap.Field{}

}

func (c *shootContext) GetContext() context.Context {
	return c.Ctx
}

func (c *shootContext) GetLogger() *zap.Logger {
	return c.Log.With(c.GetLogFields()...)
}

type AmmoMeta struct {
	ID     int
	PoolID int
}
