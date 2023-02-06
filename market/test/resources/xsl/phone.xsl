<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="text" indent="no" omit-xml-declaration="no"/>
    <xsl:strip-space elements="*"/>

    <xsl:variable name="countrycodes"
                  select="'1,7,20,27,30,31,32,33,34,36,39,39,40,41,43,44,44,45,46,47,48,49,51,52,53,54,55,56,57,58,60,61,61,61,62,63,64,65,66,81,82,84,86,90,91,92,93,94,95,98'"/>

    <xsl:template match="//phone">
        <xsl:value-of select="text()"/> -&gt; <xsl:call-template name="format-phone-template"><xsl:with-param name="phone" select="text()"/></xsl:call-template>
        <xsl:text disable-output-escaping="yes">&#13;&#10;</xsl:text>
    </xsl:template>

    <xsl:template name="format-phone-template">
        <xsl:param name="phone"/>
        <xsl:choose>
            <xsl:when test="(starts-with($phone, '8') and string-length($phone)=11)">
                8&#160;<xsl:call-template name="format-local-phone-template">
                <xsl:with-param name="phone" select="substring($phone, 2)"/>
                <xsl:with-param name="nobraces" select="false()"/>
            </xsl:call-template>
            </xsl:when>
            <xsl:when test="starts-with($phone, '+')">
                +&#160;<xsl:call-template name="format-intl-phone-template">
                <xsl:with-param name="csv" select="$countrycodes"/>
                <xsl:with-param name="phone" select="substring($phone, 2)"/>
            </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="format-local-phone-template">
                    <xsl:with-param name="phone" select="$phone"/>
                    <xsl:with-param name="nobraces" select="true()"/>
                </xsl:call-template>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="format-local-phone-template">
        <xsl:param name="phone"/>
        <xsl:param name="nobraces"/>
        <xsl:variable name="_phone" select="translate($phone, ' -()[]?$^_:;','')"/>
        <xsl:choose>
            <xsl:when test="string-length($_phone) &lt;= 4">
                <xsl:value-of select="$_phone"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:variable name="_tmp" select="substring($_phone, 1, 3)"/>
                <xsl:choose>
                    <xsl:when test="$nobraces">
                        <xsl:value-of select="$_tmp"/>
                        <xsl:variable name="_tmp2" select="substring-after($_phone, $_tmp)"/>
                        <xsl:if test="string-length($_tmp2) &gt; 0">
                            <xsl:value-of select="'-'"/>
                            <xsl:call-template name="format-local-phone-template">
                                <xsl:with-param name="phone" select="$_tmp2"/>
                                <xsl:with-param name="nobraces" select="true()"/>
                            </xsl:call-template>
                        </xsl:if>
                    </xsl:when>
                    <xsl:otherwise>(<xsl:value-of select="$_tmp"/>)&#160;<xsl:call-template name="format-local-phone-template">
                        <xsl:with-param name="phone" select="substring-after($_phone, $_tmp)"/>
                        <xsl:with-param name="nobraces" select="true()"/>
                    </xsl:call-template>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="format-intl-phone-template">
        <xsl:param name="csv"/>
        <xsl:param name="phone"/>
        <xsl:variable name="_phone" select="translate($phone, ' -()[]?$^_:;','')"/>
        <xsl:variable name="token" select="normalize-space(substring-before( concat( $csv, ','), ','))"/>
        <xsl:choose>
            <xsl:when test="string-length($token)=0">
                <xsl:call-template name="format-local-phone-template">
                    <xsl:with-param name="phone" select="$_phone"/>
                    <xsl:with-param name="nobraces" select="false()"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:when test="$token and starts-with($_phone, $token)"><xsl:value-of select="$token"/>&#160;<xsl:call-template name="format-local-phone-template">
                <xsl:with-param name="nobraces" select="false()"/>
                <xsl:with-param name="phone" select="substring-after($_phone, $token)"/>
            </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="format-intl-phone-template">
                    <xsl:with-param name="csv" select="substring-after($csv,',')"/>
                    <xsl:with-param name="phone" select="$_phone"/>
                </xsl:call-template>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="text()"/>

</xsl:stylesheet>
