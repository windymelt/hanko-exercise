package hankoexercise

import com.raquo.airstream.ownership
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.raquo.laminar.tags.CustomHtmlTag
import com.raquo.waypoint._
import org.scalajs.dom
import sttp.client3.FetchBackend
import typings.teamhankoHankoElements.mod.register
import typings.teamhankoHankoFrontendSdk.distLibDtoMod.User
import typings.teamhankoHankoFrontendSdk.mod.Hanko

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.util.Failure
import scala.util.Success

enum Pages {
  case Index
  case Login
  case Logout
  case Profile
}

object Main extends App {
  val indexRoute = Route.static(Pages.Index, root / endOfSegments)
  val loginRoute = Route.static(Pages.Login, root / "login" / endOfSegments)
  val logoutRoute = Route.static(Pages.Logout, root / "logout" / endOfSegments)
  val profileRoute = Route.static(Pages.Profile, root / "dash" / endOfSegments)

  val router = new Router[Pages](
    routes = List(indexRoute, loginRoute, logoutRoute, profileRoute),
    serializePage = p => p.ordinal.toString,
    deserializePage = ordStr => Pages.fromOrdinal(ordStr.toInt),
    getPageTitle = p => p.toString()
  )(popStateEvents = windowEvents(_.onPopState), owner = unsafeWindowOwner)

  val container = dom.document.querySelector("#app")
  val hankoVar = Var[Option[Hanko]](None)

  def renderPage(page: Pages) = {
    page match {
      case Pages.Index   => IndexPage(hankoVar)
      case Pages.Login   => LoginPage(hankoVar)
      case Pages.Logout  => LogoutPage
      case Pages.Profile => Dashboard(hankoVar)
    }
  }
  val app = router.currentPageSignal.map(renderPage)
  // val app = Application(hankoVar)

  render(container, div(child <-- app)) // should be called after render
  register("http://localhost:5173/.hanko").`then`(result => {
    hankoVar.set(Some(result.hanko.asInstanceOf[Hanko]))
    println("Hanko Elements registered")
  })
  def IndexPage(hankoVar: Var[Option[Hanko]]) = {
    div(
      cls := "container",
      hankoVar.signal --> { h =>
        h.foreach(
          _.user
            .getCurrent()
            .`then`(_ => {
              println("ok")
              router.replaceState(Pages.Profile)
            })
            .`catch`(_ => {
              println("no")
              router.replaceState(Pages.Login)
            })
        )
      },
      "Loading..."
    )
  }
  def Dashboard(hankoVar: Var[Option[Hanko]]) = {
    val logoutFunc = Var[Option[() => Unit]](None)
    val userSignal = EventBus[User]()
    val userJwt = Var[Option[String]](None)
    val resultString = Var[String]("Not sent yet...")
    div(
      cls := "container",
      hankoVar.signal --> (
        _.foreach(h => {
          logoutFunc.set(Some(() => h.user.logout()))
          h.user
            .getCurrent()
            .`then`(u => {
              println(u.email)
              userSignal.emit(u)
            })
          userJwt.set(Some(h.session.get().jwt.get))
        })
      ),
      "you are logged in",
      div(
        child <-- userSignal.events.map(u => s"You are ${u.email}")
      ),
      button(
        className := "btn btn-primary",
        "send authorized request",
        onClick --> { _ =>
          println("sent")
          import sttp.client3._
          val backend = FetchBackend()
          val res = basicRequest.auth
            .bearer(userJwt.now().get)
            .get(uri"http://localhost:5173/api/authenticated")
            .send(backend)
          res.foreach(resp => resultString.set(resp.body.toString()))
        }
      ),
      div(
        code(
          text <-- resultString
        )
      ),
      button(
        className := "btn btn-secondary",
        "logout",
        onClick --> { _ =>
          logoutFunc.now().foreach(_())
          logoutFunc.set(None)
          router.replaceState(Pages.Logout)
        }
      )
    )
  }
  def LogoutPage = {
    div(
      cls := "container",
      "You are logged out.",
      button(
        className := "btn btn-secondary",
        "login",
        onClick --> { _ =>
          router.replaceState(Pages.Login)
        }
      )
    )
  }
  def LoginPage(hankoVar: Var[Option[Hanko]]) = {
    div(
      cls := "container",
      hankoVar.signal --> (_.foreach(h => {
        h.onAuthFlowCompleted((flow) => {
          println("Auth flow completed.")
          println(s"user: ${flow.userID}")
          println(s"jwt: ${h.session.get().jwt}")
          router.replaceState(Pages.Profile)
        })
        h.onSessionCreated((sess) => {
          println("Session created")
          println(sess.jwt)
          println(s"Expires in: ${sess.expirationSeconds} secs")
        })
      })),
      HankoAuth()
    )
  }
}

object HankoAuth extends WebComponent {
  @js.native
  trait RawElement extends js.Object {}
  @js.native
  @JSImport("@teamhanko/hanko-elements", JSImport.Default)
  object RawImport extends js.Object

  used(RawImport)

  type Ref = dom.html.Element & RawElement

  protected val tag: CustomHtmlTag[Ref] = CustomHtmlTag("hanko-auth")

  object slots {}

  object events {}
}

trait WebComponent {
  val id: HtmlProp[String, String] = idAttr

  type Ref <: dom.HTMLElement

  type ModFunction = this.type => Mod[ReactiveHtmlElement[Ref]]
  type ComponentMod = ModFunction | Mod[ReactiveHtmlElement[Ref]]

  protected def tag: CustomHtmlTag[Ref]

  /** Instantiate this component using the specified modifiers.
    *
    * Modifiers can be the usual Laminar modifiers, or they can be functions
    * from this component to a modifier. Allowing these functions is very
    * practical to access the reactive attributes of the component, with the
    * `_.reactiveAttr` syntax.
    */
  final def apply(mods: ComponentMod*): HtmlElement = tag(
    mods
      .map {
        case mod: Mod[_ >: ReactiveHtmlElement[Ref]] => (_: this.type) => mod
        case mod: Function[_ >: this.type, _ <: ReactiveHtmlElement[Ref]] => mod
      }
      .map(_(this))*
  )
}

// stub function to mark some value as used to compiler
def used(any: Any): Unit = ()
