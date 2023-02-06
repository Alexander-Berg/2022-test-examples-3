package tarifficator

import (
	"bytes"
	"testing"

	"github.com/stretchr/testify/assert"
)

const tariffRevisionMeta = `
{
  "meta": {
    "revision": {
      "hash": "1BC29B36F623BA82AAF6724FD3B16718",
      "timestamp": 1560765105
    },
    "data": [
      {
        "tariff_id": 100000000101,
        "hash": "1BC29B36F623BA82AAF6724FD3B16001",
        "timestamp": 1560765001
      },
      {
        "tariff_id": 100000000102,
        "hash": "1BC29B36F623BA82AAF6724FD3B16002",
        "timestamp": 1560765002
      },
      {
        "tariff_id": 100000000103,
        "hash": "1BC29B36F623BA82AAF6724FD3B16003",
        "timestamp": 1560765001
      }
    ]
  }
}
`

func TestGetFileNamesFromMeta(t *testing.T) {
	// given: prepare tariff revision meta from json
	metaBuffer := bytes.NewBufferString(tariffRevisionMeta)

	// when:
	actualFileNames, err := getFileNamesFromMeta(metaBuffer)
	assert.NoError(t, err)

	// then:
	expectedFileNames := []string{"100000000101", "100000000102", "100000000103"}
	assert.Equal(t, expectedFileNames, actualFileNames)
}
