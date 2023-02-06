<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:output method="html" indent="yes" encoding="utf-8"/>

    <xsl:template match="*">Тестовый текст
        uid=<xsl:value-of select="/root/child/state[@name='uid']"/>
        guard_key=<xsl:value-of select="/root/child/state[@name='guard_key']"/>
        no_guard_key=<xsl:value-of select="/root/child/state[@name='no_guard_key']"/>
        key=<xsl:value-of select="/root/child/state[@name='key']"/>
        test_key=<xsl:value-of select="/root/state[@name='test_key']"/>
        test-block=<xsl:value-of select="/root/test-block"/>
    </xsl:template>
</xsl:stylesheet>
