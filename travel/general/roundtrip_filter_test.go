package filters

import (
	"github.com/stretchr/testify/require"
	"testing"

	"a.yandex-team.ru/travel/avia/weekendtour/internal/models"
	"a.yandex-team.ru/travel/avia/weekendtour/internal/readers/ytreader"
)

func TestRoundTripFilter_AcceptsQID(t *testing.T) {
	t.Run(
		"Check QID roundtrip filter",
		func(t *testing.T) {
			filter := NewRoundTripFilter()

			testCases := []struct {
				testName         string
				testQID          string
				expectedResponse bool
			}{
				{
					testName:         "acceptable QID",
					testQID:          "210501-040258-682.mavia-travel.plane.c65_c239_2021-05-09_2021-05-15_economy_1_0_0_ru.ru",
					expectedResponse: true,
				},
				{
					testName:         "two-adults QID, not a single adult",
					testQID:          "210501-040258-682.mavia-travel.plane.c65_c239_2021-05-09_2021-05-15_economy_2_0_0_ru.ru",
					expectedResponse: false,
				},
			}
			for _, tc := range testCases {
				result := filter.AcceptsQID(tc.testQID)
				require.Equal(t, result, tc.expectedResponse, tc.testName)
			}
		},
	)
}

func TestRoundTripFilter_AcceptsJourney(t *testing.T) {
	t.Run(
		"Check journey roundtrip filter",
		func(t *testing.T) {
			filter := NewRoundTripFilter()

			testCases := []struct {
				testName         string
				testJourney      ytreader.JourneyBoundaries
				expectedResponse bool
			}{
				{
					testName: "acceptable journey",
					testJourney: ytreader.JourneyBoundaries{
						DepartureDate: "2021-05-07",
						ReturnDate:    "2021-05-12",
						PointFromKey:  "c54",
						PointToKey:    "c213",
					},
					expectedResponse: true,
				},
				{
					testName: "not a round-trip journey",
					testJourney: ytreader.JourneyBoundaries{
						DepartureDate: "2021-05-07",
						PointFromKey:  "c54",
						PointToKey:    "c213",
					},
					expectedResponse: false,
				},
			}
			for _, tc := range testCases {
				result := filter.AcceptsJourney(tc.testJourney)
				require.Equal(t, result, tc.expectedResponse, tc.testName)
			}
		},
	)
}

func TestRoundTripFilter_AcceptsVariant(t *testing.T) {
	t.Run(
		"Check variant roundtrip filter",
		func(t *testing.T) {
			filter := NewRoundTripFilter()

			testCases := []struct {
				testName         string
				testVariant      models.RoundTrip
				expectedResponse bool
			}{
				{
					testName: "acceptable variant",
					testVariant: models.RoundTrip{
						PointFromKey:    "c54",
						PointToKey:      "c213",
						WeekendID:       20210611,
						ForwardFlights:  "2106110725FV68912106111040",
						BackwardFlights: "2106131430FV69842106131740",
						Currency:        "RUB",
						Price:           12345.,
					},
					expectedResponse: true,
				},
				{
					testName: "wrong currency",
					testVariant: models.RoundTrip{
						PointFromKey:    "c54",
						PointToKey:      "c213",
						WeekendID:       20210611,
						ForwardFlights:  "2106110725FV68912106111040",
						BackwardFlights: "2106131430FV69842106131740",
						Currency:        "EUR",
						Price:           12345.,
					},
					expectedResponse: false,
				},
				{
					testName: "departure is not on Friday",
					testVariant: models.RoundTrip{
						PointFromKey:    "c54",
						PointToKey:      "c213",
						WeekendID:       20210611,
						ForwardFlights:  "2106100725FV68912106111040",
						BackwardFlights: "2106131430FV69842106131740",
						Currency:        "RUB",
						Price:           12345.,
					},
					expectedResponse: false,
				},
				{
					testName: "return is not on Sunday",
					testVariant: models.RoundTrip{
						PointFromKey:    "c54",
						PointToKey:      "c213",
						WeekendID:       20210611,
						ForwardFlights:  "2106110725FV68912106111040",
						BackwardFlights: "2106121430FV69842106131740",
						Currency:        "RUB",
						Price:           12345.,
					},
					expectedResponse: false,
				},
			}
			for _, tc := range testCases {
				result := filter.AcceptsVariant(tc.testVariant)
				require.Equal(t, result, tc.expectedResponse, tc.testName)
			}
		},
	)
}
