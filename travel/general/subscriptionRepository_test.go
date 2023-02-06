package pricesubimpl

import (
	"context"
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/library/go/ptr"
	"a.yandex-team.ru/travel/avia/chatbot/public/pricesub"
	"a.yandex-team.ru/travel/avia/library/proto/common/v1"
	result "a.yandex-team.ru/travel/avia/library/proto/search_result/v1"
	travel_commons_proto "a.yandex-team.ru/travel/proto"
)

func Test_subscriberToSubscriptionTable_SaveSubscriberSubscription(t *testing.T) {
	ctx := context.Background()
	assume := assert.New(t)
	subscriptions := []pricesub.Subscription{
		pricesub.Subscription{
			Query: pricesub.Query{
				PointFrom: &common.Point{
					Type: common.PointType_POINT_TYPE_SETTLEMENT,
					Id:   213,
				},
				PointTo: &common.Point{
					Type: common.PointType_POINT_TYPE_SETTLEMENT,
					Id:   239,
				},
				When: &travel_commons_proto.TDate{
					Year:  2021,
					Month: 10,
					Day:   20,
				},
				Return: nil,
				Passengers: &common.Passengers{
					Adults: 1,
				},
				ServiceClass:    common.ServiceClass_SERVICE_CLASS_ECONOMY,
				NationalVersion: common.NationalVersion_NATIONAL_VERSION_RU,
			},
			Filter: pricesub.Filter{
				Baggage:          nil,
				MaxTransferCount: nil,
			},
		},
		pricesub.Subscription{
			Query: pricesub.Query{
				PointFrom: &common.Point{
					Type: common.PointType_POINT_TYPE_SETTLEMENT,
					Id:   213,
				},
				PointTo: &common.Point{
					Type: common.PointType_POINT_TYPE_SETTLEMENT,
					Id:   239,
				},
				When: &travel_commons_proto.TDate{
					Year:  2021,
					Month: 10,
					Day:   20,
				},
				Return: nil,
				Passengers: &common.Passengers{
					Adults: 1,
				},
				ServiceClass:    common.ServiceClass_SERVICE_CLASS_ECONOMY,
				NationalVersion: common.NationalVersion_NATIONAL_VERSION_RU,
			},
			Filter: pricesub.Filter{
				Baggage:          ptr.Bool(true),
				MaxTransferCount: nil,
			},
		}, pricesub.Subscription{
			Query: pricesub.Query{
				PointFrom: &common.Point{
					Type: common.PointType_POINT_TYPE_SETTLEMENT,
					Id:   213,
				},
				PointTo: &common.Point{
					Type: common.PointType_POINT_TYPE_SETTLEMENT,
					Id:   239,
				},
				When: &travel_commons_proto.TDate{
					Year:  2021,
					Month: 10,
					Day:   20,
				},
				Return: nil,
				Passengers: &common.Passengers{
					Adults: 1,
				},
				ServiceClass:    common.ServiceClass_SERVICE_CLASS_ECONOMY,
				NationalVersion: common.NationalVersion_NATIONAL_VERSION_RU,
			},
			Filter: pricesub.Filter{
				Baggage:          ptr.Bool(true),
				MaxTransferCount: ptr.Int(0),
			},
		},
	}
	for _, subscription := range subscriptions {
		sst := newSubscriberToSubscriptionTable()
		assume.Empty(sst.WhereSubscription(subscription))
		for i := 0; i < 3; i++ {
			sst.SaveSubscriberSubscription(ctx, pricesub.SubscriberSubscription{
				Subscriber: pricesub.Subscriber{
					ChatID: 1234567,
				},
				Subscription:     subscription,
				LastSeenMinPrice: nil,
			})
			assume.Len(sst.WhereSubscription(subscription), 1)
			assume.Nil(sst.WhereSubscription(subscription)[0].LastSeenMinPrice)
		}
	}
}

func Test_subscriberToSubscriptionTable_UpdateLastSeenMinPrice(t *testing.T) {
	ctx := context.Background()
	assume := assert.New(t)
	subsub := pricesub.SubscriberSubscription{
		Subscriber: pricesub.Subscriber{
			ChatID: 123456,
		},
		Subscription: pricesub.Subscription{
			Query: pricesub.Query{
				PointFrom: &common.Point{
					Type: common.PointType_POINT_TYPE_SETTLEMENT,
					Id:   213,
				},
				PointTo: &common.Point{
					Type: common.PointType_POINT_TYPE_SETTLEMENT,
					Id:   239,
				},
				When: &travel_commons_proto.TDate{
					Year:  2021,
					Month: 10,
					Day:   20,
				},
				Return: &travel_commons_proto.TDate{
					Year:  2021,
					Month: 10,
					Day:   30,
				},
				Passengers: &common.Passengers{
					Adults: 1,
				},
				ServiceClass:    common.ServiceClass_SERVICE_CLASS_ECONOMY,
				NationalVersion: common.NationalVersion_NATIONAL_VERSION_RU,
			},
			Filter: pricesub.Filter{
				Baggage:          nil,
				MaxTransferCount: nil,
			},
		},
		LastSeenMinPrice: nil,
	}
	sst := newSubscriberToSubscriptionTable()
	assume.Empty(sst.WhereSubscription(subsub.Subscription))
	sst.UpdateLastSeenMinPrice(ctx, &result.Result{
		Variants: []*result.Variant{
			{
				Price: &common.Price{
					Currency: "RUR",
					Value:    1000,
				},
			},
		},
	}, subsub)
	assume.Len(sst.WhereSubscription(subsub.Subscription), 1)
	assume.EqualValues(sst.WhereSubscription(subsub.Subscription)[0].LastSeenMinPrice, &common.Price{
		Currency: "RUR",
		Value:    1000,
	})
	sst.UpdateLastSeenMinPrice(ctx, &result.Result{
		Variants: []*result.Variant{
			{
				Price: &common.Price{
					Currency: "RUR",
					Value:    2000,
				},
			},
		},
	}, subsub)
	assume.Len(sst.WhereSubscription(subsub.Subscription), 1)
	assume.EqualValues(sst.WhereSubscription(subsub.Subscription)[0].LastSeenMinPrice, &common.Price{
		Currency: "RUR",
		Value:    2000,
	})

	sst.SaveSubscriberSubscription(ctx, subsub)
	assume.Len(sst.WhereSubscription(subsub.Subscription), 1)
	assume.Nil(sst.WhereSubscription(subsub.Subscription)[0].LastSeenMinPrice)
}
