package clients

type GetStockResult struct {
	Stock *Stock
	Offer *OfferInfo
	Err   error
}

func (res *GetStockResult) Unwrap() (*Stock, *OfferInfo, error) {
	return res.Stock, res.Offer, res.Err
}

func StocksArrayToGetStockResult(stocks []Stock) chan GetStockResult {
	result := make(chan GetStockResult, len(stocks)+1)
	defer close(result)
	for idx := range stocks {
		result <- GetStockResult{Stock: &stocks[idx]}
	}
	return result
}
