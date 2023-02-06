<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-result-prefixes="xs"
                xmlns:str="http://exslt.org/strings"
                xmlns:math="http://exslt.org/math"
                xmlns:date="http://exslt.org/dates-and-times"
                xmlns:regexp="http://exslt.org/regular-expressions">
    <xsl:output method="text" indent="yes" encoding="utf-8"/>

    <xsl:template match="*">Extensions:
        1.1) str:encode-uri "http%3A%2F%2Fya.ru%3Fk%3D1%26k2%3D3" = "<xsl:value-of select="str:encode-uri(concat('http://ya.ru?k=1', '&#38;k2=3'),true())"/>"
        1.2) str:encode-uri "4%2C5%2C6" = "<xsl:value-of select="str:encode-uri(data-split/text(),true())"/>"
        1.3) str:encode-uri "" = "<xsl:value-of select="str:encode-uri(data-split-empty/text(),true())"/>"
        2.1) str:split "1,2,3,4" = "<xsl:for-each select="str:split('1,2,3,4', ',')">
            <xsl:value-of select="text()"/>
            <xsl:text>+</xsl:text>
        </xsl:for-each>"
        2.2) str:split from node = "<xsl:for-each select="str:split(data-split/text(), ',')">
            <xsl:value-of select="text()"/>
            <xsl:text>+</xsl:text>
        </xsl:for-each>"
        2.3) str:split from empty node = "<xsl:for-each select="str:split(data-split-empty/text(), ',')">
            <xsl:value-of select="text()"/>
            <xsl:text>+</xsl:text>
        </xsl:for-each>"
        3) math:log log(100) = <xsl:value-of select="math:log(100)"/>
        4) math:power 2^3 = <xsl:value-of select="math:power(2, 3)"/>
        5.1) regexp:replace 1+2+3+4 = <xsl:value-of select="regexp:replace('1&#10;2&#10;3&#10;4', '(&#10;)+','g', '+')"
                      disable-output-escaping="yes"/>
        5.2) regexp:replace 4+5+6 = <xsl:value-of select="regexp:replace(data-split/text(), ',','g', '+')"
                                                  disable-output-escaping="yes"/>
        5.3) regexp:replace '' = '<xsl:value-of select="regexp:replace(data-split-empty/text(), ',','g', '+')"
                                                  disable-output-escaping="yes"/>'
        6) math:random = <xsl:value-of select="math:random()>0"/>
        7+8) format-date(date-time()) = <xsl:value-of select="date:format-date(date:date-time(),'%Y', 'C')"/>

    </xsl:template>
</xsl:stylesheet>
