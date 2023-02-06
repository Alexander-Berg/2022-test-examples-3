package graph

import (
	"context"
	"fmt"
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/tarifficator"
)

func TestEvolutionEmpty(t *testing.T) {
	spo := &ShrinkPathOptions{
		TimeDelta:     12 * time.Hour,
		MaxIterations: 10,
	}
	var paths SPaths
	pe := PathEvolving{}
	pe.EvolveMany(context.Background(), paths, spo)
	require.Len(t, paths, 0)
}

type evolutionPreparePathsOption struct {
	numPointsFast int
	numPointsSlow int
}

func evolutionPreparePaths(options []evolutionPreparePathsOption) SPaths {
	startTime := time.Date(2022, 1, 1, 12, 0, 0, 0, time.Local)
	endTime := startTime.Add(time.Hour * 24 * 7)
	tariff := &tarifficator.OptionResult{}
	makePath := func(nodes ...*Node) *SortablePath {
		return &SortablePath{
			Path: &Path{
				Nodes: nodes,
				ServiceTimeList: ServiceTimeList{
					NewServiceTime(0, startTime),
				},
				EndTime: endTime,
			},
			ShopTariff: tariff,
		}
	}
	var paths SPaths
	gb := NewGraphBuilder()
	warehouse1 := gb.MakeWarehouse()
	movement1 := gb.MakeMovement(MovementWithShipment())
	for _, option := range options {
		linehaul := gb.MakeLinehaul(LinehaulWithPartner(gb.graph.GetNodeByID(movement1).PartnerLmsID))
		for k := 0; k < option.numPointsFast; k++ {
			pickup := gb.MakePickup(PickupWithPartner(gb.graph.GetNodeByID(movement1).PartnerLmsID))
			paths = append(paths, makePath(gb.graph.GetNodeByID(warehouse1), gb.graph.GetNodeByID(movement1), gb.graph.GetNodeByID(linehaul), gb.graph.GetNodeByID(pickup)))
		}
		for k := 0; k < option.numPointsSlow; k++ {
			pickup := gb.MakePickup(PickupWithPartner(gb.graph.GetNodeByID(movement1).PartnerLmsID), PickupWithHolidays(true))
			paths = append(paths, makePath(gb.graph.GetNodeByID(warehouse1), gb.graph.GetNodeByID(movement1), gb.graph.GetNodeByID(linehaul), gb.graph.GetNodeByID(pickup)))
		}
	}
	return paths
}

func TestEvolutionEvolveMany(t *testing.T) {
	paths := evolutionPreparePaths([]evolutionPreparePathsOption{
		{2, 0},
		{0, 1},
	})
	spo := &ShrinkPathOptions{
		TimeDelta:     12 * time.Hour,
		MaxIterations: 2 * 7,
	}
	pe := PathEvolving{}
	pe.EvolveMany(context.Background(), paths, spo)

	wantEndTimes := []time.Time{
		time.Date(2022, 1, 2, 10, 0, 0, 0, time.Local),
		time.Date(2022, 1, 2, 10, 0, 0, 0, time.Local),
		time.Date(2022, 1, 3, 10, 0, 0, 0, time.Local), // точка не работает в выходной
	}
	require.Equal(t, len(wantEndTimes), len(paths))
	for i := 0; i < len(wantEndTimes); i++ {
		t1, t2 := wantEndTimes[i], paths[i].EndTime
		require.True(t, t1.Equal(t2), fmt.Sprintf("i: %d, %s != %s", i, t1, t2))
	}
}

func TestEvolutionCheckAndEvolveMany(t *testing.T) {
	paths := evolutionPreparePaths([]evolutionPreparePathsOption{
		{2, 0},
		{0, 1},
	})
	perfectID := paths[0].Nodes[3].ID
	spo := &ShrinkPathOptions{
		TimeDelta:     12 * time.Hour,
		MaxIterations: 2 * 7,
		IsPerfectPath: func(p *SortablePath) bool {
			return perfectID == p.Nodes[3].ID
		},
	}
	wantEndTimes := []time.Time{
		time.Date(2022, 1, 8, 12, 0, 0, 0, time.Local), // IsPerfectPath => не эволюционируем этот путь
		time.Date(2022, 1, 2, 10, 0, 0, 0, time.Local),
		time.Date(2022, 1, 3, 10, 0, 0, 0, time.Local), // точка не работает в выходной
	}
	pe := PathEvolving{}
	pe.CheckAndEvolveMany(context.Background(), paths, spo)
	require.Equal(t, len(wantEndTimes), len(paths))
	for i := 0; i < len(wantEndTimes); i++ {
		t1, t2 := wantEndTimes[i], paths[i].EndTime
		require.True(t, t1.Equal(t2), fmt.Sprintf("i: %d, %s != %s", i, t1, t2))
	}
}

func TestEvolutionCmpSimple(t *testing.T) {
	paths := evolutionPreparePaths([]evolutionPreparePathsOption{
		{2, 0},
		{0, 1},
	})
	spo := &ShrinkPathOptions{
		TimeDelta:     12 * time.Hour,
		MaxIterations: 2 * 7,
	}

	var paths2 SPaths
	gx := Graph{}
	for _, path := range paths {
		paths2 = append(paths2, gx.EvolvePath(context.Background(), path, spo)[0])
	}

	pe := PathEvolving{}
	pe.EvolveMany(context.Background(), paths, spo)

	for i, path := range paths {
		t1, t2 := path.EndTime, paths2[i].EndTime
		require.True(t, t1.Equal(t2), fmt.Sprintf("i: %d, %s != %s", i, t1, t2))
	}
}

func TestEvolutionEvolveManyStress(t *testing.T) {
	count := 10
	paths := evolutionPreparePaths([]evolutionPreparePathsOption{
		{count, count},
		{count, count},
	})
	spo := &ShrinkPathOptions{
		TimeDelta:     12 * time.Hour,
		MaxIterations: 2 * 7,
	}
	pe := PathEvolving{}
	pe.EvolveMany(context.Background(), paths, spo)

	wantEndTimes := []time.Time{
		time.Date(2022, 1, 2, 10, 0, 0, 0, time.Local),
		time.Date(2022, 1, 3, 10, 0, 0, 0, time.Local), // точка не работает в выходной
	}
	for i := 0; i < 4*count; i++ {
		iwant := (i / count) % 2
		t1, t2 := wantEndTimes[iwant], paths[i].EndTime
		require.True(t, t1.Equal(t2), fmt.Sprintf("i: %d, %s != %s", i, t1, t2))
	}
}

// COMBINATOR-3595
// Точки из одной группы,
// в начале идет точка с более длинным путем после эволюции.
func TestEvolutionBug(t *testing.T) {
	preparePaths := func() SPaths {
		startTime := time.Date(2022, 1, 1, 12, 0, 0, 0, time.Local)
		endTime := startTime.Add(time.Hour * 24 * 7)
		tariff := &tarifficator.OptionResult{}
		makePath := func(nodes ...*Node) *SortablePath {
			return &SortablePath{
				Path: &Path{
					Nodes: nodes,
					ServiceTimeList: ServiceTimeList{
						NewServiceTime(0, startTime),
					},
					EndTime: endTime,
				},
				ShopTariff: tariff,
			}
		}
		gb := NewGraphBuilder()
		warehouse := gb.MakeWarehouse()
		movement := gb.MakeMovement(MovementWithShipment())
		linehaul := gb.MakeLinehaul(LinehaulWithPartner(gb.graph.GetNodeByID(movement).PartnerLmsID))
		pickup1 := gb.MakePickup(PickupWithPartner(gb.graph.GetNodeByID(movement).PartnerLmsID), PickupWithHolidays(true))
		pickup2 := gb.MakePickup(PickupWithPartner(gb.graph.GetNodeByID(movement).PartnerLmsID))
		return SPaths{
			makePath(gb.graph.GetNodeByID(warehouse), gb.graph.GetNodeByID(movement), gb.graph.GetNodeByID(linehaul), gb.graph.GetNodeByID(pickup1)),
			makePath(gb.graph.GetNodeByID(warehouse), gb.graph.GetNodeByID(movement), gb.graph.GetNodeByID(linehaul), gb.graph.GetNodeByID(pickup2)),
		}
	}
	paths := preparePaths()
	spo := &ShrinkPathOptions{
		TimeDelta:     12 * time.Hour,
		MaxIterations: 2 * 7,
	}
	pe := PathEvolving{}
	pe.EvolveMany(context.Background(), paths, spo)

	wantEndTimes := []time.Time{
		time.Date(2022, 1, 3, 10, 0, 0, 0, time.Local), // точка не работает в выходной
		time.Date(2022, 1, 2, 10, 0, 0, 0, time.Local),
	}
	require.Equal(t, len(wantEndTimes), len(paths))
	for i := 0; i < len(wantEndTimes); i++ {
		t1, t2 := wantEndTimes[i], paths[i].EndTime
		require.True(t, t1.Equal(t2), fmt.Sprintf("i: %d, %s != %s", i, t1, t2))
	}
}
