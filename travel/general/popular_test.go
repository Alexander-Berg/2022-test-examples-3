package badges

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/core/log/ctxlog"
	"a.yandex-team.ru/library/go/core/log/nop"
	aviaSearchProto "a.yandex-team.ru/travel/app/backend/internal/avia/search/proto/v1"
	"a.yandex-team.ru/travel/app/backend/internal/lib/aviabackendclient"
	"a.yandex-team.ru/travel/app/backend/internal/lib/aviatdapiclient"
	"a.yandex-team.ru/travel/avia/library/go/searchcontext"
)

var (
	popularObserverBuilder = NewPopularBadgeObserverBuilder(&DefaultPopularBadgeObserverBuilderConfig, &nop.Logger{})
)

func TestPopular(t *testing.T) {
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

	_, snippet1, _ := generateWay(
		"HZ 2682",
		"snippet-1",
		time.Date(2022, 6, 23, 1, 20, 0, 0, time.Local),
		time.Date(2022, 6, 23, 4, 20, 0, 0, time.Local),
		100,
	)

	_, snippet2, _ := generateWay(
		"HZ 2683",
		"snippet-2",
		time.Date(2022, 6, 23, 1, 20, 0, 0, time.Local),
		time.Date(2022, 6, 23, 8, 20, 0, 0, time.Local),
		100,
	)

	_, snippet3, _ := generateWay(
		"HZ 2684",
		"snippet-3",
		time.Date(2022, 6, 23, 1, 20, 0, 0, time.Local),
		time.Date(2022, 6, 23, 4, 40, 0, 0, time.Local),
		100,
	)

	popularObserverToCache := popularObserverBuilder.BuildToCache(
		updatedCtx,
		aviaClients,
		&parsedQid,
		&aviatdapiclient.SearchResultReference{},
	)

	snippets := map[string]*aviaSearchProto.Snippet{
		snippet1.Key: snippet1,
		snippet2.Key: snippet2,
		snippet3.Key: snippet3,
	}

	snippets, _ = popularObserverToCache.ObserveAllSnippets(snippets, &aviaSearchProto.CacheSnippetStats{})

	snippetsWithBadges := make([]string, 0)
	for _, snippet := range snippets {
		if len(snippet.Badges) > 0 {
			snippetsWithBadges = append(snippetsWithBadges, snippet.Key)
		}
	}

	require.NotNil(t, []string{"snippet-1", "snippet-2"}, snippetsWithBadges)
}
