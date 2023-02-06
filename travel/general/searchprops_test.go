package searchprops

import (
	"context"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestEmptyContext(t *testing.T) {
	assert.Nil(t, GetMap(context.Background()))
}

func TestSetIntoEmptyContext(t *testing.T) {
	ctx := context.Background()
	Set(ctx, "test", "example")
	assert.Nil(t, GetMap(ctx))
}

func TestNilInContext(t *testing.T) {
	ctx := context.WithValue(context.Background(), &searchPropsCtxKey, nil)
	Set(ctx, "test", "example")
	assert.Nil(t, GetMap(ctx))
}

func TestSearchProps_Set(t *testing.T) {
	ctx := With(context.Background())
	Set(ctx, "int", 1)
	Set(ctx, "string", "value")

	expected := map[string]interface{}{
		"int":    1,
		"string": "value",
	}
	assert.Equal(t, expected, GetMap(ctx))
}
