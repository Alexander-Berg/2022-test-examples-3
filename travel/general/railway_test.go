package railway

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/proto/dicts/rasp"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/consts"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/dict/factories"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/dict/registry"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/helpers"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/points"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/testutils"
)

const YekaterinburgTZCode = "Asia/Yekaterinburg"

var YekaterinburgLocation = helpers.MustLoadLocation(YekaterinburgTZCode)

func TestMskDefault(t *testing.T) {
	logger := testutils.NewLogger(t)
	repos := registry.NewRepositoryRegistry(logger)
	station := factories.NewStationFactory(repos).Create()

	actual := GetLocationByPoint(points.NewStation(station), repos)
	assert.Equal(t, consts.DefaultLocation, actual)
}

func TestStationTimeZone(t *testing.T) {
	logger := testutils.NewLogger(t)
	repos := registry.NewRepositoryRegistry(logger)
	station := factories.NewStationFactory(repos).WithRailwayLocation(time.UTC).Create()

	actual := GetLocationByPoint(points.NewStation(station), repos)
	assert.Equal(t, time.UTC, actual)
}

func TestCapitalTimeZone(t *testing.T) {
	logger := testutils.NewLogger(t)
	repos := registry.NewRepositoryRegistry(logger)

	country := factories.NewCountryFactory(repos).Create()
	factories.NewSettlementFactory(repos). // capital
						WithLocation(YekaterinburgLocation).
						WithCountry(country).
						Create()

	settlement := factories.NewSettlementFactory(repos).
		WithLocation(time.UTC).
		WithMajority(rasp.TSettlement_MAJORITY_REGION_CAPITAL).
		WithCountry(country).
		Create()

	actual := GetLocationByPoint(points.NewSettlement(settlement), repos)
	assert.Equal(t, YekaterinburgLocation, actual, "%s != %s", YekaterinburgLocation.String(), actual.String())
}
