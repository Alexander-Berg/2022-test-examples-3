package calendar

import (
	"a.yandex-team.ru/mail/iex/taksa/iex"
	"a.yandex-team.ru/mail/iex/taksa/meta"
	"a.yandex-team.ru/mail/iex/taksa/tanker"
	"fmt"
	"testing"
)

import . "a.yandex-team.ru/mail/iex/matchers"
import . "a.yandex-team.ru/mail/iex/taksa/widgets/common"

func TestGetTitleUpdated(t *testing.T) {
	fact := iex.Fact{Envelope: meta.Envelope{}}
	class := Class{Fact: fact, Tanker: tanker.Mock{}}
	event := Event{IexDict{"title": "t"}}
	c, e := class.getTitleUpdated(event)
	AssertThat(t, e, Is{V: nil})
	AssertThat(t, c.Attributes["label"], Is{V: "_Updated_: t"})
}

func TestGetTitleCancelled(t *testing.T) {
	fact := iex.Fact{Envelope: meta.Envelope{}}
	class := Class{Fact: fact, Tanker: tanker.Mock{}}
	event := Event{IexDict{"title": "t"}}
	c, e := class.getTitleCancelled(event)
	AssertThat(t, e, Is{V: nil})
	AssertThat(t, c.Attributes["label"], Is{V: "Event \"t\" cancelled"})
}

func TestGetLocation_string(t *testing.T) {
	event := Event{IexDict{"location": "l"}}
	AssertThat(t, getLocation(event), Is{V: "l"})
}

func TestGetLocation_badType(t *testing.T) {
	event := Event{IexDict{"location": false}}
	AssertThat(t, getLocation(event), Is{V: ""})
}

func TestGetLocation_struct(t *testing.T) {
	event := Event{IexDict{"location": map[string]interface{}{"name": "l"}}}
	AssertThat(t, getLocation(event), Is{V: "l"})
}

func TestGetLocation_badStruct(t *testing.T) {
	event := Event{IexDict{"location": map[string]int{"name": 1}}}
	AssertThat(t, getLocation(event), Is{V: ""})
}

func TestGetLocation_nothing(t *testing.T) {
	event := Event{IexDict{}}
	AssertThat(t, getLocation(event), Is{V: ""})
}

func TestGetTime_noData(t *testing.T) {
	fact := iex.Fact{Envelope: meta.Envelope{}}
	class := Class{Fact: fact, Tanker: tanker.Mock{}}
	event := Event{IexDict{"x": "a"}}
	res := class.getTime(event, false)
	AssertThat(t, res, Is{V: ""})
}

func TestGetTime_nonRep(t *testing.T) {
	fact := iex.Fact{Envelope: meta.Envelope{}}
	class := Class{Fact: fact, Tanker: tanker.Mock{}}
	event := Event{IexDict{
		"start_date_rfc": "2017-12-05T14:00:00+04:00",
		"end_date_rfc":   "2017-12-05T14:30:00+04:00"}}
	res := class.getTime(event, false)
	AssertThat(t, res, Is{V: "Dec 5, Tue, from 14:00 till 14:30"})
}

func TestGetTime_rep(t *testing.T) {
	fact := iex.Fact{Envelope: meta.Envelope{}}
	class := Class{Fact: fact, Tanker: tanker.Mock{}}
	event := Event{IexDict{
		"start_date_rfc":        "2017-12-05T14:00:00+04:00",
		"end_date_rfc":          "2017-12-05T14:30:00+04:00",
		"repetitionDescription": "desc"}}
	res := class.getTime(event, false)
	AssertThat(t, res, Is{V: "&repeatable; from 14:00 till 14:30, desc"})
}

func TestGetTime_allDay(t *testing.T) {
	fact := iex.Fact{Envelope: meta.Envelope{}}
	class := Class{Fact: fact, Tanker: tanker.Mock{}}
	event := Event{IexDict{
		"start_date_rfc": "2017-12-05T14:00:00+04:00",
		"isAllDay":       true}}
	res := class.getTime(event, false)
	AssertThat(t, res, Is{V: "Dec 5, Tue"})
}

func TestGetTime_repAllDay(t *testing.T) {
	fact := iex.Fact{Envelope: meta.Envelope{}}
	class := Class{Fact: fact, Tanker: tanker.Mock{}}
	event := Event{IexDict{
		"start_date_rfc":        "2017-12-05T14:00:00+04:00",
		"isAllDay":              true,
		"repetitionDescription": "desc"}}
	res := class.getTime(event, false)
	AssertThat(t, res, Is{V: "&repeatable; desc"})
}

func TestGetTime_startOnly(t *testing.T) {
	fact := iex.Fact{Envelope: meta.Envelope{}}
	class := Class{Fact: fact, Tanker: tanker.Mock{}}
	event := Event{IexDict{
		"start_date_rfc": "2017-12-05T14:00:00+04:00"}}
	res := class.getTime(event, false)
	AssertThat(t, res, Is{V: "Dec 5, Tue"})
}

func TestGetTime_noData_cancelled(t *testing.T) {
	fact := iex.Fact{Envelope: meta.Envelope{}}
	class := Class{Fact: fact, Tanker: tanker.Mock{}}
	event := Event{IexDict{"x": "a"}}
	res := class.getTime(event, true)
	AssertThat(t, res, Is{V: ""})
}

func TestGetTime_nonRep_cancelled(t *testing.T) {
	fact := iex.Fact{Envelope: meta.Envelope{}}
	class := Class{Fact: fact, Tanker: tanker.Mock{}}
	event := Event{IexDict{
		"start_date_rfc": "2017-12-05T14:00:00+04:00",
		"end_date_rfc":   "2017-12-05T14:30:00+04:00"}}
	res := class.getTime(event, true)
	AssertThat(t, res, Is{V: "_FreeTime_Dec 5, Tue, from 14:00 till 14:30"})
}

func TestGetTime_rep_cancelled(t *testing.T) {
	fact := iex.Fact{Envelope: meta.Envelope{}}
	class := Class{Fact: fact, Tanker: tanker.Mock{}}
	event := Event{IexDict{
		"start_date_rfc":        "2017-12-05T14:00:00+04:00",
		"end_date_rfc":          "2017-12-05T14:30:00+04:00",
		"repetitionDescription": "desc"}}
	res := class.getTime(event, true)
	AssertThat(t, res, Is{V: "&repeatable;_FreeTime_ from 14:00 till 14:30, desc"})
}

func TestGetTime_allDay_cancelled(t *testing.T) {
	fact := iex.Fact{Envelope: meta.Envelope{}}
	class := Class{Fact: fact, Tanker: tanker.Mock{}}
	event := Event{IexDict{
		"start_date_rfc": "2017-12-05T14:00:00+04:00",
		"isAllDay":       true}}
	res := class.getTime(event, true)
	AssertThat(t, res, Is{V: "_FreeTime_Dec 5, Tue"})
}

func TestGetTime_repAllDay_cancelled(t *testing.T) {
	fact := iex.Fact{Envelope: meta.Envelope{}}
	class := Class{Fact: fact, Tanker: tanker.Mock{}}
	event := Event{IexDict{
		"start_date_rfc":        "2017-12-05T14:00:00+04:00",
		"isAllDay":              true,
		"repetitionDescription": "desc"}}
	res := class.getTime(event, true)
	AssertThat(t, res, Is{V: "&repeatable;_FreeTime_ desc"})
}

func TestGetTime_startOnly_cancelled(t *testing.T) {
	fact := iex.Fact{Envelope: meta.Envelope{}}
	class := Class{Fact: fact, Tanker: tanker.Mock{}}
	event := Event{IexDict{
		"start_date_rfc": "2017-12-05T14:00:00+04:00"}}
	res := class.getTime(event, true)
	AssertThat(t, res, Is{V: "_FreeTime_Dec 5, Tue"})
}

type ParticipantDetails struct {
	Status  string
	Name    string
	Address string
}

func (details ParticipantDetails) Match(i interface{}) bool {
	participant, ok := i.(Participant)
	return ok &&
		participant.Status == details.Status &&
		participant.Name == details.Name &&
		participant.Address == details.Address
}

func (details ParticipantDetails) String() string {
	return fmt.Sprintf("participant details status=%v, name=%v, details=%v",
		details.Status, details.Name, details.Address)
}

func TestGetParticipants(t *testing.T) {
	fact := iex.Fact{Envelope: meta.Envelope{}}
	class := Class{Fact: fact, Tanker: tanker.Mock{}}
	event := Event{IexDict{
		"people": 5,
		"organizer": map[string]interface{}{
			"name":     "Me",
			"email":    "me@ya.ru",
			"decision": "accepted",
		},
		"attendees": []interface{}{
			map[string]interface{}{
				"name":     "G1",
				"decision": "undecided",
				"email":    "g1@ya.ru",
			},
			map[string]interface{}{
				"name":     "G2",
				"decision": "accepted",
				"email":    "g2@ya.ru",
			},
			map[string]interface{}{
				"name":     "G3",
				"decision": "declined",
				"email":    "g3@ya.ru",
			},
			map[string]interface{}{
				"name":     "G4",
				"decision": "unknown",
				"email":    "g4@ya.ru",
			}}}}
	c, e := class.getParticipants(event)
	AssertThat(t, e, Is{V: nil})
	AssertThat(t, c.Role, Is{V: Action1.String()})
	AssertThat(t, c.Type, Is{V: Drop})
	AssertThat(t, c.Attributes["label"], Is{V: "5&nbsp;&man;&man;"})
	arr, ok := c.Attributes["value"].([]Participant)
	AssertThat(t, ok, Is{V: true})
	AssertThat(t, len(arr), Is{V: 5})
	AssertThat(t, arr[0], ParticipantDetails{"&organizer;", "Me", "me@ya.ru"})
	AssertThat(t, arr[1], ParticipantDetails{"&undecided;", "G1", "g1@ya.ru"})
	AssertThat(t, arr[2], ParticipantDetails{"&accepted;", "G2", "g2@ya.ru"})
	AssertThat(t, arr[3], ParticipantDetails{"&declined;", "G3", "g3@ya.ru"})
	AssertThat(t, arr[4], ParticipantDetails{"", "G4", "g4@ya.ru"})
}

type ButtonDetails struct {
	Label             string
	Value             string
	Type              ControlType
	ExternalEventID   string
	RecurrenceEventID string
	GoToState         int
	Yellow            bool
}

func (details ButtonDetails) Match(i interface{}) bool {
	button, ok := i.(SingleButton)
	return ok &&
		button.Label == details.Label &&
		button.Value == details.Value &&
		button.Type == details.Type &&
		button.ExternalEventID == details.ExternalEventID &&
		button.RecurrenceEventID == details.RecurrenceEventID &&
		button.GoToState == details.GoToState &&
		button.Yellow == details.Yellow
}

func (details ButtonDetails) String() string {
	return fmt.Sprintf("button details label=%v, value=%v, type=%v, externalid=%v, recurrenceid=%v, gotostate=%v, yellow=%v",
		details.Label, details.Value, details.Type, details.ExternalEventID, details.RecurrenceEventID, details.GoToState, details.Yellow)
}

func TestGetButtons_iAmOrg(t *testing.T) {
	fact := iex.Fact{Envelope: meta.Envelope{ToAddresses: []string{"e"}}}
	class := Class{Fact: fact, Tanker: tanker.Mock{}}
	event := Event{IexDict{
		"organizer":   map[string]interface{}{"name": "n", "decision": "d", "email": "e"},
		"calendarUrl": "url"}}
	res, _ := class.getButtons(event)
	states, _ := res.Attributes["states"].([]ButtonState)
	AssertThat(t, len(states), Is{V: 1})
	state1 := states[0]
	AssertThat(t, state1.ID, Is{V: 1})
	AssertThat(t, len(state1.ButtonsInState), Is{V: 1})
	button1 := state1.ButtonsInState[0]
	AssertThat(t, button1, ButtonDetails{"_Look_", "url", Link, "", "", 1, false})
}

func TestGetButtons_undecided(t *testing.T) {
	fact := iex.Fact{Envelope: meta.Envelope{}}
	class := Class{Fact: fact, Tanker: tanker.Mock{}}
	event := Event{IexDict{
		"decision":          "undecided",
		"externalEventId":   "123",
		"recurrenceEventId": "124",
		"calendarUrl":       "url"}}
	res, _ := class.getButtons(event)
	states, _ := res.Attributes["states"].([]ButtonState)
	AssertThat(t, len(states), Is{V: 3})

	state1 := states[0]
	AssertThat(t, state1.ID, Is{V: 1})
	AssertThat(t, len(state1.ButtonsInState), Is{V: 2})
	button11 := state1.ButtonsInState[0]
	AssertThat(t, button11, ButtonDetails{"_WillGo_", "calendar_accept", Button, "123", "124", 2, true})
	button12 := state1.ButtonsInState[1]
	AssertThat(t, button12, ButtonDetails{"_WillNotGo_", "calendar_decline", Button, "123", "124", 3, false})

	state2 := states[1]
	AssertThat(t, state2.ID, Is{V: 2})
	AssertThat(t, len(state2.ButtonsInState), Is{V: 2})
	button21 := state2.ButtonsInState[0]
	AssertThat(t, button21, ButtonDetails{"_WillNotGo_", "calendar_decline", Button, "123", "124", 3, false})
	button22 := state2.ButtonsInState[1]
	AssertThat(t, button22, ButtonDetails{"_Look_", "url", Link, "", "", 2, false})

	state3 := states[2]
	AssertThat(t, state3.ID, Is{V: 3})
	AssertThat(t, len(state3.ButtonsInState), Is{V: 2})
	button31 := state3.ButtonsInState[0]
	AssertThat(t, button31, ButtonDetails{"_WillGo_", "calendar_accept", Button, "123", "124", 2, false})
	button32 := state3.ButtonsInState[1]
	AssertThat(t, button32, ButtonDetails{"_Look_", "url", Link, "", "", 3, false})
}

func TestGetButtons_declined(t *testing.T) {
	fact := iex.Fact{Envelope: meta.Envelope{}}
	class := Class{Fact: fact, Tanker: tanker.Mock{}}
	event := Event{IexDict{
		"decision":          "declined",
		"externalEventId":   "123",
		"recurrenceEventId": "124",
		"calendarUrl":       "url"}}
	res, _ := class.getButtons(event)
	states, _ := res.Attributes["states"].([]ButtonState)
	AssertThat(t, len(states), Is{V: 2})

	state1 := states[0]
	AssertThat(t, state1.ID, Is{V: 1})
	AssertThat(t, len(state1.ButtonsInState), Is{V: 2})
	button11 := state1.ButtonsInState[0]
	AssertThat(t, button11, ButtonDetails{"_WillGo_", "calendar_accept", Button, "123", "124", 2, false})
	button12 := state1.ButtonsInState[1]
	AssertThat(t, button12, ButtonDetails{"_Look_", "url", Link, "", "", 1, false})

	state2 := states[1]
	AssertThat(t, state2.ID, Is{V: 2})
	AssertThat(t, len(state2.ButtonsInState), Is{V: 2})
	button21 := state2.ButtonsInState[0]
	AssertThat(t, button21, ButtonDetails{"_WillNotGo_", "calendar_decline", Button, "123", "124", 1, false})
	button22 := state2.ButtonsInState[1]
	AssertThat(t, button22, ButtonDetails{"_Look_", "url", Link, "", "", 2, false})
}

func TestGetButtons_accepted(t *testing.T) {
	fact := iex.Fact{Envelope: meta.Envelope{}}
	class := Class{Fact: fact, Tanker: tanker.Mock{}}
	event := Event{IexDict{
		"decision":          "accepted",
		"externalEventId":   "123",
		"recurrenceEventId": "124",
		"calendarUrl":       "url"}}
	res, _ := class.getButtons(event)
	states, _ := res.Attributes["states"].([]ButtonState)
	AssertThat(t, len(states), Is{V: 2})

	state1 := states[0]
	AssertThat(t, state1.ID, Is{V: 1})
	AssertThat(t, len(state1.ButtonsInState), Is{V: 2})
	button11 := state1.ButtonsInState[0]
	AssertThat(t, button11, ButtonDetails{"_WillNotGo_", "calendar_decline", Button, "123", "124", 2, false})
	button12 := state1.ButtonsInState[1]
	AssertThat(t, button12, ButtonDetails{"_Look_", "url", Link, "", "", 1, false})

	state2 := states[1]
	AssertThat(t, state2.ID, Is{V: 2})
	AssertThat(t, len(state2.ButtonsInState), Is{V: 2})
	button21 := state2.ButtonsInState[0]
	AssertThat(t, button21, ButtonDetails{"_WillGo_", "calendar_accept", Button, "123", "124", 1, false})
	button22 := state2.ButtonsInState[1]
	AssertThat(t, button22, ButtonDetails{"_Look_", "url", Link, "", "", 2, false})
}
