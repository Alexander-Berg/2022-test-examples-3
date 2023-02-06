package daysoff

import (
	"testing"

	"github.com/stretchr/testify/require"
)

func TestArchive(t *testing.T) {
	hashedSlice := []ServicesHashed{
		GroupDaysOffByHash(&ServiceDaysOff{
			Services: map[int64]DisabledDatesMap{
				111: {
					1: DisabledDate{
						created:   1638839722,
						IsHoliday: true,
					},
				},
			},
		}),
		GroupDaysOffByHash(&ServiceDaysOff{
			Services: map[int64]DisabledDatesMap{
				222: {
					2: DisabledDate{
						created:   1638839722,
						IsHoliday: true,
					},
				},
			},
		}),
		GroupDaysOffByHash(&ServiceDaysOff{
			Services: map[int64]DisabledDatesMap{
				333: {
					3: DisabledDate{
						created:   1638839722,
						IsHoliday: true,
					},
				},
			},
		}),
		GroupDaysOffByHash(&ServiceDaysOff{
			Services: map[int64]DisabledDatesMap{
				444: {
					4: DisabledDate{
						created:   1638839722,
						IsHoliday: true,
					},
				},
			},
		}),
		GroupDaysOffByHash(&ServiceDaysOff{
			Services: map[int64]DisabledDatesMap{
				555: {
					5: DisabledDate{
						created:   1638839722,
						IsHoliday: true,
					},
				},
			},
		}),
	}

	// Архив имеет очередь
	// {hashedSlice[0]}
	a := RecreateArchiveDaysOffQueue(nil, hashedSlice[0])
	require.Equal(t, a.daysOff.Len(), 1)
	for e := a.GetArchive(); e != nil; e = e.Next() {
		require.Equal(t, hashedSlice[0], e.Value.(ServicesHashed))
	}

	// Архив имеет очередь
	// {hashedSlice[1] -> hashedSlice[0]}
	a = RecreateArchiveDaysOffQueue(a, hashedSlice[1])
	require.Equal(t, a.daysOff.Len(), 2)
	i := 1
	for e := a.GetArchive(); e != nil; e = e.Next() {
		require.Equal(t, hashedSlice[i], e.Value.(ServicesHashed))
		i--
	}

	// Архив имеет очередь
	// {hashedSlice[2] -> hashedSlice[1] -> hashedSlice[0]}
	a = RecreateArchiveDaysOffQueue(a, hashedSlice[2])
	require.Equal(t, a.daysOff.Len(), 3)
	i = 2
	for e := a.GetArchive(); e != nil; e = e.Next() {
		require.Equal(t, hashedSlice[i], e.Value.(ServicesHashed))
		i--
	}

	// Архив имеет очередь
	// {hashedSlice[3] -> hashedSlice[2] -> hashedSlice[1] -> hashedSlice[0]}
	a = RecreateArchiveDaysOffQueue(a, hashedSlice[3])
	require.Equal(t, a.daysOff.Len(), 4)
	i = 3
	for e := a.GetArchive(); e != nil; e = e.Next() {
		require.Equal(t, hashedSlice[i], e.Value.(ServicesHashed))
		i--
	}

	// Архив имеет очередь
	// {hashedSlice[4] -> hashedSlice[3] -> hashedSlice[2] -> hashedSlice[1] -> hashedSlice[0]}
	a = RecreateArchiveDaysOffQueue(a, hashedSlice[4])
	require.Equal(t, a.daysOff.Len(), 5)
	i = 4
	for e := a.GetArchive(); e != nil; e = e.Next() {
		require.Equal(t, hashedSlice[i], e.Value.(ServicesHashed))
		i--
	}

	// Архив имеет очередь
	// {hashedSlice[0] -> hashedSlice[4] -> hashedSlice[3] -> hashedSlice[2] -> hashedSlice[1]}
	a = RecreateArchiveDaysOffQueue(a, hashedSlice[0])
	require.Equal(t, a.daysOff.Len(), 5)
	i = 0
	for e := a.GetArchive(); e != nil; e = e.Next() {
		if i == 0 {
			require.Equal(t, hashedSlice[0], e.Value.(ServicesHashed))
			i = 4
			continue
		}
		require.Equal(t, hashedSlice[i], e.Value.(ServicesHashed))
		i--
	}

	// Архив имеет очередь
	// {hashedSlice[1] -> hashedSlice[0] -> hashedSlice[4] -> hashedSlice[3] -> hashedSlice[2]}
	a = RecreateArchiveDaysOffQueue(a, hashedSlice[1])
	require.Equal(t, a.daysOff.Len(), 5)
	i = 1
	for e := a.GetArchive(); e != nil; e = e.Next() {
		if i == 0 {
			require.Equal(t, hashedSlice[0], e.Value.(ServicesHashed))
			i = 4
			continue
		}
		require.Equal(t, hashedSlice[i], e.Value.(ServicesHashed))
		i--
	}
}
