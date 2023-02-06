package deferredcourierlite

import (
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/test/requirepb"
	bg "a.yandex-team.ru/market/combinator/pkg/biggeneration"
	"a.yandex-team.ru/market/combinator/pkg/daysoff"
	"a.yandex-team.ru/market/combinator/pkg/geobase"
	httpclient "a.yandex-team.ru/market/combinator/pkg/http_client"
	"a.yandex-team.ru/market/combinator/pkg/its"
	"a.yandex-team.ru/market/combinator/pkg/lite"
	"a.yandex-team.ru/market/combinator/pkg/outlets"
	"a.yandex-team.ru/market/combinator/pkg/units"
	"a.yandex-team.ru/market/combinator/pkg/util"
	pb "a.yandex-team.ru/market/combinator/proto/grpc"
)

func TestDeferredCourierOptions(t *testing.T) {
	settings, _ := its.NewStringSettingsHolder("{\"enable_sort_courier_options\":true}")
	regionMap := geobase.NewExample()
	graphEx, disabledDates := makeDeferredCourierGraph(settings)
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
	genDaysOff := &daysoff.ServiceDaysOff{
		Services: map[int64]daysoff.DisabledDatesMap{
			3000002: {
				util.StrDateToInt("2021-06-03"): daysoff.DisabledDate{},
				util.StrDateToInt("2021-06-02"): daysoff.DisabledDate{},
				util.StrDateToInt("2021-06-01"): daysoff.DisabledDate{}, // disabledOffset
			},
		},
	}
	generationData.AddDaysOff(genDaysOff)

	generationData.TariffsFinder.Common.PointRegionMap = map[int64]geobase.RegionID{
		goPlatformDarkPointLmsID: geobase.RegionMoscow,
		goPlatformPointLmsID:     geobase.RegionMoscow,
	}
	env, cancel := lite.NewEnv(t, &generationData, settings)
	defer cancel()
	PrepareHTTPClient(env)

	startTime := time.Date(2021, 6, 4, 12, 0, 0, 0, lite.MskTZ)
	const courierOptionCount = 5
	const deferredCourierOptionDaysCount = 5
	const deferredCourierOptionPerDayCount = 8
	const deferredCourierOptionFirstDaysCount = 8

	// GetCourierOptions
	{
		//GPS координаты находятся в радиусе действия 1 пункта доставки GO
		req := lite.MakeRequest(
			lite.RequestWithStartTime(startTime),
			lite.RequestWithDeliveryType(pb.DeliveryType_COURIER),
			lite.RequestWithPartner(ffWarehouseID),
			lite.RequestWithRegion(geobase.RegionMoscow),
			lite.RequestWithRearrFactors("market_combinator_deferred_courier_options=1;use_yandex_go_in_deferred_courier=1;"),
			lite.RequestWithGpsCoords(55.722613, 37.555378),
			lite.RequestWithUserInfo(true),
		)
		resp, err := env.Client.GetCourierOptions(env.Ctx, req)
		require.NoError(t, err)
		require.Len(t, resp.Options, courierOptionCount+deferredCourierOptionFirstDaysCount+deferredCourierOptionPerDayCount*(deferredCourierOptionDaysCount-1))
		testFirstDay(t, deferredCourierOptionFirstDaysCount, resp)
		testSecondDay(t, deferredCourierOptionFirstDaysCount, deferredCourierOptionPerDayCount, deferredCourierOptionDaysCount, resp)
		testCourier(t, courierOptionCount, deferredCourierOptionPerDayCount, resp)

		dsResp, dsErr := env.Client.GetCourierDeliveryStatistics(env.Ctx, req)
		require.NoError(t, dsErr)
		require.Len(t, dsResp.DeliveryMethods, 1)
		require.Equal(t, pb.DeliveryMethod_DM_COURIER, dsResp.DeliveryMethods[0])
	}
	{
		//GPS координаты находятся в радиусе действия 2 пункта доставки GO
		req := lite.MakeRequest(
			lite.RequestWithStartTime(startTime),
			lite.RequestWithDeliveryType(pb.DeliveryType_COURIER),
			lite.RequestWithPartner(ffWarehouseID),
			lite.RequestWithRegion(geobase.RegionMoscow),
			lite.RequestWithRearrFactors("market_combinator_deferred_courier_options=1;use_yandex_go_in_deferred_courier=1;"),
			lite.RequestWithGpsCoords(55.122613, 37.855378),
			lite.RequestWithUserInfo(true),
		)
		resp, err := env.Client.GetCourierOptions(env.Ctx, req)
		require.NoError(t, err)

		require.Len(t, resp.Options, courierOptionCount+deferredCourierOptionFirstDaysCount+deferredCourierOptionPerDayCount*(deferredCourierOptionDaysCount-1))

		testFirstDay(t, deferredCourierOptionFirstDaysCount, resp)
		testSecondDay(t, deferredCourierOptionFirstDaysCount, deferredCourierOptionPerDayCount, deferredCourierOptionDaysCount, resp)
		testCourier(t, courierOptionCount, deferredCourierOptionPerDayCount, resp)

		dsResp, dsErr := env.Client.GetCourierDeliveryStatistics(env.Ctx, req)
		require.NoError(t, dsErr)
		require.Len(t, dsResp.DeliveryMethods, 1)
		require.Equal(t, pb.DeliveryMethod_DM_COURIER, dsResp.DeliveryMethods[0])
	}
	{
		//Тест с дейофами
		//GPS координаты находятся в радиусе действия 2 пункта доставки GO
		startTime := time.Date(2021, 6, 2, 12, 0, 0, 0, lite.MskTZ)
		req := lite.MakeRequest(
			lite.RequestWithStartTime(startTime),
			lite.RequestWithDeliveryType(pb.DeliveryType_COURIER),
			lite.RequestWithPartner(ffWarehouseID),
			lite.RequestWithRegion(geobase.RegionMoscow),
			lite.RequestWithRearrFactors("market_combinator_deferred_courier_options=1;use_yandex_go_in_deferred_courier=1;"),
			lite.RequestWithGpsCoords(55.122613, 37.855378),
			lite.RequestWithUserInfo(true),
		)
		resp, err := env.Client.GetCourierOptions(env.Ctx, req)
		require.NoError(t, err)

		require.Len(t, resp.Options, courierOptionCount+deferredCourierOptionFirstDaysCount+deferredCourierOptionPerDayCount*(deferredCourierOptionDaysCount-1))

		for j := 0; j < deferredCourierOptionFirstDaysCount; j++ {
			option := resp.Options[j+2]
			requirepb.Equal(t, pb.DeliverySubtype_DEFERRED_COURIER, option.GetDeliverySubtype())
			requirepb.Equal(t, &pb.Date{Year: 2021, Month: 6, Day: uint32(6)}, option.DateFrom)
			requirepb.Equal(t, &pb.Date{Year: 2021, Month: 6, Day: uint32(6)}, option.DateTo)
			requirepb.Equal(t, &pb.Time{Hour: uint32(8 + j)}, option.Interval.From)
			requirepb.Equal(t, &pb.Time{Hour: uint32(9 + j)}, option.Interval.To)
			require.Equal(t, yGoServiceID, int(option.DeliveryServiceId))
			requirepb.Equal(t, []pb.PaymentMethod{pb.PaymentMethod_PREPAYMENT}, option.PaymentMethods)
		}

		dsResp, dsErr := env.Client.GetCourierDeliveryStatistics(env.Ctx, req)
		require.NoError(t, dsErr)
		require.Len(t, dsResp.DeliveryMethods, 1)
		require.Equal(t, pb.DeliveryMethod_DM_COURIER, dsResp.DeliveryMethods[0])
		//for i := 1; i < deferredCourierOptionDaysCount; i++ {
		//	for j := 0; j < deferredCourierOptionPerDayCount; j++ {
		//		option := resp.Options[(i-1)*deferredCourierOptionPerDayCount+deferredCourierOptionFirstDaysCount+j+3]
		//		requirepb.Equal(t, pb.DeliverySubtype_DEFERRED_COURIER, option.GetDeliverySubtype())
		//		requirepb.Equal(t, &pb.Date{Year: 2021, Month: 6, Day: uint32(6 + i)}, option.DateFrom)
		//		requirepb.Equal(t, &pb.Date{Year: 2021, Month: 6, Day: uint32(6 + i)}, option.DateTo)
		//		requirepb.Equal(t, &pb.Time{Hour: uint32(8 + j)}, option.Interval.From)
		//		requirepb.Equal(t, &pb.Time{Hour: uint32(9 + j)}, option.Interval.To)
		//		require.Equal(t, yGoServiceID, int(option.DeliveryServiceId))
		//		requirepb.Equal(t, []pb.PaymentMethod{pb.PaymentMethod_PREPAYMENT}, option.PaymentMethods)
		//	}
		//}
		//for i := 0; i < courierOptionCount-1; i++ {
		//	option := resp.Options[i]
		//	requirepb.Equal(t, pb.DeliverySubtype_ORDINARY, option.GetDeliverySubtype())
		//	requirepb.Equal(t, &pb.Date{Year: 2021, Month: 6, Day: uint32(3 + i)}, option.DateFrom)
		//	requirepb.Equal(t, &pb.Date{Year: 2021, Month: 6, Day: uint32(3 + i)}, option.DateTo)
		//	requirepb.Equal(t, &pb.Time{Hour: 16}, option.Interval.From)
		//	requirepb.Equal(t, &pb.Time{Hour: 22}, option.Interval.To)
		//	require.Equal(t, lastMileDeliveryServiceID, int(option.DeliveryServiceId))
		//}
		//{
		//	i := courierOptionCount - 1 + deferredCourierOptionFirstDaysCount
		//	option := resp.Options[i]
		//	requirepb.Equal(t, pb.DeliverySubtype_ORDINARY, option.GetDeliverySubtype())
		//	requirepb.Equal(t, &pb.Date{Year: 2021, Month: 6, Day: uint32(7)}, option.DateFrom)
		//	requirepb.Equal(t, &pb.Date{Year: 2021, Month: 6, Day: uint32(7)}, option.DateTo)
		//	requirepb.Equal(t, &pb.Time{Hour: 16}, option.Interval.From)
		//	requirepb.Equal(t, &pb.Time{Hour: 22}, option.Interval.To)
		//	require.Equal(t, lastMileDeliveryServiceID, int(option.DeliveryServiceId))
		//}
	}
	{
		//GPS координаты не находятся в радиусе действия ни одного пункта доставки GO
		req := lite.MakeRequest(
			lite.RequestWithStartTime(startTime),
			lite.RequestWithDeliveryType(pb.DeliveryType_COURIER),
			lite.RequestWithPartner(ffWarehouseID),
			lite.RequestWithRegion(geobase.RegionMoscow),
			lite.RequestWithRearrFactors("market_combinator_deferred_courier_options=1;use_yandex_go_in_deferred_courier=1"),
			lite.RequestWithGpsCoords(55.782613, 37.525378),
			lite.RequestWithUserInfo(true),
		)
		resp, err := env.Client.GetCourierOptions(env.Ctx, req)
		require.NoError(t, err)

		require.Len(t, resp.Options, courierOptionCount)
		for i := 0; i < courierOptionCount; i++ {
			option := resp.Options[i]
			requirepb.Equal(t, pb.DeliverySubtype_ORDINARY, option.GetDeliverySubtype())
			requirepb.Equal(t, &pb.Date{Year: 2021, Month: 6, Day: uint32(5 + i)}, option.DateFrom)
			requirepb.Equal(t, &pb.Date{Year: 2021, Month: 6, Day: uint32(5 + i)}, option.DateTo)
			requirepb.Equal(t, &pb.Time{Hour: 16}, option.Interval.From)
			requirepb.Equal(t, &pb.Time{Hour: 22}, option.Interval.To)
			require.Equal(t, lastMileDeliveryServiceID, int(option.DeliveryServiceId))
		}

		dsResp, dsErr := env.Client.GetCourierDeliveryStatistics(env.Ctx, req)
		require.NoError(t, dsErr)
		require.Len(t, dsResp.DeliveryMethods, 1)
		require.Equal(t, pb.DeliveryMethod_DM_COURIER, dsResp.DeliveryMethods[0])
	}
}

func PrepareHTTPClient(env *lite.Env) {
	yTaxi := env.HTTPClient.YTaxi.(*lite.YTaxiClientMock)
	destTimes := make([]time.Time, 0, 5)
	sourceTimes := make([]time.Time, 0, 5)

	destTimes = append(destTimes, time.Date(2021, 06, 05, 23, 59, 0, 0, time.FixedZone("UTC+3", 3*60*60)))
	sourceTimes = append(sourceTimes, time.Date(2021, 06, 06, 8, 00, 0, 0, time.FixedZone("UTC+3", 3*60*60)))
	for i := 1; i < 5; i++ {
		destTimes = append(destTimes, time.Date(2021, 06, 05+i, 23, 59, 0, 0, time.FixedZone("UTC+3", 3*60*60)))
		sourceTimes = append(sourceTimes, time.Date(2021, 06, 06+i, 8, 00, 0, 0, time.FixedZone("UTC+3", 3*60*60)))
	}
	allOkNoID := make([]httpclient.YTaxiAvailableOffer, 0, 30)
	for i := 0; i < 40; i++ {
		allOkNoID = append(allOkNoID, httpclient.YTaxiAvailableOffer{Index: i, IndexRaw: float64(i)})
	}

	{
		offers := lite.MakeYTaxiOffers(
			goPlatformPointLmsID,
			false,
			units.GpsCoords{Latitude: 55.722613, Longitude: 37.555378},
			destTimes,
			units.GpsCoords{Latitude: 55.722613, Longitude: 37.555378},
			sourceTimes,
			8,
			8,
		)
		yTaxi.
			OnCheckAvailableOffersRequest(lite.MakeAvailableOffersRequestBody(
				offers,
				1024,
				[3]uint32{11, 22, 33},
				httpclient.PolicyTimeInterval,
				false,
			)).
			RespondWithAvailableOffers(&httpclient.CheckOffersResponse{AvailableOffers: allOkNoID})
	}
	{
		offers := lite.MakeYTaxiOffers(
			goPlatformDarkPointLmsID,
			true,
			units.GpsCoords{Latitude: 55.122613, Longitude: 37.855378},
			destTimes,
			units.GpsCoords{Latitude: 55.122613, Longitude: 37.855378},
			sourceTimes,
			8,
			8,
		)
		yTaxi.
			OnCheckAvailableOffersRequest(lite.MakeAvailableOffersRequestBody(
				offers,
				1024,
				[3]uint32{11, 22, 33},
				httpclient.PolicyTimeInterval,
				false,
			)).
			RespondWithAvailableOffers(&httpclient.CheckOffersResponse{AvailableOffers: allOkNoID})
	}
	{
		destTimes := make([]time.Time, 0, 1)
		sourceTimes := make([]time.Time, 0, 1)
		destTimes = append(destTimes, time.Date(2020, 06, 03, 11, 00, 0, 0, time.FixedZone("UTC+3", 3*60*60)))
		sourceTimes = append(sourceTimes, time.Date(2020, 06, 03, 12, 00, 0, 0, time.FixedZone("UTC+3", 3*60*60)))
		offers := lite.MakeYTaxiOffers(
			goPlatformDarkPointLmsID,
			true,
			units.GpsCoords{Latitude: 55.122613, Longitude: 37.855378},
			destTimes,
			units.GpsCoords{Latitude: 55.122613, Longitude: 37.855378},
			sourceTimes,
			1,
			0,
		)

		yTaxi.
			OnCheckAvailableOffersRequest(lite.MakeAvailableOffersRequestBody(
				offers,
				1024,
				[3]uint32{11, 22, 33},
				httpclient.PolicyTimeInterval,
				true,
			)).
			RespondWithAvailableOffers(&httpclient.CheckOffersResponse{AvailableOffers: []httpclient.YTaxiAvailableOffer{{IndexRaw: 0, Index: 0, Promise: "asd123dsa321"}}})
	}
	{
		destTimes := make([]time.Time, 0, 1)
		sourceTimes := make([]time.Time, 0, 1)
		destTimes = append(destTimes, time.Date(2020, 06, 03, 23, 59, 0, 0, time.FixedZone("UTC+3", 3*60*60)))
		sourceTimes = append(sourceTimes, time.Date(2020, 06, 04, 12, 00, 0, 0, time.FixedZone("UTC+3", 3*60*60)))
		offers := lite.MakeYTaxiOffers(
			goPlatformPointLmsID,
			false,
			units.GpsCoords{Latitude: 55.722613, Longitude: 37.555378},
			destTimes,
			units.GpsCoords{Latitude: 55.722613, Longitude: 37.555378},
			sourceTimes,
			1,
			0,
		)

		yTaxi.
			OnCheckAvailableOffersRequest(lite.MakeAvailableOffersRequestBody(
				offers,
				1024,
				[3]uint32{11, 22, 33},
				httpclient.PolicyTimeInterval,
				true,
			)).
			RespondWithAvailableOffers(&httpclient.CheckOffersResponse{AvailableOffers: []httpclient.YTaxiAvailableOffer{{IndexRaw: 0, Index: 0, Promise: "asd123dsa321"}}})
	}
	{
		destTimes := make([]time.Time, 0, 1)
		sourceTimes := make([]time.Time, 0, 1)
		destTimes = append(destTimes, time.Date(2021, 06, 05, 23, 59, 0, 0, time.FixedZone("UTC+3", 3*60*60)))
		sourceTimes = append(sourceTimes, time.Date(2021, 06, 06, 13, 00, 0, 0, time.FixedZone("UTC+3", 3*60*60)))
		offers := lite.MakeYTaxiOffers(
			goPlatformPointLmsID,
			false,
			units.GpsCoords{Latitude: 55.722613, Longitude: 37.555378},
			destTimes,
			units.GpsCoords{Latitude: 55.722613, Longitude: 37.555378},
			sourceTimes,
			1,
			0,
		)

		yTaxi.
			OnCheckAvailableOffersRequest(lite.MakeAvailableOffersRequestBody(
				offers,
				1024,
				[3]uint32{11, 22, 33},
				httpclient.PolicyTimeInterval,
				true,
			)).
			RespondWithAvailableOffers(&httpclient.CheckOffersResponse{AvailableOffers: []httpclient.YTaxiAvailableOffer{{IndexRaw: 0, Index: 0, Promise: "asd123dsa321"}}})
	}
}

func testFirstDay(t *testing.T, deferredCourierOptionFirstDaysCount int, resp *pb.DeliveryOptionsForUser) {
	for j := 0; j < deferredCourierOptionFirstDaysCount; j++ {
		option := resp.Options[j+2]
		requirepb.Equal(t, pb.DeliverySubtype_DEFERRED_COURIER, option.GetDeliverySubtype())
		requirepb.Equal(t, &pb.Date{Year: 2021, Month: 6, Day: uint32(6)}, option.DateFrom)
		requirepb.Equal(t, &pb.Date{Year: 2021, Month: 6, Day: uint32(6)}, option.DateTo)
		requirepb.Equal(t, &pb.Time{Hour: uint32(8 + j)}, option.Interval.From)
		requirepb.Equal(t, &pb.Time{Hour: uint32(9 + j)}, option.Interval.To)
		require.Equal(t, yGoServiceID, int(option.DeliveryServiceId))
		requirepb.Equal(t, []pb.PaymentMethod{pb.PaymentMethod_PREPAYMENT}, option.PaymentMethods)
	}
}

func testSecondDay(
	t *testing.T,
	deferredCourierOptionFirstDaysCount int,
	deferredCourierOptionPerDayCount int,
	deferredCourierOptionDaysCount int,
	resp *pb.DeliveryOptionsForUser,
) {

	for i := 1; i < deferredCourierOptionDaysCount-1; i++ {
		for j := 0; j < deferredCourierOptionPerDayCount; j++ {
			option := resp.Options[(i-1)*deferredCourierOptionPerDayCount+deferredCourierOptionFirstDaysCount+j+i+2]
			requirepb.Equal(t, pb.DeliverySubtype_DEFERRED_COURIER, option.GetDeliverySubtype())
			requirepb.Equal(t, &pb.Date{Year: 2021, Month: 6, Day: uint32(6 + i)}, option.DateFrom)
			requirepb.Equal(t, &pb.Date{Year: 2021, Month: 6, Day: uint32(6 + i)}, option.DateTo)
			requirepb.Equal(t, &pb.Time{Hour: uint32(8 + j)}, option.Interval.From)
			requirepb.Equal(t, &pb.Time{Hour: uint32(9 + j)}, option.Interval.To)
			require.Equal(t, yGoServiceID, int(option.DeliveryServiceId))
			requirepb.Equal(t, []pb.PaymentMethod{pb.PaymentMethod_PREPAYMENT}, option.PaymentMethods)
		}
	}
	{
		i := deferredCourierOptionDaysCount - 1
		for j := 0; j < deferredCourierOptionPerDayCount; j++ {
			option := resp.Options[(i-1)*deferredCourierOptionPerDayCount+deferredCourierOptionFirstDaysCount+j+i+1]
			requirepb.Equal(t, pb.DeliverySubtype_DEFERRED_COURIER, option.GetDeliverySubtype())
			requirepb.Equal(t, &pb.Date{Year: 2021, Month: 6, Day: uint32(6 + i)}, option.DateFrom)
			requirepb.Equal(t, &pb.Date{Year: 2021, Month: 6, Day: uint32(6 + i)}, option.DateTo)
			requirepb.Equal(t, &pb.Time{Hour: uint32(8 + j)}, option.Interval.From)
			requirepb.Equal(t, &pb.Time{Hour: uint32(9 + j)}, option.Interval.To)
			require.Equal(t, yGoServiceID, int(option.DeliveryServiceId))
			requirepb.Equal(t, []pb.PaymentMethod{pb.PaymentMethod_PREPAYMENT}, option.PaymentMethods)
		}
	}
}

func testCourier(t *testing.T, courierOptionCount int, deferredCourierOptionPerDayCount int, resp *pb.DeliveryOptionsForUser) {
	{
		option := resp.Options[0]
		requirepb.Equal(t, pb.DeliverySubtype_ORDINARY, option.GetDeliverySubtype())
		requirepb.Equal(t, &pb.Date{Year: 2021, Month: 6, Day: uint32(5)}, option.DateFrom)
		requirepb.Equal(t, &pb.Date{Year: 2021, Month: 6, Day: uint32(5)}, option.DateTo)
		requirepb.Equal(t, &pb.Time{Hour: 16}, option.Interval.From)
		requirepb.Equal(t, &pb.Time{Hour: 22}, option.Interval.To)
		require.Equal(t, lastMileDeliveryServiceID, int(option.DeliveryServiceId))
	}
	{
		option := resp.Options[1]
		requirepb.Equal(t, pb.DeliverySubtype_ORDINARY, option.GetDeliverySubtype())
		requirepb.Equal(t, &pb.Date{Year: 2021, Month: 6, Day: uint32(6)}, option.DateFrom)
		requirepb.Equal(t, &pb.Date{Year: 2021, Month: 6, Day: uint32(6)}, option.DateTo)
		requirepb.Equal(t, &pb.Time{Hour: 16}, option.Interval.From)
		requirepb.Equal(t, &pb.Time{Hour: 22}, option.Interval.To)
		require.Equal(t, lastMileDeliveryServiceID, int(option.DeliveryServiceId))
	}
	for i := 2; i < courierOptionCount; i++ {
		option := resp.Options[deferredCourierOptionPerDayCount*(i-1)+i]
		requirepb.Equal(t, pb.DeliverySubtype_ORDINARY, option.GetDeliverySubtype())
		requirepb.Equal(t, &pb.Date{Year: 2021, Month: 6, Day: uint32(5 + i)}, option.DateFrom)
		requirepb.Equal(t, &pb.Date{Year: 2021, Month: 6, Day: uint32(5 + i)}, option.DateTo)
		requirepb.Equal(t, &pb.Time{Hour: 16}, option.Interval.From)
		requirepb.Equal(t, &pb.Time{Hour: 22}, option.Interval.To)
		require.Equal(t, lastMileDeliveryServiceID, int(option.DeliveryServiceId))
	}
}
