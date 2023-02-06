package schedule

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/library/go/test/assertpb"
	"a.yandex-team.ru/travel/proto/dicts/rasp"
	dictfactories "a.yandex-team.ru/travel/trains/search_api/internal/pkg/dict/factories"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/dict/registry"
	schedulefactories "a.yandex-team.ru/travel/trains/search_api/internal/pkg/schedule/factories"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/testutils"
)

func TestThreadCaching(t *testing.T) {
	testCase := prepareCaseForCachingTests(t)

	threads := testCase.cache.getEnabledThreads()
	assert.Len(t, threads, 1)
	assertpb.Equal(t, testCase.thread, threads[0])

	stations := testCase.cache.getThreadStations(testCase.thread.Id)
	assert.Len(t, stations, 3)
}

func TestStationStopsCaching(t *testing.T) {
	testCase := prepareCaseForCachingTests(t)

	stops := testCase.cache.stationStops
	assert.Len(t, stops, 3)
	assert.Contains(t, stops, int(testCase.departureStation.Id))
	assert.Contains(t, stops, int(testCase.arrivalStation.Id))
}

func TestSettlementStopsCaching(t *testing.T) {
	testCase := prepareCaseForCachingTests(t)

	stops := testCase.cache.settlementStops
	assert.Len(t, stops, 3)
	assert.Contains(t, stops, int(testCase.departureStation.SettlementId))
	assert.Contains(t, stops, int(testCase.arrivalStation.SettlementId))
}

type caseForCachingTests struct {
	cache            *Cache
	repoRegistry     *registry.RepositoryRegistry
	departureStation *rasp.TStation
	arrivalStation   *rasp.TStation
	thread           *rasp.TThread
}

func prepareCaseForCachingTests(t *testing.T) caseForCachingTests {
	logger := testutils.NewLogger(t)

	var testCase caseForCachingTests
	testCase.repoRegistry = registry.NewRepositoryRegistry(logger)
	testCase.departureStation = dictfactories.NewStationFactory(testCase.repoRegistry).Create()
	middleStation := dictfactories.NewStationFactory(testCase.repoRegistry).Create()
	testCase.arrivalStation = dictfactories.NewStationFactory(testCase.repoRegistry).Create()
	testCase.thread = schedulefactories.NewScheduleFactory(testCase.repoRegistry).
		AddStop(testCase.departureStation, time.Hour).
		AddStop(middleStation, time.Hour).
		AddStop(testCase.arrivalStation, time.Hour).
		Create()
	testCase.cache = NewCache(logger, testCase.repoRegistry).Build()
	return testCase
}
