package trips

import (
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	apimodels "a.yandex-team.ru/travel/komod/trips/internal/components/api/trips/models"
)

func Test_Integration(t *testing.T) {
	tests := []string{
		"past_",
		"past_test",
		"active_test",
		"active_",
	}
	for _, value := range tests {
		t.Run(fmt.Sprintf("test for value=%s", value), func(t *testing.T) {
			token, err := LoadToken(value)
			require.NoError(t, err)
			require.Equal(t, value, DumpToken(*token))
		})
	}
}

func TestGenerateStartToken(t *testing.T) {
	assert.Equal(t, GenerateStartToken(apimodels.ActiveTrips), &apimodels.ContinuationToken{
		TripsType:  apimodels.ActiveTrips,
		NextTripID: nil,
	})
	assert.Equal(t, GenerateStartToken(apimodels.PastTrips), &apimodels.ContinuationToken{
		TripsType:  apimodels.PastTrips,
		NextTripID: nil,
	})
}
