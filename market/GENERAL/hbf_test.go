package hbf

import (
	"sort"
	"testing"
)

func TestRawItemParsing(t *testing.T) {
	type outputType struct {
		Address   string
		Network   string
		ProjectID uint32
	}
	type testCase struct {
		input  string
		output outputType
	}
	var cases = []testCase{
		{"37.9.69.1/24", outputType{"37.9.69.1", "37.9.69.0/24", 0}},
		{"577@2a02:6b8:b010:5026::/64", outputType{"2a02:6b8:b010:5026::", "2a02:6b8:b010:5026::/64", 1399}},
		{"dev-null01hd.market.yandex.net", outputType{"", "", 0}},
	}
	for _, c := range cases {
		var s string
		var res = Item{}
		var err error
		if err = parseRawItem(c.input, &res); err != nil {
			t.Errorf("got error: %v", err)
		}
		if res.Address != nil {
			s = res.Address.String()
		} else {
			s = ""
		}
		if s != c.output.Address {
			t.Errorf("parseRawItem result was incorrect, on input \"%s\" got \"%v\" as Address, want: \"%v\"", c.input, res.Address, c.output.Address)
		}

		if res.Network != nil {
			s = res.Network.String()
		} else {
			s = ""
		}
		if s != c.output.Network {
			t.Errorf("parseRawItem result was incorrect, on input \"%s\" got \"%v\" as Network, want: \"%v\"", c.input, res.Network, c.output.Network)
		}

		if res.ProjectID != c.output.ProjectID {
			t.Errorf("parseRawItem result was incorrect, on input \"%s\" got \"%d\" as ProjectID, want: \"%d\"", c.input, res.ProjectID, c.output.ProjectID)
		}
	}
}

func TestSorting(t *testing.T) {
	var input = []string{
		"10.208.1.0/24",
		"100.43.72.112/28",
		"100.43.72.112/29",
		"213.180.216.0/22",
		"2620:10f:d000:101::/64",
		"2a02:6b8:b080::/44",
		"37.140.128.0/21",
		"0.0.0.0/0",
	}

	var output = []string{
		"0.0.0.0/0",
		"10.208.1.0/24",
		"37.140.128.0/21",
		"100.43.72.112/29",
		"100.43.72.112/28",
		"213.180.216.0/22",
		"2620:10f:d000:101::/64",
		"2a02:6b8:b080::/44",
	}

	var items = make(Macro, 0, len(input))

	for _, s := range input {
		var res = Item{}
		var err error
		if err = parseRawItem(s, &res); err != nil {
			t.Errorf("got error: %v", err)
		}
		items = append(items, res)
	}

	sort.Sort(items)
	for i, s := range items {
		if s.OriginalName != output[i] {
			t.Errorf("sorting is broken, on pos %d want \"%s\", got \"%s\"", i, output[i], s.OriginalName)
		}
	}
}
