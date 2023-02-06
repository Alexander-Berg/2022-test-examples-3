package flight

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/storage/station"
	"a.yandex-team.ru/travel/avia/shared_flights/api/pkg/structs"
	"a.yandex-team.ru/travel/proto/dicts/rasp"
	"a.yandex-team.ru/travel/proto/shared_flights/snapshots"
)

func TestAddNormalRule(t *testing.T) {
	s := NewBlacklistRuleStorage(GetTestStations())

	rule := snapshots.TBlacklistRule{
		Id:        1,
		ForceMode: "FORCE_BAN",
	}

	assert.True(t, s.AddRule(&rule), "unable to add blacklist rule")
}

func TestAddDisabledRule(t *testing.T) {
	s := NewBlacklistRuleStorage(GetTestStations())

	rule := snapshots.TBlacklistRule{
		Id:        1,
		ForceMode: "DISABLED",
	}

	assert.False(t, s.AddRule(&rule), "unexpectedly added disabled blacklist rule")
}

func TestAddTooOldRule(t *testing.T) {
	s := NewBlacklistRuleStorage(GetTestStations())

	yesterday := time.Now().UTC().Add(-24 * time.Hour).Format("2006-01-02")
	rule1 := snapshots.TBlacklistRule{
		Id:          1,
		ForceMode:   "AS_SCHEDULED",
		ActiveUntil: yesterday,
	}

	assert.False(t, s.AddRule(&rule1), "unexpectedly added expired blacklist rule")

	rule2 := snapshots.TBlacklistRule{
		Id:          2,
		ForceMode:   "FORCE_BAN",
		ActiveUntil: yesterday,
	}

	assert.True(t, s.AddRule(&rule2), "unexpectedly ignored active blacklist rule")
}

func TestIsRuleActive(t *testing.T) {
	s := NewBlacklistRuleStorage(GetTestStations())
	nowStr := "2020-02-15T09:00"

	ruleForced := snapshots.TBlacklistRule{
		Id:        1,
		ForceMode: "FORCE_BAN",
	}
	ruleInThePast := snapshots.TBlacklistRule{
		Id:          2,
		ForceMode:   "AS_SCHEDULED",
		ActiveUntil: "2020-02-14",
	}
	ruleInTheFuture := snapshots.TBlacklistRule{
		Id:          3,
		ForceMode:   "AS_SCHEDULED",
		ActiveSince: "2020-02-16",
	}
	ruleActive := snapshots.TBlacklistRule{
		Id:          4,
		ForceMode:   "AS_SCHEDULED",
		ActiveSince: "2020-02-14",
		ActiveUntil: "2020-02-16",
	}
	s.AddRule(&ruleForced)

	assert.True(t, s.IsRuleActive(nowStr, &ruleForced), "rule is forced to be active but alas, it is not active")
	assert.False(t, s.IsRuleActive(nowStr, &ruleInThePast), "rule in the past is unexpectedly active")
	assert.False(t, s.IsRuleActive(nowStr, &ruleInTheFuture), "rule in the future is unexpectedly active")
	assert.True(t, s.IsRuleActive(nowStr, &ruleActive), "rule is not active while it should be")
}

func TestIsRuleApplies(t *testing.T) {
	s := NewBlacklistRuleStorage(GetTestStations())

	// By stations
	ruleFromSvx := snapshots.TBlacklistRule{
		StationFromId: 103,
	}
	flightPattern := &structs.FlightPattern{}
	flightBase := structs.FlightBase{
		DepartureStation: 103,
	}
	assert.True(t, s.RuleApplies(flightBase, flightPattern, &ruleFromSvx), "from SVX rule does not apply when it should")
	flightBase.DepartureStation = 102
	assert.False(t, s.RuleApplies(flightBase, flightPattern, &ruleFromSvx), "from SVX rule applies when it shouldn't")

	ruleToSvx := snapshots.TBlacklistRule{
		StationToId: 103,
	}
	flightBase.ArrivalStation = 103
	assert.True(t, s.RuleApplies(flightBase, flightPattern, &ruleToSvx), "to SVX rule does not apply when it should")
	flightBase.ArrivalStation = 102
	assert.False(t, s.RuleApplies(flightBase, flightPattern, &ruleToSvx), "to SVX rule applies when it shouldn't")

	ruleOvbToSvx := snapshots.TBlacklistRule{
		StationFromId: 102,
		StationToId:   103,
	}
	flightBase.DepartureStation = 102
	flightBase.ArrivalStation = 103
	assert.True(t, s.RuleApplies(flightBase, flightPattern, &ruleOvbToSvx), "OVB to SVX rule does not apply when it should")
	flightBase.DepartureStation = 101
	assert.False(t, s.RuleApplies(flightBase, flightPattern, &ruleOvbToSvx), "OVB to SVX rule applies when it shouldn't")
	flightBase.DepartureStation = 102
	flightBase.ArrivalStation = 101
	assert.False(t, s.RuleApplies(flightBase, flightPattern, &ruleOvbToSvx), "OVB to SVX rule applies when it shouldn't")

	// By carrier
	rule := snapshots.TBlacklistRule{
		MarketingCarrierId: 11,
	}
	flightPattern = &structs.FlightPattern{
		MarketingCarrier: 11,
	}
	flightBase = structs.FlightBase{}
	assert.True(t, s.RuleApplies(flightBase, flightPattern, &rule), "By-carrier rule does not apply when it should")
	flightPattern.MarketingCarrier = 12
	assert.False(t, s.RuleApplies(flightBase, flightPattern, &rule), "By-carrier rule applies when it shouldn't")
	flightBase.OperatingCarrier = 11
	assert.True(t, s.RuleApplies(flightBase, flightPattern, &rule), "By-operating-carrier rule does not apply when it should")

	// By flight number
	rule = snapshots.TBlacklistRule{
		MarketingFlightNumber: "5555",
	}
	flightPattern = &structs.FlightPattern{
		MarketingFlightNumber: "5555",
	}
	flightBase = structs.FlightBase{}
	assert.True(t, s.RuleApplies(flightBase, flightPattern, &rule), "By-flight-number rule does not apply when it should")
	flightPattern.MarketingFlightNumber = "7777"
	assert.False(t, s.RuleApplies(flightBase, flightPattern, &rule), "By-flight-number rule applies when it shouldn't")
	// Flight number is a special case: unlike carrier, it does not propagate to the operating flight alone
	flightBase.OperatingFlightNumber = "5555"
	assert.False(t, s.RuleApplies(flightBase, flightPattern, &rule), "By-flight-number rule applies when it shouldn't")

	// By carrier and flight number
	rule = snapshots.TBlacklistRule{
		MarketingCarrierId:    11,
		MarketingFlightNumber: "5555",
	}
	flightPattern = &structs.FlightPattern{
		MarketingCarrier:      11,
		MarketingFlightNumber: "5555",
	}
	flightBase = structs.FlightBase{}
	assert.True(t, s.RuleApplies(flightBase, flightPattern, &rule), "By-flight rule does not apply when it should")
	flightPattern.MarketingFlightNumber = "7777"
	assert.False(t, s.RuleApplies(flightBase, flightPattern, &rule), "By-flight rule applies when it shouldn't")
	flightBase.OperatingCarrier = 11
	flightBase.OperatingFlightNumber = "5555"
	assert.True(t, s.RuleApplies(flightBase, flightPattern, &rule), "By-operating-flight rule does not apply when it should")
	flightBase.OperatingFlightNumber = "7777"
	assert.False(t, s.RuleApplies(flightBase, flightPattern, &rule), "By-operating-flight rule applies when it shouldn't")

	// By settlement
	ruleFromSvx = snapshots.TBlacklistRule{
		StationFromSettlement: 23,
	}
	flightPattern = &structs.FlightPattern{}
	flightBase = structs.FlightBase{
		DepartureStation: 103,
	}
	assert.True(t, s.RuleApplies(flightBase, flightPattern, &ruleFromSvx), "from SVX rule does not apply when it should")
	flightBase.DepartureStation = 102
	assert.False(t, s.RuleApplies(flightBase, flightPattern, &ruleFromSvx), "from SVX rule applies when it shouldn't")

	ruleToSvx = snapshots.TBlacklistRule{
		StationToSettlement: 23,
	}
	flightBase.ArrivalStation = 103
	assert.True(t, s.RuleApplies(flightBase, flightPattern, &ruleToSvx), "to SVX rule does not apply when it should")
	flightBase.ArrivalStation = 102
	assert.False(t, s.RuleApplies(flightBase, flightPattern, &ruleToSvx), "to SVX rule applies when it shouldn't")

	// By country
	ruleFromSvx = snapshots.TBlacklistRule{
		StationFromCountry: 4,
	}
	flightPattern = &structs.FlightPattern{}
	flightBase = structs.FlightBase{
		DepartureStation: 103,
	}
	assert.True(t, s.RuleApplies(flightBase, flightPattern, &ruleFromSvx), "from SVX rule does not apply when it should")
	flightBase.DepartureStation = 102
	assert.False(t, s.RuleApplies(flightBase, flightPattern, &ruleFromSvx), "from SVX rule applies when it shouldn't")

	ruleToSvx = snapshots.TBlacklistRule{
		StationToCountry: 4,
	}
	flightBase.ArrivalStation = 103
	assert.True(t, s.RuleApplies(flightBase, flightPattern, &ruleToSvx), "to SVX rule does not apply when it should")
	flightBase.ArrivalStation = 102
	assert.False(t, s.RuleApplies(flightBase, flightPattern, &ruleToSvx), "to SVX rule applies when it shouldn't")

	// By flight date
	rule = snapshots.TBlacklistRule{
		FlightDateSince: "2020-02-01",
	}
	flightPattern = &structs.FlightPattern{
		OperatingFromDate:  "2020-01-01",
		OperatingUntilDate: "2020-01-31",
	}
	flightBase = structs.FlightBase{}
	assert.False(t, s.RuleApplies(flightBase, flightPattern, &rule), "By-flight-date rule applies when it shouldn't")
	flightPattern.OperatingUntilDate = "2020-02-01"
	assert.True(t, s.RuleApplies(flightBase, flightPattern, &rule), "By-flight-date rule does not apply when it should")

	rule = snapshots.TBlacklistRule{
		FlightDateUntil: "2020-03-01",
	}
	flightPattern = &structs.FlightPattern{
		OperatingFromDate:  "2020-03-02",
		OperatingUntilDate: "2020-03-31",
	}
	assert.False(t, s.RuleApplies(flightBase, flightPattern, &rule), "By-flight-date rule applies when it shouldn't")
	flightPattern.OperatingFromDate = "2020-02-29"
	assert.True(t, s.RuleApplies(flightBase, flightPattern, &rule), "By-flight-date rule does not apply when it should")

	rule = snapshots.TBlacklistRule{
		FlightDateSince: "2020-02-01",
		FlightDateUntil: "2020-03-01",
	}
	flightPattern = &structs.FlightPattern{
		OperatingFromDate:  "2020-02-15",
		OperatingUntilDate: "2020-02-15",
	}
	assert.True(t, s.RuleApplies(flightBase, flightPattern, &rule), "By-flight-date rule does not apply when it should")
	flightPattern = &structs.FlightPattern{
		OperatingFromDate:  "2020-01-15",
		OperatingUntilDate: "2020-04-15",
	}
	assert.True(t, s.RuleApplies(flightBase, flightPattern, &rule), "By-flight-date rule does not apply when it should")

	// By all parameters at once
	rule = snapshots.TBlacklistRule{
		StationFromId:         102,
		StationToId:           103,
		MarketingCarrierId:    11,
		MarketingFlightNumber: "5555",
		FlightDateSince:       "2020-02-01",
		FlightDateUntil:       "2020-03-01",
		StationFromSettlement: 22,
		StationToSettlement:   23,
		StationFromCountry:    3,
		StationToCountry:      4,
	}
	flightBase = structs.FlightBase{
		DepartureStation: 102,
		ArrivalStation:   103,
	}
	flightPattern = &structs.FlightPattern{
		OperatingFromDate:     "2020-01-15",
		OperatingUntilDate:    "2020-04-15",
		MarketingCarrier:      11,
		MarketingFlightNumber: "5555",
	}
	assert.True(t, s.RuleApplies(flightBase, flightPattern, &rule), "By-all-params rule does not apply when it should")
	// Modify something, so the rule no longer applies
	flightPattern.MarketingFlightNumber = "7777"
	assert.False(t, s.RuleApplies(flightBase, flightPattern, &rule), "By-all-params rule applies when it shouldn't")
}

func TestIsPossiblyBanned(t *testing.T) {
	s := NewBlacklistRuleStorage(GetTestStations())

	ruleFromSvx := snapshots.TBlacklistRule{
		StationFromId: 103,
	}
	ruleToOvb := snapshots.TBlacklistRule{
		StationToId: 102,
	}
	s.AddRule(&ruleFromSvx)
	s.AddRule(&ruleToOvb)

	flightPattern := &structs.FlightPattern{}
	// from SVX
	flightBase := structs.FlightBase{
		DepartureStation: 103,
		ArrivalStation:   101,
	}
	assert.True(t, s.IsPossiblyBanned(flightBase, flightPattern), "flight is not possibly banned when it should be")
	// to OVB
	flightBase = structs.FlightBase{
		DepartureStation: 101,
		ArrivalStation:   102,
	}
	assert.True(t, s.IsPossiblyBanned(flightBase, flightPattern), "flight is not possibly banned when it should be")
	// from SVX to OVB, matches both rules
	flightBase = structs.FlightBase{
		DepartureStation: 103,
		ArrivalStation:   102,
	}
	assert.True(t, s.IsPossiblyBanned(flightBase, flightPattern), "flight is not possibly banned when it should be")
	// from LED to LED, no match
	flightBase = structs.FlightBase{
		DepartureStation: 101,
		ArrivalStation:   101,
	}
	assert.False(t, s.IsPossiblyBanned(flightBase, flightPattern), "flight is possibly banned when it shouldn't be")
}

func TestIsBanned(t *testing.T) {
	s := NewBlacklistRuleStorage(GetTestStations())

	ruleDates := snapshots.TBlacklistRule{
		FlightDateSince: "2020-02-02",
		FlightDateUntil: "2020-02-05",
	}
	ruleNationalVersion := snapshots.TBlacklistRule{
		NationalVersion: "ua",
	}
	s.AddRule(&ruleDates)
	s.AddRule(&ruleNationalVersion)

	flightPattern := &structs.FlightPattern{
		OperatingFromDate:  "2020-01-15",
		OperatingUntilDate: "2020-02-02",
	}
	flightBase := structs.FlightBase{}
	// no rule condition matches
	assert.False(t, s.IsBanned(flightBase, flightPattern, "2020-01-15", "ru"), "flight is banned when it shouldn't be")
	// flight date matches
	assert.True(t, s.IsBanned(flightBase, flightPattern, "2020-02-02", "ru"), "flight is not banned when it should be")
	// national version matches
	assert.True(t, s.IsBanned(flightBase, flightPattern, "2020-01-15", "ua"), "flight is not banned when it should be")
	// both flight date and national version match
	assert.True(t, s.IsBanned(flightBase, flightPattern, "2020-02-02", "ua"), "flight is not banned when it should be")
}

func TestIsBannedByTwoConditions(t *testing.T) {
	s := NewBlacklistRuleStorage(GetTestStations())

	rule := snapshots.TBlacklistRule{
		FlightDateSince: "2020-02-02",
		FlightDateUntil: "2020-02-05",
		NationalVersion: "ua",
	}
	s.AddRule(&rule)

	flightPattern := &structs.FlightPattern{
		OperatingFromDate:  "2020-01-15",
		OperatingUntilDate: "2020-02-02",
	}
	flightBase := structs.FlightBase{}
	// no rule condition matches
	assert.False(t, s.IsBanned(flightBase, flightPattern, "2020-01-14", "ru"), "flight is banned when it shouldn't be")
	// national version matches but flight date does not
	assert.False(t, s.IsBanned(flightBase, flightPattern, "2020-03-15", "ua"), "flight is banned when it should not be")
	// national version does not match but flight date does
	assert.False(t, s.IsBanned(flightBase, flightPattern, "2020-02-02", ""), "flight is banned when it should not be")
	assert.False(t, s.IsBanned(flightBase, flightPattern, "2020-02-02", "kz"), "flight is banned when it should not be")
	// both flight date and national version match
	assert.True(t, s.IsBanned(flightBase, flightPattern, "2020-02-02", "ua"), "flight is not banned when it should be")
}

func GetTestStations() station.StationStorage {
	stations := station.NewStationStorage()
	led := &snapshots.TStationWithCodes{
		Station: &rasp.TStation{
			Id:           101,
			TimeZoneId:   200,
			SettlementId: 21,
			CountryId:    2,
		},
		IataCode: "LED",
	}
	ovb := &snapshots.TStationWithCodes{
		Station: &rasp.TStation{
			Id:           102,
			TimeZoneId:   200,
			SettlementId: 22,
			CountryId:    3,
		},
		IataCode: "OVB",
	}
	svx := &snapshots.TStationWithCodes{
		Station: &rasp.TStation{
			Id:           103,
			TimeZoneId:   200,
			SettlementId: 23,
			CountryId:    4,
		},
		IataCode: "SVX",
	}
	stations.PutStation(led)
	stations.PutStation(ovb)
	stations.PutStation(svx)
	return stations
}
