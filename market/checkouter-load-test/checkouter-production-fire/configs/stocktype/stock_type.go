package stocktype

import (
	"fmt"
	"strings"
)

type StockType int

const (
	Fulfillment         StockType = 0
	FulfillmentCashback StockType = 1 << iota
	Dropship            StockType = 1 << iota
	DropshipCashback    StockType = 1 << iota
)

func (t StockType) String() string {
	switch t {
	case FulfillmentCashback:
		return "fulfillmentCashback"
	case Dropship:
		return "dropship"
	case DropshipCashback:
		return "dropshipCashback"
	}
	return "fulfillment"
}

func FromStockType(stockType string) StockType {
	switch strings.ToLower(stockType) {
	case "fulfillment":
		return Fulfillment
	case "fulfillmentcashback":
		return FulfillmentCashback
	case "dropship":
		return Dropship
	case "dropshipcashback":
		return DropshipCashback
	}

	panic(fmt.Errorf("incorrect stock_type: '%v'", stockType))
}
