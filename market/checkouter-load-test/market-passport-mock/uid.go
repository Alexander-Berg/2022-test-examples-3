package main

import "strconv"

func isLoadUID(uid int64) bool {
	return (2_190_550_858_753_437_195 <= uid && uid <= 2_190_550_859_753_437_194) || (uid == 2_305_843_009_213_693_951)
}

func parseUID(uidStr string) (int64, error) {
	if strconv.IntSize == 64 {
		uid, err := strconv.Atoi(uidStr)
		return int64(uid), err
	} else {
		return strconv.ParseInt(uidStr, 10, 64)
	}

}
