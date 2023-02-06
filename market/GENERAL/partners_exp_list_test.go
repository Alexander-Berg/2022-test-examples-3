package hardconfig

import (
	"testing"

	"github.com/stretchr/testify/require"
)

func TestPartnersExpListDecode(t *testing.T) {
	partnersExpList, err := NewPartnersExpList("partners_exp_list_test.yaml")
	require.NoError(t, err)
	require.NotNil(t, partnersExpList, "partners_exp_list_test is empty")

	require.Equal(t, PartnersExpList{
		"test":  map[int64]bool{10: true, 20: true, 30: true},
		"test2": map[int64]bool{40: true, 50: true, 60: true},
		"test3": map[int64]bool{70: true, 80: true, 90: true},
	}, partnersExpList)
}
