package calendar

import (
	"a.yandex-team.ru/mail/iex/taksa/iex"
	"a.yandex-team.ru/mail/iex/taksa/logger"
	"a.yandex-team.ru/mail/iex/taksa/meta"
	"a.yandex-team.ru/mail/iex/taksa/request"
	"a.yandex-team.ru/mail/iex/taksa/tanker"
	"testing"
)

import . "a.yandex-team.ru/mail/iex/taksa/widgets/common"
import . "a.yandex-team.ru/mail/iex/matchers"

func extend(subtype, title, cmt string, isCancelled bool, endDate string, mobile bool, toAddresses []string) (Widget, error) {
	event := map[string]interface{}{
		"taksa_widget_type_1234543456546": "event-ticket",
		"widget_subtype":                  subtype,
		"title":                           title,
		"isCancelled":                     isCancelled,
		"end_date_rfc":                    endDate,
		"calendarMailType":                cmt,
		"organizer":                       map[string]interface{}{"email": "org@ya.ru", "name": "Organizer", "decision": "accepted"},
	}
	iexFactsArray := []interface{}{event}
	fact := iex.Fact{Envelope: meta.Envelope{Mid: "1", Types: []int{1}, ToAddresses: toAddresses}, IEX: iexFactsArray}

	return Class{
		Cfg:     Config{},
		Fact:    fact,
		Logger:  logger.Mock{},
		Request: request.RequestMock{Mobile: mobile},
		Tanker:  tanker.Mock{}}.Extend()
}

func err(_ Widget, e error) error     { return e }
func widget(w Widget, _ error) Widget { return w }

func TestExtend_noTitle_givesDeclinedWidget(t *testing.T) {
	AssertThat(t, widget(extend("calendar", "", "", false, "2038-05-22T15:00:00+03:00", false, []string{})), TypeOf{V: &DeclinedCalendarWidget{}})
}

func TestExtend_isCancelled_givesCancelledWidget(t *testing.T) {
	AssertThat(t, widget(extend("calendar", "a", "", true, "2038-05-22T15:00:00+03:00", false, []string{})), TypeOf{V: &CancelledCalendarWidget{}})
}

func TestExtend_isPast_givesPastWidget(t *testing.T) {
	AssertThat(t, widget(extend("calendar", "a", "", false, "2017-05-22T15:00:00+03:00", false, []string{})), TypeOf{V: &PastCalendarWidget{}})
}

func TestExtend_isUpdatedSubtype_givesPastWidget(t *testing.T) {
	AssertThat(t, widget(extend("calendar-updated", "a", "", false, "2038-05-22T15:00:00+03:00", false, []string{})), TypeOf{V: &PastCalendarWidget{}})
}

func TestExtend_isUpdateForEvent_givesPastWidget(t *testing.T) {
	AssertThat(t, widget(extend("calendar", "a", "event_update", false, "2038-05-22T15:00:00+03:00", false, []string{})), TypeOf{V: &UpdatedCalendarWidget{}})
}

func TestExtend_other_givesNewWidget(t *testing.T) {
	AssertThat(t, widget(extend("calendar", "a", "", false, "2038-05-22T15:00:00+03:00", false, []string{})), TypeOf{V: &NewCalendarWidget{}})
}

func TestExtend_mobile_givesNewWidget(t *testing.T) {
	AssertThat(t, widget(extend("calendar", "a", "", false, "2038-05-22T15:00:00+03:00", true, []string{})), TypeOf{V: &NewCalendarWidget{}})
}

func TestExtend_mobileForOrganizer_givesNil(t *testing.T) {
	AssertThat(t, widget(extend("calendar", "a", "", false, "2038-05-22T15:00:00+03:00", true, []string{"org@ya.ru"})), Is{V: nil})
}
