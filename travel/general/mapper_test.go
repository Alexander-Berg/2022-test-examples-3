package http

import (
	"testing"

	"github.com/stretchr/testify/assert"

	tripsapi "a.yandex-team.ru/travel/komod/trips/api/trips/v1"
)

func TestMapTripState_AllValuesHandled(t *testing.T) {
	for name, value := range tripsapi.TripState_value {
		state := tripsapi.TripState(value)
		assert.NotPanics(t, func() {
			MapTripState(state)
		}, "unexpected trip state %s", name)
	}
}
