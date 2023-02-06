package lite

import (
	"testing"

	"github.com/stretchr/testify/require"

	bg "a.yandex-team.ru/market/combinator/pkg/biggeneration"
	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/market/combinator/pkg/graph"
	"a.yandex-team.ru/market/combinator/pkg/outlets"
	tr "a.yandex-team.ru/market/combinator/pkg/tarifficator"
)

func TestStalker(t *testing.T) {
	gb := graph.NewGraphBuilder()
	graphEx := graph.BuildExample1(gb)
	tariffsBuilder := tr.NewTariffsBuilder()
	tariff := tariffsBuilder.MakeTariff(
		tr.TariffWithPartner(uint64(graphEx.Linehaul.PartnerLmsID)),
		tr.TariffWithRegion(1, 213),
	)
	tariffDS := tariffsBuilder.MakeTariff(
		tr.TariffWithPartner(uint64(graphEx.LinehaulDS.PartnerLmsID)),
		tr.TariffWithRegion(1, 213),
	)
	tariffP := tariffsBuilder.MakeTariff(
		tr.TariffWithPartner(uint64(graphEx.Linehaul.PartnerLmsID)),
		tr.TariffWithRegion(1, 213),
		tr.TariffWithPickup(gb.PickupLmsID),
	)
	regionMap := geobase.NewExample()
	genData := bg.GenerationData{
		RegionMap:     regionMap,
		TariffsFinder: tariffsBuilder.TariffsFinder,
		Graph:         graphEx.Graph,
		Outlets: outlets.Make([]outlets.Outlet{
			outlets.Outlet{
				ID:       gb.PickupLmsID,
				RegionID: geobase.RegionID(213),
				Type:     outlets.PostTerm,
			},
		}, &regionMap, nil),
	}
	env, cancel := NewEnv(t, &genData, nil)
	defer cancel()
	{
		// FF
		req := MakeRequest(
			RequestWithPartner(
				graphEx.Warehouse172.PartnerLmsID,
			),
			RequestWithRegion(213),
		)
		resp, err := env.Client.GetCourierOptions(env.Ctx, req)
		require.NoError(t, err)
		require.Len(t, resp.Options, 10)
		require.Equal(t, int(tariff.DeliveryServiceID), int(resp.Options[0].DeliveryServiceId))
	}
	{
		// DS: другой тариф.
		req := MakeRequest(
			RequestWithPartner(graphEx.WarehouseDS.PartnerLmsID),
			RequestWithRegion(213),
		)
		resp, err := env.Client.GetCourierOptions(env.Ctx, req)
		require.NoError(t, err)
		require.Len(t, resp.Options, 10)
		require.Equal(t, int(tariffDS.DeliveryServiceID), int(resp.Options[0].DeliveryServiceId))
	}
	{
		// FF (pickup)
		req := MakePickupRequest(
			RequestWithPartner(
				graphEx.Warehouse172.PartnerLmsID,
			),
			RequestWithRegion(213),
		)
		resp, err := env.Client.GetPickupPointsGrouped(env.Ctx, req)
		require.NoError(t, err)
		require.Len(t, resp.Groups, 1)
		require.Equal(t, int(tariffP.DeliveryServiceID), int(resp.Groups[0].ServiceId))
	}
}
