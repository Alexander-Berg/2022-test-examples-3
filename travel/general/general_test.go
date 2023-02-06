package utils

import (
	"math/rand"
	"reflect"
	"testing"

	"a.yandex-team.ru/travel/rasp/suggests/models"
)

var unpackTitleDataArrayCases = []struct {
	data     []models.TitleDataArray
	expected models.TitleDataArray
}{
	{
		data:     []models.TitleDataArray{{{ID: 1, IsPrefix: true}, {ID: 2, IsPrefix: false}}, {{ID: 3, IsPrefix: true}, {ID: 4, IsPrefix: false}}, {{ID: 5, IsPrefix: true}}},
		expected: models.TitleDataArray{{ID: 1, IsPrefix: true}, {ID: 2, IsPrefix: false}, {ID: 3, IsPrefix: true}, {ID: 4, IsPrefix: false}, {ID: 5, IsPrefix: true}},
	}, {
		data:     []models.TitleDataArray{},
		expected: models.TitleDataArray{},
	}, {
		data:     []models.TitleDataArray{{{ID: 1, IsPrefix: true}}, {{ID: 2, IsPrefix: false}}},
		expected: models.TitleDataArray{{ID: 1, IsPrefix: true}, {ID: 2, IsPrefix: false}},
	}, {
		data:     []models.TitleDataArray{{{ID: 1, IsPrefix: true}}},
		expected: models.TitleDataArray{{ID: 1, IsPrefix: true}},
	},
}

func TestUnpackTitleDataArray(t *testing.T) {
	for _, tc := range unpackTitleDataArrayCases {
		res := UnpackTitleDataArray(tc.data)
		if !reflect.DeepEqual(res, tc.expected) {
			t.Errorf("Expected %#v\nGot %#v.", tc.expected, res)
		}
	}
}

var transliterateCases = []struct {
	request   string
	tableName string
	expected  string
}{
	{
		request:   "test",
		tableName: "lat-cyr",
		expected:  "тест",
	}, {
		request:   "тест",
		tableName: "cyr-lat",
		expected:  "test",
	}, {
		request:   "abcdefghijklmnopqrstuvwxyz",
		tableName: "lat-cyr",
		expected:  "абцдефгхийклмнопярстуввхыз",
	}, {
		request:   "абвгдеёжзийклмнопрстуфхцчшщъыьэюя",
		tableName: "cyr-lat",
		expected:  "abvgdeyozhzijklmnoprstufxczchshshhqdqiqtyeyuya",
	}, {
		request:   "t e s t ! ! !",
		tableName: "lat-cyr",
		expected:  "т е с т ! ! !",
	},
}

func TestTransliterate(t *testing.T) {
	for _, tc := range transliterateCases {
		res := Transliterate(tc.request, tc.tableName)
		if res != tc.expected {
			t.Errorf("Expected %#v\nGot %#v.", tc.expected, res)
		}
	}
}

var getLayoutVariantsCases = []struct {
	request  string
	lang     string
	expected []string
}{
	{
		request:  "test",
		lang:     "ru",
		expected: []string{"еуые"},
	}, {
		request:  "йцукенгшщзххъъфывапролджжээячсмитьббююё",
		lang:     "en",
		expected: []string{"qwertyuiop[[]]asdfghjkl;;\"\"zxcvbnm,,..`"},
	}, {
		request:  "йцукенгшщзххъъфывапролджжээячсмитьббююё",
		lang:     "uk",
		expected: []string{"йцукенгшщзххїїфівапролджжєєячсмитьббююё"},
	}, {
		request:  "test",
		lang:     "en",
		expected: []string{},
	}, {
		request:  "йцукенгшщзххїїфівапролджжєєячсмитьббююё",
		lang:     "ru",
		expected: []string{"йцукенгшщзххъъфывапролджжээячсмитьббююё"},
	},
}

func TestGetLayoutVariants(t *testing.T) {
	for _, tc := range getLayoutVariantsCases {
		res := GetLayoutVariants(tc.request, tc.lang)
		if !reflect.DeepEqual(res, tc.expected) {
			t.Errorf("Expected %#v\nGot %#v.", tc.expected, res)
		}
	}
}

var prepareTitleTextCases = []struct {
	request  string
	expected string
}{
	{
		request:  "TeST",
		expected: "test",
	}, {
		request:  "T,esT",
		expected: "t est",
	}, {
		request:  "T,,est",
		expected: "t est",
	}, {
		request:  " , Te,!.st ,  ",
		expected: "te st",
	},
}

func TestPrepareTitleText(t *testing.T) {
	for _, tc := range prepareTitleTextCases {
		res := PrepareTitleText(tc.request)
		if res != tc.expected {
			t.Errorf("Expected %#v\nGot %#v.", tc.expected, res)
		}
	}
}

var getLangsByNationalVersionCases = []struct {
	nationalVersion string
	defaultLang     string
	expected        []string
}{
	{
		nationalVersion: "ru",
		defaultLang:     "ru",
		expected:        []string{"ru", "en", "uk"},
	}, {
		nationalVersion: "ru",
		defaultLang:     "uk",
		expected:        []string{"uk", "ru", "en"},
	}, {
		nationalVersion: "ru",
		defaultLang:     "en",
		expected:        []string{"en", "ru", "uk"},
	}, {
		nationalVersion: "uk",
		defaultLang:     "ru",
		expected:        []string{"ru", "uk", "en"},
	}, {
		nationalVersion: "uk",
		defaultLang:     "uk",
		expected:        []string{"uk", "ru", "en"},
	}, {
		nationalVersion: "uk",
		defaultLang:     "en",
		expected:        []string{"en", "uk", "ru"},
	},
}

func TestGetLangsByNationalVersion(t *testing.T) {
	for _, tc := range getLangsByNationalVersionCases {
		res := GetLangsByNationalVersion(tc.nationalVersion, tc.defaultLang)
		if !reflect.DeepEqual(res, tc.expected) {
			t.Errorf("Expected %#v\nGot %#v.", tc.expected, res)
		}
	}
}

var isValidPJSONCases = []struct {
	request  string
	expected bool
}{
	{
		request:  "<script>alert(1)</script>",
		expected: false,
	}, {
		request:  "jQuery183007112223241425645_1499632683318",
		expected: true,
	}, {
		request:  "<img src='', onerror='alert(1)'>",
		expected: false,
	}, {
		request:  "jQuery183007112223241425645_1499632683318183007112223241425645_1499632683318183007112223241425645_1499632683318",
		expected: false,
	},
}

func TestIsValidPJSON(t *testing.T) {
	for _, tc := range isValidPJSONCases {
		res := IsValidPJSON(tc.request)
		if res != tc.expected {
			t.Errorf("Expected %#v\nGot %#v.", tc.expected, res)
		}
	}
}

var wsSplitTitleDataItems = models.TitleDataArray{
	{
		ID:       1,
		IsPrefix: true,
	}, {
		ID:       2,
		IsPrefix: false,
	}, {
		ID:       3,
		IsPrefix: true,
	},
}

var wsSplitTitleDataExpectedWeighted = models.WeightedTitleDataArray{
	{
		Weights:  []int{0, 0, 6, 0},
		IsPrefix: true,
		ID:       3,
	},
}

var wsSplitTitleDataExpectedUnweighted = models.TitleDataArray{
	wsSplitTitleDataItems[0],
	wsSplitTitleDataItems[1],
}

func TestWsSplitTitleData(t *testing.T) {
	ws := WeightSplitter{
		Regions:   []int{52, 54, 225},
		Limit:     3,
		Threshold: 5,
		Routes: models.StatRouteToMapping{
			2: {},
			3: {54: 4, 225: 6},
		},
	}
	wdata, uwdata := ws.SplitData(wsSplitTitleDataItems, true, "bus")
	if len(wdata) != len(wsSplitTitleDataExpectedWeighted) || len(uwdata) != len(wsSplitTitleDataExpectedUnweighted) {
		t.Errorf("Wrong result length. Expected %d:%d, got %d:%d.",
			len(wsSplitTitleDataExpectedWeighted), len(wsSplitTitleDataExpectedUnweighted), len(wdata), len(uwdata))
	}
	if !reflect.DeepEqual(wdata, wsSplitTitleDataExpectedWeighted) {
		t.Errorf("Expected %#v, got %#v.", wdata, wsSplitTitleDataExpectedWeighted)
	}
	if !reflect.DeepEqual(uwdata, wsSplitTitleDataExpectedUnweighted) {
		t.Errorf("Expected %#v, got %#v.", uwdata, wsSplitTitleDataExpectedUnweighted)
	}
}

var unpackTitleDataArrayBenchmarkCase = []models.TitleDataArray{{{ID: 1, IsPrefix: true}, {ID: 2, IsPrefix: false}}, {{ID: 3, IsPrefix: true}, {ID: 4, IsPrefix: false}}, {{ID: 5, IsPrefix: true}}}

func BenchmarkUnpackTitleDataArray(b *testing.B) {
	for i := 0; i < b.N; i++ {
		UnpackTitleDataArray(unpackTitleDataArrayBenchmarkCase)
	}
}

func BenchmarkUnpackTitleDataArrayParallel(b *testing.B) {
	b.RunParallel(func(pb *testing.PB) {
		for pb.Next() {
			UnpackTitleDataArray(unpackTitleDataArrayBenchmarkCase)
		}
	})
}

func BenchmarkTransliterate(b *testing.B) {
	for i := 0; i < b.N; i++ {
		Transliterate("moskva", "lat-cyr")
	}
}

func BenchmarkTransliterateParallel(b *testing.B) {
	b.RunParallel(func(pb *testing.PB) {
		for pb.Next() {
			Transliterate("moskva", "lat-cyr")
		}
	})
}

func BenchmarkGetLayoutVariants(b *testing.B) {
	for i := 0; i < b.N; i++ {
		GetLayoutVariants("trfnthby,ehu", "ru")
	}
}

func BenchmarkGetLayoutVariantsParallel(b *testing.B) {
	b.RunParallel(func(pb *testing.PB) {
		for pb.Next() {
			GetLayoutVariants("trfnthby,ehu", "ru")
		}
	})
}

func BenchmarkPrepareTitleText(b *testing.B) {
	for i := 0; i < b.N; i++ {
		PrepareTitleText("Екатеринбург")
	}
}

func BenchmarkPrepareTitleTextParallel(b *testing.B) {
	b.RunParallel(func(pb *testing.PB) {
		for pb.Next() {
			PrepareTitleText("Екатеринбург")
		}
	})
}

func buildNLargestArray(size int) models.WeightedTitleDataArray {
	res := make(models.WeightedTitleDataArray, 0, size)
	for i := 0; i < size; i++ {
		k := rand.Intn(7) + 1
		buf := make([]int, k)
		for j := 0; j < k; j++ {
			buf[j] = rand.Intn(1000)
		}
		res = append(res, models.WeightedTitleData{ID: i, IsPrefix: true, Weights: buf})
	}
	return res
}

var nLargestBigArray = models.BuildWeightedTitleDataArrayCases(100000)

func BenchmarkNlargest(b *testing.B) {
	for i := 0; i < b.N; i++ {
		Nlargest(10, nLargestBigArray)
	}
}

var nLargestCases = []struct {
	limit    int
	data     models.WeightedTitleDataArray
	expected models.WeightedTitleDataArray
}{
	{
		limit: 2,
		data: models.WeightedTitleDataArray{
			{ID: 1, IsPrefix: true, Weights: []int{0, 0, 1}},
			{ID: 2, IsPrefix: false, Weights: []int{0, 0, 2}},
		},
		expected: models.WeightedTitleDataArray{
			{ID: 2, IsPrefix: false, Weights: []int{0, 0, 2}},
			{ID: 1, IsPrefix: true, Weights: []int{0, 0, 1}},
		},
	},
	{
		limit: 2,
		data: models.WeightedTitleDataArray{
			{ID: 1, IsPrefix: true, Weights: []int{1, 2}},
			{ID: 2, IsPrefix: true, Weights: []int{1, 2, 3}},
		},
		expected: models.WeightedTitleDataArray{
			{ID: 2, IsPrefix: true, Weights: []int{1, 2, 3}},
			{ID: 1, IsPrefix: true, Weights: []int{1, 2}},
		},
	},
	{
		limit: 3,
		data: models.WeightedTitleDataArray{
			{ID: 2714, IsPrefix: true, Weights: []int{0, 0, 0, 0, 0, 30}},
			{ID: 5479, IsPrefix: true, Weights: []int{0, 0, 0, 0, 0, 20}},
			{ID: 34397, IsPrefix: true, Weights: []int{0, 0, 0, 0, 0, 11}},
			{ID: 9648, IsPrefix: true, Weights: []int{0, 0, 0, 0, 0, 28}},
		},
		expected: models.WeightedTitleDataArray{
			{ID: 2714, IsPrefix: true, Weights: []int{0, 0, 0, 0, 0, 30}},
			{ID: 9648, IsPrefix: true, Weights: []int{0, 0, 0, 0, 0, 28}},
			{ID: 5479, IsPrefix: true, Weights: []int{0, 0, 0, 0, 0, 20}},
		},
	},
	{
		limit: 5,
		data: models.WeightedTitleDataArray{
			{ID: 2004, IsPrefix: true, Weights: []int{1, 1, 1, 1, 0, 1, 1, 1, 1000}},
			{ID: 2006, IsPrefix: true, Weights: []int{1, 1, 1, 1, 1, 1, 0, 1, 1000}},
			{ID: 2002, IsPrefix: true, Weights: []int{1, 1, 0, 1, 1, 1, 1, 1, 1000}},
			{ID: 2005, IsPrefix: false, Weights: []int{1, 1, 1, 1, 1, 0, 1, 1, 1000}},
			{ID: 2007, IsPrefix: false, Weights: []int{1, 1, 1, 1, 1, 1, 1, 0, 1000}},
			{ID: 2008, IsPrefix: false, Weights: []int{1, 1, 1, 1, 1, 1, 1, 1, 0}},
			{ID: 2009, IsPrefix: true, Weights: []int{1, 1, 1, 1, 1, 1, 1, 1, 0}},
			{ID: 2001, IsPrefix: false, Weights: []int{1, 0, 1, 1, 1, 1, 1, 1, 1000}},
			{ID: 2000, IsPrefix: true, Weights: []int{0, 1, 1, 1, 1, 1, 1, 1, 1000}},
			{ID: 2003, IsPrefix: false, Weights: []int{1, 1, 1, 0, 1, 1, 1, 1, 1000}},
		},
		expected: models.WeightedTitleDataArray{
			{ID: 2009, IsPrefix: true, Weights: []int{1, 1, 1, 1, 1, 1, 1, 1, 0}},
			{ID: 2008, IsPrefix: false, Weights: []int{1, 1, 1, 1, 1, 1, 1, 1, 0}},
			{ID: 2007, IsPrefix: false, Weights: []int{1, 1, 1, 1, 1, 1, 1, 0, 1000}},
			{ID: 2006, IsPrefix: true, Weights: []int{1, 1, 1, 1, 1, 1, 0, 1, 1000}},
			{ID: 2005, IsPrefix: false, Weights: []int{1, 1, 1, 1, 1, 0, 1, 1, 1000}},
		},
	},
}

func TestNLargest(t *testing.T) {
	for _, tc := range nLargestCases {
		res := Nlargest(tc.limit, tc.data)
		if !reflect.DeepEqual(res, tc.expected) {
			t.Errorf("Expected %#v\nGot %#v.", tc.expected, res)
		}
	}
}
