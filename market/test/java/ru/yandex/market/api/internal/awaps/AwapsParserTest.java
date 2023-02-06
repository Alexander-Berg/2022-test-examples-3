package ru.yandex.market.api.internal.awaps;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.domain.v2.Image;
import ru.yandex.market.api.internal.awaps.model.Banner;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.List;

/**
 * Created by vivg on 27.07.16.
 */
public class AwapsParserTest {

    @Test
    public void awapsWithStatPixelAdType() throws Exception { // 80-120 ms, 600-1000 ms, 3700-4000 ms
        AwapsParser parser = new AwapsParser();
        List<Banner> banners = parser.parse(ResourceHelpers.getResource("awaps.json"));
        Assert.assertTrue(banners.isEmpty());
    }

    @Test
    public void awapsNormal() throws Exception {// 800-1300 ms
        AwapsParser parser = new AwapsParser();
        List<Banner> banners = parser.parse(ResourceHelpers.getResource("awaps2.json"));
        Assert.assertEquals(1, banners.size());
        Banner b = banners.get(0);
        Assert.assertEquals("http://awaps.yandex.net/1/c1/tjelGfsHSXMMj5+-fzfN2LF7erU5Nnytb2-hHXK14qi8Xolbm-kK6L-xRwIfE_tLmnmVRJDoWbmYsSmfJ5s2wYXKxGkFc7Phbn3mMvRNVZzobEaXV1Cj7ehKa2W_ttJX+cj37XtiDVUZ6NWTM6+94Oggkxf6unMBF0ywKeSrwnUO8RZgKF2oAH1lb_tjSlGz7IKAOpkFGpMQqtZsq0xrcOw2CHwYrPgoH49i5spoDB1cJPFY8jw-h9e_tsnkRfkUbmfLuphq0+M9V3Wn3VzIq7VLGk-daCfn0yl59B5PkLDCrReY7vKqp_nB8+WbXWUp9BWuNpeDh7vPF6HCRSRCe3nUx6UhaJpbUwfrn-eZRx9_A_.htm",
            b.getClickUrl());
        Assert.assertEquals("yandexmarket://search/?hid=91148", b.getDeepUrl());
        Assert.assertEquals("http://awaps.yandex.net/0/c1/tjelGfsHSXMOut68pYGKMN1z-+lOPxz25syldPGFuNJ5zvlYe01wxm9l0ajuL_t4ZoEWr+6xdgWszD+Q2wREkGPHhUzREHUlM68N2lmEujU7OGpPuZiz1fzYsGZ_tMDwiE1wWMQViNEBZvHOOE-mjRg-3Rab+M2N78QqqzOXdEmEiYgt-0+mdhd1w_trvrCuaXSY9LIOT5aERI1laTGNn+nI7j6hWudEuGz0iytXVN-MrqS5pQ6d5QM_t+7EqNuwIBt0hzkVe0KWydjDVEdTv0k+YcKrBKAEYtr6K-UYG4N9-8dNYL6FK_tWjaNO+84uaHPavnIY4olr6+njvJr1J2CROXKbDEPiJgwD5mUoZNYjHf31gji_KHWWQAUtwAFDbmAAA_A_.gif",
            b.getStatUrl());
        Image image = b.getImage();
        Assert.assertEquals("http://awaps.yandex.net/0/c1/tVK-Oiz0m0j1k6YASgcYXrxitVdTKs6E+-Fjr6YkL3xZn3FkCo83FoCOTq4fG_tWe-qzjrPMNF2t3XgS1GJbLvuu3w7VvF+827lD-z1JAjhqvbQSGIamE-YVhLD_tjvaEnh+rfgx5sjQpRLFV22Ax0obifsPva+vmG5AG3us6zgAKgIP1jX04JEZy_t381VK2L4Q1jRP3En8O+c36DkjLqcntk6BWTCxU2CPA-KJuC0eVlJwDvfXTNR_t0xZszUPFxH44-1qF2w+MPRrQSJBpp+Wc5APgd6f8xbIIjE98wyBDSyg9Di9d_PqsBOvlNmPJMNOrKnzpbQ_A_.jpg",
            image.getUrl());
        Assert.assertEquals(1440, image.getWidth());
        Assert.assertEquals(891, image.getHeight());
    }
}
