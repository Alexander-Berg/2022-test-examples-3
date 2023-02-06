package ytutil

import (
	"testing"

	"github.com/stretchr/testify/require"
)

func TestGeneration(t *testing.T) {
	{
		dirPath := "//tmp/foo/20200626_120000"
		g, err := NewGeneration(dirPath)
		require.NoError(t, err)
		require.Equal(t, "foo", g.Service)
		require.Equal(t, "20200626_120000", g.Version)
		require.Equal(t, dirPath, g.DirPath)
		_, err = g.GetTime()
		require.NoError(t, err)
	}
	{
		dirPath := "//tmp"
		_, err := NewGeneration(dirPath)
		require.Error(t, err)
	}
}
