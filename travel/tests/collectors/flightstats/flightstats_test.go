package flightstats

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/library/go/test/assertpb"
	"a.yandex-team.ru/travel/avia/flight_status_receiver/internal/collectors"
	"a.yandex-team.ru/travel/proto/avia/flight_status"
)

func Test_Flightstats(t *testing.T) {
	// most of the data is ignored for now
	jsonData := `{
    "appendix": {
        "airports": {
            "airport": [
                {
                    "name": "Guangzhou Baiyun International Airport",
                    "elevationFeet": "36",
                    "countryCode": "CN",
                    "icao": "ZGGG",
                    "latitude": "23.387862",
                    "street1": "Airport W Ave",
                    "localTime": "2020-03-30T15:30:10.695",
                    "cityCode": "CAN",
                    "utcOffsetHours": "8.0",
                    "iata": "CAN",
                    "classification": "1",
                    "fs": "CAN",
                    "faa": "",
                    "active": "true",
                    "longitude": "113.29734",
                    "regionName": "Asia",
                    "timeZoneRegionName": "Asia/Shanghai",
                    "weatherZone": "",
                    "city": "Guangzhou",
                    "countryName": "China"
                },
                {
                    "name": "Sheremetyevo International Airport",
                    "elevationFeet": "630",
                    "countryCode": "RU",
                    "icao": "UUEE",
                    "latitude": "55.966324",
                    "localTime": "2020-03-30T10:30:10.695",
                    "cityCode": "MOW",
                    "utcOffsetHours": "3.0",
                    "iata": "SVO",
                    "classification": "1",
                    "fs": "SVO",
                    "faa": "",
                    "active": "true",
                    "longitude": "37.416574",
                    "regionName": "Europe",
                    "timeZoneRegionName": "Europe/Moscow",
                    "weatherZone": "",
                    "city": "Moscow",
                    "countryName": "Russian Federation"
                }
            ]
        },
        "airlines": {
            "airline": [
                {
                    "name": "Aeroflot",
                    "icao": "AFL",
                    "fs": "DP*",
                    "active": "true",
                    "iata": "SU",
                    "phoneNumber": "+7 495 223-55-55"
                },
                {
                    "name": "China Southern Airlines",
                    "fs": "CZ",
                    "icao": "CSN",
                    "active": "true",
                    "iata": "CZ",
                    "phoneNumber": "+86 20 95539"
                }
            ]
        },
        "equipments": {
            "equipment": [
                {
                    "name": "Boeing 777-300",
                    "regional": "false",
                    "jet": "true",
                    "iata": "773",
                    "widebody": "true",
                    "turboProp": "false"
                },
                {
                    "regional": "false",
                    "name": "Boeing 777-300ER",
                    "jet": "true",
                    "iata": "77W",
                    "widebody": "true",
                    "turboProp": "false"
                }
            ]
        }
    },
    "alert": {
        "rule": {
            "departure": "2020-03-30T11:10:00.000",
            "flightNumber": "221",
            "carrierFsCode": "DP*",
            "departureAirportFsCode": "CAN",
            "id": "1342061003",
            "arrival": "2020-03-30T16:25:00.000",
            "delivery": {
                "destination": "https://avia.yandex.ru/flight-stats/alert",
                "format": "JSON"
            },
            "arrivalAirportFsCode": "SVO",
            "name": "prod-SU221-2020330",
            "ruleEvents": {
                "ruleEvent": [
                    {
                        "type": "ALL_CHANGES"
                    }
                ]
            }
        },
        "flightStatus": {
            "flightStatusUpdates": {
                "flightStatusUpdate": [
                    {
                        "updatedAt": {
                            "dateUtc": "2020-03-27T20:24:21.183Z"
                        },
                        "updatedTextFields": {
                            "updatedTextField": [
                                {
                                    "newText": "2",
                                    "field": "DTM"
                                },
                                {
                                    "newText": "F",
                                    "field": "ATM"
                                },
                                {
                                    "newText": "77W",
                                    "field": "SQP"
                                },
                                {
                                    "newText": "S",
                                    "field": "STS"
                                }
                            ]
                        },
                        "updatedDateFields": {
                            "updatedDateField": [
                                {
                                    "newDateLocal": "2020-03-30T11:10:00.000",
                                    "newDateUtc": "2020-03-30T03:10:00.000Z",
                                    "field": "SGD"
                                },
                                {
                                    "newDateLocal": "2020-03-30T16:25:00.000",
                                    "newDateUtc": "2020-03-30T13:25:00.000Z",
                                    "field": "SGA"
                                }
                            ]
                        },
                        "source": "Innovata"
                    },
                    {
                        "updatedAt": {
                            "dateUtc": "2020-03-29T20:15:49.533Z"
                        },
                        "updatedTextFields": {
                            "updatedTextField": [
                                {
                                    "newText": "773",
                                    "field": "AQP"
                                }
                            ]
                        },
                        "updatedDateFields": {
                            "updatedDateField": [
                                {
                                    "newDateLocal": "2020-03-30T11:10:00.000",
                                    "newDateUtc": "2020-03-30T03:10:00.000Z",
                                    "field": "EGD"
                                },
                                {
                                    "newDateLocal": "2020-03-30T16:25:00.000",
                                    "newDateUtc": "2020-03-30T13:25:00.000Z",
                                    "field": "EGA"
                                }
                            ]
                        },
                        "source": "Airline"
                    },
                    {
                        "updatedAt": {
                            "dateUtc": "2020-03-30T03:21:50.563Z"
                        },
                        "updatedDateFields": {
                            "updatedDateField": [
                                {
                                    "newDateLocal": "2020-03-30T11:41:00.000",
                                    "newDateUtc": "2020-03-30T03:41:00.000Z",
                                    "originalDateUtc": "2020-03-30T03:10:00.000Z",
                                    "originalDateLocal": "2020-03-30T11:10:00.000",
                                    "field": "EGD"
                                },
                                {
                                    "newDateLocal": "2020-03-30T16:12:00.000",
                                    "newDateUtc": "2020-03-30T13:12:00.000Z",
                                    "originalDateLocal": "2020-03-30T16:25:00.000",
                                    "originalDateUtc": "2020-03-30T13:25:00.000Z",
                                    "field": "EGA"
                                }
                            ]
                        },
                        "source": "Airport"
                    },
                    {
                        "updatedAt": {
                            "dateUtc": "2020-03-30T03:33:19.150Z"
                        },
                        "updatedDateFields": {
                            "updatedDateField": [
                                {
                                    "newDateLocal": "2020-03-30T16:16:00.000",
                                    "newDateUtc": "2020-03-30T13:16:00.000Z",
                                    "originalDateLocal": "2020-03-30T16:12:00.000",
                                    "originalDateUtc": "2020-03-30T13:12:00.000Z",
                                    "field": "EGA"
                                }
                            ]
                        },
                        "source": "Airport"
                    },
                    {
                        "updatedAt": {
                            "dateUtc": "2020-03-30T03:51:50.608Z"
                        },
                        "updatedDateFields": {
                            "updatedDateField": [
                                {
                                    "newDateLocal": "2020-03-30T12:12:00.000",
                                    "newDateUtc": "2020-03-30T04:12:00.000Z",
                                    "originalDateLocal": "2020-03-30T11:41:00.000",
                                    "originalDateUtc": "2020-03-30T03:41:00.000Z",
                                    "field": "EGD"
                                },
                                {
                                    "newDateLocal": "2020-03-30T16:43:00.000",
                                    "newDateUtc": "2020-03-30T13:43:00.000Z",
                                    "originalDateUtc": "2020-03-30T13:16:00.000Z",
                                    "originalDateLocal": "2020-03-30T16:16:00.000",
                                    "field": "EGA"
                                }
                            ]
                        },
                        "source": "Airport"
                    },
                    {
                        "updatedAt": {
                            "dateUtc": "2020-03-30T04:18:28.042Z"
                        },
                        "updatedDateFields": {
                            "updatedDateField": [
                                {
                                    "newDateLocal": "2020-03-30T12:43:00.000",
                                    "newDateUtc": "2020-03-30T04:43:00.000Z",
                                    "originalDateLocal": "2020-03-30T12:12:00.000",
                                    "originalDateUtc": "2020-03-30T04:12:00.000Z",
                                    "field": "EGD"
                                },
                                {
                                    "newDateLocal": "2020-03-30T17:14:00.000",
                                    "newDateUtc": "2020-03-30T14:14:00.000Z",
                                    "originalDateLocal": "2020-03-30T16:43:00.000",
                                    "originalDateUtc": "2020-03-30T13:43:00.000Z",
                                    "field": "EGA"
                                }
                            ]
                        },
                        "source": "Airport"
                    },
                    {
                        "updatedAt": {
                            "dateUtc": "2020-03-30T04:48:19.390Z"
                        },
                        "updatedDateFields": {
                            "updatedDateField": [
                                {
                                    "newDateLocal": "2020-03-30T13:14:00.000",
                                    "newDateUtc": "2020-03-30T05:14:00.000Z",
                                    "originalDateLocal": "2020-03-30T12:43:00.000",
                                    "originalDateUtc": "2020-03-30T04:43:00.000Z",
                                    "field": "EGD"
                                },
                                {
                                    "newDateLocal": "2020-03-30T17:45:00.000",
                                    "newDateUtc": "2020-03-30T14:45:00.000Z",
                                    "originalDateLocal": "2020-03-30T17:14:00.000",
                                    "originalDateUtc": "2020-03-30T14:14:00.000Z",
                                    "field": "EGA"
                                }
                            ]
                        },
                        "source": "Airport"
                    },
                    {
                        "updatedAt": {
                            "dateUtc": "2020-03-30T05:04:47.210Z"
                        },
                        "updatedDateFields": {
                            "updatedDateField": [
                                {
                                    "newDateLocal": "2020-03-30T17:49:00.000",
                                    "newDateUtc": "2020-03-30T14:49:00.000Z",
                                    "originalDateUtc": "2020-03-30T14:45:00.000Z",
                                    "originalDateLocal": "2020-03-30T17:45:00.000",
                                    "field": "EGA"
                                }
                            ]
                        },
                        "source": "Airport"
                    },
                    {
                        "updatedAt": {
                            "dateUtc": "2020-03-30T05:22:11.181Z"
                        },
                        "updatedDateFields": {
                            "updatedDateField": [
                                {
                                    "newDateLocal": "2020-03-30T13:45:00.000",
                                    "newDateUtc": "2020-03-30T05:45:00.000Z",
                                    "originalDateUtc": "2020-03-30T05:14:00.000Z",
                                    "originalDateLocal": "2020-03-30T13:14:00.000",
                                    "field": "EGD"
                                },
                                {
                                    "newDateLocal": "2020-03-30T18:20:00.000",
                                    "newDateUtc": "2020-03-30T15:20:00.000Z",
                                    "originalDateUtc": "2020-03-30T14:49:00.000Z",
                                    "originalDateLocal": "2020-03-30T17:49:00.000",
                                    "field": "EGA"
                                }
                            ]
                        },
                        "source": "Airport"
                    },
                    {
                        "updatedAt": {
                            "dateUtc": "2020-03-30T05:53:01.057Z"
                        },
                        "updatedDateFields": {
                            "updatedDateField": [
                                {
                                    "newDateLocal": "2020-03-30T14:16:00.000",
                                    "newDateUtc": "2020-03-30T06:16:00.000Z",
                                    "originalDateLocal": "2020-03-30T13:45:00.000",
                                    "originalDateUtc": "2020-03-30T05:45:00.000Z",
                                    "field": "EGD"
                                },
                                {
                                    "newDateLocal": "2020-03-30T18:47:00.000",
                                    "newDateUtc": "2020-03-30T15:47:00.000Z",
                                    "originalDateLocal": "2020-03-30T18:20:00.000",
                                    "originalDateUtc": "2020-03-30T15:20:00.000Z",
                                    "field": "EGA"
                                }
                            ]
                        },
                        "source": "Airport"
                    },
                    {
                        "updatedAt": {
                            "dateUtc": "2020-03-30T06:26:53.467Z"
                        },
                        "updatedDateFields": {
                            "updatedDateField": [
                                {
                                    "newDateLocal": "2020-03-30T14:47:00.000",
                                    "newDateUtc": "2020-03-30T06:47:00.000Z",
                                    "originalDateLocal": "2020-03-30T14:16:00.000",
                                    "originalDateUtc": "2020-03-30T06:16:00.000Z",
                                    "field": "EGD"
                                },
                                {
                                    "newDateLocal": "2020-03-30T19:18:00.000",
                                    "newDateUtc": "2020-03-30T16:18:00.000Z",
                                    "originalDateLocal": "2020-03-30T18:47:00.000",
                                    "originalDateUtc": "2020-03-30T15:47:00.000Z",
                                    "field": "EGA"
                                }
                            ]
                        },
                        "source": "Airport"
                    },
                    {
                        "updatedAt": {
                            "dateUtc": "2020-03-30T06:57:42.308Z"
                        },
                        "updatedDateFields": {
                            "updatedDateField": [
                                {
                                    "newDateLocal": "2020-03-30T15:18:00.000",
                                    "newDateUtc": "2020-03-30T07:18:00.000Z",
                                    "originalDateUtc": "2020-03-30T06:47:00.000Z",
                                    "originalDateLocal": "2020-03-30T14:47:00.000",
                                    "field": "EGD"
                                },
                                {
                                    "newDateLocal": "2020-03-30T19:53:00.000",
                                    "newDateUtc": "2020-03-30T16:53:00.000Z",
                                    "originalDateLocal": "2020-03-30T19:18:00.000",
                                    "originalDateUtc": "2020-03-30T16:18:00.000Z",
                                    "field": "EGA"
                                }
                            ]
                        },
                        "source": "Airport"
                    },
                    {
                        "updatedAt": {
                            "dateUtc": "2020-03-30T07:29:31.207Z"
                        },
                        "updatedDateFields": {
                            "updatedDateField": [
                                {
                                    "newDateLocal": "2020-03-30T15:49:00.000",
                                    "newDateUtc": "2020-03-30T07:49:00.000Z",
                                    "originalDateLocal": "2020-03-30T15:18:00.000",
                                    "originalDateUtc": "2020-03-30T07:18:00.000Z",
                                    "field": "EGD"
                                },
                                {
                                    "newDateLocal": "2020-03-30T20:20:00.000",
                                    "newDateUtc": "2020-03-30T17:20:00.000Z",
                                    "originalDateLocal": "2020-03-30T19:53:00.000",
                                    "originalDateUtc": "2020-03-30T16:53:00.000Z",
                                    "field": "EGA"
                                }
                            ]
                        },
                        "source": "Airport"
                    }
                ]
            },
            "flightDurations": {
                "scheduledBlockMinutes": "615"
            },
            "arrivalDate": {
                "dateUtc": "2020-03-30T13:25:00.000Z",
                "dateLocal": "2020-03-30T16:25:00.000"
            },
            "carrierFsCode": "DP*",
            "arrivalAirportFsCode": "SVO",
            "schedule": {
                "flightType": "J",
                "restrictions": "",
                "serviceClasses": "RFJY"
            },
            "departureDate": {
                "dateUtc": "2020-03-30T03:10:00.000Z",
                "dateLocal": "2020-03-30T11:10:00.000"
            },
            "operatingCarrierFsCode": "DP*",
            "flightId": "1035404767",
            "departureAirportFsCode": "CAN",
            "flightNumber": "221",
            "status": "S",
            "delays": {
                "arrivalGateDelayMinutes": "235",
                "departureGateDelayMinutes": "279"
            },
            "operationalTimes": {
                "estimatedGateDeparture": {
                    "dateLocal": "2020-03-30T15:49:00.000",
                    "dateUtc": "2020-03-30T07:49:00.000Z"
                },
                "scheduledGateDeparture": {
                    "dateUtc": "2020-03-30T03:10:00.000Z",
                    "dateLocal": "2020-03-30T11:10:00.000"
                },
                "estimatedGateArrival": {
                    "dateUtc": "2020-03-30T17:20:00.000Z",
                    "dateLocal": "2020-03-30T20:20:00.000"
                },
                "scheduledGateArrival": {
                    "dateUtc": "2020-03-30T13:25:00.000Z",
                    "dateLocal": "2020-03-30T16:25:00.000"
                },
                "publishedDeparture": {
                    "dateUtc": "2020-03-30T03:10:00.000Z",
                    "dateLocal": "2020-03-30T11:10:00.000"
                },
                "publishedArrival": {
                    "dateUtc": "2020-03-30T13:25:00.000Z",
                    "dateLocal": "2020-03-30T16:25:00.000"
                }
            },
            "primaryCarrierFsCode": "DP*",
            "airportResources": {
                "departureTerminal": "2",
                "arrivalTerminal": "F"
            },
            "codeshares": {
                "codeshare": [
                    {
                        "relationship": "L",
                        "fsCode": "CZ",
                        "flightNumber": "7201"
                    }
                ]
            },
            "flightEquipment": {
                "scheduledEquipmentIataCode": "77W",
                "actualEquipmentIataCode": "773"
            }
        },
        "dataSource": "Airport",
        "dateTimeRecorded": "2020-03-30T07:29:31.207Z",
        "event": {
            "type": "TIME_ADJUSTMENT"
        }
    }
}`
	assert.NotPanics(t, func() { collectors.NewCollectorsManagerP() }, "Collectors manager failed")
	collector, ok := collectors.NewCollectorsManagerP().Collectors[collectors.FlightStats]
	assert.True(t, ok, "Flight stats collector not found")

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
			Airport:             "CAN",
			AirlineCode:         "SU",
			FlightNumber:        "221",
			FlightDate:          "2020-03-30",
			Direction:           "departure",
			TimeActual:          "2020-03-30 15:49:00",
			TimeScheduled:       "2020-03-30 11:10:00",
			Status:              "wait",
			Gate:                "",
			Terminal:            "2",
			CheckInDesks:        "",
			BaggageCarousels:    "",
			Diverted:            false,
			DivertedAirportCode: "",
			RoutePointFrom:      "CAN",
			RoutePointTo:        "SVO",
			Source:              "flight-stats",
		},
		{
			Airport:             "SVO",
			AirlineCode:         "SU",
			FlightNumber:        "221",
			FlightDate:          "2020-03-30",
			Direction:           "arrival",
			TimeActual:          "2020-03-30 20:20:00",
			TimeScheduled:       "2020-03-30 16:25:00",
			Status:              "wait",
			Gate:                "",
			Terminal:            "F",
			CheckInDesks:        "",
			BaggageCarousels:    "",
			Diverted:            false,
			DivertedAirportCode: "",
			RoutePointFrom:      "CAN",
			RoutePointTo:        "SVO",
			Source:              "flight-stats",
		},
		{
			Airport:             "CAN",
			AirlineCode:         "CZ",
			FlightNumber:        "7201",
			FlightDate:          "2020-03-30",
			Direction:           "departure",
			TimeActual:          "2020-03-30 15:49:00",
			TimeScheduled:       "2020-03-30 11:10:00",
			Status:              "wait",
			Gate:                "",
			Terminal:            "2",
			CheckInDesks:        "",
			BaggageCarousels:    "",
			Diverted:            false,
			DivertedAirportCode: "",
			RoutePointFrom:      "CAN",
			RoutePointTo:        "SVO",
			Source:              "flight-stats",
		},
		{
			Airport:             "SVO",
			AirlineCode:         "CZ",
			FlightNumber:        "7201",
			FlightDate:          "2020-03-30",
			Direction:           "arrival",
			TimeActual:          "2020-03-30 20:20:00",
			TimeScheduled:       "2020-03-30 16:25:00",
			Status:              "wait",
			Gate:                "",
			Terminal:            "F",
			CheckInDesks:        "",
			BaggageCarousels:    "",
			Diverted:            false,
			DivertedAirportCode: "",
			RoutePointFrom:      "CAN",
			RoutePointTo:        "SVO",
			Source:              "flight-stats",
		},
	}

	assertpb.Equal(t, expectedStatuses, statuses)

}
