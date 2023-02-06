package telegram

import (
	"bytes"
	"io"
	"io/ioutil"
	"testing"

	"github.com/golang/protobuf/jsonpb"
	. "gopkg.in/check.v1"
)

func Test(t *testing.T) { TestingT(t) }

type UpdateSuite struct{}

var _ = Suite(&UpdateSuite{})

var UpdateJSONTests = []string{
	"testdata/update.channel_post.json",
	"testdata/update.message.json",
}

func (*UpdateSuite) TestJSON(c *C) {
	for _, name := range UpdateJSONTests {
		b, err := ioutil.ReadFile(name)
		c.Assert(err, IsNil)

		r := bytes.NewReader(b)

		var u Update
		err = jsonpb.Unmarshal(r, &u)
		c.Assert(err, IsNil)

		r.Seek(0, io.SeekStart)
		err = (&jsonpb.Unmarshaler{
			AllowUnknownFields: true,
		}).Unmarshal(r, &u)
		c.Assert(err, IsNil)
	}
}
