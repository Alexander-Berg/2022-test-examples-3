package bigimage

import (
	"testing"

	"a.yandex-team.ru/mail/iex/taksa/iex"
	"a.yandex-team.ru/mail/iex/taksa/logger"
	"a.yandex-team.ru/mail/iex/taksa/meta"
)

import . "a.yandex-team.ru/mail/iex/matchers"

func TestGetIexBigImage_emptyFactsArray_givesError(t *testing.T) {
	facts := iex.Fact{Envelope: meta.Envelope{Mid: "1", Types: []int{1}}, IEX: []interface{}{}}
	class := Class{Fact: facts, Logger: logger.Mock{}}
	_, err := class.getIexBigImage()
	AssertThat(t, err, Not{V: nil})
}

func TestGetIexBigImage_wrongStructType_givesError(t *testing.T) {
	facts := iex.Fact{Envelope: meta.Envelope{Mid: "1", Types: []int{1}}, IEX: []interface{}{[]int{1}}}
	class := Class{Fact: facts, Logger: logger.Mock{}}
	_, err := class.getIexBigImage()
	AssertThat(t, err, Not{V: nil})
}

func TestGetIexBigImage_badType_givesError(t *testing.T) {
	data := map[string]interface{}{
		"taksa_widget_type_1234543456546": "bad",
		"value":                           "123",
		"button":                          "123",
	}
	iexFactsArray := []interface{}{data}
	fact := iex.Fact{Envelope: meta.Envelope{Mid: "1", Types: []int{1}}, IEX: iexFactsArray}
	class := Class{Fact: fact, Logger: logger.Mock{}}
	_, err := class.getIexBigImage()
	AssertThat(t, err, Not{V: nil})
}

func TestGetIexBigImage_goodType_givesFields(t *testing.T) {
	data := map[string]interface{}{
		"taksa_widget_type_1234543456546": "bigimage",
		"value":                           "123",
		"button":                          "123",
	}
	iexFactsArray := []interface{}{data}
	fact := iex.Fact{Envelope: meta.Envelope{Mid: "1", Types: []int{1}}, IEX: iexFactsArray}
	class := Class{Fact: fact, Logger: logger.Mock{}}
	info, err := class.getIexBigImage()
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, info, Is{V: BigImage{data}})
}

func TestGetIexBigImage_goodData_givesFields(t *testing.T) {
	data := map[string]interface{}{
		"value":   "service.link/value",
		"button":  "button.link/value",
		"id":      "588355978",
		"version": "1.2",
	}
	bigImage := BigImage{data}
	value, err := bigImage.getValue()
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, value, Is{V: "service.link/value"})
	button, err := bigImage.getButton()
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, button, Is{V: "button.link/value"})
	image, err := bigImage.getImage()
	AssertThat(t, err, Not{V: nil})
	AssertThat(t, image, Is{V: ""})
	double := bigImage.getDouble()
	AssertThat(t, double, Is{V: false})
	id, err := bigImage.getID()
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, id, Is{V: "588355978"})
	version, err := bigImage.getVersion()
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, version, Is{V: "1.2"})
}

func TestGetIexBigImage_fullData_givesFields(t *testing.T) {
	data := map[string]interface{}{
		"value":   "service.link/value",
		"button":  "button.link/value",
		"image":   "image.link/value",
		"double":  true,
		"trash":   123,
		"id":      "588355978",
		"version": "1.2",
	}
	bigImage := BigImage{data}
	value, err := bigImage.getValue()
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, value, Is{V: "service.link/value"})
	button, err := bigImage.getButton()
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, button, Is{V: "button.link/value"})
	image, err := bigImage.getImage()
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, image, Is{V: "image.link/value"})
	double := bigImage.getDouble()
	AssertThat(t, double, Is{V: true})
	id, err := bigImage.getID()
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, id, Is{V: "588355978"})
	version, err := bigImage.getVersion()
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, version, Is{V: "1.2"})
}

func TestGetIexBigImage_noRequiredFields_givesError(t *testing.T) {
	data := map[string]interface{}{
		"image":  "image.link/value",
		"double": true,
		"trash":  123,
	}
	bigImage := BigImage{data}
	_, err := bigImage.getValue()
	AssertThat(t, err, Not{V: nil})
	_, err2 := bigImage.getButton()
	AssertThat(t, err2, Not{V: nil})
	_, err3 := bigImage.getID()
	AssertThat(t, err3, Not{V: nil})
	_, err4 := bigImage.getVersion()
	AssertThat(t, err4, Not{V: nil})
}
