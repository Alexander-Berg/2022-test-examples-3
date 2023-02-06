package lite

import (
	"context"
	"testing"

	"github.com/stretchr/testify/require"

	bg "a.yandex-team.ru/market/combinator/pkg/biggeneration"
	cons "a.yandex-team.ru/market/combinator/pkg/consolidate_carts"
	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/market/combinator/pkg/graph"
	"a.yandex-team.ru/market/combinator/pkg/its"
	"a.yandex-team.ru/market/combinator/pkg/outlets"
	tr "a.yandex-team.ru/market/combinator/pkg/tarifficator"
	cr "a.yandex-team.ru/market/combinator/proto/grpc"
)

const (
	consolidateCartsWH = 172
)

// Paths WH1->MV1->WH->MV->L->P, WH2->MV2->WH->MV->L->H, WH3->MV->L->H
func prepareEasyPathConsolidateCarts() *bg.GenerationData {
	gb := graph.NewGraphBuilder()
	warehouse := gb.MakeWarehouse(
		graph.WarehouseWithPartner(consolidateCartsWH),
		graph.WarehouseWithRegion(geobase.RegionMoscow),
	)
	whsPartnerLmsID := gb.GetGraph().GetNodeByID(warehouse).PartnerLmsID
	warehouse1 := gb.MakeWarehouse(
		graph.WarehouseWithPartner(consolidateCartsWH+1),
		graph.WarehouseWithRegion(geobase.RegionMoscow),
	)
	whs1PartnerLmsID := gb.GetGraph().GetNodeByID(warehouse).PartnerLmsID
	warehouse2 := gb.MakeWarehouse(
		graph.WarehouseWithPartner(consolidateCartsWH+2),
		graph.WarehouseWithRegion(geobase.RegionMoscow),
	)
	whs2PartnerLmsID := gb.GetGraph().GetNodeByID(warehouse).PartnerLmsID
	warehouse3 := gb.MakeWarehouse(
		graph.WarehouseWithPartner(consolidateCartsWH+3),
		graph.WarehouseWithRegion(geobase.RegionMoscow),
	)
	movement := gb.MakeMovement(
		graph.MovementWithShipment(),
		graph.MovementWithPartner(whsPartnerLmsID),
	)
	movement1 := gb.MakeMovement(
		graph.MovementWithShipment(),
		graph.MovementWithPartner(whs1PartnerLmsID),
	)
	movement2 := gb.MakeMovement(
		graph.MovementWithShipment(),
		graph.MovementWithPartner(whs2PartnerLmsID),
	)
	linehaul := gb.MakeLinehaul(
		graph.LinehaulWithPartner(whsPartnerLmsID),
	)
	linehaul2 := gb.MakeLinehaul(
		graph.LinehaulWithPartner(whsPartnerLmsID),
		graph.LinehaulWithRegion(geobase.RegionSaintPetersburg),
	)
	linehaul3 := gb.MakeLinehaul(
		graph.LinehaulWithPartner(whsPartnerLmsID),
		graph.LinehaulWithRegion(geobase.RegionEkaterinburg),
	)

	_ = gb.MakeHanding(
		graph.HandingWithPartner(whsPartnerLmsID),
		graph.HandingWithRegion(geobase.RegionMoscow),
		graph.HandingWithTrivialSchedule(),
	)
	_ = gb.MakeHanding(
		graph.HandingWithPartner(whsPartnerLmsID),
		graph.HandingWithRegion(geobase.RegionSaintPetersburg),
		graph.HandingWithTrivialSchedule(),
	)
	_ = gb.MakeHanding(
		graph.HandingWithPartner(whsPartnerLmsID),
		graph.HandingWithRegion(geobase.RegionEkaterinburg),
		graph.HandingWithTrivialSchedule(),
	)
	pickup := gb.MakePickup(
		graph.PickupWithPartner(whsPartnerLmsID),
		graph.PickupWithRegion(geobase.RegionMoscow),
	)

	gb.AddEdge(warehouse1, movement1)
	gb.AddEdge(warehouse2, movement2)
	gb.AddEdge(warehouse3, movement)
	gb.AddEdge(movement1, warehouse)
	gb.AddEdge(movement2, warehouse)

	gb.AddEdge(warehouse, movement)
	gb.AddEdge(movement, linehaul)
	gb.AddEdge(movement, linehaul2)
	gb.AddEdge(movement, linehaul3)
	g := gb.GetGraph()
	g.Finish(context.Background())

	tariffsBuilder := tr.NewTariffsBuilder()
	_ = tariffsBuilder.MakeTariff(
		tr.TariffWithPartner(uint64(whsPartnerLmsID)),
		tr.TariffWithRegion(geobase.RegionMoscow, geobase.RegionMoscow),
		tr.TariffWithDays(uint32(1), uint32(2)),
		tr.TariffWithPickup(g.GetNodeByID(pickup).PointLmsID),
		tr.TariffWithIsMarketCourier(true),
	)

	_ = tariffsBuilder.MakeTariff(
		tr.TariffWithPartner(uint64(whsPartnerLmsID)),
		tr.TariffWithRegion(geobase.RegionMoscow, geobase.RegionMoscow),
		tr.TariffWithDays(uint32(1), uint32(2)),
		tr.TariffWithIsMarketCourier(true),
	)
	_ = tariffsBuilder.MakeTariff(
		tr.TariffWithPartner(uint64(whsPartnerLmsID)),
		tr.TariffWithRegion(geobase.RegionMoscow, geobase.RegionSaintPetersburg),
		tr.TariffWithDays(uint32(1), uint32(2)),
		tr.TariffWithIsMarketCourier(true),
	)
	_ = tariffsBuilder.MakeTariff(
		tr.TariffWithPartner(uint64(whsPartnerLmsID)),
		tr.TariffWithRegion(geobase.RegionMoscow, geobase.RegionEkaterinburg),
		tr.TariffWithDays(uint32(1), uint32(2)),
		tr.TariffWithIsMarketCourier(false),
	)

	regionMap := geobase.NewExample()
	generation := &bg.GenerationData{
		RegionMap: regionMap,
		Graph:     g,
		Outlets: outlets.Make([]outlets.Outlet{
			{
				ID:       g.GetNodeByID(pickup).PointLmsID,
				RegionID: geobase.RegionMoscow,
				Type:     outlets.Depot,
			},
		}, &regionMap, nil),
		TariffsFinder: tariffsBuilder.TariffsFinder,
	}

	return generation
}

func TestConsolidateCarts(t *testing.T) {
	ctx := context.Background()
	settings, _ := its.NewStringSettingsHolder(`{"use_days_off_archive_pickup": true}`)
	its.SetSettingsHolder(settings)
	env, cancel := NewEnv(t, prepareEasyPathConsolidateCarts(), settings)
	defer func() {
		cancel()
	}()
	req := &cr.ConsolidateCartsRequest{
		CartsByDestination: []*cr.ConsolidateCartsRequest_CartsByDestination{
			{
				Carts: []*cr.CartData{
					cons.MakeConsolidateCartData(
						cons.CartDataWithLabel("cart1"),
						cons.CartDataWithShopID(1),
						cons.CartDataWithPartner(consolidateCartsWH+1),
						cons.CartDataWithDates(
							&cr.Date{Day: 01, Month: 01, Year: 2001},
						),
					),
					cons.MakeConsolidateCartData(
						cons.CartDataWithLabel("cart2"),
						cons.CartDataWithShopID(1),
						cons.CartDataWithPartner(consolidateCartsWH+2),
						cons.CartDataWithDates(
							&cr.Date{Day: 01, Month: 01, Year: 2001},
							&cr.Date{Day: 02, Month: 01, Year: 2001},
						),
					),
					cons.MakeConsolidateCartData(
						cons.CartDataWithLabel("cart3"),
						cons.CartDataWithShopID(1),
						cons.CartDataWithPartner(consolidateCartsWH),
						cons.CartDataWithDates(
							&cr.Date{Day: 01, Month: 01, Year: 2001},
						),
					),
					cons.MakeConsolidateCartData(
						cons.CartDataWithLabel("cart4"),
						cons.CartDataWithShopID(1),
						cons.CartDataWithPartner(consolidateCartsWH+3),
						cons.CartDataWithDates(
							&cr.Date{Day: 02, Month: 01, Year: 2001},
						),
					),
					cons.MakeConsolidateCartData(
						cons.CartDataWithLabel("cart5"),
						cons.CartDataWithShopID(1),
						cons.CartDataWithPartner(consolidateCartsWH),
						cons.CartDataWithDates(
							&cr.Date{Day: 02, Month: 01, Year: 2001},
						),
					),
				},
				Destination: &cr.PointIds{
					RegionId:  uint32(geobase.RegionMoscow),
					GpsCoords: &cr.GpsCoords{Lat: 2, Lon: 2},
				},
			},
			{
				Carts: []*cr.CartData{
					cons.MakeConsolidateCartData(
						cons.CartDataWithLabel("cart6"),
						cons.CartDataWithShopID(1),
						cons.CartDataWithPartner(consolidateCartsWH+1),
						cons.CartDataWithDates(
							&cr.Date{Day: 01, Month: 01, Year: 2001},
						),
					),
					cons.MakeConsolidateCartData(
						cons.CartDataWithLabel("cart7"),
						cons.CartDataWithShopID(1),
						cons.CartDataWithPartner(consolidateCartsWH+1),
						cons.CartDataWithDates(
							&cr.Date{Day: 02, Month: 01, Year: 2001},
							&cr.Date{Day: 03, Month: 01, Year: 2001},
							&cr.Date{Day: 04, Month: 01, Year: 2001},
						),
					),
					cons.MakeConsolidateCartData(
						cons.CartDataWithLabel("cart8"),
						cons.CartDataWithShopID(1),
						cons.CartDataWithPartner(consolidateCartsWH+1),
						cons.CartDataWithDates(
							&cr.Date{Day: 02, Month: 01, Year: 2001},
							&cr.Date{Day: 03, Month: 01, Year: 2001},
							&cr.Date{Day: 04, Month: 01, Year: 2001},
						),
					),
				},
				Destination: &cr.PointIds{
					RegionId:  uint32(geobase.RegionSaintPetersburg),
					GpsCoords: &cr.GpsCoords{Lat: 1, Lon: 1},
				},
			},
			{
				Carts: []*cr.CartData{
					cons.MakeConsolidateCartData(
						cons.CartDataWithLabel("cart9"),
						cons.CartDataWithShopID(1),
						cons.CartDataWithPartner(consolidateCartsWH+1),
						cons.CartDataWithDates(
							&cr.Date{Day: 02, Month: 01, Year: 2001},
						),
					),
					cons.MakeConsolidateCartData(
						cons.CartDataWithLabel("cart10"),
						cons.CartDataWithShopID(1),
						cons.CartDataWithPartner(consolidateCartsWH+1),
						cons.CartDataWithDates(
							&cr.Date{Day: 02, Month: 01, Year: 2001},
						),
					),
					cons.MakeConsolidateCartData(
						cons.CartDataWithLabel("cart11"),
						cons.CartDataWithShopID(1),
						cons.CartDataWithPartner(consolidateCartsWH+1),
						cons.CartDataWithDates(
							&cr.Date{Day: 02, Month: 01, Year: 2001},
						),
					),
				},
				Destination: &cr.PointIds{
					RegionId: uint32(geobase.RegionSaintPetersburg),
				},
			},
			{
				Carts: []*cr.CartData{
					cons.MakeConsolidateCartData(
						cons.CartDataWithLabel("cart12"),
						cons.CartDataWithShopID(1),
						cons.CartDataWithPartner(consolidateCartsWH+1),
						cons.CartDataWithDates(
							&cr.Date{Day: 02, Month: 01, Year: 2001},
						),
					),
				},
				Destination: &cr.PointIds{
					RegionId:  uint32(geobase.RegionMoscow),
					GpsCoords: &cr.GpsCoords{Lat: 55.75583, Lon: 37.6172},
				},
			},
			{
				Carts: []*cr.CartData{
					cons.MakeConsolidateCartData(
						cons.CartDataWithLabel("cart13"),
						cons.CartDataWithShopID(1),
						cons.CartDataWithPartner(consolidateCartsWH+1),
						cons.CartDataWithDates(
							&cr.Date{Day: 02, Month: 01, Year: 2001},
						),
					),
				},
				Destination: &cr.PointIds{
					RegionId:  uint32(geobase.RegionMoscow),
					GpsCoords: &cr.GpsCoords{Lat: 55.75584, Lon: 37.6172},
				},
			},
			{
				Carts: []*cr.CartData{
					cons.MakeConsolidateCartData(
						cons.CartDataWithLabel("cart14"),
						cons.CartDataWithShopID(1),
						cons.CartDataWithPartner(consolidateCartsWH+1),
						cons.CartDataWithDates(
							&cr.Date{Day: 02, Month: 01, Year: 2001},
						),
					),
					cons.MakeConsolidateCartData(
						cons.CartDataWithLabel("cart15"),
						cons.CartDataWithShopID(1),
						cons.CartDataWithPartner(consolidateCartsWH+1),
						cons.CartDataWithDates(
							&cr.Date{Day: 02, Month: 01, Year: 2001},
						),
					),
				},
				Destination: &cr.PointIds{
					RegionId:  uint32(geobase.RegionEkaterinburg),
					GpsCoords: &cr.GpsCoords{Lat: 55.1, Lon: 37.1},
				},
			},
			{
				Carts: []*cr.CartData{
					cons.MakeConsolidateCartData(
						cons.CartDataWithLabel("cart16"),
						cons.CartDataWithShopID(1),
						cons.CartDataWithPartner(consolidateCartsWH+1),
						cons.CartDataWithDates(
							&cr.Date{Day: 02, Month: 01, Year: 2001},
						),
					),
					cons.MakeConsolidateCartData(
						cons.CartDataWithLabel("cart17"),
						cons.CartDataWithShopID(1),
						cons.CartDataWithPartner(consolidateCartsWH+1),
						cons.CartDataWithDates(
							&cr.Date{Day: 02, Month: 01, Year: 2001},
						),
					),
				},
				Destination: &cr.PointIds{
					RegionId:  uint32(geobase.RegionNovosibirsk),
					GpsCoords: &cr.GpsCoords{Lat: 55.5, Lon: 37.5},
				},
			},
		},
	}
	resp, err := env.Client.ConsolidateCarts(ctx, req)
	require.NoError(t, err)
	require.NotNil(t, resp)
	wantGroups := []*cr.ConsolidationGrouping{
		{
			CartLabels:     []string{"cart1", "cart2", "cart3"},
			AvailableDates: []string{"2001-01-01"},
		},
		{
			CartLabels:     []string{"cart5"},
			AvailableDates: []string{"2001-01-02"},
		},
		{
			CartLabels:     []string{"cart4"},
			AvailableDates: []string{"2001-01-02"},
		},
		{
			CartLabels:     []string{"cart7", "cart8"},
			AvailableDates: []string{"2001-01-02", "2001-01-03", "2001-01-04"},
		},
		{
			CartLabels:     []string{"cart6"},
			AvailableDates: []string{"2001-01-01"},
		},
		{
			CartLabels:     []string{"cart12", "cart13"},
			AvailableDates: []string{"2001-01-02"},
		},
		{
			CartLabels:     []string{"cart9"},
			AvailableDates: []string{"2001-01-02"},
		},
		{
			CartLabels:     []string{"cart10"},
			AvailableDates: []string{"2001-01-02"},
		},
		{
			CartLabels:     []string{"cart11"},
			AvailableDates: []string{"2001-01-02"},
		},
		{
			CartLabels:     []string{"cart14"},
			AvailableDates: []string{"2001-01-02"},
		},
		{
			CartLabels:     []string{"cart15"},
			AvailableDates: []string{"2001-01-02"},
		},
		{
			CartLabels:     []string{"cart16"},
			AvailableDates: []string{"2001-01-02"},
		},
		{
			CartLabels:     []string{"cart17"},
			AvailableDates: []string{"2001-01-02"},
		},
	}
	require.Equal(t, resp.Groups, wantGroups)
}
