package hardconfig

import (
	"testing"

	"github.com/stretchr/testify/require"
)

func TestDecodeAllowedCyclicRegionsList(t *testing.T) {
	allowedList, err := NewAllowedCyclicRegionsMap("allowed_cyclic_regions_test.yaml")
	require.NoError(t, err, "decode allowed_cyclic_regions_test err")
	require.NotNil(t, allowedList, "allowed_cyclic_regions_test is empty")

	needValues := AllowedCyclicRegionsMap{
		{
			MiddleRegion: 1,
			CyclicRegion: 2,
		}: struct{}{},
		{
			MiddleRegion: 3,
			CyclicRegion: 4,
		}: struct{}{},
	}

	require.Equal(t, needValues, allowedList)
}

func TestAllowedCyclicRegionsConfigs(t *testing.T) {
	allowed, err := NewAllowedCyclicRegionsMap("allowed_cyclic_regions_list.yaml")
	require.NoError(t, err, "ошибка при чтении конфигурации allowed_cyclic_regions_list.yaml")
	require.NotNil(t, allowed, "конфигурация allowed_cyclic_regions_list.yaml пуста")

	for key := range allowed {
		require.NotZero(t, key.MiddleRegion, "поле middle_region - обязательное и не может быть равно 0")
		require.NotZero(t, key.CyclicRegion, "поле cyclic_region - обязательное и не может быть равно 0")
	}

	allowed, err = NewAllowedCyclicRegionsMap("allowed_cyclic_regions_list_testing.yaml")
	require.NoError(t, err, "ошибка при чтении конфигурации allowed_cyclic_regions_list_testing.yaml")
	require.NotNil(t, allowed, "конфигурация allowed_cyclic_regions_list_testing.yaml пуста")

	for key := range allowed {
		require.NotZero(t, key.MiddleRegion, "поле middle_region - обязательное и не может быть равно 0")
		require.NotZero(t, key.CyclicRegion, "поле cyclic_region - обязательное и не может быть равно 0")
	}
}
