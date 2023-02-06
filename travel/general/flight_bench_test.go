package flight

import (
	"strconv"
	"strings"
	"testing"
)

type flightStruct struct {
	carrier int32
	number  string
	leg     int32
	key     string
}

var flights = [...]flightStruct{
	{
		carrier: 123,
		number:  "456",
		leg:     1,
		key:     "123.456.1",
	},
	{
		carrier: 123,
		number:  "456",
		leg:     2,
		key:     "123.456.2",
	},
	{
		carrier: 123,
		number:  "456",
		leg:     1,
		key:     "123.456.1",
	},
	{
		carrier: 123,
		number:  "456",
		leg:     2,
		key:     "123.456.2",
	},
}

func Benchmark_StructVsStringKey_Struct(b *testing.B) {
	type structKey struct {
		carrier int32
		number  string
		leg     int32
	}
	structMap := make(map[structKey]int32, 100)
	var antiOptimizer bool
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		for _, f := range flights {
			structMap[structKey{
				number:  f.number,
				carrier: f.carrier,
				leg:     f.leg,
			}] = f.leg

			if _, ok := structMap[structKey{
				carrier: f.carrier,
				number:  f.number,
				leg:     f.leg - 1,
			}]; ok {
				antiOptimizer = true
			} else {
				antiOptimizer = false
			}
		}
	}
	if antiOptimizer {
		antiOptimizer = false
	}
}

func Benchmark_StructVsStringKey_String(b *testing.B) {
	structMap := make(map[string]int, 100)
	var antiOptimizer bool
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		for _, f := range flights {
			parts := strings.Split(f.key, ".")
			legNumber, _ := strconv.Atoi(parts[2])

			key := strings.Join(parts, ".")
			structMap[key] = legNumber

			parts[2] = strconv.Itoa(legNumber - 1)

			key = strings.Join(parts, ".")

			if _, ok := structMap[key]; ok {
				antiOptimizer = true
			} else {
				antiOptimizer = false
			}
		}
	}
	if antiOptimizer {
		antiOptimizer = false
	}
}
