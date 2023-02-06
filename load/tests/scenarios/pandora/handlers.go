package main

import (
	"net/http"
	"strings"
)

type Ammo struct {
	Tag   string
	Delay string
}

func (g *Gun) makeReq(ammo *Ammo) *http.Request {
	req, _ := http.NewRequest("GET", strings.Join([]string{"http://", g.conf.Target, "/test?sleep=", ammo.Delay}, ""), nil)
	req.Header.Add("Content-Type", "application/x-www-form-urlencoded")
	req.Header.Add("Connection", "close")
	req.Header.Add("x-real-ip", "127.0.0.1")
	return req
}
