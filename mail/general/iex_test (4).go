package calendar

import (
	"a.yandex-team.ru/mail/iex/taksa/iex"
	"a.yandex-team.ru/mail/iex/taksa/logger"
	"a.yandex-team.ru/mail/iex/taksa/meta"
	"a.yandex-team.ru/mail/iex/taksa/widgets/common"
	"testing"
	"time"
)

import . "a.yandex-team.ru/mail/iex/matchers"

func TestGetEvent_emptyFactsArray_givesError(t *testing.T) {
	facts := iex.Fact{Envelope: meta.Envelope{Mid: "1", Types: []int{1}}, IEX: []interface{}{}}
	class := Class{Fact: facts, Logger: logger.Mock{}}
	_, err := class.getEvent()
	AssertThat(t, err, Not{V: Is{V: nil}})
}

func TestGetEvent_wrongStructType_givesError(t *testing.T) {
	some := []int{42}
	facts := iex.Fact{Envelope: meta.Envelope{Mid: "1", Types: []int{1}}, IEX: []interface{}{some}}
	class := Class{Fact: facts, Logger: logger.Mock{}}
	_, err := class.getEvent()
	AssertThat(t, err, Not{V: nil})
}

func TestGetEvent_badType_givesError(t *testing.T) {
	data := map[string]interface{}{
		"taksa_widget_type_1234543456546": "bad",
	}
	iexFactsArray := []interface{}{data}
	fact := iex.Fact{Envelope: meta.Envelope{Mid: "1", Types: []int{1}}, IEX: iexFactsArray}
	class := Class{Fact: fact, Logger: logger.Mock{}}
	_, err := class.getEvent()
	AssertThat(t, err, Not{V: nil})
}

func TestGetEvent_badSubType_givesError(t *testing.T) {
	data := map[string]interface{}{
		"taksa_widget_type_1234543456546": "event-ticket",
		"widget_subtype":                  "bad",
	}
	iexFactsArray := []interface{}{data}
	fact := iex.Fact{Envelope: meta.Envelope{Mid: "1", Types: []int{1}}, IEX: iexFactsArray}
	class := Class{Fact: fact, Logger: logger.Mock{}}
	_, err := class.getEvent()
	AssertThat(t, err, Not{V: nil})
}

func str(s string, _ error) string       { return s }
func intt(i int, _ error) int            { return i }
func tim(t time.Time, _ error) time.Time { return t }

func TestGetEvent_goodData_givesFields(t *testing.T) {
	data := map[string]interface{}{
		"taksa_widget_type_1234543456546": "event-ticket",
		"widget_subtype":                  "calendar",
		"people":                          "3",
	}
	iexFactsArray := []interface{}{data}
	fact := iex.Fact{Envelope: meta.Envelope{Mid: "1", Types: []int{1}}, IEX: iexFactsArray}
	class := Class{Fact: fact, Logger: logger.Mock{}}
	event, err := class.getEvent()
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, intt(event.People()), Is{V: 3})
}

func TestGetEvent_goodDataUpdated_givesFields(t *testing.T) {
	data := map[string]interface{}{
		"taksa_widget_type_1234543456546": "event-ticket",
		"widget_subtype":                  "calendar-updated",
		"people":                          "3",
	}
	iexFactsArray := []interface{}{data}
	fact := iex.Fact{Envelope: meta.Envelope{Mid: "1", Types: []int{1}}, IEX: iexFactsArray}
	class := Class{Fact: fact, Logger: logger.Mock{}}
	event, err := class.getEvent()
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, intt(event.People()), Is{V: 3})
}

func TestHasNoTitle_hasNonEmptyTitle_false(t *testing.T) {
	event := Event{common.IexDict{
		"title": "a",
	}}
	AssertThat(t, event.HasNoTitle(), Is{V: false})
}

func TestHasNoTitle_hasEmptyTitle_true(t *testing.T) {
	event := Event{common.IexDict{
		"title": "",
	}}
	AssertThat(t, event.HasNoTitle(), Is{V: true})
}

func TestHasNoTitle_hasNoTitle_true(t *testing.T) {
	event := Event{common.IexDict{
		"x": "a",
	}}
	AssertThat(t, event.HasNoTitle(), Is{V: true})
}

func TestIsCancelled_noField_false(t *testing.T) {
	event := Event{common.IexDict{
		"x": "a",
	}}
	AssertThat(t, event.IsCancelled(), Is{V: false})
}

func TestIsCancelled_wrongType_false(t *testing.T) {
	event := Event{common.IexDict{
		"isCancelled": "a",
	}}
	AssertThat(t, event.IsCancelled(), Is{V: false})
}

func TestIsCancelled_false_false(t *testing.T) {
	event := Event{common.IexDict{
		"isCancelled": false,
	}}
	AssertThat(t, event.IsCancelled(), Is{V: false})
}

func TestIsCancelled_true_true(t *testing.T) {
	event := Event{common.IexDict{
		"isCancelled": true,
	}}
	AssertThat(t, event.IsCancelled(), Is{V: true})
}

func TestIsPast_noField_false(t *testing.T) {
	event := Event{common.IexDict{
		"x": "a",
	}}
	AssertThat(t, event.IsPast(), Is{V: false})
}

func TestIsPast_wrongFormat_false(t *testing.T) {
	event := Event{common.IexDict{
		"end_date_rfc": "a",
	}}
	AssertThat(t, event.IsPast(), Is{V: false})
}

func TestIsPast_endFuture_false(t *testing.T) {
	event := Event{common.IexDict{
		"end_date_rfc": "2038-05-22T15:00:00+03:00",
	}}
	AssertThat(t, event.IsPast(), Is{V: false})
}

func TestIsPast_endPast_true(t *testing.T) {
	event := Event{common.IexDict{
		"end_date_rfc": "2017-05-22T15:00:00+03:00",
	}}
	AssertThat(t, event.IsPast(), Is{V: true})
}

func TestIsPast_lastPast_true(t *testing.T) {
	event := Event{common.IexDict{
		"dateEventLastRepetition": "2017-05-22T15:00:00+03:00",
	}}
	AssertThat(t, event.IsPast(), Is{V: true})
}

func TestIsPast_endPastLastFuture_false(t *testing.T) {
	event := Event{common.IexDict{
		"dateEventLastRepetition": "2038-05-22T15:00:00+03:00",
		"end_date_rfc":            "2017-05-22T15:00:00+03:00",
	}}
	AssertThat(t, event.IsPast(), Is{V: false})
}

func TestIsPast_untilPast_true(t *testing.T) {
	event := Event{common.IexDict{
		"dateEventRepeatUntil": "2017-05-22T15:00:00+03:00",
	}}
	AssertThat(t, event.IsPast(), Is{V: true})
}

func TestIsPast_endPastUntilFuture_false(t *testing.T) {
	event := Event{common.IexDict{
		"dateEventRepeatUntil": "2038-05-22T15:00:00+03:00",
		"end_date_rfc":         "2017-05-22T15:00:00+03:00",
	}}
	AssertThat(t, event.IsPast(), Is{V: false})
}

func TestIsPast_lastPastUntilFuture_true(t *testing.T) {
	event := Event{common.IexDict{
		"dateEventRepeatUntil":    "2038-05-22T15:00:00+03:00",
		"dateEventLastRepetition": "2017-05-22T15:00:00+03:00",
	}}
	AssertThat(t, event.IsPast(), Is{V: true})
}

func TestIsUpdated_noField_false(t *testing.T) {
	event := Event{common.IexDict{
		"x": "a",
	}}
	AssertThat(t, event.IsUpdated(), Is{V: false})
}

func TestIsUpdated_wrongType_false(t *testing.T) {
	event := Event{common.IexDict{
		"calendarMailType": 42,
	}}
	AssertThat(t, event.IsUpdated(), Is{V: false})
}

func TestIsUpdated_false_false(t *testing.T) {
	event := Event{common.IexDict{
		"calendarMailType": "zzzzz",
	}}
	AssertThat(t, event.IsUpdated(), Is{V: false})
}

func TestIsUpdated_true_true(t *testing.T) {
	event := Event{common.IexDict{
		"calendarMailType": "event_update",
	}}
	AssertThat(t, event.IsUpdated(), Is{V: true})
}

func TestIsRepeatable_noField_false(t *testing.T) {
	event := Event{common.IexDict{
		"x": "a",
	}}
	AssertThat(t, event.IsRepeatable(), Is{V: false})
}

func TestIsRepeatable_badType_false(t *testing.T) {
	event := Event{common.IexDict{
		"repetitionDescription": 123,
	}}
	AssertThat(t, event.IsRepeatable(), Is{V: false})
}

func TestIsRepeatable_emptyField_false(t *testing.T) {
	event := Event{common.IexDict{
		"repetitionDescription": "",
	}}
	AssertThat(t, event.IsRepeatable(), Is{V: false})
}

func TestIsRepeatable_hasField_true(t *testing.T) {
	event := Event{common.IexDict{
		"repetitionDescription": "aaa",
	}}
	AssertThat(t, event.IsRepeatable(), Is{V: true})
}

func TestIsRepeatable_hasField1PositiveAndField2Negative_true(t *testing.T) {
	event := Event{common.IexDict{
		"repetitionDescription": "aaa",
		"isRecurrence":          false,
	}}
	AssertThat(t, event.IsRepeatable(), Is{V: true})
}

func TestIsRepeatable_hasField1NegativeAndField2BadType_false(t *testing.T) {
	event := Event{common.IexDict{
		"repetitionDescription": "",
		"isRecurrence":          "zzz",
	}}
	AssertThat(t, event.IsRepeatable(), Is{V: false})
}

func TestIsRepeatable_hasField1NegativeAndField2Negative_false(t *testing.T) {
	event := Event{common.IexDict{
		"repetitionDescription": "",
		"isRecurrence":          false,
	}}
	AssertThat(t, event.IsRepeatable(), Is{V: false})
}

func TestIsRepeatable_hasField1NegativeAndField2Positive_true(t *testing.T) {
	event := Event{common.IexDict{
		"repetitionDescription": "",
		"isRecurrence":          true,
	}}
	AssertThat(t, event.IsRepeatable(), Is{V: true})
}

func TestGetParticipants_noData_emptyArr(t *testing.T) {
	event := Event{common.IexDict{
		"x": "a",
	}}
	res := event.GetParticipants()
	AssertThat(t, len(res), Is{V: 0})
}

func TestGetParticipants_badType_emptyArr(t *testing.T) {
	event := Event{common.IexDict{
		"attendees": "a",
	}}
	res := event.GetParticipants()
	AssertThat(t, len(res), Is{V: 0})
}

func TestGetParticipants_badType2_emptyArr(t *testing.T) {
	event := Event{common.IexDict{
		"attendees": []interface{}{42, false},
	}}
	res := event.GetParticipants()
	AssertThat(t, len(res), Is{V: 0})
}

func TestGetParticipants_one_correct_fields(t *testing.T) {
	event := Event{common.IexDict{
		"attendees": []interface{}{
			map[string]interface{}{"name": "n", "decision": "d", "email": "e"},
		},
	}}
	res := event.GetParticipants()[0]
	AssertThat(t, res.name, Is{V: "n"})
	AssertThat(t, res.decision, Is{V: "d"})
	AssertThat(t, res.email, Is{V: "e"})
}

func TestGetParticipants_data_arrWithTwo(t *testing.T) {
	event := Event{common.IexDict{
		"attendees": []interface{}{
			map[string]interface{}{"name": "n1", "decision": "d1", "email": "e1"},
			map[string]interface{}{"name": "n2", "decision": "d2", "email": "e2"},
			map[string]interface{}{"some": "shit"},
		},
	}}
	res := event.GetParticipants()
	AssertThat(t, len(res), Is{V: 2})
}

func TestOrganizer_hasData_ok(t *testing.T) {
	event := Event{common.IexDict{
		"organizer": map[string]interface{}{"name": "n", "decision": "d", "email": "e"},
	}}
	o, e := event.GetOrganizer()
	AssertThat(t, e, Is{V: nil})
	AssertThat(t, o.name, Is{V: "n"})
	AssertThat(t, o.decision, Is{V: "d"})
	AssertThat(t, o.email, Is{V: "e"})
}

func TestOrganizer_noData_err(t *testing.T) {
	event := Event{common.IexDict{
		"x": "a",
	}}
	_, e := event.GetOrganizer()
	AssertThat(t, e, Is{V: Not{V: nil}})
}

func TestOrganizer_badData_ok(t *testing.T) {
	event := Event{common.IexDict{
		"organizer": map[string]interface{}{"name": "n", "decision": 42, "email": "e"},
	}}
	_, e := event.GetOrganizer()
	AssertThat(t, e, Is{V: Not{V: nil}})
}
