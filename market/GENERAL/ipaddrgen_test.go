package ipaddrgen

import (
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestNewNetworkParams(t *testing.T) {
	var err error
	var params *NetworkParams
	t.Run("full params", func(t *testing.T) {
		params, err = NewNetworkParams("2a02:6b8:0:5409::/64", "3c:ec:ef:1a:c9:56", "test.market.yandex.net", "577")
		assert.NoError(t, err)
		assert.NotNil(t, params)
	})

	t.Run("without project", func(t *testing.T) {
		params, err = NewNetworkParams("2a02:6b8:0:5409::/64", "3c:ec:ef:1a:c9:56", "test.market.yandex.net", "")
		assert.NoError(t, err)
		assert.NotNil(t, params)
	})

	t.Run("without network", func(t *testing.T) {
		params, err = NewNetworkParams("", "3c:ec:ef:1a:c9:56", "test.market.yandex.net", "577")
		assert.Error(t, err)
		assert.Nil(t, params)
	})

	t.Run("without mac and hostname", func(t *testing.T) {
		params, err = NewNetworkParams("2a02:6b8:0:5409::/64", "", "", "577")
		assert.Error(t, err)
		assert.Nil(t, params)
	})

	t.Run("without hostname", func(t *testing.T) {
		params, err = NewNetworkParams("2a02:6b8:0:5409::/64", "3c:ec:ef:1a:c9:56", "", "577")
		assert.NoError(t, err)
		assert.NotNil(t, params)
	})

	t.Run("without mac", func(t *testing.T) {
		params, err = NewNetworkParams("2a02:6b8:0:5409::/64", "", "test.market.yandex.net", "577")
		assert.NoError(t, err)
		assert.NotNil(t, params)
	})
}

func TestNetworkParams_GetHostIdAddr(t *testing.T) {
	var params *NetworkParams
	t.Run("full params", func(t *testing.T) {
		params, _ = NewNetworkParams("2a02:6b8:0:5409::/64", "3c:ec:ef:1a:c9:56", "test.market.yandex.net", "577")
		addr, err := params.GetHostIDAddr()
		assert.NoError(t, err)
		assert.Equal(t, "2a02:6b8:0:5409:0:577:528d:906f", addr.String())
	})

	t.Run("without projectID", func(t *testing.T) {
		params, _ = NewNetworkParams("2a02:6b8:0:5409::/64", "3c:ec:ef:1a:c9:56", "test.market.yandex.net", "0")
		addr, err := params.GetHostIDAddr()
		assert.NoError(t, err)
		assert.Equal(t, "2a02:6b8:0:5409::528d:906f", addr.String())
	})

	t.Run("without projectID2", func(t *testing.T) {
		params, _ = NewNetworkParams("2a02:6b8:0:5409::/64", "3c:ec:ef:1a:c9:56", "test.market.yandex.net", "")
		addr, err := params.GetHostIDAddr()
		assert.NoError(t, err)
		assert.Equal(t, "2a02:6b8:0:5409::528d:906f", addr.String())
	})

	t.Run("with letters in projectID", func(t *testing.T) {
		params, _ = NewNetworkParams("2a02:6b8:0:5409::/64", "3c:ec:ef:1a:c9:56", "test.market.yandex.net", "457a")
		addr, err := params.GetHostIDAddr()
		assert.NoError(t, err)
		assert.Equal(t, "2a02:6b8:0:5409:0:457a:528d:906f", addr.String())
	})

	t.Run("without hostname", func(t *testing.T) {
		params, _ = NewNetworkParams("2a02:6b8:0:5409::/64", "3c:ec:ef:1a:c9:56", "", "577")
		addr, err := params.GetHostIDAddr()
		assert.Error(t, err)
		assert.Nil(t, addr)
	})

	t.Run("without mac", func(t *testing.T) {
		params, _ = NewNetworkParams("2a02:6b8:0:5409::/64", "", "test.market.yandex.net", "577")
		addr, err := params.GetHostIDAddr()
		assert.NoError(t, err)
		assert.Equal(t, "2a02:6b8:0:5409:0:577:528d:906f", addr.String())
	})
}

func TestNetworkParams_GetEUI64Addr(t *testing.T) {
	var params *NetworkParams
	t.Run("full params", func(t *testing.T) {
		params, _ = NewNetworkParams("2a02:6b8:0:5409::/64", "3c:ec:ef:1a:c9:56", "test.market.yandex.net", "577")
		addr, err := params.GetEUI64Addr()
		assert.NoError(t, err)
		assert.NotNil(t, addr)
		assert.Equal(t, "2a02:6b8:0:5409:0:577:ef1a:c956", addr.String())
	})

	t.Run("without hostname", func(t *testing.T) {
		params, _ = NewNetworkParams("2a02:6b8:0:5409::/64", "3c:ec:ef:1a:c9:56", "", "0")
		addr, err := params.GetEUI64Addr()
		assert.NoError(t, err)
		assert.NotNil(t, addr)
		assert.Equal(t, "2a02:6b8:0:5409:3eec:efff:fe1a:c956", addr.String())
	})

	t.Run("without mac", func(t *testing.T) {
		params, _ = NewNetworkParams("2a02:6b8:0:5409::/64", "", "test.market.yandex.net", "0")
		_, err := params.GetEUI64Addr()
		assert.Error(t, err)
	})
}
