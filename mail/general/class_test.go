package avia

import (
	"a.yandex-team.ru/mail/iex/taksa/client"
	"a.yandex-team.ru/mail/iex/taksa/iex"
	"a.yandex-team.ru/mail/iex/taksa/logger"
	"a.yandex-team.ru/mail/iex/taksa/meta"
	"a.yandex-team.ru/mail/iex/taksa/request"
	"a.yandex-team.ru/mail/iex/taksa/tanker"
	"testing"
)

import . "a.yandex-team.ru/mail/iex/taksa/widgets/common"
import . "a.yandex-team.ru/mail/iex/matchers"

const (
	future string = "3000-01-01T00:00:00+00:00"
	past   string = "2000-01-01T00:00:00+00:00"
)

func extend(iexSubtype, date string) (Widget, error) {
	return extendWithCustomDateArr(iexSubtype, date, date, []int{1})
}

func extendWithCustomTypes(iexSubtype, date string, types []int) (Widget, error) {
	return extendWithCustomDateArr(iexSubtype, date, date, types)
}

func extendWithCustomDateArr(iexSubtype, dateDep, dateArr string, types []int) (Widget, error) {
	iexFactsArray := []interface{}{}
	flight := map[string]interface{}{
		"widget_subtype": iexSubtype,
		"date_dep_rfc":   dateDep,
		"date_arr_rfc":   dateArr,
	}
	ticket := map[string]interface{}{
		"taksa_widget_type_1234543456546": "ticket",
		"ticket":                          []interface{}{flight},
	}
	iexFactsArray = append(iexFactsArray, ticket)
	fact := iex.Fact{Envelope: meta.Envelope{Mid: "1", Types: types}, IEX: iexFactsArray}

	return Class{
		Fact:    fact,
		Logger:  logger.Mock{},
		Client:  client.Mock{},
		Request: request.RequestMock{},
		Tanker:  tanker.Mock{}}.Extend()
}

func TestExtend_badIexSubtype_returnsError(t *testing.T) {
	_, err := extend("", future)
	AssertThat(t, err, Not{V: nil})
}

func TestExtend_bookingIexSubtypeWithFutureDate_returnsFactlessAviaWidget(t *testing.T) {
	w, err := extend("booking", future)
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, w, TypeOf{V: &FactlessAviaWidget{}})
}

func TestExtend_bookingIexSubtypeWithPastDate_returnsFactlessAviaWidget(t *testing.T) {
	w, err := extend("booking", past)
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, w, TypeOf{V: &FactlessAviaWidget{}})
}

func TestExtend_cancelingIexSubtypeWithFutureDate_returnsFactlessAviaWidget(t *testing.T) {
	w, err := extend("canceling", future)
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, w, TypeOf{V: &FactlessAviaWidget{}})
}

func TestExtend_cancelingIexSubtypeWithPastDate_returnsFactlessAviaWidget(t *testing.T) {
	w, err := extend("canceling", past)
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, w, TypeOf{V: &FactlessAviaWidget{}})
}

func TestExtend_changingIexSubtypeWithFutureDate_returnsFactlessAviaWidget(t *testing.T) {
	w, err := extend("changing", future)
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, w, TypeOf{V: &FactlessAviaWidget{}})
}

func TestExtend_changingIexSubtypeWithPastDate_returnsFactlessAviaWidget(t *testing.T) {
	w, err := extend("changing", past)
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, w, TypeOf{V: &FactlessAviaWidget{}})
}

func TestExtend_undefinedIexSubtypeWithFutureDate_returnsFactlessAviaWidget(t *testing.T) {
	w, err := extend("undefined", future)
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, w, TypeOf{V: &FactlessAviaWidget{}})
}

func TestExtend_undefinedIexSubtypeWithPastDate_returnsFactlessAviaWidget(t *testing.T) {
	w, err := extend("undefined", past)
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, w, TypeOf{V: &FactlessAviaWidget{}})
}

func TestExtend_reminderIexSubtypeWithFutureDate_returnsReminderAviaWidget(t *testing.T) {
	w, _ := extend("reminder", future)
	AssertThat(t, w, TypeOf{V: &ReminderWidget{}})
}

func TestExtend_reminderSOTypeWithFutureDate_returnsReminderAviaWidget(t *testing.T) {
	w, _ := extendWithCustomTypes("", future, []int{63})
	AssertThat(t, w, TypeOf{V: &ReminderWidget{}})
}

func TestExtend_reminderIexSubtypeWithPastDate_returnsNoWidget(t *testing.T) {
	w, _ := extend("reminder", past)
	AssertThat(t, w, Is{V: nil})
}

func TestExtend_reminderSOTypeWithPastDate_returnsNoWidget(t *testing.T) {
	w, _ := extendWithCustomTypes("", past, []int{63})
	AssertThat(t, w, Is{V: nil})
}

func TestExtend_boardingPassIexSubtypeWithFutureDate_returnsBoardingPassWidget(t *testing.T) {
	w, _ := extend("boardingpass", future)
	AssertThat(t, w, TypeOf{V: &BoardingPassWidget{}})
}

func TestExtend_boardingPassSOTypeWithFutureDate_returnsBoardingPassWidget(t *testing.T) {
	w, _ := extendWithCustomTypes("", future, []int{60})
	AssertThat(t, w, TypeOf{V: &BoardingPassWidget{}})
}

func TestExtend_boardingPassIexSubtypeWithPastDate_returnsExpiredTicketWidget(t *testing.T) {
	w, err := extend("boardingpass", past)
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, w, TypeOf{V: &ExpiredBoardingPassWidget{}})
}

func TestExtend_boardingPassSOTypeWithPastDate_returnsExpiredTicketWidget(t *testing.T) {
	w, err := extendWithCustomTypes("", past, []int{60})
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, w, TypeOf{V: &ExpiredBoardingPassWidget{}})
}

func TestExtend_bookingTodayIexSubtypeWithFutureDate_returnsFlightTomorrowWidget(t *testing.T) {
	w, err := extend("booking-today", future)
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, w, TypeOf{V: &FlightTomorrowWidget{}})
}

func TestExtend_bookingTodayIexSubtypeDepartedNotExpired_returnsTicketAviaWidget(t *testing.T) {
	w, err := extendWithCustomDateArr("booking-today", past, future, []int{1})
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, w, TypeOf{V: &TicketWidget{}})
}

func TestExtend_bookingTodayIexSubtypeWithPastDate_returnsTicketAviaWidget(t *testing.T) {
	w, err := extend("booking-today", past)
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, w, TypeOf{V: &ExpiredTicketWidget{}})
}

func TestExtend_bookingTodayIexSubtypeWithFutureDate_returnsTicketWidget(t *testing.T) {
	w, err := extend("eticket", future)
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, w, TypeOf{V: &TicketWidget{}})
}

func TestExtend_bookingTodayIexSubtypeWithPastDate_returnsExpiredTicketWidget(t *testing.T) {
	w, err := extend("eticket", past)
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, w, TypeOf{V: &ExpiredTicketWidget{}})
}
