package tanker

import (
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestKeyset_GetPlural(t *testing.T) {

	testKeyset := Keyset{
		"hour": {
			Translations: map[string]*Translation{
				"ru": {
					Form:  "час",
					Form1: "часа",
					Form2: "часов",
				},
			},
		},
		"scissors": {
			Translations: map[string]*Translation{
				"ru": {
					Form:  "ножницы",
					Form1: "ножниц",
				},
			},
		},
		"coat": {
			Translations: map[string]*Translation{
				"ru": {
					Form: "пальто",
				},
			},
		},
	}

	type testData struct {
		key    string
		number int
		answer string
	}

	tests := []testData{
		{
			key:    "hour",
			number: 1,
			answer: "час",
		},
		{
			key:    "hour",
			number: 2,
			answer: "часа",
		},
		{
			key:    "hour",
			number: 5,
			answer: "часов",
		},
		{
			key:    "hour",
			number: 12,
			answer: "часов",
		},
		{
			key:    "hour",
			number: 0,
			answer: "часов",
		},
		{
			key:    "hour",
			number: 31,
			answer: "час",
		},
		{
			key:    "hour",
			number: 32,
			answer: "часа",
		},
		{
			key:    "hour",
			number: 111,
			answer: "часов",
		},
		{
			key:    "hour",
			number: 113,
			answer: "часов",
		},
		{
			key:    "hour",
			number: 1001,
			answer: "час",
		},
		{
			key:    "scissors",
			number: 1,
			answer: "ножницы",
		},
		{
			key:    "scissors",
			number: 2,
			answer: "ножниц",
		},
		{
			key:    "scissors",
			number: 5,
			answer: "ножниц",
		},
		{
			key:    "scissors",
			number: 12,
			answer: "ножниц",
		},
		{
			key:    "scissors",
			number: 0,
			answer: "ножниц",
		},
		{
			key:    "scissors",
			number: 31,
			answer: "ножницы",
		},
		{
			key:    "scissors",
			number: 32,
			answer: "ножниц",
		},
		{
			key:    "scissors",
			number: 111,
			answer: "ножниц",
		},
		{
			key:    "scissors",
			number: 1001,
			answer: "ножницы",
		},
		{
			key:    "coat",
			number: 1,
			answer: "пальто",
		},
		{
			key:    "coat",
			number: 2,
			answer: "пальто",
		},
		{
			key:    "coat",
			number: 5,
			answer: "пальто",
		},
		{
			key:    "coat",
			number: 12,
			answer: "пальто",
		},
		{
			key:    "coat",
			number: 0,
			answer: "пальто",
		},
	}

	for _, test := range tests {
		t.Run(
			fmt.Sprintf("test %v %v", test.number, test.key), func(t *testing.T) {
				result := testKeyset.GetPlural(test.key, "ru", test.number)
				assert.Equal(t, test.answer, result)
			},
		)
	}
}
