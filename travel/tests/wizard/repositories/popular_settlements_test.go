package repositories

import (
	"io"
	"testing"

	"github.com/golang/protobuf/proto"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/travel/avia/wizard/pkg/wizard/caches/references"
	"a.yandex-team.ru/travel/avia/wizard/pkg/wizard/repositories"
	dictsWizard "a.yandex-team.ru/travel/proto/avia/wizard"
	dicts "a.yandex-team.ru/travel/proto/dicts/avia"
)

func TestPrecache_PopularByCountry(t *testing.T) {
	repository := prepare(t)

	settlement, ok := repository.GetPopularByCountry("ru", 1)

	require.True(t, ok)
	require.Equal(t, settlement.ID, 3)
}

func TestPrecache_PopularByCountry_NoNational(t *testing.T) {
	cache := prepare(t)

	_, ok := cache.GetPopularByCountry("no_ru", 1)

	require.False(t, ok)
}

func TestPrecache_PopularByRegion(t *testing.T) {
	cache := prepare(t)

	settlement, ok := cache.GetPopularByRegion("ru", 2)

	require.True(t, ok)
	require.Equal(t, settlement.ID, 2)
}

func TestPrecache_PopularByRegion_NoNational(t *testing.T) {
	cache := prepare(t)

	_, ok := cache.GetPopularByRegion("no_ru", 2)

	require.False(t, ok)
}

func prepare(t *testing.T) *repositories.PopularSettlementsRepository {
	s := []proto.Message{
		getSettlement(1, 1, 2),
		getSettlement(2, 1, 2),
		getSettlement(3, 1, 3),
		getSettlement(4, 2, 4),
	}

	sn := []proto.Message{
		getSettlementNational(1, 3, "ru"),
		getSettlementNational(2, 4, "ru"),
		getSettlementNational(3, 5, "ru"),
		getSettlementNational(4, 6, "ru"),
	}

	return getPopularSettlementsRepository(t, s, sn)
}

func getPopularSettlementsRepository(
	t *testing.T,
	settlement []proto.Message,
	settlementNationals []proto.Message,
) *repositories.PopularSettlementsRepository {
	settlements := references.NewSettlements()
	cacheReference(t, settlements, settlement)

	settlementPopularities := references.NewSettlementPopularities()
	cacheReference(t, settlementPopularities, settlementNationals)

	return repositories.NewPopularSettlementsRepository(settlementPopularities, settlements)
}

func cacheReference(
	t *testing.T,
	reference io.Writer,
	data []proto.Message,
) {
	for _, message := range data {
		b, err := proto.Marshal(message)
		require.NoError(t, err)

		_, err = reference.Write(b)
		require.NoError(t, err)
	}
}

func getSettlementNational(settlementID, Popularity int, nationalVersion string) *dictsWizard.TSettlementNational {
	return &dictsWizard.TSettlementNational{
		SettlementID:    int32(settlementID),
		Popularity:      int32(Popularity),
		NationalVersion: nationalVersion,
		Arrival:         true,
	}
}

func getSettlement(id, countryID, regionID int) *dicts.TSettlement {
	return &dicts.TSettlement{
		Id:        int64(id),
		CountryId: int32(countryID),
		RegionId:  int32(regionID),
	}
}
