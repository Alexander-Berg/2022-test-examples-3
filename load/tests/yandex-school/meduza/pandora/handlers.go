package main

import (
	"fmt"
	"net/http"
	"strings"
)

func (g *Gun) makeReq(payload *Payload) *http.Request {
	req, _ := http.NewRequest(payload.Method, strings.Join([]string{"http://", g.conf.Target, payload.URI}, ""), strings.NewReader(""))
	req.Header.Add("Content-Type", payload.Cotype)
	req.Header.Add("X-Real-IP", "213.180.206.57")
	req.Header.Add("User-Agent", "pandora")
	return req
}

func (g *Gun) genPayload(ammo *Ammo) *Payload {
	payload := Payload{
		Method: "GET",
		Assert: "error",
		Cotype: "text/plain",
	}
	URI := strings.Split(ammo.Tag, "_")[0]
	switch URI {
	// Controller /v1/blogs
	case "blogs":
		payload.URI = "/v1/blogs/"
	case "blogsPrivate":
		payload.URI = "/v1/blogs/private"
		// Controller /v1/blog
	case "blog":
		payload.URI = fmt.Sprintf("/v1/%s", ammo.BID)
	case "blogSubscribers":
		payload.URI = fmt.Sprintf("/v1/%s/subscribers", ammo.BID)
	case "blogShowcase":
		payload.URI = fmt.Sprintf("/v1/%s/showcase", ammo.BID)
		// Controller /v1/schools
	case "schools":
		payload.URI = fmt.Sprintf("/v1/schools/%s", ammo.BID)
	case "schoolsArchived":
		payload.URI = fmt.Sprintf("/v1/schools/%s/archived", ammo.BID)
	case "schoolsDrafts":
		payload.URI = fmt.Sprintf("/v1/schools/%s/drafts", ammo.BID)
	case "schoolsPublished":
		payload.URI = fmt.Sprintf("/v1/schools/%s/published", ammo.BID)
		// Controller /v1/school
	case "school":
		payload.URI = fmt.Sprintf("/v1/school/%s/%s", ammo.BID, ammo.SID)
		// Controller /v1/post
	case "post":
		payload.URI = fmt.Sprintf("/v1/post/%s/%s", ammo.BID, ammo.PID)
	case "postRelated":
		payload.URI = fmt.Sprintf("/v1/post/related/%s/%s", ammo.BID, ammo.PID)
		// Controller /v1/mediaMaterials
	case "mediaMaterialsDrafts":
		payload.URI = fmt.Sprintf("/v1/media-materials/%s/drafts", ammo.BID)
	case "mediaMaterialsFuture":
		payload.URI = fmt.Sprintf("/v1/media-materials/%s/future", ammo.BID)
	case "mediaMaterialsPublished":
		payload.URI = fmt.Sprintf("/v1/media-materials/%s/published", ammo.BID)
		// Controller /v1/taskRunner
	case "taskRunnerAggregator":
		payload.URI = "/v1/taskRunner/aggregator"
	case "taskRunnerSaas":
		payload.URI = "/v1/taskRunner/saas"
	case "taskRunnerPostsPublish":
		payload.URI = "/v1/taskRunner/postsPublish"
	case "taskRunnerMailer":
		payload.URI = "/v1/taskRunner/mailer"
	case "taskRunnerInstantMail":
		payload.URI = "/v1/taskRunner/instantMail"
		// Controller /v1/category
	case "categoriesAll":
		payload.URI = fmt.Sprintf("/v1/categories/all/%s", ammo.BID)
	case "categoriesNotEmpty":
		payload.URI = fmt.Sprintf("/v1/categories/notEmpty/%s", ammo.BID)
	case "categoryExist":
		payload.URI = fmt.Sprintf("/v1/category/exist/%s", ammo.BID)
		// Controller /v1/popularPosts
	case "popularPostsUsed":
		payload.URI = fmt.Sprintf("/v1/popularPosts/used/%s", ammo.BID)
	case "popularPosts":
		payload.URI = fmt.Sprintf("/v1/popularPosts/%s", ammo.BID)
	default:
		payload.URI = "/ping"
	}
	return &payload
}
