package cron

import (
	"testing"
)

func TestParse(t *testing.T) {
	validCrons := []string{
		"", "*", "* *", "* * *", "* * * *", "* * * * *", "* * * * * *",
		"0", "0-59/10", "0-59/1",
		// Check all full ranges
		"0-59", "* 0-59", "* * 0-23", "* * * 1-31", "* * * * 1-12",
		"* * * * * 0-6",
	}
	invalidCrons := []string{
		"* * * * * * *", "qwerty", "***", "/", "-", "0-60", "0-*", "*-10",
		"1-10/*", "1-10/0",
		// Check all full ranges
		"61", "* 61", "* * 24", "* * * 0", "* * * 32", "* * * * 0",
		"* * * * 13", "* * * * * 7",
		"0--59",
	}
	for _, cron := range validCrons {
		if _, err := Parse(cron); err != nil {
			t.Fatalf("Error for expression %q: %v", cron, err)
		}
	}
	for _, cron := range invalidCrons {
		if _, err := Parse(cron); err == nil {
			t.Fatalf("Expected error for expression %q", cron)
		}
	}
}
