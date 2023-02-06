package sorting

import (
	"testing"
	"time"

	"github.com/stretchr/testify/require"
	"google.golang.org/protobuf/types/known/timestamppb"

	aviaAPI "a.yandex-team.ru/travel/app/backend/api/avia/v1"
	commonAPI "a.yandex-team.ru/travel/app/backend/api/common/v1"
	aviaSearchProto "a.yandex-team.ru/travel/app/backend/internal/avia/search/proto/v1"
)

func TestSortSnippets(t *testing.T) {
	snippets := []*aviaAPI.Snippet{
		{
			Key:     "second",
			Forward: []string{"f1", "f2", "f-second"},
		},
		{
			Key:     "first",
			Forward: []string{"f4", "f5", "f-first"},
		},
	}
	flightReference := map[string]*aviaSearchProto.Flight{
		"f-second": {
			Arrival: timestamppb.New(time.Date(2022, 4, 29, 6, 0, 0, 0, time.Local)),
		},
		"f-first": {
			Arrival: timestamppb.New(time.Date(2022, 4, 29, 3, 0, 0, 0, time.Local)),
		},
	}
	sortBy := aviaAPI.SearchSort_SEARCH_SORT_BY_ARRIVAL
	sorter := NewManagerSorter(&ManagerSorterConfig{}, nil)
	sorter.Sort(snippets, flightReference, sortBy)

	require.Equal(t, snippets[0].Key, "first")
	require.Equal(t, snippets[1].Key, "second")
}

func TestSortSnippets_RUBAndUSD(t *testing.T) {
	snippets := []*aviaAPI.Snippet{
		getSnippetWithPrice("usd-100", 100, "USD", nil, nil),
		getSnippetWithPrice("usd-200", 200, "USD", nil, nil),
		getSnippetWithPrice("rub-400", 400, "RUB", nil, nil),
		getSnippetWithPrice("rub-300", 300, "RUB", nil, nil),
	}

	sortBy := aviaAPI.SearchSort_SEARCH_SORT_CHEAPEST_FIRST
	sorter := NewManagerSorter(&ManagerSorterConfig{}, nil)
	sorter.Sort(snippets, nil, sortBy)

	require.Equal(t, snippets[0].Key, "rub-300")
	require.Equal(t, snippets[1].Key, "rub-400")
	require.Equal(t, snippets[2].Key, "usd-100")
	require.Equal(t, snippets[3].Key, "usd-200")

	sortBy = aviaAPI.SearchSort_SEARCH_SORT_EXPENSIVE_FIRST
	sorter = NewManagerSorter(&ManagerSorterConfig{}, nil)
	sorter.Sort(snippets, nil, sortBy)

	require.Equal(t, snippets[0].Key, "rub-400")
	require.Equal(t, snippets[1].Key, "rub-300")
	require.Equal(t, snippets[2].Key, "usd-200")
	require.Equal(t, snippets[3].Key, "usd-100")
}

func TestSortSnippetsWithSamePrices(t *testing.T) {
	snippets := []*aviaAPI.Snippet{
		getSnippetWithPrice("rub-200-1", 200, "RUB", []string{"1"}, nil),
		getSnippetWithPrice("rub-200-2", 200, "RUB", []string{"2"}, nil),
		getSnippetWithPrice("rub-100", 100, "RUB", []string{"1"}, nil),
	}
	flightReference := map[string]*aviaSearchProto.Flight{
		"1": {
			Departure: timestamppb.New(time.Date(2022, 4, 29, 3, 0, 0, 0, time.Local)),
			Arrival:   timestamppb.New(time.Date(2022, 4, 29, 6, 0, 0, 0, time.Local)),
		},
		"2": {
			Departure: timestamppb.New(time.Date(2022, 4, 29, 1, 0, 0, 0, time.Local)),
			Arrival:   timestamppb.New(time.Date(2022, 4, 29, 3, 0, 0, 0, time.Local)),
		},
	}
	sortBy := aviaAPI.SearchSort_SEARCH_SORT_CHEAPEST_FIRST
	sorter := NewManagerSorter(&ManagerSorterConfig{}, nil)
	sorter.Sort(snippets, flightReference, sortBy)

	require.Equal(t, snippets[0].Key, "rub-100")
	require.Equal(t, snippets[1].Key, "rub-200-2")
	require.Equal(t, snippets[2].Key, "rub-200-1")
}

func getSnippetWithPrice(snippetKey string, value float64, currency string, forward, backward []string) *aviaAPI.Snippet {
	return &aviaAPI.Snippet{
		Key: snippetKey,
		Variant: &aviaAPI.Snippet_Variant{
			Price: &commonAPI.Price{
				Currency: currency,
				Value:    value,
			},
		},
		Forward:  forward,
		Backward: backward,
	}
}
