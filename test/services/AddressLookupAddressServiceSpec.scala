package services

import model.ProposedAddress
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.Json
import services.AddressReputationFormats._
import uk.gov.hmrc.address.v2.{Address, AddressRecord, Countries, LocalCustodian}
import uk.gov.hmrc.play.http.hooks.HttpHook
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpResponse, Upstream5xxResponse}

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.util.Random

class AddressLookupAddressServiceSpec extends PlaySpec with OneAppPerSuite with ScalaFutures {

  class Scenario(resp: Option[HttpResponse] = None) {
    implicit val hc = HeaderCarrier()
    val end = "http://localhost:42"

    val get = new HttpGet {

      override protected def doGet(url: String)(implicit hc: HeaderCarrier) = {
        if (!url.startsWith(s"$end/v2/uk/addresses?")) throw new IllegalArgumentException(s"Unexpected endpoint URL: $url")
        resp match {
          case Some(r) => Future.successful(r)
          case None => throw new Upstream5xxResponse("Service unavailable", 503, 503)
        }
      }

      override val hooks: Seq[HttpHook] = Seq.empty
    }

    val service = new AddressLookupAddressService() {
      override val endpoint = end
      override val http = get
    }
  }

  "find" should {

    "find addresses by postcode" in new Scenario(
      resp = Some(HttpResponse(200, Some(Json.toJson(oneAddress))))
    ) {
      service.find("postcode").futureValue must be (toProposals(oneAddress))
    }

  }

  private val oneAddress = someAddresses()

  private def someAddresses(num: Int = 1): List[AddressRecord] = {
    (0 to num).map { i =>
      addr
    }.toList
  }

  private def addr: AddressRecord = AddressRecord(
    rndstr(16),
    Some(Random.nextLong()),
    Address(
      List(rndstr(16), rndstr(16), rndstr(8)),
      Some(rndstr(16)),
      Some(rndstr(8)),
      rndstr(8),
      Some(Countries.England),
      Countries.UK
    ),
    "en",
    Some(LocalCustodian(123, "Tyne & Wear")), None, None, None, None
  )

  private def rndstr(i: Int): String = Random.alphanumeric.take(i).mkString

  private def toProposals(found: List[AddressRecord]): Seq[ProposedAddress] = {
    found.map { addr =>
      ProposedAddress(
        addr.id,
        addr.address.postcode,
        addr.address.lines,
        addr.address.town,
        addr.address.county,
        addr.address.country
      )
    }
  }





}