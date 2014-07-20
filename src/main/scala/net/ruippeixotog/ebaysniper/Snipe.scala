package net.ruippeixotog.ebaysniper

import java.util.{Date, Timer}

import com.jbidwatcher.auction.server.ebay.ebayServer
import com.jbidwatcher.util.Currency
import net.ruippeixotog.ebaysniper.util.Implicits._
import net.ruippeixotog.ebaysniper.util.Logging

import scala.concurrent.{CancellationException, Future, Promise}

case class SnipeInfo(auctionId: String, bid: Currency, quantity: Int, snipeTime: Option[Date])

class Snipe(val info: SnipeInfo)(implicit ebay: ebayServer) extends Logging {

  private[this] var timer: Timer = null
  private[this] var promise: Promise[Int] = null

  def activate(): Future[Int] = {
    if(promise != null) promise.future
    else {
      promise = Promise[Int]()
      timer = new Timer(s"${info.auctionId}-snipe", true)

      timer.schedule(promise.trySuccess {
        log.info("Now sniping {}", info)
        ebay.bid(info.auctionId, info.bid, info.quantity)
      }, info.snipeTime.getOrElse(new Date))

      log.info("Scheduled snipe {}", info)
      promise.future
    }
  }

  def cancel(): Unit = if(promise != null) {
    timer.cancel()
    promise.failure(new CancellationException)
    promise = null
    log.info("Cancelled snipe {}", info)
  }
}
