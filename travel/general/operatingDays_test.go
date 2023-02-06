package dtutil

import (
	"testing"
	"time"
)

func Test(t *testing.T) {
	tests := []struct {
		name        string
		inputFromDB OperatingDays
		dayToCheck  time.Weekday
		expect      bool
	}{
		{
			name:        "operates all week check monday",
			inputFromDB: 1234567,
			dayToCheck:  time.Monday,
			expect:      true,
		},
		{
			name:        "operates all week check tuesday (messed up order)",
			inputFromDB: 1726453,
			dayToCheck:  time.Tuesday,
			expect:      true,
		},
		{
			name:        "operates on wednesday, check friday",
			inputFromDB: 3,
			dayToCheck:  time.Friday,
			expect:      false,
		},
		{
			name:        "operates on sunday, check sunday",
			inputFromDB: 7,
			dayToCheck:  time.Sunday,
			expect:      true,
		},
		{
			name:        "operates on sunday, check monday",
			inputFromDB: 7,
			dayToCheck:  time.Monday,
			expect:      false,
		},
		{
			name:        "operates on sunday, monday, check monday",
			inputFromDB: 71,
			dayToCheck:  time.Monday,
			expect:      true,
		},
		{
			name:        "0 is not a sunday",
			inputFromDB: 103,
			dayToCheck:  time.Sunday,
			expect:      false,
		},
		{
			name:        "8 and 9 are illegal not monday or tuesday",
			inputFromDB: 89,
			dayToCheck:  time.Monday,
			expect:      false,
		},
		{
			name:        "8 and 9 are illegal not monday or tuesday",
			inputFromDB: 89,
			dayToCheck:  time.Tuesday,
			expect:      false,
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got := test.inputFromDB.OperatesOn(test.dayToCheck)
			if got != test.expect {
				t.Errorf(
					"Incorrect answer for input:%+v check:%+v. got %+v, expected %+v",
					test.inputFromDB,
					test.dayToCheck,
					got,
					test.expect,
				)
			}
		})
	}
}

func Test_AddDays(t *testing.T) {
	tests := []struct {
		name     string
		input    OperatingDays
		dayToAdd time.Weekday
		expected OperatingDays
	}{
		{
			name:     "add day to an empty day set",
			input:    0,
			dayToAdd: time.Monday,
			expected: 1,
		},
		{
			name:     "add Sunday to an empty day set",
			input:    0,
			dayToAdd: time.Sunday,
			expected: 7,
		},
		{
			name:     "add day to a non empty day set",
			input:    126,
			dayToAdd: time.Thursday,
			expected: 1246,
		},
		{
			name:     "add Sunday to a non empty day set",
			input:    126,
			dayToAdd: time.Sunday,
			expected: 1267,
		},
		{
			name:     "add already existing day to a day set",
			input:    126,
			dayToAdd: time.Tuesday,
			expected: 126,
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got := test.input.AddDay(test.dayToAdd)
			if got != test.expected {
				t.Errorf(
					"Incorrect answer for input:%+v add:%+v. got %+v, expected %+v",
					test.input,
					test.dayToAdd,
					got,
					test.expected,
				)
			}
		})
	}
}
