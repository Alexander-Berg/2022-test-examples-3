package flightsdelayed

import (
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/flights_delayed/format"
	storageCache "a.yandex-team.ru/travel/avia/shared_flights/api/internal/storage"
)

func TestStorageService_GetEmptyDelayedFlights(t *testing.T) {
	storage := storageCache.NewStorage()
	service := NewDelayedFlightsService(storage)
	response := service.GetDelayedFlights(
		[]int64{100, 101},
		true,
		false,
	)
	expected := format.Response{
		Stations: []format.StationElem{
			{
				StationID:             100,
				CancelledFlightsCount: 0,
				DelayedFlightsCount:   0,
			},
			{
				StationID:             101,
				CancelledFlightsCount: 0,
				DelayedFlightsCount:   0,
			},
		},
	}
	assert.Equal(t, expected, response)
}
