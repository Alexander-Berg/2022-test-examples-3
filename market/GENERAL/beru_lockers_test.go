package lite

import (
	"testing"
)

func TestBeruLockers(t *testing.T) {
	/*graphEx := graph.NewExample()
	tariffsFinder := tr.NewTariffsFinder()
	tariffsFinder.Add(makeGlobalTariff(getMskFirst()))
	tariffsFinder.Add(getMskFirst())
	tariffsFinder.Add(getMskSecond())
	tariffsFinder.Finish()
	regionMap := geobase.NewExample()
	genData := bg.GenerationData{
		RegionMap:     regionMap,
		TariffsFinder: NewFinderSet(tariffsFinder),
		Outlets: outlets.Make([]*outlets.Outlet{
			&outlets.Outlet{
				ID:       10000971018,
				Type:     outlets.PostTerm,
				IsActive: true,
				IsBeru:   true,
			},
			&outlets.Outlet{
				ID:       10000971019,
				Type:     outlets.PostTerm,
				IsActive: true,
				IsBeru:   true,
			},
		}, &regionMap),
		Graph: graphEx.G,
	}

	env, cancel := NewEnv(t, &genData, nil)
	defer cancel()

	warehouseID := graphEx.Warehouse.PartnerLmsID
	handler := routes.BeruLockersRequestHandler(env.httpServer)
	for _, useAllPointsSearch := range []bool{false, true} {
		handler.Settings.SetUseAllPointsSearch(useAllPointsSearch)

		beruLockers := routes.BeruLockers{RequestHandler: handler}
		options := routes.FindAvailableOptions{Warehouses: []int64{warehouseID}}
		resp, err := beruLockers.FindAvailable(env.Ctx, options)
		require.NoError(t, err)
		require.Equal(t, 2, len(resp))
		for _, point := range []int64{10000971018, 10000971019} {
			res, ok := resp[point]
			require.True(t, ok)
			require.Equal(t, point, res.Outlet.ID)
			require.Equal(t, map[int64]bool{warehouseID: true}, res.Warehouses)
		}
	}*/
}
