package badges

import (
	"context"
	"testing"

	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"

	aviaSearchProto "a.yandex-team.ru/travel/app/backend/internal/avia/search/proto/v1"
	"a.yandex-team.ru/travel/app/backend/internal/lib/aviabackendclient"
	"a.yandex-team.ru/travel/app/backend/internal/lib/aviatdapiclient"
	"a.yandex-team.ru/travel/avia/library/go/searchcontext"
)

func generateSearchResultPrice(fromCompany, boy bool) *aviatdapiclient.SearchResultPrice {
	return &aviatdapiclient.SearchResultPrice{
		FromCompany: fromCompany,
		Boy:         boy,
	}
}

func onlyObserveRawVariantHelper(t *testing.T, observerBuilder BadgeObserverBuilder, fromCompany, boy bool, badges []*aviaSearchProto.Badge) {
	toCacheObserver := observerBuilder.BuildToCache(
		context.Background(),
		&AviaClients{},
		&searchcontext.QID{},
		&aviatdapiclient.SearchResultReference{},
	)

	require.Equal(
		t,
		toCacheObserver.ObserveRawVariant(
			generateSearchResultPrice(fromCompany, boy),
			nil,
		),
		badges,
	)
}

func onlyObserveSnippetHelper(t *testing.T, observerBuilder BadgeObserverBuilder, fare *aviatdapiclient.SearchResultFare, snippet *aviaSearchProto.Snippet, badges []*aviaSearchProto.Badge) {
	toCacheObserver := observerBuilder.BuildToCache(
		context.Background(),
		&AviaClients{},
		&searchcontext.QID{},
		&aviatdapiclient.SearchResultReference{},
	)

	snippetResult := toCacheObserver.ObserveSnippet(fare, snippet)
	require.Equal(t, snippetResult.Badges, badges)
}

type backendClientMock struct {
	mock.Mock
}

func (c *backendClientMock) TopFlights(ctx context.Context, nationalVersion, lang string, fromKey, toKey, date string, limit int) (*aviabackendclient.TopFlightsRsp, error) {
	args := c.Called(ctx, nationalVersion, lang, fromKey, toKey, date, limit)
	return args.Get(0).(*aviabackendclient.TopFlightsRsp), args.Error(1)
}
