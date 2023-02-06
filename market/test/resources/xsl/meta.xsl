<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="html" indent="no"/>

    <!-- Email markup using microdata, see  for details
    http://schema.org/docs/full.html
    https://developers.google.com/gmail/markup/reference/order -->
    <xsl:template name="meta-order-template">
            <xsl:variable name="shopName">
                <xsl:value-of select="shopName/text()"/>
            </xsl:variable>
            <xsl:variable name="shopPhone">
                <xsl:value-of select="shopPhone/text()"/>
            </xsl:variable>

            <div itemscope="itemscope" itemtype="http://schema.org/Order">
                <div itemprop="merchant" itemscope="itemscope" itemtype="http://schema.org/Organization">
                    <meta itemprop="name" content="Яндекс.Маркет"/>
                </div>

                <xsl:variable name="orderNumber">
                    <xsl:choose>
                        <xsl:when test="string-length(order/shopOrderId/text()) &gt; 0">
                            <xsl:value-of select="order/shopOrderId/text()"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="order/id/text()"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>

                <meta itemprop="orderNumber" content="{$orderNumber}"/>
                <meta itemprop="orderDate" content="{order/creationDate}"/>

                <!-- Пока не делаем, т.к. для просмотра нужна авторизация на Я
               <link itemprop="url" href="http://market.yandex.ru/my/orders?id={order/id/text()}"/>
               <div itemprop="potentialAction" itemscope="itemscope" itemtype="http://schema.org/ViewAction">
                   <link itemprop="target" href="http://market.yandex.ru/my/orders?id={id/text()}"/>
                   <meta itemprop="url" content="http://market.yandex.ru/my/orders?id={id/text()}"/>
                   <meta itemprop="name" content="See details"/>
               </div>
               -->
                <xsl:variable name="orderStatus">
                    <xsl:choose>
                        <xsl:when test="order/status = 'PROCESSING'">
                            <xsl:value-of select="'OrderProcessing'"/>
                        </xsl:when>
                        <xsl:when test="order/status = 'PICKUP'">
                            <xsl:value-of select="'OrderPickupAvailable'"/>
                        </xsl:when>
                        <xsl:when test="order/status = 'DELIVERY'">
                            <xsl:value-of select="'OrderInTransit'"/>
                        </xsl:when>
                        <xsl:when test="order/status = 'DELIVERED'">
                            <xsl:value-of select="'OrderDelivered'"/>
                        </xsl:when>
                        <xsl:when test="order/status = 'CANCELLED'">
                            <xsl:value-of select="'OrderCancelled'"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="'OrderProcessing'"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>

                <link itemprop="orderStatus" href="http://schema.org/{$orderStatus}"/>

                <xsl:variable name="currency">
                    <xsl:call-template name="format-currency-iso-4217-template">
                        <xsl:with-param name="currency" select="order/buyerCurrency/text()"/>
                    </xsl:call-template>
                </xsl:variable>

                <xsl:for-each select="order/items/entry/item">
                    <xsl:variable name="imageUrl">
                        <xsl:variable name="smallcase" select="'abcdefghijklmnopqrstuvwxyz'"/>
                        <xsl:variable name="uppercase" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ'"/>
                        <xsl:variable name="picUrl" select="pictures/offer-picture[1]/url[1]/text()"/>
                        <xsl:choose>
                            <xsl:when test="string-length($picUrl)=0">
                            </xsl:when>
                            <xsl:when
                                    test="translate(substring($picUrl, string-length($picUrl) - string-length('.jpg') + 1), $uppercase,  $smallcase)='.jpg'">
                                <xsl:value-of select="$picUrl"/>
                            </xsl:when>
                            <xsl:when
                                    test="translate(substring($picUrl, string-length($picUrl) - string-length('.png') + 1), $uppercase,  $smallcase)='.png'">
                                <xsl:value-of select="$picUrl"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:choose>
                                    <xsl:when test="starts-with($picUrl, '//')">
                                        <xsl:value-of select="concat('http:', $picUrl)"/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:value-of select="$picUrl"/>
                                    </xsl:otherwise>
                                </xsl:choose>
                                <xsl:value-of select="'300x300.jpg'"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:variable>
                    <div itemprop="acceptedOffer" itemscope="itemscope" itemtype="http://schema.org/Offer">
                        <div itemprop="itemOffered" itemscope="itemscope" itemtype="http://schema.org/Product">
                            <meta itemprop="name" content="{offerName}"/>
                            <link itemprop="image" href="{$imageUrl}"/>
                        </div>
                        <div itemprop="eligibleQuantity" itemscope="itemscope"
                             itemtype="http://schema.org/QuantitativeValue">
                            <meta itemprop="value" content="{count}"/>
                        </div>

                        <meta itemprop="price" content="{buyerPrice}"/>
                        <meta itemprop="priceCurrency" content="{$currency}"/>

                        <div itemprop="seller" itemscope="itemscope" itemtype="http://schema.org/Organization">
                            <meta itemprop="name" content="{$shopName}"/>
                            <meta itemprop="telephone" content="{$shopPhone}"/>
                        </div>
                    </div>
                </xsl:for-each>

                <!-- Total -->
                <meta itemprop="price" content="{order/total/text()}"/>
                <meta itemprop="priceCurrency" content="{$currency}"/>

                <!-- delivery charges -->
                <div itemprop="priceSpecification" itemscope="itemscope"
                     itemtype="http://schema.org/DeliveryChargeSpecification">
                    <meta itemprop="price" content="{order/delivery/buyerPrice}"/>
                    <meta itemprop="priceCurrency" content="{$currency}"/>
                </div>

                <div itemprop="customer" itemscope="itemscope" itemtype="http://schema.org/Person">
                    <meta itemprop="name" content="{order/buyer/firstName}"/>
                    <meta itemprop="familyName" content="{order/buyer/lastName}"/>
                    <meta itemprop="telephone" content="{order/buyer/phone}"/>
                    <meta itemprop="email" content="{order/buyer/email}"/>
                </div>
            </div>
    </xsl:template>

    <xsl:template name="format-currency-iso-4217-template">
        <xsl:param name="currency"/>
        <xsl:choose>
            <xsl:when test="$currency='RUB'">RUB</xsl:when>
            <xsl:when test="$currency='RUR'">RUB</xsl:when>
            <xsl:when test="$currency='UAH'">UAH</xsl:when>
            <xsl:when test="$currency='KZT'">KZT</xsl:when>
            <xsl:when test="$currency='BYR'">BYR</xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$currency"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="/">
        <xsl:text disable-output-escaping='yes'>&lt;!DOCTYPE HTML PUBLIC "-//W3C//DTD XHTML 1.0 Transitional //EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"&gt;</xsl:text>
        <xsl:text disable-output-escaping='yes'>&#10;</xsl:text>
        <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="ru" lang="ru">
            <head>
                <meta content="text/html; charset=utf-8" http-equiv="Content-Type"/>
                <title>email order</title>
            </head>
            <body bgcolor="#f6f5f3">
                <!-- В одном письме несколько заказов от разных магазинов -->
                <xsl:apply-templates select="//local-order" />
            </body>
        </html>
    </xsl:template>

    <xsl:template match="//local-order">
        <xsl:call-template name="meta-order-template"/>
    </xsl:template>

    <xsl:template match="text()"/>

</xsl:stylesheet>