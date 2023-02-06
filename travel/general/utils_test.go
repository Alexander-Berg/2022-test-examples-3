package utils

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

func TestUtils(t *testing.T) {

	t.Run("Time converter", func(t *testing.T) {
		locationMsk, err := time.LoadLocation("Europe/Moscow")
		if !assert.NoError(t, err) {
			return
		}
		locationYekat, err := time.LoadLocation("Asia/Yekaterinburg")
		if !assert.NoError(t, err) {
			return
		}

		tm := time.Date(2000, 1, 1, 12, 0, 0, 0, locationMsk)
		if !assert.Equal(t, int64(2*60*60),
			ConvertTimeToRideSeconds(tm.In(locationYekat))-ConvertTimeToRideSeconds(tm.In(locationMsk))) {
			return
		}

		if !assert.Equal(t, tm, ConvertRideSecondsToTime(ConvertTimeToRideSeconds(tm), locationMsk)) {
			return
		}
	})
}
