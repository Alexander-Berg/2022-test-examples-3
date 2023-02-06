package hotels

import (
	"a.yandex-team.ru/mail/iex/taksa/client"
	"a.yandex-team.ru/mail/iex/taksa/iex"
	"a.yandex-team.ru/mail/iex/taksa/logger"
	"a.yandex-team.ru/mail/iex/taksa/meta"
	"a.yandex-team.ru/mail/iex/taksa/request"
	"a.yandex-team.ru/mail/iex/taksa/tanker"
	"fmt"
	"testing"
	"time"
)

import . "a.yandex-team.ru/mail/iex/taksa/widgets/common"
import . "a.yandex-team.ru/mail/iex/matchers"

const (
	future  string = "3000-01-01T00:00:00+00:00"
	past    string = "2000-01-01T00:00:00+00:00"
	invalid string = "this is not a time string"
)

func extend(iexSubtype, date string) (Widget, error) {
	iexFactsArray := []interface{}{}
	hotel := map[string]interface{}{
		"taksa_widget_type_1234543456546": "hotels",
		"widget_subtype":                  iexSubtype,
		"checkin_date_rfc":                date,
		"checkout_date_rfc":               date,
	}
	iexFactsArray = append(iexFactsArray, hotel)
	fact := iex.Fact{Envelope: meta.Envelope{Mid: "1", Types: []int{1}}, IEX: iexFactsArray}

	return Class{
		Fact:    fact,
		Logger:  logger.Mock{},
		Client:  client.Mock{},
		Request: request.RequestMock{},
		Tanker:  tanker.Mock{}}.Extend()
}

func err(_ Widget, e error) error     { return e }
func widget(w Widget, _ error) Widget { return w }

func TestExtend_invalidIexSubtype_givesError(t *testing.T) {
	AssertThat(t, err(extend("", future)), Not{V: nil})
}

func TestExtend_reminderWithPastTime_givesNoWidget(t *testing.T) {
	AssertThat(t, widget(extend("reminder", past)), Is{V: nil})
}

func TestExtend_reminderWithFutureTime_givesReminderWidget(t *testing.T) {
	//DARIA-58212
	// AssertThat(t, widget(extend("reminder", future)), TypeOf{V: &ReminderWidget{}})
	AssertThat(t, widget(extend("reminder", future)), Is{V: nil})
}

func TestExtend_bookingWithFutureTime_givesBookingWidget(t *testing.T) {
	AssertThat(t, widget(extend("booking", future)), TypeOf{V: &BookingWidget{}})
}

func TestExtend_bookingWithInvalidTime_givesBookingWidget(t *testing.T) {
	AssertThat(t, widget(extend("booking", invalid)), TypeOf{V: &BookingWidget{}})
}

func TestExtend_bookingWithPastTime_givesExpiredWidget(t *testing.T) {
	AssertThat(t, widget(extend("booking", past)), TypeOf{V: &ExpiredWidget{}})
}

func TestExtend_canceledWithValidTime_givesExpiredWidget(t *testing.T) {
	AssertThat(t, widget(extend("canceled", future)), TypeOf{V: &ExpiredWidget{}})
	AssertThat(t, widget(extend("canceled", past)), TypeOf{V: &ExpiredWidget{}})
}

func TestExtend_cancelingWithValidTime_givesCancelingWidget(t *testing.T) {
	AssertThat(t, widget(extend("canceling", future)), TypeOf{V: &CancelingWidget{}})
	AssertThat(t, widget(extend("canceling", past)), TypeOf{V: &CancelingWidget{}})
}

func TestTomorrowSubtype_checkinTomorrow(t *testing.T) {
	tomorrow := fmt.Sprintf("%v", time.Now().AddDate(0, 0, 1).Format(time.RFC3339))
	AssertThat(t, widget(extend("booking", tomorrow)), TypeOf{V: &TomorrowWidget{}})
}

func TestIsTomorrow_checkinIsTheDayAfterTomorrow_false(t *testing.T) {
	theDayAfterTomorrow := fmt.Sprintf("%v", time.Now().AddDate(0, 0, 2).Format(time.RFC3339))
	hotel := Hotel{IexDict{
		"checkout_date_rfc": future,
		"checkin_date_rfc":  theDayAfterTomorrow,
	}}
	AssertThat(t, isTomorrow(hotel), Is{V: false})
}

func TestIsTomorrow_checkinIsTomorrow_true(t *testing.T) {
	tomorrow := fmt.Sprintf("%v", time.Now().AddDate(0, 0, 1).Format(time.RFC3339))
	hotel := Hotel{IexDict{
		"checkout_date_rfc": future,
		"checkin_date_rfc":  tomorrow,
	}}
	AssertThat(t, isTomorrow(hotel), Is{V: true})
}

func TestIsTomorrow_checkinIsToday_true(t *testing.T) {
	today := fmt.Sprintf("%v", time.Now().Format(time.RFC3339))
	hotel := Hotel{IexDict{
		"checkout_date_rfc": future,
		"checkin_date_rfc":  today,
	}}
	AssertThat(t, isTomorrow(hotel), Is{V: true})
}

func TestIsTomorrow_checkinIsYesterdayCheckoutIsTomorrow_true(t *testing.T) {
	yesterday := fmt.Sprintf("%v", time.Now().AddDate(0, 0, -1).Format(time.RFC3339))
	tomorrow := fmt.Sprintf("%v", time.Now().AddDate(0, 0, 1).Format(time.RFC3339))
	hotel := Hotel{IexDict{
		"checkout_date_rfc": tomorrow,
		"checkin_date_rfc":  yesterday,
	}}
	AssertThat(t, isTomorrow(hotel), Is{V: true})
}

func TestIsTomorrow_checkinIsPastCheckoutIsToday_true(t *testing.T) {
	today := fmt.Sprintf("%v", time.Now().Format(time.RFC3339))
	hotel := Hotel{IexDict{
		"checkout_date_rfc": today,
		"checkin_date_rfc":  past,
	}}
	AssertThat(t, isTomorrow(hotel), Is{V: true})
}

func TestIsTomorrow_checkinIsPastCheckoutIsYesterday_false(t *testing.T) {
	yesterday := fmt.Sprintf("%v", time.Now().AddDate(0, 0, -1).Format(time.RFC3339))
	hotel := Hotel{IexDict{
		"checkout_date_rfc": yesterday,
		"checkin_date_rfc":  past,
	}}
	AssertThat(t, isTomorrow(hotel), Is{V: false})
}
