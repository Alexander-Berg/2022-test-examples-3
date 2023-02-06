<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:output method="xml" indent="yes" encoding="utf-8"/>

    <xsl:template match="child">
        <test-block>test_block_key=<xsl:value-of select="state[@name='included_key']"/></test-block>
    </xsl:template>
</xsl:stylesheet>
