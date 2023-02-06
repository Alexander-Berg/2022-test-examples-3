package task

import (
	"testing"

	"a.yandex-team.ru/travel/buses/backend/internal/common/dict"
	pb "a.yandex-team.ru/travel/buses/backend/proto"
	wpb "a.yandex-team.ru/travel/buses/backend/proto/worker"
	"a.yandex-team.ru/travel/library/go/metrics"
	tpb "a.yandex-team.ru/travel/proto"
)

var (
	testMetricsRegistry = metrics.NewAppMetricsRegistryWithPrefix("")
	testAppMetrics      = metrics.NewAppMetrics(testMetricsRegistry)
)

func searchTaskQueueCheck(t *testing.T, tasks []*wpb.TSearchRequest, expected map[uint32][]uint32) bool {
	queue := NewSearchTaskQueue(testAppMetrics)
	for _, request := range tasks {
		_, err := queue.Push(request)
		if err != nil {
			t.Errorf("unexpected error: %s", err.Error())
			return false
		}
	}

	for sID, expectedRequests := range expected {
		i := 0
		for {
			request, err := queue.Pop(sID)
			if err != nil {
				if err != ErrNoTasks {
					t.Errorf("error is not ErrNoTasks")
					return false
				}
				if i == len(expectedRequests) {
					break
				}
				t.Errorf("expected %d tasks but got %d for sID=%d", len(expectedRequests), i, sID)
				return false
			}
			if i >= len(expectedRequests) {
				t.Errorf("expected %d tasks but got more for sID=%d: %v", len(expectedRequests), sID, request)
				return false
			}
			if expectedRequests[i] != request.From.Id {
				t.Errorf("bad pop sequence. Got %d, expected %d", request.From.Id, expectedRequests[i])
				return false
			}
			i++
		}
	}
	return true
}

func TestSearchTaskQueue(t *testing.T) {

	suppliersID := dict.GetSuppliersList()
	supplier1 := suppliersID[0]
	supplier2 := suppliersID[1]
	supplier3 := suppliersID[2]
	from1 := &pb.TPointKey{Id: 1}
	from2 := &pb.TPointKey{Id: 2}
	from3 := &pb.TPointKey{Id: 3}
	to := &pb.TPointKey{Id: 123}
	date := &tpb.TDate{}
	headerHigh := wpb.TRequestHeader{Priority: wpb.ERequestPriority_REQUEST_PRIORITY_HIGH}
	headerNormal := wpb.TRequestHeader{Priority: wpb.ERequestPriority_REQUEST_PRIORITY_NORMAL}
	headerLow := wpb.TRequestHeader{Priority: wpb.ERequestPriority_REQUEST_PRIORITY_LOW}

	t.Run("TestSearchTaskQueue. Empty", func(t *testing.T) {
		if !searchTaskQueueCheck(
			t,
			[]*wpb.TSearchRequest{},
			map[uint32][]uint32{
				supplier1: {},
			}) {
			return
		}
	})

	t.Run("TestSearchTaskQueue. Duplicates", func(t *testing.T) {
		if !searchTaskQueueCheck(
			t,
			[]*wpb.TSearchRequest{
				{Header: &headerHigh, SupplierId: supplier1, From: from1, To: to, Date: date},
				{Header: &headerHigh, SupplierId: supplier1, From: from1, To: to, Date: date},
				{Header: &headerNormal, SupplierId: supplier1, From: from1, To: to, Date: date},
				{Header: &headerLow, SupplierId: supplier1, From: from1, To: to, Date: date},
				{Header: &headerNormal, SupplierId: supplier2, From: from2, To: to, Date: date},
				{Header: &headerLow, SupplierId: supplier2, From: from2, To: to, Date: date},
			},
			map[uint32][]uint32{
				supplier1: {from1.Id},
				supplier2: {from2.Id},
				supplier3: {},
			}) {
			return
		}
	})

	t.Run("TestSearchTaskQueue. Order", func(t *testing.T) {
		if !searchTaskQueueCheck(
			t,
			[]*wpb.TSearchRequest{
				{Header: &headerLow, SupplierId: supplier1, From: from3, To: to, Date: date},
				{Header: &headerHigh, SupplierId: supplier1, From: from1, To: to, Date: date},
				{Header: &headerNormal, SupplierId: supplier1, From: from2, To: to, Date: date},
				{Header: &headerLow, SupplierId: supplier2, From: from1, To: to, Date: date},
				{Header: &headerHigh, SupplierId: supplier2, From: from2, To: to, Date: date},
				{Header: &headerNormal, SupplierId: supplier2, From: from3, To: to, Date: date},
			},
			map[uint32][]uint32{
				supplier1: {from1.Id, from2.Id, from3.Id},
				supplier2: {from2.Id, from3.Id, from1.Id},
				supplier3: {},
			}) {
			return
		}
	})

	t.Run("TestSearchTaskQueue. Order with priority change", func(t *testing.T) {
		if !searchTaskQueueCheck(
			t,
			[]*wpb.TSearchRequest{
				{Header: &headerLow, SupplierId: supplier1, From: from1, To: to, Date: date},
				{Header: &headerLow, SupplierId: supplier1, From: from2, To: to, Date: date},
				{Header: &headerNormal, SupplierId: supplier1, From: from3, To: to, Date: date},
				{Header: &headerLow, SupplierId: supplier1, From: from1, To: to, Date: date},
				{Header: &headerHigh, SupplierId: supplier1, From: from2, To: to, Date: date},
				{Header: &headerLow, SupplierId: supplier1, From: from3, To: to, Date: date},
				{Header: &headerHigh, SupplierId: supplier1, From: from1, To: to, Date: date},
			},
			map[uint32][]uint32{
				supplier1: {from2.Id, from1.Id, from3.Id},
			}) {
			return
		}
	})

	t.Run("TestSearchTaskQueue. Index", func(t *testing.T) {
		queue := NewSearchTaskQueue(testAppMetrics)
		if i, _ := queue.Push(
			&wpb.TSearchRequest{Header: &headerLow, SupplierId: supplier1, From: from1, To: to, Date: date},
		); i != 1 {
			t.Errorf("bad number response %d!=1", i)
		}
		if i, _ := queue.Push(
			&wpb.TSearchRequest{Header: &headerLow, SupplierId: supplier1, From: from2, To: to, Date: date},
		); i != 2 {
			t.Errorf("bad number response %d!=2", i)
		}
		if i, _ := queue.Push(
			&wpb.TSearchRequest{Header: &headerHigh, SupplierId: supplier1, From: from1, To: to, Date: date},
		); i != 1 {
			t.Errorf("bad number response %d!=1", i)
		}
		if i, _ := queue.Push(
			&wpb.TSearchRequest{Header: &headerHigh, SupplierId: supplier1, From: from2, To: to, Date: date},
		); i != 2 {
			t.Errorf("bad number response %d!=2", i)
		}
		if i, _ := queue.Push(
			&wpb.TSearchRequest{Header: &headerHigh, SupplierId: supplier2, From: from1, To: to, Date: date},
		); i != 1 {
			t.Errorf("bad number response %d!=1", i)
		}
		if _, err := queue.Push(
			&wpb.TSearchRequest{Header: &headerHigh, SupplierId: 100500, From: from1, To: to, Date: date},
		); err == nil {
			t.Errorf("expected error for unknown supplier")
		}
	})
}
