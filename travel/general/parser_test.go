package searchcontext

import (
	"testing"
	"time"

	"github.com/stretchr/testify/require"
)

func TestParseQueryID(t *testing.T) {
	testCases := []struct {
		name        string
		rawQID      string
		expected    QID
		expectedErr bool
	}{
		{
			name:   "one way",
			rawQID: "210520-100129-988.wizard.plane.c10493_c10429_2021-05-22_None_economy_1_0_0_ru.ru",
			expected: QID{
				QID:       "210520-100129-988.wizard.plane.c10493_c10429_2021-05-22_None_economy_1_0_0_ru.ru",
				CreatedAt: time.Date(2021, 5, 20, 10, 1, 29, 988*1000*1000, time.UTC),
				Service:   "wizard",
				TCode:     "plane",
				QKey: QKey{
					PointFromKey:    "c10493",
					PointToKey:      "c10429",
					DateForward:     time.Date(2021, 5, 22, 0, 0, 0, 0, time.UTC),
					DateBackward:    time.Time{},
					Class:           "economy",
					Adults:          1,
					Children:        0,
					Infants:         0,
					NationalVersion: "ru",
				},
				Lang: "ru",
			},
		},
		{
			name:   "round trip",
			rawQID: "210520-100129-988.wizard.plane.c10493_c10429_2021-05-22_2021-06-15_economy_1_4_8_ru.ru",
			expected: QID{
				QID:       "210520-100129-988.wizard.plane.c10493_c10429_2021-05-22_2021-06-15_economy_1_4_8_ru.ru",
				CreatedAt: time.Date(2021, 5, 20, 10, 1, 29, 988*1000*1000, time.UTC),
				Service:   "wizard",
				TCode:     "plane",
				QKey: QKey{
					PointFromKey:    "c10493",
					PointToKey:      "c10429",
					DateForward:     time.Date(2021, 5, 22, 0, 0, 0, 0, time.UTC),
					DateBackward:    time.Date(2021, 6, 15, 0, 0, 0, 0, time.UTC),
					Class:           "economy",
					Adults:          1,
					Children:        4,
					Infants:         8,
					NationalVersion: "ru",
				},
				Lang: "ru",
			},
		},
		{
			name:        "corrupted",
			rawQID:      "210520-100129-988.wizard.plane.c10493_c10429_2021-05-22_2021-06-15_economy_1_4_8_ru",
			expected:    QID{},
			expectedErr: true,
		},
		{
			name:        "corrupted created_at",
			rawQID:      "210520100129988.wizard.plane.c10493_c10429_2021-05-22_2021-06-15_economy_1_4_8_ru",
			expected:    QID{},
			expectedErr: true,
		},
		{
			name:        "corrupted date",
			rawQID:      "210520-100129-988.wizard.plane.c10493_c10429_20210531_2021-06-15_economy_1_4_8_ru",
			expected:    QID{},
			expectedErr: true,
		},
	}

	for _, testCase := range testCases {
		t.Run(
			testCase.name, func(t *testing.T) {
				actual, err := ParseQID(testCase.rawQID)

				if testCase.expectedErr {
					require.Error(t, err)
				} else {
					require.NoError(t, err)
				}
				require.Equal(t, testCase.expected, actual)
			},
		)
	}
}
