package graph

import (
	"context"
	"sort"
	"strconv"
	"strings"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/daysoff"
	"a.yandex-team.ru/market/combinator/pkg/enums"
	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/market/combinator/pkg/hardconfig"
	"a.yandex-team.ru/market/combinator/pkg/lms"
	"a.yandex-team.ru/market/combinator/pkg/logging"
	"a.yandex-team.ru/market/combinator/pkg/settings"
	tr "a.yandex-team.ru/market/combinator/pkg/tarifficator"
	"a.yandex-team.ru/market/combinator/pkg/timex"
	"a.yandex-team.ru/market/combinator/pkg/util"
)

type CheckSearch func(found *PathsFound, err error)

func RunAndCheckSearch(g *Graph, startID int64, options *Options, check CheckSearch) {
	check(g.FindPaths(context.Background(), startID, options))

	if options.GetBuildOptions().GetBuildVersion() == BuildVersionByTree {
		if options == nil {
			options = &Options{}
		}
		if options.BuildOptions == nil {
			options.BuildOptions = &BuildOptions{}
		}
		options.BuildOptions.buildVersion = BuildVersionByList
		check(g.FindPaths(context.Background(), startID, options))
	}
}

func RunAndCheckDFS(g *Graph, startID int64, options *Options, check CheckSearch) {
	check(g.FindPaths(context.Background(), startID, options))
}

func NewNode(ID int64, price int32, duration int32, regionID geobase.RegionID) *Node {
	schedule, err := newSimpleSchedule(CreateSimpleSchedule())
	if err != nil {
		panic(err)
	}
	services := []*LogisticService{
		{
			ID:       ID * 111,
			IsActive: true,
			Price:    price,
			Duration: duration,
			Schedule: schedule,
		},
	}
	return &Node{
		LogisticSegment: LogisticSegment{
			ID:         ID,
			PointLmsID: int64(regionID),
			LocationID: regionID,
		},
		CourierServices: services,
		PickupServices:  services,
	}
}

func sortPaths(paths Paths) Paths {
	sort.Slice(
		paths,
		func(i, j int) bool {
			if len(paths[i].Nodes) == len(paths[j].Nodes) {
				for k := range paths[i].Nodes {
					if paths[i].Nodes[k].ID == paths[j].Nodes[k].ID {
						continue
					}
					return paths[i].Nodes[k].ID < paths[j].Nodes[k].ID
				}
			}
			return len(paths[i].Nodes) < len(paths[j].Nodes)
		},
	)
	return paths
}

func TestNewAndFindPaths(t *testing.T) {
	// 11 -> 22
	//    -> 33
	//
	nodes := []*Node{
		NewNode(11, 0, 0, 0),
		NewNode(22, 0, 0, 0),
		NewNode(33, 0, 0, 0),
	}
	edges := []LogisticsEdges{
		{From: 11, To: 22},
		{From: 11, To: 33},
	}
	g, err := Make(nodes, edges)
	assert.NoError(t, err)

	assert.Equal(t, len(g.Neighbors[11]), 2)
	assert.Equal(t, g.Neighbors[11][0].ID, int64(22))
	assert.Equal(t, g.Neighbors[11][1].ID, int64(33))

	options := Options{
		SearchOptions: &SearchOptions{
			StoreSkipped: true,
		},
	}
	RunAndCheckSearch(g, 11, &options, func(found *PathsFound, err error) {
		require.NoError(t, err)
		paths := sortPaths(found.Paths)
		require.Equal(t, 2, len(paths))
		require.Equal(t, int64(11), paths[0].Nodes[0].ID)
		require.Equal(t, int64(22), paths[0].Nodes[1].ID)
		require.Equal(t, int64(11), paths[1].Nodes[0].ID)
		require.Equal(t, int64(33), paths[1].Nodes[1].ID)
	})
}

func TestMakeAndFindPaths(t *testing.T) {
	regionIDFirst := geobase.RegionID(225)
	regionIDLast := geobase.RegionID(322)
	nodes := []*Node{
		NewNode(11, 10, 100, regionIDFirst),
		NewNode(22, 20, 200, regionIDLast),
		NewNode(33, 30, 300, 0),
	}
	edges := []LogisticsEdges{
		{From: 11, To: 22},
		{From: 11, To: 33},
	}
	g, err := Make(nodes, edges)
	assert.NoError(t, err)

	// Construct paths in reverse order and
	// check that regionID has the right value
	loc := timex.FixedZone("UTC+3", util.MskTZOffset)
	startTime := time.Now().Round(time.Second).In(loc)
	options := Options{
		SearchOptions: &SearchOptions{
			StoreSkipped: true,
		},
		BuildOptions: &BuildOptions{
			StartTime: startTime,
		},
	}
	RunAndCheckSearch(g, 11, &options, func(found *PathsFound, err error) {
		require.NoError(t, err)
		paths := sortPaths(found.Paths)
		require.Equal(t, 2, len(paths))

		p := paths[0]
		curTime := startTime
		require.Equal(t, uint32(30), p.Price)
		for _, n := range p.Nodes {
			for _, s := range n.GetServices(DeliveryTypeCourier) {
				startTime, ok := p.FindStartTime(s.ID)
				require.True(t, ok)
				require.Equal(t, curTime, startTime)
				curTime = curTime.Add(time.Minute * time.Duration(s.Duration))
			}
		}
		require.Equal(t, curTime, p.EndTime)
		require.Equal(t, regionIDLast, p.RegionID)

		curTime = startTime
		p = paths[1]
		require.Equal(t, uint32(40), p.Price)
		for _, n := range p.Nodes {
			for _, s := range n.GetServices(DeliveryTypeCourier) {
				startTime, ok := p.FindStartTime(s.ID)
				assert.True(t, ok)
				require.Equal(t, curTime, startTime)
				curTime = curTime.Add(time.Minute * time.Duration(s.Duration))
			}
		}
		require.Equal(t, curTime, p.EndTime)
		require.Equal(t, regionIDFirst, p.RegionID)
	})
}

func TestPaths(t *testing.T) {
	nodes := []*Node{
		NewNode(11, 10, 10, 0),
		NewNode(22, 20, 20, 0),
		NewNode(33, 30, 30, 0),
		NewNode(44, 40, 40, 0),
		NewNode(55, 50, 50, 0),
		NewNode(66, 60, 60, 0),
		NewNode(77, 70, 70, 0),
		NewNode(88, 80, 80, 0),
		NewNode(99, 90, 90, 0),
	}
	edges := []LogisticsEdges{
		{From: 11, To: 22},
		{From: 11, To: 33},
		{From: 11, To: 44},
		{From: 11, To: 55},
		{From: 11, To: 66},

		{From: 22, To: 77},

		{From: 33, To: 77},

		{From: 44, To: 77},

		{From: 55, To: 99},

		{From: 66, To: 99},

		{From: 77, To: 88},
		{From: 77, To: 99},

		{From: 88, To: 99},
	}
	g, err := Make(nodes, edges)
	assert.NoError(t, err)

	options := Options{
		SearchOptions: &SearchOptions{
			StoreSkipped: true,
		},
	}
	RunAndCheckSearch(g, 11, &options, func(found *PathsFound, err error) {
		require.NoError(t, err)
		assert.Equal(t, 8, len(found.Paths))
	})
}

func TestCycle1(t *testing.T) {
	g := NewGraphWithHintsV3(MakeHintMap(TestHints()))
	g.AddNode(*NewNode(11, 0, 0, 0))
	g.AddNode(*NewNode(22, 0, 0, 0))
	g.AddNode(*NewNode(33, 0, 0, 0))
	assert.NoError(t, g.AddEdge(11, 22))
	assert.NoError(t, g.AddEdge(22, 33))
	assert.NoError(t, g.AddEdge(33, 11))

	check := func(found *PathsFound, err error) {
		require.NoError(t, err)
		assert.Equal(t, 1, len(found.Paths)) // 11 -> 22 -> 33
	}
	options := Options{
		SearchOptions: &SearchOptions{
			StoreSkipped: true,
		},
	}
	check(g.FindPaths(context.Background(), 11, &options))
}

func TestCycle2(t *testing.T) {
	nodes := []*Node{
		NewNode(11, 0, 0, 0),
		NewNode(22, 0, 0, 0),
		NewNode(33, 0, 0, 0),
	}
	edges := []LogisticsEdges{
		{From: 11, To: 22},
		{From: 11, To: 33},
		{From: 22, To: 33},
		{From: 33, To: 22},
	}
	g, err := Make(nodes, edges)
	assert.NoError(t, err)

	check := func(found *PathsFound, err error) {
		require.NoError(t, err)
		assert.Equal(t, 2, len(found.Paths)) // 11 -> 22 -> 33 & 11 -> 33-> 22
	}
	options := Options{
		SearchOptions: &SearchOptions{
			StoreSkipped: true,
		},
	}
	check(g.FindPaths(context.Background(), 11, &options))
}

func TestDSBSCourierPath(t *testing.T) {
	gb := NewGraphBuilder()
	warehouse := gb.MakeWarehouse(WarehouseWithPartnerType(enums.PartnerTypeDSBS))
	movement := gb.MakeMovement(MovementWithPartnerType(enums.PartnerTypeDSBS))
	linehaul := gb.MakeLinehaul(LinehaulWithPartnerType(enums.PartnerTypeDSBS))
	gb.MakeHanding(HandingWithPartnerType(enums.PartnerTypeDSBS))
	gb.AddEdge(warehouse, movement)
	gb.AddEdge(movement, linehaul)
	gb.graph.Finish(context.Background())
	require.Len(t, gb.graph.PathsToLastWarehouse, 1)

	regions := geobase.NewExample()
	searchOpt := &SearchOptions{
		RegionMap: regions,
		RegionTo:  regions[213],
	}
	res := gb.graph.findBasePathsManyRegion(context.Background(), warehouse, searchOpt)
	resOld := gb.graph.findBasePathsDFS(context.Background(), warehouse, searchOpt)
	var traverse func(node *TreeNode, depth int) int
	traverse = func(node *TreeNode, depth int) int {
		maxDepth := depth
		for _, ch := range node.children {
			curDepth := traverse(ch, depth+1)
			if curDepth > maxDepth {
				maxDepth = curDepth
			}
		}
		return maxDepth
	}
	require.NotNil(t, res)
	require.NotNil(t, resOld)
	resDepth := traverse(res.Root, 0)
	resOldDepth := traverse(resOld.Root, 0)
	// Несмотря на ребро между linehaul'ом и handing'ом
	// Глубина 3, так как это ребро мы не используем для каркаса путей
	require.Equal(t, 2, resDepth)
	require.Equal(t, 2, resOldDepth)
}

func PathToKey(path *Path) string {
	slist := make([]string, len(path.Nodes))
	for i, node := range path.Nodes {
		slist[i] = strconv.Itoa(int(node.ID))
	}
	return strings.Join(slist, ".")
}

// Граф с узлом у которого есть 2 "родителя".
func TestBugConstructPath1(t *testing.T) {
	nodes := []*Node{
		NewNode(11, 10, 100, 0),
		NewNode(22, 20, 200, 0),
		NewNode(33, 30, 300, 0),
		NewNode(44, 40, 400, 0),
	}
	edges := []LogisticsEdges{
		// 11 => 22 => 44
		{From: 11, To: 22},
		{From: 22, To: 44},
		// 11 => 33 => 44
		{From: 11, To: 33},
		{From: 33, To: 44},
	}
	g, err := Make(nodes, edges)
	require.NoError(t, err)

	options := Options{
		SearchOptions: &SearchOptions{
			StoreSkipped: true,
		},
	}
	RunAndCheckSearch(g, 11, &options, func(found *PathsFound, err error) {
		require.NoError(t, err)
		paths := sortPaths(found.Paths)
		require.Equal(t, 2, len(paths))
		{
			p := paths[0]
			require.Equal(t, "11.22.44", PathToKey(p))
		}
		{
			p := paths[1]
			require.Equal(t, "11.33.44", PathToKey(p))
		}
	})
}

func TestZeroPaths(t *testing.T) {
	gb := NewGraphBuilder()
	warehouse := gb.MakeWarehouse()
	movement := gb.MakeMovement()
	warehouse1 := gb.MakeWarehouse()
	warehouse2 := gb.MakeWarehouse()
	movement1 := gb.MakeMovement()
	movement2 := gb.MakeMovement()
	linehaul1 := gb.MakeLinehaul()
	linehaul2 := gb.MakeLinehaul(LinehaulWithRegion(geobase.RegionSaintPetersburg))
	gb.AddEdge(warehouse, movement)
	gb.AddEdge(movement, warehouse1)
	gb.AddEdge(movement, warehouse2)
	gb.AddEdge(warehouse1, movement1)
	gb.AddEdge(warehouse2, movement2)
	gb.AddEdge(movement1, linehaul1)
	gb.AddEdge(movement2, linehaul2)
	gb.graph.Finish(context.Background())
	require.Len(t, gb.graph.PathsToLastWarehouse, 3)

	regions := geobase.NewExample()
	searchOpt := &SearchOptions{
		PathType:  PathTypeFull,
		RegionMap: regions,
		RegionTo:  regions[213],
	}
	defSet := settings.New(getDefaultSettings(), "skip_zero_paths=1")
	ctx := settings.ContextWithSettings(context.Background(), defSet)
	res := gb.graph.findBasePathsManyRegion(ctx, warehouse, searchOpt)
	paths := make([][]*Node, 0)
	var traverse func(node *TreeNode, path []*Node)
	traverse = func(node *TreeNode, path []*Node) {
		currentPath := append(path, node.node)

		if len(node.children) == 0 {
			paths = append(paths, currentPath)
		} else {
			for _, n := range node.children {
				traverse(n, currentPath)
			}
		}
	}
	traverse(res.Root, make([]*Node, 0))
	for _, path := range paths {
		require.Greater(t, len(path), 2)
	}
	require.Equal(t, len(paths), 1)
}

// Граф с узлом у которого есть 2 "родителя" + нужная длина пути (реаллокация Path.Nodes) для вопроизведения бага.
func TestBugConstructPath2(t *testing.T) {
	// TODO(manushkin): Одно ребро можно добавить 2 раза, это баг.
	nodes := []*Node{
		NewNode(11, 10, 100, 0),
		NewNode(22, 20, 200, 0),
		NewNode(33, 30, 300, 0),
		NewNode(44, 40, 400, 0),
		NewNode(55, 50, 500, 0),
		NewNode(66, 60, 600, 0),
	}
	edges := []LogisticsEdges{
		// 11 => 22 => 44
		{From: 11, To: 22},
		{From: 22, To: 44},
		// 11 => 33 => 44
		{From: 11, To: 33},
		{From: 33, To: 44},
		// 44 => 55 => 66
		{From: 44, To: 55},
		{From: 55, To: 66},
	}
	g, err := Make(nodes, edges)
	require.NoError(t, err)

	options := Options{
		SearchOptions: &SearchOptions{
			StoreSkipped: true,
		},
		BuildOptions: &BuildOptions{
			StartTime: time.Now().Round(time.Second),
		},
	}
	RunAndCheckSearch(g, 11, &options, func(found *PathsFound, err error) {
		require.NoError(t, err)
		paths := sortPaths(found.Paths)
		require.Equal(t, 2, len(paths))
		{
			p := paths[0]
			require.Equal(t, "11.22.44.55.66", PathToKey(p))
		}
		{
			p := paths[1]
			require.Equal(t, "11.33.44.55.66", PathToKey(p))
		}
	})
}

func TestFindBestLinehauls(t *testing.T) {
	regions := geobase.NewExample()
	ex := NewExample()
	{
		linehaul, other := findBestLinehaul(
			regions,
			regions[213],
			ex.Movement,
			ex.G.Neighbors[ex.Movement.ID],
		)
		require.Len(t, linehaul, 1)
		require.Len(t, other, 0)
		require.Equal(t, ex.LinehaulMoscow.ID, linehaul[0].ID)
	}
	{
		linehaul, other := findBestLinehaul(
			regions,
			regions[2],
			ex.Movement,
			ex.G.Neighbors[ex.Movement.ID],
		)
		require.Len(t, linehaul, 1)
		require.Equal(t, ex.LinehaulRussia.ID, linehaul[0].ID)
		require.Equal(t, 0, len(other))
	}
}

func TestWarehouseAfterMovement(t *testing.T) {
	gb := NewGraphBuilder()
	warehouse1 := gb.MakeWarehouse()
	movement := gb.MakeMovement()
	warehouse2 := gb.MakeWarehouse()
	gb.AddEdge(warehouse1, movement)
	gb.AddEdge(movement, warehouse2)
	gb.graph.Finish(context.Background())

	regions := geobase.NewExample()
	options := Options{
		SearchOptions: &SearchOptions{
			StoreSkipped: true,
			RegionMap:    regions,
			RegionTo:     regions[213],
		},
		BuildOptions: &BuildOptions{
			StartTime: time.Now().Round(time.Second),
			DType:     DeliveryTypeCourier,
		},
	}
	RunAndCheckSearch(gb.graph, warehouse1, &options, func(found *PathsFound, err error) {
		require.NoError(t, err)
		paths := found.Paths
		require.Equal(t, 1, len(paths))
		p := paths[0]
		require.Len(t, p.Nodes, 3)
		require.Equal(t, warehouse1, p.Nodes[0].ID)
		require.Equal(t, movement, p.Nodes[1].ID)
		require.Equal(t, warehouse2, p.Nodes[2].ID)
	})
}

func TestEnums(t *testing.T) {
	require.Equal(t, ServiceTypeInternal, GetServiceType("internal"))
	require.Equal(t, 1, int(ServiceTypeInbound))
	require.Equal(t, 2, int(ServiceTypeInternal))
	require.Equal(t, 4, int(ServiceTypeOutbound))
}

// Пути должны заканчиваться на 2-м складе.
func TestPathTypeConsolidation(t *testing.T) {
	makeWarehouse := func(ID int64, price1 int32, price2 int32) *Node {
		schedule, err := newSimpleSchedule(CreateSimpleSchedule())
		if err != nil {
			panic(err)
		}
		services := []*LogisticService{
			{
				ID:       ID * 111,
				IsActive: true,
				Price:    price1,
				Type:     ServiceTypeInbound,
				Schedule: schedule,
			},
			{
				ID:       ID * 222,
				IsActive: true,
				Price:    price2,
				Type:     ServiceTypeInternal,
				Code:     enums.ServiceProcessing,
				Schedule: schedule,
			},
		}
		return &Node{
			LogisticSegment: LogisticSegment{
				ID:   ID,
				Type: SegmentTypeWarehouse,
			},
			CourierServices: services,
			PickupServices:  services,
		}
	}
	warehouse1 := makeWarehouse(11, 11, 12)

	movement1 := NewNode(22, 20, 0, 0)
	movement1.Type = SegmentTypeMovement
	movement1.CourierServices[0].Code = enums.ServiceMovement

	warehouse2 := makeWarehouse(33, 31, 32)

	movement2 := NewNode(44, 40, 0, 0)
	movement2.Type = SegmentTypeMovement
	movement2.CourierServices[0].Code = enums.ServiceMovement

	linehaul := NewNode(55, 50, 0, 0)
	linehaul.Type = SegmentTypeLinehaul

	nodes := []*Node{warehouse1, movement1, warehouse2, movement2, linehaul}
	edges := []LogisticsEdges{
		{From: 11, To: 22},
		{From: 22, To: 33},
		{From: 33, To: 44},
		{From: 44, To: 55},
	}
	g, err := Make(nodes, edges)
	require.NoError(t, err)
	g.Finish(context.Background())

	options1 := Options{
		SearchOptions: &SearchOptions{
			StoreSkipped: true,
			PathType:     PathTypeCrossdockWarehouse,
		},
	}
	options2 := Options{
		SearchOptions: &SearchOptions{
			StoreSkipped: true,
			PathType:     PathTypeConsolidationWarehouse,
		},
	}
	options3 := Options{
		SearchOptions: &SearchOptions{
			StoreSkipped: true,
		},
	}
	p1 := 12 + 20
	RunAndCheckSearch(g, 11, &options1, func(found *PathsFound, err error) {
		require.NoError(t, err)
		paths := found.Paths
		require.Equal(t, 1, len(paths))
		path := paths[0]
		require.Equal(t, "11.22.33", PathToKey(path))
		require.Equal(t, p1, int(path.Price))
		// Последний узел в пути для CD это склад FF, без примененных сервисов.
		last := path.Nodes[len(path.Nodes)-1]
		require.Equal(t, nodes[2], last)
		for _, ls := range last.GetServices(DeliveryTypeCourier) {
			_, ok := path.FindStartTime(ls.ID)
			require.False(t, ok)
		}
	})
	p2 := 31 + 32 + 40 + 50
	RunAndCheckSearch(g, 33, &options2, func(found *PathsFound, err error) {
		require.NoError(t, err)
		paths := found.Paths
		require.Equal(t, 1, len(paths))
		path := paths[0]
		require.Equal(t, "33.44.55", PathToKey(path))
		require.Equal(t, p2, int(path.Price))
	})
	RunAndCheckSearch(g, 11, &options3, func(found *PathsFound, err error) {
		require.NoError(t, err)
		paths := found.Paths
		require.Equal(t, 1, len(paths))
		path := paths[0]
		require.Equal(t, "11.22.33.44.55", PathToKey(path))
		require.Equal(t, p1+p2, int(path.Price))
	})
}

func BenchmarkCalcTimeForPickup(b *testing.B) {
	loc := timex.FixedZone("UTC+3", util.MskTZOffset)
	startTime := time.Now().In(loc)
	schedule, err := newSimpleSchedule(createPickupInterval())
	if err != nil {
		panic(err)
	}
	services := []*LogisticService{
		{
			ID:       111,
			Price:    322,
			Type:     ServiceTypeOutbound,
			Code:     enums.ServiceHanding,
			Schedule: schedule,
		},
	}
	node := Node{
		LogisticSegment: LogisticSegment{
			ID:             1,
			Type:           SegmentTypePickup,
			PaymentMethods: enums.MethodCashAllowed | enums.MethodCardAllowed,
		},
		CourierServices: services,
		PickupServices:  services,
	}
	var m CargoTypes
	b.ResetTimer()
	b.Run(
		"CalcTimeForPickup",
		func(b *testing.B) {
			for i := 0; i < b.N; i++ {
				_, _ = CalcTimeForPickup(
					context.Background(),
					&node,
					startTime,
					enums.MethodCashAllowed,
					m,
					daysoff.NewServicesHashed(), //daysOffGrouped
				)
			}
		},
	)
}

func TestFixLinehaulShipmentServices(t *testing.T) {
	pickupServices := []*LogisticService{{ID: 1, Code: enums.ServiceLastMile}, {ID: 2, Code: enums.ServiceShipment}}
	courierServices := []*LogisticService{{ID: 3, Code: enums.ServiceLastMile}, {ID: 4, Code: enums.ServiceShipment}}
	lhNode := &Node{CourierServices: courierServices, PickupServices: pickupServices, LogisticSegment: LogisticSegment{Type: SegmentTypeLinehaul}}
	{
		t.Log("new logic deferred pickup base")
		st := &settings.Settings{CourierLinehaulShipmentHack: true}
		result := FixLinehaulShipmentServices(st, true, pickupServices, lhNode)
		require.Contains(t, result, pickupServices[1])
		require.NotContains(t, result, courierServices[1])
	}
	{
		t.Log("new logic deferred courier base")
		st := &settings.Settings{CourierLinehaulShipmentHack: true}
		result := FixLinehaulShipmentServices(st, true, courierServices, lhNode)
		require.Contains(t, result, pickupServices[1])
		require.NotContains(t, result, courierServices[1])
	}
	{
		t.Log("new logic not deferred pickup base")
		st := &settings.Settings{CourierLinehaulShipmentHack: true}
		result := FixLinehaulShipmentServices(st, false, pickupServices, lhNode)
		require.Contains(t, result, pickupServices[1])
		require.NotContains(t, result, courierServices[1])
	}
	{
		t.Log("new logic not deferred courier base")
		st := &settings.Settings{CourierLinehaulShipmentHack: true}
		result := FixLinehaulShipmentServices(st, false, courierServices, lhNode)
		require.NotContains(t, result, pickupServices[1])
		require.Contains(t, result, courierServices[1])
	}
	{
		t.Log("old logic deferred pickup base")
		st := &settings.Settings{CourierLinehaulShipmentHack: false}
		result := FixLinehaulShipmentServices(st, true, pickupServices, lhNode)
		require.Contains(t, result, pickupServices[1])
		require.NotContains(t, result, courierServices[1])
	}
	{
		t.Log("old logic deferred courier base")
		st := &settings.Settings{CourierLinehaulShipmentHack: false}
		result := FixLinehaulShipmentServices(st, true, courierServices, lhNode)
		require.NotContains(t, result, pickupServices[1])
		require.Contains(t, result, courierServices[1])
	}
}

func TestLogisticDayStart(t *testing.T) {
	type GraphData struct {
		graph                         *Graph
		warehouse, movement, linehaul *Node
		serviceDaysOffGrouped         daysoff.ServicesHashed
	}
	build := func() GraphData {
		gb := NewGraphBuilder()
		warehouse := gb.MakeWarehouse(
			WarehouseWithRegion(geobase.RegionSofyno),
		)
		movement := gb.MakeMovement()
		linehaul := gb.MakeLinehaul(
			LinehaulWithPartner(gb.graph.GetNodeByID(movement).PartnerLmsID),
		)
		gb.AddEdge(warehouse, movement)
		gb.AddEdge(movement, linehaul)
		gb.graph.Finish(context.Background())
		warehouseService := gb.graph.GetNodeByID(warehouse).GetServices(DeliveryTypeCourier)[0]
		warehouseService.Duration = 180

		movementService := gb.graph.GetNodeByID(movement).GetServices(DeliveryTypeCourier)[0]
		movementService.Duration = 120

		return GraphData{
			graph:     gb.graph,
			warehouse: gb.graph.GetNodeByID(warehouse),
			movement:  gb.graph.GetNodeByID(movement),
			linehaul:  gb.graph.GetNodeByID(linehaul),
		}
	}
	buildWithDisabled := func(logisticDayStart timex.DayTime, disabledDates []string) GraphData {
		gd := build()
		gd.warehouse.logisticDayStart = logisticDayStart
		gd.warehouse.setLogisticDayStart(logisticDayStart)
		schedule, _ := CreateAroundTheClockSchedule(false)
		movementService := gd.movement.GetServices(DeliveryTypeCourier)[0]
		movementService.Schedule = schedule
		gd.serviceDaysOffGrouped = daysoff.NewServicesHashed()
		gd.serviceDaysOffGrouped.DaysOffGrouped[movementService.ID] = daysoff.NewDaysOffGroupedFromStrings(disabledDates)
		gd.movement.setLogisticDayStart(logisticDayStart)
		return gd
	}
	regions := geobase.NewExample()
	check := func(gd GraphData, startTime time.Time, wantStartTimes []time.Time) {
		options := Options{
			SearchOptions: &SearchOptions{
				RegionMap: regions,
				RegionTo:  regions[213],
			},
			BuildOptions: &BuildOptions{
				StartTime:      startTime,
				DType:          DeliveryTypeCourier,
				DaysOffGrouped: gd.serviceDaysOffGrouped,
			},
		}
		RunAndCheckDFS(gd.graph, gd.warehouse.ID, &options, func(found *PathsFound, err error) {
			require.NoError(t, err)
			require.Equal(t, 1, len(found.Paths))
			path := found.Paths[0]
			i := 0
			for _, node := range path.Nodes {
				for _, service := range node.GetServices(DeliveryTypeCourier) {
					actualTime, _ := path.FindStartTime(service.ID)
					require.Equal(
						t, wantStartTimes[i], actualTime,
						"%d, %d != %d", i, wantStartTimes[i].Unix(), actualTime.Unix(),
					)
					i += 1
				}
			}
		})
	}
	Time := func(year, month, day, hour int) time.Time {
		return time.Date(year, time.Month(month), day, hour, 0, 0, 0, time.UTC)
	}
	// Обычный вариант, без сдвига.
	check(
		build(),
		Time(2020, 6, 26, 6), // startTime
		[]time.Time{
			Time(2020, 6, 26, 6),  // warehouse
			Time(2020, 6, 26, 9),  // movement
			Time(2020, 6, 26, 11), // two linehaul service
			Time(2020, 6, 26, 11),
		},
	)
	// У склада есть лог сутки, но Movement.MOVEMENT.startTime=13:00 НЕ попадает в эти сутки.
	// Поэтому результат без сдвига.
	check(
		buildWithDisabled(
			timex.DayTime{Hour: 12}, // лог сутки
			[]string{"2020-06-25"},
		),
		Time(2020, 6, 26, 10), // startTime
		[]time.Time{
			Time(2020, 6, 26, 10), // warehouse
			Time(2020, 6, 26, 13), // movement
			Time(2020, 6, 26, 15), // two linehaul service
			Time(2020, 6, 26, 15),
		},
	)
	// У склада есть лог сутки, и Movement.MOVEMENT.startTime=9:00 попадает в эти сутки.
	// Поэтому делаем сдвиг влево для disableDats, 25 выключено, двигаем startTime на 1 день вправо.
	check(
		buildWithDisabled(
			timex.DayTime{Hour: 12}, // лог сутки
			[]string{"2020-06-25"},
		),
		Time(2020, 6, 26, 6), // startTime
		[]time.Time{
			Time(2020, 6, 26, 6),  // warehouse
			Time(2020, 6, 26, 12), // movement
			Time(2020, 6, 26, 14), // two linehaul service
			Time(2020, 6, 26, 14),
		},
	)
	// У склада есть лог сутки, и Movement.MOVEMENT.startTime=9:00 попадает в эти сутки.
	// Поэтому мы попадаем в 25-ые логистические сутки и сдвига не происходит.
	check(
		buildWithDisabled(
			timex.DayTime{Hour: 12}, // лог сутки
			[]string{"2020-06-26"},
		),
		Time(2020, 6, 26, 6), // startTime
		[]time.Time{
			Time(2020, 6, 26, 6),  // warehouse
			Time(2020, 6, 26, 9),  // movement
			Time(2020, 6, 26, 11), // two linehaul service
			Time(2020, 6, 26, 11),
		},
	)
	// У склада есть лог сутки, и Movement.MOVEMENT.startTime=9:00 попадает в эти сутки.
	// Поэтому делаем сдвиг влево для disableDats, 25 и 26 выключено, двигаем startTime на 2 дня вправо.
	check(
		buildWithDisabled(
			timex.DayTime{Hour: 12}, // лог сутки
			[]string{"2020-06-25", "2020-06-26"},
		),
		Time(2020, 6, 26, 6), // startTime
		[]time.Time{
			Time(2020, 6, 26, 6),  // warehouse
			Time(2020, 6, 27, 12), // movement
			Time(2020, 6, 27, 14), // two linehaul services
			Time(2020, 6, 27, 14),
		},
	)
	// COMBINATOR-754
	// Добавляем в сегмент Movement еще один сервис ДО сервиса MOVEMENT.
	// Проверяем что сначало считаем MOVEMENT.start_time, и только потом считаем offset.
	// В случае ненулевого offset, пересчитываем MOVEMENT.start_time.
	build2 := func(logisticDayStart timex.DayTime, disabledDates []string) GraphData {
		gd := buildWithDisabled(logisticDayStart, disabledDates)
		dayScheduleList := make([]DaySchedule, 7)
		for i := 0; i < len(dayScheduleList); i++ {
			ds := DaySchedule{
				DayFloat: float64(i + 1),
				From:     "04:00:00",
				To:       "05:00:00",
			}
			dayScheduleList[i] = ds
		}
		daysoffGrouped := daysoff.NewServicesHashed()
		schedule, _ := NewSchedule(dayScheduleList, false)
		movementService := gd.movement.GetServices(DeliveryTypeCourier)[0]
		movementService.Schedule = schedule
		daysoffGrouped.DaysOffGrouped[movementService.ID] = daysoff.NewDaysOffGroupedFromStrings(disabledDates)
		gd.movement.setLogisticDayStart(logisticDayStart)
		return gd
	}
	check(
		build2(
			timex.DayTime{Hour: 6}, // лог сутки
			[]string{"2020-06-25", "2020-06-26"},
		),
		Time(2020, 6, 26, 6), // startTime
		[]time.Time{
			Time(2020, 6, 26, 6), // warehouse
			Time(2020, 6, 28, 4), // movement (без фикса получается 2020-06-27)
			Time(2020, 6, 28, 6), // two linehaul service
			Time(2020, 6, 28, 6),
		},
	)
}

func TestShrinkPath(t *testing.T) {
	graphEx := BuildExample1(NewGraphBuilder())
	gx := graphEx.Graph
	regions := geobase.NewExample()
	options := Options{
		SearchOptions: &SearchOptions{
			RegionMap: regions,
			RegionTo:  regions[213],
		},
		BuildOptions: &BuildOptions{
			StartTime: time.Now().Round(time.Second),
		},
	}
	wh172Service := graphEx.Warehouse172.GetServices(DeliveryTypeCourier)[0]
	wh172Service.Duration = 24 * 60
	r, err := gx.FindPaths(context.Background(), graphEx.Warehouse172.ID, &options)
	require.NoError(t, err)
	require.Len(t, r.Paths, 1)

	wh172Service = graphEx.Warehouse172.GetServices(DeliveryTypeCourier)[0]
	wh172Service.Duration = 12 * 60
	path := r.Paths[0]
	sp := SortablePath{
		Path:          path,
		ShopTariff:    &tr.OptionResult{},
		optionType:    lms.CheapLong,
		limitNum:      lms.PriceLimit,
		ServiceRating: 42,
		FromToRegions: tr.FromToRegions{
			From: geobase.RegionID(213),
			To:   geobase.RegionID(2),
		},
	}
	sp2 := gx.ShrinkPath(context.Background(), &sp, &ShrinkPathOptions{})
	require.NotNil(t, sp2)
	require.Equal(t, sp.optionType, sp2.optionType)
	require.Equal(t, sp.limitNum, sp2.limitNum)
	require.Equal(t, sp.ServiceRating, sp2.ServiceRating)
	require.Equal(t, sp.FromToRegions, sp2.FromToRegions)
}

func TestUseManyRegions(t *testing.T) {
	regions := geobase.NewExample()
	ex := NewExample()
	{
		options := Options{
			SearchOptions: &SearchOptions{
				RegionMap: regions,
				RegionTo:  regions[213],
			},
		}
		found, err := ex.G.FindPaths(context.Background(), ex.Warehouse.ID, &options)
		require.NoError(t, err)
		require.Equal(t, 1, len(found.Paths))
		require.Equal(t, SearchVersionManyRegions, found.SearchVersion)
	}
	{
		options := Options{
			SearchOptions: &SearchOptions{
				RegionMap: regions,
				RegionToList: []geobase.RegionChain{
					regions[geobase.RegionMoscow],
					regions[geobase.RegionSaintPetersburg],
				},
			},
		}
		found, err := ex.G.FindPaths(context.Background(), ex.Warehouse.ID, &options)
		require.NoError(t, err)
		require.Equal(t, 2, len(found.Paths))
		require.Equal(t, SearchVersionManyRegions, found.SearchVersion)
		require.Equal(t, geobase.RegionMoscow, found.Paths[0].DestID)
		require.Equal(t, geobase.RegionSaintPetersburg, found.Paths[1].DestID)
	}
	{
		options := Options{SearchOptions: &SearchOptions{}}
		found, err := ex.G.FindPaths(context.Background(), ex.Warehouse.ID, &options)
		require.NoError(t, err)
		require.Equal(t, 1, len(found.Paths))
		require.Equal(t, 0, int(found.Paths[0].DestID))
	}
	{
		options := Options{SearchOptions: &SearchOptions{}}
		found, err := ex.G.FindPaths(context.Background(), -1, &options)
		require.NoError(t, err)
		require.Len(t, found.Paths, 0)
		require.Len(t, found.SkippedNodes, 0)
	}
}

func TestFirstLastPaths(t *testing.T) {
	regions := geobase.NewExample()
	ex := NewExample()
	{
		options := Options{
			SearchOptions: &SearchOptions{
				RegionMap: regions,
				RegionTo:  regions[213],
				PathType:  PathTypeFirst,
			},
		}
		found, err := ex.G.FindPaths(context.Background(), ex.Warehouse.ID, &options)
		require.NoError(t, err)
		require.Len(t, found.Paths, 1)
		require.Len(t, found.Paths[0].Nodes, 1)
		{
			found, err := ex.G.FindPaths(context.Background(), -1, &options)
			require.NoError(t, err)
			require.Len(t, found.Paths, 0)
		}

	}
	{
		options := Options{
			SearchOptions: &SearchOptions{
				RegionMap: regions,
				RegionTo:  regions[213],
				PathType:  PathTypeLast,
			},
		}
		found, err := ex.G.FindPaths(context.Background(), ex.Warehouse.ID, &options)
		require.NoError(t, err)
		require.Len(t, found.Paths, 2)
		for _, path := range found.Paths {
			require.Len(t, path.Nodes, 3)
			require.Equal(t, SegmentTypeLinehaul, path.Nodes[2].Type)
		}
		{
			found, err := ex.G.FindPaths(context.Background(), -1, &options)
			require.NoError(t, err)
			require.Len(t, found.Paths, 0)
		}
	}
}

func TestBroadcast(t *testing.T) {
	regions := geobase.NewExample()
	ex := NewExampleWithSC()
	options := Options{
		SearchOptions: &SearchOptions{
			RegionMap: regions,
			RegionTo:  regions[213],
			PathType:  PathTypeBroadcast,
		},
	}
	found, err := ex.G.FindPaths(context.Background(), ex.Warehouse.ID, &options)
	require.NoError(t, err)
	require.Len(t, found.Paths, 4)
	for _, path := range found.Paths {
		require.Len(t, path.Nodes, 7)
		require.Equal(t, SegmentTypeLinehaul, path.Nodes[6].Type)
	}

	found, err = ex.G.FindPaths(context.Background(), -1, &options)
	require.NoError(t, err)
	require.Len(t, found.Paths, 0)
}

func TestFindNodeBySegmentType(t *testing.T) {
	pb := NewPathBuilder()
	wn := pb.AddWarehouse()
	pb.AddMovement()
	pb.AddLinehaul()
	pb.AddHanding(pb.WithLocation(225))
	wantNode := pb.graph.GetNodeByID(wn)
	path := pb.GetSortablePath()
	result := path.FindNodeBySegmentType(SegmentTypeWarehouse)
	require.Equal(t, wantNode, result)
	result = path.FindNodeBySegmentType(SegmentTypePickup)
	require.Nil(t, result)
}

func TestFindPickupServiceByCode(t *testing.T) {
	pb := NewPathBuilder()
	node := pb.AddWarehouse(
		pb.WithPartnerTypeDropship(),
		pb.MakeCutoffService(),
		pb.MakeProcessingService(),
		pb.MakeShipmentService(),
	)
	nilResult := pb.graph.GetNodeByID(node).FindPickupServiceByCode(enums.ServiceCallCourier)
	require.Nil(t, nilResult)
	result := pb.graph.GetNodeByID(node).FindPickupServiceByCode(enums.ServiceProcessing)
	require.Equal(t, pb.graph.GetNodeByID(node).PickupServices[1], result)
}

func TestGetSchedule(t *testing.T) {
	wantSchedule, err := newSimpleSchedule(CreateSimpleSchedule())
	require.NoError(t, err)
	service := &LogisticService{
		Schedule: wantSchedule,
	}
	result := service.GetSchedule()
	require.Equal(t, wantSchedule, result)
}

func TestGetMovements(t *testing.T) {
	node := Node{
		CourierServices: []*LogisticService{
			{
				ID:   10,
				Code: enums.ServiceMovement,
			},
		},
		PickupServices: []*LogisticService{
			{
				ID:   10,
				Code: enums.ServiceMovement,
			},
		},
	}

	require.Equal(t, []*LogisticService{
		{
			ID:   10,
			Code: enums.ServiceMovement,
		},
	}, node.GetServicesByType(enums.ServiceMovement))

	node = Node{
		PickupServices: []*LogisticService{
			{
				ID:   10,
				Code: enums.ServiceMovement,
			},
		},
	}

	require.Equal(t, []*LogisticService{
		{
			ID:   10,
			Code: enums.ServiceMovement,
		},
	}, node.GetServicesByType(enums.ServiceMovement))

	node = Node{
		CourierServices: []*LogisticService{
			{
				ID:   10,
				Code: enums.ServiceMovement,
			},
		},
	}

	require.Equal(t, []*LogisticService{
		{
			ID:   10,
			Code: enums.ServiceMovement,
		},
	}, node.GetServicesByType(enums.ServiceMovement))

	node = Node{
		CourierServices: []*LogisticService{
			{
				ID:   10,
				Code: enums.ServiceMovement,
			},
		},
		PickupServices: []*LogisticService{
			{
				ID:   11,
				Code: enums.ServiceMovement,
			},
		},
	}

	require.Equal(t, []*LogisticService{
		{
			ID:   11,
			Code: enums.ServiceMovement,
		},
		{
			ID:   10,
			Code: enums.ServiceMovement,
		},
	}, node.GetServicesByType(enums.ServiceMovement))

	node = Node{}
	require.Equal(t, []*LogisticService{}, node.GetServicesByType(enums.ServiceMovement))
}

func TestDaysOffDevolution(t *testing.T) {
	daysOff := &daysoff.ServiceDaysOff{
		Services: map[int64]daysoff.DisabledDatesMap{
			1991298: {
				129: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 43, 0, 385072000, time.UTC)),
				130: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 44, 0, 385072000, time.UTC)),
				131: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 45, 0, 385072000, time.UTC)),
				132: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 46, 0, 385072000, time.UTC)),
			},
			1991300: {
				125: daysoff.NewDisabledDate(time.Date(2021, 7, 4, 15, 31, 0, 385072000, time.UTC)),
				126: daysoff.NewDisabledDate(time.Date(2021, 7, 4, 15, 32, 0, 385072000, time.UTC)),
			},
			1991299: {
				129: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 43, 0, 385072000, time.UTC)),
				130: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 44, 0, 385072000, time.UTC)),
				131: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 45, 0, 385072000, time.UTC)),
				132: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 46, 0, 385072000, time.UTC)),
			},
			1991297: {
				129: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 43, 0, 385072000, time.UTC)),
				130: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 44, 0, 385072000, time.UTC)),
				131: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 45, 0, 385072000, time.UTC)),
				132: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 46, 0, 385072000, time.UTC)),
			},
		},
	}

	want := &daysoff.ServiceDaysOff{
		Services: map[int64]daysoff.DisabledDatesMap{
			1991298: {
				125: daysoff.NewDisabledDate(time.Date(2021, 7, 4, 15, 31, 0, 385072000, time.UTC)),
				126: daysoff.NewDisabledDate(time.Date(2021, 7, 4, 15, 32, 0, 385072000, time.UTC)),
				129: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 43, 0, 385072000, time.UTC)),
				130: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 44, 0, 385072000, time.UTC)),
				131: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 45, 0, 385072000, time.UTC)),
				132: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 46, 0, 385072000, time.UTC)),
			},
			1991300: {
				125: daysoff.NewDisabledDate(time.Date(2021, 7, 4, 15, 31, 0, 385072000, time.UTC)),
				126: daysoff.NewDisabledDate(time.Date(2021, 7, 4, 15, 32, 0, 385072000, time.UTC)),
			},
			1991299: {
				129: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 43, 0, 385072000, time.UTC)),
				130: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 44, 0, 385072000, time.UTC)),
				131: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 45, 0, 385072000, time.UTC)),
				132: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 46, 0, 385072000, time.UTC)),
			},
			1991301: {
				129: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 43, 0, 385072000, time.UTC)),
				130: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 44, 0, 385072000, time.UTC)),
				131: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 45, 0, 385072000, time.UTC)),
				132: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 46, 0, 385072000, time.UTC)),
			},
			1991297: {
				129: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 43, 0, 385072000, time.UTC)),
				130: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 44, 0, 385072000, time.UTC)),
				131: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 45, 0, 385072000, time.UTC)),
				132: daysoff.NewDisabledDate(time.Date(2021, 7, 5, 15, 46, 0, 385072000, time.UTC)),
			},
		},
	}

	shipmentMovements := map[int64][]*LogisticService{
		1991300: {&LogisticService{ID: 1991298}},
		1991299: {&LogisticService{ID: 1991301}},
	}

	ShipmentDevolution(daysOff, shipmentMovements)
	require.Equal(t, want, daysOff)
}

func TestLogisticDaysInit(t *testing.T) {
	f := &hardconfig.HardConfig{
		LogisticDayByPartners: hardconfig.LogisticDayByPartnersMap{
			1: 11,
			2: 12,
			3: 13,
		},
		LogisticDayByTypes: hardconfig.LogisticDayByTypeMap{
			enums.PartnerTypeFulfillment: 14,
		},
	}

	g := &Graph{
		PartnerToSegments: make(map[int64][]*Node),
		Neighbors:         make(map[int64][]*Node),
	}

	g.PartnerToSegments[1] = []*Node{
		{
			LogisticSegment: LogisticSegment{
				ID: 11,
			},
		},
	}
	g.PartnerToSegments[2] = []*Node{
		{
			LogisticSegment: LogisticSegment{
				ID: 21,
			},
		},
	}
	g.PartnerToSegments[3] = []*Node{
		{
			LogisticSegment: LogisticSegment{
				ID:          31,
				PartnerType: enums.PartnerTypeFulfillment,
			},
		},
	}
	g.PartnerToSegments[4] = []*Node{
		{
			LogisticSegment: LogisticSegment{
				ID: 41,
			},
		},
	}

	g.Neighbors[11] = []*Node{
		{
			LogisticSegment: LogisticSegment{
				ID:   111,
				Type: SegmentTypeMovement,
			},
		},
		{
			LogisticSegment: LogisticSegment{
				ID:   121,
				Type: SegmentTypeMovement,
			},
		},
	}
	g.Neighbors[21] = []*Node{
		{
			LogisticSegment: LogisticSegment{
				ID:   211,
				Type: SegmentTypeMovement,
			},
		},
		{
			LogisticSegment: LogisticSegment{
				ID:   221,
				Type: SegmentTypeMovement,
			},
		},
	}
	g.Neighbors[31] = []*Node{
		{
			LogisticSegment: LogisticSegment{
				ID:   311,
				Type: SegmentTypeMovement,
			},
		},
		{
			LogisticSegment: LogisticSegment{
				ID: 322,
			},
		},
	}
	g.Neighbors[41] = []*Node{
		{
			LogisticSegment: LogisticSegment{
				ID: 411,
			},
		},
		{
			LogisticSegment: LogisticSegment{
				ID: 422,
			},
		},
	}

	g.initLogisticDayStart(logging.GetLogger(), f)
	require.Equal(t, int8(11), g.PartnerToSegments[1][0].GetLogisticDayStart().Hour)
	require.Equal(t, int8(12), g.PartnerToSegments[2][0].GetLogisticDayStart().Hour)
	require.Equal(t, int8(13), g.PartnerToSegments[3][0].GetLogisticDayStart().Hour)
	require.Equal(t, int8(0), g.PartnerToSegments[4][0].GetLogisticDayStart().Hour)

	require.Equal(t, int8(11), g.Neighbors[11][0].GetLogisticDayStart().Hour)
	require.Equal(t, int8(11), g.Neighbors[11][1].GetLogisticDayStart().Hour)

	require.Equal(t, int8(12), g.Neighbors[21][0].GetLogisticDayStart().Hour)
	require.Equal(t, int8(12), g.Neighbors[21][1].GetLogisticDayStart().Hour)

	require.Equal(t, int8(13), g.Neighbors[31][0].GetLogisticDayStart().Hour)
	require.Equal(t, int8(0), g.Neighbors[31][1].GetLogisticDayStart().Hour)

	require.Equal(t, int8(0), g.Neighbors[41][0].GetLogisticDayStart().Hour)
	require.Equal(t, int8(0), g.Neighbors[41][1].GetLogisticDayStart().Hour)
}

func TestIntervalDuration(t *testing.T) {
	i := Interval{
		From: timex.DayTime{
			Hour:   10,
			Minute: 0,
			Second: 0,
		},
		To: timex.DayTime{
			Hour:   23,
			Minute: 0,
			Second: 0,
		},
	}

	require.Equal(t, 13*time.Hour, i.Duration())

	i = Interval{
		From: timex.DayTime{
			Hour:   0,
			Minute: 0,
			Second: 0,
		},
		To: timex.DayTime{
			Hour:   23,
			Minute: 0,
			Second: 0,
		},
	}
	require.Equal(t, 23*time.Hour, i.Duration())

	i = Interval{
		From: timex.DayTime{
			Hour:   10,
			Minute: 9,
			Second: 9,
		},
		To: timex.DayTime{
			Hour:   23,
			Minute: 59,
			Second: 59,
		},
	}
	require.Equal(t, 13*time.Hour+50*time.Minute+50*time.Second, i.Duration())

	i = Interval{
		From: timex.DayTime{
			Hour:   10,
			Minute: 0,
			Second: 0,
		},
		To: timex.DayTime{
			Hour:   9,
			Minute: 0,
			Second: 0,
		},
	}
	require.Equal(t, 23*time.Hour, i.Duration())
}
