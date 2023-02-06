package badges

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/core/log/ctxlog"
	"a.yandex-team.ru/library/go/core/log/nop"
	aviaApi "a.yandex-team.ru/travel/app/backend/api/avia/v1"
	v1 "a.yandex-team.ru/travel/app/backend/api/common/v1"
	aviaSearchProto "a.yandex-team.ru/travel/app/backend/internal/avia/search/proto/v1"
	"a.yandex-team.ru/travel/app/backend/internal/lib/aviabackendclient"
	"a.yandex-team.ru/travel/app/backend/internal/lib/aviatdapiclient"
	"a.yandex-team.ru/travel/avia/library/go/searchcontext"
)

var (
	combinedObserverBuilder = NewCombinedBadgeObserverBuilder(&DefaultCombinedBadgeObserverBuilderConfig, &nop.Logger{})
)

func newCombinedSimpleResponseObserver(cfg *CombinedBadgeObserverBuilderConfig, sortBy aviaApi.SearchSort) *ToResponseCombinedBadgeObserver {
	builder := NewCombinedBadgeObserverBuilder(cfg, &nop.Logger{})

	return builder.BuildToResponse(
		context.Background(),
		nil,
		&searchcontext.QID{},
		sortBy,
		nil,
		nil,
	).(*ToResponseCombinedBadgeObserver)
}

func CombinedTestHelper(t *testing.T, sortBy aviaApi.SearchSort, result map[string][]*aviaApi.Snippet_Badge) {
	qid := "220421-145911-315.travelapp.plane.c77_c11379_2022-04-26_None_economy_1_0_0_ru.ru"
	parsedQid, _ := searchcontext.ParseQID(qid)

	ctx := context.Background()
	updatedCtx := ctxlog.WithFields(ctx, log.String("qid", qid))

	gatewayMock := new(backendClientMock)
	gatewayMock.On("TopFlights", updatedCtx, "ru", "ru", "c77", "c11379", "2022-04-26", 100).Return(&aviabackendclient.TopFlightsRsp{
		Status: "topFlights",
		Data: [][]aviabackendclient.TopFlightElem{{
			{Numbers: "HZ 2682"},
			{Numbers: "HZ 2683"},
		}},
	}, nil)

	aviaClients := &AviaClients{AviaBackendClient: gatewayMock}

	flight1, snippet1, respSnippet1 := generateWay(
		"HZ 2682",
		"snippet-1",
		time.Date(2022, 6, 23, 1, 20, 0, 0, time.Local),
		time.Date(2022, 6, 23, 4, 20, 0, 0, time.Local),
		100,
	)

	flight2, snippet2, respSnippet2 := generateWay(
		"HZ 2683",
		"snippet-2",
		time.Date(2022, 6, 23, 1, 20, 0, 0, time.Local),
		time.Date(2022, 6, 23, 8, 20, 0, 0, time.Local),
		100,
	)

	flight3, snippet3, respSnippet3 := generateWay(
		"HZ 2684",
		"snippet-3",
		time.Date(2022, 6, 23, 1, 20, 0, 0, time.Local),
		time.Date(2022, 6, 23, 4, 40, 0, 0, time.Local),
		100,
	)

	combinedObserverToCache := combinedObserverBuilder.BuildToCache(
		updatedCtx,
		aviaClients,
		&parsedQid,
		&aviatdapiclient.SearchResultReference{
			Flights: []aviatdapiclient.Flight{flight1, flight2, flight3},
		},
	)

	snippets := map[string]*aviaSearchProto.Snippet{
		snippet1.Key: snippet1,
		snippet2.Key: snippet2,
		snippet3.Key: snippet3,
	}

	stats := &aviaSearchProto.CacheSnippetStats{}
	_, stats = combinedObserverToCache.ObserveAllSnippets(snippets, stats)

	responseObserver := combinedObserverBuilder.BuildToResponse(updatedCtx, aviaClients, &parsedQid, sortBy, nil, stats)

	respSnippets := map[string]*aviaApi.Snippet{
		respSnippet1.Key: respSnippet1,
		respSnippet2.Key: respSnippet2,
		respSnippet3.Key: respSnippet3,
	}

	localStats := &SnippetStats{BestPrice: &v1.Price{Currency: "RUB", Value: 100}}
	snippetsAndSorts := make(map[string][]*aviaApi.Snippet_Badge)

	for snippetKey, snippet := range respSnippets {
		snippet = responseObserver.ObserveSnippet(snippet, localStats)
		snippetsAndSorts[snippetKey] = snippet.Badges
	}

	require.Equal(t, result, snippetsAndSorts)
}

func TestCombinedWithSimpleSort(t *testing.T) {
	CombinedTestHelper(t, aviaApi.SearchSort_SEARCH_SORT_CHEAPEST_FIRST, map[string][]*aviaApi.Snippet_Badge{
		"snippet-1": {{Type: aviaApi.Snippet_BADGE_TYPE_BEST_PRICE}},
		"snippet-2": {{Type: aviaApi.Snippet_BADGE_TYPE_BEST_PRICE}},
		"snippet-3": {{Type: aviaApi.Snippet_BADGE_TYPE_BEST_PRICE}},
	})
}

func TestCombinedWithRecommendedSort(t *testing.T) {
	CombinedTestHelper(t, aviaApi.SearchSort_SEARCH_SORT_RECOMMENDED_FIRST, map[string][]*aviaApi.Snippet_Badge{
		"snippet-1": {{Type: aviaApi.Snippet_BADGE_TYPE_BEST_PRICE}, {Type: aviaApi.Snippet_BADGE_TYPE_COMFY}},
		"snippet-2": {{Type: aviaApi.Snippet_BADGE_TYPE_BEST_PRICE}},
		"snippet-3": {{Type: aviaApi.Snippet_BADGE_TYPE_BEST_PRICE}, {Type: aviaApi.Snippet_BADGE_TYPE_COMFY}},
	})
}

func TestCombinedRemoveBadgesIfRelativeExists(t *testing.T) {
	responseObserver := newCombinedSimpleResponseObserver(
		&CombinedBadgeObserverBuilderConfig{
			BadgesDeleteOnExist: map[aviaApi.Snippet_BadgeType][]aviaApi.Snippet_BadgeType{
				aviaApi.Snippet_BADGE_TYPE_COMFY: {aviaApi.Snippet_BADGE_TYPE_CHARTER, aviaApi.Snippet_BADGE_TYPE_BEST_PRICE},
			},
		},
		aviaApi.SearchSort_SEARCH_SORT_UNKNOWN,
	)

	badges := responseObserver.removeBadgesIfRelativeExists([]*aviaApi.Snippet_Badge{
		{
			Type: aviaApi.Snippet_BADGE_TYPE_BEST_PRICE,
		},
		{
			Type: aviaApi.Snippet_BADGE_TYPE_COMFY,
		},
	})

	require.Equal(t, []*aviaApi.Snippet_Badge{{Type: aviaApi.Snippet_BADGE_TYPE_BEST_PRICE}}, badges)
}

func TestCombinedSortAndSliceBadgesInSection(t *testing.T) {
	responseObserver := newCombinedSimpleResponseObserver(&CombinedBadgeObserverBuilderConfig{}, aviaApi.SearchSort_SEARCH_SORT_UNKNOWN)

	badges := responseObserver.sortAndSliceBadgesInSection(
		[]*aviaApi.Snippet_Badge{
			{Type: 4},
			{Type: 3},
			{Type: 2},
			{Type: 1},
		},
		&BadgeSectionConfig{
			Priority:    []aviaApi.Snippet_BadgeType{0, 1, 2, 3, 4, 5},
			MaxElements: 1,
		},
	)

	require.Equal(t, []*aviaApi.Snippet_Badge{{Type: 1}}, badges)
}

func TestCombinedSortAndSliceBadges(t *testing.T) {
	responseObserver := newCombinedSimpleResponseObserver(
		&CombinedBadgeObserverBuilderConfig{
			Sections: []BadgeSectionConfig{
				{
					Priority:    []aviaApi.Snippet_BadgeType{0},
					MaxElements: 1,
				},
				{
					Priority:    []aviaApi.Snippet_BadgeType{1, 2},
					MaxElements: 1,
				},
				{
					Priority:    []aviaApi.Snippet_BadgeType{3, 4, 5},
					MaxElements: 2,
				},
			},
		},
		aviaApi.SearchSort_SEARCH_SORT_UNKNOWN,
	)

	badges := responseObserver.sortAndSliceBadges([]*aviaApi.Snippet_Badge{
		{Type: 5},
		{Type: 4},
		{Type: 3},
		{Type: 2},
		{Type: 1},
	})

	require.Equal(t, []*aviaApi.Snippet_Badge{{Type: 1}, {Type: 3}, {Type: 4}}, badges)
}

func TestFilterBadgesBySnippetSort(t *testing.T) {
	responseObserver := newCombinedSimpleResponseObserver(&CombinedBadgeObserverBuilderConfig{
		BadgesOnlyForSort: map[aviaApi.Snippet_BadgeType][]aviaApi.SearchSort{
			0: {aviaApi.SearchSort_SEARCH_SORT_CHEAPEST_FIRST, aviaApi.SearchSort_SEARCH_SORT_RECOMMENDED_FIRST},
			1: {aviaApi.SearchSort_SEARCH_SORT_CHEAPEST_FIRST},
		},
	}, aviaApi.SearchSort_SEARCH_SORT_RECOMMENDED_FIRST,
	)

	badges := responseObserver.filterBadgesBySnippetSort([]*aviaApi.Snippet_Badge{
		{Type: 0},
		{Type: 1},
		{Type: 2},
	})

	require.Equal(t, []*aviaApi.Snippet_Badge{{Type: 0}, {Type: 2}}, badges)
}
