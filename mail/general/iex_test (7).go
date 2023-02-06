package hotels

import (
	"a.yandex-team.ru/mail/iex/taksa/client"
	"a.yandex-team.ru/mail/iex/taksa/iex"
	"a.yandex-team.ru/mail/iex/taksa/logger"
	"a.yandex-team.ru/mail/iex/taksa/meta"
	"testing"
)

import . "a.yandex-team.ru/mail/iex/matchers"
import . "a.yandex-team.ru/mail/iex/taksa/widgets/common"

func TestGetTicket_emptyFactsArray_givesError(t *testing.T) {
	facts := iex.Fact{Envelope: meta.Envelope{Mid: "1", Types: []int{1}}, IEX: []interface{}{}}
	class := Class{Fact: facts, Logger: logger.Mock{}, Client: client.Mock{}}
	_, err := class.getHotel()
	AssertThat(t, err, Not{V: Is{V: nil}})
}

func TestGetTicket_wrongStructType_givesError(t *testing.T) {
	some := []int{42}
	facts := iex.Fact{Envelope: meta.Envelope{Mid: "1", Types: []int{1}}, IEX: []interface{}{some}}
	class := Class{Fact: facts, Logger: logger.Mock{}, Client: client.Mock{}}
	_, err := class.getHotel()
	AssertThat(t, err, Not{V: Is{V: nil}})
}

func TestGetTicket_wrongFactTypeField_givesError(t *testing.T) {
	iexFactsArray := []interface{}{}
	hotel := Hotel{IexDict{"number": 42}}
	iexFactsArray = append(iexFactsArray, hotel)
	facts := iex.Fact{Envelope: meta.Envelope{Mid: "1", Types: []int{1}}, IEX: iexFactsArray}

	class := Class{Fact: facts, Logger: logger.Mock{}, Client: client.Mock{}}
	_, err := class.getHotel()
	AssertThat(t, err, Not{V: Is{V: nil}})
}
