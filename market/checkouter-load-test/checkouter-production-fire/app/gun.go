package main

import (
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/abstractions"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/clients"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/configs"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/configs/stocktype"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/flow"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/model"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/util"
	"context"
	"errors"
	"github.com/yandex/pandora/core/aggregator/netsample"
	"go.uber.org/zap"
	"sync"

	"fmt"
	"github.com/yandex/pandora/core"
	"math/rand"
	"time"
)

var clientMap = sync.Map{}
var stocksMap = sync.Map{}

type Gun struct {
	options    configs.Options
	random     *rand.Rand
	client     clients.ShootingClients
	Aggregator core.Aggregator
	Stocks     flow.Stocks
	core.GunDeps
}

func GunInit(config configs.GunConfig) *Gun {
	rand.Seed(time.Now().UnixNano())

	gun := Gun{
		options: configs.Options{
			GunConfig: config,
		},
		random: rand.New(rand.NewSource(time.Now().UnixNano())),
	}
	return &gun
}

func newClientOptions(config configs.GunConfig) clients.ClientOptions {
	handles := config.Handles

	return clients.ClientOptions{
		Environment:       config.Environment,
		CheckouterBaseURL: config.CheckouterBaseURL,
		TvmSecret:         config.TvmSecret,
		YqlSecret:         config.YQLToken,
		RpsLimiterOptions: clients.RpsLimiterOptions{
			RpsLimiterRate:     config.RpsLimiterRate,
			Rps:                config.Rps,
			CartRepeats:        config.CartRepeats,
			ChooseOfferRepeats: config.ChooseOfferRepeats,
			Handles:            handles,
		},
	}
}

func (g *Gun) Bind(aggr core.Aggregator, deps core.GunDeps) (err error) {
	defer func() {
		if r := recover(); r != nil {
			err = fmt.Errorf("unexpected bind error '%v'", r)
		}
	}()

	err = configs.ValidateGunConfig(g.options.GunConfig)
	if err != nil {
		return err
	}
	initConfig(&g.options, g.options.GunConfig)
	g.Aggregator = aggr
	g.GunDeps = deps

	if deps.InstanceID == 0 {
		after := time.After(g.options.ShootingDelay)
		g.client = clients.GetShootingClients(newClientOptions(g.options.GunConfig), g.options.ClientDependencies)
		err = storeClient(g.options.ID, g.client)
		if err != nil {
			return err
		}

		shootContext := configs.NewGunInitContext(deps, aggr).
			WithLogCustomization(func(logger *zap.Logger) *zap.Logger {
				return logger.Named("[InitStocks]")
			})

		g.Stocks, err = flow.InitStocks(g.client, shootContext, g.options, g.random)
		if err != nil {
			return err
		}
		err = storeStocks(g.options.ID, g.Stocks)
		if err != nil {
			return err
		}

		waited := make(chan bool, 1)
		go func() {
			for {
				time.Sleep(time.Second)
				select {
				case <-waited:
					return
				default:
					shootContext.GetLogger().Info("waiting")
					aggr.Report(netsample.Acquire("delayed"))
				}
			}
		}()
		<-after
		waited <- true
	} else {
		g.client, err = loadClient(g.options.ID)
		if err != nil {
			return err
		}
		g.Stocks, err = loadStocks(g.options.ID)
		if err != nil {
			return err
		}
	}
	if g.options.OnlyInitStocks {
		return fmt.Errorf("stocks initialized. quit")
	}
	return nil
}

func storeStocks(poolNum *int, stocks flow.Stocks) error {
	_, loaded := stocksMap.LoadOrStore(*poolNum, stocks)
	if loaded {
		return fmt.Errorf("stocks already initialized. gun.id is not unique: %v", poolNum)
	}
	return nil
}

func storeClient(poolNum *int, client clients.ShootingClients) error {
	if poolNum == nil {
		return fmt.Errorf("gun.id is undefined")
	}
	_, loaded := clientMap.LoadOrStore(*poolNum, client)
	if loaded {
		return fmt.Errorf("client already initialized. gun.id is not unique: %v", poolNum)
	}
	return nil
}

func loadStocks(poolNum *int) (stocks flow.Stocks, err error) {
	if poolNum == nil {
		err = fmt.Errorf("gun.id is undefined")
		return
	}
	stored, ok := stocksMap.Load(*poolNum)
	if !ok {
		err = fmt.Errorf("cached stocks not found")
		return
	}
	stocks, ok = stored.(flow.Stocks)
	if !ok {
		err = fmt.Errorf("cached stocks is not Stocks")
	}
	return
}

func loadClient(poolNum *int) (client clients.ShootingClients, err error) {
	if poolNum == nil {
		return nil, fmt.Errorf("gun.id is undefined")
	}
	stored, ok := clientMap.Load(*poolNum)
	if !ok {
		err = fmt.Errorf("cached client not found")
		return
	}
	client, ok = stored.(clients.ShootingClients)
	if !ok {
		err = fmt.Errorf("cached client is not ShootingClients")
	}
	return
}

func (g *Gun) Shoot(ammo core.Ammo) {
	st := time.Now()
	customAmmo := ammo.(configs.CustomAmmo)
	shootContext := configs.NewShootContext(configs.AmmoMeta{
		ID:     customAmmo.ID,
		PoolID: *g.options.ID,
	}, g.GunDeps, g.Aggregator).
		WithLogCustomization(func(logger *zap.Logger) *zap.Logger {
			return logger.Named("[Shoot]")
		})
	err := configs.ValidateAmmo(customAmmo, g.options)
	logger := shootContext.GetLogger()
	if err != nil {
		logger.Error("invalid ammo. stop shooting", zap.Error(err), zap.Reflect("ammo", ammo))
		panic(err)
	}

	defer func() {
		if r := recover(); r != nil {
			reportBlankShot(g.Aggregator, shootContext, fmt.Errorf("shooting panic %v", r))
			logger.Error("shooting panic", zap.Any("error", r))
		}
		logger.Info("shoot executed", zap.Int64("duration", time.Since(st).Milliseconds()))
	}()

	successfulShoot := false
	for idx := 0; idx < g.options.MaxRetriesForShoot; idx++ {
		err := g.shoot(customAmmo, shootContext)
		if err != nil {
			reportBlankShot(g.Aggregator, shootContext, err)
		} else {
			logger.Info("shoot successful", zap.Int("retryAmount", idx))
			successfulShoot = true
			break
		}
	}

	if !successfulShoot {
		fmt.Println("[ERROR] [Shoot] Cannot successful shoot")
	}
}

func reportBlankShot(aggregator core.Aggregator, shootContext abstractions.ShootContext, err error) {
	sample := shootContext.Acquire("blank shot")
	sample.SetErr(err)
	sample.SetProtoCode(599) // сделал так, чтобы холостая стрельба бросалась в глаза
	aggregator.Report(sample)
	shootContext.GetLogger().Info("blank shot", zap.Error(err))
}

func (g *Gun) shoot(ammo configs.CustomAmmo, shootContext abstractions.ShootContext) (err error) {
	logger := shootContext.GetLogger()
	defer func() {
		if r := recover(); r != nil {
			logger.Error("Attempt failed", zap.Any("error", r))
			err = fmt.Errorf("attempt failed %v", r)
		}
	}()
	unstoppableShootContext := shootContext.WithContext(context.Background())
	stocks := g.Stocks.GetStocks(ammo.UseStockType)
	if len(stocks) == 0 {
		panic(fmt.Errorf("stocks is empty for stock type: '%v'", ammo.UseStockType.String()))
	}

	fmt.Printf("[INFO] [Shoot] [%d] order configuration by order distribution:"+
		" internal carts:'%v', offers:'%v'\n", ammo.ID, ammo.Carts, ammo.Offers)

	if ammo.Carts == 0 && ammo.Offers == 0 {
		logger.Info("empty carts and offers")
		return fmt.Errorf("empty carts and offers")
	}

	if len(stocks) < ammo.Carts*ammo.Offers {
		logger.Info("there are not enough offers in stocks to create order with distribution",
			zap.Int("internalCarts", ammo.Carts), zap.Int("offers", ammo.Offers), zap.Int("stocks size", len(stocks)))
		return fmt.Errorf("there are not enough offers in stocks to create order with distribution")
	}

	offers, err := g.chooseOffers(shootContext, stocks, ammo.Carts, ammo.Offers)
	if err != nil {
		return err
	}

	var cart *clients.CartRootResponse = nil
	var newCart *clients.CartRootResponse
	var coinsForCart *clients.СoinsForCart
	var cartErr error

	for i := 0; i < g.options.CartRepeats; i++ {
		coinsForCart, err = flow.CoinsForCart(g.client, ammo, shootContext, g.options, offers, ammo.Carts, ammo.Offers)
		if err == nil {
			logger.Info("Successfully fetch coins for cart", zap.Int("ammoID", ammo.ID), zap.Int("step", i))
		} else {
			logger.Warn("Error when fetch coins for cart", zap.Int("ammoID", ammo.ID), zap.Int("step", i), zap.Any("error", err))
		}

		newCart, cartErr = flow.Cart(g.client, ammo, shootContext, g.options, offers, ammo.Carts, ammo.Offers, coinsForCart)

		if cartErr == nil {
			logger.Info("Update cart for checkout", zap.Int("ammoID", ammo.ID), zap.Int("step", i))
			cart = newCart
		} else {
			logger.Warn("Error when cart for checkout", zap.Int("ammoID", ammo.ID), zap.Int("step", i), zap.Any("error", cartErr))
		}

		time.Sleep(time.Second * time.Duration(g.options.CartDurationSec))
	}
	if cart == nil {
		logger.Error("cannot execute cart for checkout", zap.Error(cartErr))
		return cartErr
	}

	// Just a struct to provide address and get delivery
	var deliveryPayments []*clients.CheckoutParams
	for cartID, cartPart := range cart.Cart {
		if len(cartPart.DeliveryOptions) == 0 {
			return fmt.Errorf("no delivery options")
		}

		delivery, paymentMethod := flow.ChooseDelivery(ammo,
			g.options, g.options.Addresses[cartID], cartPart.DeliveryOptions)

		deliveryPayment := &clients.CheckoutParams{
			Address:       g.options.Addresses[cartID],
			Delivery:      delivery,
			PaymentMethod: paymentMethod,
		}

		deliveryPayments = append(deliveryPayments, deliveryPayment)
	}

	response := flow.Checkout(g.client, ammo, shootContext, g.options, deliveryPayments, cart, coinsForCart)

	if response.CheckedOut {
		for _, order := range response.Orders {
			orderID := order.ID
			if orderID == 0 {
				fmt.Printf("[WARN] [Shoot] [%d] orders with orderId = 0", ammo.ID)
				continue
			}
			err := flow.Unfreeze(g.client, unstoppableShootContext, g.options, orderID)
			if err != nil {
				fmt.Printf("[ERROR] [Shoot] [%d] Unexpected unfreeze error. orderId: %d, error: %v", ammo.ID, orderID, err)
			}
		}

		if response.Orders[0].ID != 0 {
			touchSomeHandles(g.client.GetCheckouter(), shootContext, ammo.ReadonlyUID, g.options, response.Orders[0].ID)
		}
	} else {
		logger.Warn("Order was not created at checkouter (checkedOut=false)", zap.Int("ammoID", ammo.ID))
		return errors.New("checkouter did not create order")
	}

	return nil
}

func (g *Gun) chooseOffers(shootContext abstractions.ShootContext, stocks []flow.Offer, cartsCount int, offersCount int) ([]clients.OfferInfo, error) {
	stocksCount := cartsCount * offersCount
	if stocksCount > len(stocks) {
		return nil, fmt.Errorf("attempt to choose stocksNumber more than is available")
	}

ChooseAttempt:
	for repeats := g.options.ChooseOfferRepeats; repeats > 0; repeats-- {
		/* To begin with get a random offer, then get next ones in series */

		offers, err := g.chooseStocks(stocks, stocksCount)
		if err != nil {
			continue ChooseAttempt
		}
		var ssItems []clients.SSItem
		for i := range offers {
			ssItems = append(ssItems, i)
		}
		amounts, err := g.client.GetStockStorage().GetAvailableAmounts(shootContext, ssItems)
		if err != nil {
			continue ChooseAttempt
		}
		var result []clients.OfferInfo
		for i := range amounts {
			if amounts[i].Amount < g.options.ChooseOfferThreshold {
				continue ChooseAttempt
			}
			result = append(result, offers[amounts[i].Item])
		}
		return result, nil
	}
	return nil, fmt.Errorf("couldnt choose offers with sufficient ammounts")

}

func (g *Gun) chooseStocks(stocks []flow.Offer, stocksNumber int) (map[clients.SSItem]clients.OfferInfo, error) {
	offerThreshold := g.options.ChooseOfferThreshold

	offers := map[clients.SSItem]clients.OfferInfo{}
	amounts := make([]int, len(stocks))
	totalAmounts := 0
	for i, stock := range stocks {
		quantity := stock.Quantity
		acceptableAmount := quantity - offerThreshold
		if acceptableAmount > 0 {

			amounts[i] = acceptableAmount
			totalAmounts += acceptableAmount
		}
	}
	for len(offers) < stocksNumber {
		if totalAmounts <= 0 {
			return nil, fmt.Errorf("total amount should be positive")
		}
		amountOffset := g.random.Intn(totalAmounts)
		idx, err := util.ChooseWeightedIndex(amountOffset, amounts)
		if err != nil {
			return nil, err
		}
		stock := stocks[idx]
		offers[stock.Stock] = stock.OfferInfo
		totalAmounts -= amounts[idx]
		amounts[idx] = 0
	}
	return offers, nil
}

func touchSomeHandles(client clients.CheckouterClient, shootContext abstractions.ShootContext, uid int, options configs.Options, orderID int) {
	var wg sync.WaitGroup
	for _, handle := range options.Handles {
		if handle.Active {
			wg.Add(1)
			go touchHandle(client, shootContext, &wg, &handle, uid, orderID)
		}
	}

	shootContext.GetLogger().Info("[Shoot] [func:'touchSomeHandles']: waiting for all goroutines to finish")
	wg.Wait()
	shootContext.GetLogger().Info("[Shoot] [func:'touchSomeHandles']: all goroutines finished")

	time.Sleep(time.Millisecond * time.Duration(options.HandlesCommonDelayMs))
}

func touchHandle(client clients.CheckouterClient, shootContext abstractions.ShootContext, wg *sync.WaitGroup, handle *model.Handle, uid int, orderID int) {
	defer func() {
		if r := recover(); r != nil {
			shootContext.GetLogger().Error("[Shoot] [Handle] error",
				zap.String("handle", string(handle.Name)),
				zap.Error(fmt.Errorf("touch error %v", r)))
		}
		wg.Done()
	}()

	delay, _ := handle.GetDelay()
	for i := 0; i < handle.Repeats; i++ {
		runFlow(client, shootContext, handle, uid, orderID)
		time.Sleep(delay)
	}
}

func runFlow(client clients.CheckouterClient, shootContext abstractions.ShootContext, handle *model.Handle, uid int, orderID int) {
	switch handle.Name {
	case model.RecentOrders:
		flow.RecentOrders(client, shootContext, handle.GetURL(), uid)
	case model.OrdersByUID:
		flow.OrdersByUID(client, shootContext, handle.GetURL(), uid)
	case model.OrdersOptionsAvailabilities:
		flow.OptionsAvailabilitiesOfOrders(client, shootContext, handle.GetURL(), orderID, uid)
	case model.GetOrders:
		flow.GetOrders(client, shootContext, handle.GetURL(), uid)
	case model.OrdersByID1:
		flow.GetOrdersByID1(client, shootContext, handle.GetURL(), orderID, uid)
	case model.OrdersByID2:
		flow.GetOrdersByID2(client, shootContext, handle.GetURL(), orderID)
	default:
		shootContext.GetLogger().Warn("[Shoot] flow handle name is undefined",
			zap.String("handle", string(handle.Name)))
	}
}

func initConfig(options *configs.Options, config configs.GunConfig) {
	//hardcode
	options.ShopID = configs.DefaultShopID
	options.Email = configs.DefaultEmail
	options.SupplierID = configs.DefaultSupplierID

	deliveryServices := map[stocktype.StockType]func(int) bool{}

	for key, deliveryService := range config.DeliveryServices {
		var fn func(int) bool
		if deliveryService == "all" {
			fn = func(int) bool { return true }
		} else if len(deliveryService) > 0 {
			fn = util.GenMatchFunc(deliveryService)
		} else {
			fn = configs.DefaultDeliveryServices
		}

		deliveryServices[stocktype.FromStockType(key)] = fn
	}

	options.DeliveryServices = deliveryServices
}
