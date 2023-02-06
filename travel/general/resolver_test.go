package staticresolver

import (
	"errors"
	"testing"

	"github.com/stretchr/testify/assert"
	"google.golang.org/grpc/resolver"
)

type mockClientConn struct {
	resolver.ClientConn
}

func (t *mockClientConn) UpdateState(s resolver.State) error {
	return nil
}

func TestStaticResolver_EmptyTarget(t *testing.T) {
	t.Run(
		"empty string", func(t *testing.T) {
			target := resolver.Target{
				Endpoint: "",
			}
			resolver, err := NewStaticResolverBuilder().Build(target, &mockClientConn{}, resolver.BuildOptions{})
			assert.Error(t, err)
			assert.True(t, errors.As(err, &ServiceHasNoHostsError{}))
			assert.Nil(t, resolver)
		},
	)

	t.Run(
		"list of empty strings", func(t *testing.T) {
			target := resolver.Target{
				Endpoint: ",,",
			}
			resolver, err := NewStaticResolverBuilder().Build(target, &mockClientConn{}, resolver.BuildOptions{})
			assert.Error(t, err)
			assert.True(t, errors.As(err, &ServiceHasNoHostsError{}))
			assert.Nil(t, resolver)
		},
	)
}

func TestStaticResolver_NormalTarget(t *testing.T) {
	target := resolver.Target{
		Endpoint: "host1:9001,host2:9001",
	}
	resolver, err := NewStaticResolverBuilder().Build(target, &mockClientConn{}, resolver.BuildOptions{})
	assert.NoError(t, err)
	assert.Equal(t, []string{"host1:9001", "host2:9001"}, resolver.(*staticResolver).addresses)
}
