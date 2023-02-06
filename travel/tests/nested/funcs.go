package nested

import "a.yandex-team.ru/travel/library/go/funcnames"

var (
	PrivateFnName     funcnames.Caller
	PublicFnName      funcnames.Caller
	FullPrivateFnName funcnames.Caller
	FullPublicFnName  funcnames.Caller
)

func init() {
	PrivateFnName = funcnames.BuildName(getPrivateFnName)
	PublicFnName = funcnames.BuildName(GetPublicFnName)
	FullPrivateFnName = funcnames.BuildFullName(getPrivateFnName)
	FullPublicFnName = funcnames.BuildFullName(GetPublicFnName)
}

// funcs return variables for checking initialization loop
func getPrivateFnName() string { return PrivateFnName.String() }
func GetPublicFnName() string  { return PublicFnName.String() }
