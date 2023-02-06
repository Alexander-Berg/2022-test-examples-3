package featureflag

import (
	"testing"
	"time"

	"github.com/jonboulle/clockwork"
	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/library/go/containers"
)

type mockClient struct {
	Flags Flags
}

func (mockClient *mockClient) CreateFlags() (Flags, error) {
	return mockClient.Flags, nil
}

func TestUpdateFlags_ShouldUpdateFlagsPeriodically(t *testing.T) {
	flags := NewFlags(
		containers.SetOf("flag1"),
		containers.SetOf[string](),
	)
	featureFlagClient := &mockClient{flags}
	updateInterval := 100 * time.Millisecond
	clock := clockwork.NewRealClock()

	storage := NewStorage(featureFlagClient, updateInterval, logger, clock)
	storage.StartPeriodicUpdates()

	assert.True(t, storage.GetFlags().IsFlagEnabled("flag1"))

	newFlags := NewFlags(
		containers.SetOf("flag2"),
		containers.SetOf[string](),
	)
	featureFlagClient.Flags = newFlags
	clock.Sleep(updateInterval + updateInterval/2)

	assert.False(t, storage.GetFlags().IsFlagEnabled("flag1"))
	assert.True(t, storage.GetFlags().IsFlagEnabled("flag2"))
}
