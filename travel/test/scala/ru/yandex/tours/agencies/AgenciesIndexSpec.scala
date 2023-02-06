package ru.yandex.tours.agencies

import java.io.FileOutputStream

import org.mockito.Matchers._
import org.mockito.Mockito._
import ru.yandex.tours.billing.BillingIndex
import ru.yandex.tours.model.{Agency, LocalizedString}
import ru.yandex.tours.testkit.{BaseSpec, TemporaryDirectory, TestData}
import ru.yandex.tours.util.IO

class AgenciesIndexSpec extends BaseSpec with TestData with TemporaryDirectory {
  "Agencies index" should {
    val agencies = Seq(
      Agency(0, LocalizedString.empty, 0, 0, LocalizedString.empty, Iterable.empty, 1, None, None, 0, Seq.empty),
      Agency(1, LocalizedString.empty, 0, 0, LocalizedString.empty, Iterable.empty, 1, None, None, 0, Seq.empty),
      Agency(2, LocalizedString.empty, 0, 0, LocalizedString.empty, Iterable.empty, 1, None, None, 0, Seq.empty)
    )
    val partneringIndex = new AgencyPartneringIndex(Map(1l -> Set(1), 2l -> Set(1)))
//    val index = new AgenciesIndex(agencies.map(a => AgencyWithBilling(a, None)), data.regionTree, partneringIndex, CustomerToOperatorIndex.empty)
    "Take into account 'not partnered' operators" in {
      pending
//      index.getSampleAndCountInRegion(1, 1, 1, None).sample should have size 1
//      index.getSampleAndCountInRegion(1, preferredOperator = None).sample should have size 3
//      index.getSampleAndCountInRegion(1, preferredOperator = Some(1)).sample should have size 1
//      index.getSampleAndCountInRegion(1, preferredOperator = Some(2)).sample should have size 3
    }

    "load from file" in {
      pending
//      val index = Agencies.fromFile(root / "agencies.proto")
//      index should not be empty
    }
  }
}
