package references

import (
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/travel/avia/wizard/pkg/wizard/caches/references"
	dicts "a.yandex-team.ru/travel/proto/avia/wizard"
)

func TestEmptySettlementPopularities(t *testing.T) {
	ref := references.NewSettlementPopularities()

	populator := NewPopulator()
	require.NoError(t, ref.UpdateFromSource(populator))
	require.Len(t, ref.GetOrderedPopularities(), 0)
}

func TestOrderedSettlementPopularities(t *testing.T) {
	ref := references.NewSettlementPopularities()

	populator := NewPopulator(
		makeSettlementNational(1, 10),
		makeSettlementNational(2, 100),
		makeSettlementNational(2, 1),
	)
	require.NoError(t, ref.UpdateFromSource(populator))
	require.Len(t, ref.GetOrderedPopularities(), 3)

	prev := 0
	for _, item := range ref.GetOrderedPopularities() {
		require.Greater(t, item.Popularity, prev)
		prev = item.Popularity
	}
}

func makeSettlementNational(settlementID int32, popularity int32) *dicts.TSettlementNational {
	return &dicts.TSettlementNational{
		SettlementID: settlementID,
		Popularity:   popularity,
		Arrival:      true,
	}
}
