package flight

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/flightdata"
	dto "a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/DTO"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/carrier"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/flightstatus"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/timezone"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/storage"
	"a.yandex-team.ru/travel/avia/shared_flights/api/pkg/structs"
	"a.yandex-team.ru/travel/avia/shared_flights/lib/go/direction"
	"a.yandex-team.ru/travel/avia/shared_flights/lib/go/dtutil"
	"a.yandex-team.ru/travel/proto/dicts/rasp"
	"a.yandex-team.ru/travel/proto/shared_flights/snapshots"
)

func Test_getDatesListOfFirstLeg(t *testing.T) {
	type args struct {
		firstLegFlightPatterns []*structs.FlightPattern
		referenceDate          dtutil.IntDate
		searchDirection        tSearchDirection
		limit                  int
	}
	tests := []struct {
		name string
		args args
		want []dtutil.IntDate
	}{
		{
			"empty",
			args{
				nil,
				20200501,
				false,
				10,
			},
			nil,
		},
		{
			"single pattern",
			args{
				[]*structs.FlightPattern{{
					OperatingFromDate:  "2020-01-01",
					OperatingUntilDate: "2020-08-01",
					OperatingOnDays:    1245,
				}},
				20200501,
				false,
				10,
			},
			[]dtutil.IntDate{
				20200501, 20200504, 20200505, 20200507, 20200508, 20200511, 20200512, 20200514, 20200515, 20200518,
			},
		},
		{
			"single pattern / reverse order",
			args{
				[]*structs.FlightPattern{{
					OperatingFromDate:  "2020-01-01",
					OperatingUntilDate: "2020-08-01",
					OperatingOnDays:    1245,
				}},
				20200501,
				true,
				10,
			},
			[]dtutil.IntDate{
				20200501, 20200430, 20200428, 20200427, 20200424, 20200423, 20200421, 20200420, 20200417, 20200416,
			},
		},
		{
			"two patterns with extra on-days",
			args{
				[]*structs.FlightPattern{{
					OperatingFromDate:  "2020-01-01",
					OperatingUntilDate: "2020-08-01",
					OperatingOnDays:    1245,
				}, {
					OperatingFromDate:  "2020-05-07",
					OperatingUntilDate: "2020-08-01",
					OperatingOnDays:    367,
				}},
				20200501,
				false,
				10,
			},
			[]dtutil.IntDate{
				20200501, 20200504, 20200505, 20200507, 20200508, 20200509, 20200510, 20200511, 20200512, 20200513,
			},
		},
		{
			"single pattern far form request",
			args{
				[]*structs.FlightPattern{{
					OperatingFromDate:  "2020-07-01",
					OperatingUntilDate: "2020-08-01",
					OperatingOnDays:    1245,
				}},
				20200501,
				false,
				10,
			},
			[]dtutil.IntDate{
				20200702, 20200703, 20200706, 20200707, 20200709, 20200710, 20200713, 20200714, 20200716, 20200717,
			},
		},
		{
			"two patterns, sparse data",
			args{
				[]*structs.FlightPattern{{
					OperatingFromDate:  "2020-07-01",
					OperatingUntilDate: "2020-07-08",
					OperatingOnDays:    1245,
				}, {
					OperatingFromDate:  "2020-07-15",
					OperatingUntilDate: "2020-07-23",
					OperatingOnDays:    1245,
				}},
				20200501,
				false,
				10,
			},
			[]dtutil.IntDate{
				20200702, 20200703, 20200706, 20200707, 20200716, 20200717, 20200720, 20200721, 20200723,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			assert.Equal(t, tt.want, getDatesListOfFirstLeg(tt.args.firstLegFlightPatterns, tt.args.referenceDate, tt.args.searchDirection, tt.args.limit))
		})
	}
}

func Test_sortDates(t *testing.T) {
	type args struct {
		before []dtutil.IntDate
		after  []dtutil.IntDate
	}
	tests := []struct {
		name string
		args args
		want []dtutil.IntDate
	}{
		{
			"empty",
			args{
				before: []dtutil.IntDate{},
				after:  []dtutil.IntDate{},
			},
			[]dtutil.IntDate{},
		},
		{
			"single element",
			args{
				before: []dtutil.IntDate{1},
				after:  []dtutil.IntDate{},
			},
			[]dtutil.IntDate{1},
		},
		{
			"only before",
			args{
				before: []dtutil.IntDate{5, 1, 4},
				after:  []dtutil.IntDate{},
			},
			[]dtutil.IntDate{1, 4, 5},
		},
		{
			"only after",
			args{
				before: []dtutil.IntDate{},
				after:  []dtutil.IntDate{5, 1, 4},
			},
			[]dtutil.IntDate{1, 4, 5},
		},
		{
			"after and before",
			args{
				before: []dtutil.IntDate{3, 2, 6},
				after:  []dtutil.IntDate{5, 1, 4},
			},
			[]dtutil.IntDate{1, 2, 3, 4, 5, 6},
		},
		{
			"with duplicates",
			args{
				before: []dtutil.IntDate{3, 2, 6, 5, 2, 3},
				after:  []dtutil.IntDate{9, 7, 8, 3, 5, 1, 2, 2},
			},
			[]dtutil.IntDate{1, 2, 3, 5, 6, 7, 8, 9},
		},
		{
			"with a single duplicate at the end",
			args{
				before: []dtutil.IntDate{2, 4, 9},
				after:  []dtutil.IntDate{9},
			},
			[]dtutil.IntDate{2, 4, 9},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			assert.Equal(t, tt.want, sortDates(tt.args.before, tt.args.after))
		})
	}
}

func makeStoreFlightRange() (*storage.Storage, []*flightdata.FlightData) {
	store := storage.NewStorage()

	store.Timezones().PutTimezone(&rasp.TTimeZone{
		Id:   5,
		Code: "Asia/Yekaterinburg",
	})
	store.Timezones().PutTimezone(&rasp.TTimeZone{
		Id:   0,
		Code: "Europe/Moscow",
	})
	store.PutStation(&snapshots.TStationWithCodes{
		Station: &rasp.TStation{
			Id:         9600370,
			TimeZoneId: 5,
		},
		IataCode:   "SVX",
		IcaoCode:   "USSS",
		SirenaCode: "ЕКБ",
	})

	store.PutStation(&snapshots.TStationWithCodes{
		Station: &rasp.TStation{
			Id:         9600216,
			TimeZoneId: 0,
		},
		IataCode:   "DME",
		IcaoCode:   "UDDD",
		SirenaCode: "ДМД",
	})

	store.PutStation(&snapshots.TStationWithCodes{
		Station: &rasp.TStation{
			Id:         9623547,
			TimeZoneId: 0,
		},
		IataCode:   "AER",
		IcaoCode:   "URSS",
		SirenaCode: "СОЧ",
	})
	tzutil := timezone.NewTimeZoneUtil(store.Timezones(), store.Stations())
	fb1 := structs.FlightBase{
		ID:                     1000001,
		OperatingCarrier:       26,
		OperatingCarrierCode:   "SU",
		OperatingFlightNumber:  "1234",
		LegNumber:              1,
		DepartureStation:       9600370,
		DepartureStationCode:   "SVX",
		DepartureTimeScheduled: 2000,
		ArrivalStation:         9600216,
		ArrivalStationCode:     "DME",
		ArrivalTimeScheduled:   502,
	}

	fb2 := structs.FlightBase{
		ID:                     2,
		OperatingCarrier:       26,
		OperatingCarrierCode:   "SU",
		OperatingFlightNumber:  "1234",
		LegNumber:              2,
		DepartureStation:       9600216,
		DepartureStationCode:   "DME",
		DepartureTimeScheduled: 2200,
		ArrivalStation:         9623547,
		ArrivalStationCode:     "AER",
		ArrivalTimeScheduled:   1620,
	}

	fp1 := operatingFlightPattern(structs.FlightPattern{
		ID:                 20001,
		OperatingFromDate:  "2020-02-12",
		OperatingUntilDate: "2020-09-15",
		OperatingOnDays:    1234567,
		ArrivalDayShift:    1,
	}, fb1)
	fp2 := operatingFlightPattern(structs.FlightPattern{
		ID:                 20001,
		OperatingFromDate:  "2020-02-13",
		OperatingUntilDate: "2020-09-16",
		OperatingOnDays:    1234567,
		ArrivalDayShift:    0,
		FlightDayShift:     1,
	}, fb2)

	segments := [...]*flightdata.FlightDataBase{
		flightdata.NewFlightDataBase(
			fb1,
			&fp1,
			nil,
			tzutil,
		),
		flightdata.NewFlightDataBase(
			fb2,
			&fp2,
			nil,
			tzutil,
		),
	}

	segmentsOnDays := [...]*flightdata.FlightData{
		{
			FlightDataBase:      segments[0],
			FlightDepartureDate: 20200402,
		},
		{
			FlightDataBase:      segments[0],
			FlightDepartureDate: 20200403,
		},
		{
			FlightDataBase:      segments[0],
			FlightDepartureDate: 20200404,
		},
		{
			FlightDataBase:      segments[0],
			FlightDepartureDate: 20200405,
		},
		{
			FlightDataBase:      segments[0],
			FlightDepartureDate: 20200406,
		},
		{
			FlightDataBase:      segments[0],
			FlightDepartureDate: 20200407,
		},
		{
			FlightDataBase:      segments[1],
			FlightDepartureDate: 20200403,
		},
		{
			FlightDataBase:      segments[1],
			FlightDepartureDate: 20200404,
		},
		{
			FlightDataBase:      segments[1],
			FlightDepartureDate: 20200405,
		},
		{
			FlightDataBase:      segments[1],
			FlightDepartureDate: 20200406,
		},
		{
			FlightDataBase:      segments[1],
			FlightDepartureDate: 20200407,
		},
		{
			FlightDataBase:      segments[1],
			FlightDepartureDate: 20200408,
		},
	}

	store.PutFlightBase(fb1)
	store.PutFlightBase(fb2)

	store.PutFlightPattern(fp1)
	store.PutFlightPattern(fp2)

	return store, segmentsOnDays[:]
}

func Test_cropFlightByLimit(t *testing.T) {
	_, segmentsOnDays := makeStoreFlightRange()
	type args struct {
		flightDataList    [][]*flightdata.FlightData
		flightStage       direction.Direction
		referenceDateTime time.Time
		limitBefore       int
		limitAfter        int
	}
	tests := []struct {
		name string
		args args
		want [][]*flightdata.FlightData
	}{
		{
			"by departure",
			args{
				flightDataList: [][]*flightdata.FlightData{
					{segmentsOnDays[0], segmentsOnDays[6]},
					{segmentsOnDays[1], segmentsOnDays[7]},
					{segmentsOnDays[2], segmentsOnDays[8]},
					{segmentsOnDays[3], segmentsOnDays[9]},
					{segmentsOnDays[4], segmentsOnDays[10]},
					{segmentsOnDays[5], segmentsOnDays[11]},
				},
				flightStage:       direction.DEPARTURE,
				referenceDateTime: time.Date(2020, 4, 5, 12, 0, 0, 0, time.UTC),
				limitBefore:       1,
				limitAfter:        1,
			},
			[][]*flightdata.FlightData{
				{segmentsOnDays[2], segmentsOnDays[8]},
				{segmentsOnDays[3], segmentsOnDays[9]},
			},
		},
		{
			"by arrival",
			args{
				flightDataList: [][]*flightdata.FlightData{
					{segmentsOnDays[0], segmentsOnDays[6]},
					{segmentsOnDays[1], segmentsOnDays[7]},
					{segmentsOnDays[2], segmentsOnDays[8]},
					{segmentsOnDays[3], segmentsOnDays[9]},
					{segmentsOnDays[4], segmentsOnDays[10]},
					{segmentsOnDays[5], segmentsOnDays[11]},
				},
				flightStage:       direction.ARRIVAL,
				referenceDateTime: time.Date(2020, 4, 5, 12, 0, 0, 0, time.UTC),
				limitBefore:       1,
				limitAfter:        1,
			},
			[][]*flightdata.FlightData{
				{segmentsOnDays[1], segmentsOnDays[7]},
				{segmentsOnDays[2], segmentsOnDays[8]},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			assert.Equal(t,
				tt.want,
				cropFlightByLimit(
					tt.args.flightDataList,
					tt.args.flightStage,
					tt.args.referenceDateTime,
					tt.args.limitBefore,
					tt.args.limitAfter),
			)
		})
	}
}

func Test_flightServiceImpl_GetFlightRange(t *testing.T) {
	store, _ := makeStoreFlightRange()
	tzutil := timezone.NewTimeZoneUtil(store.Timezones(), store.Stations())
	carrierService := carrier.NewCarrierService(store.CarrierStorage(), store.IataCorrector())

	type fields struct {
		Storage        *storage.Storage
		TimeZoneUtil   timezone.TimeZoneUtil
		CarrierService CarrierService
	}
	type args struct {
		carrier           CarrierParam
		flightNumber      string
		nationalVersion   string
		showBanned        bool
		nowDate           time.Time
		limitBefore       int
		limitAfter        int
		direction         direction.Direction
		referenceDateTime time.Time
	}
	tests := []struct {
		name         string
		fields       fields
		args         args
		wantResponse []*FlightSegment
		wantErr      bool
		wantErrMsg   string
	}{
		{
			"departure",
			fields{
				Storage:        store,
				TimeZoneUtil:   tzutil,
				CarrierService: carrierService,
			},
			args{
				carrier:           NewCarrierParamByText("SU"),
				flightNumber:      "1234",
				nationalVersion:   "ru",
				showBanned:        false,
				nowDate:           time.Now().UTC(),
				limitBefore:       1,
				limitAfter:        1,
				direction:         direction.DEPARTURE,
				referenceDateTime: time.Date(2020, 4, 5, 12, 0, 0, 0, time.UTC),
			},
			[]*FlightSegment{
				{
					CompanyIata:       "SU",
					CompanyRaspID:     26,
					Number:            "1234",
					Title:             "SU 1234",
					AirportFromIata:   "SVX",
					AirportFromRaspID: 9600370,
					DepartureDay:      "2020-04-04",
					DepartureTime:     "20:00:00",
					DepartureTzName:   "Asia/Yekaterinburg",
					DepartureUTC:      "2020-04-04 15:00:00",
					AirportToIata:     "AER",
					AirportToRaspID:   9623547,
					ArrivalDay:        "2020-04-05",
					ArrivalTime:       "16:20:00",
					ArrivalTzName:     "Europe/Moscow",
					ArrivalUTC:        "2020-04-05 13:20:00",
					Status: flightstatus.FlightStatus{
						Status:          "unknown",
						DepartureStatus: "unknown",
						ArrivalStatus:   "unknown",
						DepartureSource: "0",
						ArrivalSource:   "0",
					},
					FlightCodeshares: []dto.TitledFlight{},
					Segments: []*FlightSegment{
						{
							CompanyIata:       "SU",
							CompanyRaspID:     26,
							Number:            "1234",
							Title:             "SU 1234",
							AirportFromIata:   "SVX",
							AirportFromRaspID: 9600370,
							DepartureDay:      "2020-04-04",
							DepartureTime:     "20:00:00",
							DepartureTzName:   "Asia/Yekaterinburg",
							DepartureUTC:      "2020-04-04 15:00:00",
							DepartureTerminal: "",
							AirportToIata:     "DME",
							AirportToRaspID:   9600216,
							ArrivalDay:        "2020-04-05",
							ArrivalTime:       "05:02:00",
							ArrivalTzName:     "Europe/Moscow",
							ArrivalUTC:        "2020-04-05 02:02:00",
							ArrivalTerminal:   "",
							Status: flightstatus.FlightStatus{
								DepartureStatus: "unknown",
								ArrivalStatus:   "unknown",
								Status:          "unknown",
							},
							Segments: []*FlightSegment{},
							Source:   "",
							Banned:   "",
						},
						{
							CompanyIata:       "SU",
							CompanyRaspID:     26,
							Number:            "1234",
							Title:             "SU 1234",
							AirportFromIata:   "DME",
							AirportFromRaspID: 9600216,
							DepartureDay:      "2020-04-05",
							DepartureTime:     "22:00:00",
							DepartureTzName:   "Europe/Moscow",
							DepartureUTC:      "2020-04-05 19:00:00",
							DepartureTerminal: "",
							AirportToIata:     "AER",
							AirportToRaspID:   9623547,
							ArrivalDay:        "2020-04-05",
							ArrivalTime:       "16:20:00",
							ArrivalTzName:     "Europe/Moscow",
							ArrivalUTC:        "2020-04-05 13:20:00",
							ArrivalTerminal:   "",
							Status: flightstatus.FlightStatus{
								DepartureStatus: "unknown",
								ArrivalStatus:   "unknown",
								Status:          "unknown",
							},
							Segments: []*FlightSegment{},
							Source:   "",
							Banned:   "",
						},
					},
					Source: "",
					Banned: "",
				},
				{
					CompanyIata:       "SU",
					CompanyRaspID:     26,
					Number:            "1234",
					Title:             "SU 1234",
					AirportFromIata:   "SVX",
					AirportFromRaspID: 9600370,
					DepartureDay:      "2020-04-05",
					DepartureTime:     "20:00:00",
					DepartureTzName:   "Asia/Yekaterinburg",
					DepartureUTC:      "2020-04-05 15:00:00",
					AirportToIata:     "AER",
					AirportToRaspID:   9623547,
					ArrivalDay:        "2020-04-06",
					ArrivalTime:       "16:20:00",
					ArrivalTzName:     "Europe/Moscow",
					ArrivalUTC:        "2020-04-06 13:20:00",
					Status: flightstatus.FlightStatus{
						Status:          "unknown",
						DepartureStatus: "unknown",
						ArrivalStatus:   "unknown",
						DepartureSource: "0",
						ArrivalSource:   "0",
					},
					FlightCodeshares: []dto.TitledFlight{},
					Segments: []*FlightSegment{
						{
							CompanyIata:       "SU",
							CompanyRaspID:     26,
							Number:            "1234",
							Title:             "SU 1234",
							AirportFromIata:   "SVX",
							AirportFromRaspID: 9600370,
							DepartureDay:      "2020-04-05",
							DepartureTime:     "20:00:00",
							DepartureTzName:   "Asia/Yekaterinburg",
							DepartureUTC:      "2020-04-05 15:00:00",
							DepartureTerminal: "",
							AirportToIata:     "DME",
							AirportToRaspID:   9600216,
							ArrivalDay:        "2020-04-06",
							ArrivalTime:       "05:02:00",
							ArrivalTzName:     "Europe/Moscow",
							ArrivalUTC:        "2020-04-06 02:02:00",
							ArrivalTerminal:   "",
							Status: flightstatus.FlightStatus{
								DepartureStatus: "unknown",
								ArrivalStatus:   "unknown",
								Status:          "unknown",
							},
							Segments: []*FlightSegment{},
							Source:   "",
							Banned:   "",
						},
						{
							CompanyIata:       "SU",
							CompanyRaspID:     26,
							Number:            "1234",
							Title:             "SU 1234",
							AirportFromIata:   "DME",
							AirportFromRaspID: 9600216,
							DepartureDay:      "2020-04-06",
							DepartureTime:     "22:00:00",
							DepartureTzName:   "Europe/Moscow",
							DepartureUTC:      "2020-04-06 19:00:00",
							DepartureTerminal: "",
							AirportToIata:     "AER",
							AirportToRaspID:   9623547,
							ArrivalDay:        "2020-04-06",
							ArrivalTime:       "16:20:00",
							ArrivalTzName:     "Europe/Moscow",
							ArrivalUTC:        "2020-04-06 13:20:00",
							ArrivalTerminal:   "",
							Status: flightstatus.FlightStatus{
								DepartureStatus: "unknown",
								ArrivalStatus:   "unknown",
								Status:          "unknown",
							},
							Segments: []*FlightSegment{},
							Source:   "",
							Banned:   "",
						},
					},
					Source: "",
					Banned: "",
				},
			},
			false,
			"",
		},
		{
			"arrival",
			fields{
				Storage:        store,
				TimeZoneUtil:   tzutil,
				CarrierService: carrierService,
			},
			args{
				carrier:           NewCarrierParamByText("SU"),
				flightNumber:      "1234",
				nationalVersion:   "ru",
				showBanned:        false,
				nowDate:           time.Now().UTC(),
				limitBefore:       1,
				limitAfter:        1,
				direction:         direction.ARRIVAL,
				referenceDateTime: time.Date(2020, 4, 6, 12, 0, 0, 0, time.UTC),
			},
			[]*FlightSegment{
				{
					CompanyIata:       "SU",
					CompanyRaspID:     26,
					Number:            "1234",
					Title:             "SU 1234",
					AirportFromIata:   "SVX",
					AirportFromRaspID: 9600370,
					DepartureDay:      "2020-04-04",
					DepartureTime:     "20:00:00",
					DepartureTzName:   "Asia/Yekaterinburg",
					DepartureUTC:      "2020-04-04 15:00:00",
					AirportToIata:     "AER",
					AirportToRaspID:   9623547,
					ArrivalDay:        "2020-04-05",
					ArrivalTime:       "16:20:00",
					ArrivalTzName:     "Europe/Moscow",
					ArrivalUTC:        "2020-04-05 13:20:00",
					Status: flightstatus.FlightStatus{
						Status:          "unknown",
						DepartureStatus: "unknown",
						ArrivalStatus:   "unknown",
						DepartureSource: "0",
						ArrivalSource:   "0",
					},
					FlightCodeshares: []dto.TitledFlight{},
					Segments: []*FlightSegment{
						{
							CompanyIata:       "SU",
							CompanyRaspID:     26,
							Number:            "1234",
							Title:             "SU 1234",
							AirportFromIata:   "SVX",
							AirportFromRaspID: 9600370,
							DepartureDay:      "2020-04-04",
							DepartureTime:     "20:00:00",
							DepartureTzName:   "Asia/Yekaterinburg",
							DepartureUTC:      "2020-04-04 15:00:00",
							DepartureTerminal: "",
							AirportToIata:     "DME",
							AirportToRaspID:   9600216,
							ArrivalDay:        "2020-04-05",
							ArrivalTime:       "05:02:00",
							ArrivalTzName:     "Europe/Moscow",
							ArrivalUTC:        "2020-04-05 02:02:00",
							ArrivalTerminal:   "",
							Status: flightstatus.FlightStatus{
								DepartureStatus: "unknown",
								ArrivalStatus:   "unknown",
								Status:          "unknown",
							},
							Segments: []*FlightSegment{},
							Source:   "",
							Banned:   "",
						},
						{
							CompanyIata:       "SU",
							CompanyRaspID:     26,
							Number:            "1234",
							Title:             "SU 1234",
							AirportFromIata:   "DME",
							AirportFromRaspID: 9600216,
							DepartureDay:      "2020-04-05",
							DepartureTime:     "22:00:00",
							DepartureTzName:   "Europe/Moscow",
							DepartureUTC:      "2020-04-05 19:00:00",
							DepartureTerminal: "",
							AirportToIata:     "AER",
							AirportToRaspID:   9623547,
							ArrivalDay:        "2020-04-05",
							ArrivalTime:       "16:20:00",
							ArrivalTzName:     "Europe/Moscow",
							ArrivalUTC:        "2020-04-05 13:20:00",
							ArrivalTerminal:   "",
							Status: flightstatus.FlightStatus{
								DepartureStatus: "unknown",
								ArrivalStatus:   "unknown",
								Status:          "unknown",
							},
							Segments: []*FlightSegment{},
							Source:   "",
							Banned:   "",
						},
					},
					Source: "",
					Banned: "",
				},
				{
					CompanyIata:       "SU",
					CompanyRaspID:     26,
					Number:            "1234",
					Title:             "SU 1234",
					AirportFromIata:   "SVX",
					AirportFromRaspID: 9600370,
					DepartureDay:      "2020-04-05",
					DepartureTime:     "20:00:00",
					DepartureTzName:   "Asia/Yekaterinburg",
					DepartureUTC:      "2020-04-05 15:00:00",
					AirportToIata:     "AER",
					AirportToRaspID:   9623547,
					ArrivalDay:        "2020-04-06",
					ArrivalTime:       "16:20:00",
					ArrivalTzName:     "Europe/Moscow",
					ArrivalUTC:        "2020-04-06 13:20:00",
					Status: flightstatus.FlightStatus{
						Status:          "unknown",
						DepartureStatus: "unknown",
						ArrivalStatus:   "unknown",
						DepartureSource: "0",
						ArrivalSource:   "0",
					},
					FlightCodeshares: []dto.TitledFlight{},
					Segments: []*FlightSegment{
						{
							CompanyIata:       "SU",
							CompanyRaspID:     26,
							Number:            "1234",
							Title:             "SU 1234",
							AirportFromIata:   "SVX",
							AirportFromRaspID: 9600370,
							DepartureDay:      "2020-04-05",
							DepartureTime:     "20:00:00",
							DepartureTzName:   "Asia/Yekaterinburg",
							DepartureUTC:      "2020-04-05 15:00:00",
							DepartureTerminal: "",
							AirportToIata:     "DME",
							AirportToRaspID:   9600216,
							ArrivalDay:        "2020-04-06",
							ArrivalTime:       "05:02:00",
							ArrivalTzName:     "Europe/Moscow",
							ArrivalUTC:        "2020-04-06 02:02:00",
							ArrivalTerminal:   "",
							Status: flightstatus.FlightStatus{
								DepartureStatus: "unknown",
								ArrivalStatus:   "unknown",
								Status:          "unknown",
							},
							Segments: []*FlightSegment{},
							Source:   "",
							Banned:   "",
						},
						{
							CompanyIata:       "SU",
							CompanyRaspID:     26,
							Number:            "1234",
							Title:             "SU 1234",
							AirportFromIata:   "DME",
							AirportFromRaspID: 9600216,
							DepartureDay:      "2020-04-06",
							DepartureTime:     "22:00:00",
							DepartureTzName:   "Europe/Moscow",
							DepartureUTC:      "2020-04-06 19:00:00",
							DepartureTerminal: "",
							AirportToIata:     "AER",
							AirportToRaspID:   9623547,
							ArrivalDay:        "2020-04-06",
							ArrivalTime:       "16:20:00",
							ArrivalTzName:     "Europe/Moscow",
							ArrivalUTC:        "2020-04-06 13:20:00",
							ArrivalTerminal:   "",
							Status: flightstatus.FlightStatus{
								DepartureStatus: "unknown",
								ArrivalStatus:   "unknown",
								Status:          "unknown",
							},
							Segments: []*FlightSegment{},
							Source:   "",
							Banned:   "",
						},
					},
					Source: "",
					Banned: "",
				},
			},
			false,
			"",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			service := &flightServiceImpl{
				Storage:        tt.fields.Storage,
				TimeZoneUtil:   tt.fields.TimeZoneUtil,
				CarrierService: tt.fields.CarrierService,
			}
			gotResponse, err := service.GetFlightRange(
				tt.args.carrier,
				tt.args.flightNumber,
				tt.args.nationalVersion,
				tt.args.showBanned,
				tt.args.nowDate,
				tt.args.limitBefore,
				tt.args.limitAfter,
				tt.args.direction,
				tt.args.referenceDateTime,
			)
			assert.Equal(t, tt.wantResponse, gotResponse)

			if tt.wantErr {
				assert.Error(t, err)
				assert.EqualError(t, err, tt.wantErrMsg)
			}
		})
	}
}
