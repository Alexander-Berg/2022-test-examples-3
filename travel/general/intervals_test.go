package dtutil

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestIntersect_InvalidDates(t *testing.T) {
	operating := NewStringDateInterval("2020-08-14", "2020-08-15")
	assert.Equal(t, []StringDateInterval{operating}, Intersect(operating, NewStringDateInterval("", "2020-10-15")))
	assert.Equal(t, []StringDateInterval{operating}, Intersect(operating, NewStringDateInterval("2020-10-14", "")))

	operating = NewStringDateInterval("", "2020-08-15")
	assert.Equal(t, []StringDateInterval{operating}, Intersect(operating, NewStringDateInterval("2020-10-14", "2020-10-15")))

	operating = NewStringDateInterval("2020-08-14", "")
	assert.Equal(t, []StringDateInterval{operating}, Intersect(operating, NewStringDateInterval("2020-10-14", "2020-10-15")))
}

func TestIntersect_NonIntersectingDates(t *testing.T) {
	operating := NewStringDateInterval("2020-08-14", "2020-08-24")
	assert.Equal(t, []StringDateInterval{operating}, Intersect(operating, NewStringDateInterval("2020-08-01", "2020-08-13")))
	assert.Equal(t, []StringDateInterval{operating}, Intersect(operating, NewStringDateInterval("2020-08-25", "2020-08-31")))
}

func TestIntersect_TouchingDates(t *testing.T) {
	operating := NewStringDateInterval("2020-08-14", "2020-08-24")
	assert.Equal(
		t,
		[]StringDateInterval{NewStringDateInterval("2020-08-15", "2020-08-24")},
		Intersect(operating, NewStringDateInterval("2020-08-01", "2020-08-14")),
	)
	assert.Equal(
		t,
		[]StringDateInterval{NewStringDateInterval("2020-08-15", "2020-08-24")},
		Intersect(operating, NewStringDateInterval("2020-08-14", "2020-08-14")),
	)
	assert.Equal(
		t,
		[]StringDateInterval{NewStringDateInterval("2020-08-14", "2020-08-23")},
		Intersect(operating, NewStringDateInterval("2020-08-24", "2020-08-31")),
	)
	assert.Equal(
		t,
		[]StringDateInterval{NewStringDateInterval("2020-08-14", "2020-08-23")},
		Intersect(operating, NewStringDateInterval("2020-08-24", "2020-08-24")),
	)
}

func TestIntersect_SideIntersectingDates(t *testing.T) {
	operating := NewStringDateInterval("2020-08-14", "2020-08-24")
	assert.Equal(
		t,
		[]StringDateInterval{NewStringDateInterval("2020-08-17", "2020-08-24")},
		Intersect(operating, NewStringDateInterval("2020-08-01", "2020-08-16")),
	)
	assert.Equal(
		t,
		[]StringDateInterval{NewStringDateInterval("2020-08-14", "2020-08-21")},
		Intersect(operating, NewStringDateInterval("2020-08-22", "2020-08-31")),
	)
}

func TestIntersect_AlmostCoveringDates(t *testing.T) {
	operating := NewStringDateInterval("2020-08-14", "2020-08-24")
	assert.Equal(
		t,
		[]StringDateInterval{NewStringDateInterval("2020-08-24", "2020-08-24")},
		Intersect(operating, NewStringDateInterval("2020-08-01", "2020-08-23")),
	)
	assert.Equal(
		t,
		[]StringDateInterval{NewStringDateInterval("2020-08-14", "2020-08-14")},
		Intersect(operating, NewStringDateInterval("2020-08-15", "2020-08-31")),
	)
}

func TestIntersect_FullyCoveringDates(t *testing.T) {
	operating := NewStringDateInterval("2020-08-14", "2020-08-24")
	assert.Equal(
		t,
		[]StringDateInterval{},
		Intersect(operating, NewStringDateInterval("2020-08-01", "2020-08-31")),
	)
	assert.Equal(
		t,
		[]StringDateInterval{},
		Intersect(operating, NewStringDateInterval("2020-08-14", "2020-08-24")),
	)
}

func TestIntersect_SplitDates(t *testing.T) {
	operating := NewStringDateInterval("2020-08-14", "2020-08-24")
	assert.Equal(
		t,
		[]StringDateInterval{
			NewStringDateInterval("2020-08-14", "2020-08-20"),
			NewStringDateInterval("2020-08-22", "2020-08-24"),
		},
		Intersect(operating, NewStringDateInterval("2020-08-21", "2020-08-21")),
	)
	assert.Equal(
		t,
		[]StringDateInterval{
			NewStringDateInterval("2020-08-14", "2020-08-19"),
			NewStringDateInterval("2020-08-23", "2020-08-24"),
		},
		Intersect(operating, NewStringDateInterval("2020-08-20", "2020-08-22")),
	)
	assert.Equal(
		t,
		[]StringDateInterval{
			NewStringDateInterval("2020-08-14", "2020-08-14"),
			NewStringDateInterval("2020-08-24", "2020-08-24"),
		},
		Intersect(operating, NewStringDateInterval("2020-08-15", "2020-08-23")),
	)
}
