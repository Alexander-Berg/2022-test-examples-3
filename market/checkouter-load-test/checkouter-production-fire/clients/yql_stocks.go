package clients

import (
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/abstractions"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/configs"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/util"
	"fmt"
)

type YQLStocksClient interface {
	GetStocks(shootContext abstractions.ShootContext, options configs.Options) (*StocksRoot, error)
}

type YQLStocks struct {
	yql              abstractions.YQL
	secret           util.SecretConfig
	requestProcessor abstractions.RequestProcessor
	YQLConfig
}

const queryTemplate = `
use hahn;

select
shopsdat.priority_regions as shop_region,
warehouse.location_id as warehouse_region,
stock.warehouse_id as warehouse_id,
stock.supplier_id as supplier_id,
shopsdat.datafeed_id as feed_id,
stock.shop_sku as shop_sku,
stock.available_amount as amount
from
` + "`%s`" + ` as stock
join ` + "`%s`" + ` as shop
on shop.id = stock.supplier_id
join ` + "`%s`" + ` as shopsdat
on shopsdat.shop_id = shop.id and shopsdat.warehouse_id = stock.warehouse_id
join ` + "`%s`" + ` as warehouse
on warehouse.id = stock.warehouse_id
where shop.is_dropship = 1 and shop.is_ignore_stocks = 0 and stock.available_amount > 100 and shopsdat.is_enabled
order by amount DESC
limit 100;
`

func (stocks *YQLStocks) getQuery() string {
	return fmt.Sprintf(queryTemplate,
		stocks.StocksTable,
		stocks.PartnersTable,
		stocks.SuppliersTable,
		stocks.LogisticPartnersTable,
	)
}

func (stocks *YQLStocks) GetStocks(shootContext abstractions.ShootContext, options configs.Options) (*StocksRoot, error) {
	var response *[]abstractions.ResultRow
	var err error
	token := options.ClientDependencies.SecretResolver(stocks.secret)
	if options.YQLQueryID != "" {
		response, err = stocks.yql.GetOperationData(options.YQLQueryID, stocks.requestProcessor, shootContext, token)
		if err != nil {
			panic(fmt.Errorf("cannot get results of query %s: '%v'", options.YQLQueryID, err))
		}
	} else {
		query := stocks.getQuery()

		response, err = stocks.yql.ExecuteQuery(query, stocks.requestProcessor, shootContext, token)
		if err != nil {
			panic(fmt.Errorf("cannot get execute yql %s: '%v'", query, err))
		}

	}

	result := make([]Stock, len(*response))
	for i, row := range *response {
		stock := Stock{
			Sku:      asString(row.Values["shop_sku"]),
			Quantity: asInt(row.Values["amount"]),
			SSItem: SSItem{
				ShopSku:     asString(row.Values["shop_sku"]),
				SupplierID:  asInt(row.Values["supplier_id"]),
				WarehouseID: asInt(row.Values["warehouse_id"]),
			},
		}
		result[i] = stock
	}
	return &StocksRoot{result}, nil
}

func asInt(value interface{}) int {
	switch i := value.(type) {
	case float64:
		return int(i)
	default:
		return 0
	}
}

func asString(value interface{}) string {
	switch s := value.(type) {
	case string:
		return s
	default:
		return ""
	}
}
