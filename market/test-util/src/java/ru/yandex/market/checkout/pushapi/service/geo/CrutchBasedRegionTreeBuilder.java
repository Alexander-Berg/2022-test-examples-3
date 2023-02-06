package ru.yandex.market.checkout.pushapi.service.geo;

import org.apache.commons.lang.time.DateUtils;
import ru.yandex.common.util.URLUtils;
import ru.yandex.common.util.region.Region;
import ru.yandex.common.util.region.RegionTree;
import ru.yandex.common.util.region.RegionTreeBuilder;
import ru.yandex.common.util.region.RegionTreeHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

public class CrutchBasedRegionTreeBuilder implements RegionTreeBuilder<Region> {

    private URL plainTextURL;
    private int timeoutMillis = (int) DateUtils.MILLIS_PER_MINUTE;

    public void setPlainTextURL(URL plainTextURL) {
        this.plainTextURL = plainTextURL;
    }

    public void setTimeoutMillis(int timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public RegionTree<Region> buildRegionTree() {
        final RegionTreeHandler regionTreeHandler = new RegionTreeHandler();
        regionTreeHandler.setSkipUnRootRegions(true);

        regionTreeHandler.handleRegion(RegionTree.EARTH, "Земля", 0, null);
        try(
            final InputStream is = URLUtils.safeInputStream(plainTextURL, timeoutMillis, timeoutMillis, 3);
            final BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        ) {
            String line = null;
            try {
                while ((line = br.readLine()) != null) {
                    final String[] strings = line.split("\t");
                    if(strings[0].equals("Id")) {
                        continue;
                    }

                    final int regionId = Integer.parseInt(strings[0]);
                    final String name = strings[1];
                    final int type = Integer.parseInt(strings[2]);
                    final int parentRegionId = strings.length>3 ? Integer.parseInt(strings[3]) : 0;

                    regionTreeHandler.handleRegion(regionId, name, type, parentRegionId == 0 ? null : parentRegionId);
                }

                return new RegionTree<>(regionTreeHandler.getRegion(RegionTree.EARTH));
            } catch(IOException e) {
                throw new RuntimeException("can't parse regions. Last line:" + line, e);
            }
        } catch(Exception e) {
            throw new RuntimeException("can't build region tree", e);
        }
    }

}
