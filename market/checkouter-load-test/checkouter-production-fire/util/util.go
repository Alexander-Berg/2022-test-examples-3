package util

import (
	"fmt"
	"strconv"
	"strings"
)

func GenMatchFunc(query string) func(int) bool {
	if len(query) == 0 {
		return func(int) bool { return true }
	}

	ids := strings.Split(query, ",")
	set := map[int]bool{}

	for _, id := range ids {
		val, err := strconv.Atoi(id)
		if err != nil {
			panic(err)
		}
		set[val] = true
	}

	return func(id int) bool {
		val, ok := set[id]
		return ok && val
	}
}

func GetFloat32(v interface{}) float32 {
	switch t := v.(type) {
	case int:
		return float32(v.(int))
	case float64:
		return float32(v.(float64))
	case float32:
		return v.(float32)
	default:
		panic(fmt.Errorf("cannot cast '%v' with type '%v' to float32", v, t))
	}
}

func Min(a, b int) int {
	if a < b {
		return a
	}
	return b
}

func Max(a, b int) int {
	if a > b {
		return a
	}
	return b
}
