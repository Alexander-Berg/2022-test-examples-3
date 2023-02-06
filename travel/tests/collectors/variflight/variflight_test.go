package variflight

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/library/go/test/assertpb"
	"a.yandex-team.ru/travel/avia/flight_status_receiver/internal/collectors"
	"a.yandex-team.ru/travel/proto/avia/flight_status"
)

var jsonData = `[
{
	"FlightNo": "CZ6007",
	"FlightCompany": "China Southern Airlines",
	"FlightDepcode": "URC",
	"FlightArrcode": "ISB",
	"FlightDeptimePlanDate": "2014-09-01 09:00:00",
	"FlightArrtimePlanDate": "2014-09-01 09:00:00",
	"FlightDeptimeReadyDate": "2014-09-01 09:03:00",
	"FlightArrtimeReadyDate": "2014-09-01 08:55:00",
	"FlightDeptimeDate": "2014-09-01 09:22:00",
	"FlightArrtimeDate": "",
	"FlightIngateTime": "",
	"FlightOutgateTime": "",
	"CheckinTable": "1,2",
	"BoardGate": "17",
	"BaggageID": "3,4",
	"BoardState": "",
	"FlightState": "diversion",
	"FlightHTerminal": "3",
	"FlightTerminal": "1",
	"org_timezone": "28800",
	"dst_timezone": "18000",
	"StopFlag": "0",
	"ShareFlag": "0",
	"LegFlag": "0",
	"FlightDep": "Urumchi",
	"FlightArr": "Islamabad",
	"OntimeRate": "95.00%",
	"Deptel": "020-36066999",
	"Arrtel": "0471-96777",
	"Airlinetel": "95539",
	"Generic": "Airbus A321-200",
	"FlightYear": 7.3,
	"FlightDuration": "",
	"Distance": "2080",
	"alternate_info": [
		{
			"AlternateStatus": "arrival",
			"AlternateDepCity": "Kashgar",
			"AlternateArrCity": "Islamabad",
			"AlternateDepAirport": "KHG",
			"AlternateArrAirport": "ISB",
			"AlternateDeptimePlan": "2014-09-01 00:00:00",
			"AlternateArrtimePlan": "2014-08-31 21:00:00",
			"AlternateDeptime": "2014-09-01 13:57:00",
			"AlternateArrtime": "2014-09-01 12:20:00"
		}
	],
	"FlightDepAirport": "Urumchi Diwobao",
	"FlightArrAirport": "Islamabad",
	"fid": "2014090362233",
	"DepAirportLat": "24.543064",
	"DepAirportLon": "118.13418",
	"DepTerminalLat": "24.550356",
	"DepTerminalLon": "118.153719",
	"ArrAirportLat": "40.078537",
	"ArrAirportLon": "116.5871",
	"ArrTerminalLat": "40.086705",
	"ArrTerminalLon": "116.600726",
	"StopAirportCode": "",
	"StopCity": ""
}
]`

func Test_VariFlight(t *testing.T) {
	assert.NotPanics(t, func() { collectors.NewCollectorsManagerP() }, "Collectors manager failed")
	collector, ok := collectors.NewCollectorsManagerP().Collectors[collectors.VariFlight]
	assert.True(t, ok, "VariFlight collector not found")

	statusPack, err := collector.Collect([]byte(jsonData), context.Background())
	assert.NoError(t, err, "Error parsing data")

	statuses := statusPack.Statuses
	assert.Len(t, statuses, 2, "Wrong statuses length")

	var messageID = statusPack.MessageID
	var statusIDs []string
	for i := range statuses {
		assert.NotZero(t, statuses[i].StatusId, "status id is not filled")
		assert.NotZero(t, statuses[i].MessageId, "message id is not filled")
		assert.Equal(t, messageID, statuses[i].MessageId, "message id should be the same")
		assert.NotContains(t, statusIDs, statuses[i].StatusId, "status ids should be unique")
		assert.WithinDuration(t, time.Now(), time.Unix(statuses[i].ReceivedAt, 0), time.Minute, "received at now")
		statusIDs = append(statusIDs, statuses[i].StatusId)
		statuses[i].StatusId = ""
		statuses[i].MessageId = ""
		statuses[i].ReceivedAt = 0
	}

	expectedStatuses := []*flight_status.FlightStatus{
		{
			Airport:             "URC",
			AirlineCode:         "CZ",
			FlightNumber:        "6007",
			FlightDate:          "2014-09-01",
			Direction:           "departure",
			TimeActual:          "2014-09-01T09:22:00",
			TimeScheduled:       "2014-09-01T09:00:00",
			Status:              "diverted",
			Gate:                "17",
			Terminal:            "3",
			CheckInDesks:        "1,2",
			BaggageCarousels:    "",
			Diverted:            true,
			DivertedAirportCode: "KHG",
			RoutePointFrom:      "URC",
			RoutePointTo:        "ISB",
			Source:              "variflight",
		},
		{
			Airport:             "ISB",
			AirlineCode:         "CZ",
			FlightNumber:        "6007",
			FlightDate:          "2014-09-01",
			Direction:           "arrival",
			TimeActual:          "2014-09-01T08:55:00",
			TimeScheduled:       "2014-09-01T09:00:00",
			Status:              "diverted",
			Gate:                "",
			Terminal:            "1",
			CheckInDesks:        "",
			BaggageCarousels:    "3,4",
			Diverted:            true,
			DivertedAirportCode: "ISB",
			RoutePointFrom:      "URC",
			RoutePointTo:        "ISB",
			Source:              "variflight",
		},
	}
	assertpb.Equal(t, expectedStatuses, statuses)
}
