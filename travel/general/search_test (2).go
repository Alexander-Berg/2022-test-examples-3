package cache

import (
	"context"
	"fmt"
	"math"
	"math/rand"
	"strconv"
	"strings"
	"testing"
	"time"

	"a.yandex-team.ru/travel/library/go/metrics"
	tpb "a.yandex-team.ru/travel/proto"
	"github.com/golang/protobuf/proto"
	gpb "github.com/golang/protobuf/ptypes"
	"github.com/jonboulle/clockwork"
	"github.com/stretchr/testify/assert"
	"go.uber.org/goleak"

	"a.yandex-team.ru/travel/buses/backend/internal/common/logging"
	ipb "a.yandex-team.ru/travel/buses/backend/internal/common/proto"
	pb "a.yandex-team.ru/travel/buses/backend/proto"
)

const (
	itemsNumber int = 1000000
)

var (
	chars = []rune("ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
		"abcdefghijklmnopqrstuvwxyz" +
		"0123456789")
	searchCacheRecords     []*ipb.TSearchCacheRecord = nil
	searchCacheRecordsKeys []SearchKey               = nil
	logger, _                                        = logging.New(&logging.DefaultConfig)
	rideIDUnique                                     = 1
	metricsRegistry                                  = metrics.NewAppMetricsRegistryWithPrefix("")
	appMetrics                                       = metrics.NewAppMetrics(metricsRegistry)
)

func newRandomString(charSet *[]rune, length int) string {
	var b strings.Builder
	for i := 0; i < length; i++ {
		b.WriteRune((*charSet)[rand.Intn(len(*charSet))])
	}
	return b.String()
}

func newTestRide() *pb.TRide {
	testRide := pb.TRide{
		Id:            strconv.Itoa(rideIDUnique),
		CarrierCode:   newRandomString(&chars, 15),
		ArrivalTime:   gpb.TimestampNow().Seconds,
		DepartureTime: gpb.TimestampNow().Seconds,
		From: &pb.TPointKey{
			Type: pb.EPointKeyType_POINT_KEY_TYPE_SETTLEMENT,
			Id:   rand.Uint32(),
		},
		FromDesc: newRandomString(&chars, 35),
		To: &pb.TPointKey{
			Type: pb.EPointKeyType_POINT_KEY_TYPE_SETTLEMENT,
			Id:   rand.Uint32(),
		},
		ToDesc: newRandomString(&chars, 35),
		Fee: &tpb.TPrice{
			Amount:   rand.Int63n(70) * 100,
			Currency: tpb.ECurrency_C_RUB,
		},
		Price: &tpb.TPrice{
			Amount:   (rand.Int63n(1000) + 500) * 100,
			Currency: tpb.ECurrency_C_RUB,
		},
		Status:           pb.ERideStatus_RIDE_STATUS_SALE,
		FreeSeats:        rand.Int31n(50) - 1,
		Bus:              newRandomString(&chars, 15),
		CanPayOffline:    false,
		BookOnly:         false,
		Benefits:         []pb.EBenefitType{pb.EBenefitType_BENEFIT_TYPE_COFFEE},
		RouteName:        newRandomString(&chars, 30),
		RouteNumber:      newRandomString(&chars, 10),
		TicketLimit:      5,
		OnlineRefund:     true,
		RefundConditions: newRandomString(&chars, 100),
		BookFields:       []string{newRandomString(&chars, 10)},
	}
	rideIDUnique++
	return &testRide
}

func newSearchCacheRecord(ridesCount int) *ipb.TSearchCacheRecord {
	testRides := make([]*pb.TRide, ridesCount)
	for i := 0; i < ridesCount; i++ {
		testRides[i] = newTestRide()
	}

	return &ipb.TSearchCacheRecord{
		Status:    ipb.ECacheRecordStatus_CACHE_RECORD_STATUS_OK,
		CreatedAt: time.Now().Unix(),
		Rides:     testRides,
	}
}

func getRandomPointKey() *pb.TPointKey {
	if rand.Int31n(2) == 0 {
		return &pb.TPointKey{
			Type: pb.EPointKeyType_POINT_KEY_TYPE_SETTLEMENT,
			Id:   uint32(rand.Int63n(100000) + 900000),
		}
	} else {
		return &pb.TPointKey{
			Type: pb.EPointKeyType_POINT_KEY_TYPE_STATION,
			Id:   uint32(rand.Int63n(100000) + 900000),
		}
	}
}

func newSearchCacheRecordKey() SearchKey {
	date := tpb.TDate{
		Year:  2020,
		Month: int32(rand.Intn(4) + 2),
		Day:   int32(rand.Intn(30)),
	}

	searchKey := NewSearchKey(uint32(rand.Int31n(13)), getRandomPointKey(), getRandomPointKey(), &date)
	return searchKey
}

func newSearchCacheRecordSlice(length int) ([]*ipb.TSearchCacheRecord, []SearchKey) {
	fmt.Println("newSearchCacheRecordSlice. Creating test data...")

	searchCacheRecordSlice := make([]*ipb.TSearchCacheRecord, length)
	searchCacheRecordSliceKeys := make([]SearchKey, length)
	for i := 0; i < length; i++ {
		searchCacheRecordSlice[i] = newSearchCacheRecord(rand.Intn(20))
		searchCacheRecordSliceKeys[i] = newSearchCacheRecordKey()

		if (i+1)%100000 == 0 {
			fmt.Printf("Created %d values\n", i+1)
		}
	}

	return searchCacheRecordSlice, searchCacheRecordSliceKeys
}

func checkRequiredKeys(t *testing.T, requiredKeys []SearchKey, cache *SearchRecordStorage, rideID string) bool {
	keys, ok := cache.GetSearchKeysByRideID(rideID)
	if !ok {
		if len(requiredKeys) != 0 {
			t.Errorf("checkRequiredKeys: length diffrent: %d != 0", len(requiredKeys))
			return false
		}
		return true
	}
	if len(keys) == 0 {
		t.Errorf("checkRequiredKeys: empty key was not deleted")
		return false
	}
	for _, requiredKey := range requiredKeys {
		if _, ok = keys[requiredKey]; !ok {
			t.Errorf("checkRequiredKeys: no %v", requiredKey)
			return false
		}
	}
	if len(requiredKeys) != len(keys) {
		t.Errorf("checkRequiredKeys: length diffrent: %d != %d", len(requiredKeys), len(keys))
		return false
	}
	return true
}

func TestConcurrentMap(t *testing.T) {
	rand.Seed(time.Now().UnixNano())

	testCache := NewSearchRecordStorage(time.Duration(math.MaxInt64), appMetrics, &DefaultConfig, logger)
	ctx, ctxCancel := context.WithCancel(context.Background())
	testCache.Run(ctx)
	defer ctxCancel()

	if testCache == nil {
		t.Errorf("Error creating SearchRecordStorage")
	}

	searchCacheRecord := newSearchCacheRecord(rand.Intn(20) + 1)
	testKey := newSearchCacheRecordKey()

	t.Run("SearchRecordStorage. Get non existent key", func(t *testing.T) {
		_, ok := testCache.Get(testKey)
		if ok {
			t.Errorf("Error at SearchRecordStorage.Get() method. Unexpected 'ok' value, must be 'false', got %t", ok)
		}
	})

	t.Run("SearchRecordStorage. Set value by random key", func(t *testing.T) {
		testCache.Set(testKey, searchCacheRecord)
		if length := testCache.Len(); length != 1 {
			t.Errorf("Error at SearchRecordStorage.Set() method, cache length must be 1, got %d, key %+v", length, testKey)
		}
	})

	t.Run("SearchRecordStorage. Get value by random key", func(t *testing.T) {
		// TODO(diyakov) check content
		someSearchRecord, ok := testCache.Get(testKey)
		if !ok {
			t.Errorf("Error at SearchRecordStorage.Get() method. Unexpected 'ok' value, must be 'true', got %t, key %+v", ok, testKey)
		}
		if len(someSearchRecord.Rides) == 0 {
			t.Errorf("ERROR. Unexpected empty SearchCacheRecord %v", someSearchRecord)
		}
		for _, r := range searchCacheRecord.Rides {
			if !checkRequiredKeys(t, []SearchKey{testKey}, testCache, r.Id) {
				t.Error("Error at SearchRecordStorage.GetSearchKeyByRideID() method: not set")
				break
			}
		}
	})

	newSearchCacheRecord := newSearchCacheRecord(rand.Intn(20) + 1)

	t.Run("SearchRecordStorage. Secondary set value by random key", func(t *testing.T) {
		testCache.Set(testKey, newSearchCacheRecord)
		if length := testCache.Len(); length != 1 {
			t.Errorf("Error at SearchRecordStorage.Set() method, cache length must be 1, got %d, key %+v", length, testKey)
		}
	})

	t.Run("SearchRecordStorage. Get value by random key", func(t *testing.T) {
		// TODO(diyakov) check content
		someSearchRecord, ok := testCache.Get(testKey)
		if !ok {
			t.Errorf("Error at SearchRecordStorage.Get() method. Unexpected 'ok' value, must be 'true', got %t, key %+v", ok, testKey)
		}
		if len(someSearchRecord.Rides) == 0 {
			t.Errorf("ERROR. Unexpected empty SearchCacheRecord %v", someSearchRecord)
		}
		for _, r := range searchCacheRecord.Rides {
			if !checkRequiredKeys(t, []SearchKey{}, testCache, r.Id) {
				t.Error("Error at SearchRecordStorage.GetSearchKeyByRideID() method: should be not set")
				break
			}
		}
		for _, r := range newSearchCacheRecord.Rides {
			if !checkRequiredKeys(t, []SearchKey{testKey}, testCache, r.Id) {
				t.Error("Error at SearchRecordStorage.GetSearchKeyByRideID() method: not set")
				break
			}
		}
	})

	t.Run("SearchRecordStorage. Delete value by random key", func(t *testing.T) {
		testCache.Delete(testKey)
		if length := testCache.Len(); length != 0 {
			t.Errorf("Error at SearchRecordStorage.delete() method, cache length must be 0, got %d, key %+v", length, testKey)
		}
		for _, r := range searchCacheRecord.Rides {
			if !checkRequiredKeys(t, []SearchKey{}, testCache, r.Id) {
				t.Error("Error at SearchRecordStorage.GetSearchKeyByRideID() method: not deleted")
				break
			}
		}
	})
}

func TestExpirations(t *testing.T) {
	searchCacheRecordA := newSearchCacheRecord(7)
	searchCacheRecordB := newSearchCacheRecord(2)
	rideIDsA := make([]string, len(searchCacheRecordA.Rides))
	for i, ride := range searchCacheRecordA.Rides {
		rideIDsA[i] = ride.Id
	}
	rideIDsB := make([]string, len(searchCacheRecordB.Rides))
	for i, ride := range searchCacheRecordB.Rides {
		rideIDsB[i] = ride.Id
	}

	testKeyA := newSearchCacheRecordKey()
	testKeyA.DateDay = 1
	testKeyB := newSearchCacheRecordKey()
	testKeyB.DateDay = 2

	waitDuration := time.Millisecond * 10

	t.Run("SearchRecordStorage. Expiring is ordered", func(t *testing.T) {
		var okA, okB bool

		fakeClock := clockwork.NewFakeClock()
		testCache := NewSearchRecordStorageWithClock(
			time.Second, fakeClock, appMetrics, &Config{StorageExpiration: time.Millisecond}, logger)
		ctx, ctxCancel := context.WithCancel(context.Background())
		testCache.Run(ctx)
		defer ctxCancel()
		fakeClock.BlockUntil(1)

		searchCacheRecordA.CreatedAt = fakeClock.Now().Add(time.Second * 10).Unix()
		testCache.Set(testKeyA, searchCacheRecordA)

		searchCacheRecordB.CreatedAt = fakeClock.Now().Unix()
		testCache.Set(testKeyB, searchCacheRecordB)

		time.Sleep(waitDuration)
		_, okA = testCache.Get(testKeyA)
		_, okB = testCache.Get(testKeyB)
		if !okA || !okB {
			t.Errorf("Some record is expired too early: %v %v", okA, okB)
			return
		}

		fakeClock.Advance(time.Second * 5)
		time.Sleep(waitDuration)
		_, okA = testCache.Get(testKeyA)
		_, okB = testCache.Get(testKeyB)
		if !okA {
			t.Errorf("Record A is expired too early")
			return
		}
		for _, rideID := range rideIDsA {
			if !checkRequiredKeys(t, []SearchKey{testKeyA}, testCache, rideID) {
				t.Errorf("rideID index for A is expired too early")
				return
			}
		}
		if okB {
			t.Errorf("Record B is not expired at the time")
			return
		}
		for _, rideID := range rideIDsB {
			if !checkRequiredKeys(t, []SearchKey{}, testCache, rideID) {
				t.Errorf("rideID index for B is not expired at time")
				return
			}
		}

		fakeClock.Advance(time.Duration(10) * time.Second)
		time.Sleep(waitDuration)
		_, okA = testCache.Get(testKeyA)
		if okA {
			t.Errorf("Record A is not expired at the time")
			return
		}
		for _, rideID := range rideIDsA {
			if !checkRequiredKeys(t, []SearchKey{}, testCache, rideID) {
				t.Errorf("rideID index for A is not expired at time")
				return
			}
		}
	})

	t.Run("SearchRecordStorage. TTL increment", func(t *testing.T) {
		fakeClock := clockwork.NewFakeClock()
		testCache := NewSearchRecordStorageWithClock(
			time.Second*10, fakeClock, appMetrics, &Config{StorageExpiration: time.Millisecond}, logger)
		ctx, ctxCancel := context.WithCancel(context.Background())
		testCache.Run(ctx)
		defer ctxCancel()
		fakeClock.BlockUntil(1)

		searchCacheRecordA.CreatedAt = fakeClock.Now().Unix()
		testCache.Set(testKeyA, searchCacheRecordA)

		fakeClock.Advance(time.Second * 5)
		time.Sleep(waitDuration)
		_, okA := testCache.Get(testKeyA)
		if !okA {
			t.Errorf("Record is expired too early")
			return
		}
		for _, rideID := range rideIDsA {
			if !checkRequiredKeys(t, []SearchKey{testKeyA}, testCache, rideID) {
				t.Errorf("rideID index is expired too early")
				return
			}
		}

		searchCacheRecordB.CreatedAt += 60
		testCache.Set(testKeyA, searchCacheRecordB)

		fakeClock.Advance(time.Duration(10) * time.Second)
		time.Sleep(waitDuration)
		_, okA = testCache.Get(testKeyA)
		if !okA {
			t.Errorf("Record is expired too early")
			return
		}
		for _, rideID := range rideIDsB {
			if !checkRequiredKeys(t, []SearchKey{testKeyA}, testCache, rideID) {
				t.Errorf("rideID index is expired too00 early")
				return
			}
		}

		fakeClock.Advance(time.Minute)
		time.Sleep(waitDuration)
		_, okA = testCache.Get(testKeyA)
		if okA {
			t.Errorf("Record is not expired at the time")
			return
		}
		for _, rideID := range rideIDsA {
			if !checkRequiredKeys(t, []SearchKey{}, testCache, rideID) {
				t.Errorf("rideID index for A is not expired at the time")
				return
			}
		}
	})

	t.Run("SearchRecordStorage. TTL decrement", func(t *testing.T) {
		var okA, okB bool

		fakeClock := clockwork.NewFakeClock()
		testCache := NewSearchRecordStorageWithClock(
			time.Duration(10)*time.Second, fakeClock, appMetrics, &Config{StorageExpiration: time.Millisecond}, logger)
		ctx, ctxCancel := context.WithCancel(context.Background())
		testCache.Run(ctx)
		defer ctxCancel()
		fakeClock.BlockUntil(1)

		// expiringQueue: testKeyA
		searchCacheRecordA.CreatedAt = fakeClock.Now().Unix()
		testCache.Set(testKeyA, searchCacheRecordA)

		// expiringQueue: testKeyA, testKeyB
		fakeClock.Advance(time.Second)
		searchCacheRecordB.CreatedAt = fakeClock.Now().Unix()
		testCache.Set(testKeyB, searchCacheRecordB)

		// expiringQueue: testKeyB, testKeyA
		fakeClock.Advance(time.Second)
		searchCacheRecordA.CreatedAt = fakeClock.Now().Unix()
		testCache.Set(testKeyA, searchCacheRecordA)

		// expiringQueue: testKeyA, testKeyB
		fakeClock.Advance(time.Second)
		searchCacheRecordB.CreatedAt = fakeClock.Now().Unix()
		testCache.Set(testKeyB, searchCacheRecordB)

		fakeClock.Advance(time.Minute)
		fakeClock.BlockUntil(1)

		time.Sleep(waitDuration)
		_, okA = testCache.Get(testKeyA)
		_, okB = testCache.Get(testKeyB)
		if okA || okB {
			t.Errorf("Some record is not expired: %v %v", okA, okB)
			return
		}
		for _, rideID := range rideIDsA {
			if !checkRequiredKeys(t, []SearchKey{}, testCache, rideID) {
				t.Errorf("rideID index for A is not expired at the time")
				return
			}
		}
		for _, rideID := range rideIDsB {
			if !checkRequiredKeys(t, []SearchKey{}, testCache, rideID) {
				t.Errorf("rideID index for B is not expired at the time")
				return
			}
		}
	})
}

func TestRideIDIndex(t *testing.T) {
	searchCacheRecordA := newSearchCacheRecord(10000)
	searchCacheRecordB := newSearchCacheRecord(20000)
	rideIDsA := make([]string, len(searchCacheRecordA.Rides))
	for i, ride := range searchCacheRecordA.Rides {
		rideIDsA[i] = ride.Id
	}
	rideIDsB := make([]string, len(searchCacheRecordB.Rides))
	for i, ride := range searchCacheRecordB.Rides {
		rideIDsB[i] = ride.Id
	}

	testKeyA := newSearchCacheRecordKey()
	testKeyA.DateDay = 1
	testKeyB := newSearchCacheRecordKey()
	testKeyB.DateDay = 2

	t.Run("SearchRecordStorage. Multiple keys for RideIdIndex", func(t *testing.T) {
		fakeClock := clockwork.NewFakeClock()
		testCache := NewSearchRecordStorageWithClock(
			time.Second, fakeClock, appMetrics, &Config{StorageExpiration: time.Millisecond}, logger)
		ctx, ctxCancel := context.WithCancel(context.Background())
		testCache.Run(ctx)
		defer ctxCancel()

		testCache.Set(testKeyA, searchCacheRecordA)
		testCache.Set(testKeyB, searchCacheRecordA)
		for _, rideID := range rideIDsA {
			if !checkRequiredKeys(t, []SearchKey{testKeyA, testKeyB}, testCache, rideID) {
				t.Errorf("Multiple keys fails")
				return
			}
		}
		editedRecordA := proto.Clone(searchCacheRecordA).(*ipb.TSearchCacheRecord)
		editedRecordA.Rides[0].Price.Amount++
		testCache.Set(testKeyA, editedRecordA)
		for _, rideID := range rideIDsA {
			if !checkRequiredKeys(t, []SearchKey{testKeyA}, testCache, rideID) {
				t.Errorf("Search invalidation fails")
				return
			}
		}
	})
}

func TestCheckInterruptIterationByCache(t *testing.T) {
	searchCacheRecordA := newSearchCacheRecord(100)
	searchCacheRecordB := newSearchCacheRecord(200)

	testKeyA := newSearchCacheRecordKey()
	testKeyB := newSearchCacheRecordKey()

	testCache := NewSearchRecordStorage(
		time.Second, appMetrics, &Config{StorageExpiration: time.Millisecond}, logger)

	testCache.Set(testKeyA, searchCacheRecordA)
	testCache.Set(testKeyB, searchCacheRecordB)

	t.Run("InterruptIterationByCache. Full iteration case", func(t *testing.T) {
		var items []proto.Message
		for item := range testCache.Iter(context.Background()) {
			items = append(items, item)
		}
		assert.Len(t, items, 2)
	})
	t.Run("InterruptIterationByCache. Interruption case", func(t *testing.T) {
		ctx, cancel := context.WithCancel(context.Background())
		cancel()
		var items []proto.Message

		itemsChannel := testCache.Iter(ctx)
		time.Sleep(20 * time.Millisecond)
		for item := range itemsChannel {
			items = append(items, item)
		}
		assert.Len(t, items, 0, "Length of items should be equal %d, but equal %d", 0, len(items))
		goleak.VerifyNone(t)
	})
}

// check work with concurrent setting many values
func BenchmarkSearchRecordStorageSet(b *testing.B) {
	rand.Seed(time.Now().UnixNano())
	fmt.Printf("BenchmarkSearchRecordStorageSet\tb.N=%d\n", b.N)

	if searchCacheRecords == nil || searchCacheRecordsKeys == nil {
		searchCacheRecords, searchCacheRecordsKeys = newSearchCacheRecordSlice(itemsNumber)
	}

	testCache := NewSearchRecordStorage(time.Duration(math.MaxInt64), appMetrics, &DefaultConfig, logger)
	ctx, ctxCancel := context.WithCancel(context.Background())
	testCache.Run(ctx)
	defer ctxCancel()

	b.ReportAllocs()
	b.SetBytes(int64(b.N))
	b.ResetTimer()
	fmt.Println("BenchmarkSearchRecordStorageSet. Prepare complete.")

	b.RunParallel(func(pb *testing.PB) {
		i := 0
		for pb.Next() {
			testCache.Set(searchCacheRecordsKeys[i], searchCacheRecords[i])
			i++
			if i >= itemsNumber {
				i = 0
			}
		}
	})
}

// concurrent loading bench
func BenchmarkSearchRecordStorageGet(b *testing.B) {
	rand.Seed(time.Now().UnixNano())
	logger.Infof("BenchmarkSearchRecordStorageGet\tb.N=%d\n", b.N)

	if searchCacheRecords == nil || searchCacheRecordsKeys == nil {
		searchCacheRecords, searchCacheRecordsKeys = newSearchCacheRecordSlice(itemsNumber)
	}

	testCache := NewSearchRecordStorage(time.Duration(math.MaxInt64), appMetrics, &DefaultConfig, logger)
	ctx, ctxCancel := context.WithCancel(context.Background())
	testCache.Run(ctx)
	defer ctxCancel()

	for i := 0; i < itemsNumber; i++ {
		testCache.Set(searchCacheRecordsKeys[i], searchCacheRecords[i])
	}

	b.ReportAllocs()
	b.SetBytes(int64(b.N))
	b.ResetTimer()
	logger.Info("BenchmarkSearchRecordStorageGet. Prepare complete.")

	b.RunParallel(func(pbt *testing.PB) {
		i := 0
		for pbt.Next() {
			oneOfSearchCacheRecord, ok := testCache.Get(searchCacheRecordsKeys[i])
			if !ok {
				panic(fmt.Errorf("ERROR loading value with key %v from keys array at index %d", searchCacheRecordsKeys[i], i))
			}
			if oneOfSearchCacheRecord.Status != ipb.ECacheRecordStatus_CACHE_RECORD_STATUS_OK {
				panic(fmt.Errorf("ERROR. Unexpected zero SearchCacheRecord with key %v from keys array at index %d", searchCacheRecordsKeys[i], i))
			}
			i++
			if i >= itemsNumber {
				i = 0
			}
		}
	})
}

// concurrent GET/SET
func BenchmarkSearchRecordStorageGetSet(b *testing.B) {
	rand.Seed(time.Now().UnixNano())
	fmt.Printf("BenchmarkSearchRecordStorageGetSet\tb.N=%d\n", b.N)

	if searchCacheRecords == nil || searchCacheRecordsKeys == nil {
		searchCacheRecords, searchCacheRecordsKeys = newSearchCacheRecordSlice(itemsNumber)
	}

	testCache := NewSearchRecordStorage(time.Duration(math.MaxInt64), appMetrics, &DefaultConfig, logger)
	ctx, ctxCancel := context.WithCancel(context.Background())
	testCache.Run(ctx)
	defer ctxCancel()

	for i := 0; i < itemsNumber; i++ {
		testCache.Set(searchCacheRecordsKeys[i], searchCacheRecords[i])
	}

	b.ReportAllocs()
	b.SetBytes(int64(b.N))
	b.ResetTimer()
	fmt.Println("BenchmarkSearchRecordStorageGetSet. Prepare complete.")

	b.RunParallel(func(pbt *testing.PB) {
		i := 0
		for pbt.Next() {
			if i%5 == 0 { // do Set sometimes
				testCache.Set(searchCacheRecordsKeys[i], searchCacheRecords[i])
			} else { // in other times - do Get
				oneOfSearchCacheRecord, ok := testCache.Get(searchCacheRecordsKeys[i])
				if !ok {
					panic(fmt.Errorf("ERROR loading value with key %v from keys array at index %d", searchCacheRecordsKeys[i], i))
				}
				if oneOfSearchCacheRecord.Status != ipb.ECacheRecordStatus_CACHE_RECORD_STATUS_OK {
					panic(fmt.Errorf("ERROR. Unexpected zero SearchCacheRecord with key %v from keys array at index %d", searchCacheRecordsKeys[i], i))
				}
			}
			i++
			if i >= itemsNumber {
				i = 0
			}
		}
	})
}
