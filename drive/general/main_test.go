package taxidwh

import (
	"reflect"
	"testing"

	"a.yandex-team.ru/drive/library/go/clients/taxidwh"
)

func TestBatchFix(t *testing.T) {
	batch := []taxidwh.Document{
		{ID: "1", Data: "1"},
		{ID: "1", Data: "2"},
		{ID: "2", Data: "3"},
		{ID: "3", Data: "4"},
		{ID: "5", Data: "5"},
		{ID: "5", Data: "6"},
		{ID: "5", Data: "7"},
	}
	result := batchFix(batch)
	expected := []taxidwh.Document{
		{ID: "1", Data: "2"},
		{ID: "2", Data: "3"},
		{ID: "3", Data: "4"},
		{ID: "5", Data: "7"},
	}
	if !reflect.DeepEqual(result, expected) {
		t.Fatalf("Expected: %v, got: %v", expected, result)
	}
}
