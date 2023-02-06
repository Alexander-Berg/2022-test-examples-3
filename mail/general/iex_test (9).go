package pkpass

import (
	"a.yandex-team.ru/mail/iex/taksa/iex"
	"a.yandex-team.ru/mail/iex/taksa/logger"
	"a.yandex-team.ru/mail/iex/taksa/meta"
	"testing"
)

import . "a.yandex-team.ru/mail/iex/matchers"

func TestGetPass_emptyFactsArray_givesError(t *testing.T) {
	facts := iex.Fact{Envelope: meta.Envelope{Mid: "1", Types: []int{1}}, IEX: []interface{}{}}
	class := Class{Fact: facts, Logger: logger.Mock{}}
	_, err := class.getPass()
	AssertThat(t, err, Not{V: nil})
}

func TestGetPass_wrongStructType_givesError(t *testing.T) {
	some := []int{42}
	facts := iex.Fact{Envelope: meta.Envelope{Mid: "1", Types: []int{1}}, IEX: []interface{}{some}}
	class := Class{Fact: facts, Logger: logger.Mock{}}
	_, err := class.getPass()
	AssertThat(t, err, Not{V: nil})
}

func TestGetPass_wrongFactTypeField_givesError(t *testing.T) {
	iexFactsArray := []interface{}{map[string]interface{}{"number": 42}}
	facts := iex.Fact{Envelope: meta.Envelope{Mid: "1", Types: []int{1}}, IEX: iexFactsArray}

	class := Class{Fact: facts, Logger: logger.Mock{}}
	_, err := class.getPass()
	AssertThat(t, err, Not{V: nil})
}
