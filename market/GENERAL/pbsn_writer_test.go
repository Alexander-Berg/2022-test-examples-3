package pbsn

import (
	"bytes"
	"github.com/golang/snappy"
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestSnappyWriter_Reset(t *testing.T) {
	buf := bytes.NewBuffer(make([]byte, 0))
	writer := NewSnappyWriter(buf)

	writer.Reset(buf)
	assert.Equal(t, []byte{'S', 'N', 'A', 'P', 'S', 'N', 'A', 'P'}, buf.Bytes())

	anotherBuf := bytes.NewBuffer(make([]byte, 0))
	writer.Reset(anotherBuf)
	assert.Equal(t, []byte{'S', 'N', 'A', 'P', 'S', 'N', 'A', 'P'}, buf.Bytes())
	assert.Equal(t, []byte{'S', 'N', 'A', 'P'}, anotherBuf.Bytes())
}

func TestSnappyWriter_Write(t *testing.T) {
	t.Parallel()

	init := func() (buf *bytes.Buffer, writer *SnappyWriter) {
		buf = bytes.NewBuffer(make([]byte, 0))
		writer = NewSnappyWriter(buf)
		return
	}

	t.Run("Empty", func(t *testing.T) {
		buf, _ := init()
		assert.Equal(t, []byte{'S', 'N', 'A', 'P'}, buf.Bytes())
	})

	t.Run("Simple write", func(t *testing.T) {
		data := []byte("Simple write")
		var encodedData []byte
		encodedData = snappy.Encode(encodedData, data)
		length := sizeToBytes(len(encodedData))
		buf, writer := init()
		expectedResult := append(append([]byte(SnapMagic), length[:]...), encodedData...)

		n, err := writer.Write(data)
		assert.NoError(t, err)
		assert.Equal(t, len(data), n)

		assert.Equal(t, expectedResult, buf.Bytes())
	})

	t.Run("Two writes", func(t *testing.T) {
		data := [][]byte{
			[]byte("First write"),
			[]byte("Second write"),
		}
		expectedResult := []byte(SnapMagic)
		var encodedData []byte
		buf, writer := init()

		for _, item := range data {
			encodedData = snappy.Encode(encodedData, item)
			length := sizeToBytes(len(encodedData))
			expectedResult = append(append(expectedResult, length[:]...), encodedData...)
			n, err := writer.Write(item)
			assert.NoError(t, err)
			assert.Equal(t, len(item), n)
		}

		assert.Equal(t, expectedResult, buf.Bytes())
	})
}
