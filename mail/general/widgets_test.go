package widgets

import (
	"a.yandex-team.ru/mail/iex/taksa/client"
	"a.yandex-team.ru/mail/iex/taksa/iex"
	"a.yandex-team.ru/mail/iex/taksa/logger"
	"a.yandex-team.ru/mail/iex/taksa/meta"
	"a.yandex-team.ru/mail/iex/taksa/request"
	"a.yandex-team.ru/mail/iex/taksa/types"
	"a.yandex-team.ru/mail/iex/taksa/widgets/avia"
	"testing"
)

import . "a.yandex-team.ru/mail/iex/matchers"

func TestEnrich_withNoFacts_returnsNoWidgetsAndNoError(t *testing.T) {
	res, err := Widgets{}.Enrich([]iex.Fact{}, logger.Mock{}, client.Mock{}, request.Mock{})

	AssertThat(t, res, ElementsAre{})
	AssertThat(t, err, Is{V: nil})
}

func TestEnrich_withNonMatchingTypes_returnsNoWidgetsAndNoError(t *testing.T) {
	typs, _ := types.Parse("1")
	cfg := Config{
		Avia: avia.Config{Types: typs}}
	fact := iex.Fact{Envelope: meta.Envelope{Types: []int{}}}

	res, err := Widgets{cfg}.Enrich([]iex.Fact{fact}, logger.Mock{}, client.Mock{}, request.Mock{})

	AssertThat(t, res, ElementsAre{})
	AssertThat(t, err, Is{V: nil})
}
