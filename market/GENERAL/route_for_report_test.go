package graph

import (
	"context"
	"fmt"
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/test/requirepb"
	"a.yandex-team.ru/market/combinator/pkg/enums"
	"a.yandex-team.ru/market/combinator/pkg/timex"
	"a.yandex-team.ru/market/combinator/pkg/util"
	cr "a.yandex-team.ru/market/combinator/proto/grpc"
)

type TestDeliverySpec struct {
	WarehouseDay          int
	WarehouseHour         int
	MovementDay           int
	MovementHour          int
	MovementDuration      time.Duration
	LogisticHour          timex.DayTime
	SupplierSpecList      []supplierSpec
	LastWarehouseTzOffset int
}

func MakeTestDeliveryParams(spec TestDeliverySpec) deliverySpec {
	year, month := 2020, time.December
	return deliverySpec{
		WarehouseProcessingStart: time.Date(year, month, spec.WarehouseDay, spec.WarehouseHour, 0, 0, 0, time.UTC),
		MovementStart:            time.Date(year, month, spec.MovementDay, spec.MovementHour, 0, 0, 0, time.UTC),
		MovementDuration:         spec.MovementDuration,
		LogisticDayStart:         spec.LogisticHour,
		SupplierSpecList:         spec.SupplierSpecList,
		LastWarehouseTzOffset:    spec.LastWarehouseTzOffset,
	}
}

func TestCalcDeliveryResult(t *testing.T) {
	specWantList := []struct {
		TestDeliverySpec
		*cr.DeliveryDates
	}{
		{
			TestDeliverySpec{
				WarehouseDay:     1,
				WarehouseHour:    13,
				MovementDay:      1,
				MovementHour:     14,
				MovementDuration: 4 * time.Hour,
			},
			&cr.DeliveryDates{
				ShipmentDay:          0,
				PackagingTime:        uint32((time.Duration(18) * time.Hour).Seconds()), // 14 + 4
				ReceptionByWarehouse: timeToProtobufTimestamp(time.Date(2020, time.December, 1, 0, 0, 0, 0, time.UTC)),
				ShipmentBySupplier:   timeToProtobufTimestamp(time.Date(2020, time.December, 1, 0, 0, 0, 0, time.UTC)),
			},
		},
		{
			TestDeliverySpec{
				WarehouseDay:     1,
				WarehouseHour:    13,
				MovementDay:      1,
				MovementHour:     14,
				MovementDuration: 4 * time.Hour,
				LogisticHour:     timex.DayTime{Hour: 12},
			},
			&cr.DeliveryDates{
				ShipmentDay:          0,
				PackagingTime:        uint32((time.Duration(18) * time.Hour).Seconds()), // 14 + 4
				ReceptionByWarehouse: timeToProtobufTimestamp(time.Date(2020, time.December, 1, 0, 0, 0, 0, time.UTC)),
				ShipmentBySupplier:   timeToProtobufTimestamp(time.Date(2020, time.December, 1, 0, 0, 0, 0, time.UTC)),
			},
		},
		{
			TestDeliverySpec{
				WarehouseDay:     1,
				WarehouseHour:    23,
				MovementDay:      2,
				MovementHour:     3,
				MovementDuration: 4 * time.Hour,
				LogisticHour:     timex.DayTime{Hour: 12},
			},
			&cr.DeliveryDates{
				ShipmentDay:          0,
				PackagingTime:        uint32((time.Duration(31) * time.Hour).Seconds()), // 24 + 3 + 4
				ReceptionByWarehouse: timeToProtobufTimestamp(time.Date(2020, time.December, 1, 0, 0, 0, 0, time.UTC)),
				ShipmentBySupplier:   timeToProtobufTimestamp(time.Date(2020, time.December, 1, 0, 0, 0, 0, time.UTC)),
				LastWarehouseOffset:  &cr.ShipmentDayOffsetWarehouse{WarehousePosition: 0, Offset: -1},
			},
		},
		{
			TestDeliverySpec{
				WarehouseDay:     1,
				WarehouseHour:    23,
				MovementDay:      2,
				MovementHour:     13,
				MovementDuration: 4 * time.Hour,
				LogisticHour:     timex.DayTime{Hour: 12},
			},
			&cr.DeliveryDates{
				ShipmentDay:          1,
				PackagingTime:        uint32((time.Duration(17) * time.Hour).Seconds()), // 13 + 4
				ReceptionByWarehouse: timeToProtobufTimestamp(time.Date(2020, time.December, 2, 0, 0, 0, 0, time.UTC)),
				ShipmentBySupplier:   timeToProtobufTimestamp(time.Date(2020, time.December, 2, 0, 0, 0, 0, time.UTC)),
			},
		},
		{
			TestDeliverySpec{
				WarehouseDay:     1,
				WarehouseHour:    23,
				MovementDay:      3,
				MovementHour:     7,
				MovementDuration: 4 * time.Hour,
				LogisticHour:     timex.DayTime{Hour: 8},
				SupplierSpecList: []supplierSpec{
					{
						WarehousePartnerID:       47755,
						WarehouseProcessingStart: time.Date(2020, time.November, 29, 13, 0, 0, 0, time.UTC),
						ShipmentBySupplier:       time.Date(2020, time.November, 29, 21, 0, 0, 0, time.UTC),
					},
					{
						WarehousePartnerID:       48342,
						WarehouseProcessingStart: time.Date(2020, time.November, 30, 6, 0, 0, 0, time.UTC),
						ShipmentBySupplier:       time.Date(2020, time.November, 30, 11, 0, 0, 0, time.UTC),
					},
				},
			},
			&cr.DeliveryDates{
				ShipmentDay:          1,
				PackagingTime:        uint32((time.Duration(35) * time.Hour).Seconds()), // 7 + 4 + 24
				ReceptionByWarehouse: timeToProtobufTimestamp(time.Date(2020, time.December, 1, 23, 0, 0, 0, time.UTC)),
				ShipmentBySupplier:   timeToProtobufTimestamp(time.Date(2020, time.November, 30, 11, 0, 0, 0, time.UTC)),
				LastWarehouseOffset:  &cr.ShipmentDayOffsetWarehouse{WarehousePosition: 0, Offset: -1},
			},
		},
	}
	for i, entry := range specWantList {
		dp := MakeTestDeliveryParams(entry.TestDeliverySpec)
		result, _ := calcDeliveryResult(dp.WarehouseProcessingStart, dp, false)
		want := entry.DeliveryDates
		startDate := timex.StripUpToDay(dp.WarehouseProcessingStart)
		wantShipmentDate := timeToProtobufTimestamp(startDate.Add(time.Duration(want.ShipmentDay) * time.Hour * 24))
		require.Equal(t, want.ShipmentDay, result.ShipmentDay, i)
		require.Equal(t, want.PackagingTime, result.PackagingTime, i)
		require.Equal(t, wantShipmentDate, result.ShipmentDate, i)
		require.Equal(t, want.ReceptionByWarehouse, result.ReceptionByWarehouse, i)
		require.Equal(t, want.ShipmentBySupplier, result.ShipmentBySupplier, i)
		require.Equal(t, want.LastWarehouseOffset, result.LastWarehouseOffset)
		for j, supplier := range result.SupplierDeliveryList {
			// ???? ?????????????? ???????????? ReceptionByWarehouse ???????????????????? ?? ?? ?????????????????????? ?? ?? ?????????????????? ????????????
			require.Equal(t, want.ReceptionByWarehouse, supplier.ReceptionByWarehouse, i, j)
		}
	}
}

func TestCalcStringDeliveryResult(t *testing.T) {
	testCases := []struct {
		TestDeliverySpec
		*cr.StringDeliveryDates
	}{
		{
			TestDeliverySpec{
				WarehouseDay:     1,
				WarehouseHour:    23,
				MovementDay:      3,
				MovementHour:     7,
				MovementDuration: 4 * time.Hour,
				LogisticHour:     timex.DayTime{Hour: 8},
				SupplierSpecList: []supplierSpec{
					{
						WarehousePartnerID:       47755,
						WarehouseProcessingStart: time.Date(2020, time.November, 29, 13, 0, 0, 0, time.UTC),
						ShipmentBySupplier:       time.Date(2020, time.November, 29, 21, 0, 0, 0, time.UTC),
						TzOffset:                 util.MskTZOffset,
					},
					{
						WarehousePartnerID:       48342,
						WarehouseProcessingStart: time.Date(2020, time.November, 30, 6, 0, 0, 0, time.UTC),
						ShipmentBySupplier:       time.Date(2020, time.November, 30, 11, 0, 0, 0, time.UTC),
						TzOffset:                 util.MskTZOffset,
					},
				},
				LastWarehouseTzOffset: util.MskTZOffset,
			},
			&cr.StringDeliveryDates{
				ShipmentDate:         "2020-12-02",
				PackagingTime:        "PT35H0M",
				ShipmentBySupplier:   "2020-11-30T14:00:00+03:00",
				ReceptionByWarehouse: "2020-12-02T02:00:00+03:00",
				SupplierDeliveryList: []*cr.StringSupplierDeliveryDates{
					&cr.StringSupplierDeliveryDates{
						WarehouseId:          47755,
						ProcessingStartTime:  "2020-11-29T16:00:00+03:00",
						ShipmentBySupplier:   "2020-11-30T00:00:00+03:00",
						ReceptionByWarehouse: "2020-12-02T02:00:00+03:00",
					},
					&cr.StringSupplierDeliveryDates{
						WarehouseId:          48342,
						ProcessingStartTime:  "2020-11-30T09:00:00+03:00",
						ShipmentBySupplier:   "2020-11-30T14:00:00+03:00",
						ReceptionByWarehouse: "2020-12-02T02:00:00+03:00",
					},
				},
				LastWarehouseOffset: &cr.ShipmentDayOffsetWarehouse{WarehousePosition: 0, Offset: -1},
			},
		},
		{
			TestDeliverySpec{
				WarehouseDay:     1,
				WarehouseHour:    23,
				MovementDay:      3,
				MovementHour:     7,
				MovementDuration: 4 * time.Hour,
				LogisticHour:     timex.DayTime{Hour: 8},
				SupplierSpecList: []supplierSpec{
					{
						WarehousePartnerID:       48342,
						WarehouseProcessingStart: time.Date(2020, time.November, 30, 6, 0, 0, 0, time.UTC),
						ShipmentBySupplier:       time.Date(2020, time.November, 30, 11, 0, 0, 0, time.UTC),
						TzOffset:                 util.MskTZOffset,
					},
				},
				LastWarehouseTzOffset: util.EkbTZOffset,
			},
			&cr.StringDeliveryDates{
				ShipmentDate:         "2020-12-02",
				PackagingTime:        "PT35H0M",
				ShipmentBySupplier:   "2020-11-30T16:00:00+05:00",
				ReceptionByWarehouse: "2020-12-02T04:00:00+05:00",
				SupplierDeliveryList: []*cr.StringSupplierDeliveryDates{
					&cr.StringSupplierDeliveryDates{
						WarehouseId:          48342,
						ProcessingStartTime:  "2020-11-30T09:00:00+03:00",
						ShipmentBySupplier:   "2020-11-30T14:00:00+03:00",
						ReceptionByWarehouse: "2020-12-02T02:00:00+03:00",
					},
				},
				LastWarehouseOffset: &cr.ShipmentDayOffsetWarehouse{WarehousePosition: 0, Offset: -1},
			},
		},
	}

	for i, tc := range testCases {
		msg := fmt.Sprintf("Failed at test case %d", i)
		spec := MakeTestDeliveryParams(tc.TestDeliverySpec)
		expected := tc.StringDeliveryDates
		_, actual := calcDeliveryResult(spec.WarehouseProcessingStart, spec, true)

		require.Equal(t, expected.ShipmentDate, actual.ShipmentDate, msg)
		require.Equal(t, expected.PackagingTime, actual.PackagingTime, msg)
		require.Equal(t, expected.ShipmentBySupplier, actual.ShipmentBySupplier, msg)
		require.Equal(t, expected.ReceptionByWarehouse, actual.ReceptionByWarehouse, msg)

		for j, expectedSD := range expected.SupplierDeliveryList {
			msgExt := fmt.Sprintf("%s (supplier %d)", msg, j)
			actualSD := actual.SupplierDeliveryList[j]

			require.Equal(t, expectedSD.WarehouseId, actualSD.WarehouseId, msgExt)
			require.Equal(t, expectedSD.ProcessingStartTime, actualSD.ProcessingStartTime, msgExt)
			require.Equal(t, expectedSD.ShipmentBySupplier, actualSD.ShipmentBySupplier, msgExt)
			require.Equal(t, expectedSD.ReceptionByWarehouse, actualSD.ReceptionByWarehouse, msgExt)
		}

		requirepb.Equal(t, expected.LastWarehouseOffset, actual.LastWarehouseOffset, msg)
	}
}

// ???????? ???? ???????????? ???????????? ???????????????? + ?????????????? ??????????????????????
func TestFindWarehousesAndMovements(t *testing.T) {
	for _, breakOnDeliveryWarehouse := range []bool{false, true} {
		testFindWarehousesAndMovements(t, breakOnDeliveryWarehouse)
	}
}

func testFindWarehousesAndMovements(t *testing.T, breakOnDeliveryWarehouse bool) {
	// 1. FF -> M -> L -> H|P - ???????? FF ?????????? ????????????????
	{
		pb := NewPathBuilder()
		whs1 := pb.AddWarehouse(pb.WithPartnerTypeFulfillment())
		mv1 := pb.AddMovement()
		pb.AddLinehaul()
		pb.AddHanding(pb.WithLocation(225))
		wantLastWarehouse := pb.graph.GetNodeByID(whs1)
		wantLastMovement := pb.graph.GetNodeByID(mv1)
		path := pb.GetSortablePath()
		whParams := path.findWarehousesAndMovements(breakOnDeliveryWarehouse)
		// ???????? ???? ?????????????????? ???????????????????? SegmentTypeWarehouse ???? ?????????????????? ????????
		require.Equal(t, wantLastWarehouse, whParams.warehouse)
		// ???????? ???? ?????????????????? SegmentTypeMovement ?? ???????????????? ServiceMovement ???? ?????????????????? SegmentTypeWarehouse ???? ?????????????????? ????????
		require.Equal(t, wantLastMovement, whParams.movement)
		// ???????? ???? ???????????? ?????????? ??????????????????????
		require.Empty(t, whParams.supplierData)
		// ?????????????????? ?????????? (FF) ?? ????????
		require.Equal(t, uint32(0), whParams.lastWarehousePosition)
	}
	// 2. DS -> M -> L -> H|P - ???????? ??????????????, ???? ?????????? ?????????? ????????????????
	{
		pb := NewPathBuilder()
		whs1 := pb.AddWarehouse(pb.WithPartnerTypeDropship())
		mv1 := pb.AddMovement()
		pb.AddLinehaul()
		pb.AddHanding(pb.WithLocation(225))
		wantLastWarehouse := pb.graph.GetNodeByID(whs1)
		wantLastMovement := pb.graph.GetNodeByID(mv1)
		path := pb.GetSortablePath()
		whParams := path.findWarehousesAndMovements(breakOnDeliveryWarehouse)
		// ???????? ???? ?????????????????? ???????????????????? SegmentTypeWarehouse ???? ?????????????????? ????????
		require.Equal(t, wantLastWarehouse, whParams.warehouse)
		// ???????? ???? ?????????????????? SegmentTypeMovement ?? ???????????????? ServiceMovement ???? ?????????????????? SegmentTypeWarehouse ???? ?????????????????? ????????
		require.Equal(t, wantLastMovement, whParams.movement)
		// ???????? ???? ???????????? ?????????? ??????????????????????
		require.Empty(t, whParams.supplierData)
		// ?????????????????? ?????????? (DS) ?? ????????
		require.Equal(t, uint32(0), whParams.lastWarehousePosition)

	}
	// 3. DS -> M -> SC -> M -> L -> H|P - ???????? ??????????????, ???? ?????????? ?????????? ???? SC
	{
		pb := NewPathBuilder()
		// ?????????? ???????????????????? ???????????? ???????? ?????????????????? ???????? Warehouse, ???? ?????? ?????????????? Sort(???? Sorting Center)
		// ?????????? ???????????????????? ???? ?????????? ???????? ?????????????????? ?????????????? ?? ????????
		// ?? ???????? ?????????? ???????????????? ???????? ?????????? SC
		whs1 := pb.AddWarehouse(pb.WithPartnerTypeDropship())
		mv1 := pb.AddMovement()
		whs2 := pb.AddWarehouse(
			pb.MakeProcessingService(),
		)
		mv2 := pb.AddMovement()
		pb.AddLinehaul()
		pb.AddHanding(pb.WithLocation(225))
		wantSupplierWarehouses := []*supplierData{{}}
		wantSupplierWarehouses[0].path = pb.path
		wantSupplierWarehouses[0].warehouse = pb.graph.GetNodeByID(whs1)
		wantSupplierWarehouses[0].movement = pb.graph.GetNodeByID(mv1)
		wantLastWarehouse := pb.graph.GetNodeByID(whs2)
		wantLastMovement := pb.graph.GetNodeByID(mv2)
		path := pb.GetSortablePath()
		whParams := path.findWarehousesAndMovements(breakOnDeliveryWarehouse)
		// ???????? ???? ?????????????????? ???????????????????? SegmentTypeWarehouse ???? ?????????????????? ????????
		require.Equal(t, wantLastWarehouse, whParams.warehouse)
		// ???????? ???? ?????????????????? SegmentTypeMovement ?? ???????????????? ServiceMovement ???? ?????????????????? SegmentTypeWarehouse ???? ?????????????????? ????????
		require.Equal(t, wantLastMovement, whParams.movement)
		// ???????? ???? Dropship ?? ???????????? ??????????????????????
		require.Equal(t, wantSupplierWarehouses, whParams.supplierData)
		// ?????????????????? ?????????? (SC) ?? ????????
		require.Equal(t, uint32(2), whParams.lastWarehousePosition)
	}
	// 4. DS -> M -> SC -> M -> L -> H|P
	{
		pb := NewPathBuilder()
		// ?????????? ???????????????????? ???????????? ???????? ?????????????????? ???????? Warehouse, ???? ?????? ?????????????? Sort(???? Sorting Center)
		// ?????????? ???????????????????? ???? ?????????? ???????? ?????????????????? ?????????????? ?? ????????
		whs1 := pb.AddWarehouse(pb.WithPartnerTypeDropship())
		mv1 := pb.AddMovement()
		whs2 := pb.AddWarehouse(pb.WithPartnerTypeSortingCenter())
		mv2 := pb.AddMovement()
		pb.AddLinehaul()
		pb.AddHanding(pb.WithLocation(225))
		wantSupplierWarehouses := []*supplierData{{}}
		wantSupplierWarehouses[0].path = pb.path
		wantSupplierWarehouses[0].warehouse = pb.graph.GetNodeByID(whs1)
		wantSupplierWarehouses[0].movement = pb.graph.GetNodeByID(mv1)
		wantLastWarehouse := pb.graph.GetNodeByID(whs2)
		wantLastMovement := pb.graph.GetNodeByID(mv2)
		path := pb.GetSortablePath()
		whParams := path.findWarehousesAndMovements(breakOnDeliveryWarehouse)
		// ???????? ???? ?????????????????? ???????????????????? SegmentTypeWarehouse ???? ?????????????????? ????????
		require.Equal(t, wantLastWarehouse, whParams.warehouse)
		// ???????? ???? ?????????????????? SegmentTypeMovement ?? ???????????????? ServiceMovement ???? ?????????????????? SegmentTypeWarehouse ???? ?????????????????? ????????
		require.Equal(t, wantLastMovement, whParams.movement)
		require.Equal(t, wantSupplierWarehouses, whParams.supplierData)
		// ?????????????????? ?????????? (SC) ?? ????????
		require.Equal(t, uint32(2), whParams.lastWarehousePosition)
	}
	//TODO: ?????????? ???? ???????????????? ?????????????????? ????????????????

	//TODO: ???????? ???? ???????????? SC ?? ????????(???? ?????????? ????????).
	//????????????????: ?? ?????????????? ?????????????????????? ??????????, ?? ???????????????????????? ?????????? SC. ?????????? ?????????????????? ???????? ????????????

	// 6. DS -> MV -> SC -> MV -> SC -> MV -> LH -> H|P
	// COMBINATOR-1045
	{
		pb := NewPathBuilder()
		dsh := pb.AddWarehouse(pb.WithPartnerTypeDropship())
		pb.AddMovement()
		whs1 := pb.AddWarehouse(pb.WithPartnerTypeSortingCenter())
		pb.AddMovement()
		pb.AddWarehouse(pb.WithPartnerTypeSortingCenter())
		pb.AddMovement()
		pb.AddLinehaul()
		pb.AddHanding(pb.WithLocation(225))
		wantDropship := pb.graph.GetNodeByID(dsh)
		wantFFWarehouse := pb.graph.GetNodeByID(whs1)
		path := pb.GetSortablePath()
		whParams := path.findWarehousesAndMovements(breakOnDeliveryWarehouse)
		require.Equal(t, len(whParams.supplierData), 1)
		require.Equal(t, wantDropship, whParams.supplierData[0].warehouse)
		require.Equal(t, wantFFWarehouse, whParams.warehouse)
		require.Equal(t, uint32(2), whParams.lastWarehousePosition)

	}
	// 7. DS -> MV -> SC -> MV -> Lavka -> MV -> LH -> H|P -  ?????????? - ???? ?????????????????? ??????????
	// COMBINATOR-1287
	{
		pb := NewPathBuilder()
		dsh := pb.AddWarehouse(pb.WithPartnerTypeDropship())
		pb.AddMovement()
		whs1 := pb.AddWarehouse(pb.WithPartnerTypeSortingCenter())
		pb.AddMovement()
		pb.AddWarehouse(pb.withPartnerType(enums.PartnerTypeDelivery), pb.MakeOnDemandYandexGoService())
		pb.AddMovement()
		pb.AddLinehaul()
		pb.AddHanding(pb.WithLocation(225))
		wantDropship := pb.graph.GetNodeByID(dsh)
		wantSortingCenter := pb.graph.GetNodeByID(whs1)
		path := pb.GetSortablePath()
		whParams := path.findWarehousesAndMovements(breakOnDeliveryWarehouse)
		require.Equal(t, len(whParams.supplierData), 1)
		require.Equal(t, wantDropship, whParams.supplierData[0].warehouse)
		require.Equal(t, wantSortingCenter, whParams.warehouse)
		require.Equal(t, uint32(2), whParams.lastWarehousePosition)

	}
	// 8. FF -> M -> Lavka -> M -> L -> H|P - ???????? FF ?????????? ????????????????, ???? ?????????? ?????????? ???????????????? ?????????? ??????????,
	// ???? ?????????? ???? ?????????????????? ?????????? COMBINATOR-1287
	{
		pb := NewPathBuilder()
		whs1 := pb.AddWarehouse(pb.WithPartnerTypeFulfillment())
		mv1 := pb.AddMovement()
		pb.AddWarehouse(pb.withPartnerType(enums.PartnerTypeDelivery), pb.MakeOnDemandYandexGoService())
		pb.AddMovement()
		pb.AddLinehaul()
		pb.AddHanding(pb.WithLocation(225))
		wantLastWarehouse := pb.graph.GetNodeByID(whs1)
		wantLastMovement := pb.graph.GetNodeByID(mv1)
		path := pb.GetSortablePath()
		whParams := path.findWarehousesAndMovements(breakOnDeliveryWarehouse)
		// ???????? ???? ?????????????????? ???????????????????? SegmentTypeWarehouse ???? ?????????????????? ????????
		require.Equal(t, wantLastWarehouse, whParams.warehouse)
		// ???????? ???? ?????????????????? SegmentTypeMovement ?? ???????????????? ServiceMovement ???? ?????????????????? SegmentTypeWarehouse ???? ?????????????????? ????????
		require.Equal(t, wantLastMovement, whParams.movement)
		// ???????? ???? ???????????? ?????????? ??????????????????????
		require.Empty(t, whParams.supplierData)
		// ?????????????????? ?????????? (FF) ?? ????????
		require.Equal(t, uint32(0), whParams.lastWarehousePosition)
	}
	// 9. DS -> M -> Lavka -> MV -> L -> H|P - ???????? ??????????????, ???? ?????????? ?????????? ???????????????? ?????????? ??????????,
	// ???? ?????????? ???? ?????????????????? ?????????? COMBINATOR-1287
	{
		pb := NewPathBuilder()
		whs1 := pb.AddWarehouse(pb.WithPartnerTypeDropship())
		mv1 := pb.AddMovement()
		pb.AddWarehouse(pb.withPartnerType(enums.PartnerTypeDelivery), pb.MakeOnDemandYandexGoService())
		pb.AddMovement()
		pb.AddLinehaul()
		pb.AddHanding(pb.WithLocation(225))
		wantLastWarehouse := pb.graph.GetNodeByID(whs1)
		wantLastMovement := pb.graph.GetNodeByID(mv1)
		path := pb.GetSortablePath()
		whParams := path.findWarehousesAndMovements(breakOnDeliveryWarehouse)
		// ???????? ???? ?????????????????? ???????????????????? SegmentTypeWarehouse ???? ?????????????????? ????????
		require.Equal(t, wantLastWarehouse, whParams.warehouse)
		// ???????? ???? ?????????????????? SegmentTypeMovement ?? ???????????????? ServiceMovement ???? ?????????????????? SegmentTypeWarehouse ???? ?????????????????? ????????
		require.Equal(t, wantLastMovement, whParams.movement)
		// ???????? ???? ???????????? ?????????? ??????????????????????
		require.Empty(t, whParams.supplierData)
		// ?????????????????? ?????????? (DS) ?? ????????
		require.Equal(t, uint32(0), whParams.lastWarehousePosition)
	}
}

func TestSetOptionSupplierTimes(t *testing.T) {
	{
		//???????? ???? nil. ???????? ?? findWarehousesAndMovements ???? ?????????????? ???? ?????????? ???????? WH+MV ?????? ????????????????????
		supplierSpecs, _ := setOptionSupplierTimes(nil, DeliveryTypeUnknown)
		require.Empty(t, supplierSpecs)
	}
	{
		// ???????????????? ???? ?????????????????????? ????. ?????? ?????????? ?????????????????? PartnerLmsID ?? WH ?? ???? ?????????????????? ?? M
		wantWarehouseProcessingStart := time.Date(2021, time.February, 10, 12, 13, 0, 0, time.UTC)
		shipmentStart := time.Date(2021, time.February, 10, 17, 30, 0, 0, time.UTC)
		shipmentDuration := int32(240)
		wantShipmentBySupplier := shipmentStart.Add(time.Duration(shipmentDuration) * time.Minute)
		wantPartnerLmsID := int64(48342)
		pb := NewPathBuilder()
		supplierWarehouse := pb.AddWarehouse(
			pb.WithPartnerLmsID(wantPartnerLmsID),
			pb.MakeProcessingService(pb.WithStartTime(wantWarehouseProcessingStart)),
			pb.MakeShipmentService(
				pb.WithStartTime(shipmentStart),
				pb.WithDuration(shipmentDuration),
			),
		)
		supplierMovement := pb.AddMovement()
		supplierWarehouses := []*supplierData{
			{
				warehouse: pb.graph.GetNodeByID(supplierWarehouse),
				movement:  pb.graph.GetNodeByID(supplierMovement),
				path:      pb.path,
			},
		}
		supplierSpecs, _ := setOptionSupplierTimes(supplierWarehouses, DeliveryTypeCourier)
		require.Equal(t, wantWarehouseProcessingStart, supplierSpecs[0].WarehouseProcessingStart)
		require.Equal(t, wantShipmentBySupplier, supplierSpecs[0].ShipmentBySupplier)
		require.Equal(t, wantPartnerLmsID, supplierSpecs[0].WarehousePartnerID)
	}
	{
		// ???????????????????? ???????????????? ????????. ?????? ?????????? ???????????? ???????????????????? PartnerLmsID ?? WH ?? M ????????????????????
		wantWarehouseProcessingStart := time.Date(2021, time.February, 10, 12, 13, 0, 0, time.UTC)
		shipmentStart := time.Date(2021, time.February, 10, 14, 22, 0, 0, time.UTC)
		shipmentDuration := int32(300)
		wantShipmentBySupplier := shipmentStart.Add(time.Duration(shipmentDuration) * time.Minute)
		wantPartnerLmsID := int64(47755)
		pb := NewPathBuilder()
		supplierWarehouse := pb.AddWarehouse(
			pb.WithPartnerLmsID(wantPartnerLmsID),
			pb.MakeProcessingService(pb.WithStartTime(wantWarehouseProcessingStart)),
		)
		supplierMovement := pb.AddMovement(
			pb.WithPartnerLmsID(wantPartnerLmsID),
			pb.MakeShipmentService(
				pb.WithStartTime(shipmentStart),
				pb.WithDuration(shipmentDuration),
			),
		)
		supplierWarehouses := []*supplierData{
			{
				warehouse: pb.graph.GetNodeByID(supplierWarehouse),
				movement:  pb.graph.GetNodeByID(supplierMovement),
				path:      pb.path,
			},
		}
		supplierSpecs, err := setOptionSupplierTimes(supplierWarehouses, DeliveryTypePickup)
		require.NoError(t, err)
		require.Equal(t, wantWarehouseProcessingStart, supplierSpecs[0].WarehouseProcessingStart)
		require.Equal(t, wantShipmentBySupplier, supplierSpecs[0].ShipmentBySupplier)
		require.Equal(t, wantPartnerLmsID, supplierSpecs[0].WarehousePartnerID)
	}
	{
		// ???????????????? ???? ????????????: ???????????????????? ????????????????
		pb := NewPathBuilder()
		supplierWarehouse := pb.AddWarehouse(pb.WithPartnerLmsID(47755))
		supplierMovement := pb.AddMovement()
		supplierWarehouses := []*supplierData{
			{
				warehouse: pb.graph.GetNodeByID(supplierWarehouse),
				movement:  pb.graph.GetNodeByID(supplierMovement),
				path:      pb.path,
			},
		}
		supplierSpecs, err := setOptionSupplierTimes(supplierWarehouses, DeliveryTypeCourier)
		wantError := "failed to find processing start time or shipment time for warehouse 47755"
		require.Empty(t, supplierSpecs)
		require.Equal(t, wantError, err.Error())
	}
	{
		// ???????????????? ???? ????????????: ???????????????????? ???????? ???????????? ?????????????? ServiceProcessing ?? ???????????????? Warehouse
		pb := NewPathBuilder()
		supplierWarehouse := pb.AddWarehouse(
			pb.MakeProcessingService(pb.WithoutStartTime()),
			pb.WithPartnerLmsID(47755),
		)
		supplierMovement := pb.AddMovement()
		supplierWarehouses := []*supplierData{
			{
				warehouse: pb.graph.GetNodeByID(supplierWarehouse),
				movement:  pb.graph.GetNodeByID(supplierMovement),
				path:      pb.path,
			},
		}
		supplierSpecs, err := setOptionSupplierTimes(supplierWarehouses, DeliveryTypePickup)
		wantError := fmt.Sprintf("cannot find warehouse processing service's start time %d", pb.graph.GetNodeByID(supplierWarehouse).CourierServices[0].ID)
		require.Empty(t, supplierSpecs)
		require.Equal(t, wantError, err.Error())
	}
	{
		// ???????????????? ???? ????????????: ???????????????????? ???????? ???????????? ?????????????? ServiceShipment ?? ???????????????? Warehouse(???????????? ???????????? ???? ????????????????????)
		pb := NewPathBuilder()
		supplierWarehouse := pb.AddWarehouse(
			pb.WithPartnerLmsID(47755),
			pb.MakeProcessingService(),
			pb.MakeShipmentService(pb.WithoutStartTime()),
		)
		supplierMovement := pb.AddMovement()
		supplierWarehouses := []*supplierData{
			{
				warehouse: pb.graph.GetNodeByID(supplierWarehouse),
				movement:  pb.graph.GetNodeByID(supplierMovement),
				path:      pb.path,
			},
		}
		supplierSpecs, err := setOptionSupplierTimes(supplierWarehouses, DeliveryTypeCourier)
		require.Empty(t, supplierSpecs)
		wantError := fmt.Sprintf("cannot find warehouse shipment service's start time %d", pb.graph.GetNodeByID(supplierWarehouse).CourierServices[1].ID)
		require.Equal(t, wantError, err.Error())
	}
	{
		// ???????????????? ???? ????????????: ???????????????????? ???????? ???????????? ?????????????? ServiceShipment ?? ???????????????? Warehouse(????????????????????)
		// ???????? ?????????????????? ???????????????????? ???? ????????, ???? ?????????????????? ???????????????? PartnerLmsID(0) ?????????? ?????????? ??????????
		pb := NewPathBuilder()
		supplierWarehouse := pb.AddWarehouse()
		supplierMovement := pb.AddMovement(pb.MakeShipmentService(pb.WithoutStartTime()))
		supplierWarehouses := []*supplierData{
			{
				warehouse: pb.graph.GetNodeByID(supplierWarehouse),
				movement:  pb.graph.GetNodeByID(supplierMovement),
				path:      pb.path,
			},
		}
		supplierSpecs, err := setOptionSupplierTimes(supplierWarehouses, DeliveryTypePickup)
		require.Empty(t, supplierSpecs)
		wantError := fmt.Sprintf("cannot find movement service's start time %d", pb.graph.GetNodeByID(supplierMovement).CourierServices[0].ID)
		require.Equal(t, wantError, err.Error())
	}
	{
		// ???????????????? ???? ????????????: ???????????????????? ?????????????? ServiceShipment ?? ???????????????? Warehouse(????????????????????)
		// ???????? ?????????????????? ???????????????????? ???? ????????, ???? ?????????????????? ???????????????? PartnerLmsID(0) ?????????? ?????????? ??????????
		pb := NewPathBuilder()
		supplierWarehouse := pb.AddWarehouse()
		supplierMovement := pb.AddMovement()
		supplierWarehouses := []*supplierData{
			{
				warehouse: pb.graph.GetNodeByID(supplierWarehouse),
				movement:  pb.graph.GetNodeByID(supplierMovement),
				path:      pb.path,
			},
		}
		supplierSpecs, err := setOptionSupplierTimes(supplierWarehouses, DeliveryTypeCourier)
		require.Empty(t, supplierSpecs)
		wantError := fmt.Sprintf("cannot find movement service's shipment date %d", pb.graph.GetNodeByID(supplierMovement).ID)
		require.Equal(t, wantError, err.Error())
	}
}

func TestCalcDeliveryParams(t *testing.T) {
	optionsList := []calcDeliveryParamsOptions{
		calcDeliveryParamsOptions{
			breakOnDeliveryWarehouse: false,
		},
		calcDeliveryParamsOptions{
			breakOnDeliveryWarehouse: true,
		},
	}
	for _, options := range optionsList {
		testCalcDeliveryParams(t, options)
	}
}

func testCalcDeliveryParams(t *testing.T, options calcDeliveryParamsOptions) {
	{
		// 1. FF -> M -> L -> H|P - ???????? FF, ???? ?????????? ????????????????
		// 2. DS -> M -> L -> H|P - ???????? ??????????????, ???? ?????????? ?????????? ????????????????
		wantWarehouseProcessingStart := time.Date(2021, time.February, 9, 13, 0, 0, 0, time.UTC)
		wantMovementStart := time.Date(2021, time.February, 12, 15, 44, 0, 0, time.UTC)
		movementDuration := int32(5)
		wantMovementDuration := time.Duration(movementDuration) * time.Minute
		wantLogisticDayStart := timex.DayTime{Hour: 13}

		pb := NewPathBuilder()
		pb.AddWarehouse(
			pb.MakeProcessingService(pb.WithStartTime(wantWarehouseProcessingStart)),
			pb.WithLogisticDayStart(wantLogisticDayStart),
		)
		pb.AddMovement(
			pb.MakeMovementService(
				pb.WithStartTime(wantMovementStart),
				pb.WithDuration(movementDuration),
			),
		)
		pb.AddLinehaul()
		pb.AddHanding(pb.WithLocation(225))
		path := pb.GetSortablePath()
		deliverySpec, _ := path.calcDeliveryParams(context.Background(), DeliveryTypeCourier, options)
		require.Equal(t, wantWarehouseProcessingStart, deliverySpec.WarehouseProcessingStart)
		require.Equal(t, wantMovementStart, deliverySpec.MovementStart)
		require.Equal(t, wantMovementDuration, deliverySpec.MovementDuration)
		require.Empty(t, deliverySpec.SupplierSpecList)
		require.Equal(t, wantLogisticDayStart, deliverySpec.LogisticDayStart)
		require.Equal(t, uint32(0), deliverySpec.LastWarehousePosition)
	}

	// 3. DS -> M -> SC -> M -> L -> H|P - ???????? ??????????????, ???? ?????????? ?????????? ???? SC
	// 4. CD -> M -> FF -> M -> L -> H|P - ???????? ????????????????, ???? ?????????? ?? ?????? ???? FF ??????????. ???????????? ?? ?????????? ????????????????????(???????????? CrossdockPaths)
	{
		// 3-?? ??????????????. 4-???? ?????????? ?????????????????????????? ?????? ?????????????? ???????????? 3-???? ?? ???????????? ???????? ????????????
		wantWarehouseProcessingStart := time.Date(2021, time.February, 14, 13, 0, 0, 0, time.UTC)
		wantMovementStart := time.Date(2021, time.February, 13, 23, 44, 0, 0, time.UTC)
		movementDuration := int32(120)
		wantMovementDuration := time.Duration(movementDuration) * time.Minute
		wantLogisticDayStart := timex.DayTime{Hour: 11}
		wantSupplierWarehouseProcessingStart := time.Date(2021, time.February, 10, 12, 13, 0, 0, time.UTC)
		shipmentStart := time.Date(2021, time.February, 10, 17, 30, 0, 0, time.UTC)
		shipmentDuration := int32(240)
		wantShipmentBySupplier := shipmentStart.Add(time.Duration(shipmentDuration) * time.Minute)
		wantPartnerLmsID := int64(48342)
		pb := NewPathBuilder()
		// ???????????????? ???? ?????????????????????? ????. ?????? ?????????? ?????????????????? PartnerLmsID ?? WH ?? ???? ?????????????????? ?? M
		pb.AddWarehouse(
			pb.WithPartnerTypeDropship(),
			pb.WithPartnerLmsID(wantPartnerLmsID),
			pb.MakeProcessingService(pb.WithStartTime(wantSupplierWarehouseProcessingStart)),
			pb.MakeShipmentService(
				pb.WithStartTime(shipmentStart),
				pb.WithDuration(shipmentDuration),
			),
		)
		pb.AddMovement()
		pb.AddWarehouse(
			pb.WithPartnerTypeSortingCenter(),
			pb.WithLogisticDayStart(wantLogisticDayStart),
			pb.MakeProcessingService(pb.WithStartTime(wantWarehouseProcessingStart)),
		)
		pb.AddMovement(
			pb.MakeMovementService(
				pb.WithStartTime(wantMovementStart),
				pb.WithDuration(movementDuration),
			),
		)
		pb.AddLinehaul()
		pb.AddHanding(pb.WithLocation(225))
		path := pb.GetSortablePath()
		deliverySpec, _ := path.calcDeliveryParams(context.Background(), DeliveryTypeCourier, options)
		require.Equal(t, wantSupplierWarehouseProcessingStart, deliverySpec.SupplierSpecList[0].WarehouseProcessingStart)
		require.Equal(t, wantShipmentBySupplier, deliverySpec.SupplierSpecList[0].ShipmentBySupplier)
		require.Equal(t, wantPartnerLmsID, deliverySpec.SupplierSpecList[0].WarehousePartnerID)
		require.Equal(t, wantWarehouseProcessingStart, deliverySpec.WarehouseProcessingStart)
		require.Equal(t, wantMovementStart, deliverySpec.MovementStart)
		require.Equal(t, wantMovementDuration, deliverySpec.MovementDuration)
		require.NotEmpty(t, deliverySpec.SupplierSpecList)
		require.Equal(t, wantLogisticDayStart, deliverySpec.LogisticDayStart)
		require.Equal(t, uint32(2), deliverySpec.LastWarehousePosition)
	}
	{
		//???????????? warehouse ?????? movement
		_, err := NewPathBuilder().GetSortablePath().calcDeliveryParams(context.Background(), DeliveryTypeCourier, options)
		require.Equal(t, "there is no warehouse or movement segment", err.Error())
	}
	{
		pb := NewPathBuilder()
		warehouse := pb.AddWarehouse()
		pb.AddMovement()
		_, err := pb.GetSortablePath().calcDeliveryParams(context.Background(), DeliveryTypeCourier, options)
		wantError := fmt.Sprintf("there is no processing service's start time in warehouse segment %d", pb.graph.GetNodeByID(warehouse).ID)
		require.Equal(t, wantError, err.Error())
	}
	{
		pb := NewPathBuilder()
		pb.AddWarehouse(pb.MakeProcessingService())
		movement := pb.AddMovement()
		_, err := pb.GetSortablePath().calcDeliveryParams(context.Background(), DeliveryTypeCourier, options)
		wantError := fmt.Sprintf("there is no movement service's start time in movement segment %d", pb.graph.GetNodeByID(movement).ID)
		require.Equal(t, wantError, err.Error())
	}
	{
		// ???????? DS ?????????? SC. SC ?????? ?????????????? ???????????? ?????????????????????????? ??????????.
		pb := NewPathBuilder()
		pb.AddWarehouse(
			pb.WithPartnerTypeDropship(),
			pb.WithPartnerLmsID(48342), // ?????? ???????? ?????????? ???? ?????????????? ?? ????????????????????, ?????? ???????????? ?????????????? ??????????
			pb.MakeProcessingService(),
			pb.MakeShipmentService(),
		)
		pb.AddMovement()
		pb.AddWarehouse(
			pb.MakeProcessingService(),
		)
		pb.AddMovement(
			pb.MakeMovementService(),
		)
		deliverySpec, _ := pb.GetSortablePath().calcDeliveryParams(context.Background(), DeliveryTypePickup, options)
		require.True(t, deliverySpec.LogisticDayStart.IsZero())
	}
}
