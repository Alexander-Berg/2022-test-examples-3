package urlinfo

import (
	"a.yandex-team.ru/mail/iex/taksa/iex"
	"a.yandex-team.ru/mail/iex/taksa/logger"
	"a.yandex-team.ru/mail/iex/taksa/meta"
	"testing"
)

import . "a.yandex-team.ru/mail/iex/taksa/widgets/common"
import . "a.yandex-team.ru/mail/iex/matchers"

func extend(widgetType string, urlsInfo []interface{}) (Widget, error) {
	factMap := map[string]interface{}{
		"taksa_widget_type_1234543456546": widgetType,
		"urls_info":                       urlsInfo,
	}
	iexFactsArray := []interface{}{factMap}
	fact := iex.Fact{Envelope: meta.Envelope{Mid: "1", Types: []int{1}}, IEX: iexFactsArray}
	return Class{
		Cfg:    Config{},
		Fact:   fact,
		Logger: logger.Mock{}}.Extend()
}

func widget(w Widget, _ error) Widget { return w }

func TestExtend_badType_givesEmptyWidget(t *testing.T) {
	urlsInfo := []interface{}{
		map[string]interface{}{
			"title": "ozon.ru",
		},
	}
	widget, err := extend("eshop", urlsInfo)
	AssertThat(t, widget, Is{V: nil})
	AssertThat(t, err, Not{V: nil})
}

func TestExtend_emptyUrlAttributes_givesEmptyWidget(t *testing.T) {
	AssertThat(t, widget(extend("urls_info", []interface{}{})), Is{V: nil})
}

func TestExtend_brokenUrlAttributes_givesEmptyWidget(t *testing.T) {
	urlsInfo := []interface{}{
		map[string]interface{}{
			"title": "ozon.ru",
			"url":   "ozon.ru",
		},
		map[string]interface{}{
			"favicon": "ozon",
			"title":   []interface{}{},
			"url":     "ozon.ru",
		},
	}
	AssertThat(t, widget(extend("urls_info", urlsInfo)), Is{V: nil})
}

func TestExtend_goodUrlAttributes_givesValidWidget(t *testing.T) {
	attributes := map[string]interface{}{
		"favicon": "ozon-favicon",
		"title":   "Ozon",
		"url":     "ozon.ru",
	}
	urlsInfo := []interface{}{attributes}
	widget, err := extend("urls_info", urlsInfo)
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, widget, TypeOf{V: &URLInfoWidget{}})
	AssertThat(t, widget.Type(), Is{V: "url_info"})
	AssertThat(t, widget.SubType(), Is{V: "url_info"})
	AssertThat(t, widget.Valid(), Is{V: true})
	AssertThat(t, widget.Double(), Is{V: false})
	AssertThat(t, widget.Mid(), Is{V: "1"})
	AssertThat(t, len(widget.Controls()), Is{V: 1})
	AssertThat(t, widget.Controls(), HasRichPreview{Attributes: attributes})
}

func TestExtend_fewUrlFacts_givesValidWidgets(t *testing.T) {
	attributes1 := map[string]interface{}{
		"favicon": "ozon-favicon",
		"title":   "Ozon",
		"url":     "ozon.ru",
	}
	attributes2 := map[string]interface{}{
		"favicon":     "your favicon",
		"title":       "Youtube",
		"url":         "youtube.ru",
		"request_url": "youtube.ru",
		"snippet":     "snippet",
	}
	attributes3 := map[string]interface{}{
		"favicon": "yandex.ru favicon",
		"snippet": "Yandex",
		"url":     "yandex.ru",
	}
	urlsInfo := []interface{}{attributes1, attributes2, attributes3}
	widget, err := extend("urls_info", urlsInfo)
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, widget, TypeOf{V: &URLInfoWidget{}})
	AssertThat(t, widget.Type(), Is{V: "url_info"})
	AssertThat(t, widget.SubType(), Is{V: "url_info"})
	AssertThat(t, widget.Valid(), Is{V: true})
	AssertThat(t, widget.Double(), Is{V: false})
	AssertThat(t, widget.Mid(), Is{V: "1"})
	AssertThat(t, len(widget.Controls()), Is{V: 2})
	AssertThat(t, widget.Controls(), HasRichPreview{Attributes: attributes1})
	AssertThat(t, widget.Controls(), HasRichPreview{Attributes: attributes2})
}
