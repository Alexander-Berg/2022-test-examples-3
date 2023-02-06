package extractors

import (
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/travel/proto/dicts/rasp"
)

func TestStationIDToSettlementIDMapper(t *testing.T) {
	newMapper := func(
		stationsRepository StationsRepository,
		stationToSettlementRepository StationToSettlementRepository,
	) *StationIDToSettlementIDMapper {
		return NewStationIDToSettlementIDMapper(stationsRepository, stationToSettlementRepository)
	}

	t.Run(
		"Map/should give priority to stationToSettlement repository", func(t *testing.T) {
			stationsRepository := &fakeStationsRepository{
				byID: map[int]*rasp.TStation{
					1: {SettlementId: 2},
				},
			}
			stationToSettlementRepository := &fakeStationToSettlementRepository{
				byStationID: map[int]*rasp.TStation2Settlement{
					1: {StationId: 1, SettlementId: 3},
				},
			}
			mapper := newMapper(stationsRepository, stationToSettlementRepository)

			settlementID, found := mapper.Map(1)

			require.True(t, found)
			require.EqualValues(t, 3, settlementID)
		},
	)

	t.Run(
		"Map/no stationToSettlement should return from stations repository", func(t *testing.T) {
			stationsRepository := &fakeStationsRepository{
				byID: map[int]*rasp.TStation{
					1: {SettlementId: 2},
				},
			}
			stationToSettlementRepository := &fakeStationToSettlementRepository{byStationID: map[int]*rasp.TStation2Settlement{}}
			mapper := newMapper(stationsRepository, stationToSettlementRepository)

			settlementID, found := mapper.Map(1)

			require.True(t, found)
			require.EqualValues(t, 2, settlementID)
		},
	)

	t.Run(
		"Map/unknown stationID returns false", func(t *testing.T) {
			stationsRepository := &fakeStationsRepository{byID: map[int]*rasp.TStation{}}
			stationToSettlementRepository := &fakeStationToSettlementRepository{byStationID: map[int]*rasp.TStation2Settlement{}}
			mapper := newMapper(stationsRepository, stationToSettlementRepository)

			_, found := mapper.Map(1)

			require.False(t, found)
		},
	)
}

type fakeStationsRepository struct {
	byID map[int]*rasp.TStation
}

func (f *fakeStationsRepository) Get(id int) (*rasp.TStation, bool) {
	station, ok := f.byID[id]
	return station, ok
}

type fakeStationToSettlementRepository struct {
	byStationID map[int]*rasp.TStation2Settlement
}

func (f *fakeStationToSettlementRepository) Get(id int) (*rasp.TStation2Settlement, bool) {
	stationToSettlement, ok := f.byStationID[id]
	return stationToSettlement, ok
}
