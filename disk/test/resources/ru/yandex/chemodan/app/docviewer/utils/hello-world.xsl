<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="html"/>

  <xsl:template match="text">
    <html>
      <body>
        <p>
          <xsl:text>Hello, </xsl:text>
          <xsl:value-of select="."/>
        </p>
      </body>
    </html>
  </xsl:template>

</xsl:stylesheet>

