package ru.yandex.market.selftest.clicker;

import java.io.IOException;
import java.net.HttpURLConnection;

import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author kudrale
 */
public class AbstractTrivialPageLoaderTest {

    private static final String URL =
            "https://market-click2.yandex.ru/redir/GAkkM7lQwz62j9BQ6_qgZv9WTxC1Od9yVudW-3hbjKFrknkg_tgJvMaLYm8hK4q8gONt817g3bzDj1oFIWA88Od637rLx1ridiT4tdc7OiMJ58uUTuXqDJo0z6r4PJfb5DGbX1qEldNEvf8E4cgtUi7dZP6HHTUptws9NtzpXX5NrmVjgKY5QH47FNzj33pGFDbW_NLZJ2xUZpZnSz6nN2pCMHcSRFFOlFsaUjrC5ilsv2Eo-mq9AF1xG2EISvOnKjfHikHQIu_DqDXjltsJGslroQG11HiqkkOghNHt_lYsznkabT_wNqzSYtk3GXUFi3FoJKA7Wzql7uJj5u_1CYvOX6U2iim6KCXW4n2nHa59QKeIemCLNfxqs-5NPnN7gveyIdHtnsDa_ANR4F3gZgCQ0UQwiupbVvgYQL7PAcGAOAL7_f34eu2avU0LVOKlex3bRWZE8g29xPmxQbYFBKmzioNh9bcWiAaaxF2b1GS1Zzo0dqR9_I36Ms-h955UinMsky2_d04Jk9A6jYS-PrEVhaqFSQ1fUaGEjkKUTITa7e_ihwVcI5TfWFM_XSKuM8_9VD22abrftTpxqdVNuEKe0kaJuxcFrE401U62uxakT3Mta1e2fzRAFge_D2STa8KOtbIa0sUGVLN0-Pp2SkiKZ9F9N_ddw7tJGZPVTLI,?data=QVyKqSPyGQwwaFPWqjjgNj5C3U2HdAHJAPS1KDX1pWsyzJTi_TXFxObBlR37pqiixPtXY2AcKElEa4zXudqJT3eNgey3xlVhk9oR8rauh6695fek3rQY2Zbm7nLCHRAloGUYQOlM_Q5HxLPeNNSxdg,,&b64e=1&sign=d1e60f2be459c6f5aa928a73fb482e01&keyno=1";

    @Test
    public void testUrlNotEncoded() throws IOException {
        final AbstractTrivialPageLoader pageLoader = mock(AbstractTrivialPageLoader.class);
        when(pageLoader.openConnection(anyString(), any())).thenReturn(mock(HttpURLConnection.class));
        when(pageLoader.load(anyString(), any())).thenCallRealMethod();

        pageLoader.load(URL, null);
        verify(pageLoader).openConnection(eq(URL), any());
    }

}
