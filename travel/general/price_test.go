package serialization

import (
	"testing"

	"github.com/stretchr/testify/assert"

	tpb "a.yandex-team.ru/travel/proto"
)

func TestCheckECurrencyFullness(t *testing.T) {
	for currencyID := range tpb.ECurrency_name {
		_, found := currencyToCode[tpb.ECurrency(currencyID)]
		assert.True(t, found, "Currency %s has to be registered in currencyToCode map")
	}
}
