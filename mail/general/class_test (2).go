package bigimage

import (
	"a.yandex-team.ru/mail/iex/taksa/iex"
	"a.yandex-team.ru/mail/iex/taksa/logger"
	"a.yandex-team.ru/mail/iex/taksa/meta"
	"testing"
)

import . "a.yandex-team.ru/mail/iex/taksa/widgets/common"
import . "a.yandex-team.ru/mail/iex/matchers"

func extend(factMap map[string]interface{}) (Widget, error) {
	iexFactsArray := []interface{}{factMap}
	fact := iex.Fact{Envelope: meta.Envelope{Mid: "1", Types: []int{1}}, IEX: iexFactsArray}
	return Class{
		Cfg:    Config{},
		Fact:   fact,
		Logger: logger.Mock{}}.Extend()
}

func widget(w Widget, _ error) Widget { return w }

func TestExtend_badType_givesEmptyWidget(t *testing.T) {
	factMap := map[string]interface{}{
		"taksa_widget_type_1234543456546": "eshop",
		"value":                           "service.link/value",
	}
	widget, err := extend(factMap)
	AssertThat(t, widget, Is{V: nil})
	AssertThat(t, err, Not{V: nil})
}

func TestExtend_emptyButton_givesEmptyWidget(t *testing.T) {
	factMap := map[string]interface{}{
		"taksa_widget_type_1234543456546": "bigimage",
		"value":                           "service.link/value",
	}
	AssertThat(t, widget(extend(factMap)), Is{V: nil})
}

func TestExtend_emptyValue_givesEmptyWidget(t *testing.T) {
	factMap := map[string]interface{}{
		"taksa_widget_type_1234543456546": "bigimage",
		"button":                          "button.link/value",
		"image":                           "image.link/value",
		"double":                          true,
	}
	AssertThat(t, widget(extend(factMap)), Is{V: nil})
}

func TestExtend_emptyId_givesEmptyWidget(t *testing.T) {
	factMap := map[string]interface{}{
		"taksa_widget_type_1234543456546": "bigimage",
		"value":                           "value.link/value",
		"button":                          "button.link/value",
		"image":                           "image.link/value",
		"double":                          true,
	}
	AssertThat(t, widget(extend(factMap)), Is{V: nil})
}

func TestExtend_emptyVersion_givesEmptyWidget(t *testing.T) {
	factMap := map[string]interface{}{
		"taksa_widget_type_1234543456546": "bigimage",
		"value":                           "value.link/value",
		"id":                              "23784728",
		"button":                          "button.link/value",
		"image":                           "image.link/value",
		"double":                          true,
	}
	AssertThat(t, widget(extend(factMap)), Is{V: nil})
}

func TestExtend_noImageAndDouble_givesValidWidget(t *testing.T) {
	factMap := map[string]interface{}{
		"taksa_widget_type_1234543456546": "bigimage",
		"value":                           "value.link/value",
		"button":                          "button.link/value",
		"id":                              "23784728",
		"version":                         "1.2",
	}
	widget, err := extend(factMap)
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, widget, TypeOf{V: &BigImageWidgetSingle{}})
	AssertThat(t, widget.Type(), Is{V: "bigimage"})
	AssertThat(t, widget.SubType(), Is{V: "bigimage"})
	AssertThat(t, widget.Valid(), Is{V: true})
	AssertThat(t, widget.Double(), Is{V: false})
	AssertThat(t, widget.Mid(), Is{V: "1"})
	AssertThat(t, widget.ID(), Is{V: "23784728"})
	AssertThat(t, widget.Version(), Is{V: "1.2"})
	AssertThat(t, len(widget.Controls()), Is{V: 1})
	AssertThat(t, widget.Controls(), HasBigImage{Value: "value.link/value", Button: "button.link/value", Image: ""})
}

func TestExtend_fullFact_givesValidWidget(t *testing.T) {
	factMap := map[string]interface{}{
		"taksa_widget_type_1234543456546": "bigimage",
		"value":                           "value.link/value",
		"button":                          "button.link/value",
		"image":                           "image.link/value",
		"double":                          true,
		"id":                              "23784728",
		"version":                         "1.2",
	}
	widget, err := extend(factMap)
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, widget, TypeOf{V: &BigImageWidgetDouble{}})
	AssertThat(t, widget.Type(), Is{V: "bigimage"})
	AssertThat(t, widget.SubType(), Is{V: "bigimage"})
	AssertThat(t, widget.Valid(), Is{V: true})
	AssertThat(t, widget.Double(), Is{V: true})
	AssertThat(t, widget.Mid(), Is{V: "1"})
	AssertThat(t, widget.ID(), Is{V: "23784728"})
	AssertThat(t, widget.Version(), Is{V: "1.2"})
	AssertThat(t, len(widget.Controls()), Is{V: 1})
	AssertThat(t, widget.Controls(), HasBigImage{Value: "value.link/value", Button: "button.link/value", Image: "image.link/value"})
}
