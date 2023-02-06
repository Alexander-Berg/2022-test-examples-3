package yav

import (
	"context"
	"github.com/heetch/confita/backend"
	"os"
	"testing"

	"github.com/stretchr/testify/assert"
)

type secretsResolverMock struct {
	secrets map[string]string
	secUID  string
}

func (r secretsResolverMock) GetSecretValue(secUID string, key string) (string, error) {
	if secUID != r.secUID {
		return "", nil
	}

	if val, ok := r.secrets[key]; ok {
		return val, nil
	}

	return "", nil
}

var defaultSecUIDConfig = SecUIDConfig{
	Development: "dev",
	Testing:     "testing",
	Production:  "production",
}

func TestGetValue(t *testing.T) {
	testCases := []struct {
		key   string
		value []byte
		error error
	}{
		{"TEST_KEY", nil, backend.ErrNotFound},
		{"EMPTY_KEY", nil, backend.ErrNotFound},
		{"ZAG_ZAG__KEY", []byte("lok_tar"), nil},
		{"zag-zag--key", []byte("lok_tar"), nil},
	}

	resolver := secretsResolverMock{secUID: "testing", secrets: map[string]string{
		"ZAG_ZAG__KEY": "lok_tar",
		"EMPTY_KEY":    "",
	}}

	_ = os.Setenv("YENV_TYPE", "testing")
	back := NewBackend(resolver, defaultSecUIDConfig)

	for _, tc := range testCases {
		t.Run(tc.key, func(t *testing.T) {
			value, err := back.Get(context.Background(), tc.key)
			assert.Equal(t, tc.error, err)
			assert.EqualValues(t, tc.value, value)
		})
	}

}

func TestMissingYENV(t *testing.T) {
	resolver := secretsResolverMock{secUID: "production", secrets: map[string]string{"TEST_KEY": "some value"}}
	_ = os.Setenv("YENV_TYPE", "testing")

	back := NewBackend(resolver, defaultSecUIDConfig)
	_, err := back.Get(context.Background(), "test-key")

	assert.Equal(t, backend.ErrNotFound, err)
}
