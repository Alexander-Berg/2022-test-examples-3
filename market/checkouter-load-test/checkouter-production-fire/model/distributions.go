package model

import (
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/util"
	"fmt"
	"math/rand"
)

type GlobalOrdersDistribution struct {
	Distributions []*OrdersDistribution
}

type OrdersDistribution struct {
	Checkouts    int
	Carts        int
	Offers       int
	Distribution float32
}

type CartDistribution struct {
	InternalCarts      int
	OrdersDistribution float32
}

type OfferDistribution struct {
	OffersCount        int
	OrdersDistribution float32
}

func CreateCartDistribution(cd map[string]interface{}) CartDistribution {
	if len(cd) == 0 {
		panic(fmt.Errorf("no card distributions, no cartoons"))
	}

	internalCarts, ok := cd["internalCarts"].(int)
	if !ok {
		panic(fmt.Errorf("cannot parse 'internalCarts' from card distribution: '%v'", cd))
	}

	ordersDistribution, ok := cd["ordersDistribution"]
	if !ok {
		panic(fmt.Errorf("cannot parse 'ordersDistribution' from card distribution: '%v'", cd))
	}

	return CartDistribution{
		InternalCarts:      internalCarts,
		OrdersDistribution: util.GetFloat32(ordersDistribution),
	}
}

func (god *GlobalOrdersDistribution) GetCartsAndOffers() (carts int, offers int) {
	distSumX10000 := 0
	for i := len(god.Distributions) - 1; i >= 0; i-- {
		dist := god.Distributions[i]
		if dist.Checkouts <= 0 {
			god.Distributions = remove(god.Distributions, i)
			continue
		}
		distSumX10000 += int(dist.Distribution * 10000)
	}

	if len(god.Distributions) == 0 {
		return 0, 0
	}

	distRand := rand.Intn(distSumX10000)
	for _, dist := range god.Distributions {
		distRand -= int(dist.Distribution * 10000)
		if distRand <= 0 {
			return dist.Carts, dist.Offers
		}
	}
	d := god.Distributions[len(god.Distributions)-1]
	return d.Carts, d.Offers
}

func remove(od []*OrdersDistribution, i int) []*OrdersDistribution {
	od[i] = od[len(od)-1]
	return od[:len(od)-1]
}
