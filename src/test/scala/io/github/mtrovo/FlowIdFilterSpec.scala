package io.github.mtrovo

import javax.inject.Inject

import io.github.mtrovo.FlowIdFilterSpec._
import play.api.Application
import play.api.http.HttpFilters
import play.api.inject.bind
import play.api.mvc.Results.EmptyContent
import play.api.mvc._
import play.api.routing.{Router, SimpleRouterImpl}
import play.api.test.{FakeRequest, PlaySpecification}

object FlowIdFilterSpec {
  class IteratorFlowIdGenerator(vals: String*) extends FlowIdGenerator {
    val iter = vals.iterator
    def apply(): String = iter.next
  }

  def returningIdOnResponse(req : Request[AnyContent]): Result = {
    Results.Ok(req.headers.get(FlowIdFilter.FlowIdHeader).getOrElse(""))
  }

  class FlowIdFilters @Inject() (flowIdFilter: FlowIdFilter) extends HttpFilters {
    def filters = Seq(flowIdFilter)
  }

  class FlowIdApplicationRouter @Inject() (action: DefaultActionBuilder) extends SimpleRouterImpl({
    case _ => action(returningIdOnResponse _)
  })
}

class FlowIdFilterSpec extends PlaySpecification {
  def withApplication[T](idgen: FlowIdGenerator, conf: Map[String, _ <: Any] = Map.empty)(block: Application => T): T = {
    running(_.configure(conf).overrides(
      bind[Router].to[FlowIdApplicationRouter],
      bind[HttpFilters].to[FlowIdFilters],
      bind[FlowIdGenerator].toInstance(idgen)
    ))(block)
  }

  def ids(vals: String*) = new IteratorFlowIdGenerator(vals :_*)

  def emptyRequest() = {
    FakeRequest("GET", "/")
  }

  def requestWithFlowId(id: String) = {
    FakeRequest(GET, "/").withHeaders((FlowIdFilter.FlowIdHeader, id))
  }

  "FlowIdFilter when invoked without a FlowID" should {
    "create a new FlowID on request" in withApplication(ids("a", "b", "c")) { app =>
      val result0= route(app, emptyRequest()).get
      status(result0) must_== OK
      contentAsString(result0) must_== "a"

      val result1= route(app, emptyRequest()).get
      status(result1) must_== OK
      contentAsString(result1) must_== "b"
    }

    "use created FlowID on response header" in withApplication(ids("a", "b", "c")) { app =>
      val result = route(app, emptyRequest()).get
      status(result) must_== OK
      contentAsString(result) must_== "a"
      header(FlowIdFilter.FlowIdHeader, result) must_== Some("a")
    }
  }
  "FlowIdFilter when invoked with a FlowID" should {
    "reuse existing FlowID on request" in withApplication(ids()) { app =>
      val result0= route(app, requestWithFlowId("@id")).get
      status(result0) must_== OK
      contentAsString(result0) must_== "@id"

      val result1= route(app, requestWithFlowId("@id2")).get
      status(result1) must_== OK
      contentAsString(result1) must_== "@id2"
    }

    "use provided FlowID on response header" in withApplication(ids("a", "b", "c")) { app =>
      val result = route(app, emptyRequest()).get
      status(result) must_== OK
      contentAsString(result) must_== "@id"
      header(FlowIdFilter.FlowIdHeader, result) must_== Some("@id")
    }
  }

}

