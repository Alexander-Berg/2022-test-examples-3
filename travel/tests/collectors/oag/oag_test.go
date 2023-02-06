package oag

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/library/go/test/assertpb"
	"a.yandex-team.ru/travel/avia/flight_status_receiver/internal/collectors"
	"a.yandex-team.ru/travel/proto/avia/flight_status"
)

var jsonData = `{
    "Alert": {
        "Reinstated": false,
        "SubType": "Arrived",
        "Text": "Flight BA119: Status is now Arrived",
        "Type": "StatusChange"
    },
    "AlertIdentifier": "5cc8dfef-8799-4232-8791-b5002b0d19ce",
    "FlightChangeData": {
        "ArrivalGateChanged": false,
        "ArrivalTerminalChanged": false,
        "ArrivalTimeChanged": true,
        "BaggageChanged": false,
        "DepartureGateChanged": false,
        "DepartureTerminalChanged": false,
        "DepartureTimeChanged": false,
        "DiversionStatusChanged": false,
        "HasAnyChanges": true,
        "IsNewToFlightWindow": false,
        "OtherChanged": false,
        "PreviousArrivalGate": "",
        "PreviousArrivalTerminal": "",
        "PreviousArrivalTimeLocal": "2016-11-17T05:09:00",
        "PreviousArrivalTimeUtc": "2016-11-16T23:39:00Z",
        "PreviousBaggage": "",
        "PreviousDepartureGate": "",
        "PreviousDepartureTerminal": "",
        "PreviousDepartureTimeLocal": "",
        "PreviousDepartureTimeUtc": "",
        "PreviousDiversionStatus": "Original",
        "PreviousStatus": "Landed",
        "StatusChanged": true
    },
    "FlightData": {
        "AircraftType": "777",
        "AirlineCode": "BA",
        "AlternateArrivalAirportCode": "",
        "AlternateDepartureAirportCode": "",
        "ArrAirportCountryId": "IN",
        "ArrivalGate": "",
        "ArrivalTerminal": "",
        "Baggage": "",
        "DepAirportCountryId": "GB",
        "DepartureGate": "",
        "DepartureTerminal": "5",
        "DiversionStatus": "Original",
        "FlightNumber": "119",
        "FvFlightId": "BA119LHRBLR11161400",
        "LatestArrival": {
            "Accuracy": "Actual",
            "DateTimeLocal": "2016-11-17T05:07:00",
            "DateTimeType": "Gate",
            "DateTimeUtc": "2016-11-16T23:37:00Z",
            "SourceType": "AirlineAirport"
        },
        "LatestDeparture": {
            "Accuracy": "Actual",
            "DateTimeLocal": "2016-11-16T14:00:00",
            "DateTimeType": "Gate",
            "DateTimeUtc": "2016-11-16T14:00:00Z",
            "SourceType": "AirlineAirport"
        },
        "LegSequenceNumber": 1,
        "OperatingAirlineCode": "",
        "OperatingFlightNumber": "",
        "OptionalEquipment": "",
        "ProcessingTimeUtc": "2016-11-16T23:42:00Z",
        "RecoveryExists": false,
        "RecoveryFlight": null,
        "SchedArrivalAirportCode": "BLR",
        "SchedArrivalLocal": "2016-11-17T05:00:00",
        "SchedArrivalUtc": "2016-11-16T23:30:00Z",
        "SchedDepartureAirportCode": "LHR",
        "SchedDepartureLocal": "2016-11-16T14:00:00",
        "SchedDepartureUtc": "2016-11-16T14:00:00Z",
        "ScheduleStatus": "",
        "ServiceType": "J",
        "Status": "InGate",
        "TailNumber": "",
        "Unscheduled": false,
        "WeightClass": ""
    },
    "FlightIdentifier": {
        "AirlineCode": "BA",
        "ArrivalAirportCode": "BLR",
        "DepartureAirportCode": "LHR",
        "FlightNumber": "119",
        "SchedDepartureLocal": "2016-11-16T14:00:00"
    }
}`

func Test_OAG(t *testing.T) {
	assert.NotPanics(t, func() { collectors.NewCollectorsManagerP() }, "Collectors manager failed")
	collector, ok := collectors.NewCollectorsManagerP().Collectors[collectors.OAG]
	assert.True(t, ok, "OAG collector not found")

	statusPack, err := collector.Collect([]byte(jsonData), context.Background())
	assert.NoError(t, err, "Error parsing data")
	statuses := statusPack.Statuses
	var messageID = statusPack.MessageID
	var statusIDs []string
	for i, status := range statuses {
		assert.NotZero(t, status.StatusId, "status id is not filled")
		assert.NotZero(t, status.MessageId, "message id is not filled")
		assert.Equal(t, messageID, status.MessageId, "message id should be the same")
		assert.NotContains(t, statusIDs, status.StatusId, "status ids should be unique")
		assert.WithinDuration(t, time.Now(), time.Unix(status.ReceivedAt, 0), time.Minute, "received at now")
		statusIDs = append(statusIDs, status.StatusId)
		statuses[i].StatusId = ""
		statuses[i].MessageId = ""
		statuses[i].ReceivedAt = 0
	}

	expectedStatuses := []*flight_status.FlightStatus{
		{
			Airport:             "LHR",
			AirlineCode:         "BA",
			FlightNumber:        "119",
			FlightDate:          "2016-11-16",
			Direction:           "departure",
			TimeActual:          "2016-11-16T14:00:00",
			TimeScheduled:       "2016-11-16T14:00:00",
			Status:              "arrived",
			Gate:                "",
			Terminal:            "5",
			CheckInDesks:        "",
			BaggageCarousels:    "",
			Diverted:            false,
			DivertedAirportCode: "",
			RoutePointFrom:      "LHR",
			RoutePointTo:        "BLR",
			Source:              "oag",
		},
		{
			Airport:             "BLR",
			AirlineCode:         "BA",
			FlightNumber:        "119",
			FlightDate:          "2016-11-17",
			Direction:           "arrival",
			TimeActual:          "2016-11-17T05:07:00",
			TimeScheduled:       "2016-11-17T05:00:00",
			Status:              "arrived",
			Gate:                "",
			Terminal:            "",
			CheckInDesks:        "",
			BaggageCarousels:    "",
			Diverted:            false,
			DivertedAirportCode: "",
			RoutePointFrom:      "LHR",
			RoutePointTo:        "BLR",
			Source:              "oag",
		},
	}

	assertpb.Equal(t, expectedStatuses, statuses)
}
