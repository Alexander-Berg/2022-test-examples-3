package rollout

import (
	"testing"

	"github.com/brianvoe/gofakeit/v6"
	"github.com/stretchr/testify/require"
)

func TestRolloutService(t *testing.T) {
	t.Run(
		"IsEnabledForEmail", func(t *testing.T) {
			for percentage := uint64(0); percentage <= 100; percentage += 10 {
				service := NewService(Config{Percentage: percentage})

				enabledCount := float64(0)
				size := float64(100000)
				for i := float64(0); i < size; i++ {
					if service.IsEnabledForEmail(gofakeit.Email()) {
						enabledCount++
					}
				}

				require.InDelta(t, percentage, enabledCount/size*100, 1.5)
			}
		},
	)

	t.Run(
		"IsEnabledForEmail/disabled for all", func(t *testing.T) {
			service := NewService(Config{Percentage: 0})

			size := float64(100000)
			cornerCaseEmail := ""
			for i := float64(0); i < size; i++ {
				email := gofakeit.Email()
				bucket := service.getBucket(email)
				if bucket == 0 {
					cornerCaseEmail = email
					break
				}
			}

			require.False(t, service.IsEnabledForEmail(cornerCaseEmail))
		},
	)
}
