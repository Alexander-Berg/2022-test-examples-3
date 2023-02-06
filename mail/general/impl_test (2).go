package iex

import (
	"a.yandex-team.ru/mail/iex/taksa/client"
	"a.yandex-team.ru/mail/iex/taksa/logger"
	"a.yandex-team.ru/mail/iex/taksa/meta"
	"testing"
	"time"
)

import . "a.yandex-team.ru/mail/iex/matchers"

type emptyError struct {
}

func (e emptyError) Error() string {
	return ""
}

func empty(f []Fact) bool {
	return len(f) == 0
}

func TestFetch_clientError_failOnHttpErrors(t *testing.T) {
	cli := client.Mock{Err: emptyError{}}
	impl := Impl{Log: logger.Mock{}, Cli: cli}
	envelopes := []meta.Envelope{
		{Mid: "a"},
		{Mid: "c"}}
	facts, err := impl.Fetch(User{UID: "uid"}, envelopes, map[string]string{}, time.Second, nil, false, "taks.y.n")
	AssertThat(t, err, Not{V: nil})
	AssertThat(t, empty(facts), Is{V: true})
}

func TestFetch_incorrectResponse_failOnUnmarshalErrors(t *testing.T) {
	cli := client.Mock{Data: "qqq"}
	impl := Impl{Log: logger.Mock{}, Cli: cli}
	envelopes := []meta.Envelope{
		{Mid: "a"},
		{Mid: "c"}}
	facts, err := impl.Fetch(User{UID: "uid"}, envelopes, map[string]string{}, time.Second, nil, false, "qtaksa.yandex.net")
	AssertThat(t, err, Not{V: nil})
	AssertThat(t, empty(facts), Is{V: true})
}

func TestFetch_correctResponse_factsExtracted(t *testing.T) {
	cli := client.Mock{Data: `{"a": [], "c": []}`}
	impl := Impl{Log: logger.Mock{}, Cli: cli}
	envelopes := []meta.Envelope{
		{Mid: "a"},
		{Mid: "c"}}
	facts, err := impl.Fetch(User{UID: "uid"}, envelopes, map[string]string{}, time.Second, nil, false, "blah.y.n")
	AssertThat(t, err, Is{V: nil})
	AssertThat(t, empty(facts), Is{V: false})
}
