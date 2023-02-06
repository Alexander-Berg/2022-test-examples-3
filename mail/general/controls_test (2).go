package bounce

import (
	"a.yandex-team.ru/mail/iex/taksa/request"
	"a.yandex-team.ru/mail/iex/taksa/tanker"
	"testing"
)

import . "a.yandex-team.ru/mail/iex/matchers"
import . "a.yandex-team.ru/mail/iex/taksa/widgets/common"

func TestGetDmarcTitle_noOriginalFrom_givesError(t *testing.T) {
	class := Class{Tanker: tanker.Mock{}}
	bounce := Bounce{IexDict{"original_from": ""}}
	_, e := class.getDmarcTitle(bounce)
	AssertThat(t, e, Is{V: Not{V: nil}})
}

func TestGetDmarcTitle_hasOriginalFrom_givesTitleWithOF(t *testing.T) {
	class := Class{Tanker: tanker.Mock{}}
	bounce := Bounce{IexDict{"original_from": "of"}}
	c, e := class.getDmarcTitle(bounce)
	AssertThat(t, e, Is{V: nil})
	AssertThat(t, c.Attributes["label"], Is{V: "dmarc from of"})
}

func TestGetFaqLink_maskInCfg_expandsMask(t *testing.T) {
	class := Class{
		Cfg:     Config{FaqLinkMask: `yandex.%v/faq`},
		Request: request.RequestMock{Tld: "tld"},
		Tanker:  tanker.Mock{},
	}
	c, _ := class.getFaqLink()
	AssertThat(t, c.Attributes["value"], Is{V: "yandex.tld/faq"})
}

func TestGetComposeAction_noOriginalMid_givesError(t *testing.T) {
	class := Class{Tanker: tanker.Mock{}}
	bounce := Bounce{IexDict{"original_mid": ""}}
	_, e := class.getComposeAction(bounce)
	AssertThat(t, e, Is{V: Not{V: nil}})
}

func TestGetComposeAction_hasOriginalMid_givesActionWithMid(t *testing.T) {
	class := Class{Tanker: tanker.Mock{}}
	bounce := Bounce{IexDict{"original_mid": "mid"}}
	c, e := class.getComposeAction(bounce)
	AssertThat(t, e, Is{V: nil})
	AssertThat(t, c.Attributes["mid"], Is{V: "mid"})
	AssertThat(t, c.Attributes["fix_rcpt"], Is{V: false})
}

func TestGetComposeAction_needsToFixRcpt_fixRcptIsTrue(t *testing.T) {
	class := Class{Tanker: tanker.Mock{}}
	bounce := Bounce{IexDict{"original_mid": "mid", "bounce_type": "1"}}
	c, _ := class.getComposeAction(bounce)
	AssertThat(t, c.Attributes["fix_rcpt"], Is{V: true})
}
