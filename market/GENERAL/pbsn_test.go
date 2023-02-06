package pbsn

import (
	pb "a.yandex-team.ru/market/idx/golibrary/pbsn/test_proto"
	"bytes"
	"github.com/golang/protobuf/proto"
	"github.com/golang/snappy"
	"github.com/stretchr/testify/assert"
	"io"
	"testing"
)

func TestIsPBSNFormatError(t *testing.T) {
	assert.False(t, IsPBSNFormatError(nil))
	assert.False(t, IsPBSNFormatError(io.ErrNoProgress))
	assert.False(t, IsPBSNFormatError(bytes.ErrTooLarge))
	assert.False(t, IsPBSNFormatError(io.EOF))
	assert.True(t, IsPBSNFormatError(io.ErrUnexpectedEOF))
	assert.True(t, IsPBSNFormatError(&proto.RequiredNotSetError{}))
	assert.True(t, IsPBSNFormatError(&proto.ParseError{}))
	assert.True(t, IsPBSNFormatError(NewMagicMismatchError("Pineapple", "Orange")))
	assert.True(t, IsPBSNFormatError(snappy.ErrCorrupt))
	assert.True(t, IsPBSNFormatError(snappy.ErrTooLarge))
	assert.True(t, IsPBSNFormatError(snappy.ErrUnsupported))
}

func TestSnappyReaderWriterIntegration(t *testing.T) {
	data := []byte{1, 2, 3}
	buffer := &bytes.Buffer{}
	writer := NewSnappyWriter(buffer)
	n, err := writer.Write(data)
	assert.NoError(t, err)
	assert.Equal(t, len(data), n)

	reader := NewSnappyReader(buffer)
	buf := make([]byte, 10)
	n, err = reader.Read(buf)
	assert.NoError(t, err)
	assert.Equal(t, len(data), n)
	assert.Equal(t, data, buf[:len(data)])
}

func TestReaderWriterIntegration(t *testing.T) {
	data := []byte{'C', 'A', 'P', 'P', 'U', 'C', 'C', 'I', 'N', 'O'}
	buffer := &bytes.Buffer{}
	writer := NewWriter(buffer)
	n, err := writer.Write(data)
	assert.NoError(t, err)
	assert.Equal(t, len(data), n)

	assert.NoError(t, writer.Close())

	buf := make([]byte, 10)
	reader := NewReader(buffer)
	n, err = reader.Read(buf)
	assert.NoError(t, err)
	assert.Equal(t, len(data), n)
	assert.Equal(t, data, buf[:len(data)])
}

func TestReaderWriterProtoIntegration(t *testing.T) {
	const countMessages = 2
	messageCreate := func(id int64) *pb.TTestMessage {
		message := &pb.TTestMessage{
			First:  proto.String("Test"),
			Second: proto.Int64(id),
			Third:  proto.String("Data"),
		}
		return message
	}

	buffer := &bytes.Buffer{}
	writer := NewWriter(buffer)
	for i := int64(0); i < countMessages/2; i += 2 {
		assert.NoError(t, writer.WriteProto(magic, messageCreate(i), messageCreate(i+1)))
	}
	assert.NoError(t, writer.Close())

	reader := NewReader(buffer)
	for i := int64(0); i < countMessages; i += 2 {
		message := &pb.TTestMessage{}
		assert.NoError(t, reader.CheckMagicAndParseProto(magic, message))
		assert.Equal(t, i, *message.Second)
	}
}
