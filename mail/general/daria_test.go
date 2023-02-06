package daria

import (
	"a.yandex-team.ru/mail/iex/taksa/errs"
	"a.yandex-team.ru/mail/iex/taksa/logger"
	"a.yandex-team.ru/mail/iex/taksa/meta"
	"sort"
	"testing"
	"time"
)

import . "a.yandex-team.ru/mail/iex/matchers"

func TestMessageValid_withMid_returnsTrue(t *testing.T) {
	AssertThat(t, Message{Mid: "m"}.valid(), Is{V: true})
}

func TestMessageValid_withoutMid_returnsFalse(t *testing.T) {
	AssertThat(t, Message{}.valid(), Is{V: false})
}

func TestResponseValid_allItemsValid_returnsTrue(t *testing.T) {
	AssertThat(t,
		Response{Messages{List: List{[]Message{{Mid: "a"}, {Mid: "b"}}}}}.valid(),
		Is{V: true})
}

func TestResponseValid_hasInvalidItem_returnsFalse(t *testing.T) {
	AssertThat(t,
		Response{Messages{List: List{[]Message{{Mid: "a"}, {Mid: ""}}}}}.valid(),
		Is{V: false})
}

func TestResponseValid_hasNoItems_returnsFalse(t *testing.T) {
	AssertThat(t, Response{Messages{List: List{[]Message{}}}}.valid(), Is{V: false})
}

func TestGetEnvelopes_invalidJson_givesError(t *testing.T) {
	_, err := GetEnvelopes([]byte("aaa"), logger.Mock{})
	AssertThat(t, err, TypeOf{V: errs.BadRequest{}})
}

func TestGetEnvelopes_noMid_givesError(t *testing.T) {
	_, err := GetEnvelopes([]byte(`{"a":1}`), logger.Mock{})
	AssertThat(t, err, TypeOf{V: errs.BadRequest{}})
}

func TestGetDisplayName_withMessageWithNameAndEmail_returnsName(t *testing.T) {
	AssertThat(t, getDisplayName(Message{From: Recipient{Name: "n", Email: "e"}}), Is{V: "n"})
}

func TestGetDisplayName_withMessageWithEmail_returnsEmail(t *testing.T) {
	AssertThat(t, getDisplayName(Message{From: Recipient{Email: "e"}}), Is{V: "e"})
}

func TestGetDisplayName_withMessageWithNoNameAndEmail_returnsEmptyString(t *testing.T) {
	AssertThat(t, getDisplayName(Message{From: Recipient{}}), Is{V: ""})
}

func TestGetSortedTypes_withEolSeparatedTypes_returnsSortedTypes(t *testing.T) {
	AssertThat(t, getSortedTypes(Message{Types: "5\u000a16\u000a3\u000a7"}),
		Is{V: sort.IntSlice{3, 5, 7, 16}})
}

func TestGetSortedTypes_withSingleType_returnsSingleType(t *testing.T) {
	AssertThat(t, getSortedTypes(Message{Types: "5"}), Is{V: sort.IntSlice{5}})
}

func TestGetSortedTypes_withNoType_returnsEmptySlice(t *testing.T) {
	AssertThat(t, getSortedTypes(Message{}), Is{V: sort.IntSlice{}})
}

func TestGetSubject_plainString_givesString(t *testing.T) {
	AssertThat(t, getSubject("subj"), Is{V: "subj"})
}

func TestGetSubject_substructureHasField_givesString(t *testing.T) {
	AssertThat(t, getSubject(map[string]string{"$t": "subj"}), Is{V: "subj"})
}

func TestGetSubject_substructureHasntFoeld_givesString(t *testing.T) {
	AssertThat(t, getSubject(map[string]string{}), Is{V: ""})
}

func TestGetSubject_somethingElse_givesEmptyString(t *testing.T) {
	AssertThat(t, getSubject(123), Is{V: ""})
}

func TestGetReceivedDate_stringWithTs_givesTs(t *testing.T) {
	AssertThat(t, getReceivedDate("1493884530"), Is{V: time.Unix(1493884530, 0)})
}

func TestGetReceivedDate_intWithTs_givesTs(t *testing.T) {
	AssertThat(t, getReceivedDate(1493884530), Is{V: time.Unix(1493884530, 0)})
}

func TestGetReceivedDate_badString_givesNow(t *testing.T) {
	AssertThat(t, getReceivedDate("dfgshyrh").Round(time.Second), Is{V: time.Now().Round(time.Second)})
}

func TestGetReceivedDate_badType_givesNow(t *testing.T) {
	AssertThat(t, getReceivedDate(false).Round(time.Second), Is{V: time.Now().Round(time.Second)})
}

func TestGetEnvelopes_normalInputWithMessageArray_givesEnvelopes(t *testing.T) {
	body := `
	{
		"messages": {
			"list": {
				"message": [
					{
						"id": "1",
						"subject": "a",
						"firstline": "b",
						"types": "5\u000a16",
						"from": {
							"name": "n",
							"email": "e"
						},
						"date": {
							"utc_timestamp": "1493884530"
						}
					}
				]
			}
		}
	}`
	actual, err := GetEnvelopes([]byte(body), logger.Mock{})
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, actual, Is{V: []meta.Envelope{{Mid: "1", Subject: "a", Firstline: "b", FromDisplayName: "n", Types: []int{5, 16}, FromAddress: "e", ReceivedDate: time.Unix(1493884530, 0), ToAddresses: []string{}}}})
}

func TestGetEnvelopes_normalInputWithSingleMessageObject_givesEnvelopes(t *testing.T) {
	body := `
	{
		"messages": {
			"list": {
				"message": {
					"id": "1",
					"subject": "a",
					"firstline": "b",
					"types": "5\u000a16",
					"from": {
						"name": "n",
						"email": "e"
					},
					"date": {
						"utc_timestamp": "1493884530"
					}
				}
			}
		}
	}`
	actual, err := GetEnvelopes([]byte(body), logger.Mock{})
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, actual, Is{V: []meta.Envelope{{Mid: "1", Subject: "a", Firstline: "b", FromDisplayName: "n", Types: []int{5, 16}, FromAddress: "e", ReceivedDate: time.Unix(1493884530, 0), ToAddresses: []string{}}}})
}
