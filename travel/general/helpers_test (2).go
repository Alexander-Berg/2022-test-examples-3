package personalsearch

import (
	"testing"
	"time"

	timeformats "cuelang.org/go/pkg/time"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/travel/avia/personalization/internal/consts"
	"a.yandex-team.ru/travel/avia/personalization/internal/services/personalsearch/models"
	"a.yandex-team.ru/travel/proto/cpa"
)

func TestGetSortedEvents(t *testing.T) {
	events := []models.Event{
		{Order: models.OrderInfo{ID: "hotels_order"}, Service: consts.HotelsServiceName, Timestamp: 4},
		{Order: models.OrderInfo{}, Service: consts.HotelsServiceName, Timestamp: 5},
		{Order: models.OrderInfo{ID: "hotels_order"}, Service: consts.HotelsServiceName, Timestamp: 6},
		{Order: models.OrderInfo{ID: "avia_order"}, Service: consts.AviaServiceName, Timestamp: 1},
		{Order: models.OrderInfo{}, Service: consts.AviaServiceName, Timestamp: 2},
		{Order: models.OrderInfo{}, Service: consts.AviaServiceName, Timestamp: 3},
	}
	expectedEvents := []models.Event{
		{Order: models.OrderInfo{ID: "avia_order"}, Service: consts.AviaServiceName, Timestamp: 1},
		{Order: models.OrderInfo{}, Service: consts.AviaServiceName, Timestamp: 3},
		{Order: models.OrderInfo{}, Service: consts.AviaServiceName, Timestamp: 2},
		{Order: models.OrderInfo{ID: "hotels_order"}, Service: consts.HotelsServiceName, Timestamp: 6},
		{Order: models.OrderInfo{ID: "hotels_order"}, Service: consts.HotelsServiceName, Timestamp: 4},
		{Order: models.OrderInfo{}, Service: consts.HotelsServiceName, Timestamp: 5},
	}

	result := getSortedEvents(events, consts.HotelsServiceName)

	require.Equal(t, expectedEvents, result)
}

func TestDeduplicateByDirection(t *testing.T) {
	events := []models.Event{
		{PointFrom: models.GeoPoint{PointKey: "c213"}, PointTo: models.GeoPoint{PointKey: "c2"}, Timestamp: 2},
		{PointFrom: models.GeoPoint{PointKey: "c213"}, PointTo: models.GeoPoint{PointKey: "c2"}, Timestamp: 1},

		{PointFrom: models.GeoPoint{PointKey: "c2"}, PointTo: models.GeoPoint{PointKey: "c213"}, Timestamp: 2},
		{
			PointFrom: models.GeoPoint{PointKey: "c2"},
			PointTo:   models.GeoPoint{PointKey: "c213"},
			Order:     models.OrderInfo{ID: "1"},
			Timestamp: 1,
		},

		{
			PointFrom: models.GeoPoint{PointKey: "c213"},
			PointTo:   models.GeoPoint{PointKey: "c239"},
			Order:     models.OrderInfo{ID: "2"},
			Timestamp: 2,
		},
		{PointFrom: models.GeoPoint{PointKey: "c213"}, PointTo: models.GeoPoint{PointKey: "c239"}, Timestamp: 1},

		{PointFrom: models.GeoPoint{PointKey: "c239"}, PointTo: models.GeoPoint{PointKey: "c213"}, Timestamp: 2},
	}

	expectedEvents := []models.Event{
		{PointFrom: models.GeoPoint{PointKey: "c213"}, PointTo: models.GeoPoint{PointKey: "c2"}, Timestamp: 2},

		{
			PointFrom: models.GeoPoint{PointKey: "c2"},
			PointTo:   models.GeoPoint{PointKey: "c213"},
			Order:     models.OrderInfo{ID: "1"},
			Timestamp: 1,
		},

		{
			PointFrom: models.GeoPoint{PointKey: "c213"},
			PointTo:   models.GeoPoint{PointKey: "c239"},
			Order:     models.OrderInfo{ID: "2"},
			Timestamp: 2,
		},

		{PointFrom: models.GeoPoint{PointKey: "c239"}, PointTo: models.GeoPoint{PointKey: "c213"}, Timestamp: 2},
	}

	result := deduplicateByDirection(events, &Query{})

	require.Equal(t, expectedEvents, result)
}

func TestRemoveUnconfirmedOrders(t *testing.T) {
	events := []models.Event{
		{Order: models.OrderInfo{ID: "1", Status: cpa.EOrderStatus_OS_CANCELLED.String()}, Timestamp: 2},
		{Order: models.OrderInfo{ID: "1", Status: cpa.EOrderStatus_OS_CONFIRMED.String()}, Timestamp: 1},

		{Order: models.OrderInfo{ID: "2", Status: cpa.EOrderStatus_OS_CONFIRMED.String()}, Timestamp: 1},

		{Order: models.OrderInfo{ID: "3", Status: cpa.EOrderStatus_OS_CONFIRMED.String()}, Timestamp: 2},
		{Order: models.OrderInfo{ID: "3", Status: cpa.EOrderStatus_OS_UNPAID.String()}, Timestamp: 1},

		{Order: models.OrderInfo{ID: "4", Status: cpa.EOrderStatus_OS_UNPAID.String()}, Timestamp: 1},

		{Order: models.OrderInfo{}, Timestamp: 1},

		{Order: models.OrderInfo{ID: "5", Status: cpa.EOrderStatus_OS_UNPAID.String()}, Timestamp: 1},
		{Order: models.OrderInfo{ID: "5", Status: cpa.EOrderStatus_OS_CONFIRMED.String()}, Timestamp: 2},
	}
	expectedEvents := []models.Event{
		{Order: models.OrderInfo{ID: "2", Status: cpa.EOrderStatus_OS_CONFIRMED.String()}, Timestamp: 1},
		{Order: models.OrderInfo{ID: "3", Status: cpa.EOrderStatus_OS_CONFIRMED.String()}, Timestamp: 2},
		{Order: models.OrderInfo{}, Timestamp: 1},
		{Order: models.OrderInfo{ID: "5", Status: cpa.EOrderStatus_OS_CONFIRMED.String()}, Timestamp: 2},
	}

	result := removeUnconfirmedOrders(events)

	require.Equal(t, expectedEvents, result)
}

func TestFillAviaReturnDate(t *testing.T) {
	events := []models.Event{
		{Service: consts.HotelsServiceName, When: "2021-09-01"},
		{Service: consts.AviaServiceName, When: "2021-09-01"},
		{Service: consts.AviaServiceName, When: "2021-12-31"},
		{Service: consts.AviaServiceName, When: "2021-12-31", ReturnDate: "2022-01-05"},
	}
	expectedEvents := []models.Event{
		{Service: consts.HotelsServiceName, When: "2021-09-01"},
		{Service: consts.AviaServiceName, When: "2021-09-01", ReturnDate: "2021-09-02"},
		{Service: consts.AviaServiceName, When: "2021-12-31", ReturnDate: "2022-01-01"},
		{Service: consts.AviaServiceName, When: "2021-12-31", ReturnDate: "2022-01-05"},
	}

	result := fillAviaReturnDate(events)

	require.Equal(t, expectedEvents, result)
}

func TestRemoveLongTermAviaOrders(t *testing.T) {
	events := []models.Event{
		{Service: consts.HotelsServiceName, When: "2021-10-01"},
		{Service: consts.HotelsServiceName, When: "2021-10-01", ReturnDate: "2021-11-01"},
		{Service: consts.HotelsServiceName, Order: models.OrderInfo{ID: "1"}, When: "2021-10-01"},
		{Service: consts.HotelsServiceName, Order: models.OrderInfo{ID: "2"}, When: "2021-10-01", ReturnDate: "2021-11-01"},

		{Service: consts.AviaServiceName, When: "2021-10-01"},
		{Service: consts.AviaServiceName, When: "2021-10-01", ReturnDate: "2021-11-01"},
		{Service: consts.AviaServiceName, Order: models.OrderInfo{ID: "3"}, When: "2021-10-01"},
		{Service: consts.AviaServiceName, Order: models.OrderInfo{ID: "4"}, When: "2021-10-01", ReturnDate: "2021-10-31"},

		{Service: consts.AviaServiceName, Order: models.OrderInfo{ID: "5"}, When: "2021-10-01", ReturnDate: "2021-11-01"},
	}
	expectedEvents := []models.Event{
		{Service: consts.HotelsServiceName, When: "2021-10-01"},
		{Service: consts.HotelsServiceName, When: "2021-10-01", ReturnDate: "2021-11-01"},
		{Service: consts.HotelsServiceName, Order: models.OrderInfo{ID: "1"}, When: "2021-10-01"},
		{Service: consts.HotelsServiceName, Order: models.OrderInfo{ID: "2"}, When: "2021-10-01", ReturnDate: "2021-11-01"},

		{Service: consts.AviaServiceName, When: "2021-10-01"},
		{Service: consts.AviaServiceName, When: "2021-10-01", ReturnDate: "2021-11-01"},
		{Service: consts.AviaServiceName, Order: models.OrderInfo{ID: "3"}, When: "2021-10-01"},
		{Service: consts.AviaServiceName, Order: models.OrderInfo{ID: "4"}, When: "2021-10-01", ReturnDate: "2021-10-31"},
	}

	result := removeLongTermAviaOrders(events)

	require.Equal(t, expectedEvents, result)
}

func TestRemoveOneDayRoundTrip(t *testing.T) {
	events := []models.Event{
		{Service: consts.HotelsServiceName, When: "2021-10-01"},
		{Service: consts.HotelsServiceName, When: "2021-10-01", ReturnDate: "2021-10-01"},
		{Service: consts.HotelsServiceName, Order: models.OrderInfo{ID: "1"}, When: "2021-10-01"},
		{Service: consts.HotelsServiceName, Order: models.OrderInfo{ID: "2"}, When: "2021-10-01", ReturnDate: "2021-10-01"},

		{Service: consts.AviaServiceName, When: "2021-10-01"},
		{Service: consts.AviaServiceName, When: "2021-10-01", ReturnDate: "2021-10-02"},
		{Service: consts.AviaServiceName, Order: models.OrderInfo{ID: "3"}, When: "2021-10-01"},
		{Service: consts.AviaServiceName, Order: models.OrderInfo{ID: "4"}, When: "2021-10-01", ReturnDate: "2021-10-02"},

		{Service: consts.AviaServiceName, When: "2021-10-01", ReturnDate: "2021-10-01"},
		{Service: consts.AviaServiceName, Order: models.OrderInfo{ID: "4"}, When: "2021-10-01", ReturnDate: "2021-10-01"},
	}
	expectedEvents := []models.Event{
		{Service: consts.HotelsServiceName, When: "2021-10-01"},
		{Service: consts.HotelsServiceName, When: "2021-10-01", ReturnDate: "2021-10-01"},
		{Service: consts.HotelsServiceName, Order: models.OrderInfo{ID: "1"}, When: "2021-10-01"},
		{Service: consts.HotelsServiceName, Order: models.OrderInfo{ID: "2"}, When: "2021-10-01", ReturnDate: "2021-10-01"},

		{Service: consts.AviaServiceName, When: "2021-10-01"},
		{Service: consts.AviaServiceName, When: "2021-10-01", ReturnDate: "2021-10-02"},
		{Service: consts.AviaServiceName, Order: models.OrderInfo{ID: "3"}, When: "2021-10-01"},
		{Service: consts.AviaServiceName, Order: models.OrderInfo{ID: "4"}, When: "2021-10-01", ReturnDate: "2021-10-02"},
	}

	result := removeOneDayRoundTrip(events)

	require.Equal(t, expectedEvents, result)
}

func TestFilterOutdated(t *testing.T) {
	events := []models.Event{
		{Service: consts.HotelsServiceName, When: "2021-10-01"},
		{Service: consts.HotelsServiceName, When: "2021-10-01", ReturnDate: "2021-10-03"},
		{Service: consts.HotelsServiceName, Order: models.OrderInfo{ID: "1"}, When: "2021-10-01"},
		{Service: consts.HotelsServiceName, Order: models.OrderInfo{ID: "2"}, When: "2021-10-01", ReturnDate: "2021-10-03"},
		{Service: consts.AviaServiceName, When: "2021-10-01"},
		{Service: consts.AviaServiceName, When: "2021-10-01", ReturnDate: "2021-10-03"},
		{Service: consts.AviaServiceName, Order: models.OrderInfo{ID: "3"}, When: "2021-10-01"},
		{Service: consts.AviaServiceName, Order: models.OrderInfo{ID: "4"}, When: "2021-10-01", ReturnDate: "2021-10-03"},

		{Service: consts.HotelsServiceName, When: "2021-10-02"},
		{Service: consts.HotelsServiceName, When: "2021-10-02", ReturnDate: "2021-10-03"},
		{Service: consts.HotelsServiceName, Order: models.OrderInfo{ID: "5"}, When: "2021-10-02"},
		{Service: consts.HotelsServiceName, Order: models.OrderInfo{ID: "6"}, When: "2021-10-02", ReturnDate: "2021-10-03"},
		{Service: consts.AviaServiceName, When: "2021-10-02"},
		{Service: consts.AviaServiceName, When: "2021-10-02", ReturnDate: "2021-10-03"},
		{Service: consts.AviaServiceName, Order: models.OrderInfo{ID: "7"}, When: "2021-10-02"},
		{Service: consts.AviaServiceName, Order: models.OrderInfo{ID: "8"}, When: "2021-10-02", ReturnDate: "2021-10-03"},
	}
	expectedEvents := []models.Event{
		{Service: consts.HotelsServiceName, When: "2021-10-02"},
		{Service: consts.HotelsServiceName, When: "2021-10-02", ReturnDate: "2021-10-03"},
		{Service: consts.HotelsServiceName, Order: models.OrderInfo{ID: "5"}, When: "2021-10-02"},
		{Service: consts.HotelsServiceName, Order: models.OrderInfo{ID: "6"}, When: "2021-10-02", ReturnDate: "2021-10-03"},
		{Service: consts.AviaServiceName, When: "2021-10-02"},
		{Service: consts.AviaServiceName, When: "2021-10-02", ReturnDate: "2021-10-03"},
		{Service: consts.AviaServiceName, Order: models.OrderInfo{ID: "7"}, When: "2021-10-02"},
		{Service: consts.AviaServiceName, Order: models.OrderInfo{ID: "8"}, When: "2021-10-02", ReturnDate: "2021-10-03"},
	}

	today, _ := time.Parse(timeformats.RFC3339Date, "2021-10-02")
	result := filterOutdated(events, today)

	require.Equal(t, expectedEvents, result)
}

func TestGetDatesDiff(t *testing.T) {
	testCases := []struct {
		name                      string
		dateForward, dateBackward string
		expected                  int
	}{
		{
			name:         "zero diff for the same days",
			dateForward:  "2021-01-01",
			dateBackward: "2021-01-01",
			expected:     0,
		},
		{
			name:         "diff is one for two consecutive days",
			dateForward:  "2021-01-01",
			dateBackward: "2021-01-02",
			expected:     1,
		},
		{
			name:         "diff is one for a jump over year",
			dateForward:  "2021-12-31",
			dateBackward: "2022-01-01",
			expected:     1,
		},
		{
			name:         "diff is 365 for a common year",
			dateForward:  "2021-01-01",
			dateBackward: "2022-01-01",
			expected:     365,
		},
		{
			name:         "diff is 366 for a leap year",
			dateForward:  "2020-01-01",
			dateBackward: "2021-01-01",
			expected:     366,
		},
	}
	for _, tc := range testCases {
		t.Run(
			tc.name, func(t *testing.T) {
				require.Equal(t, tc.expected, getDatesDiff(tc.dateForward, tc.dateBackward))
			},
		)
	}
	t.Run(
		"zero diff for the same days", func(t *testing.T) {
			result := getDatesDiff("2021-01-01", "2021-01-01")
			require.Equal(t, 0, result)
		},
	)

	t.Run(
		"diff for the same days", func(t *testing.T) {
			result := getDatesDiff("2021-01-01", "2021-01-01")
			require.Equal(t, 0, result)
		},
	)
}
