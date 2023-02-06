package filters

import (
	"github.com/stretchr/testify/require"
	"testing"

	"a.yandex-team.ru/travel/avia/weekendtour/internal/models"
)

func TestMinPriceFilter_AcceptsVariant(t *testing.T) {
	t.Run(
		"Check variant minprice filter",
		func(t *testing.T) {
			filter := NewMinRoundTripPriceFilter()

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
					testName: "price is too high",
					testVariant: models.RoundTrip{
						PointFromKey:    "c54",
						PointToKey:      "c213",
						WeekendID:       20210611,
						ForwardFlights:  "2106110725FV68912106111040",
						BackwardFlights: "2106131430FV69842106131740",
						Currency:        "EUR",
						Price:           12346.,
					},
					expectedResponse: false,
				},
			}
			for _, tc := range testCases {
				result := filter.AcceptsVariant(tc.testVariant)
				filter.CacheRoundTrip(tc.testVariant)
				require.Equal(t, result, tc.expectedResponse, tc.testName)
			}
		},
	)
}
