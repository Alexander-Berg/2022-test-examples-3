package flow

import (
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/configs/stocktype"
	"sync"
)

type Stocks interface {
	GetStocks(stockType stocktype.StockType) []Offer
	GetStocksTypes() []stocktype.StockType
}

type WarmedUpStocks interface {
	UpdateStocks(stocksMap map[stocktype.StockType][]Offer)
	Stocks
}

func NewStocks(stocksMap map[stocktype.StockType][]Offer) WarmedUpStocks {
	lock := &sync.RWMutex{}
	return &stocks{
		offers:    &stocksMap,
		readLock:  lock.RLocker(),
		writeLock: lock,
	}
}

type stocks struct {
	offers    *map[stocktype.StockType][]Offer
	writeLock sync.Locker
	readLock  sync.Locker
}

func (s *stocks) GetStocksTypes() []stocktype.StockType {
	s.readLock.Lock()
	defer s.readLock.Unlock()
	offers := *s.offers
	keys := make([]stocktype.StockType, 0, len(offers))
	for k := range offers {
		keys = append(keys, k)
	}
	return keys
}

func (s *stocks) GetStocksMap() map[stocktype.StockType][]Offer {
	s.readLock.Lock()
	defer s.readLock.Unlock()
	return *s.offers
}

func (s *stocks) UpdateStocks(stocksMap map[stocktype.StockType][]Offer) {
	s.writeLock.Lock()
	defer s.writeLock.Unlock()
	s.offers = &stocksMap
}

func (s *stocks) GetStocks(stockType stocktype.StockType) []Offer {
	s.readLock.Lock()
	defer s.readLock.Unlock()
	return (*s.offers)[stockType]
}
