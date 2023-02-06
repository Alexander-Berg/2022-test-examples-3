package badges

import (
	"context"
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/core/log/nop"
	aviaApi "a.yandex-team.ru/travel/app/backend/api/avia/v1"
	v1 "a.yandex-team.ru/travel/app/backend/api/common/v1"
)

var (
	bestPriceObserverBuilder = NewBestPriceBadgeObserverBuilder(&nop.Logger{})
)

func TestBestPriceIsBest(t *testing.T) {
	toResponse := bestPriceObserverBuilder.BuildToResponse(context.Background(), nil, nil, aviaApi.SearchSort_SEARCH_SORT_UNKNOWN, nil, nil)
	localStats := &SnippetStats{BestPrice: &v1.Price{Currency: "RUB", Value: 100}}

	snippet := &aviaApi.Snippet{
		Variant: &aviaApi.Snippet_Variant{
			Price: &v1.Price{Currency: "RUB", Value: 100},
		},
	}
	snippet = toResponse.ObserveSnippet(snippet, localStats)
	require.Equal(t, []*aviaApi.Snippet_Badge{{Type: aviaApi.Snippet_BADGE_TYPE_BEST_PRICE}}, snippet.Badges)
}

func TestBestPriceNotBest(t *testing.T) {
	toResponse := bestPriceObserverBuilder.BuildToResponse(context.Background(), nil, nil, aviaApi.SearchSort_SEARCH_SORT_UNKNOWN, nil, nil)
	localStats := &SnippetStats{BestPrice: &v1.Price{Currency: "RUB", Value: 100}}

	snippet := &aviaApi.Snippet{
		Variant: &aviaApi.Snippet_Variant{
			Price: &v1.Price{Currency: "RUB", Value: 200},
		},
	}
	snippet = toResponse.ObserveSnippet(snippet, localStats)
	require.Equal(t, 0, len(snippet.Badges))
}
