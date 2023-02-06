package flight

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/carrier"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/services/storage/timezone"
	"a.yandex-team.ru/travel/avia/shared_flights/api/internal/storage"
	"a.yandex-team.ru/travel/avia/shared_flights/api/pkg/structs"
	"a.yandex-team.ru/travel/avia/shared_flights/lib/go/direction"
	"a.yandex-team.ru/travel/proto/dicts/rasp"
	"a.yandex-team.ru/travel/proto/shared_flights/snapshots"
)

func makeStoreFlightRangeMulti(t *testing.T) *storage.Storage {
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
		IataCode: "SVX",
	})

	store.PutStation(&snapshots.TStationWithCodes{
		Station: &rasp.TStation{
			Id:         9600216,
			TimeZoneId: 0,
		},
		IataCode: "DME",
	})

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

	fp1 := operatingFlightPattern(structs.FlightPattern{
		ID:                 20001,
		OperatingFromDate:  "2020-02-12",
		OperatingUntilDate: "2020-09-15",
		OperatingOnDays:    1234567,
		ArrivalDayShift:    1,
	}, fb1)

	store.PutFlightBase(fb1)
	store.PutFlightPattern(fp1)

	fb2 := structs.FlightBase{
		ID:                     1000002,
		OperatingCarrier:       23,
		OperatingCarrierCode:   "S7",
		OperatingFlightNumber:  "1234",
		LegNumber:              1,
		DepartureStation:       9600370,
		DepartureStationCode:   "SVX",
		DepartureTimeScheduled: 2100,
		ArrivalStation:         9600216,
		ArrivalStationCode:     "DME",
		ArrivalTimeScheduled:   602,
	}

	fp2 := operatingFlightPattern(structs.FlightPattern{
		ID:                 20002,
		OperatingFromDate:  "2020-02-12",
		OperatingUntilDate: "2020-04-04",
		OperatingOnDays:    1234567,
		ArrivalDayShift:    1,
	}, fb2)

	store.PutFlightBase(fb2)
	store.PutFlightPattern(fp2)

	err := store.CarriersPopularityScores().UpdateFlightNumbersCache(store.FlightStorage())
	assert.NoError(t, err)

	return store
}

func Test_testTwoCarriers(t *testing.T) {
	store := makeStoreFlightRangeMulti(t)
	tzutil := timezone.NewTimeZoneUtil(store.Timezones(), store.Stations())
	carrierService := carrier.NewCarrierService(store.CarrierStorage(), store.IataCorrector())

	type fields struct {
		Storage        *storage.Storage
		TimeZoneUtil   timezone.TimeZoneUtil
		CarrierService CarrierService
	}
	type args struct {
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
		wantResponse map[int32][]string
		wantErr      bool
		wantErrMsg   string
	}{
		{
			"basic test",
			fields{
				Storage:        store,
				TimeZoneUtil:   tzutil,
				CarrierService: carrierService,
			},
			args{
				flightNumber:      "1234",
				nationalVersion:   "ru",
				showBanned:        false,
				nowDate:           time.Now().UTC(),
				limitBefore:       1,
				limitAfter:        1,
				direction:         direction.DEPARTURE,
				referenceDateTime: time.Date(2020, 4, 5, 12, 0, 0, 0, time.UTC),
			},
			map[int32][]string{
				26: []string{"2020-04-04", "2020-04-05"},
				23: []string{"2020-04-04"},
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
			response, err := service.GetFlightRangeMulti(
				tt.args.flightNumber,
				tt.args.nationalVersion,
				tt.args.showBanned,
				tt.args.nowDate,
				tt.args.limitBefore,
				tt.args.limitAfter,
				tt.args.direction,
				tt.args.referenceDateTime,
			)

			if tt.wantErr {
				assert.Error(t, err)
				assert.EqualError(t, err, tt.wantErrMsg)
			} else {
				processedResponse := make(map[int32][]string)
				for k, v := range response {
					processedValue := make([]string, 0)
					for _, segment := range v {
						processedValue = append(processedValue, segment.DepartureDay)
					}
					processedResponse[k] = processedValue
				}
				assert.Equal(t, tt.wantResponse, processedResponse)
			}
		})
	}
}
