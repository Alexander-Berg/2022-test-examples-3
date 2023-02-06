package express

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/travel/proto/dicts/rasp"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/dict/factories"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/dict/registry"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/points"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/testutils"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/traincity"
)

func TestFindExpressIDForStation(t *testing.T) {
	preparedCase := prepareTestCase(t)
	point := points.NewStation(preparedCase.station)
	assert.Equal(t, 123, preparedCase.repository.FindExpressID(point))
}

func TestFindExpressIDForSettlement(t *testing.T) {
	preparedCase := prepareTestCase(t)
	point := points.NewSettlement(preparedCase.settlement)
	assert.Equal(t, 123, preparedCase.repository.FindExpressID(point))
}

type testCase struct {
	repository *Repository
	station    *rasp.TStation
	settlement *rasp.TSettlement
}

func prepareTestCase(t *testing.T) (res testCase) {

	logger := testutils.NewLogger(t)
	repos := registry.NewRepositoryRegistry(logger)

	res.settlement = factories.NewSettlementFactory(repos).Create()
	res.station = factories.NewStationFactory(repos).
		WithSettlement(res.settlement).
		WithMajority(rasp.TStation_MAJORITY_EXPRESS_FAKE).
		Create()
	factories.NewStationCodeFactory(repos).WithStation(res.station).WithCode("123").Create()

	trainCityRepository := traincity.NewRepository(logger, repos)
	require.NoError(t, trainCityRepository.UpdateCache())

	res.repository = NewRepository(logger, repos, trainCityRepository)
	require.NoError(t, res.repository.UpdateCache())
	return
}
