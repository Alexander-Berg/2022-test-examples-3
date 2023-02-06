package routes

import (
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/outlets"
	"a.yandex-team.ru/market/combinator/pkg/units"
)

func generateSortedOutlets() []outlets.Outlet {
	return []outlets.Outlet{
		outlets.Outlet{
			ID: 0,
			GpsCoords: units.GpsCoords{
				Latitude:  1.0,
				Longitude: 1.0,
			},
		},
		outlets.Outlet{
			ID: 1,
			GpsCoords: units.GpsCoords{
				Latitude:  1.0,
				Longitude: 1.0,
			},
		},
		outlets.Outlet{
			ID: 2,
			GpsCoords: units.GpsCoords{
				Latitude:  2.0,
				Longitude: 2.0,
			},
		},
		outlets.Outlet{
			ID: 3,
			GpsCoords: units.GpsCoords{
				Latitude:  3.0,
				Longitude: 3.0,
			},
		},
		outlets.Outlet{
			ID: 4,
			GpsCoords: units.GpsCoords{
				Latitude:  3.0,
				Longitude: 3.0,
			},
		},
	}
}

func generateSortedOutlets2() []outlets.Outlet {
	return []outlets.Outlet{
		outlets.Outlet{
			ID: 0,
			GpsCoords: units.GpsCoords{
				Latitude:  0.0,
				Longitude: 0.0,
			},
		},
		outlets.Outlet{
			ID: 1,
			GpsCoords: units.GpsCoords{
				Latitude:  1.0,
				Longitude: 1.0,
			},
		},
		outlets.Outlet{
			ID: 2,
			GpsCoords: units.GpsCoords{
				Latitude:  2.0,
				Longitude: 2.0,
			},
		},
		outlets.Outlet{
			ID: 3,
			GpsCoords: units.GpsCoords{
				Latitude:  3.0,
				Longitude: 3.0,
			},
		},
		outlets.Outlet{
			ID: 4,
			GpsCoords: units.GpsCoords{
				Latitude:  4.0,
				Longitude: 4.0,
			},
		},
		outlets.Outlet{
			ID: 5,
			GpsCoords: units.GpsCoords{
				Latitude:  5.0,
				Longitude: 5.0,
			},
		},
	}
}

func TestLowerBound(t *testing.T) {
	test := func(expected int, outlets []outlets.Outlet, lower float64) {
		require.Equal(t, expected, lowerBound(outlets, lower))
		require.Equal(t, expected, lowerBoundFast(outlets, lower))
	}
	type spec struct {
		expected int
		upper    float64
	}
	test(0, nil, 0.0)
	{
		outlets := generateSortedOutlets()
		specs := []spec{
			{0, 0.0},
			{0, 1.0},
			{2, 2.0},
			{3, 3.0},
			{len(outlets), 4.0},
		}
		for _, spec := range specs {
			test(spec.expected, outlets, spec.upper)
		}
	}
	{
		outlets := generateSortedOutlets2()
		specs := []spec{
			{0, -1.0},
			{0, 0.0},
			{1, 1.0},
			{2, 2.0},
			{3, 3.0},
			{4, 4.0},
			{5, 5.0},
			{len(outlets), 6.0},
		}
		for _, spec := range specs {
			test(spec.expected, outlets, spec.upper)
		}
	}
}

func TestUpperBound(t *testing.T) {
	test := func(expected int, outlets []outlets.Outlet, upper float64) {
		require.Equal(t, expected, upperBound(outlets, upper))
		require.Equal(t, expected, upperBoundFast(outlets, upper))
	}
	type spec struct {
		expected int
		upper    float64
	}
	test(0, nil, 0.0)
	{
		outlets := generateSortedOutlets()
		specs := []spec{
			{0, 0.0},
			{2, 1.0},
			{3, 2.0},
			{len(outlets), 3.0},
			{len(outlets), 4.0},
		}
		for _, spec := range specs {
			test(spec.expected, outlets, spec.upper)
		}
	}
	{
		outlets := generateSortedOutlets2()
		specs := []spec{
			{0, -1.0},
			{1, 0.0},
			{2, 1.0},
			{3, 2.0},
			{4, 3.0},
			{5, 4.0},
			{len(outlets), 5.0},
			{len(outlets), 6.0},
		}
		for _, spec := range specs {
			test(spec.expected, outlets, spec.upper)
		}
	}
}
