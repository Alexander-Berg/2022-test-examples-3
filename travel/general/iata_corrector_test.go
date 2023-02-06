package iatacorrection

import (
	"strconv"
	"testing"

	"a.yandex-team.ru/travel/avia/shared_flights/api/pkg/structs"
	"a.yandex-team.ru/travel/proto/shared_flights/snapshots"
)

func TestIataCorrector_TestEmptyRule(t *testing.T) {
	rule := snapshots.TIataCorrectionRule{
		CarrierId: 14,
	}
	iataCorrector := NewIataCorrector(Slice(&rule), map[int32]string{}, map[string]int32{})
	// Empty rule shall never match
	carrierID := iataCorrector.ApplyRule(&rule, "", "", "", "", 0)
	if carrierID != 0 {
		t.Errorf("Empty rule applies, result: %v", carrierID)
	}
}

func TestIataCorrector_TestFlightNumber(t *testing.T) {
	tests := []struct {
		ruleFlightNumberRegex string
		flightNumber          string
		expected              int32
	}{
		{
			ruleFlightNumberRegex: "123",
			flightNumber:          "123",
			expected:              14,
		},
		{
			ruleFlightNumberRegex: "123",
			flightNumber:          "124",
			expected:              0,
		},
		{
			ruleFlightNumberRegex: "^\\d\\d$",
			flightNumber:          "89",
			expected:              14,
		},
		{
			ruleFlightNumberRegex: "^\\d\\d$",
			flightNumber:          "8",
			expected:              0,
		},
		{
			ruleFlightNumberRegex: "^\\d\\d$",
			flightNumber:          "891",
			expected:              0,
		},
	}
	for _, test := range tests {
		t.Run(test.flightNumber, func(t *testing.T) {
			rule := snapshots.TIataCorrectionRule{
				MarketingCarrierIata: "SU",
				FlightNumberRegex:    test.ruleFlightNumberRegex,
				CarrierId:            14,
			}
			iataCorrector := NewIataCorrector(Slice(&rule), map[int32]string{}, map[string]int32{})
			carrierID := iataCorrector.ApplyRule(&rule, "SU", "", test.flightNumber, "", 0)
			if carrierID != test.expected {
				t.Errorf(
					"Incorrect answer for flight number: %+v. got %+v, expected %+v",
					test.flightNumber,
					carrierID,
					test.expected,
				)
			}
		})
	}
}

func TestIataCorrector_TestCarrierCode(t *testing.T) {
	tests := []struct {
		ruleCarrierIata   string
		ruleCarrierSirena string
		testCarrierIata   string
		testCarrierSirena string
		expected          int32
	}{
		{
			ruleCarrierIata:   "SU",
			ruleCarrierSirena: "",
			testCarrierIata:   "SU",
			testCarrierSirena: "",
			expected:          14,
		},
		{
			ruleCarrierIata:   "SU",
			ruleCarrierSirena: "",
			testCarrierIata:   "FV",
			testCarrierSirena: "",
			expected:          0,
		},
		{
			ruleCarrierIata:   "",
			ruleCarrierSirena: "ЯК",
			testCarrierIata:   "",
			testCarrierSirena: "ЯК",
			expected:          14,
		},
		{
			ruleCarrierIata:   "",
			ruleCarrierSirena: "ЯК",
			testCarrierIata:   "",
			testCarrierSirena: "FV",
			expected:          0,
		},
	}
	for idx, test := range tests {
		t.Run(strconv.Itoa(idx), func(t *testing.T) {
			rule := snapshots.TIataCorrectionRule{
				MarketingCarrierIata: test.ruleCarrierIata,
				CarrierSirena:        test.ruleCarrierSirena,
				CarrierId:            14,
			}
			iataCorrector := NewIataCorrector(Slice(&rule), map[int32]string{}, map[string]int32{})
			carrierID := iataCorrector.ApplyRule(&rule, test.testCarrierIata, test.testCarrierSirena, "123", "", 0)
			if carrierID != test.expected {
				t.Errorf(
					"Incorrect answer for carrier code: got %+v, expected %+v",
					carrierID,
					test.expected,
				)
			}
		})
	}
}

func TestIataCorrector_TestDesignatedAndFlyingCarrier(t *testing.T) {
	tests := []struct {
		designatedCarrier     string
		flyingCarrierIata     string
		testDesignatedCarrier int32
		testFlyingCarrierIata string
		expected              int32
	}{
		{
			designatedCarrier:     "JAPAN TRANSOCEAN AIR",
			flyingCarrierIata:     "",
			testDesignatedCarrier: 23,
			testFlyingCarrierIata: "",
			expected:              14,
		},
		{
			designatedCarrier:     "JAPAN TRANSOCEAN AIR",
			flyingCarrierIata:     "",
			testDesignatedCarrier: 0,
			testFlyingCarrierIata: "",
			expected:              0,
		},
		{
			designatedCarrier:     "",
			flyingCarrierIata:     "FV",
			testDesignatedCarrier: 0,
			testFlyingCarrierIata: "FV",
			expected:              14,
		},
		{
			designatedCarrier:     "",
			flyingCarrierIata:     "FV",
			testDesignatedCarrier: 0,
			testFlyingCarrierIata: "SU",
			expected:              0,
		},
		{
			designatedCarrier:     "",
			flyingCarrierIata:     "*",
			testDesignatedCarrier: 0,
			testFlyingCarrierIata: "NX",
			expected:              87,
		},
	}
	for idx, test := range tests {
		t.Run(strconv.Itoa(idx), func(t *testing.T) {
			rule := snapshots.TIataCorrectionRule{
				DesignatedCarrier: test.designatedCarrier,
				FlyingCarrierIata: test.flyingCarrierIata,
				CarrierId:         14,
			}
			designatedCarriers := map[int32]string{
				23: "JAPAN TRANSOCEAN AIR",
			}
			carriers := map[string]int32{
				"NX": 87,
			}
			iataCorrector := NewIataCorrector(Slice(&rule), designatedCarriers, carriers)
			carrierID := iataCorrector.ApplyRule(&rule, "", "", "", test.testFlyingCarrierIata, test.testDesignatedCarrier)
			if carrierID != test.expected {
				t.Errorf(
					"Incorrect answer for carrier code: got %+v, expected %+v",
					carrierID,
					test.expected,
				)
			}
		})
	}
}

func TestIataCorrector_TestAllParams(t *testing.T) {
	rule1 := snapshots.TIataCorrectionRule{
		Id:                   1,
		MarketingCarrierIata: "SU",
		FlightNumberRegex:    "^\\d\\d$",
		DesignatedCarrier:    "JAPAN TRANSOCEAN AIR",
		FlyingCarrierIata:    "*",
		CarrierId:            14,
	}
	rule2 := snapshots.TIataCorrectionRule{
		Id:                2,
		CarrierSirena:     "ЯК",
		FlightNumberRegex: "^\\d\\d$",
		DesignatedCarrier: "JAPAN TRANSOCEAN AIR",
		FlyingCarrierIata: "HH",
		CarrierId:         14,
	}
	tests := []struct {
		rule                  *snapshots.TIataCorrectionRule
		testCarrierIata       string
		testCarrierSirena     string
		testFlightNumber      string
		testFlyingCarrierIata string
		testDesignatedCarrier int32
		expected              int32
	}{
		{
			rule:                  &rule1,
			testCarrierIata:       "SU",
			testCarrierSirena:     "",
			testFlightNumber:      "55",
			testFlyingCarrierIata: "FV",
			testDesignatedCarrier: 0,
			expected:              0,
		},
		{
			rule:                  &rule1,
			testCarrierIata:       "SU",
			testCarrierSirena:     "",
			testFlightNumber:      "55",
			testFlyingCarrierIata: "FV",
			testDesignatedCarrier: 23,
			expected:              87,
		},
		{
			rule:                  &rule2,
			testCarrierIata:       "",
			testCarrierSirena:     "ЯК",
			testFlightNumber:      "33",
			testFlyingCarrierIata: "FV",
			testDesignatedCarrier: 23,
			expected:              0,
		},
		{
			rule:                  &rule2,
			testCarrierIata:       "",
			testCarrierSirena:     "ЯК",
			testFlightNumber:      "33",
			testFlyingCarrierIata: "HH",
			testDesignatedCarrier: 23,
			expected:              14,
		},
	}
	for idx, test := range tests {
		t.Run(strconv.Itoa(idx), func(t *testing.T) {
			designatedCarriers := map[int32]string{
				23: "JAPAN TRANSOCEAN AIR",
			}
			carriers := map[string]int32{
				"FV": 87,
				"HH": 55,
			}
			iataCorrector := NewIataCorrector(Slice(&rule1, &rule2), designatedCarriers, carriers)
			carrierID := iataCorrector.ApplyRule(
				test.rule,
				test.testCarrierIata,
				test.testCarrierSirena,
				test.testFlightNumber,
				test.testFlyingCarrierIata,
				test.testDesignatedCarrier,
			)
			if carrierID != test.expected {
				t.Errorf(
					"Incorrect answer for rule: %+v. got %+v, expected %+v",
					test.rule,
					carrierID,
					test.expected,
				)
			}
		})
	}
}

func TestIataCorrector_TestMultipleRules(t *testing.T) {
	rule1 := snapshots.TIataCorrectionRule{
		Id:                   1,
		MarketingCarrierIata: "SU",
		FlightNumberRegex:    "123",
		DesignatedCarrier:    "JAPAN TRANSOCEAN AIR",
		FlyingCarrierIata:    "HH",
		CarrierId:            14,
	}
	rule2 := snapshots.TIataCorrectionRule{
		Id:                   2,
		MarketingCarrierIata: "SU",
		FlightNumberRegex:    "124",
		DesignatedCarrier:    "JAPAN TRANSOCEAN AIR",
		FlyingCarrierIata:    "HH",
		CarrierId:            25,
	}
	tests := []struct {
		testFlightNumber string
		expected         int32
	}{
		{
			testFlightNumber: "123",
			expected:         14,
		},
		{
			testFlightNumber: "124",
			expected:         25,
		},
		{
			testFlightNumber: "125",
			expected:         0,
		},
	}
	for idx, test := range tests {
		t.Run(strconv.Itoa(idx), func(t *testing.T) {
			designatedCarriers := map[int32]string{
				23: "JAPAN TRANSOCEAN AIR",
			}
			iataCorrector := NewIataCorrector(Slice(&rule1, &rule2), designatedCarriers, map[string]int32{})

			carrierID := iataCorrector.FindCarrier("SU", "", test.testFlightNumber, "HH", 23)
			if carrierID != test.expected {
				t.Errorf(
					"Incorrect answer for test: %+v. got %+v, expected %+v",
					test.testFlightNumber,
					carrierID,
					test.expected,
				)
			}

			flightBase := structs.FlightBase{
				OperatingCarrierCode:  "SU",
				OperatingFlightNumber: test.testFlightNumber,
				FlyingCarrierIata:     "HH",
				DesignatedCarrier:     23,
			}
			carrierID = iataCorrector.FindFlightBaseCarrier(&flightBase, "")
			if carrierID != test.expected {
				t.Errorf(
					"Incorrect answer for flight-base in test: %+v. got %+v, expected %+v",
					test.testFlightNumber,
					carrierID,
					test.expected,
				)
			}

			flightPattern := structs.FlightPattern{
				MarketingCarrierCode:  "SU",
				MarketingFlightNumber: test.testFlightNumber,
				DesignatedCarrier:     23,
			}
			carrierID = iataCorrector.FindFlightPatternCarrier(&flightPattern, "HH", "")
			if carrierID != test.expected {
				t.Errorf(
					"Incorrect answer for flight-base in test: %+v. got %+v, expected %+v",
					test.testFlightNumber,
					carrierID,
					test.expected,
				)
			}
		})
	}
}

func TestIataCorrector_TestPriority(t *testing.T) {
	rule1 := snapshots.TIataCorrectionRule{
		Id:                   1,
		MarketingCarrierIata: "SU",
		FlightNumberRegex:    "",
		DesignatedCarrier:    "",
		FlyingCarrierIata:    "",
		CarrierId:            14,
		Priority:             1,
	}
	rule2 := snapshots.TIataCorrectionRule{
		Id:                   2,
		MarketingCarrierIata: "",
		FlightNumberRegex:    "124",
		DesignatedCarrier:    "",
		FlyingCarrierIata:    "",
		CarrierId:            25,
		Priority:             2,
	}
	tests := []struct {
		rules    []*snapshots.TIataCorrectionRule
		expected int32
	}{
		{
			rules:    Slice(&rule1, &rule2),
			expected: 25,
		},
		{
			rules:    Slice(&rule2, &rule1),
			expected: 25,
		},
	}
	for idx, test := range tests {
		t.Run(strconv.Itoa(idx), func(t *testing.T) {
			iataCorrector := NewIataCorrector(test.rules, map[int32]string{}, map[string]int32{})

			carrierID := iataCorrector.FindCarrier("SU", "", "124", "", 0)
			if carrierID != test.expected {
				t.Errorf(
					"Incorrect answer for test: %+v. got %+v, expected %+v",
					idx,
					carrierID,
					test.expected,
				)
			}
		})
	}
}

func Slice(rules ...*snapshots.TIataCorrectionRule) []*snapshots.TIataCorrectionRule {
	return rules
}
