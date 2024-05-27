package hankoexercise

import com.auth0.jwk.JwkProviderBuilder
import sttp.client3._

import java.security.PublicKey

object MinimalApplication extends cask.MainRoutes {
  val backend = HttpClientSyncBackend()
  val provider = new JwkProviderBuilder(
    sys.env("HANKO_API_URL")
  ).cached(true).build()

  @cask.get("/")
  def hello() = {
    "Hello World!"
  }

  @cask.get("/authenticated")
  def authenticated(req: cask.Request): cask.Response[String] = {
    import java.time.Clock
    import pdi.jwt.{Jwt, JwtAlgorithm, JwtHeader, JwtClaim, JwtOptions}
    implicit val clock: Clock = Clock.systemUTC
    req.headers.get("authorization") match {
      case Some(token) =>
        val jwtToken = token.head.split(" ")(1)
        val decoded = Jwt.decodeAll(
          jwtToken,
          JwtOptions(signature = false)
        )
        val key =
          Some(
            // We are fixing kid because Hanko frontend does not provide it
            provider.get("4b02ea8f-77e0-498c-8cfd-d48fca302a87").getPublicKey()
          )
        val jwt = key.map(Jwt.decode(jwtToken, _))
        println(jwt)
        cask.Response(jwt.toString, statusCode = 200)
      case None => cask.Response("No token", statusCode = 403)
    }
  }

  initialize()
}
