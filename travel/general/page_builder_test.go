package trips

import (
	"math/rand"
	"strconv"
	"testing"
	"time"

	"github.com/jonboulle/clockwork"
	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/library/go/ptr"
	apimodels "a.yandex-team.ru/travel/komod/trips/internal/components/api/trips/models"
	"a.yandex-team.ru/travel/komod/trips/internal/helpers"
	"a.yandex-team.ru/travel/komod/trips/internal/models"
	"a.yandex-team.ru/travel/komod/trips/internal/orders"
	"a.yandex-team.ru/travel/komod/trips/internal/point"
	tripsmodels "a.yandex-team.ru/travel/komod/trips/internal/trips/models"
	"a.yandex-team.ru/travel/library/go/geobase"
	ordercommons "a.yandex-team.ru/travel/orders/proto"
	"a.yandex-team.ru/travel/proto/dicts/rasp"
)

func newTrip(id string, when time.Time) *tripsmodels.Trip {
	pointFactory := point.NewFactory(
		nil,
		helpers.NewCachedLocationRepository(),
		geobase.StubGeobase{},
		nil,
	)
	return &tripsmodels.Trip{
		ID: id,
		OrderInfos: map[orders.ID]tripsmodels.OrderInfo{
			"id": {
				ID:    "id",
				State: ordercommons.EDisplayOrderState_OS_FULFILLED,
				Spans: []models.Span{
					models.NewSpan(
						models.NewVisit(pointFactory.MakeBySettlement(&rasp.TSettlement{Id: 1}), when.Add(-time.Millisecond)),
						models.NewVisit(pointFactory.MakeBySettlement(&rasp.TSettlement{Id: 2}), when.Add(time.Millisecond)),
						true,
					),
				},
			},
		},
	}
}

func TestStartPageBuilder_BuildPage(t *testing.T) {
	now := time.Now()
	var tripsList tripsmodels.Trips
	for i := -4; i < 5; i++ {
		tripsList = append(tripsList, newTrip(strconv.Itoa(i), now.Add(time.Duration(i)*time.Hour)))
	}
	rand.Shuffle(len(tripsList), func(i, j int) {
		tripsList[i], tripsList[j] = tripsList[j], tripsList[i]
	})

	builder := NewStartPageBuilder(clockwork.NewFakeClockAt(now))

	type args struct {
		token *apimodels.ContinuationToken
		limit uint
	}
	tests := []struct {
		name      string
		args      args
		wantIDs   []int
		wantToken string
	}{
		// active
		{
			"active: empty",
			args{token: GenerateStartToken(apimodels.ActiveTrips), limit: 0},
			[]int{},
			"active_0",
		},
		{
			"active: one",
			args{token: GenerateStartToken(apimodels.ActiveTrips), limit: 1},
			[]int{0},
			"active_1",
		},
		{
			"active: two",
			args{token: GenerateStartToken(apimodels.ActiveTrips), limit: 2},
			[]int{0, 1},
			"active_2",
		},
		{
			"active: next chunk",
			args{token: &apimodels.ContinuationToken{TripsType: apimodels.ActiveTrips, NextTripID: ptr.String("2")}, limit: 2},
			[]int{2, 3},
			"active_4",
		},
		{
			"active: all",
			args{token: GenerateStartToken(apimodels.ActiveTrips), limit: 5},
			[]int{0, 1, 2, 3, 4},
			"",
		},
		{
			"active: more than possible",
			args{token: GenerateStartToken(apimodels.ActiveTrips), limit: 6},
			[]int{0, 1, 2, 3, 4},
			"",
		},

		// past
		{
			"past: empty",
			args{token: GenerateStartToken(apimodels.PastTrips), limit: 0},
			[]int{},
			"past_-1",
		},
		{
			"past: one",
			args{token: GenerateStartToken(apimodels.PastTrips), limit: 1},
			[]int{-1},
			"past_-2",
		},
		{
			"past: next chunk",
			args{token: &apimodels.ContinuationToken{TripsType: apimodels.PastTrips, NextTripID: ptr.String("-2")}, limit: 2},
			[]int{-2, -3},
			"past_-4",
		},
		{
			"past: all",
			args{token: GenerateStartToken(apimodels.PastTrips), limit: 4},
			[]int{-1, -2, -3, -4},
			"",
		},
		{
			"past: more than possible",
			args{token: GenerateStartToken(apimodels.PastTrips), limit: 5},
			[]int{-1, -2, -3, -4},
			"",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			page, err := builder.BuildPage(tripsList, tt.args.token, tt.args.limit)
			assert.NoError(t, err)
			assert.Len(t, page.trips, len(tt.wantIDs))
			for i, trip := range page.trips {
				assert.Equal(t, strconv.Itoa(tt.wantIDs[i]), trip.ID, "unexpected trips %+v", page.trips)
			}

			var expectedToken *apimodels.ContinuationToken
			if tt.wantToken != "" {
				expectedToken, err = LoadToken(tt.wantToken)
				assert.NoError(t, err)
			}
			assert.Equal(t, expectedToken, page.nextToken)
		})
	}
}
