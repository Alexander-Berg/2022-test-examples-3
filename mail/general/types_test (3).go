package types

import (
	"reflect"
	"sort"
	"testing"
)

import . "a.yandex-team.ru/mail/iex/matchers"

func (types Types) Match(i interface{}) bool {
	t, _ := i.(Types)
	if len(types) != len(t) {
		return false
	}
	for i := range types {
		if reflect.DeepEqual(types[i], t[i]) {
			return false
		}
	}
	return true
}

func TestParse_empty(t *testing.T) {
	types, _ := Parse("")
	AssertThat(t, types, EqualTo{V: Types(nil)})
}

func TestParse_and(t *testing.T) {
	types, _ := Parse("1&2")
	AssertThat(t, types, EqualTo{V: Types{[]int{1, 2}}})
}

func TestParse_or(t *testing.T) {
	types, _ := Parse("1|2")
	AssertThat(t, types, EqualTo{V: Types{[]int{1}, []int{2}}})
}

func TestParse_and2Or(t *testing.T) {
	types, _ := Parse("1|3&4")
	AssertThat(t, types, EqualTo{V: Types{[]int{1}, []int{3, 4}}})
}

func TestParse_and2Or2(t *testing.T) {
	types, _ := Parse("1&2|3&4")
	AssertThat(t, types, EqualTo{V: Types{[]int{1, 2}, []int{3, 4}}})
}

func TestParse_long(t *testing.T) {
	types, _ := Parse("1&2&3|4&5&6&7|8")
	AssertThat(t, types, EqualTo{V: Types{[]int{1, 2, 3}, []int{4, 5, 6, 7}, []int{8}}})
}

func TestParse_spaces(t *testing.T) {
	types, _ := Parse("1 &  2   |\n3\t&\r4 ")
	AssertThat(t, types, EqualTo{V: Types{[]int{1, 2}, []int{3, 4}}})
}

func TestParse_nonDigital(t *testing.T) {
	_, err := Parse("1|a|2b&3")
	AssertThat(t, err, Is{V: Not{V: nil}})
}

func TestString_empty(t *testing.T) {
	AssertThat(t, Types(nil).String(), EqualTo{V: ""})
}

func TestString_and(t *testing.T) {
	AssertThat(t, Types{[]int{1, 2}}.String(), EqualTo{V: "1&2"})
}

func TestString_or(t *testing.T) {
	AssertThat(t, Types{[]int{1}, []int{2}}.String(), EqualTo{V: "1 | 2"})
}

func TestString_long(t *testing.T) {
	AssertThat(t, Types{[]int{1, 2, 3}, []int{4, 5, 6, 7}, []int{8}}.String(), EqualTo{V: "1&2&3 | 4&5&6&7 | 8"})
}

func TestFits_emptyTraits_false(t *testing.T) {
	AssertThat(t, Types(nil).Fits(sort.IntSlice{1}), Is{V: false})
}

func TestFits_emptyMessage_false(t *testing.T) {
	AssertThat(t, Types{[]int{1}}.Fits(sort.IntSlice{}), Is{V: false})
}

func TestFits_simple_true(t *testing.T) {
	AssertThat(t, Types{[]int{1}}.Fits(sort.IntSlice{1}), Is{V: true})
}

func TestFits_combined_false(t *testing.T) {
	AssertThat(t, Types{[]int{1, 2}, []int{3, 4}}.Fits(sort.IntSlice{1, 3}), Is{V: false})
}

func TestFits_combined_true(t *testing.T) {
	AssertThat(t, Types{[]int{1, 2, 3}, []int{3, 4}}.Fits(sort.IntSlice{1, 2, 3}), Is{V: true})
}
