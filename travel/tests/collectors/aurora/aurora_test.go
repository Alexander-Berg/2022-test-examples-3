package aurora

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/library/go/test/assertpb"
	"a.yandex-team.ru/travel/avia/flight_status_receiver/internal/collectors"
	"a.yandex-team.ru/travel/proto/avia/flight_status"
)

// Taken from //home/aurora/release/airport/timetable/aaq_arrival
const jsonData = `[{
    "airport": "AAQ",
    "type": "arrival",
    "flights": [
        {
            "status": "",
			"baggage_claim": "11",
			"check_in_desks": "1,2,3,4,7,8,9",
			"terminal": "A",
			"gate": "1",
            "destination": "Москва / Шереметьево",
            "flight_number": "1144",
            "time_actual": "2020-04-05T00:15:00",
            "date": "2020-04-05",
            "airline_code": "SU",
            "time_scheduled": "2020-04-05T00:15:00"
        },
        {
            "status": "",
			"gates": "1,2",
            "destination": "Москва / Шереметьево",
            "flight_number": "1546",
            "time_actual": "2020-04-05T10:35:00",
            "date": "2020-04-05",
            "airline_code": "SU",
            "time_scheduled": "2020-04-05T10:35:00"
        },
		{
			"status": "Вылетел",
			"destination": "Магадан",
			"terminal": "А",
			"codeshare": [
				"S7 5219",
				"EO EO594"
			],
			"flight_number": "1519",
			"time_actual": "2021-01-18T00:16:00",
			"date": "2021-01-17",
			"gate": "6",
			"airline_code": "KC",
			"check_in_desks": "2,3,4,5,6,7,8,9,10,11,12,13",
			"time_scheduled": "2021-01-17T23:40:00"
		}
    ]
}]`

func Test_Aurora(t *testing.T) {
	assert.NotPanics(t, func() { collectors.NewCollectorsManagerP() }, "Collectors manager failed")
	collector, ok := collectors.NewCollectorsManagerP().Collectors[collectors.Aurora]
	assert.True(t, ok, "Aurora stats collector not found")

	statusPack, err := collector.Collect([]byte(jsonData), context.Background())
	assert.NoError(t, err, "Error parsing data")

	statuses := statusPack.Statuses

	var messageID = statusPack.MessageID
	var statusIDs []string
	for i, status := range statuses {
		assert.NotZero(t, status.StatusId, "status id is not filled")
		assert.NotZero(t, status.MessageId, "message id is not filled")
		assert.Equal(t, messageID, status.MessageId, "message id should be the same")
		assert.NotContains(t, statusIDs, status.StatusId, "message ids should be unique")
		assert.WithinDuration(t, time.Now(), time.Unix(status.ReceivedAt, 0), time.Minute, "received at now")
		statusIDs = append(statusIDs, status.StatusId)
		statuses[i].StatusId = ""
		statuses[i].MessageId = ""
		statuses[i].ReceivedAt = 0
	}

	expectedStatuses := []*flight_status.FlightStatus{
		{
			Airport:             "AAQ",
			AirlineCode:         "SU",
			FlightNumber:        "1144",
			FlightDate:          "2020-04-05",
			Direction:           "arrival",
			TimeActual:          "2020-04-05T00:15:00",
			TimeScheduled:       "2020-04-05T00:15:00",
			Status:              "no-data",
			Gate:                "1",
			Terminal:            "A",
			CheckInDesks:        "1-4, 7-9",
			BaggageCarousels:    "11",
			Diverted:            false,
			DivertedAirportCode: "",
			RoutePointFrom:      "Москва / Шереметьево",
			RoutePointTo:        "AAQ",
			Source:              "aurora",
		},
		{
			Airport:             "AAQ",
			AirlineCode:         "SU",
			FlightNumber:        "1546",
			FlightDate:          "2020-04-05",
			Direction:           "arrival",
			TimeActual:          "2020-04-05T10:35:00",
			TimeScheduled:       "2020-04-05T10:35:00",
			Status:              "no-data",
			Gate:                "1,2",
			Terminal:            "",
			CheckInDesks:        "",
			BaggageCarousels:    "",
			Diverted:            false,
			DivertedAirportCode: "",
			RoutePointFrom:      "Москва / Шереметьево",
			RoutePointTo:        "AAQ",
			Source:              "aurora",
		},
		{
			Airport:        "AAQ",
			AirlineCode:    "KC",
			FlightNumber:   "1519",
			FlightDate:     "2021-01-17",
			Direction:      "arrival",
			TimeActual:     "2021-01-18T00:16:00",
			TimeScheduled:  "2021-01-17T23:40:00",
			Status:         "departed",
			Gate:           "6",
			Terminal:       "А",
			CheckInDesks:   "2-13",
			RoutePointFrom: "Магадан",
			RoutePointTo:   "AAQ",
			Source:         "aurora",
		},
		{
			Airport:        "AAQ",
			AirlineCode:    "S7",
			FlightNumber:   "5219",
			FlightDate:     "2021-01-17",
			Direction:      "arrival",
			TimeActual:     "2021-01-18T00:16:00",
			TimeScheduled:  "2021-01-17T23:40:00",
			Status:         "departed",
			Gate:           "6",
			Terminal:       "А",
			CheckInDesks:   "2-13",
			RoutePointFrom: "Магадан",
			RoutePointTo:   "AAQ",
			Source:         "aurora",
		},
		{
			Airport:        "AAQ",
			AirlineCode:    "EO",
			FlightNumber:   "594",
			FlightDate:     "2021-01-17",
			Direction:      "arrival",
			TimeActual:     "2021-01-18T00:16:00",
			TimeScheduled:  "2021-01-17T23:40:00",
			Status:         "departed",
			Gate:           "6",
			Terminal:       "А",
			CheckInDesks:   "2-13",
			RoutePointFrom: "Магадан",
			RoutePointTo:   "AAQ",
			Source:         "aurora",
		},
	}

	assertpb.Equal(t, expectedStatuses, statuses)

}
