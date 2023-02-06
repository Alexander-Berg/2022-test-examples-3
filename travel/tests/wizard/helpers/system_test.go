package helpers

import (
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/travel/avia/wizard/pkg/wizard/helpers"
)

func TestIsNil(t *testing.T) {
	type Value struct{}
	var (
		interfacePtr, valueInterface interface{}
		value                        *Value
	)

	valueInterface = (*Value)(nil)

	require.True(t, helpers.IsNil(interfacePtr))
	require.True(t, helpers.IsNil(valueInterface))
	require.True(t, helpers.IsNil(value))

	valueInterface = &Value{}
	value = &Value{}

	require.False(t, helpers.IsNil(valueInterface))
	require.False(t, helpers.IsNil(value))
}
