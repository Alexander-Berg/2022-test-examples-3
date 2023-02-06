package collectors

import (
	"testing"
)

type AuroraCheckinDeskTestCase struct {
	name     string
	input    string
	expected string
}

var testCases = []AuroraCheckinDeskTestCase{
	{
		name:     "simple",
		input:    "1,2,3,4",
		expected: "1-4",
	},
	{
		name:     "with_spaces",
		input:    "1, 2,  3,4",
		expected: "1-4",
	},
	{
		name:     "empty",
		input:    "    ",
		expected: "",
	},
	{
		name:     "multi_groups",
		input:    "1, 2,  3,4, 6, 7,  10,   12",
		expected: "1-4, 6-7, 10, 12",
	},
	{
		name:     "dash",
		input:    "-",
		expected: "",
	},
	{
		name:     "wrong_order",
		input:    "1, 2, 4, 3, 5",
		expected: "1-5",
	},
	{
		name:     "with_bracket",
		input:    "14(_), (_)",
		expected: "14",
	},
	{
		name:     "skip_one",
		input:    "33,35,36,",
		expected: "33, 35-36",
	},
	{
		name:     "interval",
		input:    "33-37",
		expected: "33-37",
	},
	{
		name:     "intervals",
		input:    "33-37, 39-43",
		expected: "33-37, 39-43",
	},
	{
		name:     "intervals with single desks",
		input:    "32, 33-37, 45, 39-43",
		expected: "32-37, 39-43, 45",
	},
}

func TestStorageAPI_TransformAuroraCheckinDesks(t *testing.T) {
	for _, testCase := range testCases {
		t.Run(testCase.name, func(t *testing.T) {
			result := transformAuroraCheckinDesks(testCase.input)
			if result != testCase.expected {
				t.Error("Expected", testCase.expected, ", got", result)
			}
		})
	}

}
