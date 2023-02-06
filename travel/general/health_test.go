package app

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	pb "a.yandex-team.ru/travel/buses/backend/proto"
)

func TestAppHealth(t *testing.T) {

	app, appClose, err := NewTestApp(t, nil)
	if !assert.NoError(t, err) {
		return
	}
	defer appClose()

	t.Run("Status check", func(t *testing.T) {

		assert.Eventually(t, func() bool {
			if app.HealthStatus().Status == pb.EStatus_STATUS_OK {
				return true
			}
			assert.Equal(t, pb.EStatus_STATUS_EXTERNAL_ERROR.String(), app.HealthStatus().Status.String(), app.HealthStatus().Message)
			return false
		}, 10*time.Second, time.Millisecond)

		_ = app.workerServiceConnection.Close()
		assert.Equal(t, pb.EStatus_STATUS_EXTERNAL_ERROR.String(), app.HealthStatus().Status.String(), app.HealthStatus().Message)
	})
}
