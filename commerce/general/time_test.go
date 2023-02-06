package goblogs

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

func Test_Time_UnmarshalJSON(t *testing.T) {
	result := new(Time)

	_ = result.UnmarshalJSON([]byte(`"2019-02-07T10:14:21.971Z"`))

	assert.Equal(t, time.Date(2019, 2, 7, 10, 14, 21, 971000000, time.UTC), result.Time)
}

func Test_Time_MarshalJSON(t *testing.T) {
	result := &Time{time.Date(2019, 2, 7, 10, 14, 21, 971000000, time.UTC)}

	b, _ := result.MarshalJSON()

	assert.Equal(t, []byte(`"2019-02-07T10:14:21.971Z"`), b)
}
