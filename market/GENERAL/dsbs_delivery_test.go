package lite

import (
	"math"
	"sort"
	"strconv"
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/test/requirepb"
	bg "a.yandex-team.ru/market/combinator/pkg/biggeneration"
	"a.yandex-team.ru/market/combinator/pkg/dsbs"
	"a.yandex-team.ru/market/combinator/pkg/enums"
	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/market/combinator/pkg/graph"
	"a.yandex-team.ru/market/combinator/pkg/its"
	"a.yandex-team.ru/market/combinator/pkg/outlets"
	tr "a.yandex-team.ru/market/combinator/pkg/tarifficator"
	"a.yandex-team.ru/market/combinator/pkg/units"
	pb "a.yandex-team.ru/market/combinator/proto/grpc"
)

var GraphExampleDSBS *graph.Example1
var GenDataDSBS bg.GenerationData

func TestWithTariff(t *testing.T) {
	settings, _ := its.NewStringSettingsHolder(`{"modify_requests": false}`)
	env, cancel := NewEnv(t, &GenDataDSBS, settings)
	defer cancel()
	{
		// Request with shop_id and tariff
		req := MakeRequest(
			RequestWithStartTime(time.Date(2021, 3, 25, 12, 0, 0, 0, MskTZ)),
			RequestWithPartner(GraphExampleDSBS.WarehouseDSBS.PartnerLmsID),
			RequestWithRegion(geobase.RegionMytishchi),
			RequestWithShopID(322),
			RequestWithItemPrice(1000),
		)
		respDebug, err := env.Client.DebugFindPaths(env.Ctx, req)
		require.NoError(t, err)
		require.Len(t, respDebug.SortedPaths, 4)

		respCour, err := env.Client.GetCourierOptions(env.Ctx, req)
		require.NoError(t, err)
		require.Len(t, respCour.Options, 2)

		opt := respCour.Options[0]
		requirepb.Equal(t, &pb.Date{Day: 28, Month: 3, Year: 2021}, opt.DateFrom)
		requirepb.Equal(t, &pb.Date{Day: 28, Month: 3, Year: 2021}, opt.DateTo)
		requirepb.Equal(t, []pb.PaymentMethod{pb.PaymentMethod_PREPAYMENT, pb.PaymentMethod_CASH, pb.PaymentMethod_CARD}, opt.PaymentMethods)

		opt = respCour.Options[1]
		requirepb.Equal(t, &pb.Date{Day: 29, Month: 3, Year: 2021}, opt.DateFrom)
		requirepb.Equal(t, &pb.Date{Day: 29, Month: 3, Year: 2021}, opt.DateTo)
		requirepb.Equal(t, []pb.PaymentMethod{pb.PaymentMethod_PREPAYMENT, pb.PaymentMethod_CASH, pb.PaymentMethod_CARD}, opt.PaymentMethods)

		checkDeliveryServiceForDSBS(t, respCour)

		// Check tariff for Moscow
		req = MakeRequest(
			RequestWithStartTime(time.Date(2021, 3, 25, 12, 0, 0, 0, MskTZ)),
			RequestWithPartner(GraphExampleDSBS.WarehouseDSBS.PartnerLmsID),
			RequestWithRegion(geobase.RegionMoscow),
			RequestWithShopID(322),
			RequestWithItemPrice(6789),
		)
		respDebug, err = env.Client.DebugFindPaths(env.Ctx, req)
		require.NoError(t, err)
		require.Len(t, respDebug.SortedPaths, 4)

		respCour, err = env.Client.GetCourierOptions(env.Ctx, req)
		require.NoError(t, err)
		require.Len(t, respCour.Options, 2)

		opt = respCour.Options[0]
		requirepb.Equal(t, &pb.Date{Day: 27, Month: 3, Year: 2021}, opt.DateFrom)
		requirepb.Equal(t, &pb.Date{Day: 27, Month: 3, Year: 2021}, opt.DateTo)
		requirepb.Equal(t, []pb.PaymentMethod{pb.PaymentMethod_PREPAYMENT}, opt.PaymentMethods)

		opt = respCour.Options[1]
		requirepb.Equal(t, &pb.Date{Day: 28, Month: 3, Year: 2021}, opt.DateFrom)
		requirepb.Equal(t, &pb.Date{Day: 28, Month: 3, Year: 2021}, opt.DateTo)
		requirepb.Equal(t, []pb.PaymentMethod{pb.PaymentMethod_PREPAYMENT}, opt.PaymentMethods)

		checkDeliveryServiceForDSBS(t, respCour)

		req2 := MakeRequest(
			RequestWithStartTime(time.Date(2021, 3, 25, 12, 0, 0, 0, MskTZ)),
			RequestWithPartner(GraphExampleDSBS.WarehouseDSBS.PartnerLmsID),
			RequestWithRegion(geobase.RegionMytishchi),
			RequestWithShopID(322),
			RequestWithDeliveryOption(
				&pb.Date{Day: 29, Month: 3, Year: 2021},
				&pb.Date{Day: 29, Month: 3, Year: 2021},
			),
		)

		respRoute, err := env.Client.GetDeliveryRoute(env.Ctx, req2)
		require.NoError(t, err)
		require.Len(t, respRoute.Route.Points, 4)

		//COMBINATOR-2290: return dsbs code for outlet points
		req3 := MakeRequest(
			RequestWithStartTime(time.Date(2021, 3, 25, 12, 0, 0, 0, MskTZ)),
			RequestWithPartner(GraphExampleDSBS.WarehouseDSBS.PartnerLmsID),
			RequestWithRegion(geobase.RegionMytishchi),
			RequestWithShopID(505),
			RequestWithDeliveryOption(
				&pb.Date{Day: 29, Month: 3, Year: 2021},
				&pb.Date{Day: 29, Month: 3, Year: 2021},
			),
			RequestWithDeliveryType(pb.DeliveryType_PICKUP),
			RequestWithRearrFactors("combinator_dsbs_branded_depots=0;use_fake_options_for_dsbs=1"),
		)
		req3.Destination.LogisticPointId = 102030
		respRoute, err = env.Client.GetDeliveryRoute(env.Ctx, req3)
		require.NoError(t, err)
		require.NotEmpty(t, respRoute.Route.Points)
		for _, point := range respRoute.Route.Points {
			if point.SegmentType == graph.SegmentTypePickup.String() {
				require.Equal(t, "test"+strconv.FormatUint(point.Ids.LogisticPointId, 10), point.Ids.DsbsPointId)
			}
		}

		//COMBINATOR-2316: find path to market branded depot if a courier partner option exists
		req4 := MakeRequest(
			RequestWithStartTime(time.Date(2021, 3, 25, 12, 0, 0, 0, MskTZ)),
			RequestWithPartner(GraphExampleDSBS.WarehouseDSBS.PartnerLmsID),
			RequestWithRegion(geobase.RegionMytishchi),
			RequestWithShopID(505),
			RequestWithDeliveryOption(
				&pb.Date{Day: 29, Month: 3, Year: 2021},
				&pb.Date{Day: 29, Month: 3, Year: 2021},
			),
			RequestWithDeliveryType(pb.DeliveryType_PICKUP),
			RequestWithRearrFactors("use_yt_dsbs_outlet_shops=1;use_fake_options_for_dsbs=1"),
		)
		req4.Destination.LogisticPointId = 404050
		respRoute, err = env.Client.GetDeliveryRoute(env.Ctx, req4)
		require.NoError(t, err)
		require.NotEmpty(t, respRoute.Route.Points)
		for _, point := range respRoute.Route.Points {
			if point.SegmentType == graph.SegmentTypePickup.String() {
				require.Equal(t, "", point.Ids.DsbsPointId)
			}
		}

		// Price less than 5000
		reqStats := MakeStatsRequest(
			RequestWithStartTime(time.Date(2021, 3, 25, 12, 0, 0, 0, MskTZ)),
			RequestWithRegion(geobase.RegionMytishchi),
			RequestWithSkuOffer(&SkuOffer{
				ShopSku:    "DSBSoffer",
				ShopID:     322,
				PartnerID:  uint64(GraphExampleDSBS.WarehouseDSBS.PartnerLmsID),
				Weight:     1000,
				Dimensions: [3]uint32{10, 20, 30},
				MarketSku:  "DSBSoffer",
				Price:      4200,
			}),
		)
		respStats, err := env.Client.GetOffersDeliveryStats(env.Ctx, reqStats)

		require.NoError(t, err)
		require.Equal(t, 1, len(respStats.OffersDelivery))
		courierStats := respStats.OffersDelivery[0].CourierStats
		require.NotNil(t, courierStats)
		requirepb.Equal(t, &pb.Date{Day: 28, Month: 3, Year: 2021}, courierStats.DateFrom)
		requirepb.Equal(t, &pb.Date{Day: 28, Month: 3, Year: 2021}, courierStats.DateTo)

		// Price more than 5000
		reqStats = MakeStatsRequest(
			RequestWithStartTime(time.Date(2021, 3, 25, 12, 0, 0, 0, MskTZ)),
			RequestWithRegion(geobase.RegionMytishchi),
			RequestWithSkuOffer(&SkuOffer{
				ShopSku:    "DSBSoffer",
				ShopID:     322,
				PartnerID:  uint64(GraphExampleDSBS.WarehouseDSBS.PartnerLmsID),
				Weight:     1000,
				Dimensions: [3]uint32{10, 20, 30},
				MarketSku:  "DSBSoffer",
				Price:      5600,
			}),
		)
		respStats, err = env.Client.GetOffersDeliveryStats(env.Ctx, reqStats)

		require.NoError(t, err)
		require.Equal(t, 1, len(respStats.OffersDelivery))
		courierStats = respStats.OffersDelivery[0].CourierStats
		require.NotNil(t, courierStats)
		requirepb.Equal(t, &pb.Date{Day: 27, Month: 3, Year: 2021}, courierStats.DateFrom)
		requirepb.Equal(t, &pb.Date{Day: 27, Month: 3, Year: 2021}, courierStats.DateTo)

		// Time after order before
		reqStats = MakeStatsRequest(
			RequestWithStartTime(time.Date(2021, 3, 25, 14, 0, 1, 0, MskTZ)),
			RequestWithRegion(geobase.RegionMytishchi),
			RequestWithSkuOffer(&SkuOffer{
				ShopSku:    "DSBSoffer",
				ShopID:     322,
				PartnerID:  uint64(GraphExampleDSBS.WarehouseDSBS.PartnerLmsID),
				Weight:     1000,
				Dimensions: [3]uint32{10, 20, 30},
				MarketSku:  "DSBSoffer",
				Price:      3200,
			}),
		)
		respStats, err = env.Client.GetOffersDeliveryStats(env.Ctx, reqStats)

		require.NoError(t, err)
		require.Equal(t, 1, len(respStats.OffersDelivery))
		courierStats = respStats.OffersDelivery[0].CourierStats
		require.NotNil(t, courierStats)
		requirepb.Equal(t, &pb.Date{Day: 29, Month: 3, Year: 2021}, courierStats.DateFrom)
		requirepb.Equal(t, &pb.Date{Day: 29, Month: 3, Year: 2021}, courierStats.DateTo)

		{
			// COMBINATOR-3356 существует опция самовывоза
			require.True(t, respStats.OffersDelivery[0].SelfPickupAvailable)

			// отсекаем опции самовывоза
			for i := range GenDataDSBS.Outlets.OutletsSlice {
				GenDataDSBS.Outlets.OutletsSlice[i].OwnerPartnerID = 1234
			}
			reqStats = MakeStatsRequest(
				RequestWithStartTime(time.Date(2021, 3, 25, 14, 0, 1, 0, MskTZ)),
				RequestWithRegion(geobase.RegionMytishchi),
				RequestWithSkuOffer(&SkuOffer{
					ShopSku:    "DSBSoffer",
					ShopID:     322,
					PartnerID:  uint64(GraphExampleDSBS.WarehouseDSBS.PartnerLmsID),
					Weight:     1000,
					Dimensions: [3]uint32{10, 20, 30},
					MarketSku:  "DSBSoffer",
					Price:      3200,
				}),
			)
			respStats, err = env.Client.GetOffersDeliveryStats(env.Ctx, reqStats)
			require.NoError(t, err)
			require.NotEmpty(t, respStats.OffersDelivery)
			require.False(t, respStats.OffersDelivery[0].SelfPickupAvailable)

			// Возвращаем как было.
			for i := range GenDataDSBS.Outlets.OutletsSlice {
				GenDataDSBS.Outlets.OutletsSlice[i].OwnerPartnerID = 0
			}
		}

	}
}

func checkDeliveryServiceForDSBS(t *testing.T, respCour *pb.DeliveryOptionsForUser) {
	// COMBINATOR-2292 hard 99 for DSBS
	require.Equal(t, dsbs.SubstituteDeliveryServiceForDSBS, int(respCour.Options[0].DeliveryServiceId))
}

// WarehouseDSBS2(shop=505)
// Это магазин _без_ dsbs отдельных тарифов, но с тарифом в фиде.
func TestDaysFromRequest(t *testing.T) {
	env, cancel := NewEnv(t, &GenDataDSBS, nil)
	defer cancel()
	{
		// Request with days and no tariff
		req := MakeRequest(
			RequestWithStartTime(time.Date(2021, 3, 25, 12, 0, 0, 0, MskTZ)),
			RequestWithPartner(GraphExampleDSBS.WarehouseDSBS2.PartnerLmsID),
			RequestWithRegion(geobase.RegionMytishchi),
			RequestWithShopID(505),
			RequestWithDaysFrom(2),
			RequestWithDaysTo(3),
		)
		respDebug, err := env.Client.DebugFindPaths(env.Ctx, req)
		require.NoError(t, err)
		require.Len(t, respDebug.SortedPaths, 3)

		respCour, err := env.Client.GetCourierOptions(env.Ctx, req)
		require.NoError(t, err)
		require.Len(t, respCour.Options, 2)

		opt := respCour.Options[0]
		requirepb.Equal(t, &pb.Date{Day: 27, Month: 3, Year: 2021}, opt.DateFrom)
		requirepb.Equal(t, &pb.Date{Day: 27, Month: 3, Year: 2021}, opt.DateTo)
		requirepb.Equal(t, []pb.PaymentMethod{pb.PaymentMethod_PREPAYMENT, pb.PaymentMethod_CASH, pb.PaymentMethod_CARD}, opt.PaymentMethods)
		checkDeliveryServiceForDSBS(t, respCour)

		// Request with days and no tariff, ignore OrderBefore, because use_dsbs_dates_as_raw is False
		req = MakeRequest(
			RequestWithStartTime(time.Date(2021, 3, 25, 12, 0, 0, 0, MskTZ)),
			RequestWithPartner(GraphExampleDSBS.WarehouseDSBS2.PartnerLmsID),
			RequestWithRegion(geobase.RegionMytishchi),
			RequestWithShopID(505),
			RequestWithDaysFrom(2),
			RequestWithDaysTo(3),
			RequestWithOrderBefore(12),
		)
		respDebug, err = env.Client.DebugFindPaths(env.Ctx, req)
		require.NoError(t, err)
		require.Len(t, respDebug.SortedPaths, 3)

		respCour, err = env.Client.GetCourierOptions(env.Ctx, req)
		require.NoError(t, err)
		require.Len(t, respCour.Options, 2)

		opt = respCour.Options[0]
		requirepb.Equal(t, &pb.Date{Day: 28, Month: 3, Year: 2021}, opt.DateFrom)
		requirepb.Equal(t, &pb.Date{Day: 28, Month: 3, Year: 2021}, opt.DateTo)
		checkDeliveryServiceForDSBS(t, respCour)

		// Check DeliveryRoute request
		req2 := MakeRequest(
			RequestWithStartTime(time.Date(2021, 3, 25, 12, 0, 0, 0, MskTZ)),
			RequestWithPartner(GraphExampleDSBS.WarehouseDSBS2.PartnerLmsID),
			RequestWithRegion(geobase.RegionMytishchi),
			RequestWithShopID(505),
			RequestWithDaysFrom(2),
			RequestWithDaysTo(3),
			RequestWithDeliveryOption(
				&pb.Date{Day: 28, Month: 3, Year: 2021},
				&pb.Date{Day: 28, Month: 3, Year: 2021},
			),
		)

		respRoute, err := env.Client.GetDeliveryRoute(env.Ctx, req2)
		require.NoError(t, err)
		require.Len(t, respRoute.Route.Points, 4)

		// The same thing but for StatsRequest
		req3 := MakeStatsRequest(
			RequestWithStartTime(time.Date(2021, 3, 25, 12, 0, 0, 0, MskTZ)),
			RequestWithRegion(geobase.RegionMytishchi),
			RequestWithSkuOffer(&SkuOffer{
				ShopSku:    "DSBSoffer",
				ShopID:     505,
				PartnerID:  uint64(GraphExampleDSBS.WarehouseDSBS2.PartnerLmsID),
				Weight:     1000,
				Dimensions: [3]uint32{10, 20, 30},
				MarketSku:  "DSBSoffer",
				Price:      4200,
			}),
			RequestWithDaysFrom(2),
			RequestWithDaysTo(3),
		)
		respStats, err := env.Client.GetOffersDeliveryStats(env.Ctx, req3)

		require.NoError(t, err)
		require.Equal(t, 1, len(respStats.OffersDelivery))
		courierStats := respStats.OffersDelivery[0].CourierStats
		require.NotNil(t, courierStats)
		requirepb.Equal(t, &pb.Date{Day: 27, Month: 3, Year: 2021}, courierStats.DateFrom)
		requirepb.Equal(t, &pb.Date{Day: 27, Month: 3, Year: 2021}, courierStats.DateTo)

		pickupStats := respStats.OffersDelivery[0].PickupStats
		require.NotNil(t, pickupStats)
		requirepb.Equal(t, &pb.Date{Day: 27, Month: 3, Year: 2021}, pickupStats.DateFrom)
		requirepb.Equal(t, &pb.Date{Day: 28, Month: 3, Year: 2021}, pickupStats.DateTo)
	}
}

func TestDaysFromRequestRawData(t *testing.T) {
	env, cancel := NewEnv(t, &GenDataDSBS, nil)
	defer cancel()
	{
		// Request with days and no tariff
		req := MakeRequest(
			RequestWithStartTime(time.Date(2021, 3, 25, 12, 0, 0, 0, MskTZ)),
			RequestWithPartner(GraphExampleDSBS.WarehouseDSBS2.PartnerLmsID),
			RequestWithRegion(geobase.RegionMytishchi),
			RequestWithShopID(505),
			RequestWithDaysFrom(2),
			RequestWithDaysTo(3),
			RequestWithRearrFactors("use_dsbs_dates_as_raw=1"),
		)
		respDebug, err := env.Client.DebugFindPaths(env.Ctx, req)
		require.NoError(t, err)
		require.Len(t, respDebug.SortedPaths, 3)

		respCour, err := env.Client.GetCourierOptions(env.Ctx, req)
		require.NoError(t, err)
		require.Len(t, respCour.Options, 2)

		opt := respCour.Options[0]
		requirepb.Equal(t, &pb.Date{Day: 27, Month: 3, Year: 2021}, opt.DateFrom)
		requirepb.Equal(t, &pb.Date{Day: 27, Month: 3, Year: 2021}, opt.DateTo)
		requirepb.Equal(t, []pb.PaymentMethod{pb.PaymentMethod_PREPAYMENT, pb.PaymentMethod_CASH, pb.PaymentMethod_CARD}, opt.PaymentMethods)

		// Request with days and no tariff, but +1 day after OrderBefore
		req = MakeRequest(
			RequestWithStartTime(time.Date(2021, 3, 25, 12, 0, 0, 0, MskTZ)),
			RequestWithPartner(GraphExampleDSBS.WarehouseDSBS.PartnerLmsID),
			RequestWithRegion(geobase.RegionKotelniki),
			RequestWithShopID(322),
			RequestWithDaysFrom(4),
			RequestWithDaysTo(5),
			RequestWithOrderBefore(12),
			RequestWithItemPrice(6789), // MoscowAndObl tariff is also suitable
			RequestWithRearrFactors("use_dsbs_dates_as_raw=1"),
		)
		respDebug, err = env.Client.DebugFindPaths(env.Ctx, req)
		require.NoError(t, err)
		require.Len(t, respDebug.SortedPaths, 3)

		respCour, err = env.Client.GetCourierOptions(env.Ctx, req)
		require.NoError(t, err)
		require.Len(t, respCour.Options, 2)

		opt = respCour.Options[0]
		requirepb.Equal(t, &pb.Date{Day: 30, Month: 3, Year: 2021}, opt.DateFrom)
		requirepb.Equal(t, &pb.Date{Day: 30, Month: 3, Year: 2021}, opt.DateTo)
		requirepb.Equal(t, []pb.PaymentMethod{pb.PaymentMethod_PREPAYMENT}, opt.PaymentMethods)
		checkDeliveryServiceForDSBS(t, respCour)

		// Check DeliveryRoute request
		req2 := MakeRequest(
			RequestWithStartTime(time.Date(2021, 3, 25, 12, 0, 0, 0, MskTZ)),
			RequestWithPartner(GraphExampleDSBS.WarehouseDSBS2.PartnerLmsID),
			RequestWithRegion(geobase.RegionMytishchi),
			RequestWithShopID(505),
			RequestWithDaysFrom(2),
			RequestWithDaysTo(3),
			RequestWithDeliveryOption(
				&pb.Date{Day: 28, Month: 3, Year: 2021},
				&pb.Date{Day: 28, Month: 3, Year: 2021},
			),
			RequestWithRearrFactors("use_dsbs_dates_as_raw=1"),
		)

		respRoute, err := env.Client.GetDeliveryRoute(env.Ctx, req2)
		require.NoError(t, err)
		require.Len(t, respRoute.Route.Points, 4)

		// The same thing but for StatsRequest
		req3 := MakeStatsRequest(
			RequestWithStartTime(time.Date(2021, 3, 25, 12, 0, 0, 0, MskTZ)),
			RequestWithRegion(geobase.RegionMytishchi),
			RequestWithSkuOffer(&SkuOffer{
				ShopSku:    "DSBSoffer",
				ShopID:     505,
				PartnerID:  uint64(GraphExampleDSBS.WarehouseDSBS2.PartnerLmsID),
				Weight:     1000,
				Dimensions: [3]uint32{10, 20, 30},
				MarketSku:  "DSBSoffer",
				Price:      4200,
			}),
			RequestWithDaysFrom(2),
			RequestWithDaysTo(3),
			RequestWithOrderBefore(12),
			RequestWithRearrFactors("use_dsbs_dates_as_raw=0"),
		)
		respStats, err := env.Client.GetOffersDeliveryStats(env.Ctx, req3)

		require.NoError(t, err)
		require.Equal(t, 1, len(respStats.OffersDelivery))
		courierStats := respStats.OffersDelivery[0].CourierStats
		require.NotNil(t, courierStats)
		requirepb.Equal(t, &pb.Date{Day: 28, Month: 3, Year: 2021}, courierStats.DateFrom)
		requirepb.Equal(t, &pb.Date{Day: 28, Month: 3, Year: 2021}, courierStats.DateTo)

		pickupStats := respStats.OffersDelivery[0].PickupStats
		require.NotNil(t, pickupStats)
		requirepb.Equal(t, &pb.Date{Day: 28, Month: 3, Year: 2021}, pickupStats.DateFrom)
		requirepb.Equal(t, &pb.Date{Day: 29, Month: 3, Year: 2021}, pickupStats.DateTo)
	}
}

// DSBS and CPA offers in one request
func TestStatsMerged(t *testing.T) {
	env, cancel := NewEnv(t, &GenDataDSBS, nil)
	defer cancel()
	{
		req := MakeStatsRequest(
			RequestWithStartTime(time.Date(2021, 3, 25, 11, 59, 59, 0, MskTZ)),
			RequestWithRegion(geobase.RegionMoscow),
			RequestWithSkuOffer(&SkuOffer{
				ShopSku:    "DSBSoffer",
				PartnerID:  uint64(GraphExampleDSBS.WarehouseDSBS2.PartnerLmsID),
				Weight:     1000,
				Dimensions: [3]uint32{10, 20, 30},
				MarketSku:  "DSBSoffer",
				Price:      4200,
			}),
			RequestWithSkuOffer(&SkuOffer{
				ShopSku:    "CPAoffer",
				PartnerID:  uint64(GraphExampleDSBS.Warehouse172.PartnerLmsID),
				Weight:     1000,
				Dimensions: [3]uint32{10, 20, 30},
				MarketSku:  "CPAoffer",
				Price:      4300,
			}),
			RequestWithDaysFrom(2),
			RequestWithDaysTo(3),
			RequestWithOrderBefore(12),
		)
		respStats, err := env.Client.GetOffersDeliveryStats(env.Ctx, req)

		require.NoError(t, err)
		require.Equal(t, 2, len(respStats.OffersDelivery))
		sort.Slice(
			respStats.OffersDelivery,
			func(i, j int) bool {
				return respStats.OffersDelivery[i].Offer.ShopSku > respStats.OffersDelivery[j].Offer.ShopSku
			},
		)
		// Check DSBS
		courierStats := respStats.OffersDelivery[0].CourierStats
		require.NotNil(t, courierStats)
		requirepb.Equal(t, &pb.Date{Day: 27, Month: 3, Year: 2021}, courierStats.DateFrom)
		requirepb.Equal(t, &pb.Date{Day: 27, Month: 3, Year: 2021}, courierStats.DateTo)
		// Check CPA
		courierStats = respStats.OffersDelivery[1].CourierStats
		require.NotNil(t, courierStats)
		requirepb.Equal(t, &pb.Date{Day: 26, Month: 3, Year: 2021}, courierStats.DateFrom)
		requirepb.Equal(t, &pb.Date{Day: 27, Month: 3, Year: 2021}, courierStats.DateTo)

		// After hours slot for DSBS, so have +1 day there
		req = MakeStatsRequest(
			RequestWithStartTime(time.Date(2021, 3, 25, 14, 1, 0, 0, MskTZ)),
			RequestWithRegion(geobase.RegionMoscow),
			RequestWithSkuOffer(&SkuOffer{
				ShopSku:    "DSBSoffer",
				PartnerID:  uint64(GraphExampleDSBS.WarehouseDSBS2.PartnerLmsID),
				Weight:     1000,
				Dimensions: [3]uint32{10, 20, 30},
				MarketSku:  "DSBSoffer",
				Price:      4200,
			}),
			RequestWithSkuOffer(&SkuOffer{
				ShopSku:    "CPAoffer",
				PartnerID:  uint64(GraphExampleDSBS.Warehouse172.PartnerLmsID),
				Weight:     1000,
				Dimensions: [3]uint32{10, 20, 30},
				MarketSku:  "CPAoffer",
				Price:      4300,
			}),
			RequestWithDaysFrom(2),
			RequestWithDaysTo(3),
			RequestWithOrderBefore(14),
			RequestWithRearrFactors("use_dsbs_dates_as_raw=1"),
		)
		respStats, err = env.Client.GetOffersDeliveryStats(env.Ctx, req)

		require.NoError(t, err)
		require.Equal(t, 2, len(respStats.OffersDelivery))
		sort.Slice(
			respStats.OffersDelivery,
			func(i, j int) bool {
				return respStats.OffersDelivery[i].Offer.ShopSku > respStats.OffersDelivery[j].Offer.ShopSku
			},
		)
		// Check DSBS
		courierStats = respStats.OffersDelivery[0].CourierStats
		require.NotNil(t, courierStats)
		requirepb.Equal(t, &pb.Date{Day: 28, Month: 3, Year: 2021}, courierStats.DateFrom)
		requirepb.Equal(t, &pb.Date{Day: 28, Month: 3, Year: 2021}, courierStats.DateTo)
		// Check CPA
		courierStats = respStats.OffersDelivery[1].CourierStats
		require.NotNil(t, courierStats)
		requirepb.Equal(t, &pb.Date{Day: 26, Month: 3, Year: 2021}, courierStats.DateFrom)
		requirepb.Equal(t, &pb.Date{Day: 27, Month: 3, Year: 2021}, courierStats.DateTo)
	}
}

func createPickupPointsRequest(shopID int, partnerID int, partnerOptions []*pb.PartnerDeliveryOptions, rearr string) pb.PickupPointsRequest {
	startTime := time.Date(2020, 06, 02, 12, 4, 5, 0, time.UTC)
	return pb.PickupPointsRequest{
		StartTime: ToProtoTimestamp(startTime),
		Items: []*pb.DeliveryRequestItem{
			{
				RequiredCount: 1,
				Weight:        1000,
				Dimensions: []uint32{
					20,
					20,
					15,
				},
				AvailableOffers: []*pb.Offer{
					{
						ShopSku:         "322",
						ShopId:          uint32(shopID),
						PartnerId:       uint64(partnerID),
						AvailableCount:  1,
						DeliveryOptions: partnerOptions,
					},
				},
				CargoTypes: []uint32{1, 2, 3, 4},
			},
			{
				RequiredCount: 2,
				Weight:        400,
				Dimensions: []uint32{
					15,
					18,
					20,
				},
				AvailableOffers: []*pb.Offer{
					{
						ShopSku:         "",
						ShopId:          uint32(shopID),
						PartnerId:       uint64(partnerID),
						AvailableCount:  3,
						DeliveryOptions: partnerOptions,
					},
				},
			},
		},
		DestinationRegions: []uint32{uint32(geobase.RegionMytishchi)},
		PostCodes:          nil,
		RearrFactors:       rearr,
	}
}

func TestDSBSGetPickupPointsGrouped(t *testing.T) {
	env, cancel := NewEnv(t, &GenDataDSBS, nil)
	defer cancel()

	req := createPickupPointsRequest(322, 4321, nil, "")
	resp, err := env.Client.GetPickupPointsGrouped(env.Ctx, &req)

	require.NoError(t, err)
	require.Equal(t, 3, len(resp.Groups))
	checkPickupPointGroup(t, geobase.RegionMytishchi, []uint64{203040, 304050}, resp.Groups[0], true)
	checkPickupPointGroup(t, geobase.RegionMytishchi, []uint64{102030}, resp.Groups[1], true)
	requirepb.Equal(t, []pb.PaymentMethod{pb.PaymentMethod_PREPAYMENT, pb.PaymentMethod_CASH},
		resp.Groups[0].PaymentMethods)
	requirepb.Equal(t, []pb.PaymentMethod{pb.PaymentMethod_PREPAYMENT, pb.PaymentMethod_CASH},
		resp.Groups[1].PaymentMethods)

	req = createPickupPointsRequest(322, 4321, nil, "use_payment_methods_from_dsbs_tariff=1")
	resp, err = env.Client.GetPickupPointsGrouped(env.Ctx, &req)

	require.NoError(t, err)
	require.Equal(t, 3, len(resp.Groups))
	checkPickupPointGroup(t, geobase.RegionMytishchi, []uint64{203040, 304050}, resp.Groups[0], true)
	checkPickupPointGroup(t, geobase.RegionMytishchi, []uint64{102030}, resp.Groups[1], true)
	requirepb.Equal(t, []pb.PaymentMethod{pb.PaymentMethod_PREPAYMENT, pb.PaymentMethod_CASH, pb.PaymentMethod_CARD},
		resp.Groups[0].PaymentMethods)
	requirepb.Equal(t, []pb.PaymentMethod{pb.PaymentMethod_CASH},
		resp.Groups[1].PaymentMethods)

	req = createPickupPointsRequest(505, 4322, nil, "")
	resp, err = env.Client.GetPickupPointsGrouped(env.Ctx, &req)

	require.NoError(t, err)
	require.Equal(t, 2, len(resp.Groups))
	checkPickupPointGroup(t, geobase.RegionMytishchi, []uint64{102030, 102040}, resp.Groups[0], true)

	partnerOpts := []*pb.PartnerDeliveryOptions{
		&pb.PartnerDeliveryOptions{
			DeliveryType: pb.DeliveryType_PICKUP,
			DayFrom:      1,
			DayTo:        1,
			OrderBefore:  24,
		},
	}
	req = createPickupPointsRequest(505, 4322, partnerOpts, "")
	resp, err = env.Client.GetPickupPointsGrouped(env.Ctx, &req)

	require.NoError(t, err)
	require.Equal(t, 1, len(resp.Groups))
	checkPickupPointGroup(t, geobase.RegionMytishchi, []uint64{102030, 102040, 203040, 304050}, resp.Groups[0], true)

	req = createPickupPointsRequest(505, 4322, partnerOpts, "")
	resp, err = env.Client.GetPickupPointsGrouped(env.Ctx, &req)

	require.NoError(t, err)
	require.Equal(t, 1, len(resp.Groups))
	checkPickupPointGroup(t, geobase.RegionMytishchi, []uint64{102030, 102040, 203040, 304050}, resp.Groups[0], true)

	// return market branded depots if courier partner option exists
	partnerOpts[0].DeliveryType = pb.DeliveryType_COURIER
	req = createPickupPointsRequest(322, 4321, partnerOpts, "combinator_dsbs_branded_depots=1;merge_dsbs_and_market_branded_pickup=0;use_yt_dsbs_outlet_shops=1")
	resp, err = env.Client.GetPickupPointsGrouped(env.Ctx, &req)

	require.NoError(t, err)
	require.Equal(t, 1, len(resp.Groups))
	checkPickupPointGroup(t, geobase.RegionMytishchi, []uint64{404050}, resp.Groups[0], false)

	req = createPickupPointsRequest(322, 4321, partnerOpts, "combinator_dsbs_branded_depots=1;merge_dsbs_and_market_branded_pickup=1;use_yt_dsbs_outlet_shops=1")
	resp, err = env.Client.GetPickupPointsGrouped(env.Ctx, &req)

	require.NoError(t, err)
	require.Equal(t, 3, len(resp.Groups))
	checkPickupPointGroup(t, geobase.RegionMytishchi, []uint64{203040, 304050}, resp.Groups[0], true)
	checkPickupPointGroup(t, geobase.RegionMytishchi, []uint64{102030}, resp.Groups[1], true)
	checkPickupPointGroup(t, geobase.RegionMytishchi, []uint64{404050}, resp.Groups[2], false)
}

func TestDisabledDirection(t *testing.T) {
	env, cancel := NewEnv(t, &GenDataDSBS, nil)
	defer cancel()
	{
		req := MakeRequest(
			RequestWithStartTime(time.Date(2021, 3, 25, 12, 0, 0, 0, MskTZ)),
			RequestWithPartner(GraphExampleDSBS.WarehouseDSBS.PartnerLmsID),
			RequestWithRegion(geobase.RegionHamovniki),
			RequestWithShopID(322),
			RequestWithDaysFrom(4),
			RequestWithDaysTo(5),
			RequestWithOrderBefore(12),
			RequestWithItemPrice(6789), // MoscowAndObl tariff is also suitable
			RequestWithRearrFactors("use_dsbs_dates_as_raw=1"),
		)
		respDebug, err := env.Client.DebugFindPaths(env.Ctx, req)
		require.NoError(t, err)
		require.Len(t, respDebug.SortedPaths, 1)
	}
}

func checkPickupPointGroup(
	t *testing.T,
	regionID geobase.RegionID,
	pointIDs []uint64,
	group *pb.PickupPointsGrouped_Group,
	dsbsPointNotEmpty bool,
) {
	require.Equal(t, len(pointIDs), len(group.Points))
	for i, pointID := range pointIDs {
		require.Equal(t, int(pointID), int(group.Points[i].LogisticPointId))
		if dsbsPointNotEmpty {
			require.Equal(t, "test"+strconv.FormatUint(pointID, 10), group.Points[i].DsbsPointId)
		} else {
			require.Equal(t, "", group.Points[i].DsbsPointId)
		}
		require.Equal(t, regionID, geobase.RegionID(group.Points[i].RegionId))
	}
	if dsbsPointNotEmpty {
		require.Equal(t, dsbs.SubstituteDeliveryServiceForDSBS, int(group.ServiceId)) // COMBINATOR-2292 hard 99 for DSBS
	} else {
		require.NotEqual(t, dsbs.SubstituteDeliveryServiceForDSBS, int(group.ServiceId)) // COMBINATOR-2292 hard 99 for DSBS
	}
}

// Need to set defaults for some fields
func patchDsbsTariff(dt *dsbs.ShopCourierTariffRT) *dsbs.ShopCourierTariffRT {
	if dt == nil {
		return dt
	}
	if dt.WeightTo == 0 {
		dt.WeightTo = math.MaxUint32
	}
	if dt.PriceTo == 0 {
		dt.PriceTo = math.MaxUint64
	}
	return dt
}

func getTariffMytyshchi(additionalDays uint16) dsbs.CommonTariffRT {
	return dsbs.CommonTariffRT{
		ShopID:          322,
		PartnerID:       4321,
		DaysFrom:        3 + additionalDays,
		DaysTo:          4 + additionalDays,
		OrderBeforeHour: 14,
	}
}

func getCommonTariffMytyshchi() dsbs.CommonTariffRT {
	return getTariffMytyshchi(uint16(0))
}

func getCommonTariffMoscowAndObl() dsbs.CommonTariffRT {
	return dsbs.CommonTariffRT{
		ShopID:          322,
		PartnerID:       4321,
		DaysFrom:        2,
		DaysTo:          3,
		OrderBeforeHour: 14,
	}
}

func getCommonTariffKotelnikiFeed() dsbs.CommonTariffRT {
	return dsbs.CommonTariffRT{
		ShopID:          322,
		PartnerID:       4321,
		DaysFrom:        90,
		DaysTo:          90,
		OrderBeforeHour: 24,
	}
}

func getCommonTariffMoscowAndOblFarAway() dsbs.CommonTariffRT {
	return dsbs.CommonTariffRT{
		ShopID:          322,
		PartnerID:       4321,
		DaysFrom:        4,
		DaysTo:          5,
		OrderBeforeHour: 12,
	}
}

func getCommonTariffSofyno() dsbs.CommonTariffRT {
	return dsbs.CommonTariffRT{
		ShopID:          322,
		PartnerID:       4321,
		DaysFrom:        2,
		DaysTo:          5,
		OrderBeforeHour: 14,
	}
}

func getDsbsCourierTariffs() dsbs.ShopCourierTariffMap {
	return dsbs.ShopCourierTariffMap{
		dsbs.CourierTariffKey{
			PartnerID: 4321, // ShopID: 322,
			From:      geobase.RegionMytishchi,
			To:        geobase.RegionMytishchi,
		}: []*dsbs.ShopCourierTariffRT{
			patchDsbsTariff(&dsbs.ShopCourierTariffRT{
				CommonTariffRT: dsbs.CommonTariffRT{
					ShopID:          322,
					PartnerID:       4321,
					DaysFrom:        3,
					DaysTo:          4,
					OrderBeforeHour: 14,
					PaymentMethods:  enums.AllPaymentMethods,
				},
				PriceTo:     5000,
				HasDelivery: true,
			}),
		},
		dsbs.CourierTariffKey{
			PartnerID: 4321, // ShopID: 322,
			From:      geobase.RegionMytishchi,
			To:        geobase.RegionMoscowAndObl,
		}: []*dsbs.ShopCourierTariffRT{
			patchDsbsTariff(&dsbs.ShopCourierTariffRT{
				CommonTariffRT: dsbs.CommonTariffRT{
					ShopID:          322,
					PartnerID:       4321,
					DaysFrom:        2,
					DaysTo:          3,
					OrderBeforeHour: 14,
					PaymentMethods:  enums.MethodPrepayAllowed,
				},
				PriceFrom:   5000,
				PriceTo:     10000,
				HasDelivery: true,
			}),
		},
		dsbs.CourierTariffKey{
			PartnerID: 4321, // ShopID: 322
			From:      geobase.RegionMytishchi,
			To:        geobase.RegionKotelniki,
		}: []*dsbs.ShopCourierTariffRT{
			patchDsbsTariff(&dsbs.ShopCourierTariffRT{
				CommonTariffRT: dsbs.CommonTariffRT{
					ShopID:          322,
					PartnerID:       4321,
					DaysFrom:        90,
					DaysTo:          90,
					OrderBeforeHour: 24,
					PaymentMethods:  enums.MethodPrepayAllowed,
				},
				HasDelivery: true,
				TariffType:  dsbs.TariffFromFeed,
			}),
		},
		dsbs.CourierTariffKey{
			PartnerID: 4321, // ShopID: 322
			From:      geobase.RegionMytishchi,
			To:        geobase.RegionHamovniki,
		}: []*dsbs.ShopCourierTariffRT{
			patchDsbsTariff(&dsbs.ShopCourierTariffRT{
				CommonTariffRT: getCommonTariffMytyshchi(),
				HasDelivery:    false, // disabled direction!!!
			}),
		},
		dsbs.CourierTariffKey{
			PartnerID: 4322, // ShopID: 505
			From:      geobase.RegionMytishchi,
			To:        geobase.RegionMytishchi,
		}: []*dsbs.ShopCourierTariffRT{
			patchDsbsTariff(&dsbs.ShopCourierTariffRT{
				CommonTariffRT: dsbs.CommonTariffRT{
					ShopID:          505,
					PartnerID:       4322,
					DaysFrom:        0,
					DaysTo:          0,
					OrderBeforeHour: 14,
				},
				HasDelivery: true,
				TariffType:  dsbs.TariffFromFeed,
			}),
		},
		dsbs.CourierTariffKey{
			PartnerID: 4322, // ShopID: 505
			From:      geobase.RegionMytishchi,
			To:        geobase.RegionMoscow,
		}: []*dsbs.ShopCourierTariffRT{
			patchDsbsTariff(&dsbs.ShopCourierTariffRT{
				CommonTariffRT: dsbs.CommonTariffRT{
					ShopID:          505,
					PartnerID:       4322,
					DaysFrom:        0,
					DaysTo:          0,
					OrderBeforeHour: 14,
				},
				HasDelivery: true,
				TariffType:  dsbs.TariffFromFeed,
			}),
		},
	}
}

func getDsbsPickupPointTariffs() dsbs.ShopPickupPointTariffMap {
	return dsbs.ShopPickupPointTariffMap{
		dsbs.CreatePickupPointTariffKey(geobase.RegionMoscowAndObl, uint64(4321)): []*dsbs.ShopPickupPointTariffRT{
			&dsbs.ShopPickupPointTariffRT{
				CommonTariffRT: dsbs.CommonTariffRT{
					ShopID:          322,
					PartnerID:       4321,
					DaysFrom:        2,
					DaysTo:          3,
					OrderBeforeHour: 14,
					PaymentMethods:  enums.MethodPrepayAllowed | enums.MethodCashAllowed | enums.MethodCardAllowed,
				},
				RegionID:        geobase.RegionMoscowAndObl,
				LogisticsPoints: []int64{203040, 304050},
			},
			&dsbs.ShopPickupPointTariffRT{
				CommonTariffRT: dsbs.CommonTariffRT{
					ShopID:          322,
					PartnerID:       4321,
					DaysFrom:        4,
					DaysTo:          5,
					OrderBeforeHour: 12,
					PaymentMethods:  enums.MethodCashAllowed,
				},
				RegionID:        geobase.RegionMoscowAndObl,
				LogisticsPoints: []int64{102030},
			},
		}, // shop 322
		dsbs.CreatePickupPointTariffKey(geobase.RegionSofyno, uint64(4321)): []*dsbs.ShopPickupPointTariffRT{
			&dsbs.ShopPickupPointTariffRT{
				CommonTariffRT: dsbs.CommonTariffRT{
					ShopID:          322,
					PartnerID:       4321,
					DaysFrom:        2,
					DaysTo:          5,
					OrderBeforeHour: 14,
					PaymentMethods:  enums.MethodPrepayAllowed | enums.MethodCashAllowed,
				},
				RegionID:        geobase.RegionSofyno,
				LogisticsPoints: []int64{304050},
			},
		}, // shop 322
		dsbs.CreatePickupPointTariffKey(geobase.RegionMytishchi, uint64(4322)): []*dsbs.ShopPickupPointTariffRT{
			&dsbs.ShopPickupPointTariffRT{
				CommonTariffRT: dsbs.CommonTariffRT{
					ShopID:          322,
					PartnerID:       4322,
					DaysFrom:        3,
					DaysTo:          4,
					OrderBeforeHour: 14,
					PaymentMethods:  enums.MethodPrepayAllowed | enums.MethodCashAllowed,
				},
				RegionID:        geobase.RegionMytishchi,
				LogisticsPoints: []int64{102030, 102040},
			},
			&dsbs.ShopPickupPointTariffRT{
				CommonTariffRT: dsbs.CommonTariffRT{
					ShopID:          322,
					PartnerID:       4322,
					DaysFrom:        6,
					DaysTo:          7,
					OrderBeforeHour: 14,
					PaymentMethods:  enums.MethodPrepayAllowed | enums.MethodCashAllowed,
				},
				RegionID:        geobase.RegionMytishchi,
				LogisticsPoints: []int64{203040, 304050},
			},
		}, // shop 505
	}
}

func getDsbsOutlets(regionMap *geobase.RegionMap) *outlets.OutletStorage {
	return outlets.Make([]outlets.Outlet{
		outlets.Outlet{
			ID:                        102030,
			Type:                      outlets.Depot,
			PostCode:                  123321,
			RegionID:                  geobase.RegionMytishchi,
			DeliveryServiceOutletCode: "test102030",
			DsbsPointID:               102030,
		},
		outlets.Outlet{
			ID:                        102040,
			Type:                      outlets.Depot,
			PostCode:                  123321,
			RegionID:                  geobase.RegionMytishchi,
			DeliveryServiceOutletCode: "test102040",
			DsbsPointID:               102040,
		},
		outlets.Outlet{
			ID:                        203040,
			Type:                      outlets.Depot,
			PostCode:                  123321,
			RegionID:                  geobase.RegionMytishchi,
			DeliveryServiceOutletCode: "test203040",
			DsbsPointID:               203040,
		},
		outlets.Outlet{
			ID:                        304050,
			Type:                      outlets.Depot,
			PostCode:                  123321,
			RegionID:                  geobase.RegionMytishchi,
			DeliveryServiceOutletCode: "test304050",
			DsbsPointID:               304050,
			IsActive:                  true,
			GpsCoords: units.GpsCoords{
				Latitude:  40.0,
				Longitude: 30.0,
			},
		},
		outlets.Outlet{
			ID:                        404050,
			Type:                      outlets.Depot,
			PostCode:                  123321,
			RegionID:                  geobase.RegionMoscow,
			DeliveryServiceOutletCode: "test404050",
			DsbsPointID:               0,
			IsMarketBranded:           true,
			IsActive:                  true,
			GpsCoords: units.GpsCoords{
				Latitude:  55.705468,
				Longitude: 37.669585,
			},
		},
	}, regionMap, nil)
}

func init() {
	gb := graph.NewGraphBuilder()
	GraphExampleDSBS = graph.BuildExamplePickupDSBS(gb)
	tariffsBuilder := tr.NewTariffsBuilder()
	_ = tariffsBuilder.MakeTariff(
		tr.TariffWithPartner(uint64(GraphExampleDSBS.Linehaul.PartnerLmsID)),
		tr.TariffWithRegion(geobase.RegionMoscowAndObl, geobase.RegionMoscow),
		tr.TariffWithDays(uint32(1), uint32(2)),
	)
	_ = tariffsBuilder.MakeTariff(
		tr.TariffWithPartner(1005555),
		tr.TariffWithRegion(geobase.RegionMoscow, geobase.RegionMoscow),
		tr.TariffWithDays(uint32(1), uint32(2)),
	)
	_ = tariffsBuilder.MakeTariff(
		tr.TariffWithPartner(1005555),
		tr.TariffWithRegion(geobase.RegionMoscow, geobase.RegionSaintPetersburg),
		tr.TariffWithDays(uint32(1), uint32(2)),
	)
	_ = tariffsBuilder.MakeTariff(
		tr.TariffWithPartner(4401),
		tr.TariffWithRegion(geobase.RegionMoscow, geobase.RegionMoscow),
		tr.TariffWithDays(1, 1),
	)
	regionMap := geobase.NewExample()
	GenDataDSBS = bg.GenerationData{
		RegionMap:              regionMap,
		Graph:                  GraphExampleDSBS.Graph,
		DsbsCourierTariffs:     getDsbsCourierTariffs(),
		DsbsPickupPointTariffs: getDsbsPickupPointTariffs(),
		DsbsToOutletShops: dsbs.ShopSet{
			322: struct{}{},
			505: struct{}{},
		},
		Outlets:       getDsbsOutlets(&regionMap),
		TariffsFinder: tariffsBuilder.TariffsFinder,
	}
}
