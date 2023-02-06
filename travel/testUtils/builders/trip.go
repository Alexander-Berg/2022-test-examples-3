package builders

import (
	"time"

	"a.yandex-team.ru/travel/komod/trips/internal/models"
	"a.yandex-team.ru/travel/komod/trips/internal/orders"
	"a.yandex-team.ru/travel/komod/trips/internal/testutils"
	tripmodels "a.yandex-team.ru/travel/komod/trips/internal/trips/models"
	ordercommons "a.yandex-team.ru/travel/orders/proto"
)

const (
	transportSpan    = true
	nonTransportSpan = false
)

type Trip struct{}

func NewTrip() *Trip {
	return &Trip{}
}

func (b Trip) Descriptive(pointID int, t string) *descriptiveTripBuilder {
	return (&descriptiveTripBuilder{
		orders: make(map[orders.ID]bool),
		spans:  make(map[orders.ID][]models.Span),
	}).Now(t).IAmThere(pointID)
}

type descriptiveTripBuilder struct {
	currentTime  time.Time
	currentPoint int
	orders       map[orders.ID]bool
	spans        map[orders.ID][]models.Span
}

func (b *descriptiveTripBuilder) RegisterOrder(id string, active bool) *descriptiveTripBuilder {
	b.orders[orders.ID(id)] = active
	b.spans[orders.ID(id)] = make([]models.Span, 0)
	return b
}

func (b *descriptiveTripBuilder) IAmThere(pointID int) *descriptiveTripBuilder {
	b.currentPoint = pointID
	return b
}

func (b *descriptiveTripBuilder) Now(t string) *descriptiveTripBuilder {
	b.currentTime = testutils.ParseTime(t)
	return b
}

func (b *descriptiveTripBuilder) After(d time.Duration) *descriptiveTripBuilder {
	b.assertCurrents()
	b.currentTime = b.currentTime.Add(d)
	return b
}

func (b *descriptiveTripBuilder) Stay(d time.Duration, orderID string) *descriptiveTripBuilder {
	b.assertCurrents()
	b.addSpan(orderID, b.currentTime, d, b.currentPoint, b.currentPoint, nonTransportSpan)
	b.After(d)
	return b
}

func (b *descriptiveTripBuilder) FlyTo(toPointID int, d time.Duration, orderID string) *descriptiveTripBuilder {
	b.assertCurrents()
	b.addSpan(orderID, b.currentTime, d, b.currentPoint, toPointID, transportSpan)
	b.After(d)
	b.IAmThere(toPointID)
	return b
}

func (b *descriptiveTripBuilder) Build(id, passportID string) *tripmodels.Trip {
	trip := tripmodels.NewTrip(id, passportID)
	for orderID, spans := range b.spans {
		state := ordercommons.EDisplayOrderState_OS_REFUNDED
		if b.orders[orderID] {
			state = ordercommons.EDisplayOrderState_OS_FULFILLED
		}
		orderInfo := tripmodels.NewOrderInfo(orderID, state, spans)
		trip.UpsertOrder(orderInfo)
	}
	return trip
}

func (b *descriptiveTripBuilder) addSpan(orderID string, startTime time.Time, d time.Duration, fromPoint int, toPoint int, isTransport bool) {
	b.ensureOrderID(orderID)
	s := models.NewSpan(
		models.NewFakeVisit(fromPoint, startTime),
		models.NewFakeVisit(toPoint, startTime.Add(d)),
		isTransport,
	)
	b.spans[orders.ID(orderID)] = append(b.spans[orders.ID(orderID)], s)
}

func (b *descriptiveTripBuilder) ensureOrderID(id string) {
	if _, found := b.orders[orders.ID(id)]; !found {
		b.RegisterOrder(id, true)
	}
}

func (b descriptiveTripBuilder) assertCurrents() {
	if b.currentTime.IsZero() {
		panic("must assign current time for trip building")
	}
	if b.currentPoint == 0 {
		panic("must assign current location for trip building")
	}
}
