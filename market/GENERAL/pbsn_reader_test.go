package pbsn

import (
	"bytes"
	"io"
	"testing"
	"testing/iotest"

	pb "a.yandex-team.ru/market/idx/golibrary/pbsn/test_proto"
	"github.com/golang/protobuf/proto"
	"github.com/golang/snappy"
	"github.com/stretchr/testify/assert"
)

const (
	magic          = "TEST"
	incorrectMagic = "FEST"
)

func makeChunk(b []byte) []byte {
	encoded := snappy.Encode([]byte{}, b)
	length := sizeToBytes(len(encoded))
	return append(length[:], encoded...)
}

func makeSnappyString(chunks ...[]byte) []byte {
	totalLen := SnapMagicLen
	for _, s := range chunks {
		totalLen += len(s)
	}
	tmp := make([]byte, totalLen)
	i := copy(tmp, SnapMagic)
	for _, s := range chunks {
		i += copy(tmp[i:], s)
	}
	return tmp
}

func TestSnappyReader_Reset(t *testing.T) {
	snappyReader := NewSnappyReader(bytes.NewReader([]byte{}))
	assert.NoError(t, snappyReader.err)
	assert.EqualValues(t, false, snappyReader.headerWasRead)

	_, err := snappyReader.Read([]byte{})
	assert.Error(t, err)
	assert.Error(t, snappyReader.err)
	assert.False(t, snappyReader.headerWasRead)

	snappyReader.Reset(bytes.NewReader(makeSnappyString(makeChunk([]byte{0}))))
	assert.NoError(t, snappyReader.err)
	assert.False(t, snappyReader.headerWasRead)

	_, err = snappyReader.Read([]byte{})
	assert.NoError(t, err)
	assert.NoError(t, snappyReader.err)
	assert.True(t, snappyReader.headerWasRead)

	snappyReader.Reset(bytes.NewReader([]byte{}))
	assert.NoError(t, snappyReader.err)
	assert.False(t, snappyReader.headerWasRead)
}

func TestSnappyReader_Read(t *testing.T) {
	t.Parallel()

	testCases := [][]byte{
		{},
		{0},
		{255, 0},
		{},
		{'D', 'E', 'A', 'D', 'B', 'E', 'E', 'F'},
	}

	validate := func(t *testing.T, reader *SnappyReader, expected []byte) {
		t.Helper()
		buf := make([]byte, len(expected))
		_, err := io.ReadFull(reader, buf)
		assert.NoError(t, err, "Unexpected read error")
		assert.Equal(t, expected, buf, "Encode/decode error.")
		n, err := reader.Read(buf)
		assert.Equal(t, 0, n)
		assert.Equal(t, err, io.EOF)
	}

	t.Run("Empty", func(t *testing.T) {
		t.Run("Empty source", func(t *testing.T) {
			snappyReader := NewSnappyReader(bytes.NewReader([]byte{}))
			_, err := snappyReader.Read([]byte{})
			assert.Equal(t, io.EOF, err)
		})

		t.Run("Initialized source", func(t *testing.T) {
			snappyReader := NewSnappyReader(bytes.NewReader(makeSnappyString()))
			_, err := snappyReader.Read([]byte{})
			assert.Equal(t, io.EOF, err)
		})
	})

	t.Run("Correct", func(t *testing.T) {
		t.Run("Single buffers", func(t *testing.T) {
			snappyReader := NewSnappyReader(bytes.NewReader([]byte{}))
			for _, testCase := range testCases {
				snappyReader.Reset(bytes.NewReader(makeSnappyString(makeChunk(testCase))))
				validate(t, snappyReader, testCase)
			}
		})

		t.Run("Paired buffers", func(t *testing.T) {
			snappyReader := NewSnappyReader(nil)
			for i := 1; i < len(testCases); i++ {
				expected := append(testCases[i-1], testCases[i]...)
				snappyReader.Reset(
					bytes.NewReader(
						makeSnappyString(
							makeChunk(testCases[i-1]),
							makeChunk(testCases[i]))))
				validate(t, snappyReader, expected)
			}
		})
	})

	t.Run("Incorrect", func(t *testing.T) {
		t.Run("Wrong chunk length", func(t *testing.T) {
			deadBeef := testCases[len(testCases)-1]
			deadBeefLen := len(deadBeef)
			chunk := makeChunk(deadBeef)

			// corrupt chunk length
			chunk[1] += 20
			snappyReader := NewSnappyReader(bytes.NewReader(makeSnappyString(chunk)))
			buf := make([]byte, deadBeefLen)
			_, err := io.ReadFull(snappyReader, buf)
			assert.Equal(t, ErrTooSmall, err)
		})

		t.Run("Faulty reader", func(t *testing.T) {
			deadBeef := testCases[len(testCases)-1]
			deadBeefLen := len(deadBeef)
			chunk := makeChunk(deadBeef)
			snappyReader := NewSnappyReader(iotest.TimeoutReader(bytes.NewReader(makeSnappyString(chunk))))
			buf := make([]byte, deadBeefLen)
			_, err := io.ReadFull(snappyReader, buf)
			assert.Equal(t, iotest.ErrTimeout, err)
			_, err = snappyReader.Read(buf)
			assert.Equal(t, iotest.ErrTimeout, err)
		})

		t.Run("Wrong magic", func(t *testing.T) {
			deadBeef := testCases[len(testCases)-1]
			deadBeefLen := len(deadBeef)
			snappyReader := NewSnappyReader(bytes.NewReader(deadBeef))
			buf := make([]byte, deadBeefLen)
			_, err := io.ReadFull(snappyReader, buf)
			assert.IsType(t, &MagicMismatchError{}, err)
		})
	})
}

func TestReader_CheckMagicAndParseProto(t *testing.T) {
	t.Parallel()

	addMagic := func(b []byte) []byte {
		length := sizeToBytes(len(b))
		return append([]byte(magic), append(length[:], b...)...)
	}

	t.Run("Empty", func(t *testing.T) {
		message := &pb.TTestMessage{}

		t.Run("Empty source", func(t *testing.T) {
			reader := NewReader(bytes.NewReader([]byte{}))
			err := reader.CheckMagicAndParseProto(magic, message)
			assert.Equal(t, io.EOF, err)
		})
		t.Run("Initialized source", func(t *testing.T) {
			reader := NewReader(bytes.NewReader(makeSnappyString()))
			err := reader.CheckMagicAndParseProto(magic, message)
			assert.Equal(t, io.EOF, err)
		})
	})

	t.Run("Correct", func(t *testing.T) {
		t.Run("Simple", func(t *testing.T) {
			original := &pb.TTestMessage{
				First:  proto.String("Sergey"),
				Second: proto.Int64(13),
				Third:  proto.String("Tarmashev"),
			}

			marshalledMessage, err := proto.Marshal(original)
			assert.NoError(t, err)

			snappyString := makeSnappyString(makeChunk(addMagic(marshalledMessage)))

			reader := NewReader(bytes.NewReader(snappyString))
			decoded := &pb.TTestMessage{}

			err = reader.CheckMagicAndParseProto(magic, decoded)

			assert.NoError(t, err)
			assert.EqualValues(t, original.First, decoded.First)
			assert.EqualValues(t, original.Second, decoded.Second)
			assert.EqualValues(t, original.Third, decoded.Third)
		})

		t.Run("Message in two chunks", func(t *testing.T) {
			original := &pb.TTestMessage{
				First:  proto.String("Ivan"),
				Second: proto.Int64(4),
				Third:  proto.String("Grozniy"),
			}

			marshalledMessage, err := proto.Marshal(original)
			assert.NoError(t, err)

			marshalledMessageWithMagic := addMagic(marshalledMessage)

			firstChunk := makeChunk(marshalledMessageWithMagic[:15])
			secondChunk := makeChunk(marshalledMessageWithMagic[15:])

			snappyString := makeSnappyString(firstChunk, secondChunk)

			reader := NewReader(bytes.NewReader(snappyString))
			decoded := &pb.TTestMessage{}

			err = reader.CheckMagicAndParseProto(magic, decoded)

			assert.NoError(t, err)
			assert.EqualValues(t, original.First, decoded.First)
			assert.EqualValues(t, original.Second, decoded.Second)
			assert.EqualValues(t, original.Third, decoded.Third)
		})

		t.Run("Too big length is ok", func(t *testing.T) {
			bstr := string(make([]byte, maxSnappyEncodedLenOfMaxBlockSize))
			message := &pb.TTestMessage{
				First: &bstr,
			}

			marshalledMessage, err := proto.Marshal(message)
			assert.NoError(t, err)

			snappyString := makeSnappyString(makeChunk(addMagic(marshalledMessage)))

			reader := NewReader(bytes.NewReader(snappyString))
			decoded := &pb.TTestMessage{}
			err = reader.CheckMagicAndParseProto(magic, decoded)

			assert.NoError(t, err)
		})
	})

	t.Run("Incorrect", func(t *testing.T) {
		t.Run("Wrong magic", func(t *testing.T) {
			message := &pb.TTestMessage{
				First:  proto.String("Time"),
				Second: proto.Int64(2),
				Third:  proto.String("Travel"),
			}

			marshalledMessage, err := proto.Marshal(message)
			assert.NoError(t, err)

			snappyString := makeSnappyString(makeChunk(addMagic(marshalledMessage)))

			reader := NewReader(bytes.NewReader(snappyString))
			decoded := &pb.TTestMessage{}

			err = reader.CheckMagicAndParseProto(incorrectMagic, decoded)

			assert.Equal(t, NewMagicMismatchError(incorrectMagic, magic), err)
		})

		t.Run("Wrong message length", func(t *testing.T) {
			message := &pb.TTestMessage{
				First:  proto.String("Va"),
				Second: proto.Int64(11),
				Third:  proto.String("Hall-A"),
			}

			marshalledMessage, err := proto.Marshal(message)
			assert.NoError(t, err)

			messageWithMagic := addMagic(marshalledMessage)

			// corrupt chunk length
			messageWithMagic[5] += 20
			snappyString := makeSnappyString(makeChunk(messageWithMagic))

			reader := NewReader(bytes.NewReader(snappyString))
			decoded := &pb.TTestMessage{}

			err = reader.CheckMagicAndParseProto(magic, decoded)

			assert.Equal(t, io.ErrUnexpectedEOF, err)
		})
	})
}
