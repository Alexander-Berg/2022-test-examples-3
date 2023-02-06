package disclaimers

import (
	"sort"
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/travel/notifier/internal/service/pretrip/blocks"
	"a.yandex-team.ru/travel/notifier/internal/service/pretrip/blocks/ui"
)

func TestProvider(t *testing.T) {
	t.Run(
		"empty blocks list", func(t *testing.T) {
			provider := NewProvider(DefaultKeyset)
			result, _ := provider.GetDisclaimersBlock(blocks.BlockTypesSet{})
			require.Equal(t, []string{}, result.(ui.Disclaimers).Disclaimers)
		},
	)

	t.Run(
		"single block", func(t *testing.T) {
			provider := NewProvider(DefaultKeyset)
			result, _ := provider.GetDisclaimersBlock(blocks.BlockTypesSet{
				blocks.RidesBlock: true,
			})
			disclaimers := result.(ui.Disclaimers).Disclaimers
			require.Equal(t, 1, len(disclaimers))
			require.Contains(t, disclaimers[0], "Яндекс Go")
		},
	)

	t.Run(
		"two blocks", func(t *testing.T) {
			provider := NewProvider(DefaultKeyset)
			result, _ := provider.GetDisclaimersBlock(blocks.BlockTypesSet{
				blocks.AudioGuideBlock: true,
				blocks.MoviesBlock:     true,
			})
			disclaimers := result.(ui.Disclaimers).Disclaimers
			require.Equal(t, 3, len(disclaimers))
			sort.Strings(disclaimers)
			require.Contains(t, disclaimers[0], "izi.TRAVEL")
			require.Contains(t, disclaimers[1], "КиноПоиск")
			require.Contains(t, disclaimers[2], "Яндекс.Музыка")
		},
	)
}
