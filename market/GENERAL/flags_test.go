package flags

import (
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"os"
	"testing"
)

func TestFlagsParse(t *testing.T) {
	f := Init()

	cwd, err := os.Getwd()
	require.NoError(t, err)

	os.Args = []string{"test"}
	err = f.Parse()
	assert.NoError(t, err)
	assert.Equal(t, cwd, f.RootPath)

	os.Args = []string{"test", "--root-path", "/test_path"}
	err = f.Parse()
	assert.NoError(t, err)
	assert.Equal(t, "/test_path", f.RootPath)
}
