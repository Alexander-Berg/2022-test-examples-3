package generator

import (
	"gorm.io/gorm/utils/tests"
	"container/heap"
	"testing"
)

func TestPQ(t *testing.T) {
	queue := &RoundHeap{}
	r1 := Round{
		Ts:        2,
		Payload:   "r2",
		RoundType: "r2",
	}
	r2 := Round{
		Ts:        1,
		Payload:   "r1",
		RoundType: "r1",
	}
	r3 := Round{
		Ts:        3,
		Payload:   "r3",
		RoundType: "r3",
	}
	heap.Init(queue)
	heap.Push(queue, &r1)
	heap.Push(queue, &r2)
	heap.Push(queue, &r3)
	tests.AssertEqual(t,heap.Pop(queue),r2)
	tests.AssertEqual(t,heap.Pop(queue),r1)
	tests.AssertEqual(t,heap.Pop(queue),r3)
}
