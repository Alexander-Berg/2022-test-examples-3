package strutil

import "testing"

func TestEncodeIcao9303(t *testing.T) {
	tests := []struct {
		name string
		args string
		want string
	}{
		{
			name: "Привет, мир! Latin",
			args: "Привет, мир! Latin",
			want: "Privet, mir! Latin",
		},
		{
			"Французские булочки",
			"Съешь ещё этих мягких французских булок, да выпей [же] чаю",
			"Sieesh eshche etikh miagkikh frantsuzskikh bulok, da vypei [zhe] chaiu",
		},
		{
			"Заглавные французские булочки",
			"СЪЕШЬ ЕЩЁ ЭТИХ МЯГКИХ ФРАНЦУЗСКИХ БУЛОК, ДА ВЫПЕЙ [ЖЕ] ЧАЮ",
			"SIEESH ESHCHE ETIKH MIAGKIKH FRANTSUZSKIKH BULOK, DA VYPEI [ZHE] CHAIU",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := EncodeIcao9303(tt.args); got != tt.want {
				t.Errorf("EncodeIcao9303() = %v, want %v", got, tt.want)
			}
		})
	}
}
