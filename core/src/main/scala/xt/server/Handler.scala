package xt.server

import xt._
import xt.middleware.{App, Env}

import org.jboss.netty.channel.{Channel,
                                SimpleChannelUpstreamHandler,
                                ChannelHandlerContext,
                                MessageEvent,
                                ExceptionEvent,
                                ChannelFutureListener}
import org.jboss.netty.handler.codec.http.{HttpRequest,
                                           DefaultHttpResponse,
                                           HttpResponse}
import org.jboss.netty.handler.codec.http.HttpHeaders._
import org.jboss.netty.handler.codec.http.HttpHeaders.Names._
import org.jboss.netty.handler.codec.http.HttpResponseStatus._
import org.jboss.netty.handler.codec.http.HttpVersion._

object Handler {
  /**
   * One may do asynchronous responding by setting IGNORE_RESPONSE to the "env"
   * so that the automatic response is ignored. Then later, use this function to
   * manually respond to the client.
   */
  def respond(channel: Channel, request: HttpRequest, response: HttpResponse) {
    val keepAlive = isKeepAlive(request)

    // Add 'Content-Length' header only for a keep-alive connection.
    // Close the non-keep-alive connection after the write operation is done.
    if (keepAlive) {
      response.setHeader(CONTENT_LENGTH, response.getContent.readableBytes)
    }
    val future = channel.write(response)
    if (!keepAlive) {
      future.addListener(ChannelFutureListener.CLOSE)
    }
  }
}

class Handler(app: App) extends SimpleChannelUpstreamHandler with Logger {
  import Handler._

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (m.isInstanceOf[HttpRequest]) {
      val request  = m.asInstanceOf[HttpRequest]
      val channel  = e.getChannel

      val response = new DefaultHttpResponse(HTTP_1_1, OK)
      val env      = new Env

      app.call(channel, request, response, env)
      if (env.autoRespond) respond(channel, request, response)
    }
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    logger.error("xt.server.Handler", e.getCause)
    e.getChannel.close
  }
}