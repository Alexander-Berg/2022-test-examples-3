package tariffmatcher

import (
	"regexp"
	"strconv"
	"testing"
)

// Current (2022-06-22) results (for locally run tests on a developer's laptop)
// Benchmark_MatchTariffCache/S7-8  92701550       367.8 ns/op      81 B/op       3 allocs/op
// Benchmark_MatchTariffCache/SU-8  92465692       391.3 ns/op      81 B/op       3 allocs/op
// Benchmark_MatchTariffRegexp/S7-8 45152253       756.8 ns/op      17 B/op       1 allocs/op
// Benchmark_MatchTariffRegexp/SU-8 29430982      1169   ns/op      17 B/op       1 allocs/op

var testData = []struct {
	testName          string
	regexpText        string
	matchingString    string
	nonMatchingString string
}{
	{
		testName:          "S7",
		regexpText:        "^((B|H|K|L|M|N|O|Q|R|P|S|T|V|W|Y|A|Z)(S?)(ST))\\w*$",
		matchingString:    "TSSTQGBHJ",
		nonMatchingString: "TSSTQGBH14+",
	},
	{
		testName:          "SU",
		regexpText:        "^(YFM|YFO|BFM|BFO|MFM|MFO|UFM|UFO|KFM|KFO|HFM|HFO|LFM|LFO|QFM|QFO|TFM|TFO|EFM|EFO|NFM|NFO|RFM|RFO)\\w*\\d*$",
		matchingString:    "TFOTQGBH1",
		nonMatchingString: "TFOTQGBH14+",
	},
}

func Benchmark_MatchTariffCache(b *testing.B) {
	regexpCache := NewRegexpCache()
	for _, test := range testData {
		compiledRegexp, _ := regexp.Compile(test.regexpText)
		b.Run(
			test.testName,
			func(b *testing.B) {
				for i := 0; i < b.N; i++ {
					match := regexpCache.MatchString(compiledRegexp, test.nonMatchingString+strconv.Itoa(i%200))
					if match {
						panic("Unexpected match")
					}
					match = regexpCache.MatchString(compiledRegexp, test.matchingString)
					if !match {
						panic("Unexpected non-match")
					}
				}
			},
		)
	}
}

func Benchmark_MatchTariffRegexp(b *testing.B) {
	for _, test := range testData {
		compiledRegexp, _ := regexp.Compile(test.regexpText)
		b.Run(
			test.testName,
			func(b *testing.B) {
				for i := 0; i < b.N; i++ {
					match := compiledRegexp.MatchString(test.nonMatchingString + strconv.Itoa(i%200))
					if match {
						panic("Unexpected match")
					}
					match = compiledRegexp.MatchString(test.matchingString)
					if !match {
						panic("Unexpected non-match")
					}
				}
			},
		)
	}
}
