package main

import (
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/clients"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/configs"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/util"
	"context"
	"fmt"
	"github.com/yandex/pandora/core"
	"go.uber.org/zap"
	"math/rand"
	"sync"
	"time"
)

var loyaltyClientMap = sync.Map{}

func NewLoyaltyGun(config configs.LoyaltyGunConfig) core.Gun {
	return &loyaltyGun{
		config: config,
	}
}

func NewLoyaltyProvider() core.Provider {
	return &loyaltyProvider{
		random:        rand.New(rand.NewSource(time.Now().UnixNano())),
		uidRange:      util.GetCoinsUIDRange(),
		currentAmmoID: 0,
		sink:          make(chan configs.LoyaltyAmmo),
	}
}

type loyaltyGun struct {
	config     configs.LoyaltyGunConfig
	client     clients.LoyaltyClient
	Aggregator core.Aggregator
	Deps       core.GunDeps
}

func (l *loyaltyGun) Bind(aggr core.Aggregator, deps core.GunDeps) (err error) {
	l.Aggregator = aggr
	l.Deps = deps
	if deps.InstanceID == 0 {
		tvmSecret := l.config.ClientDependencies.SecretResolver(l.config.TvmSecret)
		deps.Log.Info("secret", zap.String("tvm", tvmSecret))
		l.client = clients.NewLoyaltyClient(l.config.Environment, tvmSecret, clients.NewUnlimitedLimiter(), l.config.ClientDependencies)
		err = storeLoyaltyClient(l.config.ID, l.client)
		if err != nil {
			return err
		}
	} else {
		l.client, err = loadLoyaltyClient(l.config.ID)
		if err != nil {
			return err
		}
	}
	return nil
}

func (l *loyaltyGun) Shoot(ammo core.Ammo) {
	loyaltyAmmo := ammo.(configs.LoyaltyAmmo)
	l.shoot(loyaltyAmmo)
}

func (l *loyaltyGun) shoot(ammo configs.LoyaltyAmmo) {
	ctx := configs.NewShootContext(configs.AmmoMeta{
		ID:     ammo.ID,
		PoolID: l.config.ID,
	},
		l.Deps, l.Aggregator)
	coin, err := l.client.CreateCoin(ctx, l.config.PromoID, ammo.UID)
	logger := ctx.GetLogger()
	if err != nil {
		logger.Error("cannot create coin")
	} else {
		logger.Debug("created coin", zap.Int("coinID", coin))
	}
}

func storeLoyaltyClient(poolNum int, client clients.LoyaltyClient) error {
	_, loaded := loyaltyClientMap.LoadOrStore(poolNum, client)
	if loaded {
		return fmt.Errorf("client already initialized. gun.id is not unique: %v", poolNum)
	}
	return nil
}
func loadLoyaltyClient(poolNum int) (client clients.LoyaltyClient, err error) {
	stored, ok := loyaltyClientMap.Load(poolNum)
	if !ok {
		err = fmt.Errorf("cached client not found")
		return
	}
	client, ok = stored.(clients.LoyaltyClient)
	if !ok {
		err = fmt.Errorf("cached client is not LoyaltyClient")
	}
	return
}

type loyaltyProvider struct {
	sink          chan configs.LoyaltyAmmo
	uidRange      util.UIDSet
	currentAmmoID int
	random        *rand.Rand
}

func (l *loyaltyProvider) Run(ctx context.Context, deps core.ProviderDeps) error {
	defer close(l.sink)
	log := deps.Log
	for {
		ammo := configs.LoyaltyAmmo{
			ID:  l.currentAmmoID,
			UID: l.uidRange.GetUID(l.random),
		}
		l.currentAmmoID++
		select {
		case <-ctx.Done():
			fmt.Println("[INFO] [Provider] Stop generating ammo")
			return nil
		case l.sink <- ammo:
			log.Info("loyalty ammo created", zap.Reflect("ammo", ammo))
		}
	}
}

func (l *loyaltyProvider) Acquire() (ammo core.Ammo, ok bool) {
	ammo, ok = <-l.sink
	return
}

func (l *loyaltyProvider) Release(core.Ammo) {}
