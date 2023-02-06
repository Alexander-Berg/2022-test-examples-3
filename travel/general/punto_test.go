package main

import (
	"testing"
)

var transliterateCases = []struct {
	request  string
	langFrom string
	langTo   string
	expected string
}{
	{
		request:  "test",
		langFrom: "en",
		langTo:   "ru",
		expected: "тест",
	}, {
		request:  "тест",
		langFrom: "ru",
		langTo:   "en",
		expected: "test",
	}, {
		request:  "abcdefghijklmnopqrstuvwxyz",
		langFrom: "en",
		langTo:   "ru",
		expected: "абцдефгхийклмнопярстуввхыз",
	}, {
		request:  "абвгдеёжзийклмнопрстуфхцчшщъыьэюя",
		langFrom: "ru",
		langTo:   "en",
		expected: "abvgdeyozhzijklmnoprstufxczchshshhqdqiqtyeyuya",
	},
}

func TestTransliterate(t *testing.T) {
	for _, tc := range transliterateCases {
		res, ok := Transliterate(tc.request, tc.langFrom, tc.langTo)
		if !ok {
			t.Errorf("Expected successful transliteration.")
		}
		if res != tc.expected {
			t.Errorf("Expected %#v\nGot %#v.", tc.expected, res)
		}
	}
}
