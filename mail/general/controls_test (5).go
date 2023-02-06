package eshop

import (
	"a.yandex-team.ru/mail/iex/taksa/iex"
	"a.yandex-team.ru/mail/iex/taksa/meta"
	"a.yandex-team.ru/mail/iex/taksa/tanker"
	"testing"
)

import . "a.yandex-team.ru/mail/iex/matchers"
import . "a.yandex-team.ru/mail/iex/taksa/widgets/common"

func TestGetTitle_subj(t *testing.T) {
	fact := iex.Fact{Envelope: meta.Envelope{Subject: "subj"}}
	class := Class{Fact: fact, Tanker: tanker.Mock{}}
	order := Order{IexDict{}}
	c, e := class.getTitle(order)
	AssertThat(t, e, Is{V: nil})
	AssertThat(t, c.Attributes["label"], Is{V: "subj"})
}

func TestGetTitle_noItem_firstline(t *testing.T) {
	fact := iex.Fact{Envelope: meta.Envelope{Firstline: "fl"}}
	class := Class{Fact: fact, Tanker: tanker.Mock{}}
	order := Order{IexDict{"date_delivery": "04.03.2016 08:40:00"}}
	c, e := class.getDescription(order)
	AssertThat(t, e, Is{V: nil})
	AssertThat(t, c.Attributes["label"], Is{V: "fl"})
}

func TestGetTitle_noDate_item(t *testing.T) {
	fact := iex.Fact{Envelope: meta.Envelope{Firstline: "fl"}}
	class := Class{Fact: fact, Tanker: tanker.Mock{}}
	order := Order{IexDict{"order": "item"}}
	c, e := class.getDescription(order)
	AssertThat(t, e, Is{V: nil})
	AssertThat(t, c.Attributes["label"], Is{V: "item"})
}

func TestGetTitle_haveBothItemAndDate_itemAndDate(t *testing.T) {
	class := Class{Tanker: tanker.Mock{}}
	order := Order{IexDict{"date_delivery": "04.03.2016 08:40:00", "order": "item"}}
	c, e := class.getDescription(order)
	AssertThat(t, e, Is{V: nil})
	AssertThat(t, c.Attributes["label"], Is{V: "item &truck; Mar 4"})
}
