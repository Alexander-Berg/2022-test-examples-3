package eshop

import (
	"a.yandex-team.ru/mail/iex/taksa/iex"
	"a.yandex-team.ru/mail/iex/taksa/logger"
	"a.yandex-team.ru/mail/iex/taksa/meta"
	"a.yandex-team.ru/mail/iex/taksa/tanker"
	"testing"
	"time"
)

import . "a.yandex-team.ru/mail/iex/taksa/widgets/common"
import . "a.yandex-team.ru/mail/iex/matchers"

func extend(iexSubType, status string, recv time.Time) (Widget, error) {
	order := map[string]interface{}{
		"taksa_widget_type_1234543456546": "eshop",
		"widget_subtype":                  iexSubType,
		"order_status":                    status,
	}
	iexFactsArray := []interface{}{order}
	fact := iex.Fact{Envelope: meta.Envelope{Mid: "1", Types: []int{1}, ReceivedDate: recv}, IEX: iexFactsArray}

	return Class{
		Cfg:    Config{},
		Fact:   fact,
		Logger: logger.Mock{},
		Tanker: tanker.Mock{}}.Extend()
}

func err(w Widget, e error) error     { return e }
func widget(w Widget, e error) Widget { return w }

func TestExtend_invalidIexSubtype_givesError(t *testing.T) {
	AssertThat(t, err(extend("aaa", "", time.Now())), Not{V: nil})
}

func TestExtend_eshopSubtype_givesMainWidget(t *testing.T) {
	AssertThat(t, widget(extend("eshop", "", time.Now())), TypeOf{V: &MainEshopWidget{}})
}

func TestExtend_eshopSubtypeAndOldRecv_givesOutdatedWidget(t *testing.T) {
	AssertThat(t, widget(extend("eshop", "", time.Now().AddDate(0, 0, -100))), TypeOf{V: &OutdatedEshopWidget{}})
}

func TestExtend_eshopSubtypeAndCancelledStatus_givesOutdatedWidget(t *testing.T) {
	AssertThat(t, widget(extend("eshop", "OrderCancelled", time.Now())), TypeOf{V: &OutdatedEshopWidget{}})
}

func TestExtend_updatedSubtype_givesOutdatedWidget(t *testing.T) {
	AssertThat(t, widget(extend("updated", "", time.Now())), TypeOf{V: &OutdatedEshopWidget{}})
}

func TestIsTooOld_domainInMapAndOldReceived_returnsTrue(t *testing.T) {
	m := map[string]Days{"domain": 60}
	e := meta.Envelope{FromAddress: "login@domain", ReceivedDate: time.Now().AddDate(0, 0, -61)}
	res := isTooOld(m, 30, e)
	AssertThat(t, res, Is{V: true})
}

func TestIsTooOld_domainInMapAndRecentReceived_returnsFalse(t *testing.T) {
	m := map[string]Days{"domain": 60}
	e := meta.Envelope{FromAddress: "login@domain", ReceivedDate: time.Now().AddDate(0, 0, -59)}
	res := isTooOld(m, 30, e)
	AssertThat(t, res, Is{V: false})
}

func TestIsTooOld_domainNotInMapAndOldReceived_returnsTrue(t *testing.T) {
	m := map[string]Days{}
	e := meta.Envelope{FromAddress: "login@domain", ReceivedDate: time.Now().AddDate(0, 0, -31)}
	res := isTooOld(m, 30, e)
	AssertThat(t, res, Is{V: true})
}

func TestIsTooOld_domainNotInMapAndRecentReceived_returnsFalse(t *testing.T) {
	m := map[string]Days{}
	e := meta.Envelope{FromAddress: "login@domain", ReceivedDate: time.Now().AddDate(0, 0, -29)}
	res := isTooOld(m, 30, e)
	AssertThat(t, res, Is{V: false})
}
