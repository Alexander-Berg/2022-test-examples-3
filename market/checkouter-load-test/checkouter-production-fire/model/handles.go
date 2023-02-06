package model

import (
	"fmt"
	"time"
)

type HandleName string

const (
	RecentOrders                = HandleName("recentOrders")
	OrdersByUID                 = HandleName("ordersByUID")
	OrdersOptionsAvailabilities = HandleName("ordersOptionsAvailabilities")
	GetOrders                   = HandleName("getOrders")
	OrdersByID1                 = HandleName("ordersById1")
	OrdersByID2                 = HandleName("ordersById2")
)

type Handle struct {
	Name      HandleName
	Repeats   int
	Delay     *time.Duration
	DelayInMs *int //TODO: старое поле, удалить после релиза ЦУМ
	Active    bool
}

func (handle *Handle) GetDelay() (time.Duration, error) {
	if handle.Delay != nil {
		return *handle.Delay, nil
	}
	if handle.DelayInMs != nil {
		return time.Millisecond * time.Duration(*handle.DelayInMs), nil
	}
	return 0, fmt.Errorf("delay is undefined")
}

func (handle Handle) GetLabel() string {
	switch handle.Name {
	case RecentOrders:
		return LabelCheckouterRecentOrders
	case OrdersByUID:
		return LabelCheckouterOrdersByUID
	case OrdersOptionsAvailabilities:
		return LabelCheckouterOptionsAvailabilities
	case GetOrders:
		return LabelCheckouterGetOrders
	case OrdersByID1:
		return LabelCheckouterOrdersByID
	case OrdersByID2:
		return LabelCheckouterOrdersByID2
	default:
		return ""
	}
}

func (handle Handle) GetURL() string {
	switch handle.Name {
	case RecentOrders:
		return "/orders/by-uid/%v/recent"
	case OrdersByUID:
		return "/orders/by-uid/%v"
	case OrdersOptionsAvailabilities:
		return "/orders/options-availabilities"
	case GetOrders:
		return "/get-orders"
	case OrdersByID1, OrdersByID2:
		return "/orders/%v"
	default:
		panic(fmt.Errorf("URL for '%v' handle type is undefined", handle.Name))
	}
}
