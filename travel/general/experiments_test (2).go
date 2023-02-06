package experiments

import (
	"context"
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"
)

const (
	flag1 = "test-flag1"
	flag2 = "test-flag2"
)

func TestEmptyContext(t *testing.T) {
	assert.False(t, IsEnabledTestFlag1(context.Background()))
	assert.False(t, IsEnabledTestFlag2(context.Background()))
}

func TestEmptyFlags(t *testing.T) {
	ctx := ParseExperiments(context.Background(), "")
	assert.False(t, IsEnabledTestFlag1(ctx))
	assert.False(t, IsEnabledTestFlag2(ctx))
}

func TestEnabledFlags(t *testing.T) {
	ctx := ParseExperiments(context.Background(), fmt.Sprintf("fake-flag1,%s,fake-flag2", flag2))
	assert.False(t, IsEnabledTestFlag1(ctx))
	assert.True(t, IsEnabledTestFlag2(ctx))
}

func IsEnabledTestFlag1(ctx context.Context) bool {
	return flagIsEnabled(ctx, flag1)
}

func IsEnabledTestFlag2(ctx context.Context) bool {
	return flagIsEnabled(ctx, flag2)
}
