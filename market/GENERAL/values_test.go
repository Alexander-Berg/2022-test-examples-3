package values

import (
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestConvertToJSONNETAppendable(t *testing.T) {
	testCases := []struct {
		name   string
		input  string
		output string
	}{
		{
			"basic",
			"{}",
			"{}",
		},
		{
			"simple",
			"{\"d\":{\"c\":[]}}",
			"{\"d\"+:{\"c\"+:[]}}",
		},
		{
			"complex",
			"{\":d\\\"\":{\":c\\\":\":[\"\\\\\\\":\",\"\\\\\"]}}",
			"{\":d\\\"\"+:{\":c\\\":\"+:[\"\\\\\\\":\",\"\\\\\"]}}",
		},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			assert.Equal(t, tc.output, convertToJSONNETAppendable(tc.input))
		})
	}
}
