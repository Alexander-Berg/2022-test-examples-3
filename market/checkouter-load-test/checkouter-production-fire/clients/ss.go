package clients

import (
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/abstractions"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/model"
	"net/url"
	"path"
	"strconv"
)

type StockStorage struct {
	ClientConfig
}

type StocksRoot struct {
	Stocks []Stock `json:"stocks"`
}

type SSItem struct {
	ShopSku     string `json:"shopSku"`
	SupplierID  int    `json:"vendorId"`
	WarehouseID int    `json:"warehouseId"`
}

type SSItemAmount struct {
	Item   SSItem `json:"item"`
	Amount int    `json:"amount"`
}

type Stock struct {
	Sku      string `json:"sku"`
	Quantity int    `json:"quantity"`
	SSItem
}

type GetStocksAmountResponse struct {
	Items []SSItemAmount `json:"items"`
}

type GetStocksAmountRequest struct {
	Items []SSItem `json:"items"`
}

func (ss *StockStorage) UnfreezeStocks(shootContext abstractions.ShootContext, orderID int) error {
	q := url.Values{}
	q.Add("cancel", "true")

	u := ss.url
	u.Path = path.Join(u.Path, "/order/", strconv.Itoa(orderID))
	u.RawQuery = q.Encode()

	err := ss.Delete(model.LabelSsUnfreezeStocks, shootContext, &u, false)
	return err
}

func (ss *StockStorage) GetAvailableAmounts(shootContext abstractions.ShootContext, stocks []SSItem) ([]SSItemAmount, error) {
	u := ss.url
	u.Path = path.Join(u.Path, "/order/getAvailableAmounts")

	request := &GetStocksAmountRequest{
		Items: stocks,
	}

	result := &GetStocksAmountResponse{}
	err := ss.Post(model.LabelSsGetAvailableAmounts, shootContext, &u, map[string]string{}, request, result, false)
	if err != nil {
		return nil, err
	}

	return result.Items, nil
}

type StockStorageClient interface {
	UnfreezeStocks(shootContext abstractions.ShootContext, orderID int) error
	GetAvailableAmounts(shootContext abstractions.ShootContext, stocks []SSItem) ([]SSItemAmount, error)
}
