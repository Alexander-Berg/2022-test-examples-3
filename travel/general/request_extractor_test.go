package weather

import (
	"testing"
	"time"

	"github.com/jonboulle/clockwork"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/units"
	"a.yandex-team.ru/travel/komod/trips/internal/helpers"
	"a.yandex-team.ru/travel/komod/trips/internal/point"
	"a.yandex-team.ru/travel/komod/trips/internal/references"
	"a.yandex-team.ru/travel/komod/trips/internal/span"
	"a.yandex-team.ru/travel/komod/trips/internal/testutils"
	"a.yandex-team.ru/travel/komod/trips/internal/testutils/builders"
	tripsmodels "a.yandex-team.ru/travel/komod/trips/internal/trips/models"
	"a.yandex-team.ru/travel/library/go/geobase"
)

func TestRequestExtractor_Extract(t *testing.T) {
	tripBuilder := builders.NewTrip()

	tests := []struct {
		name string
		now  time.Time
		trip *tripsmodels.Trip
		want *RequestParams
	}{
		{
			name: "empty",
		},
		{
			name: "one ticket to city -> one day in city",
			now:  testutils.ParseTime("2022-05-22"),

			trip: tripBuilder.Descriptive(1, "2022-05-23").
				FlyTo(2, 3*time.Hour, "1").
				Build("tripID", "passportID"),

			want: &RequestParams{
				geoID:     2,
				startDate: testutils.ParseTime("2022-05-23"),
				days:      1,
			},
		},

		{
			name: "2 nights in Moscow hotel-> three days in city",
			now:  testutils.ParseTime("2022-05-22"),

			trip: tripBuilder.Descriptive(1, "2022-05-23T12").
				FlyTo(213, units.Day, "1").
				Stay(2*units.Day, "2").
				FlyTo(1, units.Day, "1").
				Build("tripID", "passportID"),

			want: &RequestParams{
				geoID:     213,
				startDate: testutils.ParseTime("2022-05-24"),
				days:      3,
			},
		},

		{
			name: "2 hotels in Moscow -> both of hotels",
			now:  testutils.ParseTime("2022-05-22"),

			trip: tripBuilder.Descriptive(1, "2022-05-23T12").
				FlyTo(213, units.Day, "1").
				Stay(2*units.Day, "2").
				After(units.Day).
				Stay(2*units.Day, "3").
				FlyTo(1, units.Day, "1").
				Build("tripID", "passportID"),

			want: &RequestParams{
				geoID:     213,
				startDate: testutils.ParseTime("2022-05-24"),
				days:      6,
			},
		},

		{
			name: "1 day in hotel now -> weather for two days",
			now:  testutils.ParseTime("2022-05-31T15"),

			trip: tripBuilder.Descriptive(213, "2022-05-31T12").
				Stay(units.Day, "2").
				Build("tripID", "passportID"),

			want: &RequestParams{
				geoID:     213,
				startDate: testutils.ParseTime("2022-05-31"),
				days:      2,
			},
		},

		{
			name: "trip on the far edge of months -> climate for two month",
			now:  testutils.ParseTime("2022-05-31T15"),

			trip: tripBuilder.Descriptive(213, "2022-06-30T12").
				Stay(units.Day, "2").
				Build("tripID", "passportID"),

			want: &RequestParams{
				geoID:     213,
				startDate: testutils.ParseTime("2022-06-30"),
				days:      2,
			},
		},

		{
			name: "check timezones",
			now:  testutils.ParseTime("2022-05-31T15"),

			trip: tripBuilder.Descriptive(54, "2022-06-02T12:00:00+05:00").
				Stay(2*units.Day, "2").
				Now("2022-06-03T00:30:05+05:00").
				FlyTo(213, 2*time.Hour, "3").
				Build("tripID", "passportID"),

			want: &RequestParams{
				geoID:     54,
				startDate: testutils.ParseTime("2022-06-02"),
				days:      2,
			},
		},

		{
			name: "gap after hotel -> weather with gap",
			now:  testutils.ParseTime("2022-05-22"),

			trip: tripBuilder.Descriptive(1, "2022-05-23T12").
				FlyTo(213, units.Day, "1").
				Stay(2*units.Day, "2").
				After(units.Day).
				FlyTo(1, units.Day, "1").
				Build("tripID", "passportID"),

			want: &RequestParams{
				geoID:     213,
				startDate: testutils.ParseTime("2022-05-24"),
				days:      4,
			},
		},

		{
			name: "now in gap -> weather until fly",
			now:  testutils.ParseTime("2022-05-27T15"),

			trip: tripBuilder.Descriptive(1, "2022-05-23T12").
				FlyTo(213, units.Day, "1").
				Stay(2*units.Day, "2").
				After(3*units.Day).
				FlyTo(1, units.Day, "1").
				Build("tripID", "passportID"),

			want: &RequestParams{
				geoID:     213,
				startDate: testutils.ParseTime("2022-05-27"),
				days:      3,
			},
		},

		{
			name: "roundtrip -> weather between flies",
			now:  testutils.ParseTime("2022-05-23T15"),

			trip: tripBuilder.Descriptive(1, "2022-05-27T12").
				FlyTo(213, units.Day, "1").
				After(3*units.Day).
				FlyTo(1, units.Day, "1").
				Build("tripID", "passportID"),

			want: &RequestParams{
				geoID:     213,
				startDate: testutils.ParseTime("2022-05-28"),
				days:      4,
			},
		},
	}
	for _, tt := range tests {
		extractor := createTestRuleFactory(tt.now)
		t.Run(tt.name, func(t *testing.T) {
			params := extractor.Extract(tt.trip)
			if tt.want == nil {
				require.Nil(t, params)
				return
			}
			require.NotNil(t, params)
			assert.Equal(t, tt.want.geoID, params.geoID)
			assert.Equal(t, tt.want.startDate.String(), params.startDate.String())
			assert.Equal(t, tt.want.days, params.days)
		})
	}
}

func createTestRuleFactory(now time.Time) *RequestExtractor {
	geoBase := geobase.StubGeobase{}
	reference := references.References(nil)
	pointFactory := point.NewFactory(
		nil,
		helpers.NewCachedLocationRepository(),
		geoBase,
		reference,
	)
	pointResolver := point.NewResolver(geoBase, reference, pointFactory)
	pointComparator := point.NewComparator(geoBase, reference, pointResolver)
	spanComparator := span.NewSpanComparator(pointComparator)
	spanHelper := span.NewHelper(pointComparator, spanComparator)
	clock := clockwork.NewFakeClockAt(now)
	return NewRequestExtractor(spanHelper, pointComparator, clock)
}
