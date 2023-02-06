package flight

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/library/go/core/xerrors"
	"a.yandex-team.ru/travel/avia/shared_flights/api/pkg/structs"
)

type mockTimezoneProvider struct{}

func (m mockTimezoneProvider) GetTimeZoneByStationID(int64) *time.Location {
	return time.UTC
}

func TestAeroflotFlightsCache_GetConnectionsMap(t *testing.T) {
	h := serviceTestHelper(t)

	// Test flights filter
	tests := []struct {
		name        string
		fromStation int32
		toStation   int32
		expected    func() map[string][][]FlightPatternAndBase
	}{
		{
			"empty map, no flights between JFK and SVX",
			JfkStationID,
			SvxStationID,
			func() map[string][][]FlightPatternAndBase {
				return make(map[string][][]FlightPatternAndBase)
			},
		},
		{
			"only direct flights between SVX and VKO",
			SvxStationID,
			VkoStationID,
			func() map[string][][]FlightPatternAndBase {
				expectedResult := make(map[string][][]FlightPatternAndBase)
				expectedResult[DirectFlights] = [][]FlightPatternAndBase{
					[]FlightPatternAndBase{
						{
							FlightBase:    h.flightBasesData.SVXVKO,
							FlightPattern: *h.flightPatternsData.SVXVKO,
						},
					},
				}
				return expectedResult
			},
		},
		{
			"both direct and connected flights between SVX and JFK",
			SvxStationID,
			JfkStationID,
			func() map[string][][]FlightPatternAndBase {
				expectedResult := make(map[string][][]FlightPatternAndBase)
				expectedResult[DirectFlights] = [][]FlightPatternAndBase{
					[]FlightPatternAndBase{
						{
							FlightBase:    h.flightBasesData.SVXJFK,
							FlightPattern: *h.flightPatternsData.SVXJFK,
						},
					},
				}
				expectedResult[VkoStationCode] = [][]FlightPatternAndBase{
					[]FlightPatternAndBase{
						{
							FlightBase:    h.flightBasesData.SVXVKO,
							FlightPattern: *h.flightPatternsData.SVXVKO,
						},
					},
					[]FlightPatternAndBase{
						{
							FlightBase:    h.flightBasesData.VKOJFK,
							FlightPattern: *h.flightPatternsData.VKOJFK,
						},
					},
				}
				return expectedResult
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			response := h.service.GetConnectionsMap(int64(tt.fromStation), int64(tt.toStation))
			assert.Equal(t, tt.expected(), response, "incorrect getConnectionsMap response")
		})
	}
}

const (
	SvxStationID   = 100
	JfkStationID   = 101
	VkoStationID   = 9600215
	VkoStationCode = "VKO"
)

type serviceHelper struct {
	service         AeroflotFlightsCache
	flightBases     map[int64]structs.FlightBase
	flightPatterns  map[int32]*structs.FlightPattern
	flightBasesData struct {
		SVXJFK structs.FlightBase
		SVXVKO structs.FlightBase
		VKOJFK structs.FlightBase
	}
	flightPatternsData struct {
		SVXJFK *structs.FlightPattern
		SVXVKO *structs.FlightPattern
		VKOJFK *structs.FlightPattern
	}
}

func serviceTestHelper(t *testing.T) *serviceHelper {
	h := serviceHelper{
		flightBases:    make(map[int64]structs.FlightBase),
		flightPatterns: make(map[int32]*structs.FlightPattern),
	}
	h.service = NewAeroflotFlightsCache(&h)

	// Flight bases
	h.flightBasesData.SVXJFK = structs.FlightBase{
		ID:                     301,
		OperatingCarrier:       26,
		OperatingCarrierCode:   "SU",
		OperatingFlightNumber:  "5555",
		DepartureStation:       SvxStationID,
		DepartureStationCode:   "SVX",
		DepartureTimeScheduled: 500,
		DepartureTerminal:      "A",
		ArrivalStation:         JfkStationID,
		ArrivalStationCode:     "JFK",
		ArrivalTimeScheduled:   1400,
		ArrivalTerminal:        "B",
		LegNumber:              1,
	}
	h.flightBases[int64(h.flightBasesData.SVXJFK.ID)] = h.flightBasesData.SVXJFK

	h.flightBasesData.SVXVKO = structs.FlightBase{
		ID:                     302,
		OperatingCarrier:       26,
		OperatingCarrierCode:   "SU",
		OperatingFlightNumber:  "6666",
		DepartureStation:       SvxStationID,
		DepartureStationCode:   "SVX",
		DepartureTimeScheduled: 1234,
		DepartureTerminal:      "B",
		ArrivalStation:         VkoStationID,
		ArrivalStationCode:     VkoStationCode,
		ArrivalTimeScheduled:   1345,
		ArrivalTerminal:        "C",
		LegNumber:              1,
	}
	h.flightBases[int64(h.flightBasesData.SVXVKO.ID)] = h.flightBasesData.SVXVKO

	h.flightBasesData.VKOJFK = structs.FlightBase{
		ID:                     303,
		OperatingCarrier:       26,
		OperatingCarrierCode:   "SU",
		OperatingFlightNumber:  "7777",
		DepartureStation:       VkoStationID,
		DepartureStationCode:   VkoStationCode,
		DepartureTimeScheduled: 1550,
		ArrivalStation:         JfkStationID,
		ArrivalStationCode:     "JFK",
		ArrivalTimeScheduled:   1725,
		ArrivalTerminal:        "A",
		LegNumber:              1,
	}
	h.flightBases[int64(h.flightBasesData.VKOJFK.ID)] = h.flightBasesData.VKOJFK

	// FLIGHT PATTERN
	h.flightPatternsData.SVXJFK = &structs.FlightPattern{
		ID:                    301,
		FlightBaseID:          301,
		OperatingFromDate:     "2021-07-01",
		OperatingUntilDate:    "2021-07-09",
		OperatingOnDays:       123,
		MarketingCarrier:      26,
		MarketingCarrierCode:  "SU",
		MarketingFlightNumber: "5555",
		LegNumber:             1,
	}
	h.flightPatterns[h.flightPatternsData.SVXJFK.ID] = h.flightPatternsData.SVXJFK

	h.flightPatternsData.SVXVKO = &structs.FlightPattern{
		ID:                    302,
		FlightBaseID:          302,
		OperatingFromDate:     "2021-07-01",
		OperatingUntilDate:    "2021-07-09",
		OperatingOnDays:       456,
		MarketingCarrier:      26,
		MarketingCarrierCode:  "SU",
		MarketingFlightNumber: "6666",
		LegNumber:             1,
	}
	h.flightPatterns[h.flightPatternsData.SVXVKO.ID] = h.flightPatternsData.SVXVKO

	h.flightPatternsData.VKOJFK = &structs.FlightPattern{
		ID:                    303,
		FlightBaseID:          303,
		OperatingFromDate:     "2021-07-01",
		OperatingUntilDate:    "2021-07-09",
		OperatingOnDays:       456,
		MarketingCarrier:      26,
		MarketingCarrierCode:  "SU",
		MarketingFlightNumber: "7777",
		LegNumber:             1,
	}
	h.flightPatterns[h.flightPatternsData.VKOJFK.ID] = h.flightPatternsData.VKOJFK

	err := h.service.Rebuild()
	assert.NoError(t, err, "error while rebuilding the cache")

	return &h
}

func (s *serviceHelper) GetFlightPatterns() map[int32]*structs.FlightPattern {
	return s.flightPatterns
}

func (s *serviceHelper) GetFlightBase(id int32, _ bool) (structs.FlightBase, error) {
	result, ok := s.flightBases[int64(id)]
	if !ok {
		return structs.FlightBase{}, xerrors.Errorf("flight base with id %d does not exist", id)
	}
	return result, nil
}
