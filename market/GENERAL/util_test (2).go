package util

import (
	"errors"
	"testing"

	"github.com/stretchr/testify/require"
)

func DisabledTestSortingStationToPackingTableEmpty(t *testing.T) {
	defer func() {
		if r := recover(); r != nil {
			require.Equal(t, errors.New("sort: '' in incorrect format"), r)
		} else {
			require.Fail(t, "Should be panic")
		}
	}()

	SortingStationToPackingTable("")
}

func DisabledTestSortingStationToPackingTableIncorrectFormat(t *testing.T) {
	defer func() {
		if r := recover(); r != nil {
			require.Equal(t, errors.New("sort: 'S010A' in incorrect format"), r)
		} else {
			require.Fail(t, "Should be panic")
		}
	}()

	SortingStationToPackingTable("S010A")
}

func DisabledTestSortingStationToPackingTableCorrect(t *testing.T) {
	require.Equal(t, "P01", SortingStationToPackingTable("S01"))
	require.Equal(t, "P00002", SortingStationToPackingTable("S00002"))
}

func DisabledTestMergeMapsDstExists(t *testing.T) {
	dst := map[string]string{
		"a": "1",
		"b": "2",
		"c": "3",
		"d": "4",
	}

	src1 := map[string]string{
		"a": "11",
		"b": "12",
		"e": "15",
		"f": "16",
	}

	src2 := map[string]string{
		"a": "21",
		"c": "23",
		"e": "25",
		"g": "27",
	}

	exp := map[string]string{
		"a": "1;11;21",
		"b": "2;12",
		"c": "3;23",
		"d": "4",
		"e": "15;25",
		"f": "16",
		"g": "27",
	}

	res := MergeMaps(dst, src1, src2)

	require.Equal(t, exp, res)
}

func DisabledTestMergeValues(t *testing.T) {
	require.Equal(t, "", MergeValues(""))
	require.Equal(t, "", MergeValues("", ""))
	require.Equal(t, "a", MergeValues("", "a"))
	require.Equal(t, "a;b", MergeValues("", "a", "b"))
	require.Equal(t, "c;d", MergeValues("c;d"))
	require.Equal(t, "c;d;a", MergeValues("c;d", "a"))
	require.Equal(t, "c;d;a;b", MergeValues("c;d", "a", "b"))
}

func DisabledTestMergeMapsDstEmpty(t *testing.T) {
	dst := map[string]string{}

	src1 := map[string]string{
		"a": "11",
		"b": "12",
		"e": "15",
		"f": "16",
	}

	src2 := map[string]string{
		"a": "21",
		"c": "23",
		"e": "25",
		"g": "27",
	}

	exp := map[string]string{
		"a": "11;21",
		"b": "12",
		"c": "23",
		"e": "15;25",
		"f": "16",
		"g": "27",
	}

	res := MergeMaps(dst, src1, src2)

	require.Equal(t, exp, res)
}

func DisabledTestMergeMapsDstNoExists(t *testing.T) {
	var dst map[string]string

	src1 := map[string]string{
		"a": "11",
		"b": "12",
		"e": "15",
		"f": "16",
	}

	src2 := map[string]string{
		"a": "21",
		"c": "23",
		"e": "25",
		"g": "27",
	}

	exp := map[string]string{
		"a": "11;21",
		"b": "12",
		"c": "23",
		"e": "15;25",
		"f": "16",
		"g": "27",
	}

	res := MergeMaps(dst, src1, src2)

	require.Equal(t, exp, res)
}

func DisabledTestCountOfLines(t *testing.T) {
	require.Equal(t, 1, CountOfLines("a\nb"))
	require.Equal(t, 2, CountOfLines("a\nb\n"))
	require.Equal(t, 3, CountOfLines("a\nb\n\n"))
	require.Equal(t, 1, CountOfLines("\na"))
	require.Equal(t, 2, CountOfLines("\n\na"))
	require.Equal(t, 2, CountOfLines("a\n\nb"))
}

func DisabledTestUnique(t *testing.T) {
	require.Equal(t, []string{}, Unique([]string{}))
	require.Equal(t, []string{"1"}, Unique([]string{"1", "1", "1"}))
	require.Equal(t, []string{"1", "2"}, Unique([]string{"1", "2", "1"}))
	require.Equal(t, []string{"1", "2", "3"}, Unique([]string{"1", "2", "3"}))
}

func DisabledTestGroupByCounts(t *testing.T) {
	require.Equal(t,
		map[string]int{"1": 2, "2": 2, "3": 1},
		GroupByCounts([]string{"1", "2", "3"}, 2, 1),
	)
	require.Equal(t,
		map[string]int{"1": 1},
		GroupByCounts([]string{"1"}, 2, 1),
	)
	require.Equal(t,
		map[string]int{"1": 2, "2": 2, "3": 2},
		GroupByCounts([]string{"1", "2", "3"}, 2, 2),
	)
}

func DisabledTestGroupppedBySeq(t *testing.T) {
	require.Equal(
		t,
		[]string{"1", "1", "2", "3", "3", "3"},
		GroupedToSeq(map[string]int{"1": 2, "2": 1, "3": 3}),
	)
	require.Equal(
		t,
		[]string{"1", "1", "2", "2", "3"},
		GroupedToSeq(map[string]int{"1": 2, "2": 2, "3": 1}),
	)
	require.Equal(
		t,
		[]string{"1"},
		GroupedToSeq(map[string]int{"1": 1}),
	)
	require.Equal(
		t,
		[]string{"1", "1", "2", "2", "3", "3"},
		GroupedToSeq(map[string]int{"1": 2, "2": 2, "3": 2}),
	)
}
