package helpers

import (
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/travel/avia/wizard/pkg/wizard/helpers"
)

func TestCutOnWords(t *testing.T) {
	t.Run("digits", checkWords(t, "123", "123"))
	t.Run("one word", checkWords(t, "word", "word"))
	t.Run("with space", checkWords(t, "some words", "some", "words"))
	t.Run("two spaces", checkWords(t, "one two three", "one", "two", "three"))
	t.Run("with dot", checkWords(t, "one.two.three", "one", "two", "three"))
	t.Run("complex", checkWords(t, "some words(complex)", "some", "words", "complex", ""))
	t.Run("strange dashes", checkWords(t, "some-words—complex", "some", "words", "complex"))
}

func checkWords(t *testing.T, text string, resultWords ...string) func(t *testing.T) {
	return func(t *testing.T) {
		words := helpers.CutOnWords(text)
		assert.Equal(t, resultWords, words)
	}
}

func TestIsDigit(t *testing.T) {
	assert.True(t, helpers.IsDigit("1410"), "1410")
	assert.True(t, helpers.IsDigit("0"), "zero is digit")
	assert.True(t, helpers.IsDigit("01"), "start from zero")

	assert.False(t, helpers.IsDigit("-12"), "negative contains minus")
	assert.False(t, helpers.IsDigit(""), "empty not contains digit")
	assert.False(t, helpers.IsDigit("text"), "text")
}
