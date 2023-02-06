<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:import href="../relative/included-xsl.xsl"/>
    <xsl:import href="empty.xsl"/>
    <xsl:output method="html" indent="yes" encoding="utf-8"/>
    <!--<xsl:include href="../relative/included-xsl.xsl"/>-->

    <xsl:template match="*">Тестовый текст
        test_key=<xsl:value-of select="/root/state[@name='test_key']"/>
        test-block=<xsl:apply-templates  select="/root/test-block" mode="include"/>
    </xsl:template>
</xsl:stylesheet>
