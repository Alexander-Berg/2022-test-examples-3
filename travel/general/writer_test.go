package status

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/avia/shared_flights/lib/go/direction"
	"a.yandex-team.ru/travel/avia/shared_flights/lib/go/strutil"
)

func Test_WriterStatusStatementValues(t *testing.T) {
	ssv := statusStatementValues{
		AirlineID:            1,
		AirlineCode:          "airlineCode",
		FlightNumber:         "FlightNumber",
		LegNumber:            2,
		FlightDate:           "FlightDate",
		StatusSourceID:       3,
		CreatedAtUTC:         time.Date(2020, 1, 1, 1, 1, 1, 1, time.UTC),
		UpdatedAtUTC:         time.Date(2021, 1, 1, 1, 1, 1, 1, time.UTC),
		DTimeActual:          strutil.EmptyToNil("direction Time actual"),
		DTimeScheduled:       strutil.EmptyToNil("direction time scheduled"),
		DStatus:              "direction status",
		DGate:                "direction Gate",
		DTerminal:            "direction terminal",
		DAirport:             "direction airport",
		DDiverted:            true,
		DDivertedAirportCode: "direction diverted code",
		DCreatedAtUTC:        time.Date(2022, 1, 1, 1, 1, 1, 1, time.UTC),
		DReceivedAtUTC:       time.Date(2023, 1, 1, 1, 1, 1, 1, time.UTC),
		DUpdatedAtUTC:        time.Date(2024, 1, 1, 1, 1, 1, 1, time.UTC),
		DRoutePointFrom:      "direction route point from",
		DRoutePointTo:        "direction route point to",
		RoutePointFrom:       "common route point from",
		RoutePointTo:         "common route point to",
		CheckInDesks:         "check in desks",
		BaggageCarousels:     "baggage carousels",
	}
	fields := ssv.fields()
	values := ssv.values()
	assert.True(t, len(fields) == len(values))

	// Just to make sure the order is preserved
	assert.Equal(t, []string{
		"airlineId", "airlineCode", "flightNumber", "legNumber", "flightDate", "statusSourceId", "createdAtUTC",
		"updatedAtUTC", "{{direction}}TimeActual", "{{direction}}TimeScheduled", "{{direction}}Status",
		"{{direction}}Gate", "{{direction}}Terminal", "{{direction}}Airport", "{{direction}}Diverted",
		"{{direction}}DivertedAirportCode", "{{direction}}CreatedAtUTC", "{{direction}}ReceivedAtUTC",
		"{{direction}}UpdatedAtUTC", "{{direction}}RoutePointFrom", "{{direction}}RoutePointTo",
		"RoutePointFrom", "RoutePointTo", "CheckInDesks",
		"BaggageCarousels"}, fields)
	assert.Equal(t, []interface{}{int64(1), "airlineCode", "FlightNumber", int16(2), "FlightDate", int16(3),
		time.Date(2020, 1, 1, 1, 1, 1, 1, time.UTC),
		time.Date(2021, 1, 1, 1, 1, 1, 1, time.UTC),
		strutil.EmptyToNil("direction Time actual"), strutil.EmptyToNil("direction time scheduled"),
		"direction status", "direction Gate", "direction terminal",
		"direction airport", true, "direction diverted code",
		time.Date(2022, 1, 1, 1, 1, 1, 1, time.UTC),
		time.Date(2023, 1, 1, 1, 1, 1, 1, time.UTC),
		time.Date(2024, 1, 1, 1, 1, 1, 1, time.UTC),
		"direction route point from", "direction route point to",
		"common route point from", "common route point to",
		"check in desks", "baggage carousels"}, values)

}

func Test_directionStatement(t *testing.T) {
	name, sql := directionStatement(direction.ARRIVAL)

	assert.Contains(
		t,
		sql,
		"INSERT INTO flight_status (airlineId, airlineCode, flightNumber, legNumber, flightDate, statusSourceId,"+
			" createdAtUTC, updatedAtUTC, arrivalTimeActual, arrivalTimeScheduled, arrivalStatus, arrivalGate, "+
			"arrivalTerminal, arrivalAirport, arrivalDiverted, arrivalDivertedAirportCode, arrivalCreatedAtUTC, "+
			"arrivalReceivedAtUTC, arrivalUpdatedAtUTC, arrivalRoutePointFrom, arrivalRoutePointTo, RoutePointFrom, RoutePointTo, CheckInDesks, "+
			"BaggageCarousels)\nVALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, $17, $18, $19, $20, $21, $22, $23, $24, $25)",
		sql,
	)
	assert.Contains(
		t,
		sql,
		",\nBaggageCarousels=(case when EXCLUDED.BaggageCarousels is null then flight_status.BaggageCarousels else EXCLUDED.BaggageCarousels end)",
		sql,
	)
	assert.NotContains(t, sql, "CheckInDesks=")
	assert.Equal(t, "arrival_status", name)

	name, sql = directionStatement(direction.DEPARTURE)
	assert.Contains(
		t,
		sql,
		"INSERT INTO flight_status (airlineId, airlineCode, flightNumber, legNumber, flightDate, statusSourceId,"+
			" createdAtUTC, updatedAtUTC, departureTimeActual, departureTimeScheduled, departureStatus, departureGate,"+
			" departureTerminal, departureAirport, departureDiverted, departureDivertedAirportCode, departureCreatedAtUTC,"+
			" departureReceivedAtUTC, departureUpdatedAtUTC, departureRoutePointFrom, departureRoutePointTo, RoutePointFrom, RoutePointTo, CheckInDesks,"+
			" BaggageCarousels)\nVALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, $17, $18, $19, $20, $21, $22, $23, $24, $25)",
		sql,
	)
	assert.Contains(
		t,
		sql,
		",\nCheckInDesks=(case when EXCLUDED.CheckInDesks is null then flight_status.CheckInDesks else EXCLUDED.CheckInDesks end)",
		sql,
	)
	assert.NotContains(t, sql, "BaggageCarousels=")
	assert.Equal(t, "departure_status", name)

}
