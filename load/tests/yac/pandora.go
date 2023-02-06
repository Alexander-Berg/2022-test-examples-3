package main

import (
	"fmt"
	"log"
	"math/rand"
	"net/url"
	"time"

	"github.com/gorilla/websocket"
	"github.com/spf13/afero"

	"github.com/yandex/pandora/cli"
	phttp "github.com/yandex/pandora/components/phttp/import"
	"github.com/yandex/pandora/core"
	"github.com/yandex/pandora/core/aggregator/netsample"
	coreimport "github.com/yandex/pandora/core/import"
	"github.com/yandex/pandora/core/register"
)

type Ammo struct {
	Tag   string
	Token string
	Text  string
}

type Sample struct {
	URL              string
	ShootTimeSeconds float64
}

type GunConfig struct {
	Target          string `validate:"required"`
	Handler         string `validate:"required"` // Configuration will fail, without target defined
	SleepBeforeSend int    `validate:"required"`
	UserSendFactor  int    `validate:"required"`
	MsgPerUser      int    `validate:"required"`
}

type Gun struct {
	// Configured on construction.
	client *websocket.Conn
	conf   GunConfig
	// Configured on Bind, before shooting
	aggr core.Aggregator // May be your custom Aggregator.
	core.GunDeps
}

func NewGun(conf GunConfig) *Gun {
	return &Gun{conf: conf}
}

func (g *Gun) Bind(aggr core.Aggregator, deps core.GunDeps) error {
	targetPath := url.URL{Scheme: "wss", Host: g.conf.Target, Path: g.conf.Handler}
	sample := netsample.Acquire("connection")
	code := 0
	rand.Seed(time.Now().Unix())
	conn, _, err := websocket.DefaultDialer.Dial(
		targetPath.String(),
		nil,
	)
	if err != nil {
		log.Println("ERROR: dial err:", err)
		code = 500
		return nil
	} else {
		code = 102
	}
	g.client = conn
	g.aggr = aggr
	g.GunDeps = deps
	defer func() {
		sample.SetProtoCode(code)
		g.aggr.Report(sample)
	}()

	go func() {
		for {
			_, message, err := conn.ReadMessage()
			code := 418
			sample := netsample.Acquire("/recieved_messages")

			if err != nil {
				log.Println("go func read error:", err)
				code = 400
				return
			}

			sample.SetProtoCode(code)
			g.aggr.Report(sample)

			log.Printf("go func recv message: %s", message)
		}
	}()

	//err = conn.WriteMessage(websocket.TextMessage, []byte("some websocket connection initialization text, e.g. token"))
	//if err != nil {
	//	log.Println("go func write:", err)
	//}
	return nil
}

func (g *Gun) Shoot(ammo core.Ammo) {
	customAmmo := ammo.(*Ammo)
	g.shoot(customAmmo)
}

func (g *Gun) publicConnect(ammo *Ammo) int {
	publicChat := []byte(`
	{
		"event": "main", 
		"application": "public_chat", 
		"type": "connect", 
		"data": {"chatId": "30cfb7af-213e-4934-88ef-068da30c103e"}}
	`)

	code := 0
	err := g.client.WriteMessage(websocket.TextMessage, publicChat)
	if err != nil {
		log.Println("connection closed", err)
		code = 601
	} else {
		code = 200
	}

	return code
}

func (g *Gun) publicReadMsg(ammo *Ammo) int {
	publicChat := fmt.Sprintf(`
	{
		"token": "%s",
		"event": "main", 
		"application": "public_chat", 
		"type": "get_messages", 
		"data": {"chatId": "30cfb7af-213e-4934-88ef-068da30c103e"}}
	`, ammo.Token)

	code := 0
	err := g.client.WriteMessage(websocket.TextMessage, []byte(publicChat))
	if err != nil {
		log.Println("connection closed", err)
		code = 601
	} else {
		code = 200
	}

	return code
}

func (g *Gun) publicSendMsg(ammo *Ammo, message string) int {
	publicChat := fmt.Sprintf(`
	{
		"token": "%s",
		"event": "main", 
		"application": "public_chat", 
		"type": "send_message", 
		"data": {
			"chatId": "30cfb7af-213e-4934-88ef-068da30c103e",
			"text": "%s"
		}
	}
	`, ammo.Token, message)

	code := 0
	err := g.client.WriteMessage(websocket.TextMessage, []byte(publicChat))
	if err != nil {
		log.Println("connection closed", err)
		code = 602
	} else {
		code = 200
	}

	return code
}

func (g *Gun) shoot(ammo *Ammo) {

	msg := []string{
		"Привет!",
		"Как дела?",
		"Нормально, а у тебя?",
		"Что делаешь?",
		"Ничего, а ты?",
		"Есть тут кто?",
		"И тишина...",
		"Ждем У моря погоды?",
		"И не говори....",
		"Когда начало?",
		"Началось...",
		"Привет! Сегодня дождь и скверно,",
		"А мы не виделись, наверно, сто лет.",
		"Тебе в метро? Скажи на милость,",
		"А ты совсем не изменилась, нет-нет.",
		"Привет! А жить ты будешь долго,",
		"Я вспоминал тебя вот только в обед.",
		"Прости, конечно же, нелепо",
		"Кричать тебе на весь троллейбус «Привет!»",
		"Привет! Дождливо этим летом,",
		"А, впрочем, стоит ли об этом? Ведь нет…",
		"Тогда о чем? О снах, о книгах?",
		"И черт меня попутал крикнуть «Привет!»",
		"Как жизнь? Не то, чтоб очень гладко,",
		"Но, вобщем, знаешь, все в порядке, без бед.",
		"Дела отлично, как обычно.",
		"А с «личным»? Ну, вот только с «личным» — привет…",
		"Привет! А дождь все не проходит,",
		"А я с утра не по погоде одет.",
		"Должно быть, я уже простужен,",
		"Да Бог с ним! Слушай, мне твой нужен совет.",
		"В конце концов, мне дела нету,",
		"Решишь ли ты, что я с «приветом» иль нет,",
		"Но, может, черт возьми, нам снова…",
		"Выходишь здесь? Ну, будь здорова…",
	}

	code := 0
	sample := netsample.Acquire(ammo.Tag + "connect")

	code = g.publicConnect(ammo)
	sample.SetProtoCode(code)
	g.aggr.Report(sample)

	code = 0
	sample = netsample.Acquire(ammo.Tag + "read_messages")
	code = g.publicReadMsg(ammo)
	sample.SetProtoCode(code)
	g.aggr.Report(sample)

	time.Sleep(time.Duration(g.conf.SleepBeforeSend) * time.Second)

	for n := 0; n < g.conf.MsgPerUser; n++ {
		r := rand.Intn(100)
		if r > 100-g.conf.UserSendFactor {
			time.Sleep(time.Duration(rand.Intn(7200)) * time.Second)
			message := msg[rand.Intn(len(msg))]
			sample := netsample.Acquire(ammo.Tag + "send_message")
			code = g.publicSendMsg(ammo, message)
			sample.SetProtoCode(code)
			g.aggr.Report(sample)
			//time.Sleep(time.Duration(600) * time.Second)
		}

	}

	time.Sleep(time.Duration(3600) * time.Second)
	//	defer func() {
	//		sample.SetProtoCode(code)
	//		g.aggr.Report(sample)
	//	}()
}

func main() {
	//debug.SetGCPercent(-1)
	// Standard imports.
	fs := afero.NewOsFs()
	coreimport.Import(fs)
	// May not be imported, if you don't need http guns and etc.
	phttp.Import(fs)

	// Custom imports. Integrate your custom types into configuration system.
	coreimport.RegisterCustomJSONProvider("ammo_provider", func() core.Ammo { return &Ammo{} })

	register.Gun("my_custom_gun_name", NewGun, func() GunConfig {
		return GunConfig{
			Target: "default target",
		}
	})

	cli.Run()
}
