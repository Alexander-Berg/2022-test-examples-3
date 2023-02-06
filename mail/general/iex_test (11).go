package tracker

import (
	"a.yandex-team.ru/mail/iex/taksa/iex"
	"a.yandex-team.ru/mail/iex/taksa/logger"
	"a.yandex-team.ru/mail/iex/taksa/meta"
	"testing"
)

import . "a.yandex-team.ru/mail/iex/matchers"

func TestGetTrackerInfo_emptyFactsArray_givesError(t *testing.T) {
	facts := iex.Fact{Envelope: meta.Envelope{Mid: "1", Types: []int{1}}, IEX: []interface{}{}}
	class := Class{Fact: facts, Logger: logger.Mock{}}
	_, err := class.getTrackerInfo()
	AssertThat(t, err, Not{V: nil})
}

func TestGetTrackerInfo_wrongStructType_givesError(t *testing.T) {
	some := []int{42}
	facts := iex.Fact{Envelope: meta.Envelope{Mid: "1", Types: []int{1}}, IEX: []interface{}{some}}
	class := Class{Fact: facts, Logger: logger.Mock{}}
	_, err := class.getTrackerInfo()
	AssertThat(t, err, Not{V: nil})
}

func TestGetTrackerInfo_badType_givesError(t *testing.T) {
	data := map[string]interface{}{
		"taksa_widget_type_1234543456546": "bad",
	}
	iexFactsArray := []interface{}{data}
	fact := iex.Fact{Envelope: meta.Envelope{Mid: "1", Types: []int{1}}, IEX: iexFactsArray}
	class := Class{Fact: fact, Logger: logger.Mock{}}
	_, err := class.getTrackerInfo()
	AssertThat(t, err, Not{V: nil})
}
