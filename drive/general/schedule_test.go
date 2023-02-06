package cron

import (
	"encoding"
	"encoding/json"
	"testing"
	"time"
)

var (
	_ encoding.TextMarshaler   = Schedule{}
	_ encoding.TextMarshaler   = &Schedule{}
	_ encoding.TextUnmarshaler = &Schedule{}
)

func TestSchedule_MarshalJSON(t *testing.T) {
	text := "1 2 3 4 5 6"
	schedule, err := Parse(text)
	if err != nil {
		t.Fatal("Error:", err)
	}
	expectedData, err := json.Marshal(text)
	if err != nil {
		t.Fatal("Error:", err)
	}
	resultData, err := json.Marshal(schedule)
	if err != nil {
		t.Fatal("Error:", err)
	}
	if string(expectedData) != string(resultData) {
		t.Fatalf(
			"Expected %q, found %q",
			string(expectedData), string(resultData),
		)
	}
}

func TestSchedule_UnmarshalJSON(t *testing.T) {
	text := "1 2 3 4 5 6"
	data, err := json.Marshal(text)
	if err != nil {
		t.Fatal("Error:", err)
	}
	var schedule Schedule
	if err := json.Unmarshal(data, &schedule); err != nil {
		t.Fatal("Error:", err)
	}
	if schedule.String() != text {
		t.Fatalf("Expected %q, found %q", text, schedule.String())
	}
}

func TestSchedule_UnmarshalJSON_Invalid(t *testing.T) {
	{
		text := "1 2 3 4 5 6 7"
		data, err := json.Marshal(text)
		if err != nil {
			t.Fatal("Error:", err)
		}
		var schedule Schedule
		if err := json.Unmarshal(data, &schedule); err == nil {
			t.Fatal("Expected error")
		}
	}
	{
		number := 123
		data, err := json.Marshal(number)
		if err != nil {
			t.Fatal("Error:", err)
		}
		var schedule Schedule
		if err := json.Unmarshal(data, &schedule); err == nil {
			t.Fatal("Expected error")
		}
	}
}

func TestSchedule_Next(t *testing.T) {
	tests := []struct {
		Cron string
		Time time.Time
		Next time.Time
	}{
		{
			Cron: "* * * * * *",
			Time: time.Date(2019, 1, 1, 0, 0, 0, 0, time.UTC),
			Next: time.Date(2019, 1, 1, 0, 0, 1, 0, time.UTC),
		},
		{
			Cron: "30 * * * * *",
			Time: time.Date(2019, 1, 1, 0, 0, 0, 0, time.UTC),
			Next: time.Date(2019, 1, 1, 0, 0, 30, 0, time.UTC),
		},
		{
			Cron: "30 * * * * *",
			Time: time.Date(2019, 1, 1, 0, 0, 30, 0, time.UTC),
			Next: time.Date(2019, 1, 1, 0, 1, 30, 0, time.UTC),
		},
		{
			Cron: "30 * * * * *",
			Time: time.Date(2019, 1, 1, 0, 0, 59, 0, time.UTC),
			Next: time.Date(2019, 1, 1, 0, 1, 30, 0, time.UTC),
		},
		{
			Cron: "0 30 * * * *",
			Time: time.Date(2019, 1, 1, 0, 0, 0, 0, time.UTC),
			Next: time.Date(2019, 1, 1, 0, 30, 0, 0, time.UTC),
		},
		{
			Cron: "0 30 * * * *",
			Time: time.Date(2019, 1, 1, 0, 30, 0, 0, time.UTC),
			Next: time.Date(2019, 1, 1, 1, 30, 0, 0, time.UTC),
		},
		{
			Cron: "0 30 * * * *",
			Time: time.Date(2019, 1, 1, 0, 59, 0, 0, time.UTC),
			Next: time.Date(2019, 1, 1, 1, 30, 0, 0, time.UTC),
		},
		{
			Cron: "0 0 12 * * *",
			Time: time.Date(2019, 1, 1, 0, 0, 0, 0, time.UTC),
			Next: time.Date(2019, 1, 1, 12, 0, 0, 0, time.UTC),
		},
		{
			Cron: "0 0 12 * * *",
			Time: time.Date(2019, 1, 1, 12, 0, 0, 0, time.UTC),
			Next: time.Date(2019, 1, 2, 12, 0, 0, 0, time.UTC),
		},
		{
			Cron: "0 0 0 15 * *",
			Time: time.Date(2019, 1, 1, 0, 0, 0, 0, time.UTC),
			Next: time.Date(2019, 1, 15, 0, 0, 0, 0, time.UTC),
		},
		{
			Cron: "0 0 0 15 * *",
			Time: time.Date(2019, 1, 15, 0, 0, 0, 0, time.UTC),
			Next: time.Date(2019, 2, 15, 0, 0, 0, 0, time.UTC),
		},
		{
			Cron: "0 0 0 15 6 *",
			Time: time.Date(2019, 1, 1, 0, 0, 0, 0, time.UTC),
			Next: time.Date(2019, 6, 15, 0, 0, 0, 0, time.UTC),
		},
		{
			Cron: "0 0 0 15 6 *",
			Time: time.Date(2019, 7, 1, 0, 0, 0, 0, time.UTC),
			Next: time.Date(2020, 6, 15, 0, 0, 0, 0, time.UTC),
		},
		{
			Cron: "0 0 0 * * 6",
			Time: time.Date(2021, 11, 15, 0, 0, 0, 0, time.UTC),
			Next: time.Date(2021, 11, 20, 0, 0, 0, 0, time.UTC),
		},
	}
	for _, test := range tests {
		schedule, err := Parse(test.Cron)
		if err != nil {
			t.Fatal("Error:", err)
		}
		if ts := schedule.Next(test.Time); !ts.Equal(test.Next) {
			t.Fatalf("Expected %s, got %s", test.Next, ts)
		}
	}
	// Empty Schedule does not have next time.
	if ts := (Schedule{}).Next(time.Unix(0, 0)); !ts.IsZero() {
		t.Fatalf("Expected zero time, got %s", ts)
	}
}
