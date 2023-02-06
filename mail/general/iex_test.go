package avia

import (
	"a.yandex-team.ru/mail/iex/taksa/client"
	"a.yandex-team.ru/mail/iex/taksa/iex"
	"a.yandex-team.ru/mail/iex/taksa/logger"
	"a.yandex-team.ru/mail/iex/taksa/meta"
	"testing"
)

import . "a.yandex-team.ru/mail/iex/matchers"

func TestGetTicket_emptyFactsArray_givesError(t *testing.T) {
	facts := iex.Fact{Envelope: meta.Envelope{Mid: "1", Types: []int{1}}, IEX: []interface{}{}}
	class := Class{Fact: facts, Logger: logger.Mock{}, Client: client.Mock{}}
	_, err := class.getTicket()
	AssertThat(t, err, Not{V: nil})
}

func TestGetTicket_wrongStructType_givesError(t *testing.T) {
	some := []int{42}
	facts := iex.Fact{Envelope: meta.Envelope{Mid: "1", Types: []int{1}}, IEX: []interface{}{some}}
	class := Class{Fact: facts, Logger: logger.Mock{}, Client: client.Mock{}}
	_, err := class.getTicket()
	AssertThat(t, err, Not{V: nil})
}

func TestGetTicket_wrongFactTypeField_givesError(t *testing.T) {
	iexFactsArray := []interface{}{}
	ticket := map[string]interface{}{}
	ticket["number"] = 42
	iexFactsArray = append(iexFactsArray, ticket)
	facts := iex.Fact{Envelope: meta.Envelope{Mid: "1", Types: []int{1}}, IEX: iexFactsArray}

	class := Class{Fact: facts, Logger: logger.Mock{}, Client: client.Mock{}}
	_, err := class.getTicket()
	AssertThat(t, err, Not{V: nil})
}
