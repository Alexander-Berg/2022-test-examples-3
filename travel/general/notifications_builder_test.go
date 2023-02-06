package onlineregistration

import (
	"testing"
	"time"

	"github.com/araddon/dateparse"
	"github.com/jackc/pgtype"
	"github.com/jonboulle/clockwork"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/ptr"
	"a.yandex-team.ru/travel/notifier/internal/models"
	"a.yandex-team.ru/travel/notifier/internal/orders"
	"a.yandex-team.ru/travel/proto/dicts/rasp"
)

func TestBuildNotification(t *testing.T) {
	order := &models.Order{ID: "1"}
	user := models.User{ID: 1, PassportID: ptr.String("1")}

	departureStationID := int32(1)
	arrivalStationID := int32(2)
	timezoneID := 2
	location, _ := time.LoadLocation("Asia/Yekaterinburg")

	stationCodes := &mockStationCodesRepository{}
	stationCodes.On("GetStationIDByCode", "SVX").Return(departureStationID, true)
	stationCodes.On("GetStationIDByCode", "SVO").Return(arrivalStationID, true)
	stations := &mockStationsRepository{}
	stations.On("Get", int(departureStationID)).Return(&rasp.TStation{TimeZoneId: int32(timezoneID)}, true)
	timeZones := &mockTimezoneDataProvider{}
	timeZones.On("Get", timezoneID).Return(location, true)
	now := dateparse.MustParse("2022-02-24T20:00")
	clock := clockwork.NewFakeClockAt(now)
	builder := NewNotificationsBuilder(
		stationCodes,
		stations,
		timeZones,
	)

	testCases := []struct {
		name             string
		segment          *orders.AviaSegment
		expectedNotifyAt time.Time
		expectedDeadline time.Time
		expectedPayload  []byte
	}{
		{
			name: "close segment (less than 1 day)",
			segment: &orders.AviaSegment{
				DepartureStation:  "SVX",
				DepartureDatetime: ptr.Time(dateparse.MustParse("2022-02-25T17:50")),
				ArrivalStation:    "SVO",
				MarketingTitle:    &orders.AviaFlightTitle{AirlineID: "SU", FlightNumber: "1404"},
				OperatingTitle:    &orders.AviaFlightTitle{AirlineID: "FV", FlightNumber: "1234"},
			},
			expectedNotifyAt: now,
			expectedDeadline: dateparse.MustParse("2022-02-25T12:50"),
			expectedPayload:  []byte(`{"marketingFlightNumber":"SU 1404","operatingFlightNumber":"FV 1234","localDepartureTime":"2022-02-25T17:50","departureStationId":1,"arrivalStationId":2}`),
		},
		{
			name: "far segment (more than 1 day)",
			segment: &orders.AviaSegment{
				DepartureStation:  "SVX",
				DepartureDatetime: ptr.Time(dateparse.MustParse("2022-03-01T17:50")),
				ArrivalStation:    "SVO",
				MarketingTitle:    &orders.AviaFlightTitle{AirlineID: "SU", FlightNumber: "1404"},
				OperatingTitle:    &orders.AviaFlightTitle{AirlineID: "FV", FlightNumber: "1234"},
			},
			expectedNotifyAt: dateparse.MustParse("2022-02-28T12:50"),
			expectedDeadline: dateparse.MustParse("2022-03-01T12:50"),
			expectedPayload:  []byte(`{"marketingFlightNumber":"SU 1404","operatingFlightNumber":"FV 1234","localDepartureTime":"2022-03-01T17:50","departureStationId":1,"arrivalStationId":2}`),
		},
	}
	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			notification, err := builder.buildNotification(order, user, tc.segment, clock.Now())

			require.NoError(t, err)
			require.Equal(t, &models.Notification{
				DispatchType: models.DispatchTypePull,
				Status:       models.NotificationStatusPlanned,
				User:         &user,
				Type:         models.NotificationTypeOnlineRegistration,
				Channel:      models.NotificationChannelPullAPI,
				Order:        order,
				NotifyAt:     tc.expectedNotifyAt,
				Deadline:     tc.expectedDeadline,
				Payload:      &pgtype.JSON{Bytes: tc.expectedPayload, Status: pgtype.Present},
			}, notification)
		})
	}
}
