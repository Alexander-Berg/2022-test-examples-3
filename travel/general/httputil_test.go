package httputil

import (
	"testing"
)

func TestUnescape(t *testing.T) {
	t.Run("nothing to unescape", func(t *testing.T) {
		if Unescape("qqq") != "qqq" {
			t.Errorf("Unexpected Unescape() value: %v", Unescape("qqq"))
		}
	})

	t.Run("normal unescape", func(t *testing.T) {
		if Unescape("%D0%A1%D0%A3") != "СУ" {
			t.Errorf("Unexpected Unescape() value: %v", Unescape("%D0%A1%D0%A3"))
		}
	})

	t.Run("unescape with error", func(t *testing.T) {
		if Unescape("%D%0%A%1") != "%D%0%A%1" {
			t.Errorf("Unexpected Unescape() value: %v", Unescape("%D%0%A%1"))
		}
	})
}
