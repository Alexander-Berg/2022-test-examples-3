package common

import (
	"a.yandex-team.ru/mail/iex/matchers"
	"testing"
)

func TestLogoCfgGetUnfilledMandatoryFields_lacks5fields_gives2fieldnames(t *testing.T) {
	cfg := LogoCfg{}
	matchers.AssertThat(t, cfg.GetUnfilledMandatoryFields(), matchers.ElementsAre{
		"logo_color_default",
	})
}

func TestLogoCfgGetUnfilledMandatoryFields_lacks1field_gives1fieldname(t *testing.T) {
	cfg := LogoCfg{
		LogoIconRegular: "c",
		LogoIconBright:  "d",
	}
	matchers.AssertThat(t, cfg.GetUnfilledMandatoryFields(), matchers.ElementsAre{"logo_color_default"})
}

func TestLogoCfgGetUnfilledMandatoryFields_lacksNofields_givesNofieldnames(t *testing.T) {
	cfg := LogoCfg{
		LogoIconRegular:  "c",
		LogoIconBright:   "d",
		LogoColorDefault: "e",
	}
	matchers.AssertThat(t, len(cfg.GetUnfilledMandatoryFields()), matchers.Is{V: 0})
}

func TestLogoColorCfg_noColorInCfg_givesDefault(t *testing.T) {
	cfg := LogoCfg{LogoColorDefault: "default"}
	l := Logo{cfg, "key", "defaultName"}
	matchers.AssertThat(t, LogoColorCfg(l), matchers.Is{V: "default"})
}

func TestLogoColorCfg_emptyColorInCfg_givesDefault(t *testing.T) {
	cfg := LogoCfg{LogoColorDefault: "default", LogoColor: map[string]string{"key": ""}}
	l := Logo{cfg, "key", "defaultName"}
	matchers.AssertThat(t, LogoColorCfg(l), matchers.Is{V: "default"})
}

func TestLogoColorCfg_hasColorInCfg_givesFromCfg(t *testing.T) {
	cfg := LogoCfg{LogoColorDefault: "default", LogoColor: map[string]string{"key": "color"}}
	l := Logo{cfg, "key", "defaultName"}
	matchers.AssertThat(t, LogoColorCfg(l), matchers.Is{V: "color"})
}

func TestLogoName_noNameInCfg_givesDefault(t *testing.T) {
	cfg := LogoCfg{}
	l := Logo{cfg, "key", "default"}
	matchers.AssertThat(t, LogoName(l), matchers.Is{V: "default"})
}

func TestLogoName_emptyNameInCfg_givesDefault(t *testing.T) {
	cfg := LogoCfg{LogoName: map[string]string{"key": ""}}
	l := Logo{cfg, "key", "default"}
	matchers.AssertThat(t, LogoName(l), matchers.Is{V: "default"})
}

func TestLogoName_hasNameInCfg_givesFromCfg(t *testing.T) {
	cfg := LogoCfg{LogoName: map[string]string{"key": "color"}}
	l := Logo{cfg, "key", "default"}
	matchers.AssertThat(t, LogoName(l), matchers.Is{V: "color"})
}
