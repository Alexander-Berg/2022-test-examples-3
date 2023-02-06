package context

import (
	"context"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestUsePartnerCache(t *testing.T) {
	t.Run("default -> false", func(t *testing.T) {
		assert.False(t, UsePartnerCache(context.Background()))
	})

	t.Run("false -> false", func(t *testing.T) {
		ctx := WithPartnerCache(context.Background(), false)
		assert.False(t, UsePartnerCache(ctx))
	})

	t.Run("true -> true", func(t *testing.T) {
		ctx := WithPartnerCache(context.Background(), true)
		assert.True(t, UsePartnerCache(ctx))
	})
}
