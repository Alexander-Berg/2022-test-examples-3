package clients

import (
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/abstractions"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/configs"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/model"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/util"
	"context"
	"fmt"
	"go.uber.org/zap"
	"net/url"
	"path"
	"strconv"
)

type Report struct {
	categoriesReader util.CategoriesReader
	ClientConfig
}

func (report *Report) ReadCashbackCategories() map[int]bool {
	return report.categoriesReader.ReadCashbackCategories()
}

type ReportSearchRoot struct {
	ReportSearch ReportSearch `json:"search"`
}

type ReportSearch struct {
	ReportResults []ReportOfferItem `json:"results"`
	Total         int               `json:"total"`
}

type Category struct {
	ID int `json:"id"`
}

type ReportPrimeSearchRoot struct {
	Search ReportPrimeSearch `json:"search"`
}

type ReportPrimeSearch struct {
	Total   int             `json:"total"`
	Results []ReportProduct `json:"results"`
}

type ReportOffers struct {
	Count int               `json:"count"`
	Items []ReportOfferItem `json:"items"`
}

type ReportProduct struct {
	Offers ReportOffers `json:"offers"`
}

type ReportOfferItem struct {
	Weight     string         `json:"weight"`
	Supplier   Supplier       `json:"supplier"`
	Categories []Category     `json:"categories"`
	Shop       ReportShopFeed `json:"shop"`
	FeeShow    string         `json:"feeShow"`
	ShopSku    string         `json:"shopSku"`
	Prices     ReportPrice    `json:"prices"`
}

type ReportPrice struct {
	Value    string `json:"value"`
	RawValue string `json:"rawValue"`
}

type Supplier struct {
	ID          int    `json:"id"`
	Type        string `json:"type"`
	WarehouseID int    `json:"warehouseId"`
}

type ReportShopsRoot struct {
	ReportShops []ReportShop `json:"results"`
}

type ReportShop struct {
	Feeds []FeedWarehouse `json:"feeds"`
}

type ReportShopFeed struct {
	Feed Feed `json:"feed"`
}

type Feed struct {
	FeedID  string `json:"id"`
	OfferID string `json:"offerId"`
}

type FeedWarehouse struct {
	FeedID      int `json:"feedId"`
	WarehouseID int `json:"warehouseId"`
}

type OfferInfo struct {
	ShopID       int
	FeedID       int
	OfferID      string
	ShowInfo     string
	CategoryID   int
	SupplierType string
	Price        uint64
}

func (oi OfferInfo) IsSame(item CartItem) bool {
	return oi.FeedID == item.FeedID && oi.OfferID == item.OfferID
}

func (report *Report) GetShopInfo(shootContext abstractions.ShootContext, supplierID int) (*ReportShopsRoot, error) {
	q := &url.Values{}
	q.Add("place", "shop_info")
	q.Add("fesh", strconv.Itoa(supplierID))
	q.Add("rearr-factors", "use_delivery_service_id=1")

	u := report.url
	u.Path = path.Join(u.Path, "/yandsearch")
	u.RawQuery = q.Encode()

	result := &ReportShopsRoot{}
	err := report.Get("report shop-info", shootContext, &u, result, false)

	if err != nil {
		return nil, err
	}

	return result, nil
}

func (report *Report) GetOfferInfo(shootContext abstractions.ShootContext, options configs.Options, feshofferID string) (*ReportSearchRoot, error) {
	q := &url.Values{}
	q.Add("place", "offerinfo")
	q.Add("fesh", strconv.Itoa(options.ShopID))
	q.Add("feed_shoffer_id", feshofferID)
	q.Add("rids", strconv.Itoa(options.RegionID))
	q.Add("cpa", "real")
	q.Add("pp", "18")
	q.Add("rgb", "blue")
	q.Add("regset", "2")
	q.Add("show-urls", "decrypted")
	q.Add("rearr-factors", "use_delivery_service_id=1")
	u := report.url
	u.Path = path.Join(u.Path, "/yandsearch")
	u.RawQuery = q.Encode()
	return report.GetOfferInfoByURL(shootContext, &u)
}

func (report *Report) GetOfferInfoByURL(shootContext abstractions.ShootContext, u *url.URL) (*ReportSearchRoot, error) {
	result := &ReportSearchRoot{}
	err := report.Get("report offer-info", shootContext, u, result, false)

	if err != nil {
		return nil, err
	}

	return result, nil
}

func createReportStocksQuery(options configs.Options, pageNo int, numdoc int) url.Values {
	q := url.Values{}
	q.Add("place", "prime")
	q.Add("pp", "18")
	q.Add("rids", strconv.Itoa(options.RegionID))
	q.Add("regset", "2")
	q.Add("rgb", "blue")
	q.Add("fesh", strconv.Itoa(options.ShopID))
	q.Add("page-no", strconv.Itoa(pageNo))
	q.Add("numdoc", strconv.Itoa(numdoc))
	q.Add("skip", strconv.Itoa((pageNo-1)*numdoc))
	if len(options.FlashShopPromoID) > 0 {
		q.Add("promo-type", "blue-flash")
		q.Add("shop-promo-id", options.FlashShopPromoID)
		q.Add("rearr-factors", fmt.Sprintf("promo_enable_by_shop_promo_id=%s", options.FlashShopPromoID))
	}
	return q
}

func (report *Report) getCategoryStocks(shootContext abstractions.ShootContext, options configs.Options, category int, pageNo int, numdoc int) []GetStockResult {
	q := createReportStocksQuery(options, pageNo, numdoc)
	q.Add("hid", strconv.Itoa(category))
	reportRoot, err := report.getStocks(shootContext, q)
	if err != nil {
		return []GetStockResult{
			{Err: err},
		}
	}
	stocks, offerItems := convertToStocks(*reportRoot)
	result := make([]GetStockResult, 0, len(stocks))
	for i, item := range offerItems {
		offer, err := ConvertReportOfferItemToOfferInfo(options, item)
		result = append(result, GetStockResult{Stock: &stocks[i], Offer: &offer, Err: err})
	}
	return result
}

func (report *Report) GetStocks(shootContext abstractions.ShootContext, options configs.Options) (chan GetStockResult, context.CancelFunc) {
	stocksContext, stopStocksRequests := context.WithCancel(shootContext.GetContext())
	result := make(chan GetStockResult, 100)
	go report.getStocksLoop(shootContext.WithContext(stocksContext), options, result)
	return result, stopStocksRequests
}

func (report *Report) getStocksLoop(shootContext abstractions.ShootContext, options configs.Options, result chan GetStockResult) {
	defer close(result)
	logger := shootContext.GetLogger()
	categories := CategoriesKeys(report.categoriesReader.ReadCategories())
	for pageNo := 1; len(categories) > 0; pageNo++ {
		logger.Info("read stocks from report",
			zap.Int("page", pageNo),
			zap.Ints("categories", categories),
		)
		nextPageCategories := make([]int, 0, len(categories))
		for _, category := range categories {
			stocks := report.getCategoryStocks(shootContext, options, category, pageNo, options.ReportNumDoc)
			if len(stocks) > 0 {
				nextPageCategories = append(nextPageCategories, category)
			}
			for _, stock := range stocks {
				select {
				case <-shootContext.GetContext().Done():
					return
				case result <- stock:
				}
			}
		}
		categories = nextPageCategories
	}
	logger.Info("categories have been read to the end")
}

func ConvertReportOfferItemToOfferInfo(options configs.Options, offer ReportOfferItem) (OfferInfo, error) {
	weightRaw := offer.Weight
	feedIDRaw := offer.Shop.Feed.FeedID
	feedID, err := strconv.Atoi(feedIDRaw)
	var price uint64
	if offer.Prices.RawValue != "" {
		price, _ = strconv.ParseUint(offer.Prices.RawValue, 10, 64)
	} else if offer.Prices.Value != "" {
		price, _ = strconv.ParseUint(offer.Prices.Value, 10, 64)
	}
	result := OfferInfo{
		ShopID:       offer.Supplier.ID,
		FeedID:       feedID,
		OfferID:      offer.Shop.Feed.OfferID,
		ShowInfo:     offer.FeeShow,
		CategoryID:   offer.Categories[0].ID,
		SupplierType: offer.Supplier.Type,
		Price:        price,
	}
	if err != nil {
		return result, fmt.Errorf("cannot parse raw feedID: '%v'", feedIDRaw)
	}
	weight, err := strconv.ParseFloat(weightRaw, 32)
	if err != nil {
		return result, fmt.Errorf("cannot parse weight: '%v'", weightRaw)
	}

	if float32(weight) > options.MaxWeight {
		return result, fmt.Errorf("offer has to big weight: '%v'", weight)
	}

	if offer.Supplier.ID == options.SupplierID {
		return result, fmt.Errorf("has ignored supplierID: '%v'", options.SupplierID)
	}
	return result, nil
}

func convertToStocks(reportSearchRoot ReportPrimeSearchRoot) ([]Stock, []ReportOfferItem) {
	var stocks []Stock
	var offers []ReportOfferItem
	for _, result := range reportSearchRoot.Search.Results {
		for _, offer := range result.Offers.Items {
			offers = append(offers, offer)
			stocks = append(stocks, Stock{
				Sku:      offer.ShopSku,
				Quantity: 0,
				SSItem: SSItem{
					ShopSku:     offer.ShopSku,
					SupplierID:  offer.Supplier.ID,
					WarehouseID: offer.Supplier.WarehouseID,
				},
			})
		}
	}
	fmt.Printf("[TRACE] [InitStocks] converted Report response: stocksSize=%v skus=%v\n",
		len(stocks), toShopSkus(offers))
	return stocks, offers
}

func toShopSkus(offers []ReportOfferItem) []string {
	var skus []string
	for _, offer := range offers {
		skus = append(skus, offer.ShopSku)
	}
	return skus
}

func (report *Report) getStocks(shootContext abstractions.ShootContext, queryArgs url.Values) (*ReportPrimeSearchRoot, error) {
	u := report.url
	u.Path = path.Join(u.Path, "/yandsearch")
	u.RawQuery = queryArgs.Encode()
	result := &ReportPrimeSearchRoot{}
	err := report.Get(model.LabelReportGetStocks, shootContext, &u, result, false)

	if err != nil {
		return nil, err
	}

	return result, nil
}

type ReportClient interface {
	GetShopInfo(shootContext abstractions.ShootContext, supplierID int) (*ReportShopsRoot, error)
	GetOfferInfo(shootContext abstractions.ShootContext, options configs.Options, feshofferID string) (*ReportSearchRoot, error)
	GetOfferInfoByURL(shootContext abstractions.ShootContext, u *url.URL) (*ReportSearchRoot, error)
	GetStocks(shootContext abstractions.ShootContext, options configs.Options) (chan GetStockResult, context.CancelFunc)
	ReadCashbackCategories() map[int]bool
}
