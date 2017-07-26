package io.github.mtrovo

import java.util.UUID
import javax.inject.Inject

import akka.stream.Materializer
import play.api.mvc.{Headers, Result, RequestHeader, Filter}

import scala.concurrent.{ExecutionContext, Future}

trait FlowIdGenerator extends (() => String)
object FlowIdGenerator {
  val UUID: FlowIdGenerator = new FlowIdGenerator {
    override def apply(): String = java.util.UUID.randomUUID().toString
  }
}

class FlowIdFilter @Inject() (idGenerator: FlowIdGenerator = FlowIdGenerator.UUID)(implicit val mat: Materializer, ec: ExecutionContext)
  extends Filter {
  override def apply(next: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    def putIdOnResponse(flowId: String)(res: Result): Result = {
      res.withHeaders((FlowIdFilter.FlowIdHeader, flowId))
    }

    val (rhn, flowId) = rh.headers.get(FlowIdFilter.FlowIdHeader) match {
      case Some(existingId) => (rh, existingId)
      case None =>
        val newId = idGenerator()
        val newHeaders = rh.headers.add((FlowIdFilter.FlowIdHeader, newId))
        (rh.withHeaders(newHeaders), newId)
    }

    next(rhn).map(putIdOnResponse(flowId))
  }
}

object FlowIdFilter {
  val FlowIdHeader = "X-Flow-Id"
}