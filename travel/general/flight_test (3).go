package flight

import (
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/appconst"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/storage/carrier"
	"a.yandex-team.ru/travel/avia/shared_flights/api/pkg/structs"
	"a.yandex-team.ru/travel/avia/shared_flights/lib/go/dtutil"
	iatacorrector "a.yandex-team.ru/travel/avia/shared_flights/lib/go/iata_correction"
	"a.yandex-team.ru/travel/proto/shared_flights/snapshots"
)

func TestUpdateFlights_Basic(t *testing.T) {
	s := getTestFlightStorage()

	flightPattern1 := structs.FlightPattern{
		ID:                    14688,
		MarketingCarrier:      14,
		MarketingFlightNumber: "1423",
		OperatingFromDate:     "2019-02-19",
		LegNumber:             1,
	}

	flightPattern2 := structs.FlightPattern{
		ID:                    14689,
		MarketingCarrier:      14,
		MarketingFlightNumber: "1423",
		OperatingFromDate:     "2019-02-25",
		LegNumber:             2,
	}

	s.(*flightStorageImpl).updateFlights(&flightPattern1)
	s.(*flightStorageImpl).updateFlights(&flightPattern2)

	flights, ok := s.GetFlights(14, "1423")
	assert.True(t, ok, "cannot get flight 14/1423")
	assert.Equal(t, &flightPattern1, flights[0][0])
	assert.Equal(t, &flightPattern2, flights[1][0])
}

func TestUpdateFlights_InsertSameValueTwice(t *testing.T) {
	s := getTestFlightStorage()

	flightPattern1 := structs.FlightPattern{
		ID:                    14688,
		MarketingCarrier:      14,
		MarketingFlightNumber: "1423",
		OperatingFromDate:     "2019-02-19",
		LegNumber:             1,
	}

	s.(*flightStorageImpl).updateFlights(&flightPattern1)
	s.(*flightStorageImpl).updateFlights(&flightPattern1)

	flights, ok := s.GetFlights(14, "1423")
	assert.True(t, ok, "cannot get flight 14/1423")
	assert.Equal(t, &flightPattern1, flights[0][0])
	assert.Equal(t, 1, len(flights))
}

func TestUpdateFlights_UpdatingDataReplacesTheOldOne(t *testing.T) {
	s := getTestFlightStorage()

	flightPattern1 := structs.FlightPattern{
		ID:                    14688,
		MarketingCarrier:      14,
		MarketingFlightNumber: "1423",
		OperatingFromDate:     "2019-02-19",
		LegNumber:             1,
	}

	flightPattern2 := structs.FlightPattern{
		ID:                    14689,
		MarketingCarrier:      14,
		MarketingFlightNumber: "1423",
		OperatingFromDate:     "2019-02-20",
		LegNumber:             1,
	}

	s.(*flightStorageImpl).updateFlights(&flightPattern1)
	s.(*flightStorageImpl).updateFlights(&flightPattern2)

	flights, ok := s.GetFlights(14, "1423")
	assert.True(t, ok, "cannot get flight 14/1423")
	assert.Equal(t, &flightPattern1, flights[0][0])
	assert.Equal(t, 1, len(flights))
}

func TestGetFlightKey_Basic(t *testing.T) {
	assert.Equal(t, "14/1483S", GetFlightKey(14, "1483S"))
}

func TestGetFlightKeyForPattern_Basic(t *testing.T) {
	flightPattern := structs.FlightPattern{
		ID:                    14688,
		MarketingCarrier:      14,
		MarketingFlightNumber: "1423X",
		OperatingFromDate:     "2019-02-19",
	}

	assert.Equal(t, "14/1423X", GetFlightKeyForPattern(&flightPattern))
}

func TestPutDopFlightBase_ValidValue(t *testing.T) {
	s := getTestFlightStorage()

	flightBase := structs.FlightBase{
		ID:                   14687,
		OperatingCarrierCode: "U6",
	}

	s.(*flightStorageImpl).DopFlights.PutDopFlightBase(flightBase)
	fb, err := s.GetFlightBase(14687, true)
	assert.NoError(t, err, "cannot get flight base")
	assert.Equal(t, flightBase, fb)
}

func TestPutDopFlightPattern_ValidValue(t *testing.T) {
	s := getTestFlightStorage()

	flightPattern := structs.FlightPattern{
		ID:                   14688,
		MarketingCarrierCode: "U6",
		IsDop:                true,
	}

	s.(*flightStorageImpl).DopFlights.PutDopFlightPattern(flightPattern)
	assert.Equal(t, &flightPattern, s.GetDopFlightPatterns()[14688])
}

var airportStatusSource = int32(appconst.AirportFlightStatusSource)
var nonAirportStatusSource = int32(appconst.AirportFlightStatusSource + 100)

func TestFingLegNumber_FindInMap(t *testing.T) {
	flightBase1 := structs.FlightBase{
		ID:                     1111,
		OperatingCarrier:       14,
		OperatingCarrierCode:   "QQQ",
		OperatingFlightNumber:  "1423",
		DepartureStation:       100,
		DepartureTimeScheduled: 1910,
		ArrivalStation:         200,
		ArrivalTimeScheduled:   510,
		LegNumber:              1,
	}

	flightBase2 := structs.FlightBase{
		ID:                     2222,
		OperatingCarrier:       14,
		OperatingCarrierCode:   "QQQ",
		OperatingFlightNumber:  "1423",
		DepartureStation:       200,
		DepartureTimeScheduled: 2010,
		ArrivalStation:         300,
		ArrivalTimeScheduled:   610,
		LegNumber:              2,
	}

	flightPattern1 := structs.FlightPattern{
		ID:                    14688,
		FlightBaseID:          1111,
		MarketingCarrier:      14,
		MarketingFlightNumber: "1423",
		OperatingFromDate:     "2019-02-19",
		OperatingUntilDate:    "2019-02-21",
		OperatingOnDays:       123567,
		LegNumber:             1,
	}

	flightPattern2 := structs.FlightPattern{
		ID:                    14689,
		FlightBaseID:          2222,
		MarketingCarrier:      14,
		MarketingFlightNumber: "1423",
		OperatingFromDate:     "2019-02-19",
		OperatingUntilDate:    "2019-02-21",
		OperatingOnDays:       123567,
		LegNumber:             2,
	}

	flightPatternCodeshare := structs.FlightPattern{
		ID:                    24688,
		FlightBaseID:          1111,
		MarketingCarrier:      24,
		MarketingFlightNumber: "2423",
		OperatingFromDate:     "2019-02-19",
		OperatingUntilDate:    "2019-02-21",
		OperatingOnDays:       123567,
		LegNumber:             1,
		IsCodeshare:           true,
	}

	s := getTestFlightStorage()
	s.PutFlightBase(flightBase1)
	s.PutFlightBase(flightBase2)
	s.PutFlightPattern(flightPattern1)
	s.PutFlightPattern(flightPattern2)
	s.PutFlightPattern(flightPatternCodeshare)

	expect := assert.New(t)

	dtIdx := func(d dtutil.IntDate) int {
		return dtutil.DateCache.IndexOfIntDateP(d)
	}

	// No stations, just look up the flight number and date
	legInfo, err := s.(*flightStorageImpl).findLegNumberInMap(s.(*flightStorageImpl).Flights, "14/1423", dtIdx(20190219), 0, 0, false)
	leg := legInfo.FlightPattern.LegNumber
	expect.NoError(err)
	expect.Equal(int32(1), leg)
	expect.Equal(int32(14), legInfo.FlightPattern.MarketingCarrier)

	// Matching stations for the first leg
	legInfo, err = s.(*flightStorageImpl).findLegNumberInMap(s.(*flightStorageImpl).Flights, "14/1423", dtIdx(20190219), 100, 200, false)
	expect.NoError(err)
	leg = legInfo.FlightPattern.LegNumber
	expect.Equal(int32(1), leg)
	expect.Equal(int32(14), legInfo.FlightPattern.MarketingCarrier)

	// Matching stations for the second leg
	legInfo, err = s.(*flightStorageImpl).findLegNumberInMap(s.(*flightStorageImpl).Flights, "14/1423", dtIdx(20190219), 200, 300, false)
	expect.NoError(err)
	leg = legInfo.FlightPattern.LegNumber
	expect.Equal(int32(2), leg)
	expect.Equal(int32(14), legInfo.FlightPattern.MarketingCarrier)

	// Not matching stations
	legInfo, err = s.(*flightStorageImpl).findLegNumberInMap(s.(*flightStorageImpl).Flights, "14/1423", dtIdx(20190219), 200, 100, false)
	expect.Error(err)
	expect.Empty(legInfo)

	// Not matching both stations, airport as a source does not help
	legInfo, err = s.(*flightStorageImpl).findLegNumberInMap(s.(*flightStorageImpl).Flights, "14/1423", dtIdx(20190219), 300, 100, false)
	expect.Error(err)
	expect.Empty(legInfo)

	// Not matching dates
	legInfo, err = s.(*flightStorageImpl).findLegNumberInMap(s.(*flightStorageImpl).Flights, "14/1423", dtIdx(20190217), 200, 300, false)
	expect.Error(err)
	expect.Empty(legInfo)

	legInfo, err = s.(*flightStorageImpl).findLegNumberInMap(s.(*flightStorageImpl).Flights, "14/1423", dtIdx(20190222), 200, 300, false)
	expect.Error(err)
	expect.Empty(legInfo)

	// Not matching day of week
	legInfo, err = s.(*flightStorageImpl).findLegNumberInMap(s.(*flightStorageImpl).Flights, "14/1423", dtIdx(20190221), 200, 300, false)
	expect.Error(err)
	expect.Empty(legInfo)

	// Not matching day of week but codeshare
	legInfo, err = s.(*flightStorageImpl).findLegNumberInMap(s.(*flightStorageImpl).Flights, "24/2423", dtIdx(20190220), 100, 200, false)
	expect.NoError(err)
	leg = legInfo.FlightPattern.LegNumber
	expect.Equal(flightPatternCodeshare.LegNumber, leg)
	expect.Equal(flightPatternCodeshare, legInfo.FlightPattern)

	// Not matching flight
	legInfo, err = s.(*flightStorageImpl).findLegNumberInMap(s.(*flightStorageImpl).Flights, "14/1424", dtIdx(20190219), 200, 300, false)
	expect.Error(err)
	expect.Empty(legInfo)
}

func TestFingLegNumber_ScheduledVsFiling(t *testing.T) {
	flightBase := structs.FlightBase{
		ID:                     1111,
		OperatingCarrier:       14,
		OperatingCarrierCode:   "QQQ",
		OperatingFlightNumber:  "1423",
		DepartureStation:       100,
		DepartureTimeScheduled: 1910,
		ArrivalStation:         200,
		ArrivalTimeScheduled:   510,
		LegNumber:              1,
	}

	flightPattern1 := structs.FlightPattern{
		ID:                    14688,
		FlightBaseID:          1111,
		MarketingCarrier:      14,
		MarketingFlightNumber: "1423",
		OperatingFromDate:     "2019-02-19",
		OperatingUntilDate:    "2019-02-19",
		OperatingOnDays:       1234567,
		LegNumber:             1,
	}

	flightPattern2 := structs.FlightPattern{
		ID:                    1490,
		FlightBaseID:          1111,
		MarketingCarrier:      15,
		MarketingFlightNumber: "4321",
		OperatingFromDate:     "2019-02-19",
		OperatingUntilDate:    "2019-02-19",
		OperatingOnDays:       1234567,
		LegNumber:             2,
		FilingCarrier:         19,
	}

	s := getTestFlightStorage()
	s.PutFlightBase(flightBase)
	s.PutFlightPattern(flightPattern1)
	s.PutFlightPattern(flightPattern2)

	expect := assert.New(t)

	// Leg from scheduled flights
	legInfo, err := s.FindLegInfo(14, "1423", "2019-02-19", 0, 0, false)
	expect.NoError(err)
	leg := legInfo.FlightPattern.LegNumber
	expect.Equal(int32(1), leg)
	expect.Equal(int32(14), legInfo.FlightPattern.MarketingCarrier)

	// Leg from "filed" flights (i.e. flights mapped by their filing carrier)
	legInfo, err = s.FindLegInfo(19, "4321", "2019-02-19", 0, 0, false)
	expect.NoError(err)
	leg = legInfo.FlightPattern.LegNumber
	expect.Equal(int32(2), leg)
	expect.Equal(int32(15), legInfo.FlightPattern.MarketingCarrier)

	// Leg with filing carrier, but found in "scheduled" flights
	legInfo, err = s.FindLegInfo(15, "4321", "2019-02-19", 0, 0, false)
	expect.NoError(err)
	leg = legInfo.FlightPattern.LegNumber
	expect.Equal(int32(2), leg)
	expect.Equal(int32(15), legInfo.FlightPattern.MarketingCarrier)

	// Leg not found anywhere
	legInfo, err = s.FindLegInfo(19, "4321", "2019-02-21", 0, 0, false)
	expect.Error(err)
	expect.Empty(legInfo)
}

func getTestFlightStorage() FlightStorage {
	return NewFlightStorage(
		iatacorrector.NewIataCorrector([]*snapshots.TIataCorrectionRule{}, map[int32]string{}, map[string]int32{}),
		carrier.NewCarrierStorage(),
		"2019-01-01",
	)
}
