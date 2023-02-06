package helpers

import (
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestNormalize(t *testing.T) {
	for sourceString, expected := range map[string]string{
		"nümber":          "number",
		"number i\u0307":  "number i",
		"number \u0456":   "number i",
		"number  №\"SU\"": "number su",
	} {
		assert.Equal(t, expected, Normalize(sourceString), fmt.Sprintf("source string: \"%s\"", sourceString))
	}
}
