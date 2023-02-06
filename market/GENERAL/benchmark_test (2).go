package graph

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/market/combinator/pkg/enums"
	"a.yandex-team.ru/market/combinator/pkg/geobase"
	"a.yandex-team.ru/market/combinator/pkg/timex"
)

type BigGraphEnv struct {
	gg            *Graph
	rootWarehouse *Node
}

func NewBigGraphEnv() BigGraphEnv {
	_id := 0
	CalcID := func() int64 {
		_id++
		return int64(_id)
	}
	schedule, _ := CreateAroundTheClockSchedule(false)

	NewWarehouse := func() *Node {
		id := CalcID()
		services := []*LogisticService{
			{
				Schedule: schedule,
				Price:    11,
			},
		}
		return &Node{
			LogisticSegment: LogisticSegment{
				ID:           id,
				PartnerLmsID: id,
				LocationID:   geobase.RegionKotelniki,
				PointLmsID:   id,
				Type:         SegmentTypeWarehouse,
			},
			CourierServices: services,
			PickupServices:  services,
		}
	}
	NewMovement := func() *Node {
		id := CalcID()
		services := []*LogisticService{
			{
				Schedule: schedule,
			},
		}
		return &Node{
			LogisticSegment: LogisticSegment{
				ID:           id,
				PartnerLmsID: id,
				Type:         SegmentTypeMovement,
			},
			CourierServices: services,
			PickupServices:  services,
		}
	}
	NewLinehaul := func() *Node {
		id := CalcID()
		services := []*LogisticService{
			{
				Code:     enums.ServiceLastMile,
				Price:    22,
				Schedule: schedule,
			},
		}
		return &Node{
			LogisticSegment: LogisticSegment{
				ID:           id,
				PartnerLmsID: id,
				LocationID:   geobase.RegionMoscow,
				Type:         SegmentTypeLinehaul,
			},
			CourierServices: services,
			PickupServices:  services,
		}
	}

	gg := NewGraphWithHintsV3(nil)
	edgesToAdd := make([][2]int64, 0)
	Add := func(node *Node, prev *Node) {
		gg.AddNode(*node)
		if prev != nil {
			edgesToAdd = append(edgesToAdd, [2]int64{prev.ID, node.ID})
		}
	}
	EndAdd := func() {
		for _, pair := range edgesToAdd {
			_ = gg.AddEdge(pair[0], pair[1])
		}
	}

	warehouse0 := NewWarehouse()
	Add(warehouse0, nil)

	warehouse := warehouse0
	for i2 := 0; i2 < 20; i2++ {
		movement := NewMovement()
		Add(movement, warehouse)
		for i3 := 0; i3 < 20; i3++ {
			linehaul := NewLinehaul()
			Add(linehaul, movement)
		}
	}
	EndAdd()
	return BigGraphEnv{
		gg:            gg,
		rootWarehouse: warehouse0,
	}
}

func BenchmarkFindPaths(b *testing.B) {
	env := NewBigGraphEnv()
	gg := env.gg
	startID := env.rootWarehouse.ID
	{
		pathsFound, err := gg.FindPaths(context.Background(), startID, nil)
		assert.NoError(b, err)
		assert.Equal(b, 400, len(pathsFound.Paths))
	}

	b.ResetTimer()
	b.Run("DFS", func(b *testing.B) {
		for i := 0; i < b.N; i++ {
			_, _ = gg.FindPaths(context.Background(), startID, nil)
		}
	})
}

func BenchmarkNewDayTime(b *testing.B) {
	b.Run("FromString", func(b *testing.B) {
		for i := 0; i < b.N; i++ {
			_, _ = timex.NewDayTime("11:22:33")
		}
	})
	now := time.Now().Local()
	b.Run("FromTime", func(b *testing.B) {
		for i := 0; i < b.N; i++ {
			_ = timex.NewDayTimeFromTime(now)
		}
	})
}

func BenchmarkTime(b *testing.B) {
	tloc := time.Date(1979, 06, 26, 12, 13, 14, 0, time.Local)
	tutc := time.Date(1979, 06, 26, 12, 13, 14, 0, time.UTC)
	b.ResetTimer()
	b.Run("time.Weekday.Local", func(b *testing.B) {
		for i := 0; i < b.N; i++ {
			_ = tloc.Weekday()
		}
	})
	b.Run("time.Weekday.UTC", func(b *testing.B) {
		for i := 0; i < b.N; i++ {
			_ = tutc.Weekday()
		}
	})
	b.Run("time.YearDay.Local", func(b *testing.B) {
		for i := 0; i < b.N; i++ {
			_ = tloc.YearDay()
		}
	})
	b.Run("time.YearDay.UTC", func(b *testing.B) {
		for i := 0; i < b.N; i++ {
			_ = tutc.YearDay()
		}
	})
	b.Run("time.StripUpToDay.Local", func(b *testing.B) {
		for i := 0; i < b.N; i++ {
			_ = timex.StripUpToDay(tloc)
		}
	})
	b.Run("time.StripUpToDay.UTC", func(b *testing.B) {
		for i := 0; i < b.N; i++ {
			_ = timex.StripUpToDay(tutc)
		}
	})
}
