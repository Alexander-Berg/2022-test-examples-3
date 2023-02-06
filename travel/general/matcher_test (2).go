package matcher

import (
	"reflect"
	"testing"

	"a.yandex-team.ru/travel/komod/trips/internal/models"
	"a.yandex-team.ru/travel/komod/trips/internal/orders"
	"a.yandex-team.ru/travel/komod/trips/internal/testutils"
	tripmodels "a.yandex-team.ru/travel/komod/trips/internal/trips/models"
	ordercommons "a.yandex-team.ru/travel/orders/proto"
)

var (
	span1 = makeSpan()
	span2 = makeSpan()
	span3 = makeSpan()

	newSpan1 = makeSpan()
)

func TestMatcher_MatchTripsWithSpan(t *testing.T) {
	trip1 := tripmodels.NewTrip("trip1", "")
	trip1.UpsertOrder(makeOrderInfo("1", makeSpan(), makeSpan()))
	trip1.UpsertOrder(makeOrderInfo("2", span1, makeSpan(), makeSpan()))

	trip2 := tripmodels.NewTrip("trip2", "")
	trip2.UpsertOrder(makeOrderInfo("1", makeSpan(), makeSpan()))
	trip2.UpsertOrder(makeOrderInfo("2", span2, makeSpan(), makeSpan()))

	trip3 := tripmodels.NewTrip("trip3", "")
	trip3.UpsertOrder(makeCancelledByUserOrderInfo("1", span3, makeSpan()))
	trip3.UpsertOrder(makeOrderInfo("2", makeSpan(), makeSpan(), makeSpan()))

	type args struct {
		currentTrips tripmodels.Trips
		spans        []models.Span
	}
	tests := []struct {
		name string
		rule Rule
		args args
		want tripmodels.Trips
	}{
		{
			name: "rule connect span1 and newSpan1 -> match trip1",
			rule: getKnownSpanPairRule(span1, newSpan1),
			args: args{
				currentTrips: tripmodels.Trips{trip1, trip2, trip3},
				spans:        []models.Span{newSpan1},
			},
			want: tripmodels.Trips{trip1},
		},
		{
			name: "rule connect span2 and newSpan1 -> match trip2",
			rule: getKnownSpanPairRule(span2, newSpan1),
			args: args{
				currentTrips: tripmodels.Trips{trip1, trip2, trip3},
				spans:        []models.Span{newSpan1},
			},
			want: tripmodels.Trips{trip2},
		},
		{
			name: "complicated rule that connect spans 1, 2 and 3 -> match two trips (trip1, trip2)",
			rule: NewOrRule(getKnownSpanPairRule(span1, newSpan1), getKnownSpanPairRule(span2, newSpan1)),
			args: args{
				currentTrips: tripmodels.Trips{trip1, trip2, trip3},
				spans:        []models.Span{newSpan1},
			},
			want: tripmodels.Trips{trip1, trip2},
		},
		{
			name: "complicated rule that connect whole trip with span -> match trip2",
			rule: getKnownSpanPairRule(asSpan(trip2), newSpan1),
			args: args{
				currentTrips: tripmodels.Trips{trip1, trip2, trip3},
				spans:        []models.Span{newSpan1},
			},
			want: tripmodels.Trips{trip2},
		},
		{
			name: "match orders regardless of order state",
			rule: getKnownSpanPairRule(span3, newSpan1),
			args: args{
				currentTrips: tripmodels.Trips{trip1, trip2, trip3},
				spans:        []models.Span{newSpan1},
			},
			want: tripmodels.Trips{trip3},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			m := &Matcher{
				rule: tt.rule,
			}
			if got := m.MatchTripsWithConnectedSpans(tt.args.currentTrips, tt.args.spans...); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("MatchTripsWithConnectedSpans() = %v, want %v", got, tt.want)
			}
		})
	}
}

func getKnownSpanPairRule(knownLeft, knownRight models.Span) Rule {
	return NewPredicateRule(func(lhs, rhs models.Span) bool {
		return (equal(knownLeft, lhs) || equal(knownLeft, rhs)) && (equal(knownRight, lhs) || equal(knownRight, rhs))
	})
}

func makeOrderInfo(id string, spans ...models.Span) tripmodels.OrderInfo {
	return tripmodels.OrderInfo{
		ID:    orders.ID(id),
		State: ordercommons.EDisplayOrderState_OS_FULFILLED,
		Spans: spans,
	}
}

func makeCancelledByUserOrderInfo(id string, spans ...models.Span) tripmodels.OrderInfo {
	return tripmodels.OrderInfo{
		ID:    orders.ID(id),
		State: ordercommons.EDisplayOrderState_OS_REFUNDED,
		Spans: spans,
	}
}

func makeSpan() models.Span {
	return models.NewSpan(
		// не важно как создавать span. В тестах будем сравнивать указатели
		models.NewFakeVisit(1, testutils.ParseTime("2021-12-12")),
		models.NewFakeVisit(1, testutils.ParseTime("2021-12-14")),
		false,
	)
}

func equal(lhs, rhs models.Span) bool {
	return lhs.Start() == rhs.Start() && lhs.End() == rhs.End()
}
