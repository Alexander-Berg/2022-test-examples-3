package routes

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/enums"
	"a.yandex-team.ru/market/combinator/pkg/graph"
	"a.yandex-team.ru/market/combinator/pkg/its"
	"a.yandex-team.ru/market/combinator/pkg/settings"
)

// It is Ranger from Graph,
// but for StalkerProxy interface
type RangerStalker struct {
	pathsList []graph.SPaths
}

func (r *RangerStalker) GetGraph() *graph.Graph {
	return nil
}

func (r *RangerStalker) FindPaths(
	ctx context.Context,
	startTime time.Time,
	opts ...StalkerFindPathsOption,
) (*RoutesFound, error) {
	var res RoutesFound
	if len(r.pathsList) != 0 {
		res.Routes, r.pathsList = r.pathsList[0], r.pathsList[1:]
	}
	return &res, nil
}

func getDefaultSettings() its.Settings {
	settings, _ := its.NewStringSettingsHolder("{}")
	return settings.GetSettings()
}

func TestPathFinderAndFilterBad(t *testing.T) {
	paths := graph.SPaths{
		graph.MakePathForSortCenterServices(
			enums.PartnerTypeSortingCenter,
			time.Date(2020, 12, 1, 0, 0, 0, 0, time.UTC),
			time.Date(2020, 12, 1, 0, 0, 0, 0, time.UTC),
			time.Date(2020, 12, 1, 0, 0, 0, 0, time.UTC), // <-- 25 hour gap here
			time.Date(2020, 12, 2, 1, 0, 0, 0, time.UTC),
		),
		graph.MakePathForSortCenterServices(
			enums.PartnerTypeSortingCenter,
			time.Date(2020, 12, 1, 0, 0, 0, 0, time.UTC),
			time.Date(2020, 12, 1, 0, 0, 0, 0, time.UTC),
			time.Date(2020, 12, 1, 0, 0, 0, 0, time.UTC), // <-- 49 hour gap here
			time.Date(2020, 12, 3, 1, 0, 0, 0, time.UTC),
		),
		graph.MakePathForSortCenterServices(
			enums.PartnerTypeSortingCenter,
			time.Date(2020, 12, 1, 0, 0, 0, 0, time.UTC), // <-- 48 hour gap here
			time.Date(2020, 12, 3, 0, 0, 0, 0, time.UTC),
			time.Date(2020, 12, 3, 0, 0, 0, 0, time.UTC),
			time.Date(2020, 12, 3, 0, 0, 0, 0, time.UTC),
		),
		graph.MakePathForSortCenterServices(
			enums.PartnerTypeSortingCenter,
			time.Date(2020, 12, 1, 0, 0, 0, 0, time.UTC), // <-- 24 hour gap here
			time.Date(2020, 12, 2, 0, 0, 0, 0, time.UTC),
			time.Date(2020, 12, 2, 0, 0, 0, 0, time.UTC), // <-- 24 hour gap here
			time.Date(2020, 12, 3, 0, 0, 0, 0, time.UTC),
		),
	}
	finder := &PathFinderAndFilterBad{
		StalkerProxy: &RangerStalker{[]graph.SPaths{paths}},
	}
	defSet := settings.New(getDefaultSettings(), "")
	defSet.MaxHoursToStoreSortedBoxOnSC = 24
	ctx := settings.ContextWithSettings(context.Background(), defSet)
	res, err := graph.FindBestCourierPathsV2(ctx, finder, paths[0].EndTime, nil)
	require.NoError(t, err)
	// Now we keep all paths
	require.Len(t, res.SPaths, 1)

	// Disabled inbound-sort services check
	finder = &PathFinderAndFilterBad{
		StalkerProxy: &RangerStalker{[]graph.SPaths{paths}},
	}
	res, err = graph.FindBestCourierPaths(ctx, finder, paths[0].EndTime, nil)
	require.NoError(t, err)
	require.Len(t, res.SPaths, 1)
}

type Ranger858 struct {
	pathsList []graph.SPaths
}

func (r *Ranger858) GetGraph() *graph.Graph {
	return nil
}

func (r *Ranger858) FindPaths(
	ctx context.Context,
	startTime time.Time,
	opts ...StalkerFindPathsOption,
) (*RoutesFound, error) {
	if startTime.Year() != 1 {
		return nil, nil
	}
	var res RoutesFound
	if len(r.pathsList) != 0 {
		res.Routes, r.pathsList = r.pathsList[0], r.pathsList[1:]
	}
	return &res, nil
}

// COMBINATOR-858
func TestCombinator858(t *testing.T) {
	paths := graph.SPaths{
		graph.MakePathForSortCenterServices(
			enums.PartnerTypeSortingCenter,
			time.Date(2020, 12, 1, 0, 0, 0, 0, time.UTC),
			time.Date(2020, 12, 1, 0, 0, 0, 0, time.UTC),
			time.Date(2020, 12, 1, 0, 0, 0, 0, time.UTC),
			time.Date(2020, 12, 1, 1, 0, 0, 0, time.UTC),
		),
	}
	finder := &PathFinderAndFilterBad{
		StalkerProxy: &Ranger858{[]graph.SPaths{paths}},
	}
	defSet := settings.New(getDefaultSettings(), "")
	defSet.MaxHoursToStoreSortedBoxOnSC = 24
	ctx := settings.ContextWithSettings(context.Background(), defSet)
	res, err := graph.FindBestCourierPaths(ctx, finder, time.Now().Local(), nil)
	require.NoError(t, err)
	require.Len(t, res.SPaths, 0)
	require.Len(t, res.PathsWithIntervals, 0)
}
