package flow

import (
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/clients"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/configs"
	"fmt"
)

type rangeType struct {
	min float32
	max float32
}

func (r rangeType) inRange(val float32) bool {
	return r.min <= val && val < r.max
}

func ConvertStockOfferFromReport(options configs.Options, result clients.ReportSearch, stock clients.Stock) *clients.OfferInfo {
	for _, offer := range result.ReportResults {
		result, err := clients.ConvertReportOfferItemToOfferInfo(options, offer)
		if err != nil {
			stockLogger(stock, &result, true).Warn(err.Error())
			continue
		}
		return &result
	}
	stockLogger(stock, nil, true).Error("cannot convert offer_info: 'cannot choose offers'")
	return nil
}

func ExtractFeedIDFromReport(options configs.Options, supplierID int, isDropship bool,
	shops []clients.ReportShop, stock clients.Stock) int {
	if len(shops) == 0 {
		stockLogger(stock, nil, true).Error("shops is empty")
		return -1
	}

	shop := shops[0]
	if len(shop.Feeds) == 0 {
		stockLogger(stock, nil, true).Error("shop feeds is empty")
		return -1
	}
	for _, feed := range shop.Feeds {
		if isDropship || feed.WarehouseID == stock.WarehouseID {
			return feed.FeedID
		}
	}

	stockLogger(stock, nil, true).
		Error(fmt.Sprintf("Cannot extract feedId for shop: '%v' and warehouse: '%v'", supplierID, stock.WarehouseID))
	return -1
}

func MakeShofferID(feedID int, sku string) string {
	// для fbs прямолинейный shofferid
	return fmt.Sprintf("%v-%v", feedID, sku)
}
