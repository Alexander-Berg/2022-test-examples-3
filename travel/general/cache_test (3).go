package cache

import (
	"context"
	"fmt"
	"math/rand"
	"testing"
	"time"

	"a.yandex-team.ru/library/go/test/assertpb"
	"a.yandex-team.ru/travel/library/go/logging"
	"github.com/golang/protobuf/proto"
	"github.com/jonboulle/clockwork"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"go.uber.org/goleak"

	api "a.yandex-team.ru/travel/trains/search_api/api/tariffs"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/consts"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/date"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/tariffs"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/testutils"
)

func TestIterating(t *testing.T) {
	logger := testutils.NewLogger(t)
	clock := clockwork.NewFakeClockAt(shortDate(2020, 1, 1))
	cache := NewTariffCacheWithClock(clock, &DefaultConfig, logger)
	expectedTariffs := make([]*api.DirectionTariffInfo, 0)
	actualTariffs := make([]*api.DirectionTariffInfo, 0)

	for i := 0; i < 100; i++ {
		tariff := newDirectionTariffInfo(i, i+1, createDateProto(shortDate(2020, 1, 5)))
		expectedTariffs = append(expectedTariffs, tariff)
		cache.Add(tariff)
	}

	t.Run("Valid objects in iteration", func(t *testing.T) {
		for message := range cache.Iter(context.Background()) {
			actualTariffs = append(actualTariffs, message.(*api.DirectionTariffInfo))
		}
		assertEqualTariffs(t, expectedTariffs, actualTariffs)
	})

	t.Run("Interrupt iteration by cache", func(t *testing.T) {
		ctx, cancel := context.WithCancel(context.Background())
		cancel()

		var items []proto.Message

		for item := range cache.Iter(ctx) {
			items = append(items, item)
		}
		assert.Less(t, len(items), len(actualTariffs))
		goleak.VerifyNone(t)
	})
}

func TestTariffCache_Expirations_ByDepartures(t *testing.T) {
	clock := clockwork.NewFakeClockAt(shortDate(2020, 1, 1))
	cache := NewTariffCacheWithClock(clock, &DefaultConfig, testutils.NewLogger(t))
	cache.Add(newDirectionTariffInfo(1, 2, createDateProto(shortDate(2020, 1, 1))))
	cache.Add(newDirectionTariffInfo(1, 2, createDateProto(shortDate(2020, 1, 2))))

	cache.removeExpired()
	assertCacheSize(t, cache, 2)

	clock.Advance(24 * time.Hour)
	cache.removeExpired()
	assertCacheSize(t, cache, 1)

	clock.Advance(24 * time.Hour)
	cache.removeExpired()
	assertCacheSize(t, cache, 0)
}

func TestTariffCache_Expirations_CorrectRemoveAfterUpdating(t *testing.T) {
	clock := clockwork.NewFakeClockAt(shortDate(2020, 1, 1))
	cache := NewTariffCacheWithClock(clock, &DefaultConfig, testutils.NewLogger(t))
	cache.Add(newDirectionTariffInfo(1, 2, createDateProto(shortDate(2020, 1, 4))))

	clock.Advance(48 * time.Hour)
	cache.removeExpired()
	assertCacheSize(t, cache, 1)

	cache.Add(newDirectionTariffInfo(1, 2, createDateProto(shortDate(2020, 1, 4))))
	clock.Advance(48 * time.Hour)
	cache.removeExpired()
	assertCacheSize(t, cache, 0)
}

func TestTariffCache_Expirations_CheckMemoryLeaks(t *testing.T) {
	clock := clockwork.NewFakeClockAt(shortDate(2020, 1, 1))
	cache := NewTariffCacheWithClock(clock, &DefaultConfig, testutils.NewLogger(t))

	// frequently changing tariff
	for i := 0; i < 100; i++ {
		cache.Add(newDirectionTariffInfo(1, 2, createDateProto(shortDate(2020, 1, 4))))
		cache.Add(newDirectionTariffInfo(1, 2, createDateProto(shortDate(2020, 1, 4))))
	}
	assertCacheSize(t, cache, 1)

	clock.Advance(10 * 24 * time.Hour)
	cache.removeExpired()
	assertCacheSize(t, cache, 0)
}

func assertCacheSize(t *testing.T, cache *TariffCache, size int) {
	assert.Len(t, cache.tariffs, size)
	assert.Len(t, cache.expirations.container, size)
}

func TestTariffCache_SelectByDirections(t *testing.T) {
	clock := clockwork.NewFakeClockAt(shortDate(2020, 1, 1))
	cache := NewTariffCacheWithClock(clock, &DefaultConfig, testutils.NewLogger(t))
	t1, t2, t3 := getCachedTariffs(t, cache)

	for _, testCase := range []struct {
		description     string
		directions      []tariffs.TrainDirection
		leftBorder      time.Time
		rightBorder     time.Time
		expectedTariffs []*api.DirectionTariffInfo
	}{
		{
			"One tariff",
			[]tariffs.TrainDirection{
				{DeparturePointExpressID: 1, ArrivalPointExpressID: 2},
			},
			shortDate(2020, 1, 4),
			shortDate(2020, 1, 5),
			[]*api.DirectionTariffInfo{t1},
		},
		{
			"Check selecting with timezones",
			[]tariffs.TrainDirection{
				{DeparturePointExpressID: 1, ArrivalPointExpressID: 2},
			},
			shortDate(2020, 1, 4),
			shortDate(2020, 1, 5).Add(-time.Hour).In(consts.MskLocation),
			[]*api.DirectionTariffInfo{t1},
		},
		{
			"Big time interval",
			[]tariffs.TrainDirection{
				{DeparturePointExpressID: 1, ArrivalPointExpressID: 2},
			},
			shortDate(2020, 1, 4),
			shortDate(2020, 1, 6),
			[]*api.DirectionTariffInfo{t1, t2},
		},
		{
			"Many directions",
			[]tariffs.TrainDirection{
				{DeparturePointExpressID: 1, ArrivalPointExpressID: 2},
				{DeparturePointExpressID: 3, ArrivalPointExpressID: 2},
			},
			shortDate(2020, 1, 4),
			shortDate(2020, 1, 6),
			[]*api.DirectionTariffInfo{t1, t2, t3},
		},
		{
			"Empty interval",
			[]tariffs.TrainDirection{
				{DeparturePointExpressID: 1, ArrivalPointExpressID: 2},
				{DeparturePointExpressID: 3, ArrivalPointExpressID: 2},
			},
			shortDate(2020, 1, 2),
			shortDate(2020, 1, 3),
			[]*api.DirectionTariffInfo{},
		},
	} {
		t.Run(testCase.description, func(t *testing.T) {
			cachedTariffs, err := cache.SelectByDirections(
				context.Background(),
				testCase.directions,
				testCase.leftBorder,
				testCase.rightBorder,
			)
			require.NoError(t, err)
			assertEqualTariffs(t, testCase.expectedTariffs, cachedTariffs)
		})
		t.Run(testCase.description+": same behavior for select method", func(t *testing.T) {
			var departureIDs, arrivalIDs []int32
			for _, direction := range testCase.directions {
				departureIDs = append(departureIDs, direction.DeparturePointExpressID)
				arrivalIDs = append(arrivalIDs, direction.ArrivalPointExpressID)
			}

			cachedTariffs, err := cache.Select(
				context.Background(),
				departureIDs,
				arrivalIDs,
				testCase.leftBorder,
				testCase.rightBorder,
			)
			require.NoError(t, err)
			assertEqualTariffs(t, testCase.expectedTariffs, cachedTariffs)
		})
	}
}

func TestTariffCache_Select(t *testing.T) {
	clock := clockwork.NewFakeClockAt(shortDate(2020, 1, 1))
	cache := NewTariffCacheWithClock(clock, &DefaultConfig, testutils.NewLogger(t))
	t1, t2, t3 := getCachedTariffs(t, cache)

	for _, testCase := range []struct {
		description     string
		departureIDs    []int32
		arrivalIDs      []int32
		leftBorder      time.Time
		rightBorder     time.Time
		expectedTariffs []*api.DirectionTariffInfo
	}{
		{
			"One points",
			[]int32{1, 3},
			[]int32{2},
			shortDate(2020, 1, 5),
			shortDate(2020, 1, 6),
			[]*api.DirectionTariffInfo{t1, t2, t3},
		},
		{
			"With garbage points",
			[]int32{1, 3, 6, 1},
			[]int32{2, 3},
			shortDate(2020, 1, 5),
			shortDate(2020, 1, 6),
			[]*api.DirectionTariffInfo{t1, t2, t3},
		},
	} {
		t.Run(testCase.description, func(t *testing.T) {
			cachedTariffs, err := cache.Select(
				context.Background(),
				testCase.departureIDs,
				testCase.arrivalIDs,
				testCase.leftBorder,
				testCase.rightBorder,
			)
			require.NoError(t, err)
			assertEqualTariffs(t, testCase.expectedTariffs, cachedTariffs)
		})
	}
}

func assertEqualTariffs(t *testing.T, expected, actual []*api.DirectionTariffInfo) {
	require.Equal(t, len(expected), len(actual))

	actualTariffIndex := make(map[tariffRouteKey]*api.DirectionTariffInfo)
	for _, tariff := range actual {
		actualTariffIndex[makeTariffRouteKey(
			tariff.DeparturePointExpressId,
			tariff.ArrivalPointExpressId,
			tariff.DepartureDate,
		)] = tariff
	}

	for _, tariff := range expected {
		key := makeTariffRouteKey(
			tariff.DeparturePointExpressId,
			tariff.ArrivalPointExpressId,
			tariff.DepartureDate,
		)
		actualTariff, found := actualTariffIndex[key]
		if !found {
			t.Fatalf("not found tariff [%d, %d, %d-%d-%d]",
				key.departurePointExpressID,
				key.arrivalPointExpressID,
				key.departureDate.year,
				key.departureDate.month,
				key.departureDate.day,
			)
		}
		assertpb.Equal(t, tariff, actualTariff)
	}
}

func getCachedTariffs(t *testing.T, cache *TariffCache) (*api.DirectionTariffInfo, *api.DirectionTariffInfo, *api.DirectionTariffInfo) {
	t1 := newDirectionTariffInfo(1, 2, createDateProto(shortDate(2020, 1, 5)))
	t2 := newDirectionTariffInfo(1, 2, createDateProto(shortDate(2020, 1, 6)))
	t3 := newDirectionTariffInfo(3, 2, createDateProto(shortDate(2020, 1, 6)))
	t4 := newDirectionTariffInfo(3, 2, createDateProto(shortDate(2020, 1, 7)))
	cache.Add(t1)
	cache.Add(t2)
	cache.Add(t3)
	cache.Add(t4)
	return t1, t2, t3
}

const benchmarkTariffsCount = 1000000

var (
	benchmarkTariffs []*api.DirectionTariffInfo
	logger, _        = logging.New(&logging.DefaultConfig)
)

// check work with concurrent setting many values
func BenchmarkTariffCache_Add(b *testing.B) {
	ctx, ctxCancel := context.WithCancel(context.Background())
	defer ctxCancel()

	clock := clockwork.NewFakeClockAt(shortDate(2021, 1, 1))
	cache := prepareCacheForBenchmark(b, clock, ctx, false)

	b.RunParallel(func(pb *testing.PB) {
		i := 0
		for pb.Next() {
			cache.Add(benchmarkTariffs[i])
			i++
			if i >= benchmarkTariffsCount {
				i = 0
			}
		}
	})
}

// check work with concurrent setting many values
func BenchmarkTariffCache_Select(b *testing.B) {
	ctx, ctxCancel := context.WithCancel(context.Background())
	defer ctxCancel()

	clock := clockwork.NewFakeClockAt(shortDate(2020, 1, 1))
	cache := prepareCacheForBenchmark(b, clock, ctx, true)

	b.RunParallel(func(pb *testing.PB) {
		i := 0
		for pb.Next() {

			selectTariff(cache, i)
			i++
			if i >= benchmarkTariffsCount {
				i = 0
			}
		}
	})
}

// check work with concurrent setting many values
func BenchmarkTariffCache_AddSelect(b *testing.B) {
	ctx, ctxCancel := context.WithCancel(context.Background())
	defer ctxCancel()

	clock := clockwork.NewFakeClockAt(shortDate(2020, 1, 1))
	cache := prepareCacheForBenchmark(b, clock, ctx, true)

	b.RunParallel(func(pbt *testing.PB) {
		i := 0
		for pbt.Next() {
			if i%5 == 0 { // do Set sometimes
				cache.Add(benchmarkTariffs[i])
			} else { // in other times - do Get
				selectTariff(cache, i)
			}
			i++
			if i >= benchmarkTariffsCount {
				i = 0
			}
		}
	})
}

// check work with concurrent setting many values
func BenchmarkTariffCache_AddSelectWhileExpiring(b *testing.B) {
	ctx, ctxCancel := context.WithCancel(context.Background())
	defer ctxCancel()

	clock := clockwork.NewFakeClockAt(shortDate(2020, 1, 1))
	cache := prepareCacheForBenchmark(b, clock, ctx, true)

	clock.Advance(2 * 365 * 24 * time.Hour)
	go cache.removeExpired()
	b.RunParallel(func(pbt *testing.PB) {
		i := 0
		for pbt.Next() {
			if i%5 == 0 { // do Set sometimes
				cache.Add(benchmarkTariffs[i])
			} else { // in other times - do Get
				selectTariff(cache, i)
			}
			i++
			if i >= benchmarkTariffsCount {
				i = 0
			}
		}
		fmt.Printf("Cache size: %d\n", len(cache.tariffs))
	})
}

func selectTariff(cache *TariffCache, tariffNumber int) {
	tariffInfo := benchmarkTariffs[tariffNumber]
	_, _ = cache.Select(
		context.Background(),
		[]int32{tariffInfo.DeparturePointExpressId},
		[]int32{tariffInfo.ArrivalPointExpressId},
		date.GetDateFromProto(tariffInfo.DepartureDate),
		date.GetDateFromProto(tariffInfo.DepartureDate),
	)
}

func prepareCacheForBenchmark(b *testing.B, clock clockwork.Clock, ctx context.Context, fillCache bool) *TariffCache {
	rand.Seed(time.Now().UnixNano())
	fmt.Printf("b.N=%d\n", b.N)
	initBenchmarkData()

	cache := NewTariffCacheWithClock(clock, &DefaultConfig, logger)
	cache.Run(ctx)

	if fillCache {
		for i := 0; i < benchmarkTariffsCount; i++ {
			cache.Add(benchmarkTariffs[i])
		}
		fmt.Printf("Cache size: %d\n", len(cache.tariffs))
	}

	b.ReportAllocs()
	b.SetBytes(int64(b.N))
	b.ResetTimer()
	fmt.Println("Prepare complete")
	return cache
}

func initBenchmarkData() {
	if benchmarkTariffs != nil {
		return
	}

	for i := 0; i < benchmarkTariffsCount; i++ {
		departureDate := shortDate(2020, rand.Intn(11)+1, rand.Intn(20)+1)
		benchmarkTariffs = append(benchmarkTariffs, newDirectionTariffInfo(
			rand.Int(),
			rand.Int(),
			createDateProto(departureDate),
		))
	}
}
