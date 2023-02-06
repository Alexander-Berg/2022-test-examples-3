package urlinfo

import (
	"testing"

	"a.yandex-team.ru/mail/iex/taksa/iex"
	"a.yandex-team.ru/mail/iex/taksa/logger"
	"a.yandex-team.ru/mail/iex/taksa/meta"
	"a.yandex-team.ru/mail/iex/taksa/widgets/common"
)

import . "a.yandex-team.ru/mail/iex/matchers"

func TestGetUrlInfo_emptyFactsArray_givesError(t *testing.T) {
	facts := iex.Fact{Envelope: meta.Envelope{Mid: "1", Types: []int{1}}, IEX: []interface{}{}}
	class := Class{Fact: facts, Logger: logger.Mock{}}
	_, err := class.getIexUrlsInfo()
	AssertThat(t, err, Not{V: nil})
}

func TestGetUrlInfo_wrongStructType_givesError(t *testing.T) {
	facts := iex.Fact{Envelope: meta.Envelope{Mid: "1", Types: []int{1}}, IEX: []interface{}{[]int{1}}}
	class := Class{Fact: facts, Logger: logger.Mock{}}
	_, err := class.getIexUrlsInfo()
	AssertThat(t, err, Not{V: nil})
}

func TestGetUrlInfo_badType_givesError(t *testing.T) {
	data := map[string]interface{}{
		"taksa_widget_type_1234543456546": "bad",
		"urls_info":                       []map[string]interface{}{},
	}
	iexFactsArray := []interface{}{data}
	fact := iex.Fact{Envelope: meta.Envelope{Mid: "1", Types: []int{1}}, IEX: iexFactsArray}
	class := Class{Fact: fact, Logger: logger.Mock{}}
	_, err := class.getIexUrlsInfo()
	AssertThat(t, err, Not{V: nil})
}

func TestGetUrlInfo_goodType_givesFields(t *testing.T) {
	data := map[string]interface{}{
		"taksa_widget_type_1234543456546": "urls_info",
		"urls_info":                       []interface{}{},
	}
	iexFactsArray := []interface{}{data}
	fact := iex.Fact{Envelope: meta.Envelope{Mid: "1", Types: []int{1}}, IEX: iexFactsArray}
	class := Class{Fact: fact, Logger: logger.Mock{}}
	info, err := class.getIexUrlsInfo()
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, info, Is{V: URLInfo{data}})
}

func TestGetUrlInfo_goodData_givesFields(t *testing.T) {
	data := map[string]interface{}{
		"url":     "https://www.ozon.com/",
		"title":   "Ozon shop",
		"favicon": "https://www.ozon.com/favicon",
	}
	urlInfo := URLInfo{common.IexDict{
		"urls_info": []interface{}{data}}}
	urls := urlInfo.getUrlsAttributes()
	AssertThat(t, len(urls), Is{V: 1})
	AssertThat(t, urls, Is{V: []common.Attributes{data}})
}

func TestGetUrlInfo_extraFields_givesFields(t *testing.T) {
	data := map[string]interface{}{
		"url":         "https://www.ozon.com/",
		"title":       "Ozon shop",
		"favicon":     "https://www.ozon.com/favicon",
		"trash":       "trash",
		"request_url": "https://www.ozon.com/",
		"url_meta": map[string]interface{}{
			"green_url": "https://www.ozon.com/",
		},
		"snippet": "snippet",
		"trash2":  "trash2",
	}
	output := map[string]interface{}{
		"url":         "https://www.ozon.com/",
		"title":       "Ozon shop",
		"favicon":     "https://www.ozon.com/favicon",
		"request_url": "https://www.ozon.com/",
		"url_meta": map[string]interface{}{
			"green_url": "https://www.ozon.com/",
		},
		"snippet": "snippet",
	}
	urlInfo := URLInfo{common.IexDict{
		"urls_info": []interface{}{data}}}
	urls := urlInfo.getUrlsAttributes()
	AssertThat(t, len(urls), Is{V: 1})
	AssertThat(t, urls, Is{V: []common.Attributes{output}})
}

func TestGetUrlInfo_noUrl_givesEmptyAttributes(t *testing.T) {
	data := map[string]interface{}{
		"title":       "Ozon shop",
		"favicon":     "https://www.ozon.com/favicon",
		"trash":       "trash",
		"request_url": "https://www.ozon.com/",
		"url_meta": map[string]interface{}{
			"green_url": "https://www.ozon.com/",
		},
		"snippet": "snippet",
		"trash2":  "trash2",
	}
	urlInfo := URLInfo{common.IexDict{
		"urls_info": []interface{}{data}}}
	urls := urlInfo.getUrlsAttributes()
	AssertThat(t, len(urls), Is{V: 0})
}

func TestGetUrlInfo_noTitle_givesEmptyAttributes(t *testing.T) {
	data := map[string]interface{}{
		"url":         "https://www.ozon.com/",
		"favicon":     "https://www.ozon.com/favicon",
		"request_url": "https://www.ozon.com/",
		"url_meta": map[string]interface{}{
			"green_url": "https://www.ozon.com/",
		},
		"snippet": "snippet",
	}
	urlInfo := URLInfo{common.IexDict{
		"urls_info": []interface{}{data}}}
	urls := urlInfo.getUrlsAttributes()
	AssertThat(t, len(urls), Is{V: 0})
}

func TestGetUrlInfo_noFavicon_givesEmptyAttributes(t *testing.T) {
	data := map[string]interface{}{
		"url":         "https://www.ozon.com/",
		"title":       "Ozon shop",
		"trash":       "trash",
		"request_url": "https://www.ozon.com/",
		"url_meta": map[string]interface{}{
			"green_url": "https://www.ozon.com/",
		},
		"snippet": "snippet",
		"trash2":  "trash2",
	}
	urlInfo := URLInfo{common.IexDict{
		"urls_info": []interface{}{data}}}
	urls := urlInfo.getUrlsAttributes()
	AssertThat(t, len(urls), Is{V: 0})
}

func TestGetUrlInfo_badUrlType_givesEmptyAttributes(t *testing.T) {
	data := map[string]interface{}{
		"url":         123,
		"title":       "Ozon shop",
		"favicon":     "https://www.ozon.com/favicon",
		"request_url": "https://www.ozon.com/",
		"url_meta": map[string]interface{}{
			"green_url": "https://www.ozon.com/",
		},
		"snippet": "snippet",
	}
	urlInfo := URLInfo{common.IexDict{
		"urls_info": []interface{}{data}}}
	urls := urlInfo.getUrlsAttributes()
	AssertThat(t, len(urls), Is{V: 0})
}

func TestGetUrlInfo_fewUrls_givesFields(t *testing.T) {
	data1 := map[string]interface{}{
		"url":         "https://www.ozon.com/",
		"title":       "Ozon shop",
		"favicon":     "https://www.ozon.com/favicon",
		"request_url": "https://www.ozon.com/",
		"url_meta": map[string]interface{}{
			"green_url": "https://www.ozon.com/",
		},
		"snippet": "snippet",
	}
	data2 := map[string]interface{}{
		"url":         "https://yandex.ru/",
		"title":       "YANDEX",
		"favicon":     "",
		"request_url": "https://yandex.ru/",
		"url_meta": map[string]interface{}{
			"green_url": "https://yandex.ru/green",
		},
		"snippet": "Yandex.Snippet",
	}
	data3 := map[string]interface{}{
		"url":     "https://blabla.ru/",
		"title":   "123",
		"favicon": "favicon",
	}
	data4 := map[string]interface{}{
		"url":         "https://google.com/",
		"favicon":     "favicon",
		"request_url": "https://google.com/",
	}
	urlInfo := URLInfo{common.IexDict{
		"urls_info": []interface{}{data1, data2, data3, data4}}}
	urls := urlInfo.getUrlsAttributes()
	AssertThat(t, len(urls), Is{V: 3})
	AssertThat(t, urls, Is{V: []common.Attributes{data1, data2, data3}})
}
