package tarifficator

import (
	"fmt"
	"sort"
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/geobase"
)

func TestIntersectTrees(t *testing.T) {
	fakePath := func(nums ...int) (ret geobase.RegionChain) {
		for _, num := range nums {
			ret = append(
				ret,
				geobase.Region{
					ID: geobase.RegionID(num),
				},
			)
		}
		return ret
	}

	linehaulTree := make(TariffRegionTree)
	linehaulTree.AddRegionPath(fakePath(10, 1))
	linehaulTree.AddRegionPath(fakePath(111, 11, 1))
	linehaulTree.AddRegionPath(fakePath(11, 1))
	linehaulTree.AddRegionPath(fakePath(121, 12, 1))
	linehaulTree.AddRegionPath(fakePath(131, 13, 1))

	tariffTree := make(TariffRegionTree)
	tariffTree.AddRegionPath(fakePath(111, 11, 1))
	tariffTree.AddRegionPath(fakePath(1111, 111, 11, 1))
	tariffTree.AddRegionPath(fakePath(1121, 112, 11, 1))
	tariffTree.AddRegionPath(fakePath(1122, 112, 11, 1))
	tariffTree.AddRegionPath(fakePath(121, 12, 1))
	tariffTree.AddRegionPath(fakePath(12111, 1211, 121, 12, 1))
	tariffTree.AddRegionPath(fakePath(1212, 121, 12, 1))
	tariffTree.AddRegionPath(fakePath(13, 1))

	intersection := make([]geobase.RegionID, 0)
	IntersectSubtrees(linehaulTree, tariffTree, linehaulTree[0], &intersection)
	sort.Slice(intersection, func(i, j int) bool {
		return intersection[i] < intersection[j]
	})
	expected := []geobase.RegionID{
		111, 1111, 1121, 1122, 121, 12111, 1212,
	}
	sort.Slice(expected, func(i, j int) bool {
		return expected[i] < expected[j]
	})
	fmt.Printf("actual   %+v\n", intersection)
	fmt.Printf("expected %+v\n", expected)
	require.Equal(t, expected, intersection)
}
