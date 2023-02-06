package tarifficator

import (
	"sort"
	"testing"

	"github.com/stretchr/testify/require"
)

func TestProgramType(t *testing.T) {
	programs0 := []string{"BERU_CROSSDOCK", "MARKET_DELIVERY"}
	ptl := NewProgramTypeList(programs0)

	programs := ptl.ProgramNames()
	sort.Strings(programs)
	require.Equal(t, programs0, programs)

	types := ptl.ProgramTypes()
	require.Len(t, types, 2)
	require.Contains(t, types, ProgramBeruCrossdock)
	require.Contains(t, types, ProgramMarketDelivery)
}

func TestProgramTypeBad(t *testing.T) {
	programs := []string{"foo"}
	ptl := NewProgramTypeList(programs)
	require.Empty(t, ptl.ProgramNames())
	require.Empty(t, ptl.ProgramTypes())
}
