package deferredcourierlite

import (
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/test/requirepb"
	bg "a.yandex-team.ru/market/combinator/pkg/biggeneration"
	"a.yandex-team.ru/market/combinator/pkg/daysoff"
	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/market/combinator/pkg/its"
	"a.yandex-team.ru/market/combinator/pkg/lite"
	"a.yandex-team.ru/market/combinator/pkg/outlets"
	"a.yandex-team.ru/market/combinator/pkg/units"
	pb "a.yandex-team.ru/market/combinator/proto/grpc"
)

func TestGetOffersDeliveryStatsWithDeferredCourier(t *testing.T) {
	settings, _ := its.NewStringSettingsHolder("{}")
	graphEx, disabledDates := makeDeferredCourierGraph(settings)
	regionMap := geobase.NewExample()
	generationData := bg.GenerationData{
		RegionMap:           regionMap,
		TariffsFinder:       makeTariffsFinder(),
		Graph:               graphEx,
		DisabledDatesHashed: disabledDates,
		Outlets: outlets.Make([]outlets.Outlet{
			{
				ID:                         goPlatformPointLmsID,
				Type:                       outlets.Depot,
				PostCode:                   123321,
				GpsCoords:                  units.GpsCoords{Latitude: 55.722613, Longitude: 37.555378},
				RegionID:                   geobase.RegionID(geobase.RegionMoscow),
				DeferredCourierRadius:      200,
				IsDeferredCourierAvailable: true,
			},
			{
				ID:                         goPlatformDarkPointLmsID,
				Type:                       outlets.Depot,
				PostCode:                   123321,
				RegionID:                   geobase.RegionID(geobase.RegionMoscow),
				GpsCoords:                  units.GpsCoords{Latitude: 55.122613, Longitude: 37.855378},
				DeferredCourierRadius:      200,
				IsDeferredCourierAvailable: true,
				IsDarkStore:                true,
			},
		}, &regionMap, nil),
	}
	generationData.TariffsFinder.Common.PointRegionMap = map[int64]geobase.RegionID{
		goPlatformDarkPointLmsID: geobase.RegionMoscow,
		goPlatformPointLmsID:     geobase.RegionMoscow,
	}

	courierAndHandingDaysOff := &daysoff.ServiceDaysOff{
		Services: map[int64]daysoff.DisabledDatesMap{
			3000005: daysoff.NewHolidayDaysOff([]string{"2020-06-02", "2020-06-03"}),
		},
	}

	generationData.AddDaysOff(courierAndHandingDaysOff)

	startTime := time.Date(2020, 06, 02, 12, 4, 5, 0, time.UTC)

	env, cancel := lite.NewEnv(t, &generationData, settings)
	defer cancel()

	//БЕЗ подменшивания часовых слотов в курьерские опции
	{
		req := &pb.OffersDeliveryRequest{
			StartTime: lite.ToProtoTimestamp(startTime),
			Destination: &pb.PointIds{
				RegionId: 213,
			},
			Offers: []*pb.SkuOffer{
				&pb.SkuOffer{
					Offer: &pb.Offer{
						ShopSku:        "ssku1",
						AvailableCount: 1,
						PartnerId:      ffWarehouseID,
					},
					MarketSku:  "msku",
					Weight:     1000,
					Dimensions: []uint32{11, 22, 33},
				},
			},
		}

		resp, err := env.Client.GetOffersDeliveryStats(env.Ctx, req)
		require.Equal(t, nil, err)
		require.Equal(t, 1, len(resp.OffersDelivery))
		requirepb.Equal(t, &pb.Date{Year: 2020, Month: 06, Day: 04}, resp.OffersDelivery[0].CourierStats.DateFrom)
	}
	//С подменшиванием часовых слотов в курьерские опции
	{
		//with rear
		req := &pb.OffersDeliveryRequest{
			StartTime: lite.ToProtoTimestamp(startTime),
			Destination: &pb.PointIds{
				RegionId: 213,
			},
			Offers: []*pb.SkuOffer{
				&pb.SkuOffer{
					Offer: &pb.Offer{
						ShopSku:        "ssku1",
						AvailableCount: 1,
						PartnerId:      ffWarehouseID,
					},
					MarketSku:  "msku",
					Weight:     1000,
					Dimensions: []uint32{11, 22, 33},
				},
			},
			RearrFactors: "market_combinator_deferred_courier_options=1;use_deferred_in_delivery_stats=1;",
		}

		resp, err := env.Client.GetOffersDeliveryStats(env.Ctx, req)
		require.Equal(t, nil, err)
		require.Equal(t, 1, len(resp.OffersDelivery))
		requirepb.Equal(t, &pb.Date{Year: 2020, Month: 06, Day: 03}, resp.OffersDelivery[0].CourierStats.DateFrom)
	}
}
