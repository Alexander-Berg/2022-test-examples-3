package deferredcourierlite

import (
	"context"

	"a.yandex-team.ru/market/combinator/pkg/daysoff"
	"a.yandex-team.ru/market/combinator/pkg/enums"
	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/market/combinator/pkg/graph"
	"a.yandex-team.ru/market/combinator/pkg/its"
	"a.yandex-team.ru/market/combinator/pkg/lite"
	"a.yandex-team.ru/market/combinator/pkg/settings"
	tr "a.yandex-team.ru/market/combinator/pkg/tarifficator"
)

const (
	ffWarehouseID = 172
	dropOffID     = 48629

	middleMileDeliveryServiceID = 239
	yGoServiceID                = 93730
	lastMileDeliveryServiceID   = 48

	ffWarehouseSegmentID        = 612910
	middleMileMovementSegmentID = 734986
	dropOffSegmentID            = 734984

	lastMileMovementSegmentID   = 700000
	lastMileLinehaulSegmentID   = 700001
	lastMileHandingSegmentID    = 700002
	lastMilePickupSegmentID     = 700003
	lastMilePickupDarkSegmentID = 700004
	goPlatformDarkSegmentID     = 700005
	goPlatformSegmentID         = 700006

	goPlatformDarkPointLmsID = 10000971018
	goPlatformPointLmsID     = 10000971019

	userLatitude  = 55.724987
	userLongitude = 37.568081
)

func makeDeferredCourierGraph(settingsHolder *its.SettingsHolder) (*graph.Graph, daysoff.ServicesHashed) {
	g := graph.NewGraphWithHintsV3(nil)
	ctx := settings.ContextWithSettings(context.Background(), settings.New(settingsHolder.GetSettings(), ""))
	defer g.Finish(ctx)

	// FF warehouse
	ffWarehouse := graph.Node{
		LogisticSegment: graph.LogisticSegment{
			ID:           ffWarehouseSegmentID,
			PartnerLmsID: ffWarehouseID,
			LocationID:   geobase.RegionSofyno,
			PointLmsID:   10000004403,
			Type:         graph.SegmentTypeWarehouse,
			PartnerType:  enums.PartnerTypeFulfillment,
		},
		CourierServices: []*graph.LogisticService{
			{
				ID:           2792173,
				SegmentLmsID: ffWarehouseSegmentID,
				IsActive:     true,
				Duration:     180,
				Type:         graph.ServiceTypeInternal,
				Code:         enums.ServiceProcessing,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("00:00:00", "23:59:59", false),
			},
			{
				ID:           2827307,
				SegmentLmsID: ffWarehouseSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeInternal,
				Code:         enums.ServiceConsolidation,
				Price:        0,
				Schedule:     lite.CreateEmptySchedule(),
			},
			{
				ID:           2822637,
				SegmentLmsID: ffWarehouseSegmentID,
				IsActive:     true,
				Duration:     180,
				Type:         graph.ServiceTypeOutbound,
				Code:         enums.ServiceShipment,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("00:00:00", "23:59:59", false),
			},
		},
		PickupServices: []*graph.LogisticService{
			{
				ID:           2792173,
				SegmentLmsID: ffWarehouseSegmentID,
				IsActive:     true,
				Duration:     180,
				Type:         graph.ServiceTypeInternal,
				Code:         enums.ServiceProcessing,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("00:00:00", "23:59:59", false),
			},
			{
				ID:           2827307,
				SegmentLmsID: ffWarehouseSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeInternal,
				Code:         enums.ServiceConsolidation,
				Price:        0,
				Schedule:     lite.CreateEmptySchedule(),
			},
			{
				ID:           2822637,
				SegmentLmsID: ffWarehouseSegmentID,
				IsActive:     true,
				Duration:     180,
				Type:         graph.ServiceTypeOutbound,
				Code:         enums.ServiceShipment,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("00:00:00", "23:59:59", false),
			},
		},
	}
	g.AddNode(ffWarehouse)

	// middle mile movement
	middleMileMovement := graph.Node{
		LogisticSegment: graph.LogisticSegment{
			ID:           middleMileMovementSegmentID,
			PartnerLmsID: middleMileDeliveryServiceID,
			Type:         graph.SegmentTypeMovement,
			PartnerType:  enums.PartnerTypeDelivery,
		},
		CourierServices: []*graph.LogisticService{
			{
				ID:           3107651,
				SegmentLmsID: middleMileMovementSegmentID,
				IsActive:     true,
				Duration:     90,
				Type:         graph.ServiceTypeInbound,
				Code:         enums.ServiceInbound,
				Price:        0,
				Schedule:     lite.CreateEmptySchedule(),
			},
			{
				ID:           3107653,
				SegmentLmsID: middleMileMovementSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeInternal,
				Code:         enums.ServiceMovement,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("01:30:00", "02:30:00", false),
			},
			{
				ID:           3107652,
				SegmentLmsID: middleMileMovementSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeOutbound,
				Code:         enums.ServiceShipment,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("01:30:00", "02:30:00", false),
			},
		},
	}
	g.AddNode(middleMileMovement)

	// Yandex.Go warehouse (ул. Кооперативная, д. 2, корп. 14)
	dropOffOutlet := graph.Node{
		LogisticSegment: graph.LogisticSegment{
			ID:           dropOffSegmentID,
			PartnerLmsID: dropOffID,
			LocationID:   geobase.RegionMoscow,
			PointLmsID:   10000995271,
			Type:         graph.SegmentTypeWarehouse,
			PartnerType:  enums.PartnerTypeDelivery,
		},
		CourierServices: []*graph.LogisticService{
			{
				ID:           3107643,
				SegmentLmsID: dropOffSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeInbound,
				Code:         enums.ServiceInbound,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("09:00:00", "15:00:00", false),
			},
			{
				ID:           3107644,
				SegmentLmsID: dropOffSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeOutbound,
				Code:         enums.ServiceShipment,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("00:00:00", "23:59:59", false),
			},
			{
				ID:           3107739,
				SegmentLmsID: dropOffSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeInternal,
				Code:         enums.ServiceSort,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("00:00:00", "23:59:59", false),
			},
			{
				ID:           3107645,
				SegmentLmsID: dropOffSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeInternal,
				Code:         enums.ServiceDeferredCourierYandexGo,
				Price:        0,
				Schedule:     lite.CreateEmptySchedule(),
			},
		},
	}
	g.AddNode(dropOffOutlet)

	// last mile movement
	lastMileMovement := graph.Node{
		LogisticSegment: graph.LogisticSegment{
			ID:           lastMileMovementSegmentID,
			PartnerLmsID: lastMileDeliveryServiceID,
			Type:         graph.SegmentTypeMovement,
			PartnerType:  enums.PartnerTypeDelivery,
		},
		CourierServices: []*graph.LogisticService{
			{
				ID:           3000000,
				SegmentLmsID: lastMileMovementSegmentID,
				IsActive:     true,
				Duration:     90,
				Type:         graph.ServiceTypeInbound,
				Code:         enums.ServiceInbound,
				Price:        0,
				Schedule:     lite.CreateEmptySchedule(),
			},
			{
				ID:           3000001,
				SegmentLmsID: lastMileMovementSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeInternal,
				Code:         enums.ServiceMovement,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("01:30:00", "02:30:00", false),
			},
			{
				ID:           3000002,
				SegmentLmsID: lastMileMovementSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeOutbound,
				Code:         enums.ServiceShipment,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("01:30:00", "02:30:00", false),
			},
		},
		PickupServices: []*graph.LogisticService{
			{
				ID:           3000000,
				SegmentLmsID: lastMileMovementSegmentID,
				IsActive:     true,
				Duration:     90,
				Type:         graph.ServiceTypeInbound,
				Code:         enums.ServiceInbound,
				Price:        0,
				Schedule:     lite.CreateEmptySchedule(),
			},
			{
				ID:           3000001,
				SegmentLmsID: lastMileMovementSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeInternal,
				Code:         enums.ServiceMovement,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("01:30:00", "02:30:00", false),
			},
			{
				ID:           3000002,
				SegmentLmsID: lastMileMovementSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeOutbound,
				Code:         enums.ServiceShipment,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("10:00:00", "11:00:00", false),
			},
		},
	}
	g.AddNode(lastMileMovement)
	daysOffGrouped := daysoff.NewServicesHashed()
	daysOffGrouped.DaysOffGrouped[3000002] = daysoff.NewDaysOffGroupedFromStrings(
		[]string{
			"2021-06-03",
			"2021-06-02",
			"2021-06-01", // disabledOffset
		})

	// last mile linehaul
	lastMileLinehaul := graph.Node{
		LogisticSegment: graph.LogisticSegment{
			ID:           lastMileLinehaulSegmentID,
			PartnerLmsID: lastMileDeliveryServiceID,
			LocationID:   geobase.RegionMoscow,
			Type:         graph.SegmentTypeLinehaul,
			PartnerType:  enums.PartnerTypeDelivery,
		},
		CourierServices: []*graph.LogisticService{
			{
				ID:           3000003,
				SegmentLmsID: lastMileLinehaulSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeInternal,
				Code:         enums.ServiceDelivery,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("00:00:00", "23:59:59", false),
			},
			{
				ID:           3000004,
				SegmentLmsID: lastMileLinehaulSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeInternal,
				Code:         enums.ServiceLastMile,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("12:00:00", "12:00:00", true),
			},
		},
		PickupServices: []*graph.LogisticService{
			{
				ID:           3000003,
				SegmentLmsID: lastMileLinehaulSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeInternal,
				Code:         enums.ServiceDelivery,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("00:00:00", "23:59:59", false),
			},
			{
				ID:           3000004,
				SegmentLmsID: lastMileLinehaulSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeInternal,
				Code:         enums.ServiceLastMile,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("12:00:00", "12:00:00", true),
			},
			{
				ID:           3000014,
				SegmentLmsID: lastMileLinehaulSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeOutbound,
				Code:         enums.ServiceShipment,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("00:00:00", "12:00:00", true),
			},
		},
	}
	g.AddNode(lastMileLinehaul)

	// last mile handing
	lastMileHanding := graph.Node{
		LogisticSegment: graph.LogisticSegment{
			ID:           lastMileHandingSegmentID,
			PartnerLmsID: lastMileDeliveryServiceID,
			LocationID:   geobase.RegionMoscow,
			Type:         graph.SegmentTypeHanding,
			PartnerType:  enums.PartnerTypeDelivery,
		},
		CourierServices: []*graph.LogisticService{
			{
				ID:           3000005,
				SegmentLmsID: lastMileHandingSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeOutbound,
				Code:         enums.ServiceHanding,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("16:00:00", "22:00:00", false),
			},
		},
	}
	g.AddNode(lastMileHanding)

	lastMilePickup := graph.Node{
		LogisticSegment: graph.LogisticSegment{
			ID:             lastMilePickupSegmentID,
			PointLmsID:     goPlatformPointLmsID,
			LocationID:     geobase.RegionMoscow,
			Type:           graph.SegmentTypePickup,
			PartnerType:    enums.PartnerTypeDelivery,
			PaymentMethods: enums.MethodCardAllowed,
		},
		CourierServices: []*graph.LogisticService{
			{
				ID:           3000006,
				SegmentLmsID: lastMilePickupSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeOutbound,
				Code:         enums.ServiceHanding,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("16:00:00", "22:00:00", false),
			},
		},
		PickupServices: []*graph.LogisticService{
			{
				ID:           3000007,
				SegmentLmsID: lastMilePickupSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeOutbound,
				Code:         enums.ServiceHanding,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("16:00:00", "22:00:00", false),
			},
		},
	}
	g.AddNode(lastMilePickup)

	lastMileDarkPickup := graph.Node{
		LogisticSegment: graph.LogisticSegment{
			ID:             lastMilePickupDarkSegmentID,
			PointLmsID:     goPlatformDarkPointLmsID,
			LocationID:     geobase.RegionMoscow,
			Type:           graph.SegmentTypePickup,
			PartnerType:    enums.PartnerTypeDelivery,
			PaymentMethods: enums.MethodCardAllowed,
		},
		CourierServices: []*graph.LogisticService{
			{
				ID:           3000008,
				SegmentLmsID: lastMilePickupDarkSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeOutbound,
				Code:         enums.ServiceHanding,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("16:00:00", "22:00:00", false),
			},
		},
		PickupServices: []*graph.LogisticService{
			{
				ID:           3000009,
				SegmentLmsID: lastMilePickupDarkSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeOutbound,
				Code:         enums.ServiceHanding,
				Price:        0,
				Schedule:     lite.CreateEveryDaySchedule("16:00:00", "22:00:00", false),
			},
		},
	}
	g.AddNode(lastMileDarkPickup)

	goPlatform := graph.Node{
		LogisticSegment: graph.LogisticSegment{
			ID:             goPlatformSegmentID,
			PartnerLmsID:   yGoServiceID,
			LocationID:     geobase.RegionMoscow,
			Type:           graph.SegmentTypeGoPlatform,
			PartnerType:    enums.PartnerTypeDelivery,
			PaymentMethods: enums.MethodCardAllowed,
		},
		CourierServices: []*graph.LogisticService{
			{
				ID:           3000012,
				SegmentLmsID: goPlatformSegmentID,
				IsActive:     true,
				Duration:     60,
				Type:         graph.ServiceTypeInternal,
				Code:         enums.ServiceProcessing,
				Price:        0,
			},
			{
				ID:           3000010,
				SegmentLmsID: goPlatformSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeOutbound,
				Code:         enums.ServiceHanding,
				Price:        0,
				Schedule: lite.CreateEveryDayIntervalSchedule(
					[]string{"08:00:00", "09:00:00", "10:00:00", "11:00:00", "12:00:00", "13:00:00", "14:00:00", "15:00:00"},
					[]string{"09:00:00", "10:00:00", "11:00:00", "12:00:00", "13:00:00", "14:00:00", "15:00:00", "16:00:00"},
					false,
				),
			},
		},
	}
	g.AddNode(goPlatform)

	goPlatformDark := graph.Node{
		LogisticSegment: graph.LogisticSegment{
			ID:             goPlatformDarkSegmentID,
			PartnerLmsID:   yGoServiceID,
			LocationID:     geobase.RegionMoscow,
			Type:           graph.SegmentTypeGoPlatform,
			PartnerType:    enums.PartnerTypeDelivery,
			PaymentMethods: enums.MethodCardAllowed,
		},
		CourierServices: []*graph.LogisticService{
			{
				ID:           3000013,
				SegmentLmsID: goPlatformSegmentID,
				IsActive:     true,
				Duration:     60,
				Type:         graph.ServiceTypeInternal,
				Code:         enums.ServiceProcessing,
				Price:        0,
			},
			{
				ID:           3000011,
				SegmentLmsID: goPlatformDarkSegmentID,
				IsActive:     true,
				Duration:     0,
				Type:         graph.ServiceTypeOutbound,
				Code:         enums.ServiceHanding,
				Price:        0,
				Schedule: lite.CreateEveryDayIntervalSchedule(
					[]string{"08:00:00", "09:00:00", "10:00:00", "11:00:00", "12:00:00", "13:00:00", "14:00:00", "15:00:00"},
					[]string{"09:00:00", "10:00:00", "11:00:00", "12:00:00", "13:00:00", "14:00:00", "15:00:00", "16:00:00"},
					false,
				),
			},
		},
	}
	g.AddNode(goPlatformDark)

	// Simple courier route starting at Sofyno
	_ = g.AddEdge(ffWarehouse.ID, lastMileMovement.ID)
	_ = g.AddEdge(lastMileMovement.ID, lastMileLinehaul.ID)
	_ = g.AddEdge(lastMilePickup.ID, goPlatform.ID)
	_ = g.AddEdge(lastMileDarkPickup.ID, goPlatformDark.ID)

	return g, daysOffGrouped
}

func makeTariffsFinder() *tr.TariffFinderSet {
	tf := tr.NewTariffsFinder()

	for _, deliveryServiceID := range []uint64{lastMileDeliveryServiceID} {
		// shop tariff
		tf.Add(&tr.TariffRT{
			DeliveryServiceID: deliveryServiceID,
			DeliveryMethod:    enums.DeliveryMethodCourier,
			ProgramTypeList:   tr.ProgramTypeList(tr.ProgramMarketDelivery),
			Option: tr.Option{
				Cost:    50,
				DaysMin: 1,
				DaysMax: 1,
			},
			RuleAttrs: tr.RuleAttrs{
				WeightMax: 15000,
				HeightMax: 100,
				LengthMax: 100,
				WidthMax:  100,
				DimSumMax: 400,
			},
			FromToRegions: tr.FromToRegions{
				From: geobase.RegionMoscowAndObl,
				To:   geobase.RegionMoscow,
			},
			Type: tr.RuleTypePayment,
		})

		tf.Add(&tr.TariffRT{
			DeliveryServiceID: deliveryServiceID,
			DeliveryMethod:    enums.DeliveryMethodPickup,
			ProgramTypeList:   tr.ProgramTypeList(tr.ProgramMarketDelivery),
			Option: tr.Option{
				Cost:    50,
				DaysMin: 1,
				DaysMax: 1,
			},
			RuleAttrs: tr.RuleAttrs{
				WeightMax: 15000,
				HeightMax: 100,
				LengthMax: 100,
				WidthMax:  100,
				DimSumMax: 400,
			},
			FromToRegions: tr.FromToRegions{
				From: geobase.RegionMoscowAndObl,
				To:   geobase.RegionMoscow,
			},
			Type: tr.RuleTypePayment,
		})

		tf.Add(&tr.TariffRT{
			DeliveryServiceID: deliveryServiceID,
			DeliveryMethod:    enums.DeliveryMethodPickup,
			ProgramTypeList:   tr.ProgramTypeList(tr.ProgramMarketDelivery),
			Option: tr.Option{
				Cost:    50,
				DaysMin: 1,
				DaysMax: 1,
			},
			RuleAttrs: tr.RuleAttrs{
				WeightMax: 15000,
				HeightMax: 100,
				LengthMax: 100,
				WidthMax:  100,
				DimSumMax: 400,
			},
			FromToRegions: tr.FromToRegions{
				From: geobase.RegionMoscowAndObl,
				To:   geobase.RegionMoscow,
			},
			Points: lite.MakePoints([]int64{
				goPlatformPointLmsID,
				goPlatformDarkPointLmsID,
			}),
			Type: tr.RuleTypeForPoint,
		})

		// global rule
		tf.Add(&tr.TariffRT{
			DeliveryServiceID: deliveryServiceID,
			DeliveryMethod:    enums.DeliveryMethodCourier,
			FromToRegions: tr.FromToRegions{
				From: geobase.RegionMoscowAndObl,
				To:   geobase.RegionMoscow,
			},
			Type: tr.RuleTypeGlobal,
			RuleAttrs: tr.RuleAttrs{
				WeightMax: 10000,
			},
		})
	}

	return &tr.TariffFinderSet{
		Common:     tf,
		B2B:        tf,
		Nordstream: tf,
	}
}
