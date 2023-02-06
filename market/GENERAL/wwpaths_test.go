package graph

import (
	"context"
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/enums"
	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/market/combinator/pkg/hardconfig"
	"a.yandex-team.ru/market/combinator/pkg/its"
)

func TestCycleWarehouseMovement(t *testing.T) {
	gb := NewGraphBuilder()
	warehouse := gb.MakeWarehouse()
	movement := gb.MakeMovement()
	gb.AddEdge(warehouse, movement)
	gb.AddEdge(movement, warehouse)
	gb.graph.Finish(context.Background())
	require.Len(t, gb.graph.PathsToLastWarehouse, 0)

	wwPaths, cyclicPaths := calcSimplePathsEndedOnWarehouse(gb.graph, func(node *Node) bool {
		return node.IsFirstWarehouse()
	})
	require.Len(t, wwPaths, 0)
	require.Len(t, cyclicPaths, 1)
	cpath := cyclicPaths[0]
	require.Len(t, cpath, 3)
	require.Equal(t, warehouse, cpath[0].ID)
	require.Equal(t, movement, cpath[1].ID)
	require.Equal(t, warehouse, cpath[2].ID)
}

func TestCycleWarehouseMovementWarehouse(t *testing.T) {
	gb := NewGraphBuilder()
	warehouse1 := gb.MakeWarehouse()
	movement := gb.MakeMovement()
	warehouse2 := gb.MakeWarehouse()
	gb.graph.GetNodeByID(warehouse2).PartnerType = enums.PartnerTypeSortingCenter // Не хотим чтобы с этого склада искались пути.
	gb.AddEdge(warehouse1, movement)
	gb.AddEdge(movement, warehouse2)
	gb.AddEdge(warehouse2, movement)
	gb.graph.Finish(context.Background())
	require.Len(t, gb.graph.PathsToLastWarehouse, 0)
	wwPaths, cyclicPaths := calcSimplePathsEndedOnWarehouse(gb.graph, func(node *Node) bool {
		return node.IsFirstWarehouse()
	})
	require.Len(t, wwPaths, 0)
	require.Len(t, cyclicPaths, 1)
	cpath := cyclicPaths[0]
	require.Len(t, cpath, 4)
	require.Equal(t, warehouse1, cpath[0].ID)
	require.Equal(t, movement, cpath[1].ID)
	require.Equal(t, warehouse2, cpath[2].ID)
	require.Equal(t, movement, cpath[3].ID)
}

func TestIsGoodWWPath(t *testing.T) {
	makeWarehouse := func(id, partnerID int64) *Node {
		return &Node{
			LogisticSegment: LogisticSegment{
				ID:           id,
				PartnerLmsID: partnerID,
				Type:         SegmentTypeWarehouse,
			},
		}
	}
	makePath := func(ids ...int64) Nodes {
		movement := &Node{
			LogisticSegment: LogisticSegment{
				Type: SegmentTypeMovement,
			},
		}
		nodes := Nodes{
			makeWarehouse(0, ids[0]), // FF
		}
		for i, id := range ids[1:] {
			nodes = append(nodes, movement, makeWarehouse(int64(i+1), id))
		}
		return nodes
	}

	require.True(t, isGoodWWPath(makePath(11)))
	require.True(t, isGoodWWPath(makePath(11, 22)))
	require.True(t, isGoodWWPath(makePath(11, 22, 33)))
	require.True(t, isGoodWWPath(makePath(11, 22, 33, 44)))
	require.True(t, isGoodWWPath(makePath(11, 22, 33, 44, 55)))

	require.False(t, isGoodWWPath(makePath(11, 22, 11)))
	require.False(t, isGoodWWPath(makePath(11, 22, 22)))
	require.False(t, isGoodWWPath(makePath(11, 11, 33)))
	require.False(t, isGoodWWPath(makePath(11, 22, 33, 11)))
}

func TestCycleWarehouseRegionsWithFlag(t *testing.T) {
	gb := NewGraphBuilder()
	regionMoscow := geobase.RegionChain{
		{
			ID:   geobase.RegionMoscowAndObl,
			Type: geobase.RegionTypeRegion,
		},
	}
	regionSpb := geobase.RegionChain{
		{
			ID:   geobase.RegionSaintPetersburgAndObl,
			Type: geobase.RegionTypeRegion,
		},
	}
	warehouse1 := gb.MakeWarehouse(
		WarehouseWithRegionPath(regionMoscow),
	)
	movement := gb.MakeMovement()
	warehouse2 := gb.MakeWarehouse(
		WarehouseWithRegionPath(regionSpb),
	)
	movement2 := gb.MakeMovement()
	warehouse3 := gb.MakeWarehouse(
		WarehouseWithRegionPath(regionMoscow),
	)
	movement3 := gb.MakeMovement()
	linehaul := gb.MakeLinehaul()
	gb.AddEdge(warehouse1, movement)
	gb.AddEdge(movement, warehouse2)
	gb.AddEdge(warehouse2, movement2)
	gb.AddEdge(movement2, warehouse3)
	gb.AddEdge(warehouse3, movement3)
	gb.AddEdge(movement3, linehaul)
	gb.AddEdge(movement2, linehaul)
	gb.graph.Finish(context.Background())
	require.Len(t, gb.graph.PathsToLastWarehouse, 3)
	require.Len(t, gb.graph.PathsToLastWarehouse[warehouse1], 1)
	require.Len(t, gb.graph.PathsToLastWarehouse[warehouse2], 2)
	require.Len(t, gb.graph.PathsToLastWarehouse[warehouse3], 1)
}

func TestCycleWarehouseRegionsWithoutFlag(t *testing.T) {
	its.GetSettingsHolder()
	settings, _ := its.NewStringSettingsHolder(`{"check_region_cycles": false}`)
	its.SetSettingsHolder(settings)
	gb := NewGraphBuilder()
	regionMoscow := geobase.RegionChain{
		{
			ID:   geobase.RegionMoscowAndObl,
			Type: geobase.RegionTypeRegion,
		},
	}
	regionSpb := geobase.RegionChain{
		{
			ID:   geobase.RegionSaintPetersburgAndObl,
			Type: geobase.RegionTypeRegion,
		},
	}
	warehouse1 := gb.MakeWarehouse(
		WarehouseWithRegionPath(regionMoscow),
	)
	movement := gb.MakeMovement()
	warehouse2 := gb.MakeWarehouse(
		WarehouseWithRegionPath(regionSpb),
	)
	movement2 := gb.MakeMovement()
	warehouse3 := gb.MakeWarehouse(
		WarehouseWithRegionPath(regionMoscow),
	)
	movement3 := gb.MakeMovement()
	linehaul := gb.MakeLinehaul()
	gb.AddEdge(warehouse1, movement)
	gb.AddEdge(movement, warehouse2)
	gb.AddEdge(warehouse2, movement2)
	gb.AddEdge(movement2, warehouse3)
	gb.AddEdge(warehouse3, movement3)
	gb.AddEdge(movement3, linehaul)
	gb.AddEdge(movement2, linehaul)
	gb.graph.Finish(context.Background())
	require.Len(t, gb.graph.PathsToLastWarehouse, 3)
	require.Len(t, gb.graph.PathsToLastWarehouse[warehouse1], 2)
	require.Len(t, gb.graph.PathsToLastWarehouse[warehouse2], 2)
	require.Len(t, gb.graph.PathsToLastWarehouse[warehouse3], 1)
}

func TestCycleWarehouseRegionsWithTree(t *testing.T) {
	gb := NewGraphBuilder()
	regionMoscow := geobase.RegionChain{
		{
			ID:   geobase.RegionMoscowAndObl,
			Type: geobase.RegionTypeRegion,
		},
	}
	regionSpb := geobase.RegionChain{
		{
			ID:   geobase.RegionSaintPetersburgAndObl,
			Type: geobase.RegionTypeRegion,
		},
	}
	regionNovosib := geobase.RegionChain{
		{
			ID:   geobase.RegionNovosibirsk,
			Type: geobase.RegionTypeRegion,
		},
	}
	warehouse1 := gb.MakeWarehouse(
		WarehouseWithRegionPath(regionMoscow),
	)
	movement := gb.MakeMovement()
	warehouse2 := gb.MakeWarehouse(
		WarehouseWithRegionPath(regionSpb),
	)
	movement2 := gb.MakeMovement()
	warehouse3 := gb.MakeWarehouse(
		WarehouseWithRegionPath(regionNovosib),
	)
	movement3 := gb.MakeMovement()
	warehouse4 := gb.MakeWarehouse(
		WarehouseWithRegionPath(regionNovosib),
	)
	movement4 := gb.MakeMovement()
	movement5 := gb.MakeMovement()
	linehaul := gb.MakeLinehaul()
	linehaul2 := gb.MakeLinehaul()
	gb.AddEdge(warehouse1, movement)
	gb.AddEdge(movement, warehouse2)
	gb.AddEdge(warehouse2, movement2)
	gb.AddEdge(movement2, warehouse3)
	gb.AddEdge(warehouse3, movement3)
	gb.AddEdge(movement3, linehaul)
	gb.AddEdge(warehouse2, movement4)
	gb.AddEdge(movement4, warehouse4)
	gb.AddEdge(warehouse4, movement5)
	gb.AddEdge(movement5, linehaul2)
	gb.graph.Finish(context.Background())
	require.Len(t, gb.graph.PathsToLastWarehouse, 4)
	require.Len(t, gb.graph.PathsToLastWarehouse[warehouse1], 2)
	require.Len(t, gb.graph.PathsToLastWarehouse[warehouse2], 2)
	require.Len(t, gb.graph.PathsToLastWarehouse[warehouse3], 1)
	require.Len(t, gb.graph.PathsToLastWarehouse[warehouse4], 1)
}

func TestAllowedCyclicRegions(t *testing.T) {
	pb := NewPathBuilder()
	regionMoscow := geobase.RegionChain{
		{
			ID:   geobase.RegionMoscowAndObl,
			Type: geobase.RegionTypeRegion,
		},
	}
	regionSpb := geobase.RegionChain{
		{
			ID:   geobase.RegionSaintPetersburgAndObl,
			Type: geobase.RegionTypeRegion,
		},
	}

	pb.AddWarehouse(
		pb.WithRegionPath(regionSpb),
	)
	pb.AddMovement()
	pb.AddWarehouse(
		pb.WithRegionPath(regionMoscow),
	)
	pb.AddMovement()
	pb.AddWarehouse(
		pb.WithRegionPath(regionSpb),
	)

	path := pb.GetSortablePath()

	isCycle := IsCycleRegionsPath(path.Path)
	require.True(t, isCycle)

	hardconfig.GetHardConfig().AllowedCyclicRegionsMap = hardconfig.AllowedCyclicRegionsMap{
		hardconfig.AllowedCyclicRegion{
			MiddleRegion: geobase.RegionMoscowAndObl,
			CyclicRegion: geobase.RegionSaintPetersburgAndObl,
		}: struct{}{},
	}

	isCycle = IsCycleRegionsPath(path.Path)
	require.False(t, isCycle)
}
