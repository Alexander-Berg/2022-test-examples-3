package popular

import (
	"context"
	"sync"
	"testing"
	"time"

	"github.com/golang/protobuf/proto"
	"github.com/stretchr/testify/assert"
	"go.uber.org/goleak"

	"a.yandex-team.ru/travel/buses/backend/internal/common/logging"
	pb "a.yandex-team.ru/travel/buses/backend/proto"
)

var (
	logger, _      = logging.New(&logging.DefaultConfig)
	pointKeyUniqID uint32
)

func newPointKey() *pb.TPointKey {
	pointKeyUniqID++
	return &pb.TPointKey{
		Type: pb.EPointKeyType_POINT_KEY_TYPE_SETTLEMENT,
		Id:   pointKeyUniqID,
	}
}

type Sample struct {
	From  *pb.TPointKey
	To    *pb.TPointKey
	Count int
}

func TestPopularDirections(t *testing.T) {
	cfg := Config{
		DeflationMultiplier: 0.5,
		TopSize:             4,
		DropThreshold:       0.7,
	}
	directions := NewDirections(cfg, logger)

	samples := []Sample{
		{
			From:  newPointKey(),
			To:    newPointKey(),
			Count: 1000,
		},
		{
			From:  newPointKey(),
			To:    newPointKey(),
			Count: 500,
		},
		{
			From:  newPointKey(),
			To:    newPointKey(),
			Count: 10,
		},
		{
			From:  newPointKey(),
			To:    newPointKey(),
			Count: 2,
		},
		{
			From:  newPointKey(),
			To:    newPointKey(),
			Count: 1,
		},
	}

	var wg sync.WaitGroup
	for _, sample := range samples {
		wg.Add(1)
		go func(sample Sample) {
			defer wg.Done()
			for i := 0; i < sample.Count; i++ {
				directions.Register(sample.From, sample.To)
			}
		}(sample)
	}
	wg.Wait()

	directions.rebuild()
	popularDirections := directions.GetDirections()
	if len(popularDirections) != cfg.TopSize {
		t.Errorf("Unexpected quantity: %d != %d", len(popularDirections), cfg.TopSize)
		return
	}
	for i, direction := range popularDirections {
		if direction.To.Id != samples[i].To.Id || direction.From.Id != samples[i].From.Id {
			t.Error("Unexpected order (1)")
			return
		}
	}

	deflation := cfg.DeflationMultiplier
	for i := 1; i <= 100; i++ {
		directions.deflate()
		directions.rebuild()
		popularDirections = directions.GetDirections()
		j := 0
		for _, sample := range samples {
			if float32(sample.Count)*deflation < cfg.DropThreshold {
				continue
			}
			if sample.To.Id != popularDirections[j].To.Id || sample.From.Id != popularDirections[j].From.Id {
				t.Error("Unexpected order (2)")
				return
			}
			j++
		}
		deflation *= cfg.DeflationMultiplier
	}
}

func TestPopularDirectionsFrom(t *testing.T) {
	cfg := Config{
		DeflationMultiplier: 0.5,
		TopSize:             4,
		TopSizeFrom:         4,
		DropThreshold:       0.7,
	}
	directions := NewDirections(cfg, logger)

	from := newPointKey()
	samples := []Sample{
		{
			From:  from,
			To:    newPointKey(),
			Count: 1000,
		},
		{
			From:  from,
			To:    newPointKey(),
			Count: 500,
		},
		{
			From:  from,
			To:    newPointKey(),
			Count: 10,
		},
		{
			From:  from,
			To:    newPointKey(),
			Count: 2,
		},
		{
			From:  from,
			To:    newPointKey(),
			Count: 1,
		},
	}

	var wg sync.WaitGroup
	for _, sample := range samples {
		wg.Add(1)
		go func(sample Sample) {
			defer wg.Done()
			for i := 0; i < sample.Count; i++ {
				directions.Register(sample.From, sample.To)
			}
		}(sample)
	}
	wg.Wait()

	directions.rebuild()
	popularDirections := directions.GetDirectionsFrom(from)
	if len(popularDirections) != cfg.TopSizeFrom {
		t.Errorf("Unexpected quantity: %d != %d", len(popularDirections), cfg.TopSize)
		return
	}
	for i, direction := range popularDirections {
		if direction.To.Id != samples[i].To.Id || direction.From.Id != samples[i].From.Id {
			t.Error("Unexpected order (1)")
			return
		}
	}

	deflation := cfg.DeflationMultiplier
	for i := 1; i <= 100; i++ {
		directions.deflate()
		directions.rebuild()
		popularDirections := directions.GetDirectionsFrom(from)
		j := 0
		for _, sample := range samples {
			if float32(sample.Count)*deflation < cfg.DropThreshold {
				continue
			}
			if j == len(popularDirections) {
				continue
			}
			if sample.To.Id != popularDirections[j].To.Id || sample.From.Id != popularDirections[j].From.Id {
				t.Error("Unexpected order (2)")
				return
			}
			j++
		}
		deflation *= cfg.DeflationMultiplier
	}
}

func TestPopularDirectionsIteration(t *testing.T) {
	cfg := Config{
		DeflationMultiplier: 0.5,
		TopSize:             4,
		TopSizeFrom:         4,
		DropThreshold:       0.7,
	}
	directions := NewDirections(cfg, logger)

	for i := 0; i < 10; i++ {
		directions.Register(newPointKey(), newPointKey())
	}

	t.Run("PopularDirectionsIteration. Full iteration case", func(t *testing.T) {
		var items []proto.Message
		for item := range directions.Iter(context.Background()) {
			items = append(items, item)
		}
		assert.Len(t, items, 10)
	})
	t.Run("PopularDirectionsIteration. Interruption case", func(t *testing.T) {
		ctx, cancel := context.WithCancel(context.Background())
		cancel()
		var items []proto.Message

		itemsChannel := directions.Iter(ctx)
		time.Sleep(20 * time.Millisecond)
		for item := range itemsChannel {
			items = append(items, item)
		}
		assert.Len(t, items, 0, "Length of items should be equal %d, but equal %d", 0, len(items))
		goleak.VerifyNone(t)
	})
}

// concurrent Register/Get/deflate/rebuild
func BenchmarkPopularDirections(b *testing.B) {

	cfg := Config{
		DeflationMultiplier: 0.9,
		TopSize:             50,
		DropThreshold:       1,
	}
	directions := NewDirections(cfg, logger)

	b.ReportAllocs()
	b.SetBytes(int64(b.N))
	b.ResetTimer()

	b.RunParallel(func(pbt *testing.PB) {
		i := 0
		from := newPointKey()
		to := newPointKey()
		for pbt.Next() {
			directions.Register(from, to)
			if i%100 == 0 {
				from = newPointKey()
			}
			if i%51 == 0 {
				to = newPointKey()
			}
			if i%1000 == 0 {
				directions.deflate()
			}
			if i%1001 == 0 {
				directions.rebuild()
			}
			_ = directions.GetDirections()
			_ = directions.GetDirectionsFrom(from)
			_ = directions.GetDirectionsFrom(to)
			i++
		}
	})
}
