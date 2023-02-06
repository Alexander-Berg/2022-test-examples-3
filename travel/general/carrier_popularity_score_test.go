package popularityscores

import (
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/avia/shared_flights/api/pkg/structs"
)

func TestCarriersPopularityScores(t *testing.T) {
	scores := NewCarriersPopularityScores()
	scores.SetScore(1, "ru", 14)

	assert.Equal(t, int32(14), scores.GetScore(1, "ru"))
	assert.Equal(t, int32(0), scores.GetScore(1, ""))
	assert.Equal(t, int32(0), scores.GetScore(2, "ru"))
}

func TestDefaultCarriersPopularityScores(t *testing.T) {
	scores := NewCarriersPopularityScores()
	scores.SetScore(1, "ru", 19)
	scores.SetScore(1, "", 29)

	assert.Equal(t, int32(29), scores.GetScore(1, "kz"))
}

func TestGetCarriers(t *testing.T) {
	scores := NewCarriersPopularityScores()
	scores.SetScore(1, "ru", 19)
	scores.SetScore(1, "", 29)
	scores.SetScore(2, "", 24)

	err := scores.UpdateFlightNumbersCache(newMockFlightPatternsProvider())
	assert.NoError(t, err)

	// Unknown flight number
	assert.Equal(t, []int32{}, scores.GetCarriers("flight33", ""))

	// Default national version: carrier1 has higher priority (29) than carrier2 (24)
	assert.Equal(t, []int32{1, 2}, scores.GetCarriers("5", ""))

	// "ru" national version: carrier2 has higher priority (24) than carrier1 (19)
	assert.Equal(t, []int32{2, 1}, scores.GetCarriers("5", "ru"))
}

func TestUpdatingCacheTwiceDoesNotChangeAnything(t *testing.T) {
	scores := NewCarriersPopularityScores()
	scores.SetScore(1, "", 29)
	scores.SetScore(2, "", 24)

	// Update cache for the first time
	err := scores.UpdateFlightNumbersCache(newMockFlightPatternsProvider())
	assert.NoError(t, err)
	assert.Equal(t, []int32{1, 2}, scores.GetCarriers("5", ""))

	// Update cache for the second time, make sure nothing has been changed
	err = scores.UpdateFlightNumbersCache(newMockFlightPatternsProvider())
	assert.NoError(t, err)
	assert.Equal(t, []int32{1, 2}, scores.GetCarriers("5", ""))
}

type mockFlightPatternsProvider struct{}

func newMockFlightPatternsProvider() FlightPatternsProvider {
	return &mockFlightPatternsProvider{}
}

func (p mockFlightPatternsProvider) GetFlightPatterns() map[int32]*structs.FlightPattern {
	result := make(map[int32]*structs.FlightPattern)
	result[101] = &structs.FlightPattern{
		MarketingCarrier:      1,
		MarketingFlightNumber: "5",
	}
	result[102] = &structs.FlightPattern{
		MarketingCarrier:      2,
		MarketingFlightNumber: "5",
	}
	return result
}
