package errcheck

import (
	"bytes"
	"fmt"
	"os"
)

func testfprintf() {
	f, err := os.Create("/tmp/blah")
	if err != nil {
		panic(err)
	}
	buf := bytes.Buffer{}
	fmt.Fprintln(f, "blah") // want `unhandled error`
	fmt.Fprintln(os.Stderr, "blah")
	fmt.Fprintln(&buf, "blah")
}
