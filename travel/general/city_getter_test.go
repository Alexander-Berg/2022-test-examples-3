package countrysearch

import (
	"context"
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/library/go/core/log/nop"
	"a.yandex-team.ru/travel/avia/avia_statistics/api/internal/pkg/dicts"
	"a.yandex-team.ru/travel/avia/avia_statistics/api/internal/pkg/repositories"
	"a.yandex-team.ru/travel/avia/avia_statistics/api/internal/pkg/tables"
	"a.yandex-team.ru/travel/proto/dicts/rasp"
)

func NewSettlementRepository(items ...*rasp.TSettlement) *dicts.SettlementRepository {
	repo := dicts.NewSettlementRepository()
	for _, item := range items {
		repo.Add(item)
	}
	return repo
}

func NewStationRepository(items ...*rasp.TStation) *dicts.StationRepository {
	repo := dicts.NewStationRepository()
	for _, item := range items {
		repo.Add(item)
	}
	return repo
}

func NewStationCodesRepositoryByStations(stations ...*rasp.TStation) *dicts.StationCodesRepository {
	repo := dicts.NewStationCodesRepository()
	for _, station := range stations {
		repo.Add(&rasp.TStationCode{
			StationId: station.Id,
			Code:      "some_code",
			SystemId:  rasp.ECodeSystem_CODE_SYSTEM_IATA,
		})
	}
	return repo
}

func TestCityListGetter(t *testing.T) {
	logger := &nop.Logger{}

	someID := "someID"
	settlements := []*rasp.TSettlement{
		{CountryId: 205, Id: 1, Iata: someID},
		{CountryId: 205, Id: 2, SirenaId: someID},
		{CountryId: 205, Id: 3, Iata: someID},
		{CountryId: 205, Id: 5, IsHidden: true, Iata: someID},
		{CountryId: 205, Id: 6, Iata: someID},
		{CountryId: 205, Id: 10, Iata: someID},
	}
	settlementRepository := NewSettlementRepository(settlements...)

	stations := []*rasp.TStation{
		{Id: 123, SettlementId: 1, CountryId: 205, Type: rasp.TStation_TYPE_AIRPORT},
		{Id: 125, SettlementId: 2, CountryId: 205, Type: rasp.TStation_TYPE_AIRPORT},
		{Id: 127, SettlementId: 3, CountryId: 205, Type: rasp.TStation_TYPE_AIRPORT},
		{Id: 131, SettlementId: 10, CountryId: 205, Type: rasp.TStation_TYPE_BUS_STATION},
	}
	stationRepository := NewStationRepository(stations...)

	popularity := repositories.NewSettlementPopularityRepository(logger, func() (
		[]tables.DirectionPopularityEntry,
		error,
	) {
		return []tables.DirectionPopularityEntry{
			{SettlementFromID: 123, SettlementToID: 1, RedirNumber: 10},
			{SettlementFromID: 5, SettlementToID: 2, RedirNumber: 1000},
			{SettlementFromID: 123, SettlementToID: 3, RedirNumber: 3},
		}, nil
	})
	err := popularity.Update()
	assert.NoError(t, err)
	getter := NewCityListGetter(
		stationRepository,
		settlementRepository,
		popularity,
		NewStationCodesRepositoryByStations(stations...),
		logger,
	)
	t.Run("by popularity", func(t *testing.T) {
		actual, ok := getter.GetCities(context.Background(), 205)
		assert.Equal(t, ok, true)
		assert.Equal(t, actual, []*rasp.TSettlement{
			{CountryId: 205, Id: 2, SirenaId: someID},
			{CountryId: 205, Id: 1, Iata: someID},
			{CountryId: 205, Id: 3, Iata: someID},
		})
	})
	t.Run("no country", func(t *testing.T) {
		actual, ok := getter.GetCities(context.Background(), 10)
		assert.Equal(t, ok, false)
		assert.Empty(t, actual)
	})
}
