package collectors

import (
	"context"
	"testing"

	"a.yandex-team.ru/travel/library/go/testutil"
)

func TestTravelHotelsOfferCacheLogCollector_checkOfferCacheClientID(t *testing.T) {
	clientID := "test-client"
	tests := []struct {
		name     string
		only     []string
		excluded []string
		want     bool
	}{
		{
			name: "empty restrictions",
			want: true,
		},
		{
			name: "in only",
			only: []string{"fail", clientID},
			want: true,
		},
		{
			name: "not in only",
			only: []string{"fail", "test-client2"},
			want: false,
		},
		{
			name:     "excluded",
			excluded: []string{"fail", clientID},
			want:     false,
		},
		{
			name:     "not excluded",
			excluded: []string{"fail", "test-client2"},
			want:     true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			logger := testutil.NewLogger(t)
			c := &TravelHotelsOfferCacheLogCollector{
				logger: logger,
				config: TravelHotelsOfferCacheLogCollectorConfig{
					OnlyClientIDs:     tt.only,
					ExcludedClientIDs: tt.excluded,
				},
			}
			if got := c.checkOfferCacheClientID(context.Background(), clientID); got != tt.want {
				t.Errorf("checkOfferCacheClientID() = %v, want %v", got, tt.want)
			}
		})
	}
}
