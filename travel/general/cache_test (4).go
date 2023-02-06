package traincity

import (
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/proto/dicts/rasp"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/dict/factories"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/dict/registry"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/testutils"
)

func TestFakeExpressStation(t *testing.T) {
	logger := testutils.NewLogger(t)
	repos := registry.NewRepositoryRegistry(logger)

	settlement := factories.NewSettlementFactory(repos).Create()
	fakeStation := factories.NewStationFactory(repos).
		WithSettlement(settlement).
		WithMajority(rasp.TStation_MAJORITY_EXPRESS_FAKE).
		Create()
	factories.NewStationCodeFactory(repos).WithStation(fakeStation).Create()

	cache := NewCache(logger, repos).Build()
	assert.Equal(t, fakeStation, cache.Get(settlement.Id))
}

func TestExpressWithPreferredMajority(t *testing.T) {
	logger := testutils.NewLogger(t)
	repos := registry.NewRepositoryRegistry(logger)

	settlement := factories.NewSettlementFactory(repos).Create()

	for _, m := range []rasp.TStation_EMajority{
		rasp.TStation_MAJORITY_STATION,
		rasp.TStation_MAJORITY_IN_TABLO,
		rasp.TStation_MAJORITY_NOT_IN_TABLO,
	} {
		station := factories.NewStationFactory(repos).
			WithSettlement(settlement).
			WithMajority(m).
			Create()
		factories.NewStationCodeFactory(repos).WithStation(station).Create()
	}

	cache := NewCache(logger, repos).Build()
	assert.NotNil(t, cache.Get(settlement.Id))
	assert.Equal(t, rasp.TStation_MAJORITY_IN_TABLO, cache.Get(settlement.Id).Majority)
}
