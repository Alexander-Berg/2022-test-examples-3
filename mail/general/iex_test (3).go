package bounce

import (
	"a.yandex-team.ru/mail/iex/taksa/iex"
	"testing"
)

import . "a.yandex-team.ru/mail/iex/matchers"

func TestGetBounce_emptyFactsArray_givesSomeBounceNotError(t *testing.T) {
	facts := iex.Fact{IEX: []interface{}{}}
	class := Class{Fact: facts}
	b, err := class.getBounce()
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, b.subtype(), TypeOf{V: TrivialSubType{}})
}
