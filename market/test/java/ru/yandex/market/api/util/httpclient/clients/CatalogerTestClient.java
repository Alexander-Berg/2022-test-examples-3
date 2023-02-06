package ru.yandex.market.api.util.httpclient.clients;

import java.util.function.Function;

import io.netty.handler.codec.http.HttpResponseStatus;
import it.unimi.dsi.fastutil.longs.LongList;
import org.springframework.stereotype.Service;

import ru.yandex.market.api.geo.GeoUtils;
import ru.yandex.market.api.util.CommonCollections;
import ru.yandex.market.api.util.httpclient.spi.HttpRequestExpectationBuilder;
import ru.yandex.market.api.util.httpclient.spi.HttpResponseConfigurer;

/**
 * @author dimkarp93
 */
@Service
public class CatalogerTestClient extends AbstractFixedConfigurationTestClient {

    public CatalogerTestClient() {
        super("Cataloger");
    }

    public HttpResponseConfigurer getChildren(int nid, String resultFileName) {
        return getTree(nid, 1, GeoUtils.Region.MOSCOW, resultFileName);
    }

    public HttpResponseConfigurer getTree(int nid, int depth, int region, String resultFileName) {
        return configure(b -> b.serverMethod("cataloger/GetNavigationTree")
            .param("nid", String.valueOf(nid))
            .param("depth", String.valueOf(depth))
            .apply(cataloger(region))
        ).ok().body(resultFileName);
    }

    public HttpResponseConfigurer getPath(int hid,  String resultFileName) {
        return getPath(hid, GeoUtils.Region.MOSCOW, resultFileName);
    }


    public HttpResponseConfigurer getPath(int hid, int region, String resultFileName) {
        return configure(b -> b.serverMethod("cataloger/GetNavigationPath")
            .param("hid", String.valueOf(hid))
            .apply(cataloger(region))
        ).ok().body(resultFileName);
    }

    public HttpResponseConfigurer getPathByNid(int nid, int region, String resultFileName) {
        return configure(b -> b.serverMethod("cataloger/GetNavigationPath")
                .param("nid", String.valueOf(nid))
                .apply(cataloger(region))
        ).ok().body(resultFileName);
    }

    public HttpResponseConfigurer getPathNotFound(int hid) {
        return getPathNotFound(hid, GeoUtils.Region.MOSCOW);
    }

    public HttpResponseConfigurer getPathNotFound(int hid, int region) {
        return configure(b -> b.serverMethod("cataloger/GetNavigationPath")
            .param("hid", String. valueOf(hid))
            .apply(cataloger(region))
        ).status(HttpResponseStatus.NOT_FOUND);
    }

    public HttpResponseConfigurer getVendors(LongList vendorIds, String resultFileName) {
        return configure(b -> b.serverMethod("cataloger/GetBrandInfo")
            .param("ids", CommonCollections.join(vendorIds, String::valueOf))
        ).ok().body(resultFileName);
    }

    public HttpResponseConfigurer getPopularBrands(int hid, int count, String resultFileName) {
        return configure(b -> b.serverMethod("cataloger/GetPopularBrands")
            .param("hid", String.valueOf(hid))
            .param("n", String.valueOf(count))
        ).ok().body(resultFileName);
    }

    private Function<HttpRequestExpectationBuilder, HttpRequestExpectationBuilder> cataloger(int region) {
        return b -> b.get()
            .param("region", String.valueOf(region));
    }

}
