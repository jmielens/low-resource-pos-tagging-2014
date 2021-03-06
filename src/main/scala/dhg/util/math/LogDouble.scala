package dhg.util.math

import scala.math._
import scalaz._
import scalaz.Scalaz._

/**
 * This Numeric class represents values using logarithms.  The underlying
 * logarithmic representation is completely hidden from the calling code.
 *
 * This class exists to allow for the use of obvious operators (* for
 * multiplication instead of + on logarithms) and to prevent coding mistakes
 * resulting from the inadvertent mixing of logarithmic and non-logarithmic
 * Double representations of probabilities.  Additionally, it is possible to
 * use the `sum` and `product` collection methods on collections of
 * LogDoubles, and get the expected results.
 *
 * All to* methods return the (non-logarithmic) value stored.  The only
 * way to access the actual logarithmic value is by the 'logValue' field.
 *
 * @author Dan Garrette (dhgarrette@gmail.com)
 */
class LogDouble(val logValue: Double) extends AnyVal with Ordered[LogDouble] {
  def +(other: LogDouble): LogDouble = {
    val oLogValue = other.logValue
    if (logValue == Double.NegativeInfinity)
      other
    else if (oLogValue == Double.NegativeInfinity)
      this
    else if (logValue > oLogValue)
      new LogDouble(logValue + log1p(exp(oLogValue - logValue)))
    else
      new LogDouble(oLogValue + log1p(exp(logValue - oLogValue)))
  }

  def -(other: LogDouble): LogDouble = {
    val oLogValue = other.logValue
    if (oLogValue == 0.0)
      this
    else if (logValue < oLogValue)
      sys.error("subtraction results in a negative LogDouble")
    else
      new LogDouble(logValue + log1p(-exp(oLogValue - logValue)))
  }

  def *(other: LogDouble): LogDouble = new LogDouble(logValue + other.logValue)
  def /(other: LogDouble): LogDouble = new LogDouble(logValue - other.logValue)

  def **(pow: Int): LogDouble = new LogDouble(pow * logValue)
  def **(pow: Double): LogDouble = new LogDouble(pow * logValue)

  override def compare(that: LogDouble) = logValue.compare(that.logValue)
  def max(that: LogDouble): LogDouble = if (this.logValue > that.logValue) this else that
  def min(that: LogDouble): LogDouble = if (this.logValue < that.logValue) this else that

  def approx(o: LogDouble, tolerance: Double): Boolean = (logValue - o.logValue).abs < tolerance
  def approx(o: LogDouble): Boolean = this.approx(o, 1e-10)

  def isZero: Boolean = logValue.isNegInfinity
  def nonZero: Boolean = !logValue.isNegInfinity
  def isNaN: Boolean = logValue.isNaN
  def notNaN: Boolean = !logValue.isNaN
  def isInfinite: Boolean = logValue.isPosInfinity
  def nonInfinite: Boolean = !logValue.isPosInfinity

  def toInt = toDouble.toInt
  def toLong = toDouble.toLong
  def toFloat = toDouble.toFloat
  def toDouble = exp(logValue)

  override def toString = s"LogDouble(${toDouble})"
}

object LogDouble {

  def apply[N](n: N)(implicit num: Numeric[N]): LogDouble = {
    n match {
      case logDouble: LogDouble => logDouble
      case _ =>
        val d = num.toDouble(n)
        require(d >= 0.0, "cannot take log of a negative number")
        new LogDouble(log(d))
    }
  }

  def sum(xs: Seq[LogDouble]): LogDouble = {
    new LogDouble(FastMathUtil.logSum(xs.map(_.logValue)))
  }

  val zero = new LogDouble(Double.NegativeInfinity)
  val one = new LogDouble(0.0)

  trait LogDoubleOrdering extends scala.math.Ordering[LogDouble] {
    override def compare(a: LogDouble, b: LogDouble) = a compare b
  }

  implicit object LogDoubleIsFractional extends LogDoubleIsFractional with LogDoubleOrdering

  trait LogDoubleIsFractional extends Fractional[LogDouble] {
    def plus(x: LogDouble, y: LogDouble): LogDouble = x + y
    def minus(x: LogDouble, y: LogDouble): LogDouble = x - y
    def times(x: LogDouble, y: LogDouble): LogDouble = x * y
    def div(x: LogDouble, y: LogDouble): LogDouble = x / y
    def negate(x: LogDouble): LogDouble = sys.error("LogDouble values cannot be negated")
    def fromInt(x: Int): LogDouble = new LogDouble(log(x))
    def toInt(x: LogDouble): Int = x.toInt
    def toLong(x: LogDouble): Long = x.toLong
    def toFloat(x: LogDouble): Float = x.toFloat
    def toDouble(x: LogDouble): Double = x.toDouble
    override def zero = LogDouble.zero
    override def one = LogDouble.one
  }

  implicit object LogDoubleSemigroup extends Semigroup[LogDouble] {
    override def append(f1: LogDouble, f2: => LogDouble) = f1 + f2
  }
  
}
