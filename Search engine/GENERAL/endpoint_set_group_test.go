package esgroups

import (
	"errors"
	"fmt"
	"net"
	"os"
	"testing"
	"time"

	"github.com/jonboulle/clockwork"
	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/infra/yp_service_discovery/golang/resolver"
	"a.yandex-team.ru/search/go/iss"
)

var (
	clock          clockwork.FakeClock
	group          *EndpointSetGroup
	resolverClient *ResolverMock
)

func resetMock() error {
	for key := range resolverClient.Responses {
		delete(resolverClient.Responses, key)
	}

	resolverClient.Responses[&EndpointSetID{Cluster: "test", ID: "alpha"}] = &resolver.ResolveEndpointsResponse{
		EndpointSet: &resolver.EndpointSet{
			Endpoints: []*resolver.Endpoint{
				{
					ID:   "alpha",
					FQDN: "alpha.sas.yp-c.yandex.net",
					Port: 80,
					IPv6: net.IPv6zero,
				},
			},
		},
	}
	resolverClient.Responses[&EndpointSetID{Cluster: "test", ID: "bravo"}] = &resolver.ResolveEndpointsResponse{
		EndpointSet: &resolver.EndpointSet{
			Endpoints: []*resolver.Endpoint{
				{
					ID:   "bravo",
					FQDN: "bravo.vla.yp-c.yandex.net",
					Port: 80,
					IPv6: net.IPv6zero,
				},
			},
		},
	}

	return group.UpdateEndpoints()
}

func TestMain(m *testing.M) {
	resolverClient = NewResolverMock()
	clock = clockwork.NewFakeClockAt(time.Unix(0, 0))
	group = MakeEndpointSetGroup(
		resolverClient,
		&iss.GeoDetectorMock{Geo: "sas"},
		clock,

		&EndpointSetID{Cluster: "test", ID: "alpha"},
		&EndpointSetID{Cluster: "test", ID: "bravo"},
	)

	os.Exit(m.Run())
}

func TestEndpointSetGroup(t *testing.T) {
	assert.NoError(t, resetMock())
	t.Run("TestReResolve", testReResolve)

	assert.NoError(t, resetMock())
	t.Run("TestWeights", testWeights)
}

func testReResolve(t *testing.T) {
	i := group.PickInstanceOrPanic()
	assert.Equal(t, "alpha", i.Endpoint.ID)

	// Remove alpha from ES and replace it with charlie.
	for key := range resolverClient.Responses {
		delete(resolverClient.Responses, key)
	}
	resolverClient.Responses[&EndpointSetID{Cluster: "test", ID: "alpha"}] = &resolver.ResolveEndpointsResponse{
		EndpointSet: &resolver.EndpointSet{
			Endpoints: []*resolver.Endpoint{
				{
					ID:   "charlie",
					FQDN: "charlie.sas.yp-c.yandex.net",
					Port: 80,
					IPv6: net.IPv6zero,
				},
			},
		},
	}
	assert.NoError(t, group.UpdateEndpoints())

	i = group.PickInstanceOrPanic()
	assert.Equal(t, "charlie", i.Endpoint.ID)
}

func testWeights(t *testing.T) {
	// First request, no usages on any instances.
	i := group.PickInstanceOrPanic()
	// Alpha is used, because it's in the same DC as a client.
	assert.Equal(t, "alpha", i.Endpoint.ID)
	alpha := i

	// While alpha is healthy, bravo is not used.
	for k := 0; k < 100; k++ {
		clock.Advance(time.Second)

		i = group.PickInstanceOrPanic()
		assert.Equal(t, "alpha", i.Endpoint.ID)
		_ = i.Do(func(_ *resolver.Endpoint) error {
			return nil
		})
	}

	// Alpha's weight is decreasing with each fail, until it's lower than bravo's weight.
	// 3 fails should be enough to attempt XDC (fail count scales with instance count in closest DC).
	for k := 0; k < 3; k++ {
		clock.Advance(time.Second)

		i = group.PickInstanceOrPanic()
		assert.Equal(t, "alpha", i.Endpoint.ID)

		_ = i.Do(func(i *resolver.Endpoint) error {
			return errors.New("request to alpha failed")
		})
	}

	// Just checking if fail tracking works...
	successCount, failCount := 0, 0
	alpha.usages.Do(func(element interface{}) {
		if element != nil {
			usage := element.(*Usage)
			if usage.success {
				successCount++
			} else {
				failCount++
			}
		}
	})

	assert.Equal(t, 17, successCount)
	assert.Equal(t, 3, failCount)

	// After 3 alpha fails, bravo should be used for at least 50 seconds.
	for k := 0; k < 49; k++ {
		clock.Advance(time.Second)

		i = group.PickInstanceOrPanic()
		assert.Equal(t, "bravo", i.Endpoint.ID, fmt.Sprint(k, clock.Now().Unix()))
	}

	// But after 50 seconds alpha gets a second chance. And fails again.
	clock.Advance(time.Second)

	i = group.PickInstanceOrPanic()
	assert.Equal(t, "alpha", i.Endpoint.ID)
	_ = i.Do(func(i *resolver.Endpoint) error {
		return errors.New("request to alpha failed")
	})

	// Since it's still failing, Alpha won't get another chance for at least 67 more seconds.
	clock.Advance(66 * time.Second)
	i = group.PickInstanceOrPanic()
	assert.Equal(t, "bravo", i.Endpoint.ID)

	clock.Advance(time.Second)
	i = group.PickInstanceOrPanic()
	assert.Equal(t, "alpha", i.Endpoint.ID)

	// Once recovered, alpha gets a weight coefficient reset.
	assert.Equal(t, alpha.getWeightCoefficient(), float64(1))

	for k := 0; k < 3; k++ {
		i = group.PickInstanceOrPanic()
		assert.Equal(t, "alpha", i.Endpoint.ID)
		clock.Advance(time.Second)
	}
}
