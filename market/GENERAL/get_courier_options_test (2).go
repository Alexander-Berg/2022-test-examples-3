package lite

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/test/requirepb"
	bg "a.yandex-team.ru/market/combinator/pkg/biggeneration"
	"a.yandex-team.ru/market/combinator/pkg/enums"
	"a.yandex-team.ru/market/combinator/pkg/fashion"
	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/market/combinator/pkg/graph"
	partdel "a.yandex-team.ru/market/combinator/pkg/partial_delivery"
	tr "a.yandex-team.ru/market/combinator/pkg/tarifficator"
	"a.yandex-team.ru/market/combinator/proto/common"
	pb "a.yandex-team.ru/market/combinator/proto/grpc"
)

func TestGetCourierOptions(t *testing.T) {
	graphEx := graph.NewExample()
	// This tariff couldn't handle such a heavy items
	unsuitableTariff := tr.TariffRT{
		ID:                1,
		DeliveryServiceID: 106,
		DeliveryMethod:    enums.DeliveryMethodCourier,
		ProgramTypeList:   tr.ProgramTypeList(tr.ProgramMarketDelivery | tr.ProgramBeruCrossdock),
		Option: tr.Option{
			Cost:    11,
			DaysMin: 3,
			DaysMax: 5,
		},
		RuleAttrs: tr.RuleAttrs{
			WeightMax: 15000,
			HeightMax: 30,
			LengthMax: 30,
			WidthMax:  30,
			DimSumMax: 100,
		},
		FromToRegions: tr.FromToRegions{
			From: 1,
			To:   213,
		},
		Type: tr.RuleTypePayment,
	}
	globalRule := newShopTariff(&tariffSettings{
		deliveryMethod:    enums.DeliveryMethodCourier,
		deliveryServiceID: 106,
		regionTo:          1,
		ruleType:          tr.RuleTypeGlobal,
	})
	shopTariff := tr.TariffRT{
		ID:                1,
		DeliveryServiceID: 106,
		DeliveryMethod:    enums.DeliveryMethodCourier,
		ProgramTypeList:   tr.ProgramTypeList(tr.ProgramMarketDelivery | tr.ProgramBeruCrossdock),
		Option: tr.Option{
			Cost:    42,
			DaysMin: 5,
			DaysMax: 7,
		},
		RuleAttrs: tr.RuleAttrs{
			WeightMax: 20000,
			HeightMax: 30,
			LengthMax: 30,
			WidthMax:  30,
			DimSumMax: 100,
		},
		FromToRegions: tr.FromToRegions{
			From: 1,
			To:   213,
		},
		Type: tr.RuleTypePayment,
	}
	globalRuleSlow := newShopTariff(&tariffSettings{
		deliveryMethod:    enums.DeliveryMethodCourier,
		deliveryServiceID: 139,
		regionTo:          1,
		ruleType:          tr.RuleTypeGlobal,
	})
	shopTariffSlow := tr.TariffRT{
		ID:                2,
		DeliveryServiceID: 139,
		DeliveryMethod:    enums.DeliveryMethodCourier,
		ProgramTypeList:   tr.ProgramTypeList(tr.ProgramMarketDelivery | tr.ProgramBeruCrossdock),
		Option: tr.Option{
			Cost:    54,
			DaysMin: 10,
			DaysMax: 12,
		},
		RuleAttrs: tr.RuleAttrs{
			WeightMax: 20000,
			HeightMax: 30,
			LengthMax: 30,
			WidthMax:  30,
			DimSumMax: 100,
		},
		FromToRegions: tr.FromToRegions{
			From: 1,
			To:   213,
		},
		Type: tr.RuleTypePayment,
	}
	tariffsFinder := tr.NewTariffsFinder()
	tariffsFinder.Add(&unsuitableTariff)
	tariffsFinder.Add(&globalRule)
	tariffsFinder.Add(&shopTariff)

	genData := bg.GenerationData{
		RegionMap:     geobase.NewExample(),
		TariffsFinder: NewFinderSet(tariffsFinder),
		Graph:         graphEx.G,
	}

	startTime := time.Date(2020, 06, 02, 12, 4, 5, 0, time.UTC)

	dest := pb.PointIds{
		RegionId: 213,
	}
	reqSettings := newRequestSettings(requestSettings{
		startTime: startTime,
		dest:      &dest,
		dType:     pb.DeliveryType_COURIER,
		dayTo:     2 + shopTariff.DaysMax,
	})
	req := PrepareDeliveryRequest(reqSettings)

	env, cancel := NewEnv(t, &genData, nil)
	defer cancel()

	resp, err := env.Client.GetCourierOptions(env.Ctx, req)

	checkResponse := func(b2b bool) {
		NumIntervals := 2 * (1 + 4) // по 2 интервала на каждый день
		require.NoError(t, err)
		require.Len(t, resp.Options, NumIntervals)
		for i := 0; i < NumIntervals/2; i++ {
			for j := 0; j < 2; j++ {
				opt := resp.Options[2*i+j]
				// require.Equal(t, uint32(customerTariff.Cost), opt.Cost)
				requirepb.Equal(t, &pb.Date{Year: 2020, Month: 6, Day: uint32(9 + i)}, opt.DateFrom)
				requirepb.Equal(t, &pb.Date{Year: 2020, Month: 6, Day: uint32(9 + i)}, opt.DateTo)
				if j == 0 {
					requirepb.Equal(t, &pb.Time{Hour: 10}, opt.Interval.From)
					requirepb.Equal(t, &pb.Time{Hour: 22}, opt.Interval.To)
				} else {
					requirepb.Equal(t, &pb.Time{Hour: 18}, opt.Interval.From)
					requirepb.Equal(t, &pb.Time{Hour: 22}, opt.Interval.To)
				}
				require.Equal(t, uint32(shopTariff.DeliveryServiceID), opt.DeliveryServiceId)
				expectedPaymentMethods := []common.PaymentMethod{
					common.PaymentMethod_PREPAYMENT,
					common.PaymentMethod_CASH,
					common.PaymentMethod_CARD,
				}
				if b2b {
					expectedPaymentMethods = []common.PaymentMethod{
						common.PaymentMethod_B2B_ACCOUNT_PREPAYMENT,
					}
				}
				require.Equal(t, expectedPaymentMethods, opt.PaymentMethods)
			}
		}
		var expectedWeight uint32
		for _, item := range req.Items {
			expectedWeight += item.Weight * item.RequiredCount
		}
		expectedDimensions := []uint32{uint32(20), uint32(30), uint32(35)}
		require.NotNil(t, resp.VirtualBox)
		require.Equal(t, expectedWeight, resp.VirtualBox.Weight)
		require.Equal(t, expectedDimensions, resp.VirtualBox.Dimensions)
	}
	checkResponse(false)

	respStat, errStat := env.Client.GetCourierDeliveryStatistics(env.Ctx, req)
	require.NoError(t, errStat)
	require.Len(t, respStat.DeliveryMethods, 1)
	require.Equal(t, pb.DeliveryMethod_DM_COURIER, respStat.DeliveryMethods[0])

	// B2B
	reqSettings.b2b = true
	req = PrepareDeliveryRequest(reqSettings)
	resp, err = env.Client.GetCourierOptions(env.Ctx, req)
	checkResponse(true)

	respStat, errStat = env.Client.GetCourierDeliveryStatistics(env.Ctx, req)
	require.NoError(t, errStat)
	require.Len(t, respStat.DeliveryMethods, 1)
	require.Equal(t, pb.DeliveryMethod_DM_COURIER, respStat.DeliveryMethods[0])

	// Too heavy item
	reqSettings = newRequestSettings(requestSettings{
		startTime: startTime,
		dest:      &dest,
		dType:     pb.DeliveryType_COURIER,
		dayTo:     2 + shopTariff.DaysMax,
		weight:    12001, // by default more than 12 + 4*2
	})
	req = PrepareDeliveryRequest(reqSettings)

	resp, err = env.Client.GetCourierOptions(env.Ctx, req)
	require.NoError(t, err)
	requirepb.Equal(t, resp, &pb.DeliveryOptionsForUser{})

	respStat, errStat = env.Client.GetCourierDeliveryStatistics(env.Ctx, req)
	require.NoError(t, errStat)
	require.Len(t, respStat.DeliveryMethods, 0)

	// Bad cargo type
	reqSettings = newRequestSettings(requestSettings{
		startTime:  startTime,
		dest:       &dest,
		dType:      pb.DeliveryType_COURIER,
		dayTo:      2 + shopTariff.DaysMax,
		cargoTypes: []uint32{322322},
	})
	req = PrepareDeliveryRequest(reqSettings)

	resp, err = env.Client.GetCourierOptions(env.Ctx, req)
	require.NoError(t, err)
	require.Len(t, resp.Options, 0)

	respStat, errStat = env.Client.GetCourierDeliveryStatistics(env.Ctx, req)
	require.NoError(t, errStat)
	require.Len(t, respStat.DeliveryMethods, 0)

	// Partially deliverable order is delivered only by Market Courier
	reqSettings = newRequestSettings(requestSettings{
		startTime:  startTime,
		dest:       &dest,
		dType:      pb.DeliveryType_COURIER,
		dayTo:      2 + shopTariff.DaysMax,
		cargoTypes: []uint32{fashion.CargoType},
	})
	req = PrepareDeliveryRequest(reqSettings)
	req.RearrFactors = "partial_delivery_flag_courier_options_v2=0;partial_delivery_flag_pickup_points_v2=0"
	partdel.Warehouses[145] = true

	resp, err = env.Client.GetCourierOptions(env.Ctx, req)
	require.NoError(t, err)
	require.Empty(t, resp.Options)

	respStat, errStat = env.Client.GetCourierDeliveryStatistics(env.Ctx, req)
	require.NoError(t, errStat)
	require.Empty(t, respStat.DeliveryMethods)

	shopTariff.IsMarketCourier = true
	resp, err = env.Client.GetCourierOptions(env.Ctx, req)
	require.NoError(t, err)
	require.NotEmpty(t, resp.Options)

	respStat, errStat = env.Client.GetCourierDeliveryStatistics(env.Ctx, req)
	require.NoError(t, errStat)
	require.Len(t, respStat.DeliveryMethods, 1)
	require.Equal(t, pb.DeliveryMethod_DM_COURIER, respStat.DeliveryMethods[0])

	req.RearrFactors = "partial_delivery_flag_courier_options_v2=0;partial_delivery_flag_pickup_points_v2=0;fashion_partial_msk_and_spb=1;"
	respn, errn := env.Client.GetCourierDeliveryStatistics(env.Ctx, req)
	require.NoError(t, errn)
	require.Empty(t, respn.DeliveryMethods)

	// Do not return non-fashion delivery services
	resp, err = env.Client.GetCourierOptions(env.Ctx, req)
	require.NoError(t, err)
	require.Empty(t, resp.Options)

	respStat, errStat = env.Client.GetCourierDeliveryStatistics(env.Ctx, req)
	require.NoError(t, errStat)
	require.Empty(t, respStat.DeliveryMethods)

	fashion.GetPartialDeliveryServices(nil)[106] = true
	resp, err = env.Client.GetCourierOptions(env.Ctx, req)
	require.NoError(t, err)
	require.NotEmpty(t, resp.Options)

	respStat, errStat = env.Client.GetCourierDeliveryStatistics(env.Ctx, req)
	require.NoError(t, errStat)
	require.Len(t, respStat.DeliveryMethods, 1)
	require.Equal(t, pb.DeliveryMethod_DM_COURIER, respStat.DeliveryMethods[0])

	{
		// COMBINATOR-3015 do not return courier options for fashion fbs under flag fashion_fbs_depots_only=1
		graphEx := graph.NewExample()
		graphEx.Warehouse.PartnerType = enums.PartnerTypeDropship
		graphEx.G.Finish(context.Background())

		genData := bg.GenerationData{
			RegionMap:     geobase.NewExample(),
			TariffsFinder: NewFinderSet(tariffsFinder),
			Graph:         graphEx.G,
		}

		env, cancel := NewEnv(t, &genData, nil)
		defer cancel()

		req.RearrFactors = "fashion_fbs_depots_only=0"
		resp, err = env.Client.GetCourierOptions(env.Ctx, req)
		require.NoError(t, err)
		require.NotEmpty(t, resp.Options)

		respStat, errStat = env.Client.GetCourierDeliveryStatistics(env.Ctx, req)
		require.NoError(t, errStat)
		require.Len(t, respStat.DeliveryMethods, 1)
		require.Equal(t, pb.DeliveryMethod_DM_COURIER, respStat.DeliveryMethods[0])

		req.RearrFactors = "fashion_fbs_depots_only=1;partial_delivery_flag_courier_options_v2=0;partial_delivery_flag_pickup_points_v2=0"
		resp, err = env.Client.GetCourierOptions(env.Ctx, req)
		require.NoError(t, err)
		require.Empty(t, resp.Options)

		respStat, errStat = env.Client.GetCourierDeliveryStatistics(env.Ctx, req)
		require.NoError(t, errStat)
		require.Len(t, respStat.DeliveryMethods, 0)
	}

	fashion.GetPartialDeliveryServices(nil)[106] = false
	partdel.Warehouses[145] = false

	{
		// COMBINATOR-1986 Return avia options only when the flag is set
		graphEx := graph.NewCourierAlternativeExample() // delivery service IDs = 106, 139
		tariffsFinder.Add(&globalRuleSlow)
		tariffsFinder.Add(&shopTariffSlow)
		genData := bg.GenerationData{
			RegionMap:     geobase.NewExample(),
			TariffsFinder: NewFinderSet(tariffsFinder),
			Graph:         graphEx.G,
		}
		reqSettings = newRequestSettings(requestSettings{
			startTime: startTime,
			dest:      &dest,
			dType:     pb.DeliveryType_COURIER,
			dayTo:     2 + shopTariffSlow.DaysMax,
		})

		req = PrepareDeliveryRequest(reqSettings)
		req.RearrFactors = ""

		env, cancel := NewEnv(t, &genData, nil)
		defer cancel()

		graph.GetFastDeliveryServices()[106] = true // 106 is avia delivery now
		resp, err = env.Client.GetCourierOptions(env.Ctx, req)
		respStat, errStat = env.Client.GetCourierDeliveryStatistics(env.Ctx, req)
		graph.GetFastDeliveryServices()[106] = false

		require.NoError(t, err)
		require.NotEmpty(t, resp.Options)
		for _, option := range resp.Options {
			// all options are for ordinary courier service
			require.Equal(t, 139, int(option.DeliveryServiceId))
		}
		countOrdinary := len(resp.Options)

		require.NoError(t, errStat)
		require.Len(t, respStat.DeliveryMethods, 1)
		require.Equal(t, pb.DeliveryMethod_DM_COURIER, respStat.DeliveryMethods[0])

		// COMBINATOR-2413 not intersected options for avia and ordinary courier
		req.RearrFactors = "avia_delivery=1"
		graph.GetFastDeliveryServices()[106] = true // 106 is avia delivery now
		resp, err = env.Client.GetCourierOptions(env.Ctx, req)
		respStat, errStat = env.Client.GetCourierDeliveryStatistics(env.Ctx, req)
		graph.GetFastDeliveryServices()[106] = false

		require.NoError(t, err)
		require.NotEmpty(t, resp.Options)
		countFast := 0
		for _, option := range resp.Options {
			if option.DeliveryServiceId == 106 {
				countFast++
			}
		}
		require.NotZero(t, countFast)
		require.Len(t, resp.Options, countOrdinary+countFast)

		require.NoError(t, errStat)
		require.Len(t, respStat.DeliveryMethods, 1)
		require.Equal(t, pb.DeliveryMethod_DM_COURIER, respStat.DeliveryMethods[0])
	}
	{
		// COMBINATOR-2413 intersected options for avia and ordinary courier: ordinary wins
		graphEx := graph.NewCourierAlternativeExample() // delivery service IDs = 106, 139
		shopTariffSlow.DaysMin = shopTariff.DaysMin
		shopTariffSlow.DaysMax = shopTariff.DaysMax
		genData := bg.GenerationData{
			RegionMap:     geobase.NewExample(),
			TariffsFinder: NewFinderSet(tariffsFinder),
			Graph:         graphEx.G,
		}
		env, cancel := NewEnv(t, &genData, nil)
		defer cancel()

		reqSettings = newRequestSettings(requestSettings{
			startTime: startTime,
			dest:      &dest,
			dType:     pb.DeliveryType_COURIER,
			dayTo:     2 + shopTariffSlow.DaysMax,
		})
		req = PrepareDeliveryRequest(reqSettings)
		req.RearrFactors = "avia_delivery=1"
		graph.GetFastDeliveryServices()[106] = true // 106 is avia delivery now
		resp, err = env.Client.GetCourierOptions(env.Ctx, req)
		respStat, errStat = env.Client.GetCourierDeliveryStatistics(env.Ctx, req)
		graph.GetFastDeliveryServices()[106] = false

		require.NoError(t, err)
		require.NotEmpty(t, resp.Options)
		for _, option := range resp.Options {
			// all options are for ordinary courier service: should win over avia if same day
			require.Equal(t, 139, int(option.DeliveryServiceId))
		}

		require.NoError(t, errStat)
		require.Len(t, respStat.DeliveryMethods, 1)
		require.Equal(t, pb.DeliveryMethod_DM_COURIER, respStat.DeliveryMethods[0])
	}

	{
		// COMBINATOR-3838 do not return dropoff options for jewelry under flag
		graphEx := graph.NewExampleWithDropoff()
		genData := bg.GenerationData{
			RegionMap:     geobase.NewExample(),
			TariffsFinder: NewFinderSet(tariffsFinder),
			Graph:         graphEx.G,
		}

		env, cancel := NewEnv(t, &genData, nil)
		defer cancel()

		req.Items[0].CargoTypes = []uint32{80}
		req.Items[1].CargoTypes = []uint32{80}

		// allowed to return dropoff options
		req.RearrFactors = "disable_jewelry_dropoffs=0"

		resp, err = env.Client.GetCourierOptions(env.Ctx, req)
		require.NoError(t, err)
		require.NotEmpty(t, resp.Options)

		respStat, errStat = env.Client.GetCourierDeliveryStatistics(env.Ctx, req)
		require.NoError(t, errStat)
		require.Len(t, respStat.DeliveryMethods, 1)
		require.Equal(t, pb.DeliveryMethod_DM_COURIER, respStat.DeliveryMethods[0])

		// do not return dropoff options
		req.RearrFactors = "disable_jewelry_dropoffs=1"
		resp, err = env.Client.GetCourierOptions(env.Ctx, req)
		require.NoError(t, err)
		require.Empty(t, resp.Options)

		respStat, errStat = env.Client.GetCourierDeliveryStatistics(env.Ctx, req)
		require.NoError(t, errStat)
		require.Len(t, respStat.DeliveryMethods, 0)
	}
}
