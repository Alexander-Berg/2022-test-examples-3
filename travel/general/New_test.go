package carriercache

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/avia/shared_flights/status_importer/internal/objects/model"
)

type mockCarrierProvider struct {
}

func (m mockCarrierProvider) All() (chan *model.Carrier, error) {
	ch := make(chan *model.Carrier)
	go func() {
		defer close(ch)
		for _, carrier := range []model.Carrier{
			{
				ID:       1,
				Iata:     "SU",
				Sirena:   "",
				Icao:     "",
				IcaoRU:   "",
				Priority: 0,
			},
			{
				ID:       2,
				Iata:     "SU",
				Sirena:   "",
				Icao:     "",
				IcaoRU:   "",
				Priority: 10,
			},
			{
				ID:       3,
				Iata:     "DP",
				Sirena:   "",
				Icao:     "",
				IcaoRU:   "",
				Priority: 0,
			},
			{
				ID:       4,
				Iata:     "",
				Sirena:   "DP",
				Icao:     "",
				IcaoRU:   "",
				Priority: 10,
			},
			{
				ID:       5,
				Iata:     "KK",
				Sirena:   "KK",
				Icao:     "KK",
				IcaoRU:   "KK",
				Priority: 0,
			},
		} {
			carrierCopy := carrier
			ch <- &carrierCopy
		}
	}()
	return ch, nil
}

func TestNew(t *testing.T) {

	var provider mockCarrierProvider
	cache := New(&Config{
		CarrierProvider: provider,
		UpdateInterval:  1 * time.Hour,
	})

	// Sorted in a right order
	assert.Equal(t, []model.Carrier{
		{
			ID:       2,
			Iata:     "SU",
			Sirena:   "",
			Icao:     "",
			IcaoRU:   "",
			Priority: 10,
		},
		{
			ID:       1,
			Iata:     "SU",
			Sirena:   "",
			Icao:     "",
			IcaoRU:   "",
			Priority: 0,
		},
	}, cache.ByCode("SU"), "should be sorted by priority")

	// Even if from different code systems
	assert.Equal(t, []model.Carrier{
		{
			ID:       4,
			Iata:     "",
			Sirena:   "DP",
			Icao:     "",
			IcaoRU:   "",
			Priority: 10,
		},
		{
			ID:       3,
			Iata:     "DP",
			Sirena:   "",
			Icao:     "",
			IcaoRU:   "",
			Priority: 0,
		},
	}, cache.ByCode("DP"), "should be sorted by priority even though code systems differ")

	// Even if from different code systems
	assert.Equal(t, []model.Carrier{
		{
			ID:       5,
			Iata:     "KK",
			Sirena:   "KK",
			Icao:     "KK",
			IcaoRU:   "KK",
			Priority: 0,
		},
	}, cache.ByCode("KK"), "there should be no duplicates even though there are several codes in different code systems")

	assert.Nil(t, cache.ByCode("ABC"), "unknown code should return nil")

	assert.Equal(t, model.Carrier{
		ID:       5,
		Iata:     "KK",
		Sirena:   "KK",
		Icao:     "KK",
		IcaoRU:   "KK",
		Priority: 0,
	}, cache.ByID(5))

	assert.Equal(t, model.Carrier{}, cache.ByID(-1), "should return zero carrier")

}
