package geobase

import (
	"testing"

	"github.com/stretchr/testify/require"
)

func TestNewExample(t *testing.T) {
	regionMap := NewExample()

	moscowPath := regionMap[RegionMoscow]
	require.Len(t, moscowPath, 4)
	require.Equal(t, moscowPath[0].Name, "Москва")
	require.Equal(t, moscowPath[3].ID, RegionID(225))

	kotelnikiPath := regionMap[21651]
	require.Len(t, kotelnikiPath, 5)
	require.Equal(t, kotelnikiPath[0].Name, "Котельники")
	require.Equal(t, kotelnikiPath[4].ID, RegionID(225))
}
