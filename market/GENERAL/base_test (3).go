package plugins

import (
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"os"
	"testing"
)

func TestSetUnusedEnvs(t *testing.T) {
	clearEnv := func() {
		_ = os.Unsetenv("TEST_TEST_1")
		_ = os.Unsetenv("TEST_TEST_2")
	}
	clearEnv()
	defer clearEnv()

	err := os.Setenv("TEST_TEST_1", "noTest")
	require.NoError(t, err)

	err = SetUnusedEnvs([]envValue{
		{"TEST_TEST_1", "test"},
		{"TEST_TEST_2", "test"},
	}, "")
	assert.NoError(t, err)
	assert.Equal(t, "noTest", os.Getenv("TEST_TEST_1"))
	assert.Equal(t, "test", os.Getenv("TEST_TEST_2"))
}
