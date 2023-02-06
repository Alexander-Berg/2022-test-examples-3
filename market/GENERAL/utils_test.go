package dispenser

import (
	"github.com/stretchr/testify/assert"
	"testing"
)

func Test_getDefaultHttpClient(t *testing.T) {
	client := getDefaultHTTPClient()
	assert.NotNil(t, client)
}
