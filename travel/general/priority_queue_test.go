package containers

import (
	"math/rand"
	"reflect"
	"sort"
	"testing"

	"a.yandex-team.ru/travel/rasp/suggests/models"
)

var priorityQueueCases = models.BuildWeightedTitleDataArrayCases(1000)

const priorityQueueTestCapacity = 10

func AreWeightedTitleDataArrayEqual(a, b models.WeightedTitleDataArray) bool {
	if len(a) != len(b) {
		return false
	}

	for i := range a {
		if !reflect.DeepEqual(a[i].Weights, b[i].Weights) {
			return false
		}
	}

	return true
}

func TestPriorityQueue(t *testing.T) {
	rand.Seed(42)

	pq := NewPriorityQueue(priorityQueueTestCapacity)
	for ind, testCase := range priorityQueueCases {
		expected := append(models.WeightedTitleDataArray(nil), priorityQueueCases[:ind+1]...)
		sort.Sort(sort.Reverse(expected))

		if len(expected) > priorityQueueTestCapacity {
			expected = expected[:priorityQueueTestCapacity]
		}
		pq.TryPushItem(testCase)
		items := pq.GetItems()
		if !AreWeightedTitleDataArrayEqual(expected, items) {
			t.Errorf("Wrong value for %#v, expected\n%#v,\ngot\n%#v.", ind, expected, pq.GetItems())
		}
	}
}

func BenchmarkPriorityQueue_TryPushItem(b *testing.B) {
	rand.Seed(42)

	for i := 0; i < b.N; i++ {
		pq := NewPriorityQueue(priorityQueueTestCapacity)
		for _, testCase := range priorityQueueCases {
			pq.TryPushItem(testCase)
		}
	}
}

func BenchmarkPriorityQueue_GetItems(b *testing.B) {
	rand.Seed(42)
	pq := NewPriorityQueue(priorityQueueTestCapacity)
	for _, testCase := range priorityQueueCases {
		pq.TryPushItem(testCase)
	}

	for i := 0; i < b.N; i++ {
		pq.GetItems()
	}
}
