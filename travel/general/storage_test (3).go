package storage

import (
	"sort"
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/storage/flight"
	"a.yandex-team.ru/travel/avia/shared_flights/api/pkg/structs"
	"a.yandex-team.ru/travel/proto/shared_flights/snapshots"
)

func TestPutFlightBase_ValidValue(t *testing.T) {
	s := NewStorage()

	flightBase := structs.FlightBase{
		ID:                   14687,
		OperatingCarrierCode: "U6",
	}

	s.PutFlightBase(flightBase)
	fb, err := s.FlightStorage().GetFlightBase(14687, false)
	assert.NoError(t, err, "cannot get flight base")
	assert.Equal(t, flightBase, fb)
}

func TestPutFlightPattern_ValidValue(t *testing.T) {
	s := NewStorage()

	flightPattern := structs.FlightPattern{
		ID:                   14688,
		MarketingCarrierCode: "U6",
	}

	s.PutFlightPattern(flightPattern)
	assert.Equal(t, &flightPattern, s.FlightStorage().GetFlightPatterns()[14688])
}

type mockIataCorrector struct {
}

func (s *mockIataCorrector) FindFlightBaseCarrier(flightBase *structs.FlightBase, carrierSirena string) int32 {
	return 0
}

func (s *mockIataCorrector) FindFlightPatternCarrier(flightPattern *structs.FlightPattern, flyingCarrierIata, carrierSirena string) int32 {
	return 1
}

func (s *mockIataCorrector) FindCarrier(iataCode, sirenaCode, flightNumber, flyingCarrierIata string, designatedCarrierID int32) int32 {
	return 0
}

func (s *mockIataCorrector) ApplyRule(rule *snapshots.TIataCorrectionRule, iataCode, sirenaCode, flightNumber, flyingCarrierIata string, designatedCarrierID int32) int32 {
	return 0
}

func (s *mockIataCorrector) GetRules() []*snapshots.TIataCorrectionRule {
	return []*snapshots.TIataCorrectionRule{}
}

func TestPutFlightPattern_NoIataCorrectionForCodeshares(t *testing.T) {
	s := NewStorage()

	s.SetIataCorrector(&mockIataCorrector{})

	codeshareFlightPattern := structs.FlightPattern{
		ID:                   14688,
		MarketingCarrierCode: "U6",
		IsCodeshare:          true,
	}

	s.PutFlightPattern(codeshareFlightPattern)
	assert.Equal(t, &codeshareFlightPattern, s.FlightStorage().GetFlightPatterns()[14688])

	iataCorrectedFlightBase := structs.FlightBase{
		ID:                   300,
		OperatingCarrierCode: "U6",
		OperatingCarrier:     1,
	}
	s.PutFlightBase(iataCorrectedFlightBase)

	iataCorrectedFlightPattern := structs.FlightPattern{
		ID:                   3000,
		FlightBaseID:         300,
		MarketingCarrierCode: "S7",
		MarketingCarrier:     2,
	}

	s.PutFlightPattern(iataCorrectedFlightPattern)
	ids := []int{}
	for k := range s.FlightStorage().GetFlightPatterns() {
		ids = append(ids, int(k))
	}
	sort.Ints(ids)
	assert.Equal(t, []int{3000, 14688, int(flight.MinIataCorrectedFlightPatternID + 1)}, ids)
}
