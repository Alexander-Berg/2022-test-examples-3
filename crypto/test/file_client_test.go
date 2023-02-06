package rsserver_test

import (
	rsserver "a.yandex-team.ru/crypta/utils/rtmr_resource_service/bin/server/lib"
	"a.yandex-team.ru/library/go/test/yatest"
	"github.com/stretchr/testify/require"
	"io/ioutil"
	"os"
	"testing"
)

func ls(path string) ([]string, error) {
	files, err := ioutil.ReadDir(path)
	if err != nil {
		return nil, err
	}

	var result []string
	for _, fileInfo := range files {
		result = append(result, fileInfo.Name())
	}
	return result, nil
}

func TestFileClient(t *testing.T) {
	root := yatest.OutputPath("test")
	client := rsserver.NewFileClient(&rsserver.FileClientConfig{Root: root})

	const (
		resource1 = "resource1"
		resource2 = "resource2"
		resource3 = "resource3"
		version   = 123
	)

	needed := []string{resource2, resource3}
	require.NoError(t, client.Init(needed))

	actual, err := ls(root)
	require.NoError(t, err)
	require.Equal(t, actual, needed)

	needed = []string{resource1, resource2}
	require.NoError(t, client.Init(needed))

	actual, err = ls(root)
	require.NoError(t, err)
	require.Equal(t, actual, needed)

	require.False(t, client.IsPresent(resource1, version))

	filename := client.GetResourceFilename(resource1, version)
	_, err = os.Create(filename)
	require.NoError(t, err)
	require.True(t, client.IsPresent(resource1, version))
}
