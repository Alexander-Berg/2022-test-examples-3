package flow

import (
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/abstractions"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/clients"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/configs"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/configs/stocktype"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/model"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/util"
	"errors"
	"fmt"
	"go.uber.org/zap"
	"golang.org/x/net/context"
	"math"
	"math/rand"
	"runtime/debug"
	"sort"
	"time"
)

type Offer struct {
	OfferInfo clients.OfferInfo
	Stock     clients.SSItem
	Quantity  int
}

func InitStocks(client clients.ShootingClients, shootContext abstractions.ShootContext, options configs.Options, random *rand.Rand) (stocks Stocks, err error) {
	logger := shootContext.GetLogger()
	logger.Info("Init stocks started")
	defer func() {
		if r := recover(); r != nil {
			logger.Info("Init stocks failed")
			sourceError, ok := r.(error)
			shootContext.GetLogger().Error(string(debug.Stack()))
			if ok {
				err = fmt.Errorf("stocks init failed %w", sourceError)
			} else {
				err = fmt.Errorf("stocks init failed %v", r)
			}
		} else {
			logger.Info("Init stocks finished")
		}
	}()

	stocksFF, cashbackStocksFF, stocksDropship, cashbackStocksDropship := initStocks0(client, shootContext, options, random)

	if options.MustHaveFulfillmentOrders() {
		err = ValidateStocks(options, false, stocksFF, cashbackStocksFF, true)
		if err != nil {
			panic(err)
		}
	}

	if options.MustHaveDropshipOrders() {
		err = ValidateStocks(options, true, stocksDropship, cashbackStocksDropship, true)
		if err != nil {
			panic(err)
		}
	}

	newStocks := NewStocks(
		map[stocktype.StockType][]Offer{
			stocktype.Fulfillment:         stocksFF,
			stocktype.FulfillmentCashback: cashbackStocksFF,
			stocktype.Dropship:            stocksDropship,
			stocktype.DropshipCashback:    cashbackStocksDropship,
		})
	startStocksUpdating(client, shootContext, options, newStocks)
	return newStocks, nil
}

func startStocksUpdating(client clients.ShootingClients, shootContext abstractions.ShootContext, options configs.Options, stocks WarmedUpStocks) {
	stockStorageClient := client.GetStockStorage()
	printStocksStats(stocks, shootContext)
	go func() {
		for {
			timer := time.NewTimer(options.GunConfig.StocksUpdateInterval)
			select {
			case <-timer.C:
				updatedStocksMap := getUpdatedStocksMap(stocks, stockStorageClient, shootContext, options.GunConfig.StockStorageBatch)
				stocks.UpdateStocks(updatedStocksMap)
				printStocksStats(stocks, shootContext)
			case <-shootContext.GetContext().Done():
				timer.Stop()
				return
			}
		}
	}()
}

func getUpdatedStocksMap(stocks Stocks, stockStorageClient clients.StockStorageClient, shootContext abstractions.ShootContext, batchSize int) map[stocktype.StockType][]Offer {
	stockTypes := stocks.GetStocksTypes()
	updatedStocksMap := make(map[stocktype.StockType][]Offer, len(stockTypes))
	for _, stockType := range stockTypes {
		offers := stocks.GetStocks(stockType)
		updatedStocksMap[stockType] = updateOffersAmounts(offers, stockStorageClient, shootContext, batchSize)
	}
	return updatedStocksMap
}

func updateOffersAmounts(offers []Offer, stockStorageClient clients.StockStorageClient, shootContext abstractions.ShootContext, batchSize int) []Offer {
	logger := shootContext.GetLogger()
	offersCount := len(offers)
	newOffers := make([]Offer, 0, offersCount)
	for _, batch := range splitOffers(offers, batchSize) {
		stocksBatch := convertToAmountsRequest(batch)
		amounts, err := stockStorageClient.GetAvailableAmounts(shootContext, stocksBatch)
		if err != nil {
			logger.Warn("unexpected error while batch amount update",
				zap.Error(err),
			)
			for _, offer := range batch {
				newOffers = append(newOffers, updateSingleOfferAmount(stockStorageClient, shootContext, offer, logger))
			}
		} else {
			newOffers = processAmountResponse(batch, amounts, newOffers, logger)
		}
	}
	return newOffers
}

func splitOffers(offers []Offer, batchSize int) [][]Offer {
	var offersCount = len(offers)
	var result = make([][]Offer, 0, offersCount/batchSize+1)
	for offersOffset := 0; offersOffset < offersCount; offersOffset += batchSize {
		batch := offers[offersOffset:util.Min(offersCount, offersOffset+batchSize)]
		result = append(result, batch)
	}
	return result
}

func processAmountResponse(batch []Offer, amounts []clients.SSItemAmount, newOffers []Offer, logger *zap.Logger) []Offer {
	stocksMap := convertToSSMap(batch)
	for _, amount := range amounts {
		ssItem := amount.Item
		info, ok := stocksMap[ssItem]
		if ok {
			delete(stocksMap, ssItem)
			newOffers = append(newOffers, Offer{
				OfferInfo: info,
				Stock:     ssItem,
				Quantity:  amount.Amount,
			})
		} else {
			logger.Error("unknown ssItem", zap.Reflect("ssItem", ssItem))
		}
	}
	for ssItem, info := range stocksMap {
		logger.Warn("missed ssItem", zap.Reflect("ssItem", ssItem))
		newOffers = append(newOffers, Offer{
			OfferInfo: info,
			Stock:     ssItem,
			Quantity:  0,
		})
	}
	return newOffers
}

func updateSingleOfferAmount(stockStorageClient clients.StockStorageClient, shootContext abstractions.ShootContext, offer Offer, logger *zap.Logger) Offer {
	availableAmounts, err := stockStorageClient.GetAvailableAmounts(shootContext, []clients.SSItem{offer.Stock})
	if err != nil {
		logger.Error("unexpected error while amount update",
			zap.Reflect("offer", offer),
			zap.Error(err),
		)
		return withAmount(offer, 0)
	} else if len(availableAmounts) != 1 {
		logger.Error("unexpected quantity while amount update",
			zap.Reflect("offer", offer),
			zap.Reflect("availableAmounts", availableAmounts),
		)
		return withAmount(offer, 0)
	} else {
		return withAmount(offer, availableAmounts[0].Amount)
	}
}

func withAmount(offer Offer, amount int) Offer {
	offer.Quantity = amount
	return offer
}

func convertToAmountsRequest(batch []Offer) []clients.SSItem {
	batchSize := len(batch)
	ssItems := make([]clients.SSItem, 0, batchSize)
	for _, offer := range batch {
		ssItems = append(ssItems, offer.Stock)
	}
	return ssItems
}

func convertToSSMap(batch []Offer) map[clients.SSItem]clients.OfferInfo {
	batchSize := len(batch)
	stocksMap := make(map[clients.SSItem]clients.OfferInfo, batchSize)
	for _, offer := range batch {
		stocksMap[offer.Stock] = offer.OfferInfo
	}
	return stocksMap
}

func printStocksStats(stocks Stocks, shootContext abstractions.ShootContext) {
	stockTypes := stocks.GetStocksTypes()
	for _, t := range stockTypes {
		offers := stocks.GetStocks(t)
		quantity := 0
		for _, offer := range offers {
			quantity += offer.Quantity
		}
		shootContext.GetLogger().Info("stocks",
			zap.Int("quantity", quantity),
			zap.Int("stocks", len(offers)),
			zap.String("type", t.String()),
		)
	}
}

func validateStocksHaveEnoughQuantity(options configs.Options, stocks []Offer, totalOrders int, verbose bool) error {
	var quantities []int
	for _, offer := range stocks {
		if offer.Quantity >= options.ChooseOfferThreshold {
			quantities = append(quantities, offer.Quantity-options.ChooseOfferThreshold+1)
		}
	}
	sort.Ints(quantities)

	for _, oDist := range options.OffersDistribution {
		for _, cDist := range options.CartsDistribution {
			multiOrdersRequired := int(math.Round(
				float64(totalOrders) *
					float64(oDist.OrdersDistribution) * float64(cDist.OrdersDistribution) /
					float64(cDist.InternalCarts) * options.StocksRequiredRate))
			if verbose {
				fmt.Printf("[TRACE] [InitStocks] validate offers distribution: orders=%v; carts=%v; required=%v; quantities=%v\n",
					oDist, cDist, multiOrdersRequired, quantities[0:util.Min(len(quantities), 5)])
			}
			for multiOrdersRequired > 0 {
				if cDist.InternalCarts > len(quantities) {
					return errors.New("stocks have not enough quantities")
				}
				orders := util.Min(multiOrdersRequired, quantities[0]/oDist.OffersCount)
				multiOrdersRequired -= orders
				for i := 0; i < cDist.InternalCarts; i++ {
					quantities[i] -= orders * oDist.OffersCount
				}
				for len(quantities) > 0 && quantities[0] < oDist.OffersCount {
					quantities = quantities[1:]
				}
			}
		}
	}
	return nil
}

func ValidateStocks(options configs.Options, isDropship bool, nonCbStocks []Offer, cbStocks []Offer, verbose bool) error {
	var usualStockType stocktype.StockType
	var cashbackStockType stocktype.StockType

	if isDropship {
		usualStockType = stocktype.Dropship
		cashbackStockType = stocktype.DropshipCashback
	} else {
		usualStockType = stocktype.Fulfillment
		cashbackStockType = stocktype.FulfillmentCashback
	}

	usualTotalOrders := int(float64(options.TotalOrdersForStockType(usualStockType)) * (1.0 - options.GetCashbackRate()))
	cashbackTotalOrders := int(float64(options.TotalOrdersForStockType(cashbackStockType)) * options.GetCashbackRate())

	err := validateStocksHaveEnoughQuantity(options, nonCbStocks, usualTotalOrders, verbose)
	if err != nil {
		return fmt.Errorf("%v for stockType: '%v'", err, usualStockType.String())
	}
	err = validateStocksHaveEnoughQuantity(options, cbStocks, cashbackTotalOrders, verbose)
	if err != nil {
		return fmt.Errorf("%v for stockType: '%v'", err, cashbackStockType.String())
	}
	return nil
}

func initStock(client clients.ShootingClients, shootContext abstractions.ShootContext, options configs.Options,
	stock clients.Stock, ammoID int, isDropship bool, cbCategories map[int]bool, random *rand.Rand) (*clients.OfferInfo, bool, error) {
	offer, err := getOffer(client, shootContext, options, isDropship, stock)
	if err != nil {
		return nil, false, err
	}
	if offer == nil {
		return nil, false, nil
	}

	_, cashback := cbCategories[offer.CategoryID]
	cbLogField := zap.Bool("cashback", cashback)
	if !options.IsAllowedSupplierType(offer.SupplierType) {
		stockLogger(stock, offer, true).With(cbLogField).
			Info(fmt.Sprintf("supplierType type must be one of: %v", options.SupplierTypes))
		return nil, cashback, nil
	}
	uid := util.GetShootingUIDRange().GetUID(random)
	ammo := configs.CustomAmmo{ID: ammoID, UID: uid}
	cart, err := Cart(client, ammo, shootContext, options, []clients.OfferInfo{*offer}, 1, 1, nil)
	if err != nil {
		return offer, cashback, err
	}

	if len(cart.Cart) == 0 {
		stockLogger(stock, offer, true).With(cbLogField).Warn("cart is empty")
		return nil, cashback, nil
	}
	if len(cart.Cart[0].DeliveryOptions) == 0 {
		stockLogger(stock, offer, true).With(cbLogField).Warn("no delivery options")
		return nil, cashback, nil
	}

	delivery, _ := ChooseDelivery(ammo, options, options.Addresses[0], cart.Cart[0].DeliveryOptions)
	if len(delivery.ID) == 0 {
		stockLogger(stock, offer, true).With(cbLogField).Warn("delivery is empty")
		return nil, cashback, nil
	}

	stockLogger(stock, offer, false).With(cbLogField).Info("OK")
	return offer, cashback, nil
}

func initStocks0(client clients.ShootingClients, shootContext abstractions.ShootContext, options configs.Options,
	random *rand.Rand) ([]Offer, []Offer, []Offer, []Offer) {

	var nonCbOffersReport, cbOffersReport []Offer

	if options.MustHaveFulfillmentOrders() {
		stocksReport, stopStocksRequestsReport := getStocksFromReport(client, shootContext, options)
		nonCbOffersReport, cbOffersReport = initOffers(client, shootContext, options, random, stocksReport, false, stopStocksRequestsReport)
	} else {
		nonCbOffersReport, cbOffersReport = []Offer{}, []Offer{}
	}

	var nonCbOffersYQL, cbOffersYQL []Offer

	if options.MustHaveDropshipOrders() {
		stocksYQL, stopStocksRequestsYQL := getStocksFromYQL(client, shootContext, options)
		nonCbOffersYQL, cbOffersYQL = initOffers(client, shootContext, options, random, stocksYQL, true, stopStocksRequestsYQL)
	} else {
		nonCbOffersYQL, cbOffersYQL = []Offer{}, []Offer{}
	}

	//TODO почему тут только nonCb
	shootContext.GetLogger().Info("Stocks filtered", zap.Int("filtered_count", len(nonCbOffersReport)+len(nonCbOffersYQL)))

	return nonCbOffersReport, cbOffersReport, nonCbOffersYQL, cbOffersYQL
}

func initOffers(client clients.ShootingClients, shootContext abstractions.ShootContext, options configs.Options,
	random *rand.Rand, stocks chan clients.GetStockResult, isDropship bool, stopStocksRequests context.CancelFunc) ([]Offer, []Offer) {
	var nonCbOffers, cbOffers []Offer
	cbCategories := client.GetReport().ReadCashbackCategories()
	ammoID := 0
	for stockResult := range stocks {
		err := ValidateStocks(options, isDropship, nonCbOffers, cbOffers, false)
		if err == nil {
			break
		}
		ammoID--
		stock, offer, err := stockResult.Unwrap()
		if err != nil {
			stockLogger(*stock, offer, true).Error(err.Error())
			continue
		}
		offer, cashback, err := initStock(client, shootContext, options, *stock, ammoID, isDropship, cbCategories, random)
		for attempt := 0; err != nil && attempt < options.InitStocksAttempts; attempt++ {

			offer, cashback, err = initStock(client, shootContext, options, *stock, ammoID, isDropship, cbCategories, random)
		}

		if err != nil {
			panic(err)
		}
		if offer == nil {
			continue
		}
		offerItem := Offer{
			OfferInfo: *offer,
			Stock:     stock.SSItem,
			Quantity:  stock.Quantity,
		}
		if cashback && options.InitCashbackStocks {
			cbOffers = append(cbOffers, offerItem)
		} else {
			nonCbOffers = append(nonCbOffers, offerItem)
		}
	}
	stopStocksRequests()

	return nonCbOffers, cbOffers
}

func stockLogger(stock clients.Stock, offer *clients.OfferInfo, filtered bool) *zap.Logger {
	state := zap.String("state", "used")
	if filtered {
		state = zap.String("state", "filtered")
	}
	var fields = []zap.Field{
		zap.String("sku", stock.Sku),
		zap.Int("quantity", stock.Quantity),
		zap.String("shopSku", stock.ShopSku),
		zap.Int("supplierID", stock.SupplierID),
		zap.Int("warehouseId", stock.WarehouseID),
		state,
	}
	var result = clients.GetStocksLogger().With(fields...)
	if offer != nil {
		result = result.With(
			zap.String("offerId", offer.OfferID),
			zap.Int("feedId", offer.FeedID),
			zap.String("showInfo", offer.ShowInfo),
			zap.Int("category", offer.CategoryID),
			zap.String("supplierType", offer.SupplierType),
		)
	}
	return result
}

func getOffer(client clients.ShootingClients, shootContext abstractions.ShootContext,
	options configs.Options, isDropship bool, stock clients.Stock) (*clients.OfferInfo, error) {
	if !options.UseIdxAPI {
		feedID, err := GetFeedID(client, shootContext, options, stock.SupplierID, isDropship, stock)
		if err != nil {
			return nil, err
		}
		if feedID <= 0 {
			stockLogger(stock, nil, true).Warn("FeedID less than zero")
			return nil, nil
		}
		return GetOfferInfo(client, shootContext, options, MakeShofferID(feedID, stock.Sku), stock)
	} else {
		return GetOfferFromIndex(client, shootContext, options, stock)
	}
}

func InitOrdersDistribution(totalOrders int, cartsDist []model.CartDistribution, offersDist []model.OfferDistribution) *model.GlobalOrdersDistribution {
	fmt.Printf("[INFO] [InitStocks] Init orders distribution started\n")
	globalOrdersDistribution := &model.GlobalOrdersDistribution{
		Distributions: initOrdersDistribution(totalOrders, cartsDist, offersDist),
	}
	fmt.Printf("[INFO] [InitStocks] Init orders distribution finished\n")
	for _, gd := range globalOrdersDistribution.Distributions {
		fmt.Printf("orders distribution {cart: '%v', offers: '%v', checkouts: '%v'}\n", gd.Carts, gd.Offers, gd.Checkouts)
	}
	return globalOrdersDistribution
}

func initOrdersDistribution(totalOrders int, cartsDist []model.CartDistribution, offersDist []model.OfferDistribution) []*model.OrdersDistribution {
	var fullOrdersDistribution []*model.OrdersDistribution
	var ordersDistribution []*model.OrdersDistribution
	/* According to carts distributions preparing orders*/
	/* Here we could lost some orders. Because of rounding.
	   E.g. When distribution says we need 3 multi from 10 orders.
	   We could make only 3*3 = 9. So we will lost no more orders than multi index (3) (multi index = internal carts) */

	/*
			Input: Checkouts: 100000; cartsDist:[{carts:1, percent:0.8}, {carts:2, percent:0.2}]
		    Output: orderDist:[{carts:1, orders: 80000}, {carts:2, orders: 10000}]
	*/
	for _, cd := range cartsDist {
		checkouts := math.Ceil(float64(cd.OrdersDistribution) * float64(totalOrders) / float64(cd.InternalCarts))
		ordersDistribution = append(ordersDistribution, &model.OrdersDistribution{
			Checkouts:    int(checkouts),
			Carts:        cd.InternalCarts,
			Distribution: cd.OrdersDistribution / float32(cd.InternalCarts),
		})
	}

	/*
	   Input: orderDist:[{carts:1, orders: 80000}, {carts:2, orders: 20000}]
	          offerDist:[{offers:1, percent:0.8}, {offers:2, percent:0.2}]

	   Output: orderDist:[
	              {carts:1, offers:1, orders: 64000},
	              {carts:1, offers:2, orders: 16000},
	              {carts:2, offers:1, orders: 16000},
	              {carts:2, offers:2, orders: 4000}
	           ]
	*/
	for _, orderDist := range ordersDistribution {
		for _, offerDist := range offersDist {
			orders := int(float32(orderDist.Checkouts) * offerDist.OrdersDistribution)

			fullOrdersDistribution = append(fullOrdersDistribution, &model.OrdersDistribution{
				Carts:        orderDist.Carts,
				Offers:       offerDist.OffersCount,
				Checkouts:    orders,
				Distribution: offerDist.OrdersDistribution * orderDist.Distribution,
			})
		}
	}

	return fullOrdersDistribution
}
