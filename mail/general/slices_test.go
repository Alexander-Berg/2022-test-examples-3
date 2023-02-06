package tutil

import (
	"testing"
)

import . "a.yandex-team.ru/mail/iex/matchers"

func TestContains_WithEmptySliceAndAnyNumber_ReturnsFalse(t *testing.T) {
	AssertThat(t, ContainsInt([]int{}, 1), Is{V: false})
}

func TestContains_WithSortedSliceContainsTheNumberAndTheNumber_ReturnsTrue(t *testing.T) {
	AssertThat(t, ContainsInt([]int{1, 2, 4}, 2), Is{V: true})
}

func TestContains_WithSortedSliceDoesNotContainTheNumberAndTheNumber_ReturnsFalse(t *testing.T) {
	AssertThat(t, ContainsInt([]int{1, 2, 4}, 3), Is{V: false})
}

func TestIntersects_WithEmptySlices_ReturnsFalse(t *testing.T) {
	AssertThat(t, Intersects([]int{}, []int{}), Is{V: false})
}

func TestIntersects_WithIntersectedSortedSlices_ReturnsTrue(t *testing.T) {
	AssertThat(t, Intersects([]int{1, 2, 3, 4}, []int{2, 4, 6, 8}), Is{V: true})
}

func TestIntersects_WithNotIntersectedSortedSlices_ReturnsFalse(t *testing.T) {
	AssertThat(t, Intersects([]int{1, 3, 5, 7}, []int{2, 4, 6, 8}), Is{V: false})
}

func TestIntersection_WithEmptySlices_ReturnsEmptySlice(t *testing.T) {
	AssertThat(t, Intersection([]int{}, []int{}), ElementsAre{})
}

func TestIntersection_WithIntersectedSortedSlices_ReturnsIntersection(t *testing.T) {
	AssertThat(t, Intersection([]int{1, 2, 3, 4}, []int{2, 4, 6, 8}), ElementsAre{2, 4})
}

func TestIntersection_WithNotIntersectedSortedSlices_ReturnsEmptySlice(t *testing.T) {
	AssertThat(t, Intersection([]int{1, 3, 5, 7}, []int{2, 4, 6, 8}), ElementsAre{})
}

func TestIncludes_emptySetEmptySubset_true(t *testing.T) {
	AssertThat(t, Includes([]int{}, []int{}), Is{V: true})
}

func TestIncludes_emptySetNonemptySubset_false(t *testing.T) {
	AssertThat(t, Includes([]int{}, []int{1}), Is{V: false})
}

func TestIncludes_nonemptySetEmptySubset_true(t *testing.T) {
	AssertThat(t, Includes([]int{1}, []int{}), Is{V: true})
}

func TestIncludes_setAndSubsetEquals_true(t *testing.T) {
	AssertThat(t, Includes([]int{1, 2, 3}, []int{1, 2, 3}), Is{V: true})
}

func TestIncludes_setIncludesSubset_true(t *testing.T) {
	AssertThat(t, Includes([]int{1, 2, 3}, []int{1, 3}), Is{V: true})
}

func TestIncludes_setDoesnotIncludesSubset_false(t *testing.T) {
	AssertThat(t, Includes([]int{2, 3}, []int{1, 3}), Is{V: false})
}

func TestPrintIntSlice_WithEmptySlice_ReturnsEmptySlice(t *testing.T) {
	AssertThat(t, PrintIntSlice([]int{}), Is{V: ""})
}

func TestPrintIntSlice_WithIntSlice_ReturnsStringSliceWithValues(t *testing.T) {
	AssertThat(t, PrintIntSlice([]int{-1, 1, 2}), Is{V: "-1, 1, 2"})
}

func TestInts2Strings_WithEmptySlice_ReturnsEmptySlice(t *testing.T) {
	AssertThat(t, Ints2Strings([]int{}), Is{V: []string{}})
}

func TestInts2Strings_WithIntSlice_ReturnsStringSliceWithValues(t *testing.T) {
	AssertThat(t, Ints2Strings([]int{-1, 1, 2}), Is{V: []string{"-1", "1", "2"}})
}

func TestStrings2Ints_WithEmptySlice_ReturnsEmptySlice(t *testing.T) {
	AssertThat(t, Strings2Ints([]string{}), Is{V: []int{}})
}

func TestStrings2Ints_WithSlice_ReturnsIntsSliceWithValues(t *testing.T) {
	AssertThat(t, Strings2Ints([]string{"-1", "1", "2"}), Is{V: []int{-1, 1, 2}})
}

func TestStrings2Ints_WithNotNumberSliceValue_OmmitsNotNumberValue(t *testing.T) {
	AssertThat(t, Strings2Ints([]string{"zzz", "1"}), Is{V: []int{1}})
}

func TestStrings2IntsStrict_WithNotNumberSliceValue_ReturnsError(t *testing.T) {
	_, e := Strings2IntsStrict([]string{"zzz", "1"})
	AssertThat(t, e, Is{V: Not{V: nil}})
}

func TestContainsString_emptySlice_returnsFalse(t *testing.T) {
	AssertThat(t, ContainsString("", []string{}), Is{V: false})
}

func TestContainsString_sliceWithoutRequestedString_returnsFalse(t *testing.T) {
	AssertThat(t, ContainsString("a", []string{"b", "c"}), Is{V: false})
}

func TestContainsString_sliceWithRequestedString_returnsTrue(t *testing.T) {
	AssertThat(t, ContainsString("a", []string{"z", "a", "b"}), Is{V: true})
}
