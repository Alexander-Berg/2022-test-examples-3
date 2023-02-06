package status

import (
	"bytes"
	"context"
	"encoding/json"
	"math/rand"
	"os"
	"path"
	"strings"
	"testing"
	"time"

	"github.com/jackc/pgconn"
	"github.com/jackc/pgx/v4"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"

	"a.yandex-team.ru/library/go/core/log"
	arczap "a.yandex-team.ru/library/go/core/log/zap"
	"a.yandex-team.ru/travel/avia/shared_flights/lib/go/direction"
	"a.yandex-team.ru/travel/avia/shared_flights/lib/go/dtutil"
	"a.yandex-team.ru/travel/avia/shared_flights/status_importer/internal/objects"
	carriercache "a.yandex-team.ru/travel/avia/shared_flights/status_importer/internal/objects/cache/carrier"
	legcache "a.yandex-team.ru/travel/avia/shared_flights/status_importer/internal/objects/cache/flight-leg"
	stationcache "a.yandex-team.ru/travel/avia/shared_flights/status_importer/internal/objects/cache/station"
	"a.yandex-team.ru/travel/avia/shared_flights/status_importer/internal/objects/model"
	"a.yandex-team.ru/travel/avia/shared_flights/status_importer/pkg/logging/yt/updatestatuslog"
	"a.yandex-team.ru/travel/library/go/logging"
	"a.yandex-team.ru/travel/proto/avia/flight_status"
)

type mockedStatusSourceRepo struct {
	mock.Mock
}

type mockedStationRepo struct {
}

type mockedCarrierRepo struct {
}

type mockedFlightLegRepo struct {
}

type mockedStopPointRepo struct {
	mock.Mock
}

type mockedBatchSender struct {
	mock.Mock
	requests [][]interface{}
}

type mockedBatchResult struct {
	mock.Mock
	callCount int
}

const dashedDateFormat = "2006-01-02"

func randString(n int) string {
	letters := []rune("1234567890abcdefghijklmnopqrstuvwxyz")
	bb := make([]rune, n)
	for i := range bb {
		bb[i] = letters[rand.Intn(len(letters))]
	}
	return string(bb)
}

func tempYtLogger(t *testing.T) (*arczap.Logger, string) {
	dir := t.TempDir()

	filename := randString(8)
	logPath := path.Join(dir, "yt", filename)
	logDir := path.Dir(logPath)
	err := os.MkdirAll(logDir, os.FileMode(0777))
	if err != nil {
		t.Error(err)
	}
	logger, err := logging.NewYtFileLogger(log.InfoLevel, logPath)
	if err != nil {
		t.Error(err)
	}
	return logger, logPath
}

func inFiveMinutes(now time.Time) time.Time {
	return now.Add(5 * time.Minute)
}
func datetime(t time.Time) string {
	s := t.Format("2006-01-02T15:04:05")
	return s
}

func timeParseOrFail(tst *testing.T, layout, s string) time.Time {
	t, err := time.Parse(layout, s)
	if err != nil {
		tst.Fatalf("Invalid datetime format: %s %s", layout, s)
	}
	return t
}

func Test_StatusUpdater(t *testing.T) {
	// Setup
	now := time.Now()
	var err error
	statusSourceRepo := mockedStatusSourceRepo{}
	stationRepo := stationcache.New(&stationcache.Config{
		StationProvider: &mockedStationRepo{},
		UpdateInterval:  time.Hour,
	})
	carrierRepo := carriercache.New(&carriercache.Config{
		CarrierProvider: &mockedCarrierRepo{},
		UpdateInterval:  time.Hour,
	})
	flightLegRepo := legcache.New(&legcache.Config{
		FlightLegProvider: &mockedFlightLegRepo{},
		FlightsProvider:   &mockedFlightLegRepo{},
		UpdateInterval:    time.Hour,
		PrecacheWindow:    1,
	})
	stopPointRepo := mockedStopPointRepo{}
	batchSender := mockedBatchSender{}
	poolFn := func() BatchSender {
		return &batchSender
	}
	l, ytLogPath := tempYtLogger(t)
	defer l.L.Sync()
	updaterConfig := UpdaterConfig{
		Parallelism:   1,
		BatchSenderFn: poolFn,
		Objects: &objects.Objects{
			Carrier:      carrierRepo,
			FlightLeg:    flightLegRepo,
			Station:      stationRepo,
			StatusSource: &statusSourceRepo,
			StopPoint:    &stopPointRepo,
		},
		UpdateLogger: l,
	}
	expect := assert.New(t)
	statusUpdater := NewStatusUpdater(updaterConfig)

	// Update no statuses
	finishedChan := make(chan error, 2)
	err = statusUpdater.Update(ProcessingUnit{
		Statuses: nil,
		Finished: finishedChan,
	}, true)
	expect.Error(err, "Expected `no statuses` error")

	// Update one status
	statusSourceRepo.On("ByName", "airport").Return(&model.StatusSource{ID: 3, Name: "airport"})
	stopPointRepo.On("ByCode", "Шереметьево").Return(&model.StopPoint{
		StationID:   9600213,
		StationCode: "SVO",
		CityCode:    "MOW",
	})
	batchSender.On("SendBatch").Return(&mockedBatchResult{})

	finishedChan = make(chan error, 2)
	_ = statusUpdater.Update(ProcessingUnit{
		Statuses: []*tStatus{
			{
				FlightStatus: flight_status.FlightStatus{
					StatusId:            "status-id-1",
					MessageId:           "message-id-1",
					ReceivedAt:          now.Unix(),
					Airport:             "SVO",
					AirlineId:           0,
					AirlineCode:         "SU",
					FlightNumber:        "1404",
					FlightDate:          now.Format(dashedDateFormat),
					Direction:           "departure",
					TimeActual:          "",
					TimeScheduled:       datetime(inFiveMinutes(now)),
					Status:              "wait",
					Gate:                "15",
					Terminal:            "A",
					CheckInDesks:        "301-303,314-317",
					BaggageCarousels:    "",
					Diverted:            false,
					DivertedAirportCode: "",
					RoutePointFrom:      "Шереметьево",
					RoutePointTo:        "SVX",
					Source:              "airport",
				},
			},
			{
				FlightStatus: flight_status.FlightStatus{
					StatusId:            "status-id-2",
					MessageId:           "message-id-1",
					ReceivedAt:          now.Unix(),
					Airport:             "ШРМ",
					AirlineId:           9999,
					AirlineCode:         "ДР",
					FlightNumber:        "404",
					FlightDate:          now.Format(dashedDateFormat),
					Direction:           "arrival",
					TimeActual:          strings.Replace(datetime(inFiveMinutes(now)), "T", " ", 1),
					TimeScheduled:       datetime(inFiveMinutes(now)),
					Status:              "wait",
					Gate:                "16",
					Terminal:            "B",
					CheckInDesks:        "14",
					BaggageCarousels:    "",
					Diverted:            false,
					DivertedAirportCode: "",
					RoutePointFrom:      "SVX",
					RoutePointTo:        "Шереметьево",
					Source:              "airport",
				},
			},
		},

		Finished: finishedChan,
	}, false)

	select {
	case err = <-finishedChan:
		break
	case <-time.After(10 * time.Second):
		t.Fatal("Status updater timeout exceeded")
	}
	expect.NoError(err, "No errors expected")

	file, err := os.ReadFile(ytLogPath)
	expect.NoError(err)

	var logRecords = make([]updatestatuslog.Record, 2)
	for i, line := range bytes.Split(file, []byte("\n")) {
		if len(line) == 0 {
			continue
		}
		expect.LessOrEqual(i, len(logRecords), "Too many logRecords lines")
		err := json.Unmarshal(line, &logRecords[i])
		expect.NoError(err)
	}

	expect.WithinDuration(time.Now(), time.Unix(logRecords[0].Unixtime, 0), time.Minute)
	expect.WithinDuration(time.Now(), timeParseOrFail(t, "2006-01-02 15:04:05", logRecords[0].ReceivedAt), time.Minute)
	expect.Equal("SVO", logRecords[0].AirportCode)
	expect.Equal(int64(26), logRecords[0].AirlineID)
	expect.Equal("SU", logRecords[0].AirlineCode)
	expect.Equal("1404", logRecords[0].FlightNumber)
	expect.Equal(now.Format("2006-01-02"), logRecords[0].FlightDate)
	expect.Equal(direction.DEPARTURE.String(), logRecords[0].Direction)
	expect.Empty(logRecords[0].TimeActual)
	expect.Equal(datetime(inFiveMinutes(now)), logRecords[0].TimeScheduled)
	expect.Equal("15", logRecords[0].Gate)
	expect.Equal("success", logRecords[0].Result)

	expect.WithinDuration(time.Now(), time.Unix(logRecords[1].Unixtime, 0), time.Minute)
	expect.WithinDuration(time.Now(), timeParseOrFail(t, "2006-01-02 15:04:05", logRecords[1].ReceivedAt), time.Minute)
	expect.Equal("SVO", logRecords[1].AirportCode)
	expect.Equal(int64(9144), logRecords[1].AirlineID)
	expect.Equal("ДР", logRecords[1].AirlineCode)
	expect.Equal("404", logRecords[1].FlightNumber)
	expect.Equal(now.Format("2006-01-02"), logRecords[1].FlightDate)
	expect.Equal(direction.ARRIVAL.String(), logRecords[1].Direction)
	expect.Equal(datetime(inFiveMinutes(now)), logRecords[1].TimeActual)
	expect.Equal(datetime(inFiveMinutes(now)), logRecords[1].TimeScheduled)
	expect.Equal("16", logRecords[1].Gate)
	expect.Equal("success", logRecords[1].Result)

}

func (m mockedCarrierRepo) All() (chan *model.Carrier, error) {
	carriers := []*model.Carrier{
		{
			ID:     26,
			Iata:   "SU",
			Sirena: "СУ",
			Icao:   "AFL",
		},
		{
			ID:     9144,
			Iata:   "DP",
			Sirena: "ДР",
			IcaoRU: "ПБД",
		},
	}

	ch := make(chan *model.Carrier, len(carriers))
	for _, carrier := range carriers {
		ch <- carrier
	}
	close(ch)
	return ch, nil
}

func (m *mockedFlightLegRepo) Flights(dest chan *model.FlightPatternRecord, _ time.Time, _ time.Time) error {
	flights := []*model.FlightPatternRecord{
		{
			FlightPatternKey: model.FlightPatternKey{
				CarrierID:       26,
				Number:          "1404",
				OperatingFrom:   time.Now().AddDate(0, 0, -30),
				OperatingUntil:  time.Now().AddDate(0, 0, 30),
				OperatingOnDays: 1234567,
				ArrivalDayShift: 0,
			},
			LegNumber:        1,
			DepartureStation: 9600213,
			ArrivalStation:   9600370,
		},
		{
			FlightPatternKey: model.FlightPatternKey{
				CarrierID:       9144,
				Number:          "404",
				OperatingFrom:   time.Now().AddDate(0, 0, -30),
				OperatingUntil:  time.Now().AddDate(0, 0, 30),
				OperatingOnDays: 1234567,
				ArrivalDayShift: 0,
			},
			LegNumber:        1,
			DepartureStation: 9600370,
			ArrivalStation:   9600213,
		},
	}

	for _, flight := range flights {
		dest <- flight
	}
	close(dest)
	return nil
}

func (m *mockedFlightLegRepo) FlightLeg(int64, string, dtutil.StringDate, direction.Direction, model.StationID) (leg int16, departureDate dtutil.StringDate, err error) {
	return 0, "", nil
}

func (m *mockedBatchResult) Exec() (pgconn.CommandTag, error) {
	m.callCount += 1
	if m.callCount == 2 {
		return nil, &pgconn.PgError{}
	}
	return pgconn.CommandTag("INSERT 0 1"), nil
}

func (m *mockedBatchResult) Query() (pgx.Rows, error) {
	panic("implement me")
}

func (m *mockedBatchResult) QueryRow() pgx.Row {
	panic("implement me")
}

func (m *mockedBatchResult) QueryFunc(scans []interface{}, f func(pgx.QueryFuncRow) error) (pgconn.CommandTag, error) {
	panic("implement me")
}

func (m *mockedBatchResult) Close() error {
	return nil
}

func (m *mockedBatchSender) SendBatch(ctx context.Context, b *pgx.Batch) pgx.BatchResults {
	args := m.Called()
	return args.Get(0).(pgx.BatchResults)
}

func (m *mockedStopPointRepo) ByCode(code string) *model.StopPoint {
	args := m.Called(code)
	return args.Get(0).(*model.StopPoint)
}

func (m *mockedStationRepo) All() (chan *model.Station, error) {
	stations := []*model.Station{
		{
			ID:     9600213,
			Iata:   "SVO",
			Sirena: "ШРМ",
		},
		{
			ID:     9600370,
			Iata:   "SVX",
			Sirena: "ЕКБ",
		},
	}
	ch := make(chan *model.Station, len(stations))
	for _, s := range stations {
		ch <- s
	}
	close(ch)
	return ch, nil
}

func (m *mockedStatusSourceRepo) ByName(name string) *model.StatusSource {
	args := m.Called(name)
	return args.Get(0).(*model.StatusSource)
}
