package controllers

import (
	"github.com/labstack/echo/v4"

	"a.yandex-team.ru/commerce/blogs_pumpkin/cache"
	"a.yandex-team.ru/commerce/blogs_pumpkin/context"
)

type contextWithCache struct {
	echo.Context

	cache cache.Cache
}

func NewWithCache(c echo.Context, cache cache.Cache) context.Context {
	return &contextWithCache{c, cache}
}

func (c *contextWithCache) Cache() cache.Cache {
	return c.cache
}
