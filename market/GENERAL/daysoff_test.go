package daysoff

import (
	"sort"
	"testing"
	"time"

	"github.com/stretchr/testify/require"
)

func makeDD(a map[int]bool) DisabledDatesMap {
	dd := make(DisabledDatesMap)
	for k, v := range a {
		dd[k] = DisabledDate{IsHoliday: v}
	}
	return dd
}

func TestNewDaysOff(t *testing.T) {
	{
		daysOff := NewHolidayDaysOff([]string{})
		require.Nil(t, daysOff)
	}
	{
		dates := []string{
			"2021-05-05",
			"2021-05-06",
			"2021-05-09",
			"2021-05-10",
			"2021-05-11",
			"2021-05-12",
		}
		wantDaysOff := DisabledDatesMap{
			125: DisabledDate{IsHoliday: true},
			126: DisabledDate{IsHoliday: true},
			129: DisabledDate{IsHoliday: true},
			130: DisabledDate{IsHoliday: true},
			131: DisabledDate{IsHoliday: true},
			132: DisabledDate{IsHoliday: true},
		}
		daysOff := NewHolidayDaysOff(dates)
		require.Equal(t, wantDaysOff, daysOff)
	}
}

func TestNewDetailedDaysOff(t *testing.T) {
	{
		daysOff := NewDetailedDaysOff([]detailedDayOffYT{})
		require.Nil(t, daysOff)
	}
	{
		dates := []detailedDayOffYT{
			{
				"2021-05-05",
				"2021-07-05T17:28:47.416798",
			},
			{
				"2021-05-06",
				"2021-07-05T18:28:47.416798",
			},
			{
				"2021-05-09",
				"2021-07-05T19:28:47.416798",
			},
			{
				"2021-05-10",
				"2021-07-05T20:28:47.416798",
			},
			{
				"2021-05-11",
				"2021-07-05T21:28:47.416798",
			},
			{
				"2021-05-12",
				"MinTime",
			},
		}
		wantDaysOff := DisabledDatesMap{
			125: NewDisabledDate(time.Date(2021, 07, 05, 17, 28, 47, 416798000, time.UTC)),
			126: NewDisabledDate(time.Date(2021, 07, 05, 18, 28, 47, 416798000, time.UTC)),
			129: NewDisabledDate(time.Date(2021, 07, 05, 19, 28, 47, 416798000, time.UTC)),
			130: NewDisabledDate(time.Date(2021, 07, 05, 20, 28, 47, 416798000, time.UTC)),
			131: NewDisabledDate(time.Date(2021, 07, 05, 21, 28, 47, 416798000, time.UTC)),
			132: DisabledDate{},
		}
		daysOff := NewDetailedDaysOff(dates)
		require.Equal(t, wantDaysOff, daysOff)
	}
}

func TestAppendDates(t *testing.T) {
	// Оба источника пустые
	{
		wantDaysOff := NewServiceDaysOff()
		daysOff := AppendCopiesOfDates(nil, nil)
		require.Equal(t, wantDaysOff, daysOff)
	}
	// Источник из графа не пустой, пустые дейоффы
	{
		wantDaysOff := &ServiceDaysOff{
			Services: map[int64]DisabledDatesMap{
				2: {
					133: DisabledDate{},
					256: DisabledDate{},
				},
			},
		}
		holiday := &ServiceDaysOff{
			Services: map[int64]DisabledDatesMap{
				2: {
					133: DisabledDate{},
					256: DisabledDate{},
				},
			},
		}
		daysOff := AppendCopiesOfDates(nil, holiday)
		require.Equal(t, wantDaysOff, daysOff)
	}
	// Дейоффы не пустые, пустые даты из графа
	{
		wantDaysOff := &ServiceDaysOff{
			Services: map[int64]DisabledDatesMap{
				2: {
					133: DisabledDate{},
					256: DisabledDate{},
				},
			},
		}
		daysOff := &ServiceDaysOff{
			Services: map[int64]DisabledDatesMap{
				2: {
					133: DisabledDate{},
					256: DisabledDate{},
				},
			},
		}
		daysOff = AppendCopiesOfDates(daysOff, nil)
		require.Equal(t, wantDaysOff, daysOff)
	}
	// Общий случай с разынми сервисами
	{
		daysOff := &ServiceDaysOff{
			Services: map[int64]DisabledDatesMap{
				1: {
					125: DisabledDate{},
					126: DisabledDate{},
				},
			},
		}
		holiday := &ServiceDaysOff{
			Services: map[int64]DisabledDatesMap{
				2: {
					133: DisabledDate{},
					256: DisabledDate{},
				},
			},
		}

		wantDaysOff := &ServiceDaysOff{
			Services: map[int64]DisabledDatesMap{
				1: {
					125: DisabledDate{},
					126: DisabledDate{},
				},
				2: {
					133: DisabledDate{},
					256: DisabledDate{},
				},
			},
		}
		daysOff = AppendCopiesOfDates(daysOff, holiday)
		require.Equal(t, wantDaysOff, daysOff)
	}
	// Один сервис, одинаковые даты + Один сервис, разные даты(дополнение)
	{
		daysOff := &ServiceDaysOff{
			Services: map[int64]DisabledDatesMap{
				1: {
					125: DisabledDate{},
					126: DisabledDate{},
				},
				2: {
					256: DisabledDate{},
				},
			},
		}
		holiday := &ServiceDaysOff{
			Services: map[int64]DisabledDatesMap{
				1: {
					125: DisabledDate{},
					126: DisabledDate{},
				},
				2: {
					257: DisabledDate{},
				},
			},
		}

		wantDaysOff := &ServiceDaysOff{
			Services: map[int64]DisabledDatesMap{
				1: {
					125: DisabledDate{},
					126: DisabledDate{},
				},
				2: {
					256: DisabledDate{},
					257: DisabledDate{},
				},
			},
		}
		daysOff = AppendCopiesOfDates(daysOff, holiday)
		require.Equal(t, wantDaysOff, daysOff)
	}
}

func TestGroupDaysOffByHash(t *testing.T) {
	ddm := &ServiceDaysOff{
		Services: map[int64]DisabledDatesMap{
			555: {
				1: DisabledDate{
					created:   1638839722,
					IsHoliday: true,
				},
				2: DisabledDate{
					created:   1638839733,
					IsHoliday: false,
				},
				123: DisabledDate{
					created:   1638839733,
					IsHoliday: false,
				},
			},
			666: {
				1: DisabledDate{
					created:   1638839722,
					IsHoliday: true,
				},
				2: DisabledDate{
					created:   1638839733,
					IsHoliday: false,
				},
				123: DisabledDate{
					created:   1638839733,
					IsHoliday: false,
				},
			},
		},
	}
	result := GroupDaysOffByHash(ddm)
	want := ServicesHashed{
		Time: result.Time,
		DaysOffGrouped: map[int64]*DaysOffGrouped{
			555: {
				DaysOffMap: makeDD(map[int]bool{
					1:   true,
					2:   false,
					123: false,
				}),
				Index: 1,
			},
			666: {
				DaysOffMap: makeDD(map[int]bool{
					1:   true,
					2:   false,
					123: false,
				}),
				Index: 1,
			},
		},
	}
	require.Equal(t, want, result)
}

func TestNewDaysOffGrouped(t *testing.T) {
	ddm := map[int64]DisabledDatesMap{
		111: {
			1: DisabledDate{
				created:   1638839722,
				IsHoliday: true,
			},
			2: DisabledDate{
				created:   1638839733,
				IsHoliday: false,
			},
			123: DisabledDate{
				created:   1638839733,
				IsHoliday: false,
			},
		},
		222: {
			1: DisabledDate{
				created:   1638839722,
				IsHoliday: true,
			},
			2: DisabledDate{
				created:   1638839733,
				IsHoliday: false,
			},
			123: DisabledDate{
				created:   1638839733,
				IsHoliday: false,
			},
		},
		333: {
			1: DisabledDate{
				created:   1638839722,
				IsHoliday: true,
			},
			2: DisabledDate{
				created:   1638839733,
				IsHoliday: false,
			},
		},
		444: {
			1: DisabledDate{
				created:   1638839722,
				IsHoliday: true,
			},
			2: DisabledDate{
				created:   1638839733,
				IsHoliday: false,
			},
			123: DisabledDate{
				created:   1638839733,
				IsHoliday: true,
			},
		},
		555: {
			1: DisabledDate{
				created:   1638839722,
				IsHoliday: true,
			},
			2: DisabledDate{
				created:   1638839733,
				IsHoliday: false,
			},
		},
		666: {},
		777: {},
	}
	associatedSlice := make([]int64, len(ddm))
	i := 0
	for k := range ddm {
		associatedSlice[i] = k
		i++
	}
	sort.Slice(associatedSlice, func(i, j int) bool {
		return associatedSlice[i] < associatedSlice[j]
	})
	factory := newDaysOffFactory()
	result := ServicesHashed{
		DaysOffGrouped: make(map[int64]*DaysOffGrouped),
	}
	for _, k := range associatedSlice {
		result.DaysOffGrouped[k] = factory.newDaysOffGrouped(ddm[k])
	}

	want := ServicesHashed{
		DaysOffGrouped: map[int64]*DaysOffGrouped{
			111: {
				DaysOffMap: makeDD(map[int]bool{
					1:   true,
					2:   false,
					123: false,
				}),
				Index: 1,
			},
			222: {
				DaysOffMap: makeDD(map[int]bool{
					1:   true,
					2:   false,
					123: false,
				}),
				Index: 1,
			},
			333: {
				DaysOffMap: makeDD(map[int]bool{
					1: true,
					2: false,
				}),
				Index: 2,
			},
			444: {
				DaysOffMap: makeDD(map[int]bool{
					1:   true,
					2:   false,
					123: true,
				}),
				Index: 3,
			},
			555: {
				DaysOffMap: makeDD(map[int]bool{
					1: true,
					2: false,
				}),
				Index: 2,
			},
			666: {
				DaysOffMap: DisabledDatesMap{},
				Index:      4,
			},
			777: {
				DaysOffMap: DisabledDatesMap{},
				Index:      4,
			},
		},
	}

	require.Equal(t, want, result)
}

func TestDeepCopy(t *testing.T) {
	daysOff := ServiceDaysOff{
		Services: map[int64]DisabledDatesMap{
			1: {
				22: DisabledDate{created: 42},
			},
		},
	}

	deepCopy := daysOff.DeepCopy()
	require.Len(t, deepCopy.Services[1], 1)
	require.Equal(t, deepCopy.Services[1][22], daysOff.Services[1][22])

	deepCopy.Services[1][33] = DisabledDate{}
	require.Len(t, deepCopy.Services[1], 2)
	require.Len(t, daysOff.Services[1], 1)
}
