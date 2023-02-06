package flow

import (
	"a.yandex-team.ru/library/go/slices"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/abstractions"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/clients"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/configs"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/configs/stocktype"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/model"
	"context"
	"fmt"
	"go.uber.org/zap"
	"net/url"
	"strconv"
)

func getStocksFromReport(client clients.ShootingClients, shootContext abstractions.ShootContext, options configs.Options) (chan clients.GetStockResult, context.CancelFunc) {
	stocks, stopStocksRequests := client.GetReport().GetStocks(shootContext, options)
	filtered := make(chan clients.GetStockResult, cap(stocks))
	go func() {
		defer close(filtered)
		for stock := range stocks {
			if stock.Stock == nil {
				continue
			}
			if stock.Err != nil {
				stockLogger(*stock.Stock, stock.Offer, true).Warn(stock.Err.Error())
				continue
			}
			if !options.IsAllowedWarehouseID(stock.Stock.WarehouseID, stocktype.Fulfillment, stocktype.FulfillmentCashback) {
				stockLogger(*stock.Stock, stock.Offer, true).Warn("not suitable warehouseId")
				continue
			}
			amounts, err := client.GetStockStorage().
				GetAvailableAmounts(shootContext, []clients.SSItem{stock.Stock.SSItem})
			if err != nil {
				panic(err)
			}
			if len(amounts) == 0 {
				stockLogger(*stock.Stock, stock.Offer, true).Warn("empty amounts")
				continue
			}
			if amounts[0].Amount < options.ChooseOfferThreshold {
				stockLogger(*stock.Stock, stock.Offer, true).With(zap.Int("quantity", amounts[0].Amount)).
					Warn("quantity is less than threshold")
				continue
			}
			stock.Stock.Quantity = amounts[0].Amount
			filtered <- stock
		}
	}()

	return filtered, stopStocksRequests
}

func getStocksFromYQL(client clients.ShootingClients, shootContext abstractions.ShootContext, options configs.Options) (chan clients.GetStockResult, context.CancelFunc) {
	resp, err := client.GetYQLStocksClient().GetStocks(shootContext, options)
	if err != nil {
		panic(fmt.Errorf("cannot execute getStocks: '%v'", err))
	}
	return clients.StocksArrayToGetStockResult(resp.Stocks), func() {}
}

func GetFeedID(client clients.ShootingClients, shootContext abstractions.ShootContext, options configs.Options,
	supplierID int, isDropship bool, stock clients.Stock) (int, error) {
	reportResult, err := client.GetReport().GetShopInfo(shootContext, supplierID)
	if err != nil {
		return -1, err
	}
	return ExtractFeedIDFromReport(options, supplierID, isDropship, reportResult.ReportShops, stock), nil
}

func GetOfferInfo(client clients.ShootingClients, shootContext abstractions.ShootContext, options configs.Options, feshofferID string, stock clients.Stock) (*clients.OfferInfo, error) {
	resp, err := client.GetReport().GetOfferInfo(shootContext, options, feshofferID)

	if err != nil {
		return nil, fmt.Errorf("cannot execute offer_info: '%v'", err)
	}

	return ConvertStockOfferFromReport(options, resp.ReportSearch, stock), nil
}

func CoinsForCart(client clients.ShootingClients, ammo configs.CustomAmmo, shootContext abstractions.ShootContext, options configs.Options, offers []clients.OfferInfo, cartsCount int, offersCount int) (*clients.СoinsForCart, error) {
	var items []clients.CartItem
	for cartID := 0; cartID < cartsCount; cartID++ {
		items = append(items, getCartItemsFromOffers(offersCount, offers, cartID)...)
	}
	orderItemsRequest := clients.OrderItemsRequest{Items: items}
	resp, err := client.GetLoyaltyClient().FetchBonusesForCart(shootContext, ammo.UID, options.RegionID, orderItemsRequest)
	if err != nil {
		return nil, fmt.Errorf("cannot execute /coins/cart/WHITE/v2: '%w'", err)
	}
	return resp, nil
}

func Cart(client clients.ShootingClients, ammo configs.CustomAmmo, shootContext abstractions.ShootContext, options configs.Options, offers []clients.OfferInfo, cartsCount int, offersCount int, coinsForCart *clients.СoinsForCart) (*clients.CartRootResponse, error) {

	var internalCarts []clients.CartRequest

	var coins []int

	if coinsForCart != nil {
		coinsResponse := *coinsForCart
		for _, coin := range coinsResponse.ApplicableCoins {
			coins = append(coins, coin.ID)
		}
	}

	for cartID := 0; cartID < cartsCount; cartID++ {

		items := getCartItemsFromOffers(offersCount, offers, cartID)

		var shopID int
		if ammo.IsDropship() {
			shopID = offers[0].ShopID
		} else {
			shopID = options.ShopID
		}
		paymentMethod := "CASH_ON_DELIVERY"
		paymentType := "POSTPAID"
		internalCarts = append(internalCarts,
			clients.CartRequest{
				ShopID: shopID,
				Items:  items,
				CartDelivery: clients.CartDelivery{
					RegionID: options.RegionID,
					Address:  options.Addresses[cartID],
				},
				PaymentMethod: &paymentMethod,
				PaymentType:   &paymentType,
				Buyer: clients.Buyer{
					LastName:   "последнееимя",
					FirstName:  "первоеимя",
					MiddleName: "среднееимя",
					Phone:      "+77777777777",
					Email:      options.Email,
					IP:         "8.8.8.8",
					DontCall:   true,
				},
			})
	}

	if ammo.UsePromo && ammo.Promocode == nil {
		panic(fmt.Errorf("missing promocode parameter"))
	}

	req := &clients.CartRootRequest{
		BuyerRegionID: options.RegionID,
		BuyerCurrency: "RUR",
		Cart:          internalCarts,
		Promocode:     ammo.Promocode,
		CoinIdsToUse:  &coins,
	}

	resp, err := client.GetCheckouter().Cart(shootContext, req, options, ammo.UID, ammo.UseFlash)
	if err != nil {
		return nil, fmt.Errorf("cannot execute cart: '%w'", err)
	}

	return resp, nil
}

func getCartItemsFromOffers(offersCount int, offers []clients.OfferInfo, cartID int) []clients.CartItem {
	var items []clients.CartItem
	for offerID := 0; offerID < offersCount; offerID++ {
		offer := offers[offersCount*cartID+offerID]
		items = append(items,
			clients.CartItem{
				FeedID:     offer.FeedID,
				OfferID:    offer.OfferID,
				ShowInfo:   offer.ShowInfo,
				Count:      1,
				BuyerPrice: offer.Price,
			})
	}
	return items
}

func Checkout(client clients.ShootingClients, ammo configs.CustomAmmo, shootContext abstractions.ShootContext, options configs.Options, deliveryPayment []*clients.CheckoutParams, cartRootResponse *clients.CartRootResponse, coinsForCart *clients.СoinsForCart) *clients.CheckoutResponse {

	var orderRequests []clients.OrderRequest

	for cartID := 0; cartID < len(cartRootResponse.Cart); cartID++ {
		cartResponse := cartRootResponse.Cart[cartID]
		var coinIdsToUse *[]int
		if ammo.UseCoins {
			var logMsg string
			coinIdsToUse, logMsg = GetAllowedCoins(cartRootResponse, coinsForCart)
			shootContext.GetLogger().Warn(logMsg)
		} else {
			coinIdsToUse = nil
		}
		for i := range cartResponse.CartItems {
			cart := &cartResponse.CartItems[i]
			if cart.BuyerPriceNominal != nil {
				cart.BuyerPrice = *cart.BuyerPriceNominal
				cart.BuyerPriceNominal = nil
			}
		}
		orderRequests = append(orderRequests,
			clients.OrderRequest{
				ShopID:       options.ShopID,
				Items:        cartResponse.CartItems,
				Delivery:     deliveryPayment[cartID].Delivery,
				Notes:        "заметочка",
				CoinIdsToUse: coinIdsToUse,
			})
	}

	if ammo.UsePromo && ammo.Promocode == nil {
		panic(fmt.Errorf("missing promocode parameter"))
	}

	req := &clients.CheckoutRequest{
		BuyerRegionID: options.RegionID,
		BuyerCurrency: "RUR",
		PaymentType:   "POSTPAID",
		PaymentMethod: deliveryPayment[0].PaymentMethod,
		Orders:        orderRequests,
		Buyer: clients.Buyer{
			LastName:   "последнееимя",
			FirstName:  "первоеимя",
			MiddleName: "среднееимя",
			Phone:      "+77777777777",
			Email:      options.Email,
			IP:         "8.8.8.8",
			DontCall:   true,
		},
		Promocode: ammo.Promocode,
	}

	resp, err := client.GetCheckouter().Checkout(shootContext, req, options, ammo.UID, ammo.UseFlash)
	if err != nil {
		panic(fmt.Errorf("cannot execute checkout: '%w'", err))
	}

	return resp
}

func GetAllowedCoins(cartRootResponse *clients.CartRootResponse, coinsForCart *clients.СoinsForCart) (*[]int, string) {
	var logMsg string
	coinIdsToUse := cartRootResponse.CoinIdsToUse
	var allowedCoinIds []int
	if coinIdsToUse == nil || len(*coinIdsToUse) == 0 {
		logMsg = "CoinIdsToUse null or Empty"
	} else if coinsForCart != nil {
		var applicableCoinIds []int
		for _, appCoin := range coinsForCart.ApplicableCoins {
			applicableCoinIds = append(applicableCoinIds, appCoin.ID)
		}
		for _, coinID := range *coinIdsToUse {
			isApplicable, _ := slices.Contains(applicableCoinIds, coinID)
			if isApplicable {
				allowedCoinIds = append(allowedCoinIds, coinID)
				continue
			} else {
				var reason string
				for restrictionType, disCoins := range coinsForCart.DisabledCoins {
					var disCoinIds []int
					for _, disCoin := range disCoins {
						disCoinIds = append(disCoinIds, disCoin.ID)
					}
					isDisabled, _ := slices.Contains(disCoinIds, coinID)
					if isDisabled {
						reason = restrictionType
						break
					}
				}
				logMsg = fmt.Sprintf("Can not use coin with id: %d, reason: %s", coinID, reason)
			}
		}
	}
	coinIdsToUse = &allowedCoinIds
	return coinIdsToUse, logMsg
}

func Unfreeze(client clients.ShootingClients, shootContext abstractions.ShootContext, options configs.Options, orderID int) error {
	if options.ForceUnfreeze {
		err := client.GetStockStorage().UnfreezeStocks(shootContext, orderID)
		if err != nil {
			return fmt.Errorf("cannot execute unfreeze: '%v'", err)
		}
	} else {
		err := client.GetCheckouter().Unfreeze(shootContext, orderID)
		if err != nil {
			return fmt.Errorf("cannot execute unfreeze: '%v'", err)
		}
	}
	return nil
}

func RecentOrders(client clients.CheckouterClient, shootContext abstractions.ShootContext, path string, uid int) {
	err := client.RecentOrders(shootContext, path, uid)
	if err != nil {
		panic(fmt.Errorf("cannot get recent orders: '%v'", err))
	}
}

func OptionsAvailabilitiesOfOrders(client clients.CheckouterClient, shootContext abstractions.ShootContext, path string, orderID int, uid int) {
	err := client.OptionsAvailabilitiesOfOrders(shootContext, orderID, path, uid)
	if err != nil {
		panic(fmt.Errorf("cannot get options-availabilities of orders: '%v'", err))
	}
}

func OrdersByUID(client clients.CheckouterClient, shootContext abstractions.ShootContext, path string, uid int) {
	err := client.OrdersByUID(shootContext, path, uid)
	if err != nil {
		panic(fmt.Errorf("cannot get orders by uid: '%v'", err))
	}
}

func GetOrdersByID1(client clients.CheckouterClient, shootContext abstractions.ShootContext, path string, orderID int, uid int) {
	query := &url.Values{}
	query.Add("clientRole", "SYSTEM")
	query.Add("clientId", strconv.Itoa(uid))
	query.Add("partials", "CHANGE_REQUEST")
	query.Add("partials", "CASHBACK_EMIT_INFO")

	getOrdersByID(client, model.LabelCheckouterOrdersByID, shootContext, path, orderID, query)
}

func GetOrdersByID2(client clients.CheckouterClient, shootContext abstractions.ShootContext, path string, orderID int) {
	query := &url.Values{}
	query.Add("clientRole", "SYSTEM")

	getOrdersByID(client, model.LabelCheckouterOrdersByID2, shootContext, path, orderID, query)
}

func getOrdersByID(client clients.CheckouterClient, label string, shootContext abstractions.ShootContext, path string, orderID int, query *url.Values) {
	err := client.GetOrdersByID(label, shootContext, path, query, orderID)
	if err != nil {
		panic(fmt.Errorf("cannot get orders by uid: '%v'", err))
	}
}

func GetOrders(client clients.CheckouterClient, shootContext abstractions.ShootContext, path string, uid int) {

	req := &clients.GetOrdersRequest{
		FromDate: "1970-00-00 00:00:00",
		PageInfo: clients.PageInfo{
			From:        1,
			To:          50,
			PageSize:    50,
			CurrentPage: 1,
		},
		Partials: []string{"CHANGE_REQUEST", "CASHBACK_EMIT_INFO"},
		RGBs:     []string{"BLUE", "WHITE"},
	}

	err := client.GetOrders(shootContext, req, path, uid)
	if err != nil {
		panic(fmt.Errorf("cannot execute getOrders: '%v'", err))
	}

}

func GetOfferFromIndex(client clients.ShootingClients, shootContext abstractions.ShootContext, options configs.Options, stock clients.Stock) (*clients.OfferInfo, error) {
	info, err := client.GetOTrace().GetOfferInfo(shootContext, stock, options)
	if err != nil {
		return nil, err
	}
	infoURL := info.Urls.Report.OfferInfoURL
	parsedURL, err := url.Parse(infoURL)
	if err != nil {
		return nil, err
	}
	offerInfo, err := client.GetReport().GetOfferInfoByURL(shootContext, parsedURL)

	if err != nil {
		return nil, err
	}
	return ConvertStockOfferFromReport(options, offerInfo.ReportSearch, stock), nil
}
