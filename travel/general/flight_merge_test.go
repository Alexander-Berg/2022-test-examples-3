package flight

import (
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/avia/shared_flights/api/pkg/structs"
)

func TestFlightMerge_AddNormalFlightMergeRule(t *testing.T) {
	s := NewFlightMergeRuleStorage()

	rule := structs.FlightMergeRule{
		ID:                    1,
		OperatingCarrier:      26,
		OperatingFlightRegexp: "1.+",
		MarketingCarrier:      30,
		MarketingFlightRegexp: "272",
		ShouldMerge:           true,
		IsActive:              true,
	}

	assert.Equal(t, 0, s.RulesCount())
	assert.True(t, s.AddRule(rule), "unable to add flight-merge rule")
	assert.Equal(t, 1, s.RulesCount())
}

func TestFlightMerge_AddDisabledFlightMergeRule(t *testing.T) {
	s := NewFlightMergeRuleStorage()

	rule := structs.FlightMergeRule{
		ID:       1,
		IsActive: false,
	}

	assert.False(t, s.AddRule(rule), "unexpectedly added disabled flight-merge rule")
	assert.Equal(t, 0, s.RulesCount())
}

func TestFlightMerge_DoesFlightMergeRuleApply(t *testing.T) {
	s := NewFlightMergeRuleStorage()
	s.UseLesserFlightNumberAsOperating(false)

	rule := structs.FlightMergeRule{
		ID:                    1,
		OperatingCarrier:      26,
		OperatingFlightRegexp: "1.+",
		MarketingCarrier:      30,
		MarketingFlightRegexp: "272",
		ShouldMerge:           true,
		IsActive:              true,
	}
	s.AddRule(rule)

	assert.True(t, s.ShouldMerge(26, "151", 30, "272"), "rule does not apply when it should")
	assert.False(t, s.ShouldMerge(27, "151", 30, "272"), "rule applies when operating carrier does not match")
	assert.False(t, s.ShouldMerge(27, "251", 30, "272"), "rule applies when operating flight does not match")
	assert.False(t, s.ShouldMerge(26, "151", 31, "272"), "rule applies when marketing carrier does not match")
	assert.False(t, s.ShouldMerge(26, "151", 30, "273"), "rule applies when marketing flight does not match")
}

func TestFlightMerge_EmptyFlightMergeRuleRegexp(t *testing.T) {
	s := NewFlightMergeRuleStorage()
	s.UseLesserFlightNumberAsOperating(false)

	rule := structs.FlightMergeRule{
		ID:                    1,
		OperatingCarrier:      26,
		OperatingFlightRegexp: "1.+",
		MarketingCarrier:      30,
		ShouldMerge:           true,
		IsActive:              true,
	}
	s.AddRule(rule)

	assert.True(t, s.ShouldMerge(26, "151", 30, "555"), "empty regexp should match any flight number")
}

func TestFlightMerge_ExcludedCarriers(t *testing.T) {
	s := NewFlightMergeRuleStorage()

	rule := structs.FlightMergeRule{
		ID:              1,
		ExcludedCarrier: 26,
		IsActive:        true,
	}
	s.AddRule(rule)

	assert.False(t, s.ShouldMerge(26, "151", 30, "555"), "excluded carrier flight should not be merged")
}

func TestFlightMerge_LesserFlightNumber(t *testing.T) {
	s := NewFlightMergeRuleStorage()

	assert.True(t, s.ShouldMerge(26, "151", 30, "555"), "lesser operating flight number should be merged")
	assert.False(t, s.ShouldMerge(26, "555", 30, "151"), "bigger operating flight number should not be merged")

	// now exclude carrier 30 from merging
	rule := structs.FlightMergeRule{
		ID:               1,
		OperatingCarrier: 26,
		MarketingCarrier: 30,
		ShouldMerge:      false,
		IsActive:         true,
	}
	s.AddRule(rule)

	assert.False(t, s.ShouldMerge(26, "151", 30, "555"), "expressly prohibited merging should not occur")
}

func TestFlightMerge_NeverMergeSameCarrier(t *testing.T) {
	s := NewFlightMergeRuleStorage()

	assert.False(t, s.ShouldMerge(26, "151", 26, "555"), "same carrier merges shall not be allowed")

	// now exclude carrier 30 from merging
	rule := structs.FlightMergeRule{
		ID:               1,
		OperatingCarrier: 26,
		MarketingCarrier: 26,
		ShouldMerge:      true,
		IsActive:         true,
	}
	assert.False(t, s.AddRule(rule))
	assert.False(t, s.ShouldMerge(26, "151", 26, "555"), "same carrier merges shall not be allowed, no matter what")
}
