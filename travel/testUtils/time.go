package testutils

import (
	"fmt"
	"time"
)

func ParseTime(value string) time.Time {
	t, err := time.Parse(time.RFC3339, value)
	if err == nil {
		return t
	}
	t, err = time.Parse("2006-01-02T15:04:05", value)
	if err == nil {
		return t
	}
	t, err = time.Parse("2006-01-02T15", value)
	if err == nil {
		return t
	}
	t, err = time.Parse("2006-01-02", value)
	if err == nil {
		return t
	}
	panic(fmt.Sprintf("unable to parse time %s", value))
}
