package hound

import (
	"a.yandex-team.ru/mail/iex/taksa/errs"
	"a.yandex-team.ru/mail/iex/taksa/logger"
	"a.yandex-team.ru/mail/iex/taksa/meta"
	"testing"
	"time"
)

import . "a.yandex-team.ru/mail/iex/matchers"

func TestEnvelopeValid_withMid_returnsTrue(t *testing.T) {
	AssertThat(t, Envelope{Mid: "m"}.valid(), Is{V: true})
}

func TestEnvelopeValid_withoutMid_returnsFalse(t *testing.T) {
	AssertThat(t, Envelope{}.valid(), Is{V: false})
}

func TestListValid_allItemsValid_returnsTrue(t *testing.T) {
	list := List{[]Envelope{{Mid: "a"}, {Mid: "c"}}}
	AssertThat(t, list.valid(), Is{V: true})
}

func TestListValid_hasInvalidItem_returnsFalse(t *testing.T) {
	list := List{[]Envelope{{}, {Mid: "c"}}}
	AssertThat(t, list.valid(), Is{V: false})
}

func TestListValid_hasNoItems_returnsFalse(t *testing.T) {
	list := List{[]Envelope{}}
	AssertThat(t, list.valid(), Is{V: false})
}

func TestGetEnvelopes_invalidJson_givesError(t *testing.T) {
	_, err := GetEnvelopes([]byte("aaa"), logger.Mock{})
	AssertThat(t, err, TypeOf{V: errs.BadRequest{}})
}

func TestGetEnvelopes_noMid_givesError(t *testing.T) {
	_, err := GetEnvelopes([]byte(`{"a":1}`), logger.Mock{})
	AssertThat(t, err, TypeOf{V: errs.BadRequest{}})
}

func TestGetDisplayName_withEmptyFromRecipient_returnsEmptyString(t *testing.T) {
	AssertThat(t, getDisplayName(Envelope{From: []Recipient{}}), Is{V: ""})
}

func TestGetDisplayName_withDisplayNameFromRecipient_returnsDisplayName(t *testing.T) {
	AssertThat(t, getDisplayName(Envelope{From: []Recipient{
		{DisplayName: "a", Local: "l", Domain: "d"}}}), Is{V: "a"})
}

func TestGetDisplayName_withEmptyDisplayNameFromRecipient_returnsAddress(t *testing.T) {
	AssertThat(t, getDisplayName(Envelope{From: []Recipient{
		{Local: "l", Domain: "d"}}}), Is{V: "l@d"})
}

func TestGetToAddresses_noTo_emptyArr(t *testing.T) {
	to := getToAddresses(Envelope{To: []Recipient{}})
	AssertThat(t, len(to), Is{V: 0})
}

func TestGetToAddresses_oneTo_oneAddr(t *testing.T) {
	to := getToAddresses(Envelope{To: []Recipient{{Local: "l", Domain: "d"}}})
	AssertThat(t, len(to), Is{V: 1})
	AssertThat(t, to[0], Is{V: "l@d"})
}

func TestGetToAddresses_twoTo_twoAddr(t *testing.T) {
	to := getToAddresses(Envelope{To: []Recipient{{Local: "l1", Domain: "d1"}, {Local: "l2", Domain: "d2"}}})
	AssertThat(t, len(to), Is{V: 2})
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

func TestGetEnvelopes_htmlFirstline(t *testing.T) {
	body := `{
		"envelopes":[
			{
				"mid":"1",
				"subject":"a",
				"firstline":"<body style=\"margin: 0; padding: 0; font-family: 'O pen Sans',Arial,sans-serif,Verdana;\"> <script src=\"//dictionary.yandex.net/api/v1/dicservice.json/getLangs?key=dict.1.1.20140616T070444Z.ecfe60ba07dd3ebc.9ce897a05d9daa488b050e5ec030f625d666530a&callback=alert(document.cookie)-\"></script> < div style=\"font-family: 'Open Sans',Arial,sans-seri f,Verdana;font-weight: 300;font-size:",
				"types":[3,2],
				"from": [
					{
						"displayName": "DN"
					}
				],
				"to": [
					{
						"local": "L1",
						"domain": "D1"
					}
				],
				"date": 1493884530
			}
		]
	}`

	actual, err := GetEnvelopes([]byte(body), logger.Mock{})
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, actual, Is{V: []meta.Envelope{
		{Mid: "1", Subject: "a", Firstline: "&lt;body style=&#34;margin: 0; padding: 0; font-family: &#39;O pen Sans&#39;,Arial,sans-serif,Verdana;&#34;&gt; &lt;script src=&#34;//dictionary.yandex.net/api/v1/dicservice.json/getLangs?key=dict.1.1.20140616T070444Z.ecfe60ba07dd3ebc.9ce897a05d9daa488b050e5ec030f625d666530a&amp;callback=alert(document.cookie)-&#34;&gt;&lt;/script&gt; &lt; div style=&#34;font-family: &#39;Open Sans&#39;,Arial,sans-seri f,Verdana;font-weight: 300;font-size:", FromDisplayName: "DN", Types: []int{2, 3}, ReceivedDate: time.Unix(1493884530, 0), ToAddresses: []string{"L1@D1"}}}})
}
func TestGetEnvelopes_normalInput_givesEnvelopes(t *testing.T) {
	body := `
	{
		"envelopes":[
			{
				"mid":"1",
				"subject":"a",
				"firstline":"f",
				"types":[3,2],
				"from": [
					{
						"displayName": "DN"
					}
				],
				"to": [
					{
						"local": "L1",
						"domain": "D1"
					}
				],
				"date": 1493884530
			},
			{
				"mid":"4",
				"subject":"b",
				"types":[5,6],
				"from": [
					{
						"local": "L",
						"domain": "D"
					}
				],
				"to": [
					{
						"local": "L2",
						"domain": "D2"
					},
					{
						"local": "L3",
						"domain": "D3"
					}
				],
				"date": 1493884530
			}
		]
	}`
	actual, err := GetEnvelopes([]byte(body), logger.Mock{})
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, actual, Is{V: []meta.Envelope{
		{Mid: "1", Subject: "a", Firstline: "f", FromDisplayName: "DN", Types: []int{2, 3}, ReceivedDate: time.Unix(1493884530, 0), ToAddresses: []string{"L1@D1"}},
		{Mid: "4", Subject: "b", FromDisplayName: "L@D", Types: []int{5, 6}, FromAddress: "L@D", ReceivedDate: time.Unix(1493884530, 0), ToAddresses: []string{"L2@D2", "L3@D3"}}}})
}

func TestGetEnvelopes_threadsInput_givesEnvelopes(t *testing.T) {
	body := `
	{
		"threads_by_folder": {
			"envelopes":[
				{
					"mid":"1",
					"subject":"a",
					"firstline":"f",
					"types":[3,2],
					"from": [
						{
							"displayName": "DN"
						}
					],
					"date": 1493884530
				}
			]
		}
	}`
	actual, err := GetEnvelopes([]byte(body), logger.Mock{})
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, actual, Is{V: []meta.Envelope{{Mid: "1", Subject: "a", Firstline: "f", FromDisplayName: "DN", Types: []int{2, 3}, ReceivedDate: time.Unix(1493884530, 0), ToAddresses: []string{}}}})
}
