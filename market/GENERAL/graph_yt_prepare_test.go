package graph

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

func TestNeedUpdateByTime(t *testing.T) {
	tests := []struct {
		modTime string
		nowTime string
		delta   time.Duration
		want    bool
		err     bool
	}{
		{
			modTime: "2020-05-21T08:00:00.123456Z",
			nowTime: "2020-05-21T09:00:01.123456Z",
			delta:   time.Duration(time.Hour),
			want:    true,
			err:     false,
		},
		{
			modTime: "2020-05-21T08:00:00.123456Z",
			nowTime: "2020-05-21T08:00:01.123456Z",
			delta:   time.Duration(time.Hour),
			want:    false,
			err:     false,
		},
		{
			modTime: "bad",
			nowTime: "2020-05-21T08:00:01.123456Z",
			delta:   time.Duration(time.Hour),
			want:    false,
			err:     true,
		},
	}
	for _, tt := range tests {
		nowTime, _ := time.Parse(time.RFC3339, tt.nowTime)
		need, err := needUpdateByTime(tt.modTime, nowTime, tt.delta)
		assert.Equal(t, tt.want, need)
		assert.Equal(t, tt.err, err != nil)
	}
}
