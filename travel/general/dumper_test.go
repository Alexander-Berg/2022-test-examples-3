package dumper

import (
	"encoding/json"
	"reflect"
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/library/go/core/xerrors"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/segment"
	"a.yandex-team.ru/travel/avia/shared_flights/api/pkg/structs"
	"a.yandex-team.ru/travel/avia/shared_flights/lib/go/dtutil"
)

var u6366JsonData = `{"Patterns":[{"Id":1000001618,"FlightBaseId":1000001618,"FlightLegKey":"U6.366.1.1618","LegNumber":1,"OperatingFromDate":"2020-10-07","OperatingUntilDate":"2020-10-21","OperatingOnDays":3,"MarketingCarrier":30,"MarketingCarrierCode":"U6","MarketingFlightNumber":"366","BucketKey":"30.366.1"},{"Id":1000001605,"FlightBaseId":1000001605,"FlightLegKey":"U6.366.3.1605","LegNumber":3,"OperatingFromDate":"2020-07-13","OperatingUntilDate":"2020-09-28","OperatingOnDays":1,"MarketingCarrier":30,"MarketingCarrierCode":"U6","MarketingFlightNumber":"366","BucketKey":"30.366.3"},{"Id":1000001611,"FlightBaseId":1000001611,"FlightLegKey":"U6.366.3.1611","LegNumber":3,"OperatingFromDate":"2020-07-22","OperatingUntilDate":"2020-09-30","OperatingOnDays":3,"MarketingCarrier":30,"MarketingCarrierCode":"U6","MarketingFlightNumber":"366","BucketKey":"30.366.3"},{"Id":1000001599,"FlightBaseId":1000001599,"FlightLegKey":"U6.366.3.1599","LegNumber":3,"OperatingFromDate":"2020-07-08","OperatingUntilDate":"2020-07-08","OperatingOnDays":3,"MarketingCarrier":30,"MarketingCarrierCode":"U6","MarketingFlightNumber":"366","BucketKey":"30.366.3"},{"Id":1000001617,"FlightBaseId":1000001617,"FlightLegKey":"U6.366.3.1617","LegNumber":3,"OperatingFromDate":"2020-10-05","OperatingUntilDate":"2020-10-19","OperatingOnDays":1,"MarketingCarrier":30,"MarketingCarrierCode":"U6","MarketingFlightNumber":"366","BucketKey":"30.366.3"},{"Id":1000001614,"FlightBaseId":1000001614,"FlightLegKey":"U6.366.3.1614","LegNumber":3,"OperatingFromDate":"2020-10-03","OperatingUntilDate":"2020-10-24","OperatingOnDays":6,"MarketingCarrier":30,"MarketingCarrierCode":"U6","MarketingFlightNumber":"366","BucketKey":"30.366.3"},{"Id":1000001619,"FlightBaseId":1000001619,"FlightLegKey":"U6.366.2.1619","LegNumber":2,"OperatingFromDate":"2020-10-07","OperatingUntilDate":"2020-10-21","OperatingOnDays":3,"MarketingCarrier":30,"MarketingCarrierCode":"U6","MarketingFlightNumber":"366","BucketKey":"30.366.2"},{"Id":1000001613,"FlightBaseId":1000001613,"FlightLegKey":"U6.366.2.1613","LegNumber":2,"OperatingFromDate":"2020-10-03","OperatingUntilDate":"2020-10-24","OperatingOnDays":6,"MarketingCarrier":30,"MarketingCarrierCode":"U6","MarketingFlightNumber":"366","BucketKey":"30.366.2"},{"Id":1000001608,"FlightBaseId":1000001608,"FlightLegKey":"U6.366.3.1608","LegNumber":3,"OperatingFromDate":"2020-07-15","OperatingUntilDate":"2020-07-15","OperatingOnDays":3,"MarketingCarrier":30,"MarketingCarrierCode":"U6","MarketingFlightNumber":"366","BucketKey":"30.366.3"},{"Id":1000001612,"FlightBaseId":1000001612,"FlightLegKey":"U6.366.1.1612","LegNumber":1,"OperatingFromDate":"2020-10-03","OperatingUntilDate":"2020-10-24","OperatingOnDays":6,"MarketingCarrier":30,"MarketingCarrierCode":"U6","MarketingFlightNumber":"366","BucketKey":"30.366.1"},{"Id":1000001607,"FlightBaseId":1000001607,"FlightLegKey":"U6.366.2.1607","LegNumber":2,"OperatingFromDate":"2020-07-15","OperatingUntilDate":"2020-07-15","OperatingOnDays":3,"MarketingCarrier":30,"MarketingCarrierCode":"U6","MarketingFlightNumber":"366","BucketKey":"30.366.2"},{"Id":1000001603,"FlightBaseId":1000001603,"FlightLegKey":"U6.366.1.1603","LegNumber":1,"OperatingFromDate":"2020-07-13","OperatingUntilDate":"2020-09-28","OperatingOnDays":1,"MarketingCarrier":30,"MarketingCarrierCode":"U6","MarketingFlightNumber":"366","BucketKey":"30.366.1"},{"Id":1000001610,"FlightBaseId":1000001610,"FlightLegKey":"U6.366.2.1610","LegNumber":2,"OperatingFromDate":"2020-07-22","OperatingUntilDate":"2020-09-30","OperatingOnDays":3,"MarketingCarrier":30,"MarketingCarrierCode":"U6","MarketingFlightNumber":"366","BucketKey":"30.366.2"},{"Id":1000001609,"FlightBaseId":1000001609,"FlightLegKey":"U6.366.1.1609","LegNumber":1,"OperatingFromDate":"2020-07-22","OperatingUntilDate":"2020-09-30","OperatingOnDays":3,"MarketingCarrier":30,"MarketingCarrierCode":"U6","MarketingFlightNumber":"366","BucketKey":"30.366.1"},{"Id":1000001598,"FlightBaseId":1000001598,"FlightLegKey":"U6.366.2.1598","LegNumber":2,"OperatingFromDate":"2020-07-08","OperatingUntilDate":"2020-07-08","OperatingOnDays":3,"MarketingCarrier":30,"MarketingCarrierCode":"U6","MarketingFlightNumber":"366","BucketKey":"30.366.2"},{"Id":1000001597,"FlightBaseId":1000001597,"FlightLegKey":"U6.366.1.1597","LegNumber":1,"OperatingFromDate":"2020-07-08","OperatingUntilDate":"2020-07-08","OperatingOnDays":3,"MarketingCarrier":30,"MarketingCarrierCode":"U6","MarketingFlightNumber":"366","BucketKey":"30.366.1"},{"Id":1000001615,"FlightBaseId":1000001615,"FlightLegKey":"U6.366.1.1615","LegNumber":1,"OperatingFromDate":"2020-10-05","OperatingUntilDate":"2020-10-19","OperatingOnDays":1,"MarketingCarrier":30,"MarketingCarrierCode":"U6","MarketingFlightNumber":"366","BucketKey":"30.366.1"},{"Id":1000001620,"FlightBaseId":1000001620,"FlightLegKey":"U6.366.3.1620","LegNumber":3,"OperatingFromDate":"2020-10-07","OperatingUntilDate":"2020-10-21","OperatingOnDays":3,"MarketingCarrier":30,"MarketingCarrierCode":"U6","MarketingFlightNumber":"366","BucketKey":"30.366.3"},{"Id":1000001616,"FlightBaseId":1000001616,"FlightLegKey":"U6.366.2.1616","LegNumber":2,"OperatingFromDate":"2020-10-05","OperatingUntilDate":"2020-10-19","OperatingOnDays":1,"MarketingCarrier":30,"MarketingCarrierCode":"U6","MarketingFlightNumber":"366","BucketKey":"30.366.2"},{"Id":1000001602,"FlightBaseId":1000001602,"FlightLegKey":"U6.366.3.1602","LegNumber":3,"OperatingFromDate":"2020-07-11","OperatingUntilDate":"2020-09-26","OperatingOnDays":6,"MarketingCarrier":30,"MarketingCarrierCode":"U6","MarketingFlightNumber":"366","BucketKey":"30.366.3"},{"Id":1000001604,"FlightBaseId":1000001604,"FlightLegKey":"U6.366.2.1604","LegNumber":2,"OperatingFromDate":"2020-07-13","OperatingUntilDate":"2020-09-28","OperatingOnDays":1,"MarketingCarrier":30,"MarketingCarrierCode":"U6","MarketingFlightNumber":"366","BucketKey":"30.366.2"},{"Id":1000001601,"FlightBaseId":1000001601,"FlightLegKey":"U6.366.2.1601","LegNumber":2,"OperatingFromDate":"2020-07-11","OperatingUntilDate":"2020-09-26","OperatingOnDays":6,"MarketingCarrier":30,"MarketingCarrierCode":"U6","MarketingFlightNumber":"366","BucketKey":"30.366.2"},{"Id":1000001600,"FlightBaseId":1000001600,"FlightLegKey":"U6.366.1.1600","LegNumber":1,"OperatingFromDate":"2020-07-11","OperatingUntilDate":"2020-09-26","OperatingOnDays":6,"MarketingCarrier":30,"MarketingCarrierCode":"U6","MarketingFlightNumber":"366","BucketKey":"30.366.1"},{"Id":1000001606,"FlightBaseId":1000001606,"FlightLegKey":"U6.366.1.1606","LegNumber":1,"OperatingFromDate":"2020-07-15","OperatingUntilDate":"2020-07-15","OperatingOnDays":3,"MarketingCarrier":30,"MarketingCarrierCode":"U6","MarketingFlightNumber":"366","BucketKey":"30.366.1"}],"Bases":[{"Id":1000001618,"Carrier":30,"CarrierCode":"U6","FlightNumber":"366","LegNumber":1,"DepartureStation":9626399,"DepartureTimeScheduled":1145,"ArrivalStation":9600174,"ArrivalTimeScheduled":1415,"AircraftType":77,"BucketKey":"30.366.1","Source":2},{"Id":1000001605,"Carrier":30,"CarrierCode":"U6","FlightNumber":"366","LegNumber":3,"DepartureStation":9600370,"DepartureTimeScheduled":1530,"ArrivalStation":9600366,"ArrivalTimeScheduled":1620,"ArrivalTerminal":"1","AircraftType":67,"BucketKey":"30.366.3","Source":2},{"Id":1000001611,"Carrier":30,"CarrierCode":"U6","FlightNumber":"366","LegNumber":3,"DepartureStation":9600370,"DepartureTimeScheduled":1430,"ArrivalStation":9600366,"ArrivalTimeScheduled":1520,"ArrivalTerminal":"1","AircraftType":77,"BucketKey":"30.366.3","Source":2},{"Id":1000001599,"Carrier":30,"CarrierCode":"U6","FlightNumber":"366","LegNumber":3,"DepartureStation":9600370,"DepartureTimeScheduled":1430,"ArrivalStation":9600366,"ArrivalTimeScheduled":1520,"ArrivalTerminal":"1","AircraftType":77,"BucketKey":"30.366.3","Source":2},{"Id":1000001617,"Carrier":30,"CarrierCode":"U6","FlightNumber":"366","LegNumber":3,"DepartureStation":9600370,"DepartureTimeScheduled":1820,"ArrivalStation":9600366,"ArrivalTimeScheduled":1910,"ArrivalTerminal":"1","AircraftType":67,"BucketKey":"30.366.3","Source":2},{"Id":1000001614,"Carrier":30,"CarrierCode":"U6","FlightNumber":"366","LegNumber":3,"DepartureStation":9600370,"DepartureTimeScheduled":1820,"ArrivalStation":9600366,"ArrivalTimeScheduled":1910,"ArrivalTerminal":"1","AircraftType":67,"BucketKey":"30.366.3","Source":2},{"Id":1000001619,"Carrier":30,"CarrierCode":"U6","FlightNumber":"366","LegNumber":2,"DepartureStation":9600174,"DepartureTimeScheduled":1535,"ArrivalStation":9600370,"ArrivalTimeScheduled":1650,"AircraftType":77,"BucketKey":"30.366.2","Source":2},{"Id":1000001613,"Carrier":30,"CarrierCode":"U6","FlightNumber":"366","LegNumber":2,"DepartureStation":9600174,"DepartureTimeScheduled":1535,"ArrivalStation":9600370,"ArrivalTimeScheduled":1650,"AircraftType":67,"BucketKey":"30.366.2","Source":2},{"Id":1000001608,"Carrier":30,"CarrierCode":"U6","FlightNumber":"366","LegNumber":3,"DepartureStation":9600370,"DepartureTimeScheduled":1430,"ArrivalStation":9600366,"ArrivalTimeScheduled":1520,"ArrivalTerminal":"1","AircraftType":77,"BucketKey":"30.366.3","Source":2},{"Id":1000001612,"Carrier":30,"CarrierCode":"U6","FlightNumber":"366","LegNumber":1,"DepartureStation":9626399,"DepartureTimeScheduled":1145,"ArrivalStation":9600174,"ArrivalTimeScheduled":1415,"AircraftType":67,"BucketKey":"30.366.1","Source":2},{"Id":1000001607,"Carrier":30,"CarrierCode":"U6","FlightNumber":"366","LegNumber":2,"DepartureStation":9600174,"DepartureTimeScheduled":1145,"ArrivalStation":9600370,"ArrivalTimeScheduled":1300,"AircraftType":77,"BucketKey":"30.366.2","Source":2},{"Id":1000001603,"Carrier":30,"CarrierCode":"U6","FlightNumber":"366","LegNumber":1,"DepartureStation":9626399,"DepartureTimeScheduled":820,"ArrivalStation":9600174,"ArrivalTimeScheduled":1040,"AircraftType":67,"BucketKey":"30.366.1","Source":2},{"Id":1000001610,"Carrier":30,"CarrierCode":"U6","FlightNumber":"366","LegNumber":2,"DepartureStation":9600174,"DepartureTimeScheduled":1145,"ArrivalStation":9600370,"ArrivalTimeScheduled":1300,"AircraftType":77,"BucketKey":"30.366.2","Source":2},{"Id":1000001609,"Carrier":30,"CarrierCode":"U6","FlightNumber":"366","LegNumber":1,"DepartureStation":9626399,"DepartureTimeScheduled":755,"ArrivalStation":9600174,"ArrivalTimeScheduled":1025,"AircraftType":77,"BucketKey":"30.366.1","Source":2},{"Id":1000001598,"Carrier":30,"CarrierCode":"U6","FlightNumber":"366","LegNumber":2,"DepartureStation":9600174,"DepartureTimeScheduled":1145,"ArrivalStation":9600370,"ArrivalTimeScheduled":1300,"AircraftType":77,"BucketKey":"30.366.2","Source":2},{"Id":1000001597,"Carrier":30,"CarrierCode":"U6","FlightNumber":"366","LegNumber":1,"DepartureStation":9626399,"DepartureTimeScheduled":755,"ArrivalStation":9600174,"ArrivalTimeScheduled":1025,"AircraftType":77,"BucketKey":"30.366.1","Source":2},{"Id":1000001615,"Carrier":30,"CarrierCode":"U6","FlightNumber":"366","LegNumber":1,"DepartureStation":9626399,"DepartureTimeScheduled":1155,"ArrivalStation":9600174,"ArrivalTimeScheduled":1415,"AircraftType":67,"BucketKey":"30.366.1","Source":2},{"Id":1000001620,"Carrier":30,"CarrierCode":"U6","FlightNumber":"366","LegNumber":3,"DepartureStation":9600370,"DepartureTimeScheduled":1820,"ArrivalStation":9600366,"ArrivalTimeScheduled":1910,"ArrivalTerminal":"1","AircraftType":77,"BucketKey":"30.366.3","Source":2},{"Id":1000001616,"Carrier":30,"CarrierCode":"U6","FlightNumber":"366","LegNumber":2,"DepartureStation":9600174,"DepartureTimeScheduled":1535,"ArrivalStation":9600370,"ArrivalTimeScheduled":1650,"AircraftType":67,"BucketKey":"30.366.2","Source":2},{"Id":1000001602,"Carrier":30,"CarrierCode":"U6","FlightNumber":"366","LegNumber":3,"DepartureStation":9600370,"DepartureTimeScheduled":1530,"ArrivalStation":9600366,"ArrivalTimeScheduled":1620,"ArrivalTerminal":"1","AircraftType":67,"BucketKey":"30.366.3","Source":2},{"Id":1000001604,"Carrier":30,"CarrierCode":"U6","FlightNumber":"366","LegNumber":2,"DepartureStation":9600174,"DepartureTimeScheduled":1215,"ArrivalStation":9600370,"ArrivalTimeScheduled":1310,"AircraftType":67,"BucketKey":"30.366.2","Source":2},{"Id":1000001601,"Carrier":30,"CarrierCode":"U6","FlightNumber":"366","LegNumber":2,"DepartureStation":9600174,"DepartureTimeScheduled":1210,"ArrivalStation":9600370,"ArrivalTimeScheduled":1335,"AircraftType":67,"BucketKey":"30.366.2","Source":2},{"Id":1000001600,"Carrier":30,"CarrierCode":"U6","FlightNumber":"366","LegNumber":1,"DepartureStation":9626399,"DepartureTimeScheduled":830,"ArrivalStation":9600174,"ArrivalTimeScheduled":1050,"AircraftType":67,"BucketKey":"30.366.1","Source":2},{"Id":1000001606,"Carrier":30,"CarrierCode":"U6","FlightNumber":"366","LegNumber":1,"DepartureStation":9626399,"DepartureTimeScheduled":755,"ArrivalStation":9600174,"ArrivalTimeScheduled":1025,"AircraftType":77,"BucketKey":"30.366.1","Source":2}]}`

type mockFlightStorage struct {
	Patterns []structs.FlightPattern
	Bases    []structs.FlightBase
}

func (m mockFlightStorage) GetFlightPatterns() map[int32]*structs.FlightPattern {
	patterns := make(map[int32]*structs.FlightPattern)
	for _, p := range m.Patterns {
		pattern := p
		patterns[pattern.ID] = &pattern
	}
	return patterns
}

func (m mockFlightStorage) GetFlightBase(id int32, _ bool) (flightBase structs.FlightBase, err error) {
	for _, b := range m.Bases {
		if b.ID == id {
			return b, nil
		}
	}
	return structs.FlightBase{}, xerrors.New("Not found")
}

type tShortData struct {
	DepartureStation int64
	DepartureTime    string
	ArrivalStation   int64
	ArrivalTime      string
	LegNumber        int32

	OperatesFrom  string
	OperatesUntil string
	OperatesOn    int32
}

func Test_generateNextCombination(t *testing.T) {
	var U6366 mockFlightStorage

	segment.SetGlobalStartDateIndex(dtutil.DateCache.IndexOfStringDateP("2020-06-01"))

	err := json.Unmarshal([]byte(u6366JsonData), &U6366)
	assert.NoError(t, err, "Corrupted test data %v", err)
	var counters = make(map[interface{}]int)
	flights := fetchFlights(U6366, counters)

	assert.Len(t, flights, 1)

	flightSchedules := flights[0]
	assert.Equal(t, "U6 366", flightSchedules.Title)

	assert.Equal(t, 30, int(flightSchedules.AirlineID))

	expectedRoutes := []tRoute{
		{
			{
				AirportID:         9626399,
				ArrivalTime:       "",
				ArrivalDayShift:   0,
				DepartureTime:     "08:30:00",
				DepartureDayShift: 0,
			},
			{
				AirportID:         9600174,
				ArrivalTime:       "10:50:00",
				ArrivalDayShift:   0,
				DepartureTime:     "12:10:00",
				DepartureDayShift: 0,
			},
			{
				AirportID:         9600370,
				ArrivalTime:       "13:35:00",
				ArrivalDayShift:   0,
				DepartureTime:     "15:30:00",
				DepartureDayShift: 0,
			},
			{
				AirportID:         9600366,
				ArrivalTime:       "16:20:00",
				ArrivalTerminal:   "1",
				ArrivalDayShift:   0,
				DepartureTime:     "",
				DepartureDayShift: 0,
			},
		},
		{

			{
				AirportID:         9626399,
				ArrivalTime:       "",
				ArrivalDayShift:   0,
				DepartureTime:     "11:45:00",
				DepartureDayShift: 0,
			},
			{
				AirportID:         9600174,
				ArrivalTime:       "14:15:00",
				ArrivalDayShift:   0,
				DepartureTime:     "15:35:00",
				DepartureDayShift: 0,
			},
			{
				AirportID:         9600370,
				ArrivalTime:       "16:50:00",
				ArrivalDayShift:   0,
				DepartureTime:     "18:20:00",
				DepartureDayShift: 0,
			},
			{
				AirportID:         9600366,
				ArrivalTime:       "19:10:00",
				ArrivalTerminal:   "1",
				ArrivalDayShift:   0,
				DepartureTime:     "",
				DepartureDayShift: 0,
			},
		},
		{
			{
				AirportID:         9626399,
				ArrivalTime:       "",
				ArrivalDayShift:   0,
				DepartureTime:     "08:20:00",
				DepartureDayShift: 0,
			},
			{
				AirportID:         9600174,
				ArrivalTime:       "10:40:00",
				ArrivalDayShift:   0,
				DepartureTime:     "12:15:00",
				DepartureDayShift: 0,
			},
			{
				AirportID:         9600370,
				ArrivalTime:       "13:10:00",
				ArrivalDayShift:   0,
				DepartureTime:     "15:30:00",
				DepartureDayShift: 0,
			},
			{
				AirportID:         9600366,
				ArrivalTime:       "16:20:00",
				ArrivalTerminal:   "1",
				ArrivalDayShift:   0,
				DepartureTime:     "",
				DepartureDayShift: 0,
			},
		},
		{
			{
				AirportID:         9626399,
				ArrivalTime:       "",
				ArrivalDayShift:   0,
				DepartureTime:     "11:55:00",
				DepartureDayShift: 0,
			},
			{
				AirportID:         9600174,
				ArrivalTime:       "14:15:00",
				ArrivalDayShift:   0,
				DepartureTime:     "15:35:00",
				DepartureDayShift: 0,
			},
			{
				AirportID:         9600370,
				ArrivalTime:       "16:50:00",
				ArrivalDayShift:   0,
				DepartureTime:     "18:20:00",
				DepartureDayShift: 0,
			},
			{
				AirportID:         9600366,
				ArrivalTime:       "19:10:00",
				ArrivalDayShift:   0,
				ArrivalTerminal:   "1",
				DepartureTime:     "",
				DepartureDayShift: 0,
			},
		},
		{
			{
				AirportID:         9626399,
				ArrivalTime:       "",
				ArrivalDayShift:   0,
				DepartureTime:     "07:55:00",
				DepartureDayShift: 0,
			},
			{
				AirportID:         9600174,
				ArrivalTime:       "10:25:00",
				ArrivalDayShift:   0,
				DepartureTime:     "11:45:00",
				DepartureDayShift: 0,
			},
			{
				AirportID:         9600370,
				ArrivalTime:       "13:00:00",
				ArrivalDayShift:   0,
				DepartureTime:     "14:30:00",
				DepartureDayShift: 0,
			},
			{
				AirportID:         9600366,
				ArrivalTime:       "15:20:00",
				ArrivalDayShift:   0,
				ArrivalTerminal:   "1",
				DepartureTime:     "",
				DepartureDayShift: 0,
			},
		},
	}
	assert.Len(t, flightSchedules.Schedules, len(expectedRoutes), "incorrect number of routes generated")

	for _, route := range expectedRoutes {
		var match bool
		for _, schedule := range flightSchedules.Schedules {
			if reflect.DeepEqual(route, schedule.Route) {
				match = true
				break
			}
		}
		assert.True(t, match, "did not find any match for route %#v", route)
	}

}
