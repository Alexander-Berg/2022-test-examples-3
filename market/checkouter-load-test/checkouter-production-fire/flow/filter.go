package flow

import (
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/clients"
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/configs"
	"fmt"
	"math/rand"
	"sort"
)

type logStateType struct {
	label   string
	enabled bool
}

func resolveLogState(ammo configs.CustomAmmo, options configs.Options) logStateType {
	if ammo.ID >= 0 {
		return logStateType{
			label:   "InitStocks",
			enabled: options.LogChooseDeliveryOptionsInInit,
		}
	} else {
		return logStateType{
			label:   "Shoot",
			enabled: options.LogChooseDeliveryOptionsInShoot,
		}
	}
}

func ChooseDelivery(ammo configs.CustomAmmo, options configs.Options, address map[string]string,
	deliveryOptions []clients.DeliveryOption) (clients.Delivery, string) {

	postpaidDeliveryOptions := []clients.DeliveryOption{}

	logState := resolveLogState(ammo, options)

	for _, delivery := range deliveryOptions {

		if len(options.DeliveryType) > 0 && delivery.Type != options.DeliveryType {
			if logState.enabled {
				fmt.Printf("[WARN] [%v] [%d] deliveryType not equal: '%v'\n",
					logState.label,
					ammo.ID,
					options.DeliveryType)
			}
			continue
		}

		hasPostpaidOption := false
		for _, payment := range delivery.PaymentOptions {
			if payment.PaymentType == "POSTPAID" {
				hasPostpaidOption = true
				break
			}
		}

		if hasPostpaidOption {
			postpaidDeliveryOptions = append(postpaidDeliveryOptions, delivery)
		} else {
			if logState.enabled {
				fmt.Printf("[WARN] [InitStocks] [%d] delivery has not postpaid option\n", ammo.ID)
			}
		}
	}

	filteredDeliveryOptions := []clients.DeliveryOption{}
	for _, delivery := range postpaidDeliveryOptions {
		if options.DeliveryServices[ammo.UseStockType](delivery.DeliveryServiceID) {
			filteredDeliveryOptions = append(filteredDeliveryOptions, delivery)
		} else {
			if logState.enabled {
				fmt.Printf("[WARN] [%v] [%d] delvieryServiceId: '%v' filtered by deliveryServiceId\n",
					logState.label,
					ammo.ID,
					delivery.DeliveryServiceID)
			}
		}
	}

	if logState.enabled {
		fmt.Printf("[WARN] [%v] [%d] filteredDeliveryOptions count: '%v'\n",
			logState.label,
			ammo.ID,
			len(filteredDeliveryOptions))
	}

	delivery := randomByDistribution(ammo, options, filteredDeliveryOptions)

	if len(delivery.ID) == 0 {
		return clients.Delivery{ID: ""}, ""
	}

	paymentMethod := ""
	for _, payment := range delivery.PaymentOptions {
		if payment.PaymentType == "POSTPAID" {
			paymentMethod = payment.PaymentMethod
		}
	}

	return makeDelivery(options, delivery, address), paymentMethod
}

func makeDistributionMap(distribution map[string]float32) (map[string]rangeType, []string) {
	distributionMap := map[string]rangeType{}

	keys := []string{}
	for k := range distribution {
		keys = append(keys, k)
	}
	sort.Strings(keys)
	value := float32(0.0)
	for _, k := range keys {
		distributionMap[k] = rangeType{
			min: value,
			max: value + distribution[k],
		}
		value += distribution[k]
	}

	return distributionMap, keys
}

func randomByDistribution(ammo configs.CustomAmmo, options configs.Options, deliveryOptions []clients.DeliveryOption) clients.DeliveryOption {
	logState := resolveLogState(ammo, options)

	if len(deliveryOptions) == 0 {
		if logState.enabled {
			fmt.Printf("[ERROR] [%v] [%d] empty delivery options in randomize\n",
				logState.label,
				ammo.ID)
		}
		return clients.DeliveryOption{}
	} else if len(deliveryOptions) == 1 {
		option := deliveryOptions[0]
		if logState.enabled {
			fmt.Printf("[INFO] [%v] [%d] delivery options used with id: '%v'\n",
				logState.label,
				ammo.ID,
				option.ID)
		}
		return option
	} else if options.Distribution == nil || len(options.Distribution) == 0 {
		option := deliveryOptions[rand.Intn(len(deliveryOptions))]
		if logState.enabled {
			fmt.Printf("[INFO] [%v] [%d] delivery options used with id: '%v'\n",
				logState.label,
				ammo.ID,
				option.ID)
		}
		return option
	}

	if logState.enabled {
		fmt.Printf("[INFO] [%v] try to use delivery options with randomize: '%v'\n",
			logState.label,
			ammo.ID)
	}

	distributionMap, keys := makeDistributionMap(options.Distribution)

	rnd := rand.Float32()

	deliveryID := ""
	for _, k := range keys {
		if distributionMap[k].inRange(rnd) {
			deliveryID = k
		}
	}

	for _, delivery := range deliveryOptions {
		if delivery.ID == deliveryID {
			return delivery
		}
	}

	return clients.DeliveryOption{ID: ""}
}

func makeDelivery(options configs.Options, deliveryOption clients.DeliveryOption,
	address map[string]string) clients.Delivery {

	deliveryType := deliveryOption.Type

	if deliveryType == "PICKUP" {
		return clients.Delivery{
			ID:       deliveryOption.ID,
			RegionID: options.RegionID,
			OutletID: &deliveryOption.Outlets[0].ID,
			Outlet:   &clients.CheckoutOutlet{OutletID: deliveryOption.Outlets[0].ID},
		}
	} else if deliveryType == "DELIVERY" {
		return clients.Delivery{
			ID:       deliveryOption.ID,
			RegionID: options.RegionID,
			Address:  address,
			Dates: clients.Dates{
				FromDate: deliveryOption.DeliveryIntervals[0].Date,
				ToDate:   deliveryOption.DeliveryIntervals[0].Date,
				FromTime: deliveryOption.DeliveryIntervals[0].TimeIntervals[0].FromTime,
				ToTime:   deliveryOption.DeliveryIntervals[0].TimeIntervals[0].ToTime,
			},
		}
	} else {
		return clients.Delivery{ID: ""}
	}
}
