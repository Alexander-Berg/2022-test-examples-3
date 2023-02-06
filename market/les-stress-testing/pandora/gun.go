package main

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/json"
	"fmt"
	"github.com/yandex/pandora/cli"
	"github.com/yandex/pandora/core/aggregator/netsample"
	coreimport "github.com/yandex/pandora/core/import"
	"github.com/yandex/pandora/core/register"
	"io/ioutil"
	"net/http"
	"net/url"
	"strings"
	"time"

	"github.com/yandex/pandora/core"
)

type Ammo struct {
	Queue       string
	MessageBody map[string]interface{}
}

type GunConfig struct {
	Target string `validate:"required"`
	Global int
}

type RequestBody struct {
	QueueURL      string
	MessageBody   string
	AttributeName string `json:"MessageAttribute.1.Name"`
	StringValue   string `json:"MessageAttribute.1.Value.StringValue"`
	DataType      string `json:"MessageAttribute.1.Value.DataType"`
	Action        string
	Version       string
}

type Gun struct {
	client http.Client
	conf   GunConfig
	aggr   core.Aggregator
	core.GunDeps
}

func ExampleGun(conf GunConfig) *Gun {
	return &Gun{conf: conf}
}

func (g *Gun) Bind(aggr core.Aggregator, deps core.GunDeps) error {
	tr := &http.Transport{
		MaxIdleConns:       1,
		IdleConnTimeout:    time.Duration(g.conf.Global) * time.Second,
		DisableCompression: true,
	}
	g.client = http.Client{Transport: tr}
	g.aggr = aggr
	g.GunDeps = deps
	return nil
}

func (g *Gun) Shoot(ammo core.Ammo) {
	customAmmo := ammo.(*Ammo)
	g.shoot(customAmmo)
}

func Sign(key []byte, msg string) []byte {
	return hmac.New(sha256.New, key).Sum([]byte(msg))
}

func GetSignatureKey(key string, dateStamp string, regionName string, serviceName string) []byte {
	kDate := Sign([]byte("AWS4"+key), dateStamp)
	kRegion := Sign(kDate, regionName)
	kService := Sign(kRegion, serviceName)
	kSigning := Sign(kService, "aws4_request")
	return kSigning
}

func (g *Gun) shoot(ammo *Ammo) {
	messageBodyData, err := json.Marshal(ammo.MessageBody)
	if err != nil {
		fmt.Println("Error during reading messageBody as JSON: ", err)
		return
	}

	messageBody := string(messageBodyData)
	method := "POST"
	service := "s"
	host := "sqs.yandex.net:8771"
	region := "eu-west-1"
	contentType := "application/json"
	queueURL := "http://" + g.conf.Target + "/logistics-event-service-ymq-testing/" + ammo.Queue

	requestParamsString, err := json.Marshal(RequestBody{
		QueueURL:      queueURL,
		MessageBody:   messageBody,
		AttributeName: "_type",
		StringValue:   "ru.yandex.market.logistics.les.base.Event",
		DataType:      "String",
		Action:        "SendMessage",
		Version:       "2012-11-05",
	})
	if err != nil {
		fmt.Println("Error during serialization of request params: ", err)
		return
	}

	accessKey := "logistics-event-service-ymq-testing"
	secretKey := "unused"

	t := time.Now().UTC()
	amzDate := fmt.Sprintf("%v%v%vT%v%v%vZ", t.Year(), int(t.Month()), t.Day(), t.Hour(), t.Minute(), t.Second())
	dateStamp := fmt.Sprintf("%v%v%v", t.Year(), int(t.Month()), t.Day())

	canonicalURI := "/"
	canonicalQuerystring := ""

	canonicalHeaders := "content-type:" + contentType + "\n" + "host:" + host + "\n" + "x-amz-date:" + amzDate + "\n"
	signedHeaders := "content-type;host;x-amz-date"

	payloadHash := sha256.Sum256([]byte(requestParamsString))

	canonicalRequest := method + "\n" +
		canonicalURI + "\n" +
		canonicalQuerystring + "\n" +
		canonicalHeaders + "\n" +
		signedHeaders + "\n" +
		fmt.Sprintf("%x", payloadHash)

	algorithm := "AWS4-HMAC-SHA256"
	credentialScope := dateStamp + "/" + region + "/" + service + "/" + "aws4_request"
	stringToSign := algorithm + "\n" +
		amzDate + "\n" +
		credentialScope + "\n" +
		fmt.Sprintf("%x", sha256.Sum256([]byte(canonicalRequest)))

	signingKey := GetSignatureKey(secretKey, dateStamp, region, service)

	signature := fmt.Sprintf("%x", hmac.New(sha256.New, signingKey).Sum([]byte(stringToSign)))

	authorizationHeader := algorithm + " " +
		"Credential=" + accessKey + "/" + credentialScope + ", " +
		"SignedHeaders=" + signedHeaders + ", " +
		"'Signature=' " + signature

	data := url.Values{}
	data.Set("QueueURL", queueURL)
	data.Set("MessageBody", messageBody)
	data.Set("MessageAttribute.1.Name", "_type")
	data.Set("MessageAttribute.1.Value.StringValue", "ru.yandex.market.logistics.les.base.Event")
	data.Set("MessageAttribute.1.Value.DataType", "String")
	data.Set("Action", "SendMessage")
	data.Set("Version", "2012-11-05")

	req, err := http.NewRequest("POST", queueURL, strings.NewReader(data.Encode()))
	if err != nil {
		fmt.Println("Error during creating request: ", err)
		return
	}

	req.Header.Add("Action", "SendMessage")
	req.Header.Add("X-Amz-Date", amzDate)
	req.Header.Add("Authorization", authorizationHeader)
	req.Header.Add("X-Amz-Security-Token", "TOKEN_PLACEHOLDER")

	code := 0
	sample := netsample.Acquire("tag")
	rs, err := g.client.Do(req)
	if err == nil {
		defer func() {
			if errClose := rs.Body.Close(); errClose != nil {
				fmt.Println("Failed to close response body: ", errClose)
			}
		}()
		code = rs.StatusCode
		respBody, _ := ioutil.ReadAll(rs.Body)
		fmt.Println(string(respBody))
	} else {
		fmt.Println("Failed to make request: ", err)
	}

	sample.SetProtoCode(code)
	g.aggr.Report(sample)
}

func main() {
	fs := coreimport.GetFs()
	coreimport.Import(fs)
	coreimport.RegisterCustomJSONProvider("example_provider", func() core.Ammo { return &Ammo{} })
	register.Gun("les-stress-testing", ExampleGun, func() GunConfig {
		return GunConfig{
			Target: "default target",
		}
	})
	cli.Run()
}
