package badges

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/core/log/nop"
	aviaApi "a.yandex-team.ru/travel/app/backend/api/avia/v1"
	v1 "a.yandex-team.ru/travel/app/backend/api/common/v1"
	aviaSearchProto "a.yandex-team.ru/travel/app/backend/internal/avia/search/proto/v1"
	"a.yandex-team.ru/travel/app/backend/internal/lib/aviatdapiclient"
	"a.yandex-team.ru/travel/avia/library/go/searchcontext"
	"a.yandex-team.ru/travel/library/go/containers"
)

var (
	comfortableObserverBuilder = NewComfortableBadgeObserverBuilder(&DefaultComfortableBadgeObserverBuilderConfig, &nop.Logger{})
)

func generateSnippetPair(key string, forward []string, forwardDuration uint32, price float64) (*aviaSearchProto.Snippet, *aviaApi.Snippet) {
	searchProtoSnippet := &aviaSearchProto.Snippet{
		Key:                     key,
		Forward:                 forward,
		ForwardDurationMinutes:  forwardDuration,
		BackwardDurationMinutes: 0,
	}
	respSnippet := &aviaApi.Snippet{
		Key: key,
		Variant: &aviaApi.Snippet_Variant{
			Key:   key,
			Price: &v1.Price{Currency: "RUB", Value: price},
		},
		Forward:                 forward,
		ForwardDurationMinutes:  forwardDuration,
		BackwardDurationMinutes: 0,
	}

	return searchProtoSnippet, respSnippet
}

func generateWay(flightNumber, snippetKey string, departure, arrival time.Time, price float64) (aviatdapiclient.Flight, *aviaSearchProto.Snippet, *aviaApi.Snippet) {
	departureString := departure.Format("2006-01-02T15:04:05")
	arrivalString := arrival.Format("2006-01-02T15:04:05")

	forwardDuration := uint32(arrival.Sub(departure).Minutes())

	flight := aviatdapiclient.Flight{
		Key:    flightNumber,
		Number: flightNumber,
		Departure: aviatdapiclient.DateTime{
			Local: departureString,
		},
		Arrival: aviatdapiclient.DateTime{
			Local: arrivalString,
		},
	}

	searchProtoSnippet, respSnippet := generateSnippetPair(snippetKey, []string{flight.Key}, forwardDuration, price)
	return flight, searchProtoSnippet, respSnippet
}

func TestSimple(t *testing.T) {
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

	comfortableObserverToCache := comfortableObserverBuilder.BuildToCache(
		context.Background(),
		&AviaClients{},
		&searchcontext.QID{},
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
	_, stats = comfortableObserverToCache.ObserveAllSnippets(snippets, stats)
	require.NotNil(t, stats.SnippetKeysForComfortableBadge, []string{"snippet-1", "snippet-3"})

	responseObserver := comfortableObserverBuilder.BuildToResponse(context.Background(), nil, nil, aviaApi.SearchSort_SEARCH_SORT_UNKNOWN, nil, stats)

	respSnippets := map[string]*aviaApi.Snippet{
		respSnippet1.Key: respSnippet1,
		respSnippet2.Key: respSnippet2,
		respSnippet3.Key: respSnippet3,
	}

	localStats := &SnippetStats{BestPrice: &v1.Price{Currency: "RUB", Value: 100}}
	snippetsWithBadges := containers.SetOf[string]()

	for _, snippet := range respSnippets {
		snippet = responseObserver.ObserveSnippet(snippet, localStats)
		if len(snippet.Badges) > 0 {
			snippetsWithBadges.Add(snippet.Key)
		}
	}

	require.Equal(t, snippetsWithBadges, containers.SetOf[string]("snippet-1", "snippet-3"))
}

func TestComfortableWithNightSimple(t *testing.T) {
	flight1, snippet1, respSnippet1 := generateWay(
		"HZ 2682",
		"snippet-1",
		time.Date(2022, 6, 23, 9, 20, 0, 0, time.Local),
		time.Date(2022, 6, 23, 12, 20, 0, 0, time.Local),
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

	comfortableObserverToCache := comfortableObserverBuilder.BuildToCache(
		context.Background(),
		&AviaClients{},
		&searchcontext.QID{},
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
	_, stats = comfortableObserverToCache.ObserveAllSnippets(snippets, stats)
	require.Equal(t, []string{"snippet-1"}, stats.SnippetKeysForComfortableBadge)

	responseObserver := comfortableObserverBuilder.BuildToResponse(context.Background(), nil, nil, aviaApi.SearchSort_SEARCH_SORT_UNKNOWN, nil, stats)

	respSnippets := map[string]*aviaApi.Snippet{
		respSnippet1.Key: respSnippet1,
		respSnippet2.Key: respSnippet2,
		respSnippet3.Key: respSnippet3,
	}

	localStats := &SnippetStats{BestPrice: &v1.Price{Currency: "RUB", Value: 100}}
	snippetsWithBadges := make([]string, 0)

	for _, snippet := range respSnippets {
		snippet = responseObserver.ObserveSnippet(snippet, localStats)
		if len(snippet.Badges) > 0 {
			snippetsWithBadges = append(snippetsWithBadges, snippet.Key)
		}
	}

	require.Equal(t, []string{"snippet-1"}, snippetsWithBadges)
}
