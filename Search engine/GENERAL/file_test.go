package spravkas

import (
	"fmt"
	"os"
	"strings"
	"testing"
)

func TestOpen(t *testing.T) {
	_, err := Open("testdata/does-not-exist")
	if !os.IsNotExist(err) {
		t.Errorf("unexpected error: %s", err)
	}

	f, err := Open("testdata/spravkas.txt")
	if err != nil {
		t.Fatal(err)
	}
	f.Close()
}

func TestFile_Read(t *testing.T) {
	f, err := Open("testdata/spravkas.txt")
	if err != nil {
		t.Fatal(err)
	}
	defer f.Close()

	want := []string{
		"  one",
		"  two",
		"three",
	}

	for i, s := range want {
		sp, err := f.Read()
		if err != nil {
			t.Fatalf("%d: %s", i, err)
		}
		if got := fmt.Sprint(sp); got != s {
			t.Errorf("%d: want %q, got %q", i, s, got)
		}
	}

	_, err = f.Read()
	if err == nil {
		t.Error("got nil error")
	}
}

func TestFile_Seek(t *testing.T) {
	f, err := Open("testdata/spravkas.txt")
	if err != nil {
		t.Fatal(err)
	}
	defer f.Close()

	prev := "  one\n  two\n"
	want := "three"

	_, _ = f.Seek(int64(len(prev)), 0)

	sp, err := f.Read()
	if err != nil {
		t.Fatal(err)
	}
	if s := fmt.Sprint(sp); s != want {
		t.Errorf("want %q, got %q", want, s)
	}
}

var CapacityTests = []struct {
	S string
	N int64
}{
	{
		"one\ntwo\r\nthree\n",
		3,
	},
}

func TestCapacity(t *testing.T) {
	for _, tt := range CapacityTests {
		t.Run(fmt.Sprintf("%q", tt.S), func(t *testing.T) {
			n, err := Capacity(strings.NewReader(tt.S))
			if err != nil {
				t.Fatal(err)
			}
			if n != tt.N {
				t.Errorf("want %v, got %v", tt.N, n)
			}
		})
	}
}
