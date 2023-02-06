package geosearch

import (
	"testing"

	"a.yandex-team.ru/travel/rasp/wizards/go/wizard_proxy_api/internal/geomodel"
)

func TestCheckInStopWords(t *testing.T) {
	examples := map[string]bool{"пункт": true, " -.()пункт -.()": true, "словоНеИзСпискаСтопСлов": false}
	for s, expected := range examples {
		if checkInStopWords(s) != expected {
			t.Errorf("Error checkInStopWords for \"%v\" is different from expected %v", s, expected)
		}
	}
}

func TestSortedSettlements(t *testing.T) {
	var settlements []*geomodel.Settlement

	result := sortedSettlements(settlements)
	if result != nil {
		t.Error("Error sortedSettlements for nil case")
	}

	settlements = []*geomodel.Settlement{}
	result = sortedSettlements(settlements)
	if len(result) != 0 {
		t.Error("Error sortedSettlements for empty case")
	}
}
