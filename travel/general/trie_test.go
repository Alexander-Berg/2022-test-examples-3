package containers

import (
	"testing"

	. "a.yandex-team.ru/travel/rasp/suggests/models"
)

var trieCases = []struct {
	Key   string
	Value TitleDataArray
}{
	{"foobar", TitleDataArray{TitleData{ID: 1, IsPrefix: true}}},
	{"foo", TitleDataArray{TitleData{ID: 2, IsPrefix: false}}},
	{"football", TitleDataArray{TitleData{ID: 3, IsPrefix: true}}},
	{"bar", TitleDataArray{TitleData{ID: 4, IsPrefix: false}}},
	{"", TitleDataArray{TitleData{ID: 5, IsPrefix: true}}},
	{"barbar", TitleDataArray{TitleData{ID: 6, IsPrefix: false}}},
	{"test", TitleDataArray{TitleData{ID: 7, IsPrefix: false}}},
	{"te", TitleDataArray{TitleData{ID: 8, IsPrefix: true}}},
	{"tetete", TitleDataArray{TitleData{ID: 9, IsPrefix: false}}},
	{"fooball", TitleDataArray{TitleData{ID: 10, IsPrefix: true}}},
}

func compareTitleDataArrays(result, expected TitleDataArray) bool {
	for _, resultItem := range result {
		for _, expectedItem := range expected {
			if resultItem != expectedItem {
				return false
			}
		}
	}
	return true
}

func TestTrieCases(t *testing.T) {
	trie := Trie{}
	trie.Init()
	for _, testCase := range trieCases {
		trie.Add(testCase.Key, testCase.Value)
	}
	for _, testCase := range trieCases {
		result := trie.Find(testCase.Key)
		if !compareTitleDataArrays(result, testCase.Value) {
			t.Errorf("Wrong value for %#v key, expected %#v, got %#v.",
				testCase.Key, testCase.Value, result)
		}
	}
}

func BenchmarkTrie(b *testing.B) {
	trie := Trie{}
	trie.Init()
	for i := 0; i < b.N; i++ {
		trie.Add("г. Санкт-Петербург, Санкт-Петербург и Ленинградская область, Россия",
			TitleDataArray{TitleData{ID: 9, IsPrefix: false}})
	}
}
