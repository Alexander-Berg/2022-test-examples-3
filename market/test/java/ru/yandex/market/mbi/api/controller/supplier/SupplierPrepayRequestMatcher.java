package ru.yandex.market.mbi.api.controller.supplier;

import java.util.Objects;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import ru.yandex.market.api.cpa.yam.entity.PrepayRequest;

/**
 * Матчер для сравнения {@link PrepayRequest заявок} поставщика.
 *
 * @author fbokovikov
 */
public class SupplierPrepayRequestMatcher extends TypeSafeMatcher<PrepayRequest> {

    private final PrepayRequest prepayRequest;

    public SupplierPrepayRequestMatcher(PrepayRequest prepayRequest) {
        this.prepayRequest = prepayRequest;
    }

    public static SupplierPrepayRequestMatcher equals(PrepayRequest prepayRequest) {
        return new SupplierPrepayRequestMatcher(prepayRequest);
    }

    @Override
    protected boolean matchesSafely(PrepayRequest that) {
        return prepayRequest.getId() == that.getId() &&
                prepayRequest.getPrepayType() == that.getPrepayType() &&
                prepayRequest.getStatus() == that.getStatus() &&
                Objects.equals(prepayRequest.getOrganizationName(), that.getOrganizationName()) &&
                prepayRequest.getOrganizationType() == that.getOrganizationType() &&
                Objects.equals(prepayRequest.getOgrn(), that.getOgrn()) &&
                Objects.equals(prepayRequest.getInn(), that.getInn()) &&
                Objects.equals(prepayRequest.getKpp(), that.getKpp()) &&
                Objects.equals(prepayRequest.getPostcode(), that.getPostcode()) &&
                Objects.equals(prepayRequest.getFactAddress(), that.getFactAddress()) &&
                Objects.equals(prepayRequest.getJurAddress(), that.getJurAddress()) &&
                Objects.equals(prepayRequest.getLicenseNum(), that.getLicenseNum()) &&
                Objects.equals(prepayRequest.getLicenseDate(), that.getLicenseDate()) &&
                Objects.equals(prepayRequest.getBik(), that.getBik()) &&
                Objects.equals(prepayRequest.getBankName(), that.getBankName()) &&
                Objects.equals(prepayRequest.getCorrAccountNumber(), that.getCorrAccountNumber()) &&
                Objects.equals(prepayRequest.getAccountNumber(), that.getAccountNumber()) &&
                Objects.equals(prepayRequest.getContactPerson(), that.getContactPerson()) &&
                Objects.equals(prepayRequest.getPhoneNumber(), that.getPhoneNumber()) &&
                Objects.equals(prepayRequest.getEmail(), that.getEmail()) &&
                Objects.equals(prepayRequest.getSellerClientId(), that.getSellerClientId()) &&
                Objects.equals(prepayRequest.getPersonId(), that.getPersonId()) &&
                Objects.equals(prepayRequest.getContractId(), that.getContractId()) &&
                Objects.equals(prepayRequest.getComment(), that.getComment()) &&
                Objects.equals(prepayRequest.getSignatory(), that.getSignatory()) &&
                prepayRequest.getSignatoryGender() == that.getSignatoryGender() &&
                prepayRequest.getSignatoryDocType() == that.getSignatoryDocType() &&
                Objects.equals(prepayRequest.getSignatoryDocInfo(), that.getSignatoryDocInfo()) &&
                Objects.equals(prepayRequest.getSignatoryPosition(), that.getSignatoryPosition()) &&
                Objects.equals(prepayRequest.getStartDate(), that.getStartDate()) &&
                Objects.equals(prepayRequest.getDocuments(), that.getDocuments()) &&
                prepayRequest.getRequestType() == that.getRequestType();
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(prepayRequest.toString());
    }
}
