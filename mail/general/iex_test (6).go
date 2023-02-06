package eshop

import (
	"a.yandex-team.ru/mail/iex/taksa/iex"
	"a.yandex-team.ru/mail/iex/taksa/logger"
	"a.yandex-team.ru/mail/iex/taksa/meta"
	"testing"
	"time"
)

import . "a.yandex-team.ru/mail/iex/matchers"

func TestGetOrder_emptyFactsArray_givesError(t *testing.T) {
	facts := iex.Fact{Envelope: meta.Envelope{Mid: "1", Types: []int{1}}, IEX: []interface{}{}}
	class := Class{Fact: facts, Logger: logger.Mock{}}
	_, err := class.getOrder()
	AssertThat(t, err, Not{V: Is{V: nil}})
}

func TestGetOrder_wrongStructType_givesError(t *testing.T) {
	some := []int{42}
	facts := iex.Fact{Envelope: meta.Envelope{Mid: "1", Types: []int{1}}, IEX: []interface{}{some}}
	class := Class{Fact: facts, Logger: logger.Mock{}}
	_, err := class.getOrder()
	AssertThat(t, err, Not{V: nil})
}

func TestGetOrder_badType_givesFields(t *testing.T) {
	data := map[string]interface{}{
		"taksa_widget_type_1234543456546": "bad",
	}
	iexFactsArray := []interface{}{data}
	fact := iex.Fact{Envelope: meta.Envelope{Mid: "1", Types: []int{1}}, IEX: iexFactsArray}
	class := Class{Fact: fact, Logger: logger.Mock{}}
	_, err := class.getOrder()
	AssertThat(t, err, Not{V: nil})
}

func str(s string, _ error) string       { return s }
func tim(t time.Time, _ error) time.Time { return t }

func TestGetOrder_goodData_givesFields(t *testing.T) {
	data := map[string]interface{}{
		"taksa_widget_type_1234543456546": "eshop",
		"order_number":                    "number",
		"order_status":                    "status",
		"order":                           "item",
		"date_delivery":                   "04.03.2016 08:40:00",
		"url":                             "link",
	}
	iexFactsArray := []interface{}{data}
	fact := iex.Fact{Envelope: meta.Envelope{Mid: "1", Types: []int{1}}, IEX: iexFactsArray}
	class := Class{Fact: fact, Logger: logger.Mock{}}
	order, err := class.getOrder()
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, str(order.Number()), Is{V: "number"})
	AssertThat(t, str(order.Status()), Is{V: "status"})
	AssertThat(t, str(order.Item()), Is{V: "item"})
	AssertThat(t, tim(order.DeliveryDate()).Unix(), Is{V: int64(1457080800)})
	AssertThat(t, str(order.Link()), Is{V: "link"})
}
