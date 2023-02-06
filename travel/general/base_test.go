package base

import (
	"bytes"
	"errors"
	"io"
	"testing"
)

type testCase struct {
	name    string
	payload []byte
	result  []byte
	err     error
}

var testCases = []testCase{
	{"empty iterator", make([]byte, 0), nil, ErrStopIteration},
	{
		"iterator with value",
		[]byte{8, 0, 0, 0, 102, 111, 111, 98, 97, 114, 52, 50},
		[]byte{102, 111, 111, 98, 97, 114, 52, 50},
		nil},
	{
		"broken number of bytes",
		[]byte{8, 0, 0, 0},
		nil,
		io.EOF},
}

func TestBytesIterator_Next(t *testing.T) {
	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			iterator, err := BuildIteratorFromBytes(tc.payload)
			if err != nil {
				t.Errorf("Error while creating iterator from bytes %+v", err)
			}

			res, err := iterator.Next()
			if !bytes.Equal(res, tc.result) {
				t.Errorf("Result should be %v, but %v found", tc.result, res)
			}
			if !errors.Is(err, tc.err) {
				t.Errorf("Error should be %v but %v found", tc.err, err)
			}
		})
	}
}

func TestBytesIterator_Populate(t *testing.T) {
	iterator, err := BuildIteratorFromBytes([]byte{
		8, 0, 0, 0, 102, 111, 111, 98, 97, 114, 52, 50, 4, 0, 0, 0, 102, 111, 111, 98})
	if err != nil {
		t.Errorf("Error while creating iterator from bytes %+v", err)
	}

	var buffer bytes.Buffer
	err = iterator.Populate(&buffer)
	if err != nil {
		t.Errorf("Error while populating buffer %+v", err)
	}
	expected := []byte{102, 111, 111, 98, 97, 114, 52, 50, 102, 111, 111, 98}
	if !bytes.Equal(buffer.Bytes(), expected) {
		t.Errorf("Expecting %s but got %s", expected, buffer.Bytes())
	}
}

func TestBytesGenerator_Write(t *testing.T) {
	var buffer bytes.Buffer
	g, err := BuildGeneratorForWriter(&buffer)
	if err != nil {
		t.Errorf("Error while creating generator %+v", err)
	}
	data := [][]byte{
		{101, 102, 103, 104, 105, 106},
		{101, 102, 103, 104},
		{},
	}
	for _, d := range data {
		err = g.Write(d)
		if err != nil {
			t.Errorf("Error while writing %+v", err)
		}
	}
	expected := []byte{6, 0, 0, 0, 101, 102, 103, 104, 105, 106, 4, 0, 0, 0, 101, 102, 103, 104, 0, 0, 0, 0}
	if !bytes.Equal(buffer.Bytes(), expected) {
		t.Errorf("Expecting %b but got %b", expected, buffer.Bytes())
	}
	expectedHashSum := []byte{104, 117, 42, 122, 210, 131, 141, 104, 175, 73, 128, 152, 153, 67, 65, 97}
	if !bytes.Equal(g.HashSum(), expectedHashSum) {
		t.Errorf("Expecting %b but got %b", expectedHashSum, g.HashSum())
	}
}
