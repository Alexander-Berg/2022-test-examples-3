package testutils

import proto "a.yandex-team.ru/travel/proto"

func PriceOf(amount int64) *proto.TPrice {
	return &proto.TPrice{
		Amount:    amount,
		Precision: 2,
		Currency:  proto.ECurrency_C_RUB,
	}
}
