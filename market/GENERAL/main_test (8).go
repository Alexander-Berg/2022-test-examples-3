package main

import (
	"github.com/stretchr/testify/assert"
	"os"
	"testing"
)

func TestParseArgs(t *testing.T) {
	os.Args = []string{}

	_, _, err := parseArgs()
	assert.Error(t, err)

	os.Args = []string{"", "name"}
	name, args, err := parseArgs()
	assert.NoError(t, err)
	assert.Equal(t, "name", name)
	assert.Equal(t, []string{}, args)

	os.Args = []string{"", "name", "value"}
	name, args, err = parseArgs()
	assert.NoError(t, err)
	assert.Equal(t, "name", name)
	assert.Equal(t, []string{"value"}, args)
}
