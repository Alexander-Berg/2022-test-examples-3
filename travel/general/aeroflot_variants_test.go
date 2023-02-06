package aeroflotvariants

import (
	"sort"
	"testing"

	"github.com/stretchr/testify/assert"

	dto "a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/DTO"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/aeroflot_variants/format"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/segment"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/timezone"
	storageCache "a.yandex-team.ru/travel/avia/shared_flights/api/internal/storage"
	"a.yandex-team.ru/travel/avia/shared_flights/api/pkg/structs"
	"a.yandex-team.ru/travel/avia/shared_flights/lib/go/dtutil"
	"a.yandex-team.ru/travel/proto/dicts/rasp"
	"a.yandex-team.ru/travel/proto/shared_flights/snapshots"
)

type resolvedFlightData struct {
	// first segment or direct flight
	fp1             structs.FlightPattern
	fb1             structs.FlightBase
	depDateIndices1 []int
	// second segment (not filled in for the direct flighs)
	fp2             structs.FlightPattern
	fb2             structs.FlightBase
	depDateIndices2 []int
	hasBannedDates  bool
}

func resolve(fdatas []flightData) []resolvedFlightData {
	result := make([]resolvedFlightData, 0, len(fdatas))
	for _, fdata := range fdatas {
		resolved := resolvedFlightData{
			fp1:             *fdata.fp1,
			fb1:             *fdata.fb1,
			depDateIndices1: fdata.depDateIndices1,
			depDateIndices2: fdata.depDateIndices2,
			hasBannedDates:  fdata.hasBannedDates,
		}
		if fdata.fp2 != nil {
			resolved.fp2 = *fdata.fp2
		}
		if fdata.fb2 != nil {
			resolved.fb2 = *fdata.fb2
		}
		result = append(result, resolved)
	}
	sort.SliceStable(result, func(i, j int) bool {
		timeDiff := result[i].fb1.DepartureTimeScheduled - result[j].fb1.DepartureTimeScheduled
		if timeDiff != 0 {
			return timeDiff < 0
		}
		return result[i].fp1.MarketingFlightNumber < result[j].fp1.MarketingFlightNumber
	})
	return result
}

func Test_TestAeroflotVariants_GetFlightsForward(t *testing.T) {
	helper := serviceTestHelper(t)

	// On Sunday there is a direct flight SVX-JFK, should get only that flight
	directFlights, err := helper.Service.getFlightsForward(
		helper.Service.FlightStorage(),
		int64(helper.Station.SVX.Station.Id),
		int64(helper.Station.JFK.Station.Id),
		dtutil.DateCache.IndexOfIntDateP(20210711),
		dtutil.DateCache.IndexOfIntDateP(20210712),
		"",
		false,
	)
	assert.NoError(t, err)
	assert.Equal(
		t,
		resolve([]flightData{
			flightData{
				fp1:             helper.FlightPattern.SVXJFK,
				fb1:             &helper.FlightBase.SVXJFK,
				depDateIndices1: []int{dtutil.DateCache.IndexOfIntDateP(20210711)},
			},
		}),
		resolve(directFlights),
		"should be direct flights only, since there are direct flights",
	)

	// On Monday there is no direct flight SVX-JFK, hence the connection in VKO
	connectingFlights, err := helper.Service.getFlightsForward(
		helper.Service.FlightStorage(),
		int64(helper.Station.SVX.Station.Id),
		int64(helper.Station.JFK.Station.Id),
		dtutil.DateCache.IndexOfIntDateP(20210712),
		dtutil.DateCache.IndexOfIntDateP(20210712),
		"",
		false,
	)
	assert.NoError(t, err)
	assert.Equal(
		t,
		resolve([]flightData{
			{
				fp1:             helper.FlightPattern.SVXVKO,
				fb1:             &helper.FlightBase.SVXVKO,
				depDateIndices1: []int{dtutil.DateCache.IndexOfIntDateP(20210712)},
				fp2:             helper.FlightPattern.VKOJFK,
				fb2:             &helper.FlightBase.VKOJFK,
				depDateIndices2: []int{dtutil.DateCache.IndexOfIntDateP(20210712)},
			},
			{
				fp1:             helper.FlightPattern.SVXVKO2,
				fb1:             &helper.FlightBase.SVXVKO2,
				depDateIndices1: []int{dtutil.DateCache.IndexOfIntDateP(20210712)},
				fp2:             helper.FlightPattern.VKOJFK,
				fb2:             &helper.FlightBase.VKOJFK,
				depDateIndices2: []int{dtutil.DateCache.IndexOfIntDateP(20210713)},
			},
		}),
		resolve(connectingFlights),
		"should be connecting flights only, since there are no direct flights",
	)
}

var popularFlights = map[string]format.Flight{
	"2021-07-12 SU6666 SVX-VKO": {
		TitledFlight: dto.TitledFlight{
			FlightID: dto.FlightID{
				AirlineID: 26,
				Number:    "6666",
			},
			Title: "SU 6666",
		},
		DepartureDatetime: "2021-07-12T12:34:00+05:00",
		DepartureTerminal: "B",
		DepartureStation:  100,
		ArrivalDatetime:   "2021-07-12T13:45:00+03:00",
		ArrivalTerminal:   "C",
		ArrivalStation:    9600215,
	},
	"2021-07-12 FV7777 VKO-JFK": {
		TitledFlight: dto.TitledFlight{
			FlightID: dto.FlightID{
				AirlineID: 8565,
				Number:    "7777",
			},
			Title: "FV 7777",
		},
		DepartureDatetime: "2021-07-12T15:50:00+03:00",
		DepartureStation:  9600215,
		ArrivalDatetime:   "2021-07-12T17:25:00-04:00",
		ArrivalTerminal:   "A",
		ArrivalStation:    101,
	},
	"2021-07-12 SU6667 SVX-VKO": {
		TitledFlight: dto.TitledFlight{
			FlightID: dto.FlightID{
				AirlineID: 26,
				Number:    "6667",
			},
			Title: "SU 6667",
		},
		DepartureDatetime: "2021-07-12T14:01:00+05:00",
		DepartureTerminal: "B",
		DepartureStation:  100,
		ArrivalDatetime:   "2021-07-12T16:51:00+03:00",
		ArrivalTerminal:   "C",
		ArrivalStation:    9600215,
	},
	"2021-07-13 FV7777 VKO-JFK": {
		TitledFlight: dto.TitledFlight{
			FlightID: dto.FlightID{
				AirlineID: 8565,
				Number:    "7777",
			},
			Title: "FV 7777",
		},
		DepartureDatetime: "2021-07-13T15:50:00+03:00",
		DepartureStation:  9600215,
		ArrivalDatetime:   "2021-07-13T17:25:00-04:00",
		ArrivalTerminal:   "A",
		ArrivalStation:    101,
	},
}

func Test_TestAeroflotVariants_GetFlightsInternal(t *testing.T) {
	helper := serviceTestHelper(t)

	// Should be no flights since there is no backward flights in the storage yet
	noFlights, err := helper.Service.getVariantsInternal(
		helper.Service.FlightStorage(),
		helper.Station.SVX.Station.Id,
		helper.Station.JFK.Station.Id,
		"2021-07-11",
		"2021-07-12",
		false, // isOneWay
		"",
		false,
		maxVariants,
	)
	assert.NoError(t, err)
	assert.Equal(
		t,
		[]format.Variant{},
		noFlights,
		"should be no flights, since there are no backward flights",
	)

	// But one-way request should return some flights
	oneWayFlights, err := helper.Service.getVariantsInternal(
		helper.Service.FlightStorage(),
		helper.Station.SVX.Station.Id,
		helper.Station.JFK.Station.Id,
		"2021-07-11",
		"2021-07-12",
		true, // isOneWay
		"",
		false,
		maxVariants,
	)
	assert.NoError(t, err)
	assert.Equal(
		t,
		1,
		len(oneWayFlights),
		"should be some one-way flights",
	)

	helper.AddBackwardFlights(t)
	// On 2021-07-13 there is a direct backward flight JFK-SVX, should get only that flight for the backward options
	directFlights, err := helper.Service.getVariantsInternal(
		helper.Service.FlightStorage(),
		helper.Station.SVX.Station.Id,
		helper.Station.JFK.Station.Id,
		"2021-07-11",
		"2021-07-12",
		false, // isOneWay
		"",
		false,
		maxVariants,
	)
	assert.NoError(t, err)
	assert.Equal(
		t,
		[]format.Variant{
			{
				Forward: format.Slice{
					Flights: []format.Flight{
						{
							TitledFlight: dto.TitledFlight{
								FlightID: dto.FlightID{
									AirlineID: 26,
									Number:    "5555",
								},
								Title: "SU 5555",
							},
							DepartureDatetime: "2021-07-11T05:00:00+05:00",
							DepartureTerminal: "A",
							DepartureStation:  100,
							ArrivalDatetime:   "2021-07-11T14:00:00-04:00",
							ArrivalTerminal:   "B",
							ArrivalStation:    101,
						},
					},
					Duration: 1080,
				},
				Backward: format.Slice{
					Flights: []format.Flight{
						{
							TitledFlight: dto.TitledFlight{
								FlightID: dto.FlightID{
									AirlineID: 26,
									Number:    "5588",
								},
								Title: "SU 5588",
							},
							DepartureDatetime: "2021-07-12T19:00:00-04:00",
							DepartureTerminal: "1",
							DepartureStation:  101,
							ArrivalDatetime:   "2021-07-13T11:00:00+05:00",
							ArrivalTerminal:   "A",
							ArrivalStation:    100,
						},
					},
					Duration: 420,
				},
			},
		},
		directFlights,
		"should be direct flights, since there are some direct flights available",
	)

	// After 2021-07-13 there is no direct flight JFK-SVX, hence the connection in VKO
	connectingFlights, err := helper.Service.getVariantsInternal(
		helper.Service.FlightStorage(),
		helper.Station.SVX.Station.Id,
		helper.Station.JFK.Station.Id,
		"2021-07-12",
		"2021-07-12",
		false, // isOneWay
		"",
		false,
		maxVariants,
	)
	assert.NoError(t, err)
	expectedBackwardFlights := format.Slice{
		Flights: []format.Flight{
			{
				TitledFlight: dto.TitledFlight{
					FlightID: dto.FlightID{
						AirlineID: 8565,
						Number:    "7788",
					},
					Title: "FV 7788",
				},
				DepartureDatetime: "2021-07-17T19:30:00-04:00",
				DepartureStation:  101,
				ArrivalDatetime:   "2021-07-18T07:25:00+03:00",
				ArrivalTerminal:   "C",
				ArrivalStation:    9600215,
			},
			{
				TitledFlight: dto.TitledFlight{
					FlightID: dto.FlightID{
						AirlineID: 26,
						Number:    "6688",
					},
					Title: "SU 6688",
				},
				DepartureDatetime: "2021-07-18T11:00:00+03:00",
				DepartureStation:  9600215,
				DepartureTerminal: "C",
				ArrivalDatetime:   "2021-07-18T15:00:00+05:00",
				ArrivalStation:    100,
				ArrivalTerminal:   "B",
			},
		},
		Duration: 630,
	}
	expectedConnectingFlights := []format.Variant{
		{
			Forward: format.Slice{
				Flights: []format.Flight{
					popularFlights["2021-07-12 SU6666 SVX-VKO"],
					popularFlights["2021-07-12 FV7777 VKO-JFK"],
				},
				Duration: 831,
			},
			Backward: expectedBackwardFlights,
		},
		{
			Forward: format.Slice{
				Flights: []format.Flight{
					popularFlights["2021-07-12 SU6667 SVX-VKO"],
					popularFlights["2021-07-13 FV7777 VKO-JFK"],
				},
				Duration: 2184,
			},
			Backward: expectedBackwardFlights,
		},
	}
	sort.Slice(connectingFlights, func(i, j int) bool {
		return connectingFlights[i].Forward.Duration < connectingFlights[j].Forward.Duration
	})
	assert.Equal(
		t,
		expectedConnectingFlights,
		connectingFlights,
		"should be connecting flights, since there are no direct flights available",
	)
}

func Test_TestAeroflotVariants_Multistation(t *testing.T) {
	helper := serviceTestHelper(t)

	response, err := helper.Service.GetAeroflotConnectingVariants(
		[]*snapshots.TStationWithCodes{helper.Station.SVX, helper.Station.VKO},
		[]*snapshots.TStationWithCodes{helper.Station.JFK},
		"2021-07-12",
		"2021-07-12",
		true, // isOneWay
		"",
		false, // showBanned
	)
	assert.NoError(t, err)
	assert.Equal(
		t,
		3,
		len(response.Variants),
		"should be some variants",
	)

	assert.Equal(
		t,
		[]format.Variant{
			{
				Forward: format.Slice{
					Flights: []format.Flight{
						popularFlights["2021-07-12 FV7777 VKO-JFK"],
					},
					Duration: 515,
				},
			},
			{
				Forward: format.Slice{
					Flights: []format.Flight{
						popularFlights["2021-07-12 SU6666 SVX-VKO"],
						popularFlights["2021-07-12 FV7777 VKO-JFK"],
					},
					Duration: 831,
				},
			},
			{
				Forward: format.Slice{
					Flights: []format.Flight{
						popularFlights["2021-07-12 SU6667 SVX-VKO"],
						popularFlights["2021-07-13 FV7777 VKO-JFK"],
					},
					Duration: 2184,
				},
			},
		},
		response.Variants,
		"variants with direct flights should be listed first regardless of the order of the stations",
	)
}

type ServiceHelper struct {
	Service  *aeroflotVariantsServiceImpl
	Storage  *storageCache.Storage
	Timezone struct {
		SVX *rasp.TTimeZone
		JFK *rasp.TTimeZone
		VKO *rasp.TTimeZone
	}
	Station struct {
		SVX *snapshots.TStationWithCodes
		JFK *snapshots.TStationWithCodes
		VKO *snapshots.TStationWithCodes
	}
	FlightBase struct {
		SVXJFK  structs.FlightBase
		SVXVKO  structs.FlightBase
		SVXVKO2 structs.FlightBase
		VKOJFK  structs.FlightBase
	}
	FlightPattern struct {
		SVXJFK  *structs.FlightPattern
		SVXVKO  *structs.FlightPattern
		SVXVKO2 *structs.FlightPattern
		VKOJFK  *structs.FlightPattern
	}
}

func serviceTestHelper(t *testing.T) *ServiceHelper {
	var h ServiceHelper
	storage := storageCache.NewStorageWithStartDate("2021-07-01")
	h.Storage = storage
	segment.SetGlobalStartDateIndex(dtutil.DateCache.IndexOfStringDateP("2021-07-01"))
	tz := timezone.NewTimeZoneUtil(storage.Timezones(), storage.Stations())
	h.Service = &aeroflotVariantsServiceImpl{
		Storage:      storage,
		TimeZoneUtil: tz,
	}

	// TIMEZONES
	h.Timezone.SVX = &rasp.TTimeZone{Id: 200, Code: "Asia/Yekaterinburg"}
	storage.Timezones().PutTimezone(h.Timezone.SVX)

	h.Timezone.JFK = &rasp.TTimeZone{Id: 201, Code: "America/New_York"}
	storage.Timezones().PutTimezone(h.Timezone.JFK)

	h.Timezone.VKO = &rasp.TTimeZone{Id: 202, Code: "Europe/Moscow"}
	storage.Timezones().PutTimezone(h.Timezone.VKO)

	// STATIONS
	h.Station.SVX = &snapshots.TStationWithCodes{
		Station: &rasp.TStation{
			Id:         100,
			TimeZoneId: 200,
		},
		IataCode: "SVX",
	}
	storage.PutStation(h.Station.SVX)

	h.Station.JFK = &snapshots.TStationWithCodes{
		Station: &rasp.TStation{
			Id:         101,
			TimeZoneId: 201,
		},
		IataCode: "JFK",
	}
	storage.PutStation(h.Station.JFK)

	h.Station.VKO = &snapshots.TStationWithCodes{
		Station: &rasp.TStation{
			Id:         9600215,
			TimeZoneId: 202,
		},
		IataCode: "VKO",
	}
	storage.PutStation(h.Station.VKO)

	// FLIGHT BASE
	h.FlightBase.SVXJFK = structs.FlightBase{
		ID:                     301,
		OperatingCarrier:       26,
		OperatingCarrierCode:   "SU",
		OperatingFlightNumber:  "5555",
		DepartureStation:       100,
		DepartureStationCode:   "SVX",
		DepartureTimeScheduled: 500,
		DepartureTerminal:      "A",
		ArrivalStation:         101,
		ArrivalStationCode:     "JFK",
		ArrivalTimeScheduled:   1400,
		ArrivalTerminal:        "B",
		LegNumber:              1,
	}
	storage.PutFlightBase(h.FlightBase.SVXJFK)

	h.FlightBase.SVXVKO = structs.FlightBase{
		ID:                     302,
		OperatingCarrier:       26,
		OperatingCarrierCode:   "SU",
		OperatingFlightNumber:  "6666",
		DepartureStation:       100,
		DepartureStationCode:   "SVX",
		DepartureTimeScheduled: 1234,
		DepartureTerminal:      "B",
		ArrivalStation:         9600215,
		ArrivalStationCode:     "VKO",
		ArrivalTimeScheduled:   1345,
		ArrivalTerminal:        "C",
		LegNumber:              1,
	}
	storage.PutFlightBase(h.FlightBase.SVXVKO)

	h.FlightBase.SVXVKO2 = structs.FlightBase{
		ID:                     30201,
		OperatingCarrier:       26,
		OperatingCarrierCode:   "SU",
		OperatingFlightNumber:  "6667",
		DepartureStation:       100,
		DepartureStationCode:   "SVX",
		DepartureTimeScheduled: 1401,
		DepartureTerminal:      "B",
		ArrivalStation:         9600215,
		ArrivalStationCode:     "VKO",
		ArrivalTimeScheduled:   1651,
		ArrivalTerminal:        "C",
		LegNumber:              1,
	}
	storage.PutFlightBase(h.FlightBase.SVXVKO2)

	// Breaks MCT (minimum connection time), will be ignored
	fbSVXVKO3 := structs.FlightBase{
		ID:                     30202,
		OperatingCarrier:       26,
		OperatingCarrierCode:   "SU",
		OperatingFlightNumber:  "6666",
		DepartureStation:       100,
		DepartureStationCode:   "SVX",
		DepartureTimeScheduled: 1234,
		DepartureTerminal:      "B",
		ArrivalStation:         9600215,
		ArrivalStationCode:     "VKO",
		ArrivalTimeScheduled:   1425,
		ArrivalTerminal:        "C",
		LegNumber:              1,
	}
	storage.PutFlightBase(fbSVXVKO3)

	h.FlightBase.VKOJFK = structs.FlightBase{
		ID:                     303,
		OperatingCarrier:       8565,
		OperatingCarrierCode:   "FV",
		OperatingFlightNumber:  "7777",
		LegNumber:              1,
		DepartureStation:       9600215,
		DepartureStationCode:   "VKO",
		DepartureTimeScheduled: 1550,
		ArrivalStation:         101,
		ArrivalStationCode:     "JFK",
		ArrivalTimeScheduled:   1725,
		ArrivalTerminal:        "A",
	}
	storage.PutFlightBase(h.FlightBase.VKOJFK)

	// FLIGHT PATTERN
	h.FlightPattern.SVXJFK = &structs.FlightPattern{
		ID:                    3001,
		FlightBaseID:          301,
		OperatingFromDate:     "20210701",
		OperatingUntilDate:    "20220601",
		OperatingOnDays:       234567,
		MarketingCarrier:      26,
		MarketingCarrierCode:  "SU",
		MarketingFlightNumber: "5555",
		LegNumber:             1,
	}
	storage.PutFlightPattern(*h.FlightPattern.SVXJFK)

	h.FlightPattern.SVXVKO = &structs.FlightPattern{
		ID:                    3002,
		FlightBaseID:          302,
		OperatingFromDate:     "20210701",
		OperatingUntilDate:    "20220601",
		OperatingOnDays:       123,
		MarketingCarrier:      26,
		MarketingCarrierCode:  "SU",
		MarketingFlightNumber: "6666",
		LegNumber:             1,
	}
	storage.PutFlightPattern(*h.FlightPattern.SVXVKO)

	h.FlightPattern.SVXVKO2 = &structs.FlightPattern{
		ID:                    300201,
		FlightBaseID:          h.FlightBase.SVXVKO2.ID,
		OperatingFromDate:     "20210712",
		OperatingUntilDate:    "20210712",
		OperatingOnDays:       123,
		MarketingCarrier:      26,
		MarketingCarrierCode:  "SU",
		MarketingFlightNumber: "6667",
		LegNumber:             1,
	}
	storage.PutFlightPattern(*h.FlightPattern.SVXVKO2)

	fpSVXVKO3 := structs.FlightPattern{
		ID:                    300202,
		FlightBaseID:          fbSVXVKO3.ID,
		OperatingFromDate:     "20210701",
		OperatingUntilDate:    "20220601",
		OperatingOnDays:       123,
		MarketingCarrier:      26,
		MarketingCarrierCode:  "SU",
		MarketingFlightNumber: "6666",
		LegNumber:             1,
	}
	storage.PutFlightPattern(fpSVXVKO3)

	h.FlightPattern.VKOJFK = &structs.FlightPattern{
		ID:                    3003,
		FlightBaseID:          303,
		OperatingFromDate:     "2021-07-01",
		OperatingUntilDate:    "2022-06-01",
		OperatingOnDays:       1267,
		MarketingCarrier:      8565,
		MarketingCarrierCode:  "FV",
		MarketingFlightNumber: "7777",
		LegNumber:             1,
	}
	storage.PutFlightPattern(*h.FlightPattern.VKOJFK)

	// FLIGHT BOARD
	err := storage.UpdateCacheDependentData()
	assert.NoError(t, err, "cannot update cache dependent data")

	// Aeroflot Cache
	err = storage.BuildAeroflotCache()
	assert.NoError(t, err, "cannot build aeroflot flights cache")

	return &h
}

func (h *ServiceHelper) AddBackwardFlights(t *testing.T) {
	fbJFKSVX := structs.FlightBase{
		ID:                     601,
		OperatingCarrier:       26,
		OperatingCarrierCode:   "SU",
		OperatingFlightNumber:  "5588",
		DepartureStation:       101,
		DepartureStationCode:   "JFK",
		DepartureTimeScheduled: 1900,
		DepartureTerminal:      "1",
		ArrivalStation:         100,
		ArrivalStationCode:     "SVX",
		ArrivalTimeScheduled:   1100,
		ArrivalTerminal:        "A",
		LegNumber:              1,
	}
	h.Storage.PutFlightBase(fbJFKSVX)

	fbVKOSVX := structs.FlightBase{
		ID:                     602,
		OperatingCarrier:       26,
		OperatingCarrierCode:   "SU",
		OperatingFlightNumber:  "6688",
		DepartureStation:       9600215,
		DepartureStationCode:   "VKO",
		DepartureTimeScheduled: 1100,
		DepartureTerminal:      "C",
		ArrivalStation:         100,
		ArrivalStationCode:     "SVX",
		ArrivalTimeScheduled:   1500,
		ArrivalTerminal:        "B",
		LegNumber:              1,
	}
	h.Storage.PutFlightBase(fbVKOSVX)

	fbJFKVKO := structs.FlightBase{
		ID:                     603,
		OperatingCarrier:       8565,
		OperatingCarrierCode:   "FV",
		OperatingFlightNumber:  "7788",
		LegNumber:              1,
		DepartureStation:       101,
		DepartureStationCode:   "JFK",
		DepartureTimeScheduled: 1930,
		ArrivalStation:         9600215,
		ArrivalStationCode:     "VKO",
		ArrivalTimeScheduled:   725,
		ArrivalTerminal:        "C",
	}
	h.Storage.PutFlightBase(fbJFKVKO)

	// FLIGHT PATTERN
	fpJFKSVX := structs.FlightPattern{
		ID:                    6001,
		FlightBaseID:          fbJFKSVX.ID,
		OperatingFromDate:     "20210701",
		OperatingUntilDate:    "20210712",
		OperatingOnDays:       1234567,
		MarketingCarrier:      26,
		MarketingCarrierCode:  "SU",
		MarketingFlightNumber: "5588",
		LegNumber:             1,
		ArrivalDayShift:       1,
	}
	h.Storage.PutFlightPattern(fpJFKSVX)

	fpVKOSVX := structs.FlightPattern{
		ID:                    6002,
		FlightBaseID:          fbVKOSVX.ID,
		OperatingFromDate:     "20210701",
		OperatingUntilDate:    "20220601",
		OperatingOnDays:       1237,
		MarketingCarrier:      26,
		MarketingCarrierCode:  "SU",
		MarketingFlightNumber: "6688",
		LegNumber:             1,
	}
	h.Storage.PutFlightPattern(fpVKOSVX)

	fpJFKVKO := structs.FlightPattern{
		ID:                    6003,
		FlightBaseID:          fbJFKVKO.ID,
		OperatingFromDate:     "2021-07-01",
		OperatingUntilDate:    "2022-06-01",
		OperatingOnDays:       1267,
		MarketingCarrier:      8565,
		MarketingCarrierCode:  "FV",
		MarketingFlightNumber: "7788",
		LegNumber:             1,
		ArrivalDayShift:       1,
	}
	h.Storage.PutFlightPattern(fpJFKVKO)

	// FLIGHT BOARD
	err := h.Storage.UpdateCacheDependentData()
	assert.NoError(t, err, "cannot update cache dependent data")

	// Aeroflot Cache
	err = h.Storage.BuildAeroflotCache()
	assert.NoError(t, err, "cannot rebuild aeroflot flights cache")
}
