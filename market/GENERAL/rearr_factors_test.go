package settings

import (
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/market/combinator/pkg/its"
)

const (
	ReportFlags = "market_enable_combinator=1;market_user_delivery_price=1;no_snippet_arc=0;ext_snippet=1"
)

func TestRearrFlagsParseInt(t *testing.T) {
	rp := NewRearrParser(ReportFlags)

	intVal, ok := rp.GetInt("market_enable_combinator")
	require.True(t, ok)
	require.Equal(t, 1, intVal)

	intVal, ok = rp.GetInt("market_user_delivery_price")
	require.True(t, ok)
	require.Equal(t, 1, intVal)

	intVal, ok = rp.GetInt("ext_snippet")
	require.True(t, ok)
	require.Equal(t, 1, intVal)

	intVal, ok = rp.GetInt("no_snippet_arc")
	require.True(t, ok)
	require.Equal(t, 0, intVal)

	_, ok = rp.GetInt("some_flag")
	require.False(t, ok)
}

func TestRearrFlagsParseBool(t *testing.T) {
	rp := NewRearrParser(ReportFlags)

	boolVal, ok := rp.GetBool("market_enable_combinator")
	require.True(t, ok)
	require.True(t, boolVal)

	boolVal, ok = rp.GetBool("market_user_delivery_price")
	require.True(t, ok)
	require.True(t, boolVal)

	boolVal, ok = rp.GetBool("ext_snippet")
	require.True(t, ok)
	require.True(t, boolVal)

	boolVal, ok = rp.GetBool("no_snippet_arc")
	require.True(t, ok)
	require.False(t, boolVal)

	_, ok = rp.GetBool("some_flag")
	require.False(t, ok)
}

func TestRepeatedFlags(t *testing.T) {
	RepeatedFlags := "flag1=0;flag2=1;flag1=1;flag2=1;flag2=0"
	rp := NewRearrParser(RepeatedFlags)

	boolVal, ok := rp.GetBool("flag1")
	require.True(t, ok)
	require.True(t, boolVal) // последнее значение true

	boolVal, ok = rp.GetBool("flag2")
	require.True(t, ok)
	require.False(t, boolVal) // последнее значение false
}

func TestBadValueFlags(t *testing.T) {
	BadValueFlags := "no_value=;;;;;str_value=znachenie;gde_ravno?;pochemu=ih=mnogo;good=322;"
	rp := NewRearrParser(BadValueFlags)

	_, ok := rp.GetInt("no_value")
	require.False(t, ok)

	_, ok = rp.GetBool("str_value")
	require.False(t, ok)

	_, ok = rp.GetInt("gde_ravno?")
	require.False(t, ok)

	_, ok = rp.GetBool("pochemu")
	require.False(t, ok)

	intVal, ok := rp.GetInt("good")
	require.True(t, ok)
	require.Equal(t, 322, intVal)

	_, ok = rp.GetBool("good")
	require.False(t, ok)

	require.Len(t, rp.flags, 3)
}

func TestMergeITSandRearrFactors(t *testing.T) {
	FlagsForITS := "override_flag=1;new_flag=4;bad_flag=sdasd"
	rp := NewRearrParser(FlagsForITS)
	ss := its.SimpleSettings{}
	ss.Values = make(its.KeyValMap)
	ss.Constraints = make(map[string]uint8)

	ss.Values["its_flag"] = true
	ss.Constraints["its_flag"] = its.Global
	ss.Values["override_flag"] = false
	ss.Constraints["override_flag"] = its.Global
	ss.Values["bad_flag"] = 64.0 // В json всё парсится во float
	ss.Constraints["bad_flag"] = its.Global

	sc := SettingsCreator{st: &ss, rp: rp}

	// Нет ни в its, ни в rearr берём дефолт
	intVal := sc.GetInt("default_value", 322, its.Any)
	require.Equal(t, 322, intVal)

	// Берём из its'a в rearr нет
	boolVal := sc.GetBool("its_flag", false, its.Any)
	require.True(t, boolVal)

	// Берём из rearr, в its нет
	intVal = sc.GetInt("new_flag", 0, its.Any)
	require.Equal(t, 4, intVal)

	// Перезаписывается через rearr_factors
	boolVal = sc.GetBool("override_flag", false, its.Any)
	require.True(t, boolVal)

	// Берём из its в rearr пришло плохое значение
	intVal = sc.GetInt("bad_flag", 0, its.Any)
	require.Equal(t, 64, intVal)
}
