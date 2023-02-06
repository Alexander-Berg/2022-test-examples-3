package ru.yandex.travel.hotels.common.partners.expedia;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;

import com.google.common.io.Resources;
import com.google.i18n.phonenumbers.NumberParseException;
import org.junit.Test;

import ru.yandex.travel.hotels.common.partners.expedia.exceptions.UnexpectedResponseException;
import ru.yandex.travel.hotels.common.partners.expedia.model.common.BedConfiguration;
import ru.yandex.travel.hotels.common.partners.expedia.model.common.Link;
import ru.yandex.travel.hotels.common.partners.expedia.model.content.BedGroup;
import ru.yandex.travel.hotels.common.partners.expedia.model.common.PricingInformation;
import ru.yandex.travel.hotels.common.partners.expedia.model.shopping.PropertyAvailabilityList;
import ru.yandex.travel.hotels.common.partners.expedia.model.shopping.RoomPriceCheck;
import ru.yandex.travel.hotels.common.partners.expedia.model.shopping.ShoppingBedGroup;
import ru.yandex.travel.hotels.common.partners.expedia.model.shopping.ShoppingBedGroupLinks;
import ru.yandex.travel.hotels.common.partners.expedia.model.shopping.ShoppingRate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class HelperTests {
    @Test
    public void testReservationLinkMatches() {
        String link = "/2.4/itineraries?token=QldfCGlcUA4GUldRAAcWF10DUBBCUAxeORJQXQgHVVtWAFQKVhRcUAZXFFBWXwIbAFYMVUkAAANSWFoCBFUAAAEQWlZEUFhUWRZrClJrE1NbXkdRCSE9YnwheXcTSBRRUwFpUF8AXQRdUFkHVVRBBB8UVBFUaFABDlEADgVWA1cPAxZAWUFWO0NBFgcJBVVVWBRVRURXAlsURQBcVx9eABgEA0MLVBBfUBVYCFNsUlcEDQFEEFcWBm5AGxVUCGBtcnkmdjZyH1YEQGZBVRJQSgRWA1lLVBUGVURZAA8EQ1ANB1EPDAsNU1MJWxwFVxxSCBJIBUVHWgZGOUUXCVEPVAA7XVBeBwVVDl4JQFdMF0cEXwJBWGJlfBFfBBEJUkwNXgJoAFxTXgxQDQUJEgFWQA5CWkM7F1ZJE18UXFUEA1VaF10SAQtVUwhfEEwJUwRUWh5XU0gCBhVaAV1nCwANUg8CVlMeTBMPUwkGVwRUFFAPDhMLRV9SVlFIWgURAQ5dQ11UOkNGUUFoAxMNQUYNA1kJRgcWSwdfAxRLUFtTPEsJRBRQUAQWAkQNXUlEFFpVXEhWDAFWCEdSTBFAXQtTQGtFUkBFWl9baAtQWAwAVlANBQ1RGVxdVlpIBAJTD0taAVUGHlUHAAEBBwBZAwACWR8VUBZCXlYLbApXCwwHC1EPBAABHQYKV1IVUlJRUB0KDwBbHAFaAw9XB1hTBlUHAhNFQAoWUBBATD5YAgkBAgNYCRcFB1gHEVBABwFuQVpUVgpQB1UOFAlQGQsAYFcDHApxVgQdAnJQDBoDUwRuQ1AKF1wQFxxvAgxcBgxmMBcBXVBnAFJAVxFdFkEMCVlbCk5BcARGcHBGc1YUJAAcIQVEcyUdIQcTenIXIVJHDwlBdFUSIXIXdFIQIwgccFUWciMUdwZBXXAdIgZDe3UcIQUTXHoRJlIVCFYbQHwERHYkR3dXRF0DF3cIQXt9RyAARHoDRyYIHSFWEnACQwxWF3VSRCRdFwADBzoFDVlRCwVHQllEDFlbCRYCJkRVdhMCV0kQVAxEUBZAQ1QLHAZ1UR1UckMBB0oNGVFBC1YRVXITC1dhEw1bRlMDHFB3F1QERk1GUhYEARUGdkcGV2BDCFh6VFEQBlZABX1ABXITSxNIQAhfVkU-WgcFBlBHd1BXAlIVQAgMBA1XC1EBAgxVBQ==";
        assertThat(Helpers.retrieveReservationToken(link)).isNotEmpty();
    }

    @Test
    public void testConfirmationLinkMatches() {
        String link = "/2.4/itineraries/9150342578742?token=QldfCGlcUA4GUldRAAcWF10DUBBCUAxeORJQXQhRAVlUVwUGBhQBUFACFFAMV1IbAVIBAElRAVkAWgEDAAJVCwUQWlZEUFhUWRZrClJrE1NbXkdRCSE9YnwheXcTSBRRUwFpUF8AXQRdUFkHVVRBBB8UVBFUaFABDlEADgVWA1cPAxZAWUFWO0NBFgcJBVVVWBRVRURXAlsURQBcVx9eABgEA0MLVBBfUBVYCFNsUlcEDQFEEFcWBm5AGxVUCGBtcnkmdjZyH1YEQGZBVRJQSgRWA1lLVBUGVURZAA8EQ1ANB1EPDAsNU1MJWxwFVxxSCBJIBUVHWgZGOUUXCVEPVAA7XVBeBwVVDl4JQFdMF0cEXwJBWGJlfBFfBBEJUkwNXgJoAFxTXgxQDQUJEgFWQA5CWkM7F1ZJE18UXFUEA1VaF10SAQtVUwhfEEwJUwRUWh5XU0gCBhVdCVhRDllWDlcUVlJdQAILR1hWSFcLXxcABAI6WAZbUFJVVgYRFhJbVAUFVAYFElJaCxQMQAsEUAxLDVAWSwdfAxRLUFtTPEsJRBRQUAQWAkQNXUlEFFpVXEhWDAFWCEdSTBFAXQtTQGtFUkBFWl9baAtQWAwAVlANBQ1RGVxdVlpIBAJTD0taAVUGHlUHAAEBBwBZAwACWR8VUBZCXlYLbApXCwwHC1EPBAABHQYKV1IVUlJRUB0KDwBbHAFaAw9XB1hTBlUHAhNFQAoWUBBATD5YAgkBAgNYCRcFB1gHEVBABwFuQVpUVgpQB1UOFAlQGQgHYFcMHApxVgwdAnJXDRoDVQNuQ1AKF1wQFxxvAgxcBgxmMBcBXVBnAFJAVxFdFkEMCVlbCk5BcARGcHBGc1YUJAAcIQVEcyUdIQcTenIXIVJHDwlBdFUSIXIXdFIQIwgccFUWciMUdwZBXXAdIgZDe3UcIQUTXHoRJlIVCFYbQHwERHYkR3dXRF0DF3cIQXt9RyAARHoDRyYIHSFWEnACQwxWF3VSRCRdFwADBzoFDVlRCwVHQllEDFlbCRYCJkRVdhMCV0kQVAxEUBZAQ1QLHAZ1UR1UckMBB0oNGVFBC1YRVXITC1dhEw1bRlMDHFB3F1QERk1GUhYEARUGdkcGV2BDCFh6VFEQBlZABX1ABXITSxNIQAhfVkU-WgcFBlBHd1BXAlIVQAgMBgtXAloCBAxWAg==";
        assertThat(Helpers.retrieveConfirmationToken(link)).isNotEmpty();
    }

    @Test
    public void testConfirmationLinkWithIdMatches() {
        String link = "/2.4/itineraries/9150342578742?token=QldfCGlcUA4GUldRAAcWF10DUBBCUAxeORJQXQhRAVlUVwUGBhQBUFACFFAMV1IbAVIBAElRAVkAWgEDAAJVCwUQWlZEUFhUWRZrClJrE1NbXkdRCSE9YnwheXcTSBRRUwFpUF8AXQRdUFkHVVRBBB8UVBFUaFABDlEADgVWA1cPAxZAWUFWO0NBFgcJBVVVWBRVRURXAlsURQBcVx9eABgEA0MLVBBfUBVYCFNsUlcEDQFEEFcWBm5AGxVUCGBtcnkmdjZyH1YEQGZBVRJQSgRWA1lLVBUGVURZAA8EQ1ANB1EPDAsNU1MJWxwFVxxSCBJIBUVHWgZGOUUXCVEPVAA7XVBeBwVVDl4JQFdMF0cEXwJBWGJlfBFfBBEJUkwNXgJoAFxTXgxQDQUJEgFWQA5CWkM7F1ZJE18UXFUEA1VaF10SAQtVUwhfEEwJUwRUWh5XU0gCBhVdCVhRDllWDlcUVlJdQAILR1hWSFcLXxcABAI6WAZbUFJVVgYRFhJbVAUFVAYFElJaCxQMQAsEUAxLDVAWSwdfAxRLUFtTPEsJRBRQUAQWAkQNXUlEFFpVXEhWDAFWCEdSTBFAXQtTQGtFUkBFWl9baAtQWAwAVlANBQ1RGVxdVlpIBAJTD0taAVUGHlUHAAEBBwBZAwACWR8VUBZCXlYLbApXCwwHC1EPBAABHQYKV1IVUlJRUB0KDwBbHAFaAw9XB1hTBlUHAhNFQAoWUBBATD5YAgkBAgNYCRcFB1gHEVBABwFuQVpUVgpQB1UOFAlQGQgHYFcMHApxVgwdAnJXDRoDVQNuQ1AKF1wQFxxvAgxcBgxmMBcBXVBnAFJAVxFdFkEMCVlbCk5BcARGcHBGc1YUJAAcIQVEcyUdIQcTenIXIVJHDwlBdFUSIXIXdFIQIwgccFUWciMUdwZBXXAdIgZDe3UcIQUTXHoRJlIVCFYbQHwERHYkR3dXRF0DF3cIQXt9RyAARHoDRyYIHSFWEnACQwxWF3VSRCRdFwADBzoFDVlRCwVHQllEDFlbCRYCJkRVdhMCV0kQVAxEUBZAQ1QLHAZ1UR1UckMBB0oNGVFBC1YRVXITC1dhEw1bRlMDHFB3F1QERk1GUhYEARUGdkcGV2BDCFh6VFEQBlZABX1ABXITSxNIQAhfVkU-WgcFBlBHd1BXAlIVQAgMBgtXAloCBAxWAg==";
        assertThat(Helpers.retrieveConfirmationToken(link, "9150342578742")).isNotEmpty();
    }

    @Test
    public void testConfirmationLinkWithWrongIdThrows() {
        String link = "/2.4/itineraries/9150342578742?token=QldfCGlcUA4GUldRAAcWF10DUBBCUAxeORJQXQhRAVlUVwUGBhQBUFACFFAMV1IbAVIBAElRAVkAWgEDAAJVCwUQWlZEUFhUWRZrClJrE1NbXkdRCSE9YnwheXcTSBRRUwFpUF8AXQRdUFkHVVRBBB8UVBFUaFABDlEADgVWA1cPAxZAWUFWO0NBFgcJBVVVWBRVRURXAlsURQBcVx9eABgEA0MLVBBfUBVYCFNsUlcEDQFEEFcWBm5AGxVUCGBtcnkmdjZyH1YEQGZBVRJQSgRWA1lLVBUGVURZAA8EQ1ANB1EPDAsNU1MJWxwFVxxSCBJIBUVHWgZGOUUXCVEPVAA7XVBeBwVVDl4JQFdMF0cEXwJBWGJlfBFfBBEJUkwNXgJoAFxTXgxQDQUJEgFWQA5CWkM7F1ZJE18UXFUEA1VaF10SAQtVUwhfEEwJUwRUWh5XU0gCBhVdCVhRDllWDlcUVlJdQAILR1hWSFcLXxcABAI6WAZbUFJVVgYRFhJbVAUFVAYFElJaCxQMQAsEUAxLDVAWSwdfAxRLUFtTPEsJRBRQUAQWAkQNXUlEFFpVXEhWDAFWCEdSTBFAXQtTQGtFUkBFWl9baAtQWAwAVlANBQ1RGVxdVlpIBAJTD0taAVUGHlUHAAEBBwBZAwACWR8VUBZCXlYLbApXCwwHC1EPBAABHQYKV1IVUlJRUB0KDwBbHAFaAw9XB1hTBlUHAhNFQAoWUBBATD5YAgkBAgNYCRcFB1gHEVBABwFuQVpUVgpQB1UOFAlQGQgHYFcMHApxVgwdAnJXDRoDVQNuQ1AKF1wQFxxvAgxcBgxmMBcBXVBnAFJAVxFdFkEMCVlbCk5BcARGcHBGc1YUJAAcIQVEcyUdIQcTenIXIVJHDwlBdFUSIXIXdFIQIwgccFUWciMUdwZBXXAdIgZDe3UcIQUTXHoRJlIVCFYbQHwERHYkR3dXRF0DF3cIQXt9RyAARHoDRyYIHSFWEnACQwxWF3VSRCRdFwADBzoFDVlRCwVHQllEDFlbCRYCJkRVdhMCV0kQVAxEUBZAQ1QLHAZ1UR1UckMBB0oNGVFBC1YRVXITC1dhEw1bRlMDHFB3F1QERk1GUhYEARUGdkcGV2BDCFh6VFEQBlZABX1ABXITSxNIQAhfVkU-WgcFBlBHd1BXAlIVQAgMBgtXAloCBAxWAg==";
        assertThatThrownBy(() -> Helpers.retrieveConfirmationToken(link, "wrong")).isInstanceOf(UnexpectedResponseException.class);
    }

    @Test
    public void testRefundLinkMatches() {
        String itineraryId = "9885339650434";
        String roomId = "d6a03284-8b94-456e-91b1-fd50633d0ed6";
        String token = "QldfCGlcUA4GXFZVAw8WF10DUBBCUAxeORJQXQgNWgwABV4GAxQHVw1VFFBRXwIbAQFUU0kGWlkDDlQBAgRQBwMQWlZEUFhUWRZrClJrE1NbXkdRCSE9YnwheXcTSBRRUwFpUF8AXQRdUFkHVVRBBB8UVBFUaFABDlEADgVWA1cPDhZAWUFWO0NBFgcJBVVVWBRVRURXAlsURQBcVx9eABNYUxcNUBZdWwZuAFFWCQJHQlAWB2kWGkFRXzZldH19cmUmEQpTTWcTVU1WR1tTWFVDBxNdUEMKUAkAQlFcAFAOC1xZV1UBWE4IUBwGXRcTWUZMClJBaxNGCVMMClI5UQFZAQRVCw1bEQVEFEZcC1YYDDRrIRFbWUVZABcLWV87Uw1WDVpXXF8FR1xcRApAWRVuQVIVEFpKA1JbX1BVFlEQB1BRAQhfRRcNVwgFWBlVUR5UWENWX1JRCAReDQsVVQhUGgNVSA8DGVdcCxIGV1U9CAJYAlVVUVRAFkdeBl8HAQgAQ1dYW0ZZEFxXBQMeUQhDVg5fSgddORNKXEdrBEoJRBYOU1gIEFFCSwFSA0FEUAtSOxdaFhNSXF9GUxZfVhlGRVpVVh1WXwdXDhJXFEVMXlhQRjsWV0oWWVlbZw9cDVAEUgZRUABaGVcCAg8fVQEABkhTDg8DHltXU1IEUgYPVAQBAxNAAURLDw1aPllXBFcIAgdeBQkDGFdTAgdOBlQGC0hfAQNSGFdXUwwLVVQFUVUBVRASEV5EBxdFTGxQVwpQAVUPAB4GUVdWRgdBXF1vFlxVVA5RBAUJSQIFSAJSNgNQQFZxUVYdUHAAUB9XDQxiQlRcQQ1AFEw6BVgCXVg2YRIBV1E8UwNCBUZQFUEIXg8FVBwTfAcXJ1dHcwhBclcSJwUXCFEQJQkcDFQWdFEUcXFBIQMdJAZDfQEcJ3YTIAkRWiAVdFMVJ3wRJQRAIANDJVQWCnUTQX0IRyZxRHwAR1oIHSdWEnZ2Q3BUF3NQRCJVFCBWRiFXRw8FRyYDFQBzThN2BhZ2JkQmBBMJJB0hBUdyAUd9VkN7fRBwUx0kAksWcQhBWwVBfVQRJAMTfVUQJl0QJ1EUe1URcVQTcHYQVVZSbFNaWQRdAkFGAEJRXlsIEVEnFw4nFQQHSRNZXhBfR05EAVEdByVQEQpxRAtURgxLUhxXAUYAdxFUAS9QWFcWAgcWVnQdVFBAGEBWHFMLFFF5RApTfghcUyQGVhAHAEBRcUcBcUdCE0RDWFsEQm4LBgtQVxd3CgBSXkBMXgpRBVYDDwFVDA4D";
        String link = "/2.4/itineraries/" + itineraryId + "/rooms/" + roomId + "?token=" + token;
        assertThat(Helpers.retriveRoomAndRefundToken(link, itineraryId)).containsExactly(roomId, token);
    }

    @Test
    public void testCheckPriceMatches() {
        String propertyId = "26199";
        String roomId = "187779";
        String rateId = "238100368";
        String token = "Ql1WAERHXV1QOxULUAtTVQ5WBwMACk8AUQIMFAEGBgtLCFAFARQHUwxVAVYDXgICDgMTCQFHAAlQVxZrXQBpQFFVWEFSDnVtZydwLHUSEl9MVGpBTRQAD10AVldAVBIeXwdVRkcAXQBBCVZHWUtCPlQHRw5UQ1ALVDxQXlUIXQFVEF1SQl5WEF5WAT1SBFUOCUdVUAxfFFkGUFxAQUsxZxNHUxEDag1EQQheCAkVRlMVVW4WG0YHXmJgIyt1dGF9FVIDRw1SBAhHV1FWVw1cVwQCUgQBHANQGQQCQkJVF0cLB0A7FRdfBwpUBm5dAQxWCAIBXA8VVxZGFFALBU5bbTYgElkCQF4GQw9fAWtaDVQPXwRUWAcQW19XBggNQkxZAlUGWhkCA08FUh5bUQFsWQUMAABXVwIeEkYPXQwMVAQGQltYDRBVVDxFFl1GPlMXDUYWXANSXkBdQktdBAFCE1FfBT1LVxYUVFEOFFUUW1VPEg4KQUQFFhYSDVpSED1BVUtDDFlba1pTWQcHDFAFUllSGAMFClUUUgQKDBgNVwhSHF4KAVtVAAJXD1UHBRVFXBZGDQtbPAhVBARRBlYEBgYGGgZSBVQYAwACVhlWVANeHAYFDFUHUAtUBlcEWUBLUQhTQGgCWwJWWgENCU9XA0oPQQAXUFwLVhFSQlECbBBQW1UOAgUCXRoIVE8FUGQCCkQKcFMMRAsgBlQcA19WaBNGUwkDRj1RWxdYFFtdWVcPRAwKDUIHD25bDAlIE15KQ2gWRw1SH14IWE1WRlsTWlZFDEFKSGwAW1BVWWBhQ1EABm0AABZTEwpIF1hbCwxSExF8VBJxAEZwVhAnVBIiCUBcBxEnAxBbBkN1VhF7IxAlAUR6VRJyCBJwJ0YmBh1cc0BzUxFwdEdxUR17BEB3AUQJdRxBIQMdJHdDfQAcXQQTIAgRICYVdFMVJwoRJQRAIANDJVQWCgEdIAgdWicbRHsDRyN6HSdWEg1yQ3BUF3NaRCJVFCAiRiFWR3UET0d2AR0IVBNxBBZ1VEQmBBNyXB0hBUdyDkd6VEN7exNWBlw5UgldU1ADFkYFTQ1bCA4TDCcQUyYQUVNATANaRg9CSxEEBRYFcgEQBSERVwZHCExdFAcHEVckFwtXe19bX0MKAkEEcBJTARdBRAFEBgoXUnhDB1d6XlcCcQZXEwZUFlN9EwV3FkZGFEdUDwdGPllXBFMNFxJKCFsEahVdQAcPDwEBB0tTDER3XQRSDUdGWQ9VAANSUwRTWw==";
        String link = "/2.4/properties/26199/rooms/187779/rates/238100368?token=Ql1WAERHXV1QOxULUAtTVQ5WBwMACk8AUQIMFAEGBgtLCFAFARQHUwxVAVYDXgICDgMTCQFHAAlQVxZrXQBpQFFVWEFSDnVtZydwLHUSEl9MVGpBTRQAD10AVldAVBIeXwdVRkcAXQBBCVZHWUtCPlQHRw5UQ1ALVDxQXlUIXQFVEF1SQl5WEF5WAT1SBFUOCUdVUAxfFFkGUFxAQUsxZxNHUxEDag1EQQheCAkVRlMVVW4WG0YHXmJgIyt1dGF9FVIDRw1SBAhHV1FWVw1cVwQCUgQBHANQGQQCQkJVF0cLB0A7FRdfBwpUBm5dAQxWCAIBXA8VVxZGFFALBU5bbTYgElkCQF4GQw9fAWtaDVQPXwRUWAcQW19XBggNQkxZAlUGWhkCA08FUh5bUQFsWQUMAABXVwIeEkYPXQwMVAQGQltYDRBVVDxFFl1GPlMXDUYWXANSXkBdQktdBAFCE1FfBT1LVxYUVFEOFFUUW1VPEg4KQUQFFhYSDVpSED1BVUtDDFlba1pTWQcHDFAFUllSGAMFClUUUgQKDBgNVwhSHF4KAVtVAAJXD1UHBRVFXBZGDQtbPAhVBARRBlYEBgYGGgZSBVQYAwACVhlWVANeHAYFDFUHUAtUBlcEWUBLUQhTQGgCWwJWWgENCU9XA0oPQQAXUFwLVhFSQlECbBBQW1UOAgUCXRoIVE8FUGQCCkQKcFMMRAsgBlQcA19WaBNGUwkDRj1RWxdYFFtdWVcPRAwKDUIHD25bDAlIE15KQ2gWRw1SH14IWE1WRlsTWlZFDEFKSGwAW1BVWWBhQ1EABm0AABZTEwpIF1hbCwxSExF8VBJxAEZwVhAnVBIiCUBcBxEnAxBbBkN1VhF7IxAlAUR6VRJyCBJwJ0YmBh1cc0BzUxFwdEdxUR17BEB3AUQJdRxBIQMdJHdDfQAcXQQTIAgRICYVdFMVJwoRJQRAIANDJVQWCgEdIAgdWicbRHsDRyN6HSdWEg1yQ3BUF3NaRCJVFCAiRiFWR3UET0d2AR0IVBNxBBZ1VEQmBBNyXB0hBUdyDkd6VEN7exNWBlw5UgldU1ADFkYFTQ1bCA4TDCcQUyYQUVNATANaRg9CSxEEBRYFcgEQBSERVwZHCExdFAcHEVckFwtXe19bX0MKAkEEcBJTARdBRAFEBgoXUnhDB1d6XlcCcQZXEwZUFlN9EwV3FkZGFEdUDwdGPllXBFMNFxJKCFsEahVdQAcPDwEBB0tTDER3XQRSDUdGWQ9VAANSUwRTWw==";
        assertThat(Helpers.retrievePriceCheckToken(link, propertyId, roomId, rateId)).isEqualTo(token);
        assertThat(Helpers.retrievePriceCheckToken(link)).isEqualTo(token);
    }

    @Test
    public void testRounder() throws IOException {
        String data = Resources.toString(Resources.getResource("expediaResponses/PriceCheckMatchResponse.json"), Charset.defaultCharset());
        RoomPriceCheck roomPriceCheck = DefaultExpediaClient.createObjectMapper().readerFor(RoomPriceCheck.class).readValue(data);
        PricingInformation pricing = Helpers.round(roomPriceCheck.getOccupancyPricing().get("2"));
        assertThat(pricing.getTotals().getExclusive().getBillableCurrency().getValue()).isEqualTo("518.00");
        assertThat(pricing.getTotals().getInclusive().getBillableCurrency().getValue()).isEqualTo("570.00");
        pricing.getNightly().stream().flatMap(Collection::stream).forEach(item -> assertThat(item.getValue().toString().endsWith(".00")).isTrue());
    }

    @Test
    public void testEmailConversion() {
        assertThat(Helpers.convertEmailDomainNameToAscii("foo@яндекс.рф")).isEqualTo("foo@xn--d1acpjx3f.xn--p1ai");
        assertThat(Helpers.convertEmailDomainNameToAscii("foo@yandex.ru")).isEqualTo("foo@yandex.ru");
        // we convert only domains, logins are left intact even if they are in unicode
        assertThat(Helpers.convertEmailDomainNameToAscii("фуу@yandex.ru")).isEqualTo("фуу@yandex.ru");
        assertThat(Helpers.convertEmailDomainNameToAscii("яндекс.рф")).isEqualTo("яндекс.рф");
    }

    @Test
    public void testRoundingOnSyntheticData() throws IOException {
        String data = Resources.toString(Resources.getResource("expediaResponses/roundingTestSynthetic.json"), Charset.defaultCharset());
        PricingInformation pricing = DefaultExpediaClient.createObjectMapper().readerFor(PricingInformation.class).readValue(data);
        var inclusiveBefore = pricing.getTotals().getInclusive().getBillableCurrency().getValue();
        var exclusiveBefore = pricing.getTotals().getExclusive().getBillableCurrency().getValue();
        Helpers.round(pricing);
        var inclusiveAfter = pricing.getTotals().getInclusive().getBillableCurrency().getValue();
        var exclusiveAfter = pricing.getTotals().getExclusive().getBillableCurrency().getValue();
        assertThat(inclusiveAfter).isEqualTo(inclusiveBefore);
        assertThat(exclusiveAfter).isEqualTo(exclusiveAfter);

    }


    @Test
    public void testRounderForAlreadyRoundRate() throws IOException {
        String data = Resources.toString(Resources.getResource("expediaResponses/roundingTestWithFx1.json"), Charset.defaultCharset());
        PropertyAvailabilityList list = DefaultExpediaClient.createObjectMapper().readerFor(PropertyAvailabilityList.class).readValue(data);
        var rate = list.get(0).getRooms().get(2).getRates().get(3);
        assertThat(rate.getId()).isEqualTo("230109672");
        var inclusiveBefore = rate.getOccupancyPricing().get("1").getTotals().getInclusive().getBillableCurrency().getValue();
        var exclusiveBefore = rate.getOccupancyPricing().get("1").getTotals().getExclusive().getBillableCurrency().getValue();
        Helpers.round(rate.getOccupancyPricing().get("1"));
        var inclusiveAfter = rate.getOccupancyPricing().get("1").getTotals().getInclusive().getBillableCurrency().getValue();
        var exclusiveAfter = rate.getOccupancyPricing().get("1").getTotals().getExclusive().getBillableCurrency().getValue();
        assertThat(inclusiveAfter).isEqualTo(inclusiveBefore);
        assertThat(exclusiveAfter).isEqualTo(exclusiveAfter);
    }

    @Test
    public void testRounderV2_4() throws IOException {
        String data = Resources.toString(Resources.getResource("expediaResponses/ShoppingRate.json"), Charset.defaultCharset());
        ShoppingRate shoppingRate = DefaultExpediaClient.createObjectMapper().readerFor(ShoppingRate.class).readValue(data);
        PricingInformation rate = shoppingRate.getOccupancyPricing().get("2");
        var rounded = Helpers.round(rate);
        assertThat(rounded.getTotals().getExclusive().getBillableCurrency().getValue()).isEqualTo("518.00");
        assertThat(rounded.getTotals().getInclusive().getBillableCurrency().getValue()).isEqualTo("570.00");
        rounded.getNightly().stream().flatMap(Collection::stream).forEach(item -> assertThat(item.getValue().toString().endsWith(".00")).isTrue());
    }


    @Test
    public void testRoundingOnSyntheticDataV2_4() throws IOException {
        String data = Resources.toString(Resources.getResource("expediaResponses/roundingTestSynthetic.json"), Charset.defaultCharset());
        PricingInformation rate = DefaultExpediaClient.createObjectMapper().readerFor(PricingInformation.class).readValue(data);
        var inclusiveBefore = rate.getTotals().getInclusive().getBillableCurrency().getValue();
        var exclusiveBefore = rate.getTotals().getExclusive().getBillableCurrency().getValue();
        var rounded = Helpers.round(rate);
        var inclusiveAfter = rounded.getTotals().getInclusive().getBillableCurrency().getValue();
        var exclusiveAfter = rounded.getTotals().getExclusive().getBillableCurrency().getValue();
        assertThat(inclusiveAfter).isEqualTo(inclusiveBefore);
        assertThat(exclusiveAfter).isEqualTo(exclusiveAfter);
    }

    @Test
    public void testRounderForAlreadyRoundRateV2_4() throws IOException {
        String data = Resources.toString(Resources.getResource("expediaResponses/roundingTestWithFx1.json"), Charset.defaultCharset());
        ru.yandex.travel.hotels.common.partners.expedia.model.shopping.PropertyAvailabilityList list = DefaultExpediaClient.createObjectMapper().readerFor(ru.yandex.travel.hotels.common.partners.expedia.model.shopping.PropertyAvailabilityList.class).readValue(data);
        var rate = list.get(0).getRooms().get(2).getRates().get(3);
        assertThat(rate.getId()).isEqualTo("230109672");
        var inclusiveBefore = rate.getOccupancyPricing().get("1").getTotals().getInclusive().getBillableCurrency().getValue();
        var exclusiveBefore = rate.getOccupancyPricing().get("1").getTotals().getExclusive().getBillableCurrency().getValue();
        var rounded = Helpers.round(rate.getOccupancyPricing().get("1"));
        var inclusiveAfter = rounded.getTotals().getInclusive().getBillableCurrency().getValue();
        var exclusiveAfter = rounded.getTotals().getExclusive().getBillableCurrency().getValue();
        assertThat(inclusiveAfter).isEqualTo(inclusiveBefore);
        assertThat(exclusiveAfter).isEqualTo(exclusiveAfter);
    }

    @Test
    public void testMapBedGroupDetails() {
        List<ShoppingBedGroup> shoppingBedGroups = List.of(
            ShoppingBedGroup.builder()
                    .configuration(List.of(BedConfiguration.builder()
                            .type("TwinBed")
                            .size("Twin")
                            .quantity(2)
                            .build()))
                    .links(ShoppingBedGroupLinks.builder().priceCheck(Link.builder().href("foo").build()).build())
                    .build(),
                ShoppingBedGroup.builder()
                        .configuration(List.of(BedConfiguration.builder()
                                .type("KingBed")
                                .size("King")
                                .quantity(1)
                                .build()))
                        .links(ShoppingBedGroupLinks.builder().priceCheck(Link.builder().href("bar").build()).build())
                        .build());
        List<BedGroup> propertyBedGroups = List.of(
                BedGroup.builder()
                        .id("37321")
                        .description("1 двуспальная кровать «Кинг-сайз»")
                        .configuration(List.of(BedConfiguration.builder()
                                .type("KingBed")
                                .size("King")
                                .quantity(1)
                                .build()))
                        .build(),
                BedGroup.builder()
                        .id("37341")
                        .description("2 односпальных кровати")
                        .configuration(List.of(BedConfiguration.builder()
                                .type("TwinBed")
                                .size("Twin")
                                .quantity(2)
                                .build()))
                        .build()
                );

        var mapped = Helpers.mapBedGroupDetails(shoppingBedGroups, propertyBedGroups);
        assertThat(mapped.size()).isEqualTo(2);
        assertThat(mapped.stream().filter(bg -> bg.getId().equals("37341")).findFirst().get().getLinks().getPriceCheck().getHref()).isEqualTo("foo");
        assertThat(mapped.stream().filter(bg -> bg.getId().equals("37321")).findFirst().get().getLinks().getPriceCheck().getHref()).isEqualTo("bar");
    }

    @Test
    public void testPhoneNumbersRussianWith7() throws NumberParseException {
        var res = Helpers.parsePhone("78001234567");
        assertThat(res.getCountryCode()).isEqualTo("7");
        assertThat(res.getNumber()).isEqualTo("8001234567");
    }


    @Test
    public void testPhoneNumbersRussian8() throws NumberParseException {
        var res = Helpers.parsePhone("88001234567");
        assertThat(res.getCountryCode()).isEqualTo("7");
        assertThat(res.getNumber()).isEqualTo("8001234567");
    }

    @Test
    public void testUkrainianPhone() throws NumberParseException {
        var res = Helpers.parsePhone("38044232481");
        assertThat(res.getCountryCode()).isEqualTo("380");
        assertThat(res.getNumber()).isEqualTo("44232481");
    }

}
