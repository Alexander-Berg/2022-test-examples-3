package lite

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	bg "a.yandex-team.ru/market/combinator/pkg/biggeneration"
	"a.yandex-team.ru/market/combinator/pkg/daysoff"
	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/market/combinator/pkg/graph"
	"a.yandex-team.ru/market/combinator/pkg/its"
	"a.yandex-team.ru/market/combinator/pkg/outlets"
	tr "a.yandex-team.ru/market/combinator/pkg/tarifficator"
	common1 "a.yandex-team.ru/market/combinator/proto/common"
	pb "a.yandex-team.ru/market/combinator/proto/grpc"
)

const (
	archivePartner = 172
)

// Paths WH->MV->L->P, WH->MV->L->H
func prepareEasyPath() *bg.GenerationData {
	gb := graph.NewGraphBuilder()
	warehouse := gb.MakeWarehouse(
		graph.WarehouseWithPartner(archivePartner),
		graph.WarehouseWithRegion(geobase.RegionMoscow),
	)
	whsPartnerID := gb.GetGraph().GetNodeByID(warehouse).PartnerLmsID
	movement := gb.MakeMovement(
		graph.MovementWithShipment(),
		graph.MovementWithPartner(whsPartnerID),
	)
	linehaul := gb.MakeLinehaul(
		graph.LinehaulWithPartner(whsPartnerID),
	)

	_ = gb.MakeHanding(
		graph.HandingWithPartner(whsPartnerID),
		graph.HandingWithRegion(geobase.RegionMoscow),
		graph.HandingWithTrivialSchedule(),
	)
	pickup := gb.MakePickup(
		graph.PickupWithPartner(whsPartnerID),
		graph.PickupWithRegion(geobase.RegionMoscow),
	)

	gb.AddEdge(warehouse, movement)
	gb.AddEdge(movement, linehaul)

	g := gb.GetGraph()
	g.Finish(context.Background())

	tariffsBuilder := tr.NewTariffsBuilder()
	_ = tariffsBuilder.MakeTariff(
		tr.TariffWithPartner(uint64(whsPartnerID)),
		tr.TariffWithRegion(geobase.RegionMoscow, geobase.RegionMoscow),
		tr.TariffWithDays(uint32(1), uint32(2)),
		tr.TariffWithPickup(g.GetNodeByID(pickup).PointLmsID),
	)

	_ = tariffsBuilder.MakeTariff(
		tr.TariffWithPartner(uint64(whsPartnerID)),
		tr.TariffWithRegion(geobase.RegionMoscow, geobase.RegionMoscow),
		tr.TariffWithDays(uint32(1), uint32(2)),
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

	currentDaysOff := &daysoff.ServiceDaysOff{
		Services: map[int64]daysoff.DisabledDatesMap{
			1004: daysoff.NewHolidayDaysOff([]string{"2021-03-24", "2021-03-25", "2021-03-26"}),
		},
	}
	generation.AddDaysOff(currentDaysOff)
	history := daysoff.NewServicesHashed()
	archive := daysoff.RecreateArchiveDaysOffQueue(nil, history)
	generation.SetDisabledDatesArchive(archive)

	return generation
}

func TestArchiveDaysOff(t *testing.T) {
	ctx := context.Background()
	settings, _ := its.NewStringSettingsHolder(`{"use_days_off_archive_pickup": true}`)
	its.SetSettingsHolder(settings)
	env, cancel := NewEnv(t, prepareEasyPath(), settings)

	defer func() {
		cancel()
	}()

	req := MakeRequest(
		RequestWithStartTime(time.Date(2021, 3, 25, 12, 0, 0, 0, MskTZ)),
		RequestWithPartner(archivePartner),
		RequestWithRegion(geobase.RegionMoscow),
		RequestWithUserInfo(true),
		RequestWithDeliveryType(common1.DeliveryType_PICKUP),
		RequestWithDeliveryOption(
			&pb.Date{Day: 27, Month: 3, Year: 2021},
			&pb.Date{Day: 27, Month: 3, Year: 2021},
		),
	)
	resp, err := env.Client.GetDeliveryRoute(ctx, req)
	require.NoError(t, err)
	require.NotNil(t, resp)

	req = MakeRequest(
		RequestWithStartTime(time.Date(2021, 3, 25, 12, 0, 0, 0, MskTZ)),
		RequestWithPartner(archivePartner),
		RequestWithRegion(geobase.RegionMoscow),
		RequestWithUserInfo(true),
		RequestWithDeliveryType(common1.DeliveryType_COURIER),
		RequestWithDeliveryOption(
			&pb.Date{Day: 27, Month: 3, Year: 2021},
			&pb.Date{Day: 27, Month: 3, Year: 2021},
		),
		RequestWithInterval(&pb.DeliveryInterval{
			From: &pb.Time{Hour: 0},
			To:   &pb.Time{Hour: 24},
		}),
		RequestWithRearrFactors("use_days_off_archive_courier=1"),
	)
	resp, err = env.Client.GetDeliveryRoute(ctx, req)
	require.NoError(t, err)
	require.NotNil(t, resp)

	req = MakeRequest(
		RequestWithStartTime(time.Date(2021, 3, 25, 12, 0, 0, 0, MskTZ)),
		RequestWithPartner(archivePartner),
		RequestWithRegion(geobase.RegionMoscow),
		RequestWithUserInfo(true),
		RequestWithDeliveryType(common1.DeliveryType_PICKUP),
		RequestWithDeliveryOption(
			&pb.Date{Day: 26, Month: 3, Year: 2021},
			&pb.Date{Day: 26, Month: 3, Year: 2021},
		),
		RequestWithRearrFactors("use_days_off_archive_pickup=0"),
	)
	resp, err = env.Client.GetDeliveryRoute(ctx, req)
	require.Error(t, err)
	require.Nil(t, resp)

	req = MakeRequest(
		RequestWithStartTime(time.Date(2021, 3, 25, 12, 0, 0, 0, MskTZ)),
		RequestWithPartner(archivePartner),
		RequestWithRegion(geobase.RegionMoscow),
		RequestWithUserInfo(true),
		RequestWithDeliveryType(common1.DeliveryType_COURIER),
		RequestWithDeliveryOption(
			&pb.Date{Day: 26, Month: 3, Year: 2021},
			&pb.Date{Day: 26, Month: 3, Year: 2021},
		),
		RequestWithInterval(&pb.DeliveryInterval{
			From: &pb.Time{Hour: 0},
			To:   &pb.Time{Hour: 24},
		}),
		RequestWithRearrFactors("use_days_off_archive_courier=0"),
	)
	resp, err = env.Client.GetDeliveryRoute(ctx, req)
	require.Error(t, err)
	require.Nil(t, resp)
}
