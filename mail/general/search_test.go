package search

import (
	"a.yandex-team.ru/mail/iex/taksa/errs"
	"a.yandex-team.ru/mail/iex/taksa/logger"
	"a.yandex-team.ru/mail/iex/taksa/meta"
	"testing"
	"time"
)

import . "a.yandex-team.ru/mail/iex/matchers"

func TestMessageValid_hasNoMid_false(t *testing.T) {
	AssertThat(t, Message{Subject: "s"}.valid(), Is{V: false})
}

func TestMessageValid_hasEmptyMid_false(t *testing.T) {
	AssertThat(t, Message{Mid: ""}.valid(), Is{V: false})
}

func TestMessageValid_hasGoodMid_true(t *testing.T) {
	AssertThat(t, Message{Mid: "m"}.valid(), Is{V: true})
}

func TestResponseValid_allItemsValid_true(t *testing.T) {
	response := Response{[]Message{Message{Mid: "a"}, Message{Mid: "b"}}, []Message{}}
	AssertThat(t, response.valid(), Is{V: true})
}

func TestResponseValid_hasInvalidItem_false(t *testing.T) {
	response := Response{[]Message{Message{Mid: "a"}, Message{Mid: ""}}, []Message{}}
	AssertThat(t, response.valid(), Is{V: false})
}

func TestResponseValid_hasNoItems_false(t *testing.T) {
	response := Response{[]Message{}, []Message{}}
	AssertThat(t, response.valid(), Is{V: false})
}

func TestGetEnvelopes_invalidJson_givesError(t *testing.T) {
	_, err := GetEnvelopes([]byte("aaa"), logger.Mock{})
	AssertThat(t, err, Is{V: Not{V: nil}})
	_, errorTypeIsBadRequest := err.(errs.BadRequest)
	AssertThat(t, errorTypeIsBadRequest, Is{V: true})
}

func TestGetEnvelopes_noMid_givesError(t *testing.T) {
	_, err := GetEnvelopes([]byte(`{"a":1}`), logger.Mock{})
	AssertThat(t, err, Is{V: Not{V: nil}})
	_, errorTypeIsBadRequest := err.(errs.BadRequest)
	AssertThat(t, errorTypeIsBadRequest, Is{V: true})
}

func TestGetDisplayName_hasDisplayName_givesDisplayName(t *testing.T) {
	AssertThat(t, getDisplayName(Message{From: []Recipient{Recipient{DisplayName: "n"}}}), Is{V: "n"})
}

func TestGetDisplayName_hasNoDisplayName_givesLocalAndDomain(t *testing.T) {
	AssertThat(t, getDisplayName(Message{From: []Recipient{Recipient{Local: "l", Domain: "d"}}}), Is{V: "l@d"})
}

func TestGetDisplayName_hasEmptyFromArray_givesDisplayName(t *testing.T) {
	AssertThat(t, getDisplayName(Message{From: []Recipient{}}), Is{V: ""})
}

func TestTrimParenthesisAndWhitespace(t *testing.T) {
	orig := []byte("\r (\ta ) \n")
	AssertThat(t, trimParenthesisAndWhitespace(orig), Is{V: []byte("a")})
}

func TestGetReceivedDate_stringWithTs_givesTs(t *testing.T) {
	AssertThat(t, getReceivedDate("1493884530"), Is{V: time.Unix(1493884530, 0)})
}

func TestGetReceivedDate_intWithTs_givesTs(t *testing.T) {
	AssertThat(t, getReceivedDate(1493884530), Is{V: time.Unix(1493884530, 0)})
}

func TestGetReceivedDate_badString_givesNow(t *testing.T) {
	AssertThat(t, getReceivedDate("dfgshyrh").Round(time.Minute), Is{V: time.Now().Round(time.Minute)})
}

func TestGetReceivedDate_badType_givesNow(t *testing.T) {
	AssertThat(t, getReceivedDate(false).Round(time.Minute), Is{V: time.Now().Round(time.Minute)})
}

func TestGetEnvelopes_normalInputWithMessageArray_givesEnvelopes(t *testing.T) {
	body := `
	({
    "details": {
        "crc32": "0", "search-limits": {
            "offset": 0, "length": 31
        }
        ,
        "search-options": {
            "request": "", "pure": true, "experiments": "32039,28236,30747"
        }
    },
    "envelopes": [{
		"mid": "1",
		"subject": "a",
		"firstline": "b",
		"from": [{"displayName": "n", "local": "L", "domain": "D"}],
		"to": [{"local": "L", "domain": "D"}],
		"types": [5, 16],
		"date": 1493884530
    }]
	})`
	actual, err := GetEnvelopes([]byte(body), logger.Mock{})
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, actual, EqualTo{V: []meta.Envelope{{Mid: "1", Subject: "a", Firstline: "b", FromDisplayName: "n", Types: []int{5, 16}, FromAddress: "L@D", ReceivedDate: time.Unix(1493884530, 0), ToAddresses: []string{"L@D"}}}})
}

func TestGetEnvelopes_normalInputWithMessageArrayAndTopResults_givesEnvelopes(t *testing.T) {
	body := `
	({
    "details": {
        "crc32": "0", "search-limits": {
            "offset": 0, "length": 31
        }
        ,
        "search-options": {
            "request": "", "pure": true, "experiments": "32039,28236,30747"
        }
    },
    "top-relevant": [{
		"mid": "2",
		"subject": "c",
		"firstline": "d",
		"from": [{"displayName": "n2", "local": "L2", "domain": "D2"}],
		"to": [{"local": "L2", "domain": "D2"}],
		"types": [6, 17],
		"date": 1493884531
    }],
    "envelopes": [{
		"mid": "1",
		"subject": "a",
		"firstline": "b",
		"from": [{"displayName": "n", "local": "L", "domain": "D"}],
		"to": [{"local": "L", "domain": "D"}],
		"types": [5, 16],
		"date": 1493884530
    }]
	})`
	actual, err := GetEnvelopes([]byte(body), logger.Mock{})
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, actual, EqualTo{V: []meta.Envelope{
		{Mid: "1", Subject: "a", Firstline: "b", FromDisplayName: "n", Types: []int{5, 16}, FromAddress: "L@D", ReceivedDate: time.Unix(1493884530, 0), ToAddresses: []string{"L@D"}},
		{Mid: "2", Subject: "c", Firstline: "d", FromDisplayName: "n2", Types: []int{6, 17}, FromAddress: "L2@D2", ReceivedDate: time.Unix(1493884531, 0), ToAddresses: []string{"L2@D2"}}}})
}
