package main

import (
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/configs"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/configs/stocktype"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/flow"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/model"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/util"
	"context"
	"fmt"
	"github.com/yandex/pandora/core"
	"go.uber.org/zap"
	"math/rand"
	"time"
)

func CustomAmmoProviderInit(config CustomAmmoProviderConfig) *UIDProvider {
	return &UIDProvider{
		random:                     rand.New(rand.NewSource(time.Now().UnixNano())),
		currentAmmoID:              0,
		uidRange:                   util.GetShootingUIDRange(),
		coinOwnerUIDRange:          util.GetCoinsUIDRange(),
		percentOfOrdersPaidByCoins: config.PercentOfOrdersPaidByCoins,
		readonlyUIDRange:           config.getReadonlyRange(),
		percentOfOrdersUsingPromo:  config.PercentOfOrdersUsingPromo,
		percentOfCashbackOrders:    config.PercentOfCashbackOrders,
		percentOfDropshipOrders:    config.PercentOfDropshipOrders,
		promocodes:                 config.Promocodes,
		percentOfFlashOrders:       config.PercentOfFlashOrders,
		flashShopPromoID:           config.FlashShopPromoID,
		sink:                       make(chan configs.CustomAmmo),
		preShootAmmoType:           config.PreShootAmmoType,
		CartsDistribution:          config.CartsDistribution,
		OffersDistribution:         config.OffersDistribution,
	}
}

type CustomAmmoProviderConfig struct {
	PercentOfOrdersPaidByCoins int
	PercentOfOrdersUsingPromo  int
	ReadonlyUIDs               util.UIDArray
	Promocodes                 []string
	PercentOfCashbackOrders    int
	PercentOfDropshipOrders    int
	PercentOfFlashOrders       int
	FlashShopPromoID           string
	PreShootAmmoType           bool
	CartsDistribution          []model.CartDistribution
	OffersDistribution         []model.OfferDistribution
}

type UIDProvider struct {
	currentAmmoID              int
	uidRange                   util.UIDSet
	coinOwnerUIDRange          util.UIDSet
	percentOfOrdersPaidByCoins int
	readonlyUIDRange           util.UIDSet
	sink                       chan configs.CustomAmmo
	percentOfOrdersUsingPromo  int
	percentOfCashbackOrders    int
	percentOfDropshipOrders    int
	promocodes                 []string
	percentOfFlashOrders       int
	flashShopPromoID           string
	preShootAmmoType           bool
	CartsDistribution          []model.CartDistribution
	OffersDistribution         []model.OfferDistribution
	random                     *rand.Rand
}

func (config *CustomAmmoProviderConfig) getReadonlyRange() util.UIDSet {
	if len(config.ReadonlyUIDs) > 0 {
		return config.ReadonlyUIDs
	}

	return nil
}

func (u *UIDProvider) Run(ctx context.Context, deps core.ProviderDeps) error {
	defer close(u.sink)
	log := deps.Log
	hasReadonlyUIDRange := u.readonlyUIDRange != nil
	globalOrdersDistribution := flow.InitOrdersDistribution(100_000, u.CartsDistribution, u.OffersDistribution)
	for {
		var ammo configs.CustomAmmo
		if u.preShootAmmoType {
			ammo = u.toPreShootAmmo(hasReadonlyUIDRange)
		} else {
			carts, offers := globalOrdersDistribution.GetCartsAndOffers()
			ammo = u.toCustomAmmo(carts, offers, u.usePromo(), u.useCoins(), u.useFlash(), u.useStockType(), hasReadonlyUIDRange)
		}
		select {
		case <-ctx.Done():
			fmt.Println("[INFO] [Provider] Stop generating ammo")
			return nil
		case u.sink <- ammo:
			log.Info("shooting ammo created", zap.Reflect("ammo", ammo))
		}
	}
}

func (u *UIDProvider) toPreShootAmmo(hasReadonlyUIDRange bool) configs.CustomAmmo {
	n1 := u.currentAmmoID / 2
	n2 := n1 / 2
	n3 := n2 / 2
	n4 := n3 / 2
	n5 := n4 / 2
	return u.toCustomAmmo(n5%2+1, n4%2+1, n3%2 == 1, n2%2 == 1, n1%2 == 1, stocktype.StockType(u.currentAmmoID%2), hasReadonlyUIDRange)
}

func (u *UIDProvider) toCustomAmmo(carts int, offers int, usePromo bool, useCoins bool, useFlash bool, useStockType stocktype.StockType, hasReadonlyUIDRange bool) configs.CustomAmmo {
	var code *string
	if usePromo {
		code = &(u.promocodes[u.currentAmmoID%len(u.promocodes)])
	}
	uid := u.uidRange.GetUID(u.random)
	if useCoins {
		uid = u.coinOwnerUIDRange.GetUID(u.random)
	}
	readonlyUID := uid
	if hasReadonlyUIDRange {
		readonlyUID = u.readonlyUIDRange.GetUID(u.random)
	}

	var shopPromoID string

	if useFlash {
		shopPromoID = u.flashShopPromoID
	} else {
		shopPromoID = ""
	}
	u.currentAmmoID++

	return configs.CustomAmmo{
		ID:           u.currentAmmoID,
		UID:          uid,
		ReadonlyUID:  readonlyUID,
		UseCoins:     useCoins,
		UsePromo:     usePromo,
		UseStockType: useStockType,
		Promocode:    code,
		ShopPromoID:  shopPromoID,
		Carts:        carts,
		Offers:       offers,
	}
}

func (u *UIDProvider) useCoins() bool {
	return u.isIntoPercentage(u.percentOfOrdersPaidByCoins)
}

func (u *UIDProvider) isIntoPercentage(percent int) bool {
	return u.random.Intn(100) < percent
}

func (u *UIDProvider) usePromo() bool {
	return u.isIntoPercentage(u.percentOfOrdersUsingPromo)
}

func (u *UIDProvider) useStockType() stocktype.StockType {
	/*TODO будем не честно прогружать так как пока нет кэшбека для не дропшипов по нашим товарам
	распределение на кэшбек указывает долю таких заказов среди fulfillment товаров
	*/
	isDropship := u.isIntoPercentage(u.percentOfDropshipOrders)
	isCashback := u.isIntoPercentage(u.percentOfCashbackOrders)

	if isDropship && isCashback {
		return stocktype.DropshipCashback
	} else if isDropship && !isCashback {
		return stocktype.Dropship
	} else if !isDropship && isCashback {
		return stocktype.FulfillmentCashback
	} else {
		return stocktype.Fulfillment
	}
}

func (u *UIDProvider) useFlash() bool {
	return u.isIntoPercentage(u.percentOfFlashOrders)
}

func (u *UIDProvider) Acquire() (a core.Ammo, ok bool) {
	a, ok = <-u.sink
	return
}

func (u *UIDProvider) Release(core.Ammo) {}
