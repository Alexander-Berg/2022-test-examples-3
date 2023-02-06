package regioncapital

import (
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/test/requirepb"
	"a.yandex-team.ru/library/go/yandex/geobase"
	dicts "a.yandex-team.ru/travel/proto/dicts/rasp"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/dict/registry"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/geo"
	"a.yandex-team.ru/travel/trains/search_api/internal/pkg/testutils"
)

func newRegionCapitalRepository(t *testing.T) (*Repository, *geo.FakeGeobaseClient) {
	logger := testutils.NewLogger(t)
	geobaseClient := geo.NewFakeGeobaseClient()
	repoRegistry := registry.NewRepositoryRegistry(logger)
	return NewRepository(logger, geobaseClient, repoRegistry), geobaseClient
}

func TestNotFoundStartRegion(t *testing.T) {
	repository, _ := newRegionCapitalRepository(t)
	settlement := repository.GetRegionCapital(1)
	require.Nil(t, settlement)
}

func TestGettingUnexpectedRegionType(t *testing.T) {
	repository, geobaseClient := newRegionCapitalRepository(t)
	geobaseClient.AddRegions(
		&geobase.Region{ID: 1, Type: geobase.RegionTypeCity},
		&geobase.Region{ID: 2, ParentID: 1, Type: geobase.RegionTypeVillage},
		&geobase.Region{ID: 3, Type: geobase.RegionTypeCity},
		&geobase.Region{ID: 4, ParentID: 3, Type: geobase.RegionTypeRegion},
	)

	settlementRepo := repository.repoProvider.GetSettlementRepo()

	expectedSettlement := &dicts.TSettlement{GeoId: 1}
	settlementRepo.Add(expectedSettlement)
	settlementRepo.Add(&dicts.TSettlement{GeoId: 3})

	settlement := repository.GetRegionCapital(2)
	require.NotNil(t, settlement)
	requirepb.Equal(t, expectedSettlement, settlement)

	settlement = repository.GetRegionCapital(4)
	require.Nil(t, settlement)
}

func TestGetCapital(t *testing.T) {
	repository, geobaseClient := newRegionCapitalRepository(t)
	geobaseClient.AddRegions(
		&geobase.Region{ID: 1, Type: geobase.RegionTypeRegion},
		&geobase.Region{ID: 2, ParentID: 1, Type: geobase.RegionTypeVillage},
	)

	settlementRepo := repository.repoProvider.GetSettlementRepo()

	expectedSettlement := &dicts.TSettlement{RegionId: 1, Majority: dicts.TSettlement_MAJORITY_CAPITAL, IsHidden: false}
	settlementRepo.Add(expectedSettlement)

	settlement := repository.GetRegionCapital(2)
	require.NotNil(t, settlement)
	requirepb.Equal(t, expectedSettlement, settlement)
}

func TestGetMiddleParentInChain(t *testing.T) {
	repository, geobaseClient := newRegionCapitalRepository(t)

	expectedRegion := &geobase.Region{ID: 2, ParentID: 1, Type: geobase.RegionTypeRegion}
	geobaseClient.AddRegions(
		&geobase.Region{ID: 1, Type: geobase.RegionTypeCountry},
		expectedRegion,
		&geobase.Region{ID: 3, ParentID: 2, Type: geobase.RegionTypeVillage},
		&geobase.Region{ID: 4, ParentID: 3, Type: geobase.RegionTypeCityDistrict},
	)

	foundRegion := repository.getParentByType(&geobase.Region{ID: 5, ParentID: 4}, geobase.RegionTypeRegion)
	require.NotNil(t, foundRegion)
	require.Equal(t, expectedRegion, foundRegion)
}

func TestGetCapitalDirectlyForRegion(t *testing.T) {
	repository, geobaseClient := newRegionCapitalRepository(t)

	geobaseClient.AddRegions(&geobase.Region{ID: 1})

	settlementRepo := repository.repoProvider.GetSettlementRepo()

	expectedSettlement := &dicts.TSettlement{Id: 2, RegionId: 1}
	settlementRepo.Add(expectedSettlement)

	foundSettlement := repository.GetRegionCapital(1)
	require.NotNil(t, foundSettlement)
	require.Equal(t, expectedSettlement, foundSettlement)
}
