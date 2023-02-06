package helpers

import (
	"reflect"
	"sort"
	"testing"
)

func TestNetworksSorting(t *testing.T) {
	var input = []string{
		"10.208.1.0/24",
		"100.43.72.112/28",
		"100.43.72.112/29",
		"213.180.216.0/22",
		"2620:10f:d000:101::/64",
		"2a02:6b8:b080::/44",
		"37.140.128.0/21",
		"0.0.0.0/0",
		"just some text",
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
		"just some text",
	}

	sort.Sort(StringAsNetwork(input))

	if !reflect.DeepEqual(input, output) {
		t.Errorf("StringAsNetwork is broken: want %v, got %v", output, input)
	}
}
