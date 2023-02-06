package flow

import (
	"a.yandex-team.ru/market/capi/checkouter-load-test/checkouter-production-fire/clients"
	"github.com/stretchr/testify/require"
	"testing"
)

func TestGetAllowedCoins(t *testing.T) {
	cartRootResponse := &clients.CartRootResponse{CoinIdsToUse: &[]int{10101, 10102, 10103}}
	coinsForCart := &clients.СoinsForCart{
		ApplicableCoins: []clients.UserCoinResponse{{ID: 10101}, {ID: 10102}},
		DisabledCoins:   map[string][]clients.UserCoinResponse{"DROP_RESTRICTION": {{ID: 10103}}},
	}

	result, logMsg := GetAllowedCoins(cartRootResponse, coinsForCart)

	require.Equal(t, len(*result), 2)
	require.Equal(t, (*result)[0], 10101)
	require.Equal(t, (*result)[1], 10102)
	require.Equal(t, logMsg, "Can not use coin with id: 10103, reason: DROP_RESTRICTION")
}
