package actions

import (
	"fmt"
	"reflect"
	"testing"
)

func TestSimpleMerge(t *testing.T) {
	m1 := make(map[string]interface{})
	m1["one"] = 1
	m2 := make(map[string]interface{})
	m2["two"] = 2
	expected := make(map[string]interface{})
	expected["one"] = 1
	expected["two"] = 2
	merged := Merge(m1, m2)
	eq := reflect.DeepEqual(merged, expected)
	if !eq {

		fmt.Println("They're unequal.")
	}
}
