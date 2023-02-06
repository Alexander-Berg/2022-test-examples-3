package telegram

import (
	"bytes"
	"io/ioutil"
	"testing"

	"github.com/pkg/errors"
	. "gopkg.in/check.v1"
)

func Test(t *testing.T) { TestingT(t) }

type TestSuite struct{}

var (
	_ = Suite(&TestSuite{})
)

func (s *TestSuite) TestDecodeUpdates(c *C) {
	b, err := ioutil.ReadFile("testdata/updates_result.json")
	c.Assert(err, IsNil)

	r := bytes.NewReader(b)
	_, err = decodeUpdates(r)
	c.Assert(err, IsNil)
}

func (s *TestSuite) TestRemoveBotSecret(c *C) {
	a := "getUpdates: Get https://api.telegram.org/bot123:s3c_R-t/getUpdates: context canceled"
	b := "getUpdates: Get https://api.telegram.org/bot123:***/getUpdates: context canceled"
	c.Assert(removeBotSecret(errors.New(a)).Error(), Equals, b)
}
