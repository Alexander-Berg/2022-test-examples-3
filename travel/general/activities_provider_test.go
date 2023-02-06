package async

import (
	"reflect"
	"testing"

	apimodels "a.yandex-team.ru/travel/komod/trips/internal/components/api/trips/models"
)

func Test_zipActivities(t *testing.T) {
	tests := []struct {
		name   string
		chunks [][]apimodels.Activity
		want   []apimodels.Activity
	}{
		{
			name: "left chunk is empty",
			chunks: [][]apimodels.Activity{
				generateActivityMocks(1, 2, 3),
				generateActivityMocks(),
			},
			want: generateActivityMocks(1, 2, 3),
		},
		{
			name: "right chunk is empty",
			chunks: [][]apimodels.Activity{
				generateActivityMocks(),
				generateActivityMocks(1, 2, 3),
			},
			want: generateActivityMocks(1, 2, 3),
		},
		{
			name: "two chunks",
			chunks: [][]apimodels.Activity{
				generateActivityMocks(1, 5, 9),
				generateActivityMocks(10, 11, 3),
			},
			want: generateActivityMocks(1, 10, 5, 11, 9, 3),
		},
		{
			name: "three chunks",
			chunks: [][]apimodels.Activity{
				generateActivityMocks(1, 5, 9),
				generateActivityMocks(10, 11, 3),
				generateActivityMocks(16, 12, 4),
			},
			want: generateActivityMocks(1, 10, 16, 5, 11, 12, 9, 3, 4),
		},
		{
			name: "different lenght",
			chunks: [][]apimodels.Activity{
				generateActivityMocks(1, 5, 9, 100),
				generateActivityMocks(10, 11),
				generateActivityMocks(16, 12, 4),
			},
			want: generateActivityMocks(1, 10, 16, 5, 11, 12, 9, 4, 100),
		},
		{
			name: "with repeated numbers",
			chunks: [][]apimodels.Activity{
				generateActivityMocks(1, 2),
				generateActivityMocks(1, 3),
				generateActivityMocks(1, 2),
			},
			want: generateActivityMocks(1, 1, 1, 2, 3, 2),
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := zipActivities(tt.chunks...); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("zipActivities() = %v, want %v", got, tt.want)
			}
		})
	}
}

type activityMock struct {
	apimodels.Activity
	id int
}

func (a activityMock) isActivity() {}

func newActivityMock(id int) activityMock {
	return activityMock{id: id}
}

func generateActivityMocks(ids ...int) []apimodels.Activity {
	result := make([]apimodels.Activity, 0)
	for _, id := range ids {
		result = append(result, newActivityMock(id))
	}
	return result
}
